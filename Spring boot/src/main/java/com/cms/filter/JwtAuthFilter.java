package com.cms.filter;

import com.cms.service.jwt.UserDetailsServiceImpl;
import com.cms.utils.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");
        logger.info("Processing request for URL: {}", request.getRequestURL());

        String username = null;
        String jwt = null;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7).trim();

            if (jwt.isEmpty()) {
                logger.error("JWT token is empty.");
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "JWT token is missing or empty.");
                return;
            }

            try {
                username = jwtUtil.extractUsername(jwt);
                logger.info("Extracted username from JWT: {}", username);
            } catch (Exception e) {
                logger.error("Invalid JWT token format or signature.", e);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT token.");
                return;
            }
        } else {
            logger.warn("Missing or improperly formatted Authorization header.");
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

            // Validate token including credential version check
            if (jwtUtil.validateToken(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                logger.info("Authentication successful for user: {}", username);
            } else {
                logger.warn("Invalid JWT token for user: {}", username);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT token or credentials have changed.");
                return;
            }
        }

        chain.doFilter(request, response);
    }
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // Skip JWT authentication for login requests
        return path.startsWith("/api/email-actions/") || 
               path.equals("/api/student/login") || 
               path.equals("/api/faculty/login");
    }
}