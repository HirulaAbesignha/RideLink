package com.ridelink.fare.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            InternalServiceTokenFilter serviceTokenFilter,
                                            SecurityErrorWriter errorWriter,
                                            JwtAuthenticationConverter jwtConverter) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/status", "/actuator/health", "/v3/api-docs/**",
                                "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/internal/v1/**").hasRole("SERVICE")
                        .requestMatchers(HttpMethod.POST, "/api/v1/fares/estimates").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/payments").hasRole("PASSENGER")
                        .requestMatchers("/api/v1/fares/rides/**", "/api/v1/payments/**").authenticated()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter))
                        .authenticationEntryPoint((request, response, exception) -> {
                            String message = exception.getMessage() == null ? "" : exception.getMessage().toLowerCase();
                            if (message.contains("expired")) {
                                errorWriter.write(request, response, 401, "TOKEN_EXPIRED", "The access token has expired");
                            } else {
                                errorWriter.write(request, response, 401, "INVALID_TOKEN", "A valid access token is required");
                            }
                        }))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                errorWriter.write(request, response, 401,
                                        "AUTHENTICATION_REQUIRED", "Authentication is required"))
                        .accessDeniedHandler((request, response, exception) ->
                                errorWriter.write(request, response, 403,
                                        "ACCESS_DENIED", "You do not have permission for this request")))
                .addFilterBefore(serviceTokenFilter, BearerTokenAuthenticationFilter.class)
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .build();
    }

    @Bean
    JwtDecoder jwtDecoder(@Value("${security.jwt.secret}") String secret,
                          @Value("${security.jwt.issuer}") String issuer) {
        if (secret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET must contain at least 32 characters");
        }
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(
                        new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
                .macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
        return decoder;
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("role");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }
}
