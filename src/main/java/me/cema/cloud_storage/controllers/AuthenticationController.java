package me.cema.cloud_storage.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.cema.cloud_storage.dto.UserRegistrationResponse;
import me.cema.cloud_storage.dto.UserRequest;
import me.cema.cloud_storage.services.RegistrationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller()
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final RegistrationService registrationService;

    @PostMapping("/sign-up")
    public ResponseEntity<UserRegistrationResponse> signUp(@RequestBody @Valid UserRequest credentials,
                                                           HttpServletRequest request) {

        UserRegistrationResponse response = registrationService.save(credentials, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

}
