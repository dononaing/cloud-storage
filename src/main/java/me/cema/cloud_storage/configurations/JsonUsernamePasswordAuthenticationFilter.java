package me.cema.cloud_storage.configurations;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import me.cema.cloud_storage.dto.UserAuthenticationResponse;
import me.cema.cloud_storage.dto.UserExceptionResponse;
import me.cema.cloud_storage.dto.UserRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import java.io.IOException;


public class JsonUsernamePasswordAuthenticationFilter extends UsernamePasswordAuthenticationFilter {
    private final ObjectMapper objectMapper;

    public JsonUsernamePasswordAuthenticationFilter(AuthenticationManager authenticationManager, ObjectMapper objectMapper) {
        super(authenticationManager);
        this.objectMapper = objectMapper;
        setFilterProcessesUrl("/auth/sign-in");

        setAuthenticationFailureHandler((request, response, exception) -> {
            response.setStatus(403);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(new UserExceptionResponse(exception.getMessage())));
        });

        setAuthenticationSuccessHandler((request, response, authentication) -> {
            response.setStatus(200);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            UserAuthenticationResponse userAuthenticationResponse = new UserAuthenticationResponse(authentication.getName());
            response.getWriter().write(objectMapper.writeValueAsString(userAuthenticationResponse));
        });
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        if (!MediaType.APPLICATION_JSON_VALUE.equals(request.getContentType())) {
            throw new BadCredentialsException("accept only json");
        }
        UserRequest credentials;
        try {
            credentials = objectMapper.readValue(request.getInputStream(), UserRequest.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                credentials.getUsername(),
                credentials.getPassword()
        );
        Authentication authenticated = SecurityContextHolder.getContext().getAuthentication();
        if (authenticated != null && authenticated.isAuthenticated()) {
            throw new AuthenticationServiceException("user already authenticated");
        }
        Authentication authentication = getAuthenticationManager().authenticate(token);
        setDetails(request, token);
        createSession(authentication, request);
        return authentication;
    }

    private static void createSession(Authentication authentication, HttpServletRequest request) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        request.getSession().invalidate();
        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    }
}
