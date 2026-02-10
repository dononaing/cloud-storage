package me.cema.cloud_storage.models.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRequestDTO {
    @NotBlank(message = "please enter the username")
    @Size(min = 3, max = 50, message = "name must be between 3 and 50 characters")
    private String username;
    @NotBlank(message = "please enter the password")
    @Size(min = 8, max = 255, message = "password must be between 8 and 255 characters")
    private String password;
}
