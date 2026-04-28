package me.cema.cloud_storage.configuration.minio;

import io.minio.MinioClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfiguration {
    private static final String URL = "http://localhost:9000";
    private static final String USERNAME = "minioadmin";
    private static final String PASSWORD = "minioadmin";

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(URL)
                .credentials(USERNAME, PASSWORD)
                .build();
    }
}
