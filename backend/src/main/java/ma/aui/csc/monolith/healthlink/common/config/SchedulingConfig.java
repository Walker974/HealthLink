package ma.aui.csc.monolith.healthlink.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Enables @Scheduled tasks, such as the keep-alive comments on live-update connections. */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
