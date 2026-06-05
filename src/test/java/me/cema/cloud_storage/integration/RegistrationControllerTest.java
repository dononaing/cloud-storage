package me.cema.cloud_storage.integration;

import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
import io.minio.RemoveObjectsArgs;
import io.minio.Result;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import me.cema.cloud_storage.CloudStorageApplicationTests;
import me.cema.cloud_storage.dto.user.UserExceptionResponse;
import me.cema.cloud_storage.dto.user.UserRequest;
import me.cema.cloud_storage.dto.user.UserResponse;
import me.cema.cloud_storage.model.user.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;

import java.util.ArrayList;
import java.util.List;


class RegistrationControllerTest extends CloudStorageApplicationTests {
    private final static String TEST_USERNAME = "testUsername";
    private final static String BUCKET_NAME = "user-files";
    private final static String SESSION_COOKIE_NAME = "SESSION";

    @BeforeAll
    void createBucket() {
        try {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(BUCKET_NAME).build());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @AfterEach
    void clearContainers() {
        userRepository.deleteAll();
        Iterable<Result<Item>> results = minioClient.listObjects(ListObjectsArgs.builder()
                .bucket(BUCKET_NAME)
                .recursive(true)
                .prefix("")
                .build());
        List<DeleteObject> deleteObjectList = new ArrayList<>();
        for (Result<Item> itemResult : results) {
            try {
                deleteObjectList.add(new DeleteObject(itemResult.get().objectName()));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        minioClient.removeObjects(RemoveObjectsArgs.builder()
                .bucket(BUCKET_NAME)
                .objects(deleteObjectList)
                .build());
    }

    @Test
    void signUp_unauthenticatedUserValidRequest_mustSaveToDbAndCreateUserFolder_return201WithValidBody() throws JsonProcessingException {
        UserRequest userRequest = new UserRequest(
                TEST_USERNAME,
                "testPassword"
        );
        WebTestClient.ResponseSpec unauthenticatedExchange = webTestClient
                .post()
                .uri(uri -> uri.path("/auth/sign-up").build())
                .bodyValue(userRequest)
                .accept(MediaType.APPLICATION_JSON)
                .exchange();
        unauthenticatedExchange.expectStatus().isEqualTo(201);
        unauthenticatedExchange.expectBody().json(objectMapper.writeValueAsString(new UserResponse(userRequest.getUsername())));
        unauthenticatedExchange.expectCookie().exists(SESSION_COOKIE_NAME);
    }

    @Test
    void signUp_unauthenticatedUserExistingUsername_mustThrowException_return409WithValidExceptionBody() throws JsonProcessingException {
        UserRequest userRequest = new UserRequest(
                TEST_USERNAME,
                "testPassword"
        );
        User user = new User(
                null,
                userRequest.getUsername(),
                passwordEncoder.encode(userRequest.getPassword())
        );
        userRepository.save(user);
        WebTestClient.ResponseSpec exchange = webTestClient
                .post()
                .uri(uri -> uri.path("/auth/sign-up").build())
                .bodyValue(userRequest)
                .accept(MediaType.APPLICATION_JSON)
                .exchange();
        exchange.expectStatus().isEqualTo(409);
        exchange.expectBody().json(objectMapper.writeValueAsString(new UserExceptionResponse("409 CONFLICT \"username already exists\"")));
    }

    @Test
    void signUp_unauthenticatedUserInvalidCredentials_mustThrowException_return400WithValidExceptionBody() throws JsonProcessingException {
        UserRequest invalidUserRequest = new UserRequest(
                "us",
                "pas"
        );
        WebTestClient.ResponseSpec exchange = webTestClient
                .post()
                .uri("/auth/sign-up")
                .bodyValue(invalidUserRequest)
                .accept(MediaType.APPLICATION_JSON)
                .exchange();
        exchange.expectStatus().isEqualTo(400);
        exchange.expectBody().json(objectMapper.writeValueAsString(new UserExceptionResponse("name must be between 5 and 20 characters" +
                ";password must be between 5 and 20 characters")));

    }
}