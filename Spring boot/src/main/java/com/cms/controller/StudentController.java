package com.cms.controller;

import com.cms.entities.Student;
import com.cms.service.ExcelService;
import com.cms.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    @Autowired
    private StudentService studentService;

    @Autowired
    private ExcelService excelService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllStudents() {
        List<Student> students = studentService.getAllStudents();
        
        // Transform to simplified objects to avoid recursion and ensure email is included
        List<Map<String, Object>> result = students.stream()
            .map(student -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", student.getId());
                map.put("name", student.getName());
                // Include email directly in the student object for frontend compatibility
                map.put("email", student.getUser() != null ? student.getUser().getEmail() : null);
                map.put("dno", student.getDno());
                map.put("department", student.getDepartment());
                map.put("batchName", student.getBatchName());
                map.put("mobileNumber", student.getMobileNumber());
                map.put("section", student.getSection());
                return map;
            })
            .collect(Collectors.toList());
            
        return ResponseEntity.ok(result);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/search")
    public ResponseEntity<List<Map<String, Object>>> searchStudentsByName(@RequestParam String name) {
        List<Student> students = studentService.searchStudentsByName(name);
        
        // Transform to simplified objects and ensure email is included
        List<Map<String, Object>> result = students.stream()
            .map(student -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", student.getId());
                map.put("name", student.getName());
                // Include email directly in the student object
                map.put("email", student.getUser() != null ? student.getUser().getEmail() : null);
                map.put("dno", student.getDno());
                map.put("department", student.getDepartment());
                map.put("batchName", student.getBatchName());
                map.put("mobileNumber", student.getMobileNumber());
                map.put("section", student.getSection());
                return map;
            })
            .collect(Collectors.toList());
            
        return ResponseEntity.ok(result);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<Student> registerStudent(@RequestBody Student student) {
        return ResponseEntity.ok(studentService.createOrUpdateStudent(student));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<Student> updateStudent(@PathVariable Long id, @RequestBody Student student) {
        return ResponseEntity.ok(studentService.updateStudent(id, student));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteStudent(@PathVariable Long id) {
        try {
            studentService.deleteStudent(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Student deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Failed to delete student: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/upload")
    public ResponseEntity<?> uploadStudents(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("Please select a file to upload");
            }
            
            List<Student> students = excelService.extractStudentsFromExcel(file);
            int updated = 0;
            int created = 0;
            List<String> errors = new ArrayList<>();
            
            for (Student student : students) {
                try {
                    boolean exists = studentService.studentExistsByEmail(student.getUser().getEmail());
                    Student registeredStudent = studentService.createOrUpdateStudent(student);
                    
                    if (exists) {
                        updated++;
                    } else {
                        created++;
                    }
                } catch (Exception e) {
                    errors.add("Error processing student with email " + student.getUser().getEmail() + ": " + e.getMessage());
                }
            }
            
            StringBuilder resultMessage = new StringBuilder("Students upload completed: ");
            resultMessage.append(created).append(" created, ").append(updated).append(" updated");
            
            if (!errors.isEmpty()) {
                resultMessage.append(". Errors: ").append(String.join("; ", errors));
            }
            
            return ResponseEntity.ok(resultMessage.toString());
        } catch (Exception e) {
            e.printStackTrace(); // Log the full stack trace
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error uploading students: " + e.getMessage());
        }
    }
    
    @GetMapping("/filter")
    public ResponseEntity<List<Map<String, Object>>> filterStudents(
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String batchName) {
        List<Student> filteredStudents = studentService.getFilteredStudents(department, batchName);
        
        // Transform to simplified objects and ensure email is included
        List<Map<String, Object>> result = filteredStudents.stream()
            .map(student -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", student.getId());
                map.put("name", student.getName());
                // Include email directly in the student object
                map.put("email", student.getUser() != null ? student.getUser().getEmail() : null);
                map.put("dno", student.getDno());
                map.put("department", student.getDepartment());
                map.put("batchName", student.getBatchName());
                map.put("mobileNumber", student.getMobileNumber());
                map.put("section", student.getSection());
                return map;
            })
            .collect(Collectors.toList());
            
        return ResponseEntity.ok(result);
    }
}