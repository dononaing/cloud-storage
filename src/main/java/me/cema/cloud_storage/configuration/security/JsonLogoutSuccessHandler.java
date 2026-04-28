package me.cema.cloud_storage.configuration.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import me.cema.cloud_storage.dto.user.UserExceptionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JsonLogoutSuccessHandler implements LogoutSuccessHandler {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        response.setContentType("application/json");
        if (authentication == null || !authentication.isAuthenticated()) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write(objectMapper.writeValueAsString(new UserExceptionResponse("unauthorized")));
        } else {
            response.setStatus(HttpStatus.NO_CONTENT.value());
        }
    }
}
