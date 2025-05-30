package com.cms.service;

import com.cms.dto.AttendanceDTO;
import com.cms.dto.AttendancePercentageDTO;
import com.cms.entities.*;
import com.cms.repository.*;

import jakarta.mail.internet.MimeMessage;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AttendanceService {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private FacultyRepository facultyRepository;

    @Autowired
    private BatchRepository batchRepository;

    @Autowired
    private FacultyCourseRepository facultyCourseRepository;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Value("${attendance.threshold:75.0}")
    private double attendanceThreshold;

    /**
     * Generate an Excel template for attendance recording
     */
    public ByteArrayInputStream generateAttendanceTemplate(Long facultyId, Long courseId, String batchName, 
                                                          String department, String section) throws IOException {
        // Validate faculty, course, and batch
        Faculty faculty = facultyRepository.findById(facultyId)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));
        
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        
        // Get students based on filters
        List<Student> students;
        
        if (department != null && !department.isEmpty() && section != null && !section.isEmpty()) {
            // Filter by batch, department, and section
            students = studentRepository.findByBatchNameAndDepartmentAndSection(batchName, department, section);
            if (students.isEmpty()) {
                throw new RuntimeException("No students found in batch " + batchName + 
                                          " with department " + department + " and section " + section);
            }
        } else if (department != null && !department.isEmpty()) {
            // Filter by batch and department only
            students = studentRepository.findByBatchNameAndDepartment(batchName, department);
            if (students.isEmpty()) {
                throw new RuntimeException("No students found in batch " + batchName + 
                                          " with department " + department);
            }
        } else if (section != null && !section.isEmpty()) {
            // Filter by batch and section only
            students = studentRepository.findByBatchNameAndSection(batchName, section);
            if (students.isEmpty()) {
                throw new RuntimeException("No students found in batch " + batchName + 
                                          " with section " + section);
            }
        } else {
            // Get all students in the batch (existing functionality)
            students = studentRepository.findByBatchName(batchName);
            if (students.isEmpty()) {
                throw new RuntimeException("No students found in the batch");
            }
        }

        // Create workbook and sheet
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Attendance");

        // Create header row
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Student ID");
        headerRow.createCell(1).setCellValue("Student Name");
        headerRow.createCell(2).setCellValue("Student Roll No");
        headerRow.createCell(3).setCellValue("Course ID");
        headerRow.createCell(4).setCellValue("Course Code");
        headerRow.createCell(5).setCellValue("Batch");
        headerRow.createCell(6).setCellValue("Semester No");
        headerRow.createCell(7).setCellValue("Total Periods");
        headerRow.createCell(8).setCellValue("Periods Attended");
        headerRow.createCell(9).setCellValue("Date (YYYY-MM-DD)");
        headerRow.createCell(10).setCellValue("Department");
        headerRow.createCell(11).setCellValue("Section");

        // Fill student data
        int rowNum = 1;
        for (Student student : students) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(student.getId());
            row.createCell(1).setCellValue(student.getName());
            row.createCell(2).setCellValue(student.getDno());
            row.createCell(3).setCellValue(course.getId());
            row.createCell(4).setCellValue(course.getCode());
            row.createCell(5).setCellValue(batchName);
            row.createCell(6).setCellValue(course.getSemesterNo());
            row.createCell(7).setCellValue(""); // Total periods to be filled by faculty
            row.createCell(8).setCellValue(""); // Periods attended to be filled by faculty
            
            // Set current date as default
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            row.createCell(9).setCellValue(dateFormat.format(new Date()));
            
            // Add department and section
            row.createCell(10).setCellValue(student.getDepartment());
            row.createCell(11).setCellValue(student.getSection() != null ? student.getSection() : "");
        }

        // Auto-size columns
        for (int i = 0; i < 12; i++) {
            sheet.autoSizeColumn(i);
        }

        // Write to output stream
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    /**
     * Process attendance data from uploaded Excel file
     */
    @Transactional
    public List<AttendanceDTO> processAttendanceFromExcel(Long facultyId, String department, 
                                                         String section, MultipartFile file) throws IOException {
        Faculty faculty = facultyRepository.findById(facultyId)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));

        List<Attendance> attendanceList = new ArrayList<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            // Skip header row
            if (rows.hasNext()) {
                rows.next();
            }

            while (rows.hasNext()) {
                Row currentRow = rows.next();
                
                // Skip empty rows
                if (isRowEmpty(currentRow)) {
                    continue;
                }

                // Extract data from Excel
                Long studentId = getLongCellValue(currentRow.getCell(0));
                Long courseId = getLongCellValue(currentRow.getCell(3));
                String batchName = getStringCellValue(currentRow.getCell(5));
                Integer semesterNo = getIntegerCellValue(currentRow.getCell(6));
                Integer totalPeriods = getIntegerCellValue(currentRow.getCell(7));
                Integer periodsAttended = getIntegerCellValue(currentRow.getCell(8));
                String date = getStringCellValue(currentRow.getCell(9));
                String studentDepartment = getStringCellValue(currentRow.getCell(10));
                String studentSection = getStringCellValue(currentRow.getCell(11));

                // Validate required fields
                if (studentId == null || courseId == null || batchName == null || 
                    semesterNo == null || totalPeriods == null || periodsAttended == null) {
                    continue; // Skip invalid rows
                }

                // Apply department filter if provided
                if (department != null && !department.isEmpty() && 
                    !department.equalsIgnoreCase(studentDepartment)) {
                    continue;
                }
                
                // Apply section filter if provided
                if (section != null && !section.isEmpty() && 
                    !section.equalsIgnoreCase(studentSection)) {
                    continue;
                }

                // Fetch entities
                Student student = studentRepository.findById(studentId)
                        .orElseThrow(() -> new RuntimeException("Student not found with ID: " + studentId));
                
                Course course = courseRepository.findById(courseId)
                        .orElseThrow(() -> new RuntimeException("Course not found with ID: " + courseId));
                
                // With this:
                List<FacultyCourse> facultyCourses = facultyCourseRepository
                        .findAllByFacultyIdAndCourseId(facultyId, courseId);

                if (facultyCourses.isEmpty()) {
                    throw new RuntimeException("Faculty is not assigned to this course");
                }

                // Create attendance record
                Attendance attendance = new Attendance();
                attendance.setStudent(student);
                attendance.setCourse(course);
                attendance.setFaculty(faculty);
                attendance.setBatchName(batchName);
                attendance.setSemesterNo(semesterNo);
                attendance.setTotalPeriods(totalPeriods);
                attendance.setPeriodsAttended(periodsAttended);
                attendance.setDate(date);
                
                // Calculate attendance percentage
                attendance.calculateAttendancePercentage();
                
                attendanceList.add(attendance);
            }
        }

        // Save all attendance records
        List<Attendance> savedAttendance = attendanceRepository.saveAll(attendanceList);
        
        // Check for attendance below threshold and send notifications
        checkAndNotifyLowAttendance(savedAttendance);
        
        // Convert to DTOs
        return savedAttendance.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get attendance records for a faculty's course and batch with optional filters
     */
 // Only updating the getAttendanceByFacultyCourseAndBatch method, the rest of the file remains the same

    /**
     * Get attendance records for a faculty's course and batch with optional filters
     */
    public List<AttendanceDTO> getAttendanceByFacultyCourseAndBatch(Long facultyId, Long courseId, 
                                                                   String batchName, String department, String section) {
        List<Attendance> attendanceList = attendanceRepository
                .findByFacultyIdAndCourseIdAndBatchName(facultyId, courseId, batchName);
        
        // Apply filters if provided
        if (department != null && !department.isEmpty() || section != null && !section.isEmpty()) {
            attendanceList = attendanceList.stream()
                .filter(a -> {
                    boolean match = true;
                    
                    // Filter by department if provided
                    if (department != null && !department.isEmpty()) {
                        match = match && department.equalsIgnoreCase(a.getStudent().getDepartment());
                    }
                    
                    // Filter by section if provided
                    if (section != null && !section.isEmpty()) {
                        match = match && section.equalsIgnoreCase(a.getStudent().getSection());
                    }
                    
                    return match;
                })
                .collect(Collectors.toList());
        }
        
        return attendanceList.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Generate attendance report Excel for a course and batch with optional filters
     */
    public ByteArrayInputStream generateAttendanceReport(Long facultyId, Long courseId, 
                                                       String batchName, String department, String section) throws IOException {
        List<Attendance> attendanceList = attendanceRepository
                .findByFacultyIdAndCourseIdAndBatchName(facultyId, courseId, batchName);
        
        if (attendanceList.isEmpty()) {
            throw new RuntimeException("No attendance records found");
        }

        // Apply filters if provided
        if (department != null && !department.isEmpty() || section != null && !section.isEmpty()) {
            attendanceList = attendanceList.stream()
                .filter(a -> {
                    boolean match = true;
                    
                    // Filter by department if provided
                    if (department != null && !department.isEmpty()) {
                        match = match && department.equalsIgnoreCase(a.getStudent().getDepartment());
                    }
                    
                    // Filter by section if provided
                    if (section != null && !section.isEmpty()) {
                        match = match && section.equalsIgnoreCase(a.getStudent().getSection());
                    }
                    
                    return match;
                })
                .collect(Collectors.toList());
            
            if (attendanceList.isEmpty()) {
                String filterDesc = "";
                if (department != null && !department.isEmpty()) {
                    filterDesc += "department: " + department;
                }
                if (section != null && !section.isEmpty()) {
                    filterDesc += (filterDesc.isEmpty() ? "" : " and ") + "section: " + section;
                }
                throw new RuntimeException("No attendance records found for " + filterDesc);
            }
        }

        // Create workbook and sheet
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Attendance Report");

        // Create styles
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        // Create header row
        Row headerRow = sheet.createRow(0);
        String[] columns = {"Student ID", "Student Name", "Roll No", "Department", "Section", "Course Code", "Batch", 
                           "Semester", "Total Periods", "Periods Attended", "Attendance %", "Date"};
        
        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }

        // Fill data rows
        int rowNum = 1;
        for (Attendance attendance : attendanceList) {
            Row row = sheet.createRow(rowNum++);
            
            row.createCell(0).setCellValue(attendance.getStudent().getId());
            row.createCell(1).setCellValue(attendance.getStudent().getName());
            row.createCell(2).setCellValue(attendance.getStudent().getDno());
            row.createCell(3).setCellValue(attendance.getStudent().getDepartment());
            row.createCell(4).setCellValue(attendance.getStudent().getSection() != null ? 
                                          attendance.getStudent().getSection() : "");
            row.createCell(5).setCellValue(attendance.getCourse().getCode());
            row.createCell(6).setCellValue(attendance.getBatchName());
            row.createCell(7).setCellValue(attendance.getSemesterNo());
            row.createCell(8).setCellValue(attendance.getTotalPeriods());
            row.createCell(9).setCellValue(attendance.getPeriodsAttended());
            row.createCell(10).setCellValue(String.format("%.2f%%", attendance.getAttendancePercentage()));
            row.createCell(11).setCellValue(attendance.getDate());
        }

        // Auto-size columns
        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // Write to output stream
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    // The rest of the methods remain unchanged
    
    /**
     * Get attendance records for a student in a specific course
     */
    public List<AttendanceDTO> getStudentAttendanceForCourse(Long studentId, Long courseId) {
        List<Attendance> attendanceList = attendanceRepository
                .findByStudentIdAndCourseId(studentId, courseId);
        
        return attendanceList.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get attendance percentage for a student across all courses
     */
    public List<AttendancePercentageDTO> getStudentAttendancePercentageByCourses(Long studentId, Integer semesterNo) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        
        // Get all courses for the student based on semester if provided
        List<Course> courses;
        if (semesterNo != null) {
            courses = courseRepository.findBySemesterNo(semesterNo);
        } else {
            courses = courseRepository.findAll();
        }
        
        List<AttendancePercentageDTO> result = new ArrayList<>();
        
        for (Course course : courses) {
            // Get latest attendance record for this course
            List<Attendance> attendanceRecords = attendanceRepository
                    .findByStudentIdAndCourseIdOrderByDateDesc(studentId, course.getId());
            
            if (!attendanceRecords.isEmpty()) {
                Attendance latestAttendance = attendanceRecords.get(0);
                
                AttendancePercentageDTO dto = new AttendancePercentageDTO();
                dto.setStudentId(student.getId());
                dto.setStudentName(student.getName());
                dto.setStudentDno(student.getDno());
                dto.setCourseId(course.getId());
                dto.setCourseCode(course.getCode());
                dto.setCourseTitle(course.getTitle());
                dto.setFacultyId(latestAttendance.getFaculty().getId());
                dto.setFacultyName(latestAttendance.getFaculty().getName());
                dto.setSemesterNo(course.getSemesterNo());
                dto.setAttendancePercentage(latestAttendance.getAttendancePercentage());
                dto.setIsBelowThreshold(latestAttendance.getAttendancePercentage() < attendanceThreshold);
                
                result.add(dto);
            }
        }
        
        return result;
    }
    
    /**
     * Check attendance below threshold and send notifications
     */
    private void checkAndNotifyLowAttendance(List<Attendance> attendanceList) {
        for (Attendance attendance : attendanceList) {
            if (attendance.getAttendancePercentage() < attendanceThreshold) {
                // Get student email from user
                User user = attendance.getStudent().getUser();
                if (user != null && user.getEmail() != null) {
                    sendLowAttendanceNotification(
                        attendance.getStudent(),
                        attendance.getCourse(),
                        attendance.getFaculty(),
                        attendance.getAttendancePercentage()
                    );
                }
            }
        }
    }
    
    /**
     * Send notification for low attendance
     */
    private void sendLowAttendanceNotification(Student student, Course course, Faculty faculty, double percentage) {
        User user = student.getUser();
        if (user == null || user.getEmail() == null) {
            return;
        }
        
        String subject = "Low Attendance Alert - " + course.getCode();
        String body = "<p>Dear " + student.getName() + ",</p>"
                + "<p>This is to inform you that your attendance in <strong>" + course.getTitle() 
                + " (" + course.getCode() + ")</strong> has fallen below the required threshold.</p>"
                + "<p><strong>Current Attendance:</strong> " + String.format("%.2f%%", percentage) + "</p>"
                + "<p><strong>Required Attendance:</strong> " + attendanceThreshold + "%</p>"
                + "<p><strong>Faculty:</strong> " + faculty.getName() + "</p>"
                + "<p>Please improve your attendance to avoid any academic penalties.</p>"
                + "<p>Thank you.</p>"
                + "<p>With Regards,</p>"
                + "<p>College Management System</p>";
        
        try {
            MimeMessage message = emailService.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(user.getEmail());
            helper.setSubject(subject);
            helper.setText(body, true);
            
            emailService.sendEmail(message);
        } catch (Exception e) {
            // Log error but don't stop the process
            System.err.println("Failed to send attendance notification: " + e.getMessage());
        }
    }
    
    /**
     * Scheduled task to check attendance and send notifications
     * Runs daily at midnight
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void scheduledAttendanceCheck() {
        checkAttendanceBelowThresholdAndNotify();
    }
    
    /**
     * Check all students' attendance and send notifications for those below threshold
     */
    @Transactional(readOnly = true)
    public Map<String, Object> checkAttendanceBelowThresholdAndNotify() {
        List<Student> students = studentRepository.findAll();
        int notificationsSent = 0;
        List<Map<String, Object>> notifiedStudents = new ArrayList<>();
        
        for (Student student : students) {
            List<AttendancePercentageDTO> attendancePercentages = getStudentAttendancePercentageByCourses(student.getId(), null);
            
            for (AttendancePercentageDTO dto : attendancePercentages) {
                if (dto.getIsBelowThreshold()) {
                    // Get course and faculty
                    Course course = courseRepository.findById(dto.getCourseId())
                            .orElseThrow(() -> new RuntimeException("Course not found"));
                    
                    Faculty faculty = facultyRepository.findById(dto.getFacultyId())
                            .orElseThrow(() -> new RuntimeException("Faculty not found"));
                    
                    // Send notification
                    sendLowAttendanceNotification(student, course, faculty, dto.getAttendancePercentage());
                    notificationsSent++;
                    
                    Map<String, Object> notifiedInfo = new HashMap<>();
                    notifiedInfo.put("studentId", student.getId());
                    notifiedInfo.put("studentName", student.getName());
                    notifiedInfo.put("courseCode", course.getCode());
                    notifiedInfo.put("percentage", dto.getAttendancePercentage());
                    notifiedStudents.add(notifiedInfo);
                }
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("notificationsSent", notificationsSent);
        result.put("notifiedStudents", notifiedStudents);
        
        return result;
    }

    // Helper methods for cell value extraction
    private boolean isRowEmpty(Row row) {
        if (row == null) {
            return true;
        }
        if (row.getCell(0) == null) {
            return true;
        }
        return false;
    }

    private String getStringCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        
        DataFormatter formatter = new DataFormatter();
        return formatter.formatCellValue(cell);
    }

    private Long getLongCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        
        try {
            DataFormatter formatter = new DataFormatter();
            String value = formatter.formatCellValue(cell);
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer getIntegerCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        
        try {
            DataFormatter formatter = new DataFormatter();
            String value = formatter.formatCellValue(cell);
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // Convert entity to DTO
    private AttendanceDTO convertToDTO(Attendance attendance) {
        AttendanceDTO dto = new AttendanceDTO();
        
        dto.setId(attendance.getId());
        dto.setStudentId(attendance.getStudent().getId());
        dto.setStudentName(attendance.getStudent().getName());
        dto.setStudentDno(attendance.getStudent().getDno());
        dto.setDepartment(attendance.getStudent().getDepartment());
        dto.setSection(attendance.getStudent().getSection());
        dto.setCourseId(attendance.getCourse().getId());
        dto.setCourseCode(attendance.getCourse().getCode());
        dto.setCourseTitle(attendance.getCourse().getTitle());
        dto.setFacultyId(attendance.getFaculty().getId());
        dto.setFacultyName(attendance.getFaculty().getName());
        dto.setBatchName(attendance.getBatchName());
        dto.setSemesterNo(attendance.getSemesterNo());
        dto.setTotalPeriods(attendance.getTotalPeriods());
        dto.setPeriodsAttended(attendance.getPeriodsAttended());
        dto.setAttendancePercentage(attendance.getAttendancePercentage());
        dto.setDate(attendance.getDate());
        
        return dto;
    }
}