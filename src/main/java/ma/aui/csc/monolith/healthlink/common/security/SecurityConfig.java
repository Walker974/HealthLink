package ma.aui.csc.monolith.healthlink.common.security;

import java.util.Arrays;
import java.util.List;

import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRoleConverter());
        // The principal name remains the immutable Keycloak sub, never an email.
        return http
                .cors(cors -> {})
                // This API accepts bearer headers only and does not authenticate cookies.
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.GET, "/swagger-ui.html", "/swagger-ui/**",
                                "/v3/api-docs", "/v3/api-docs/**", "/v3/api-docs.yaml").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/equipment/available").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/organization/regions",
                                "/api/organization/regions/{id}",
                                "/api/organization/regions/{id}/hospitals",
                                "/api/organization/hospitals/{id}").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/organization/regions",
                                "/api/organization/hospitals", "/api/staff").hasRole("PLATFORM_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/organization/departments",
                                "/api/equipment").hasRole("HOSPITAL_MANAGER")
                        .requestMatchers(HttpMethod.PATCH, "/api/equipment/{id}/status")
                                .hasRole("RADIOLOGY_OPERATOR")
                        .requestMatchers(HttpMethod.GET, "/api/organization/departments/{id}",
                                "/api/organization/hospitals/{id}/departments")
                                .hasAnyRole("HOSPITAL_MANAGER", "RADIOLOGY_OPERATOR", "MAINTENANCE_TECHNICIAN")
                        .requestMatchers(HttpMethod.GET, "/api/staff/me").authenticated()
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(converter)))
                .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${healthlink.security.allowed-origins}") String allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::strip).filter(origin -> !origin.isEmpty()).toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setExposedHeaders(List.of("Location"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
