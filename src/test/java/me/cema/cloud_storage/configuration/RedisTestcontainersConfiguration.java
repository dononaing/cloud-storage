package me.cema.cloud_storage.configuration;

import com.redis.testcontainers.RedisContainer;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.utility.DockerImageName;

import java.util.HashMap;
import java.util.Map;

public class RedisTestcontainersConfiguration implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    private static final String IMAGE_NAME = "redis:latest";

    static RedisContainer redisContainer = new RedisContainer(DockerImageName.parse(IMAGE_NAME));

    static {
        redisContainer.start();
    }

    @Override
    public void initialize(@NotNull ConfigurableApplicationContext applicationContext) {
        Map<String, String> propertyMap = new HashMap<>();
        propertyMap.put("spring.data.redis.port", String.valueOf(redisContainer.getFirstMappedPort()));
        propertyMap.put("spring.data.redis.host", redisContainer.getHost());
        TestPropertyValues.of(propertyMap).applyTo(applicationContext.getEnvironment());
    }
}
