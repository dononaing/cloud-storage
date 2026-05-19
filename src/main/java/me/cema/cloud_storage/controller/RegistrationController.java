package me.cema.cloud_storage.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.cema.cloud_storage.dto.user.UserRegistrationResponse;
import me.cema.cloud_storage.dto.user.UserRequest;
import me.cema.cloud_storage.service.RegistrationService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class RegistrationController {
    private final RegistrationService registrationService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/auth/sign-up")
    public UserRegistrationResponse signUp(@RequestBody @Valid UserRequest credentials,
                                           HttpServletRequest request) throws ServletException {
        UserRegistrationResponse body = registrationService.save(credentials);
        request.login(credentials.getUsername(), credentials.getPassword());
        return body;
    }

    @PostMapping("/auth/sign-in")
    public UserRegistrationResponse singIn(@RequestBody @Valid UserRequest credentials,
                                           HttpServletRequest request) throws ServletException {
        request.logout();
        request.login(credentials.getUsername(), credentials.getPassword());
        return new UserRegistrationResponse(credentials.getUsername());

    }

    @GetMapping("/user/me")
    public UserRegistrationResponse currentUser() {
        return new UserRegistrationResponse(SecurityContextHolder.getContext().getAuthentication().getName());

    }
}
