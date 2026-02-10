package me.cema.cloud_storage.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.cema.cloud_storage.models.user.dto.UserAuthenticationResponseDTO;
import me.cema.cloud_storage.models.user.dto.UserRegistrationResponseDTO;
import me.cema.cloud_storage.models.user.dto.UserRequestDTO;
import me.cema.cloud_storage.services.AuthenticationService;
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

    private final AuthenticationService authenticationService;

    @PostMapping("/sign-up")
    public ResponseEntity<UserRegistrationResponseDTO> signUp(@RequestBody @Valid UserRequestDTO credentials,
                                                              HttpServletRequest request) {
        UserRegistrationResponseDTO response = authenticationService.save(credentials, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    @PostMapping("/sign-in")
    public ResponseEntity<UserAuthenticationResponseDTO> singIn(@RequestBody @Valid UserRequestDTO credentials,
                                                                HttpServletRequest request) {

        UserAuthenticationResponseDTO response = authenticationService.checkAndAuthenticate(credentials, request);

        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

}
