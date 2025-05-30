package com.cms.service;

import com.cms.dto.FacultyProfileUpdateRequest;
import com.cms.dto.FacultyRegistrationRequest;
import com.cms.dto.FacultyUpdateRequest;
import com.cms.entities.Faculty;
import com.cms.entities.User;
import com.cms.enums.UserRole;
import com.cms.repository.FacultyRepository;
import com.cms.repository.LeaveRepository;
import com.cms.repository.LeaveRequestRepository;
import com.cms.repository.StudentLeaveRepository;
import com.cms.repository.SubstituteRequestRepository;
import com.cms.repository.AttendanceRepository;
import com.cms.repository.FacultyCourseRepository;
import com.cms.repository.TimetableEntryRepository;
import com.cms.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class FacultyService {

    @Autowired
    private FacultyRepository facultyRepository;
    
    @Autowired
    private StudentLeaveRepository studentLeaveRepository;
    
    @Autowired
    private AttendanceRepository attendanceRepository;

    
    @Autowired
    private SubstituteRequestRepository substituteRequestRepository;

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private TimetableEntryRepository timetableEntryRepository;
    
    @Autowired
    private FacultyCourseRepository facultyCourseRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired 
    private LeaveRepository leaveRepository;
    
    @Autowired 
    private LeaveRequestRepository leaveRequestRepository;
    
    
    

    public List<Faculty> getAllFaculty() {
        return facultyRepository.findAll();
    }

    public List<Faculty> searchFacultyByName(String name) {
        return facultyRepository.findByNameContainingIgnoreCase(name);
    }

    @Transactional
    public Faculty registerFaculty(FacultyRegistrationRequest request) {
        // Check if a user with this email already exists
        Optional<User> existingUser = userRepository.findFirstByEmail(request.getEmail());
        
        if (existingUser.isPresent()) {
            // User exists, update the faculty information
            User user = existingUser.get();
            
            // Find the associated faculty
            Faculty faculty = facultyRepository.findByUser(user)
                    .orElseThrow(() -> new RuntimeException("Faculty not found for existing user"));
            
            // Update faculty details
            faculty.setName(request.getName());
            faculty.setDepartment(request.getDepartment());
            faculty.setDesignation(request.getDesignation());
            faculty.setMobileNo(request.getMobileNo());
            
            // Update user details
            user.setName(request.getName());
            // Only update password if it's different
            if (!passwordEncoder.matches(request.getMobileNo(), user.getPassword())) {
                user.setPassword(passwordEncoder.encode(request.getMobileNo()));
            }
            
            userRepository.save(user);
            return facultyRepository.save(faculty);
        } else {
            // Create new user and faculty
            User user = new User();
            user.setName(request.getName());
            user.setEmail(request.getEmail());
            user.setPassword(passwordEncoder.encode(request.getMobileNo()));
            user.setUserRole(UserRole.FACULTY);

            Faculty faculty = new Faculty();
            faculty.setName(request.getName());
            faculty.setDepartment(request.getDepartment());
            faculty.setDesignation(request.getDesignation());
            faculty.setMobileNo(request.getMobileNo());
            faculty.setUser(user);

            user.setFaculty(faculty);

            userRepository.save(user);
            return facultyRepository.save(faculty);
        }
    }

    @Transactional
    public Faculty updateFacultyByUserId(Long userId, FacultyUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Faculty faculty = facultyRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Faculty not found for the given User ID"));

        faculty.setName(request.getName());
        faculty.setDepartment(request.getDepartment());
        faculty.setDesignation(request.getDesignation());
        faculty.setMobileNo(request.getMobileNo());

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getMobileNo()));

        userRepository.save(user);
        return facultyRepository.save(faculty);
    }

    @Transactional
    public void deleteFacultyByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Faculty faculty = facultyRepository.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Faculty not found for the given User ID"));

        // Step 1: Remove faculty as an approver (prevents foreign key constraint violation)
        leaveRepository.removeFacultyAsApprover(faculty.getId());

        // Step 2: Delete all leave records associated with the faculty
        leaveRepository.deleteByFaculty(faculty);
        leaveRequestRepository.deleteByFaculty(faculty);
        studentLeaveRepository.deleteByFaculty(faculty);

        // Step 3: Delete substitute requests where faculty is either requester or substitute
        substituteRequestRepository.deleteByRequesterOrSubstitute(faculty, faculty);

        // Step 4: Delete attendance records for the faculty (NEW STEP)
        attendanceRepository.deleteByFacultyId(faculty.getId());

        // Step 5: Delete related timetable entries
        timetableEntryRepository.deleteByFaculty(faculty);

        // Step 6: Delete faculty record
        facultyRepository.delete(faculty);

        // Step 7: Delete associated user record
        userRepository.delete(user);
    }


    /**
     * Get faculty by email
     */
    public Faculty getFacultyByEmail(String email) {
        User user = userRepository.findFirstByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
                
        return facultyRepository.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Faculty not found for the given email"));
    }
    
    /**
     * Update faculty profile by email
     */
    @Transactional
    public Faculty updateFacultyProfile(String email, FacultyProfileUpdateRequest request) {
        User user = userRepository.findFirstByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
                
        Faculty faculty = facultyRepository.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Faculty not found for the given email"));
        
        // Update faculty details
        faculty.setName(request.getName());
        faculty.setDepartment(request.getDepartment());
        faculty.setDesignation(request.getDesignation());
        faculty.setMobileNo(request.getMobileNo());
        
        // Update user name to keep it in sync
        user.setName(request.getName());
        
        userRepository.save(user);
        return facultyRepository.save(faculty);
    }
    
    /**
     * Check if faculty exists by email and update if it does
     */
    @Transactional
    public Faculty createOrUpdateFaculty(FacultyRegistrationRequest request) {
        return registerFaculty(request); // Using the updated registerFaculty method that handles duplicates
    }
}
