package me.cema.cloud_storage.configuration.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "minio")
@Data
public class MinioProperties {
    private final String url;
    private final String username;
    private final String password;

}
