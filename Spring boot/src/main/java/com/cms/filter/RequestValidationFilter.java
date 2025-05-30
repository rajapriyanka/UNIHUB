package com.cms.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestValidationFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(RequestValidationFilter.class);
    private static final Set<String> VALID_METHODS = new HashSet<>(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "TRACE", "CONNECT"));

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String method = httpRequest.getMethod();

        if (!isValidHttpMethod(method)) {
            logger.warn("Invalid HTTP method detected: {}", method);
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid HTTP method");
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isValidHttpMethod(String method) {
        return VALID_METHODS.contains(method.toUpperCase());
    }
}

