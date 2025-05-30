package com.cms.controller;

import com.cms.dto.AuthenticationRequest;
import com.cms.dto.AuthenticationResponse;
import com.cms.entities.Faculty;
import com.cms.entities.User;
import com.cms.repository.FacultyRepository;
import com.cms.repository.UserRepository;
import com.cms.service.jwt.UserDetailsServiceImpl;
import com.cms.utils.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthenticationController {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationController.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private FacultyRepository facultyRepository;

    @Autowired
    private UserRepository userRepository;

    @CrossOrigin(origins = "http://localhost:3000")
    @PostMapping("/authenticate")
    public ResponseEntity<?> createAuthenticationToken(@RequestBody AuthenticationRequest authenticationRequest) {
        try {
            logger.info("Attempting authentication for user: {}", authenticationRequest.getEmail());

            // Authenticate the user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authenticationRequest.getEmail(),
                            authenticationRequest.getPassword()
                    )
            );

            if (!authentication.isAuthenticated()) {
                logger.error("Authentication failed for user: {}", authenticationRequest.getEmail());
                return ResponseEntity.badRequest().body("Authentication failed");
            }

            // Load user details
            final UserDetails userDetails = userDetailsService.loadUserByUsername(authenticationRequest.getEmail());
            User user = userRepository.findFirstByEmail(authenticationRequest.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Faculty faculty = facultyRepository.findByUser(user).orElse(null);
            Long facultyId = (faculty != null) ? faculty.getId() : null;

            final String token = jwtUtil.generateToken(userDetails);
            logger.info("Token generated for user: {}, Role: {}, Faculty ID: {}",
                    authenticationRequest.getEmail(), user.getUserRole(), facultyId);

            // Return the response with facultyId if the user is a faculty
            return ResponseEntity.ok(new AuthenticationResponse(token, user.getUserRole(), facultyId));

        } catch (Exception e) {
            logger.error("Authentication error: ", e);
            return ResponseEntity.badRequest().body("Authentication error: " + e.getMessage());
        }
    }
}
