package me.cema.cloud_storage;

import jakarta.annotation.PostConstruct;
import me.cema.cloud_storage.configurations.PostgresContextInitializer;
import me.cema.cloud_storage.repositories.UserRepository;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ContextConfiguration(initializers = {PostgresContextInitializer.class})
public class CloudStorageApplicationTests {
    protected WebTestClient webTestClient;
    @LocalServerPort
    protected int port;
    protected static final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    public UserRepository userRepository;
    @Autowired
    public BCryptPasswordEncoder passwordEncoder;
    @Value("${server.servlet.context-path}")
    private String contextPath;
    @PostConstruct
    void initWebTestClient() {
        webTestClient = WebTestClient.bindToServer().baseUrl("http://localhost:" + port + contextPath).build();
    }
}
