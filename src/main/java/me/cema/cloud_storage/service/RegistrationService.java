package me.cema.cloud_storage.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import me.cema.cloud_storage.dto.user.UserRegistrationResponse;
import me.cema.cloud_storage.dto.user.UserRequest;
import me.cema.cloud_storage.model.user.User;
import me.cema.cloud_storage.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;


@Service
@RequiredArgsConstructor
public class RegistrationService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final ResourceService resourceService;

    @Transactional
    public UserRegistrationResponse save(UserRequest credentials) {
        if (userRepository.findByUsername(credentials.getUsername()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "username already exists");
        }
        User user = new User(
                null,
                credentials.getUsername(),
                passwordEncoder.encode(credentials.getPassword())
        );
        userRepository.save(user);
        resourceService.uploadEmptyDirectory("/", user.getId());
        return new UserRegistrationResponse(credentials.getUsername());
    }
}
