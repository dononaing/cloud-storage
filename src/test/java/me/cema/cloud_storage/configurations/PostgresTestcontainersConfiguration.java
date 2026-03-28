package me.cema.cloud_storage.configurations;

import org.jetbrains.annotations.NotNull;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.HashMap;
import java.util.Map;

public class PostgresTestcontainersConfiguration implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    private static final String IMAGE_NAME = "postgres:18.1-alpine3.23";
    private static final String DATABASE_NAME = "test_cloud_storage_db";
    private static final String USERNAME = "postgres";
    private static final String PASSWORD = "postgres";

    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>(IMAGE_NAME)
            .withDatabaseName(DATABASE_NAME)
            .withUsername(USERNAME)
            .withPassword(PASSWORD);

    static {postgreSQLContainer.start();}

    @Override
    public void initialize(@NotNull ConfigurableApplicationContext applicationContext) {
        Map<String, String> propertyMap = new HashMap<>();
        propertyMap.put("spring.datasource.url", postgreSQLContainer.getJdbcUrl());
        propertyMap.put("spring.datasource.username", postgreSQLContainer.getUsername());
        propertyMap.put("spring.datasource.password", postgreSQLContainer.getPassword());
        TestPropertyValues.of(propertyMap).applyTo(applicationContext.getEnvironment());
    }
}
