package org.aui.csc.monolith.healthlink.common.config;

import java.util.Optional;

import org.aui.csc.monolith.healthlink.staff.entity.StaffMember;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

    /**
     * @Bean: Spring calls this method ONCE at startup and stores the returned
     * object in the container. We never call it ourselves (Inversion of Control):
     * the auditing listener asks this bean for the current user on every save.
     *
     * For now there is no login, so we return "nobody" (Optional.empty()):
     * only the dates are filled. When Keycloak is added, ONLY this method
     * changes: read the email from the JWT, then look up the StaffMember.
     */
    @Bean
    public AuditorAware<StaffMember> auditorProvider() {
        return () -> Optional.empty();
    }
}
