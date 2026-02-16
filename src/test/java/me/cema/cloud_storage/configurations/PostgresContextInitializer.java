package me.cema.cloud_storage.configurations;

import org.jetbrains.annotations.NotNull;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
import java.util.Map;

@Testcontainers
public class PostgresContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    private final static String IMAGE_NAME = "postgres:18.1-alpine3.23";

    @Container
    public static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer(IMAGE_NAME)
            .withDatabaseName("test_cloud_storage_db")
            .withUsername("postgres")
            .withPassword("postgres");

    static void configureProperties(ConfigurableApplicationContext context) {
        Map<String, String> property = new HashMap<>();
        property.put("spring.datasource.url", postgreSQLContainer.getJdbcUrl());
        property.put("spring.datasource.username", postgreSQLContainer.getUsername());
        property.put("spring.datasource.password", postgreSQLContainer.getPassword());
        TestPropertyValues.of(property).applyTo(context.getEnvironment());

    }

    @Override
    public void initialize(@NotNull ConfigurableApplicationContext applicationcontext) {
        postgreSQLContainer.start();
        configureProperties(applicationcontext);
    }
}
