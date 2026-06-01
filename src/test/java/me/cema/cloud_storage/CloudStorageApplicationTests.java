package me.cema.cloud_storage;

import io.minio.MinioClient;
import jakarta.annotation.PostConstruct;
import me.cema.cloud_storage.configuration.MinioTestcontainersConfiguration;
import me.cema.cloud_storage.configuration.PostgresTestcontainersConfiguration;
import me.cema.cloud_storage.configuration.RedisTestcontainersConfiguration;
import me.cema.cloud_storage.repository.UserRepository;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith({SpringExtension.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ContextConfiguration(initializers = {
        PostgresTestcontainersConfiguration.class,
        MinioTestcontainersConfiguration.class,
        RedisTestcontainersConfiguration.class
})
public class CloudStorageApplicationTests {
    @LocalServerPort
    protected int port;
    @Autowired
    public UserRepository userRepository;
    @Autowired
    public BCryptPasswordEncoder passwordEncoder;
    @Autowired
    public MinioClient minioClient;
    protected WebTestClient webTestClient;
    protected static final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    void initWebTestClient() {
        webTestClient = WebTestClient.bindToServer().baseUrl("http://localhost:" + port + "/api").build();
    }
}
