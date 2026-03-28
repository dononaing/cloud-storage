package me.cema.cloud_storage.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.cema.cloud_storage.dto.user.UserRegistrationResponse;
import me.cema.cloud_storage.dto.user.UserRequest;
import me.cema.cloud_storage.services.RegistrationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Controller
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;

    @PostMapping("/auth/sign-up")
    public ResponseEntity<UserRegistrationResponse> signUp(@RequestBody @Valid UserRequest credentials,
                                                           HttpServletRequest request) {
        UserRegistrationResponse response = registrationService.save(credentials, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    @GetMapping("/user/me")
    public ResponseEntity<UserRegistrationResponse> currentUser() {
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new UserRegistrationResponse(SecurityContextHolder.getContext().getAuthentication().getName()));

    }

}
