package com.cms.service;

import com.cms.dto.FacultyRegistrationRequest;
import com.cms.entities.Student;
import com.cms.entities.User;
import com.cms.entities.Faculty;
import com.cms.entities.Course;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Service
public class ExcelService {

    @Autowired
    private FacultyService facultyService;

    public List<Student> extractStudentsFromExcel(MultipartFile file) throws IOException {
        List<Student> students = new ArrayList<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            // Skip the header row
            if (rows.hasNext()) {
                rows.next();
            }

            while (rows.hasNext()) {
                Row currentRow = rows.next();
                Student student = new Student();
                User user = new User();

                // Extract user info
                user.setEmail(getCellValueAsString(currentRow.getCell(0)));
                user.setPassword(getCellValueAsString(currentRow.getCell(1)));
                student.setUser(user);

                // Extract student details
                student.setName(getCellValueAsString(currentRow.getCell(2)));
                student.setDno(getCellValueAsString(currentRow.getCell(3)));
                student.setDepartment(getCellValueAsString(currentRow.getCell(4)));
                student.setBatchName(getCellValueAsString(currentRow.getCell(5)));
                student.setSection(getCellValueAsString(currentRow.getCell(6)));

                // Extract mobile number and ensure it is treated as a string
                String mobileNumber = getCellValueAsString(currentRow.getCell(7)); // Assume column 6 is for mobile number
                student.setMobileNumber(mobileNumber); // Ensure it's set as a string

                students.add(student);
            }
        }

        return students;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        // Use DataFormatter to get the cell's value as a string, correctly handling numeric cells
        DataFormatter dataFormatter = new DataFormatter();
        return dataFormatter.formatCellValue(cell);
    }

    public List<FacultyRegistrationRequest> extractFacultyFromExcel(MultipartFile file) throws IOException {
        List<FacultyRegistrationRequest> facultyRequests = new ArrayList<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            // Skip the header row
            if (rows.hasNext()) {
                rows.next();
            }

            while (rows.hasNext()) {
                Row currentRow = rows.next();
                
                String email = getCellValueAsString(currentRow.getCell(0));
                String password = getCellValueAsString(currentRow.getCell(1));
                String name = getCellValueAsString(currentRow.getCell(2));
                String department = getCellValueAsString(currentRow.getCell(3));
                String designation = getCellValueAsString(currentRow.getCell(4));
                String mobileNo = getCellValueAsString(currentRow.getCell(5));
                
                FacultyRegistrationRequest request = new FacultyRegistrationRequest(
                    name, email, password, department, designation, mobileNo
                );
                
                facultyRequests.add(request);
            }
        }

        return facultyRequests;
    }
    
    public List<Course> extractCoursesFromExcel(MultipartFile file) throws IOException {
        List<Course> courses = new ArrayList<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            // Skip the header row
            if (rows.hasNext()) {
                rows.next();
            }

            while (rows.hasNext()) {
                Row currentRow = rows.next();
                Course course = new Course();

                // Extract course details
                course.setTitle(getCellValueAsString(currentRow.getCell(0)));
                course.setCode(getCellValueAsString(currentRow.getCell(1)));
                
                // Handle numeric values
                String contactPeriodsStr = getCellValueAsString(currentRow.getCell(2));
                if (!contactPeriodsStr.isEmpty()) {
                    course.setContactPeriods(Integer.parseInt(contactPeriodsStr));
                }
                
                String semesterNoStr = getCellValueAsString(currentRow.getCell(3));
                if (!semesterNoStr.isEmpty()) {
                    course.setSemesterNo(Integer.parseInt(semesterNoStr));
                }
                
                course.setDepartment(getCellValueAsString(currentRow.getCell(4)));
                
                // Handle course type enum
                String courseTypeStr = getCellValueAsString(currentRow.getCell(5));
                if (!courseTypeStr.isEmpty()) {
                    try {
                        course.setType(Course.CourseType.valueOf(courseTypeStr.toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        // Default to ACADEMIC if invalid type is provided
                        course.setType(Course.CourseType.ACADEMIC);
                    }
                }

                courses.add(course);
            }
        }

        return courses;
    }
}
