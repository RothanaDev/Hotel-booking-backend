package com.Rothana.hotel_booking_system.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final PasswordEncoder passwordEncoder;
    private final UserDetailsService userDetailsService;
    private final CookieBearerTokenResolver cookieBearerTokenResolver;

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider auth = new DaoAuthenticationProvider();
        auth.setUserDetailsService(userDetailsService);
        auth.setPasswordEncoder(passwordEncoder);
        return auth;
    }

    @Bean
    public JwtAuthenticationProvider jwtAuthenticationProvider(
            @Qualifier("refreshTokenJwtDecoder") JwtDecoder refreshTokenJwtDecoder) {
        return new JwtAuthenticationProvider(refreshTokenJwtDecoder);
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter gac = new JwtGrantedAuthoritiesConverter();
        gac.setAuthoritiesClaimName("scope");
        gac.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(gac);
        return converter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            @Qualifier("accessTokenjwtDecoder") JwtDecoder accessTokenjwtDecoder) throws Exception {

        http.cors(cors -> {});
        http.csrf(csrf -> csrf.disable());

        http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.authorizeHttpRequests(endpoint -> endpoint

                // ✅ AUTH (PUBLIC)
                .requestMatchers("/api/v1/auth/**").permitAll()

                // ✅ AUTH (PROTECTED)
                .requestMatchers("/api/v1/auth/me").authenticated()
                .requestMatchers("/api/v1/auth/all").hasRole("ADMIN")

                // ✅ USER actions
                .requestMatchers(HttpMethod.PATCH).hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.DELETE).hasAnyRole("USER", "ADMIN")

                // ✅ PUBLIC
                .requestMatchers("/upload/**").permitAll()
                .requestMatchers("/api/v1/telegram/test/**").permitAll()
                .requestMatchers("/api/v1/payments/paypal/**").permitAll()
                .requestMatchers("/api/v1/maintenance-tickets/**").permitAll()
                .requestMatchers("/api/v1/housekeeping-tasks/**").permitAll()
                .requestMatchers("/api/v1/room-calendar/**").permitAll()

                // ✅ INVENTORY
                .requestMatchers(HttpMethod.GET, "/api/v1/inventory/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/inventory/**").hasAnyRole("ADMIN", "STAFF")
                .requestMatchers(HttpMethod.PUT, "/api/v1/inventory/**").hasAnyRole("ADMIN", "STAFF")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/inventory/**").hasRole("ADMIN")

                // ✅ BOOKING SERVICES
                .requestMatchers(HttpMethod.GET, "/api/v1/booking_services/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/booking_services/**").hasAnyRole("ADMIN", "STAFF")
                .requestMatchers(HttpMethod.PUT, "/api/v1/booking_services/**").hasAnyRole("ADMIN", "STAFF")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/booking_services/**").hasRole("ADMIN")

                // ✅ BOOKINGS
                .requestMatchers(HttpMethod.GET, "/api/v1/bookings/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/bookings/**").hasAnyRole("ADMIN", "STAFF", "USER")
                .requestMatchers(HttpMethod.PUT, "/api/v1/bookings/**").hasAnyRole("ADMIN", "STAFF")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/bookings/**").hasRole("ADMIN")

                // ✅ SERVICES
                .requestMatchers(HttpMethod.GET, "/api/v1/services/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/services/**").hasAnyRole("ADMIN", "STAFF")
                .requestMatchers(HttpMethod.PUT, "/api/v1/services/**").hasAnyRole("ADMIN", "STAFF")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/services/**").hasRole("ADMIN")

                // ✅ ROOMS
                .requestMatchers(HttpMethod.GET, "/api/v1/rooms/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/rooms/**").hasAnyRole("ADMIN", "STAFF", "USER")
                .requestMatchers(HttpMethod.PUT, "/api/v1/rooms/**").hasAnyRole("ADMIN", "STAFF", "USER")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/rooms/**").hasRole("ADMIN")

                // ✅ ROOM TYPES
                .requestMatchers(HttpMethod.GET, "/api/v1/roomTypes/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/roomTypes/**").hasAnyRole("ADMIN", "STAFF")
                .requestMatchers(HttpMethod.PUT, "/api/v1/roomTypes/**").hasAnyRole("ADMIN", "STAFF")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/roomTypes/**").hasRole("ADMIN")

                // ✅ UPLOAD
                .requestMatchers(HttpMethod.POST, "/api/v1/upload/**").hasAnyRole("ADMIN", "STAFF")

                .anyRequest().authenticated()
        );

        http.oauth2ResourceServer(oauth2 -> oauth2
                .bearerTokenResolver(cookieBearerTokenResolver)
                .jwt(jwt -> jwt
                        .decoder(accessTokenjwtDecoder)
                        .jwtAuthenticationConverter(jwtAuthenticationConverter()))
        );

        return http.build();
    }
}