package me.cema.cloud_storage.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserRequest {
    @NotBlank(message = "please enter the username")
    @Size(min = 5, max = 20, message = "name must be between 5 and 20 characters")
    private String username;
    @NotBlank(message = "please enter the password")
    @Size(min = 5, max = 20, message = "password must be between 5 and 20 characters")
    private String password;
}
