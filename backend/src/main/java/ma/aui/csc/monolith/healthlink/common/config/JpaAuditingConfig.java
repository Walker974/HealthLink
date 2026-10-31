package ma.aui.csc.monolith.healthlink.common.config;

import java.util.Optional;

import ma.aui.csc.monolith.healthlink.staff.data.entity.StaffMember;
import ma.aui.csc.monolith.healthlink.staff.data.repository.StaffMemberRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Turns on JPA auditing (createdBy, createdDate, lastModifiedBy, lastModifiedDate).
 *
 * @Configuration: this class gives bean definitions to the IoC container.
 * @EnableJpaAuditing: activates auditing and tells Spring which bean
 *                     answers "who is the current user?" (auditorProvider).
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfig {
    @Bean
    public AuditorAware<StaffMember> auditorProvider(@Lazy StaffMemberRepository staff) {
        return () -> {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication instanceof JwtAuthenticationToken token && token.isAuthenticated()
                    && token.getToken().getSubject() != null && !token.getToken().getSubject().isBlank()) {
                return staff.findByKeycloakSubjectAndActiveTrue(token.getToken().getSubject()).filter(StaffMember::isActive);
            }
            return Optional.empty();
        };
    }
}
