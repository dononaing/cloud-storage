package me.cema.cloud_storage.configuration.property;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "minio")
@Data
public class MinioProperties {
    @NotBlank
    private final String url;
    @NotBlank
    private final String username;
    @NotBlank
    private final String password;
}
