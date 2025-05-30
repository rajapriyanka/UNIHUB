package com.cms.service;

import com.cms.dto.StudentProfileUpdateRequest;
import com.cms.entities.Student;
import com.cms.entities.User;
import com.cms.enums.UserRole;
import com.cms.repository.AttendanceRepository;
import com.cms.repository.StudentLeaveRepository;
import com.cms.repository.StudentRepository;
import com.cms.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class StudentService {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ExcelService excelService;
    
    @Autowired
    private StudentLeaveRepository studentLeaveRepository;
    
    @Autowired
    private AttendanceRepository attendanceRepository;
    
    /**
     * Get student by email
     */
    public Student getStudentByEmail(String email) {
        if (Objects.isNull(email) || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email must not be null or empty");
        }

        return studentRepository.findByUserEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found with email: " + email));
    }
    
    /**
     * Check if student exists by email
     */
    public boolean studentExistsByEmail(String email) {
        if (Objects.isNull(email) || email.isBlank()) {
            return false;
        }
        
        return studentRepository.findByUserEmail(email).isPresent();
    }
    
    /**
     * Update student profile
     */
    @Transactional
    public Student updateStudentProfile(String email, StudentProfileUpdateRequest request) {
        Student student = getStudentByEmail(email);
        
        if (request.getName() != null && !request.getName().isEmpty()) {
            student.setName(request.getName());
        }
        
        if (request.getMobileNumber() != null && !request.getMobileNumber().isEmpty()) {
            student.setMobileNumber(request.getMobileNumber());
        }
        
        return studentRepository.save(student);
    }

    public List<Student> getAllStudents() {
        return studentRepository.findAllWithUsers();
    }

    public List<Student> searchStudentsByName(String name) {
        return studentRepository.findByNameContainingIgnoreCase(name);
    }

    /**
     * Create or update a student
     * If a student with the given email already exists, update it
     * Otherwise, create a new student
     */
    @Transactional
    public Student createOrUpdateStudent(Student student) {
        if (student.getUser() == null || student.getUser().getEmail() == null || student.getUser().getPassword() == null) {
            throw new IllegalArgumentException("User, email, and password are required for student registration");
        }
        
        // Check if a student with this email already exists
        Optional<User> existingUserOpt = userRepository.findFirstByEmail(student.getUser().getEmail());
        
        if (existingUserOpt.isPresent()) {
            // User exists, update the student information
            User existingUser = existingUserOpt.get();
            
            // Find the associated student
            Optional<Student> existingStudentOpt = studentRepository.findByUserEmail(existingUser.getEmail());
            
            if (existingStudentOpt.isPresent()) {
                Student existingStudent = existingStudentOpt.get();
                
                // Update student details
                existingStudent.setName(student.getName());
                existingStudent.setDno(student.getDno());
                existingStudent.setDepartment(student.getDepartment());
                existingStudent.setBatchName(student.getBatchName());
                existingStudent.setMobileNumber(student.getMobileNumber());
                existingStudent.setSection(student.getSection());
                
                // Update user details
                existingUser.setName(student.getName());
                
                // Only update password if it's provided and different
                if (student.getUser().getPassword() != null && !student.getUser().getPassword().isEmpty() && 
                    !passwordEncoder.matches(student.getUser().getPassword(), existingUser.getPassword())) {
                    existingUser.setPassword(passwordEncoder.encode(student.getUser().getPassword()));
                }
                
                userRepository.save(existingUser);
                return studentRepository.save(existingStudent);
            } else {
                // This should not happen normally, but handle it by creating a new student
                return registerNewStudent(student);
            }
        } else {
            // Create new student
            return registerNewStudent(student);
        }
    }

    /**
     * Register a new student (internal method)
     */
    private Student registerNewStudent(Student student) {
        User user = new User();
        user.setName(student.getName());
        user.setEmail(student.getUser().getEmail());
        user.setPassword(passwordEncoder.encode(student.getUser().getPassword()));
        user.setUserRole(UserRole.STUDENT);

        user = userRepository.save(user);

        student.setUser(user);
        student.setMobileNumber(student.getMobileNumber());  // Ensure mobile number is set
        return studentRepository.save(student);
    }

    /**
     * Register a student (public method that handles duplicates)
     */
    @Transactional
    public Student registerStudent(Student student) {
        return createOrUpdateStudent(student);
    }

    @Transactional
    public Student updateStudent(Long id, Student updatedStudent) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        student.setName(updatedStudent.getName());
        student.setDno(updatedStudent.getDno());
        student.setDepartment(updatedStudent.getDepartment());
        student.setBatchName(updatedStudent.getBatchName());
        student.setMobileNumber(updatedStudent.getMobileNumber());
        student.setSection(updatedStudent.getSection());

        User user = student.getUser();
        user.setName(updatedStudent.getName());
        
        if (updatedStudent.getUser() != null) {
            if (updatedStudent.getUser().getEmail() != null) {
                user.setEmail(updatedStudent.getUser().getEmail());
            }
            
            if (updatedStudent.getUser().getPassword() != null) {
                user.setPassword(passwordEncoder.encode(updatedStudent.getUser().getPassword()));
            }
        }

        userRepository.save(user);
        return studentRepository.save(student);
    }

    @Transactional
    public void deleteStudent(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        User user = student.getUser();

        // Step 1: Delete student leave records
        studentLeaveRepository.deleteByStudentId(id);

        // Step 2: Delete attendance records
        attendanceRepository.deleteByStudentId(id);

        // Step 3: Break relationship before deleting student
        student.setUser(null);
        studentRepository.save(student);

        // Step 4: Delete student record
        studentRepository.delete(student);

        // Step 5: Delete user record (if exists)
        if (user != null) {
            userRepository.delete(user);
        }
    }

    @Transactional
    public List<Student> registerStudentsFromExcel(MultipartFile file) throws IOException {
        // Extract student data from the Excel file
        List<Student> students = excelService.extractStudentsFromExcel(file);
        List<Student> registeredStudents = new ArrayList<>();

        for (Student student : students) {
            // Use the createOrUpdateStudent method to handle duplicates
            registeredStudents.add(createOrUpdateStudent(student));
        }

        return registeredStudents;
    }
    
    public List<Student> filterStudents(String department, String batchName) {
        return studentRepository.findAllWithUsers().stream()
                .filter(student -> {
                    boolean departmentMatch = (department == null || department.isEmpty()) || student.getDepartment().equalsIgnoreCase(department);
                    boolean batchMatch = (batchName == null || batchName.isEmpty()) || student.getBatchName().equalsIgnoreCase(batchName);
                    return departmentMatch && batchMatch;
                })
                .toList(); // Collect the filtered students into a list
    }
    
    public List<Student> getFilteredStudents(String department, String batchName) {
        return filterStudents(department, batchName);
    }
}