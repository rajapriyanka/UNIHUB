package com.cms.service.admin;

import com.cms.dto.AuthenticationRequest;
import com.cms.dto.AuthenticationResponse;
import com.cms.dto.FacultyRegistrationRequest;
import com.cms.dto.AdminRegistrationRequest;
import com.cms.entities.Faculty;
import com.cms.entities.Student;
import com.cms.entities.User;
import com.cms.enums.UserRole;
import com.cms.repository.FacultyRepository;
import com.cms.repository.StudentRepository;
import com.cms.repository.UserRepository;
import com.cms.utils.JwtUtil;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminServiceImpl {

    private static final Logger logger = LoggerFactory.getLogger(AdminServiceImpl.class);

    private final UserRepository userRepository;
    private final FacultyRepository facultyRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @Value("${admin.default.email}")
    private String defaultAdminEmail;

    @Value("${admin.default.password}")
    private String defaultAdminPassword;

    public AdminServiceImpl(UserRepository userRepository,
                            FacultyRepository facultyRepository,
                            StudentRepository studentRepository,
                            PasswordEncoder passwordEncoder,
                            AuthenticationManager authenticationManager,
                            JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.facultyRepository = facultyRepository;
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    @PostConstruct
    @Transactional
    public void initDefaultAdmin() {
        if (userRepository.findFirstByEmail(defaultAdminEmail).isEmpty()) {
            User admin = new User();
            admin.setName("Default Admin");
            admin.setEmail(defaultAdminEmail);
            admin.setPassword(passwordEncoder.encode(defaultAdminPassword));
            admin.setUserRole(UserRole.ADMIN);
            userRepository.save(admin);
            logger.info("Default admin user created with email: {}", defaultAdminEmail);
        } else {
            logger.info("Default admin user already exists");
        }
    }

    @Transactional
    public User registerFaculty(FacultyRegistrationRequest request) {
        // Check if email already exists
        if (userRepository.findFirstByEmail(request.getEmail()).isPresent()) {
            logger.error("Email already in use: {}", request.getEmail());
            throw new RuntimeException("Email already in use");
        }
        
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getMobileNo())); // Using mobile number as initial password
        user.setUserRole(UserRole.FACULTY);

        Faculty faculty = new Faculty();
        faculty.setName(request.getName());
        faculty.setDepartment(request.getDepartment());
        faculty.setDesignation(request.getDesignation());
        faculty.setMobileNo(request.getMobileNo());
        faculty.setUser(user);

        user.setFaculty(faculty);

        user = userRepository.save(user);
        facultyRepository.save(faculty);
        
        logger.info("Faculty registered successfully with email: {}", request.getEmail());
        return user;
    }

    public AuthenticationResponse login(AuthenticationRequest request) {
        logger.info("Attempting login for user: {}", request.getEmail());
        
        // First check if user exists
        User user = userRepository.findFirstByEmail(request.getEmail())
            .orElseThrow(() -> {
                logger.error("User not found: {}", request.getEmail());
                return new RuntimeException("Invalid username or password");
            });
        
        logger.info("User found in database. UserRole: {}", user.getUserRole());
        
        // Check if password is properly stored
        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            logger.error("Password not set for user: {}", request.getEmail());
            throw new RuntimeException("Account setup incomplete. Please contact administrator.");
        }
        
        logger.info("Password field is {}", 
            (user.getPassword() == null ? "NULL" : 
             user.getPassword().isEmpty() ? "EMPTY" : "POPULATED"));
        
        // Verify password directly first for debugging
        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), user.getPassword());
        logger.info("Password match result: {}", passwordMatches);
        
        if (!passwordMatches) {
            logger.error("Password mismatch for user: {}", request.getEmail());
            throw new RuntimeException("Invalid username or password");
        }
        
        try {
            // Now use the authentication manager
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            final UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            final String jwt = jwtUtil.generateToken(userDetails);

            // Determine entity ID based on role
            Long entityId = null;
            if (user.getUserRole() == UserRole.FACULTY && user.getFaculty() != null) {
                entityId = user.getFaculty().getId();
            } else if (user.getUserRole() == UserRole.STUDENT && user.getStudent() != null) {
                entityId = user.getStudent().getId();
            }

            logger.info("Login successful for user: {}", request.getEmail());
            return new AuthenticationResponse(jwt, user.getUserRole(), entityId);
        } catch (BadCredentialsException e) {
            logger.error("Authentication manager rejected credentials for user: {}", request.getEmail());
            throw new RuntimeException("Invalid username or password", e);
        } catch (Exception e) {
            logger.error("Login failed for user: {}", request.getEmail(), e);
            throw new RuntimeException("Login failed: " + e.getMessage(), e);
        }
    }

    public AuthenticationResponse adminLogin(AuthenticationRequest request) {
        logger.info("Attempting admin login for user: {}", request.getEmail());
        
        // Check if user exists
        User user = userRepository.findFirstByEmail(request.getEmail())
            .orElseThrow(() -> {
                logger.error("User not found: {}", request.getEmail());
                return new RuntimeException("Invalid username or password");
            });
        
        // Check if user is admin
        if (user.getUserRole() != UserRole.ADMIN) {
            logger.error("Non-admin user attempted to log in as admin: {}", request.getEmail());
            throw new RuntimeException("Invalid username or password");
        }
        
        logger.info("Admin user found in database");
        
        // Check if password is properly stored
        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            logger.error("Password not set for admin: {}", request.getEmail());
            throw new RuntimeException("Account setup incomplete. Please contact system administrator.");
        }
        
        // Verify password directly first for debugging
        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), user.getPassword());
        logger.info("Password match result: {}", passwordMatches);
        
        if (!passwordMatches) {
            logger.error("Password mismatch for admin: {}", request.getEmail());
            throw new RuntimeException("Invalid username or password");
        }
        
        try {
            // Now use the authentication manager
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            final UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            final String jwt = jwtUtil.generateToken(userDetails);

            logger.info("Admin login successful for user: {}", request.getEmail());
            return new AuthenticationResponse(jwt, user.getUserRole(), null);
        } catch (BadCredentialsException e) {
            logger.error("Authentication manager rejected credentials for admin: {}", request.getEmail());
            throw new RuntimeException("Invalid username or password", e);
        } catch (Exception e) {
            logger.error("Admin login failed for user: {}", request.getEmail(), e);
            throw new RuntimeException("Login failed: " + e.getMessage(), e);
        }
    }

    public AuthenticationResponse facultyLogin(AuthenticationRequest request) {
        logger.info("Attempting faculty login for user: {}", request.getEmail());
        
        // Check if user exists
        User user = userRepository.findFirstByEmail(request.getEmail())
            .orElseThrow(() -> {
                logger.error("User not found: {}", request.getEmail());
                return new RuntimeException("Invalid username or password");
            });
        
        // Check if user is faculty
        if (user.getUserRole() != UserRole.FACULTY) {
            logger.error("Non-faculty user attempted to log in as faculty: {}", request.getEmail());
            throw new RuntimeException("Invalid username or password");
        }
        
        // Ensure faculty record exists
        Faculty faculty = user.getFaculty();
        if (faculty == null) {
            logger.error("Faculty record not found for user: {}", request.getEmail());
            throw new RuntimeException("Account setup incomplete. Please contact administrator.");
        }
        
        // Verify password directly first for debugging
        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), user.getPassword());
        logger.info("Password match result: {}", passwordMatches);
        
        if (!passwordMatches) {
            logger.error("Password mismatch for faculty: {}", request.getEmail());
            throw new RuntimeException("Invalid username or password");
        }
        
        try {
            // Now use the authentication manager
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            final UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            final String jwt = jwtUtil.generateToken(userDetails);

            logger.info("Faculty login successful for user: {}", request.getEmail());
            return new AuthenticationResponse(jwt, user.getUserRole(), faculty.getId());
        } catch (BadCredentialsException e) {
            logger.error("Authentication manager rejected credentials for faculty: {}", request.getEmail());
            throw new RuntimeException("Invalid username or password", e);
        } catch (Exception e) {
            logger.error("Faculty login failed for user: {}", request.getEmail(), e);
            throw new RuntimeException("Login failed: " + e.getMessage(), e);
        }
    }
    
    public AuthenticationResponse studentLogin(AuthenticationRequest request) {
        logger.info("Attempting student login for user: {}", request.getEmail());

        // Check if user exists
        User user = userRepository.findFirstByEmail(request.getEmail())
            .orElseThrow(() -> {
                logger.error("User not found: {}", request.getEmail());
                return new RuntimeException("Invalid username or password");
            });

        // Check if user is student
        if (user.getUserRole() != UserRole.STUDENT) {
            logger.error("Non-student user attempted to log in as student: {}", request.getEmail());
            throw new RuntimeException("Invalid username or password");
        }

        // Ensure student record exists
        Student student = user.getStudent();
        if (student == null) {
            logger.error("Student record not found for user: {}", request.getEmail());
            throw new RuntimeException("Account setup incomplete. Please contact administrator.");
        }

        // Verify password directly first for debugging
        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), user.getPassword());
        logger.info("Password match result: {}", passwordMatches);
        
        if (!passwordMatches) {
            logger.error("Password mismatch for student: {}", request.getEmail());
            throw new RuntimeException("Invalid username or password");
        }

        try {
            // Now use the authentication manager
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            final UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            final String jwt = jwtUtil.generateToken(userDetails);

            logger.info("Student login successful for user: {}", request.getEmail());
            return new AuthenticationResponse(jwt, user.getUserRole(), student.getId());
        } catch (BadCredentialsException e) {
            logger.error("Authentication manager rejected credentials for student: {}", request.getEmail());
            throw new RuntimeException("Invalid username or password", e);
        } catch (Exception e) {
            logger.error("Student login failed for user: {}", request.getEmail(), e);
            throw new RuntimeException("Login failed: " + e.getMessage(), e);
        }
    }
    
    @Transactional
    public User registerAdmin(AdminRegistrationRequest request) {
        logger.info("Attempting to register admin: {}", request.getEmail());
    
        if (userRepository.findFirstByEmail(request.getEmail()).isPresent()) {
            logger.error("Email already in use: {}", request.getEmail());
            throw new RuntimeException("Email already in use");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setUserRole(UserRole.ADMIN);

        user = userRepository.save(user);
        logger.info("Admin registered successfully with ID: {} and email: {}", user.getId(), user.getEmail());

        return user;
    }

    public boolean defaultAdminExists() {
        return userRepository.findFirstByEmail(defaultAdminEmail).isPresent();
    }
}