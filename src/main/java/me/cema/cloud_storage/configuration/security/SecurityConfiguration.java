package me.cema.cloud_storage.configuration.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
    private final JsonLogoutSuccessHandler jsonLogoutSuccessHandler;

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
                .exceptionHandling(exc -> exc
                        .authenticationEntryPoint(jsonAuthenticationEntryPoint)
                        .accessDeniedHandler(jsonAccessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/sign-up").permitAll()
                        .anyRequest().authenticated())
                .addFilterAt(jsonUsernamePasswordAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .logout(logout -> logout
                        .logoutUrl("/auth/sign-out")
                        .logoutSuccessHandler(jsonLogoutSuccessHandler)
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("SESSION"))
                .sessionManagement(config ->
                        config.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED));

        return http.build();
    }
}
