package ma.aui.csc.monolith.healthlink.security;

import java.util.List;
import java.util.Map;

import ma.aui.csc.monolith.healthlink.common.security.KeycloakRoleConverter;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakRoleConverterTests {
    private final KeycloakRoleConverter converter = new KeycloakRoleConverter();

    @Test
    void acceptsOnlyKnownRealmRolesAndRemovesDuplicates() {
        Jwt jwt = token().claim("realm_access", Map.of("roles", List.of(
                "HOSPITAL_MANAGER", "offline_access", "HOSPITAL_MANAGER", "admin"))).build();
        assertThat(converter.convert(jwt)).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_HOSPITAL_MANAGER");
    }

    @Test
    void ignoresMissingOrMalformedRoleClaims() {
        assertThat(converter.convert(token().build())).isEmpty();
        assertThat(converter.convert(token().claim("realm_access", "invalid").build())).isEmpty();
        assertThat(converter.convert(token().claim("realm_access", Map.of("roles", "HOSPITAL_MANAGER"))
                .build())).isEmpty();
        assertThat(converter.convert(token().claim("realm_access", Map.of("roles", List.of(42)))
                .build())).isEmpty();
    }

    @Test
    void doesNotTreatOAuthScopesOrOtherClientRolesAsApplicationRoles() {
        Jwt jwt = token().claim("scope", "PLATFORM_ADMIN")
                .claim("resource_access", Map.of("other-client", Map.of("roles", List.of("PLATFORM_ADMIN"))))
                .build();
        assertThat(converter.convert(jwt)).isEmpty();
    }

    private Jwt.Builder token() {
        return Jwt.withTokenValue("test").header("alg", "RS256").subject("user-id");
    }
}
