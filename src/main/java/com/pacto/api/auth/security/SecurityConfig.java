package com.pacto.api.auth.security;

import com.pacto.api.auth.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/auth/signup",
                                "/api/v1/auth/login",
                                "/actuator/health",
                                "/swagger-ui/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/campaigns").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/campaigns/{campaignId}").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/payments/webhook/portone").permitAll()

                        .requestMatchers(HttpMethod.POST, "/api/v1/campaigns").hasRole("ADVERTISER")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/campaigns/**").hasRole("ADVERTISER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/campaigns/*/missions").hasRole("ADVERTISER")
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/payments",
                                "/api/v1/payments/*"
                        ).hasRole("ADVERTISER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/payments").hasRole("ADVERTISER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/payments/*/refund").hasRole("ADVERTISER")
                        .requestMatchers(HttpMethod.PATCH,
                                "/api/v1/applications/*/accept",
                                "/api/v1/applications/*/reject"
                        ).hasRole("ADVERTISER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/applications/campaign/*").hasRole("ADVERTISER")
                        .requestMatchers(HttpMethod.PATCH,
                                "/api/v1/missions/*/approve",
                                "/api/v1/missions/*/reject",
                                "/api/v1/missions/*/cancel"
                        ).hasRole("ADVERTISER")

                        .requestMatchers(HttpMethod.POST, "/api/v1/applications").hasRole("BLOGGER")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/applications/*/cancel").hasRole("BLOGGER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/applications/me").hasRole("BLOGGER")
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/missions/me",
                                "/api/v1/escrows"
                        ).hasRole("BLOGGER")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/missions/*/submit").hasRole("BLOGGER")

                        .requestMatchers("/api/v1/wallets/**").hasAnyRole("BLOGGER", "ADVERTISER")
                        .requestMatchers("/api/v1/advertiser/**").hasRole("ADVERTISER")
                        .anyRequest().authenticated()
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"
        ));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
