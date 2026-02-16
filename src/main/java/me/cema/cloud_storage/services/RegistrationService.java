package me.cema.cloud_storage.services;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import me.cema.cloud_storage.dto.UserRegistrationResponse;
import me.cema.cloud_storage.dto.UserRequest;
import me.cema.cloud_storage.models.user.User;
import me.cema.cloud_storage.repositories.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;


@Service
@RequiredArgsConstructor
public class RegistrationService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserRegistrationResponse save(UserRequest credentials, HttpServletRequest request) {
        if (userRepository.findByUsername(credentials.getUsername()).isPresent()) {
            throw new HttpClientErrorException(HttpStatus.CONFLICT, "username already exists");
        }

        User user = new User(
                null,
                credentials.getUsername(),
                passwordEncoder.encode(credentials.getPassword())
        );
        userRepository.save(user);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getUsername(),
                null,
                user.getAuthorities()
        );
        createSession(authentication, request);

        return new UserRegistrationResponse(credentials.getUsername());
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
