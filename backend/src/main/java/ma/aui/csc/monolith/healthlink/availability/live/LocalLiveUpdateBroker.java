package ma.aui.csc.monolith.healthlink.availability.live;

import lombok.RequiredArgsConstructor;
import ma.aui.csc.monolith.healthlink.availability.data.AvailabilityDtos.LiveAvailabilityEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Single-instance mode (Redis disabled): deliver directly to this instance's browsers. */
@Component
@ConditionalOnProperty(name = "healthlink.redis.enabled", havingValue = "false", matchIfMissing = true)
@RequiredArgsConstructor
public class LocalLiveUpdateBroker implements LiveUpdateBroker {
    private final SseConnectionRegistry connections;

    @Override
    public void publish(LiveAvailabilityEvent event) {
        connections.broadcast(event);
    }
}
