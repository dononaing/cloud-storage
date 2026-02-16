package me.cema.cloud_storage.integration.controllers;

import me.cema.cloud_storage.CloudStorageApplicationTests;
import me.cema.cloud_storage.dto.UserExceptionResponse;
import me.cema.cloud_storage.dto.UserRegistrationResponse;
import me.cema.cloud_storage.dto.UserRequest;
import me.cema.cloud_storage.models.user.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;


class RegistrationControllerTest extends CloudStorageApplicationTests {

    @AfterEach
    void clearPostgresContainer() {
        userRepository.deleteAll();
    }

    @Test
    void signUp_unauthenticatedUserValidRequest_mustSaveToDb_return201WithValidBody() throws JsonProcessingException {
        UserRequest userRequest = new UserRequest(
                "testUsername",
                "testPassword"
        );
        WebTestClient.ResponseSpec unauthenticatedExchange = webTestClient
                .post()
                .uri(uri -> uri.path("/auth/sign-up").build())
                .bodyValue(userRequest)
                .accept(MediaType.APPLICATION_JSON)
                .exchange();
        unauthenticatedExchange.expectStatus().isEqualTo(201);
        unauthenticatedExchange.expectBody().json(objectMapper.writeValueAsString(new UserRegistrationResponse(userRequest.getUsername())));
        unauthenticatedExchange.expectCookie().exists("JSESSIONID");
    }

    @Test
    void signUp_unauthenticatedUserExistingUsername_mustThrowException_return409WithValidExceptionBody() throws JsonProcessingException {
        UserRequest userRequest = new UserRequest(
                "testUsername",
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
        exchange.expectBody().json(objectMapper.writeValueAsString(new UserExceptionResponse("409 username already exists")));
    }

    @Test
    void signUp_authenticatedUser_mustThrowException_return403WithValidExceptionBody() throws JsonProcessingException {
        UserRequest userRequest = new UserRequest(
                "testUsername",
                "testPassword"
        );

        WebTestClient.ResponseSpec exchange = webTestClient
                .post()
                .uri("/auth/sign-up")
                .bodyValue(userRequest)
                .accept(MediaType.APPLICATION_JSON)
                .exchange();
        exchange.expectStatus().isEqualTo(201);
        exchange.expectCookie().exists("JSESSIONID");

        String sessionId = exchange.returnResult(Void.class)
                .getResponseCookies()
                .getFirst("JSESSIONID")
                .getValue();

        WebTestClient.ResponseSpec authenticatedExchange = webTestClient
                .post()
                .uri("/auth/sign-up")
                .cookies(cookies -> cookies.add("JSESSIONID", sessionId))
                .bodyValue(userRequest)
                .accept(MediaType.APPLICATION_JSON)
                .exchange();
        authenticatedExchange.expectStatus().isEqualTo(403);
        authenticatedExchange.expectBody().json((objectMapper.writeValueAsString(
                new UserExceptionResponse("Access Denied")
        )));
    }

    @Test
    void signUp_unauthenticatedUserInvalidCredentials_mustThrowException_return400WithValidExceptionBody() throws JsonProcessingException {
        UserRequest invalidUserRequest = new UserRequest(
                "us",
                "password"
        );
        WebTestClient.ResponseSpec exchange = webTestClient
                .post()
                .uri("/auth/sign-up")
                .bodyValue(invalidUserRequest)
                .accept(MediaType.APPLICATION_JSON)
                .exchange();
        exchange.expectStatus().isEqualTo(400);
        exchange.expectBody().json(objectMapper.writeValueAsString(new UserExceptionResponse("name must be between 3 and 50 characters")));
    }
}