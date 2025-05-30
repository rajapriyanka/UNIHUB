package com.cms.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.cms.entities.Leave;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class EmailTokenService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailTokenService.class);
    
    @Value("${app.token.secret:defaultSecretKey}")
    private String tokenSecret;
    
    @Value("${app.token.expiry:24}")
    private int tokenExpiryHours;
    
    private final Map<String, TokenData> tokenCache = new HashMap<>();

    private byte[] secret;
    
    private int expiration;
    
    @jakarta.annotation.PostConstruct
    public void init() {
        System.out.println("DEBUG: Initializing EmailTokenService");
        logger.info("Initializing EmailTokenService");
        
        // Initialize the secret key from the tokenSecret property
        this.secret = tokenSecret.getBytes();
        
        // Set expiration time in milliseconds (hours to milliseconds)
        this.expiration = tokenExpiryHours * 60 * 60 * 1000;
        
        System.out.println("DEBUG: EmailTokenService initialized with secret length: " + 
                          (secret != null ? secret.length : 0) + 
                          ", expiration: " + expiration + " ms");
        logger.info("EmailTokenService initialized with secret length: {}, expiration: {} ms", 
                   (secret != null ? secret.length : 0), expiration);
    }
    
    /**
     * Generate a token for substitute request approval/rejection
     */
    public String generateSubstituteToken(Long requestId, boolean approve) {
        System.out.println("DEBUG: Generating substitute token for requestId: " + requestId + ", approve: " + approve);
        logger.info("Generating substitute token for requestId: {}, approve: {}", requestId, approve);
        
        // Ensure secret is initialized
        if (secret == null) {
            System.out.println("DEBUG: Secret is null, initializing from tokenSecret");
            logger.warn("Secret key is null, initializing from tokenSecret property");
            secret = tokenSecret.getBytes();
            
            if (secret == null || secret.length == 0) {
                System.out.println("DEBUG: Failed to initialize secret key, tokenSecret is null or empty");
                logger.error("Failed to initialize secret key, tokenSecret is null or empty");
                return null;
            }
        }
        
        // Ensure expiration is initialized
        if (expiration == 0) {
            System.out.println("DEBUG: Expiration is 0, initializing from tokenExpiryHours");
            logger.warn("Expiration is 0, initializing from tokenExpiryHours property");
            expiration = tokenExpiryHours * 60 * 60 * 1000;
        }
        
        try {
            Map<String, Object> claims = new HashMap<>();
            claims.put("type", "substitute");
            claims.put("requestId", requestId);
            claims.put("approve", approve);
            
            String token = createToken(claims);
            System.out.println("DEBUG: Generated token: " + token);
            logger.info("Generated token successfully");
            return token;
        } catch (Exception e) {
            System.out.println("DEBUG: Error generating token: " + e.getMessage());
            e.printStackTrace();
            logger.error("Error generating token: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Validate a substitute token and return the request ID
     */
    public Long validateSubstituteToken(String token) {
        System.out.println("DEBUG: Validating substitute token: " + token);
        logger.info("Validating substitute token");
        
        try {
            Claims claims = getAllClaimsFromToken(token);
            
            // Check if token is expired
            if (claims.getExpiration().before(new Date())) {
                System.out.println("DEBUG: Token is expired");
                logger.warn("Token is expired");
                return null;
            }
            
            // Check if token is for substitute requests
            String type = claims.get("type", String.class);
            if (!"substitute".equals(type)) {
                System.out.println("DEBUG: Token is not for substitute requests, type: " + type);
                logger.warn("Token is not for substitute requests, type: {}", type);
                return null;
            }
            
            // Return the request ID
            Long requestId = claims.get("requestId", Long.class);
            System.out.println("DEBUG: Token validated successfully, requestId: " + requestId);
            logger.info("Token validated successfully, requestId: {}", requestId);
            return requestId;
        } catch (Exception e) {
            System.out.println("DEBUG: Error validating token: " + e.getMessage());
            e.printStackTrace();
            logger.error("Error validating token: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Check if the token is for approval (true) or rejection (false)
     */
    public Boolean isApprovalToken(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            return claims.get("approve", Boolean.class);
        } catch (Exception e) {
            logger.error("Error checking if token is for approval: {}", e.getMessage(), e);
            return null;
        }
    }

    // Helper methods
    private String createToken(Map<String, Object> claims) {
        System.out.println("DEBUG: Creating token with claims: " + claims);
        
        if (secret == null || secret.length == 0) {
            System.out.println("DEBUG: Secret key is null or empty in createToken");
            logger.error("Secret key is null or empty in createToken");
            throw new IllegalStateException("Secret key cannot be null or empty");
        }
        
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact();
    }

    private Claims getAllClaimsFromToken(String token) {
        if (secret == null || secret.length == 0) {
            System.out.println("DEBUG: Secret key is null or empty in getAllClaimsFromToken");
            logger.error("Secret key is null or empty in getAllClaimsFromToken");
            throw new IllegalStateException("Secret key cannot be null or empty");
        }
        
        return Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
    }

    private <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }
    
    public String generateToken(Long leaveId, Long approverId, Leave.LeaveStatus action) {
        try {
            System.out.println("DEBUG: Generating token for leaveId: " + leaveId + ", approverId: " + approverId + ", action: " + action);
            logger.info("Generating token for leaveId: {}, approverId: {}, action: {}", leaveId, approverId, action);
            
            String tokenData = leaveId + ":" + approverId + ":" + action + ":" + 
                               LocalDateTime.now().plusHours(tokenExpiryHours) + ":" + tokenSecret;
            
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(tokenData.getBytes());
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
            
            // Store token data in cache
            tokenCache.put(token, new TokenData(leaveId, approverId, action, 
                                               LocalDateTime.now().plusHours(tokenExpiryHours)));
            
            System.out.println("DEBUG: Generated token: " + token);
            logger.info("Generated token successfully");
            return token;
        } catch (NoSuchAlgorithmException e) {
            System.out.println("DEBUG: Error generating token: " + e.getMessage());
            e.printStackTrace();
            logger.error("Error generating token: {}", e.getMessage(), e);
            throw new RuntimeException("Error generating token", e);
        }
    }
    
    public TokenData validateToken(String token) {
        System.out.println("DEBUG: Validating token: " + token);
        logger.info("Validating token");
        
        TokenData data = tokenCache.get(token);
        
        if (data == null) {
            System.out.println("DEBUG: Token not found in cache");
            logger.warn("Token not found in cache");
            return null;
        }
        
        if (data.getExpiryTime().isBefore(LocalDateTime.now())) {
            System.out.println("DEBUG: Token is expired");
            logger.warn("Token is expired");
            tokenCache.remove(token);
            return null;
        }
        
        System.out.println("DEBUG: Token validated successfully");
        logger.info("Token validated successfully");
        return data;
    }
    
    public void invalidateToken(String token) {
        System.out.println("DEBUG: Invalidating token: " + token);
        logger.info("Invalidating token");
        tokenCache.remove(token);
    }
    
    public static class TokenData {
        private final Long leaveId;
        private final Long approverId;
        private final Leave.LeaveStatus action;
        private final LocalDateTime expiryTime;
        
        public TokenData(Long leaveId, Long approverId, Leave.LeaveStatus action, LocalDateTime expiryTime) {
            this.leaveId = leaveId;
            this.approverId = approverId;
            this.action = action;
            this.expiryTime = expiryTime;
        }
        
        public Long getLeaveId() {
            return leaveId;
        }
        
        public Long getApproverId() {
            return approverId;
        }
        
        public Leave.LeaveStatus getAction() {
            return action;
        }
        
        public LocalDateTime getExpiryTime() {
            return expiryTime;
        }
    }
}

