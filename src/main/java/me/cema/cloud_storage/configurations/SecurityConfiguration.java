package me.cema.cloud_storage.configurations;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final JsonAccessDeniedHandler jsonAccessDeniedHandler;
    private final JsonAuthenticationEntryPoint jsonAuthenticationEntryPoint;
    private final AuthenticatedLogoutHandler authenticatedLogoutHandler;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JsonUsernamePasswordAuthenticationFilter jsonUsernamePasswordAuthenticationFilter(AuthenticationManager authenticationManager, ObjectMapper objectMapper) {
        return new JsonUsernamePasswordAuthenticationFilter(authenticationManager, objectMapper);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JsonUsernamePasswordAuthenticationFilter jsonUsernamePasswordAuthenticationFilter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(exc ->
                        exc
                                .authenticationEntryPoint(jsonAuthenticationEntryPoint)
                                .accessDeniedHandler(jsonAccessDeniedHandler)
                ).authorizeHttpRequests(auth ->
                        auth
                                .requestMatchers("/auth/sign-up", "/auth/sign-in").anonymous()
                                .anyRequest().authenticated()
                ).addFilterAt(jsonUsernamePasswordAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .logout(logout ->
                        logout
                                .addLogoutHandler(authenticatedLogoutHandler)
                                .logoutUrl("/auth/sign-out")
                                .logoutSuccessHandler((request, response, authentication) ->
                                        response.setStatus(HttpStatus.NO_CONTENT.value())
                                )
                                .invalidateHttpSession(true)
                                .clearAuthentication(true)
                                .deleteCookies("JSESSIONID")
                ).sessionManagement(config ->
                        config.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                );

        return http.build();
    }
}
