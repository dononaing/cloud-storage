package me.cema.cloud_storage.configuration;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.HashMap;
import java.util.Map;

public class MinioTestcontainersConfiguration implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    private static final String IMAGE_NAME = "minio/minio:latest";
    private static final String USERNAME = "minioadmin";
    private static final String PASSWORD = "minioadmin";

    static MinIOContainer minIOContainer =  new MinIOContainer(DockerImageName.parse(IMAGE_NAME))
            .withUserName(USERNAME)
            .withPassword(PASSWORD);

    static {
        minIOContainer.start();
    }

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        Map<String, String> propertyMap = new HashMap<>();
        propertyMap.put("minio.url", minIOContainer.getS3URL());
        propertyMap.put("minio.username", minIOContainer.getUserName());
        propertyMap.put("minio.password", minIOContainer.getPassword());
        TestPropertyValues.of(propertyMap).applyTo(applicationContext.getEnvironment());

    }
}
