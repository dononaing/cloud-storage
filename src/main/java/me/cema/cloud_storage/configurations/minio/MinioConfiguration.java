package me.cema.cloud_storage.configurations.minio;

import io.minio.MinioClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfiguration {
    private final static String URL = "http://localhost:9000";
    private final static String USERNAME = "minioadmin";
    private final static String PASSWORD = "minioadmin";

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(URL)
                .credentials(USERNAME, PASSWORD)
                .build();
    }
}
