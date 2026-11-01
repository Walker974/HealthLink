package ma.aui.csc.monolith.healthlink.availability.live;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.aui.csc.monolith.healthlink.availability.data.AvailabilityDtos.LiveAvailabilityEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Multi-instance mode. The instance that committed a change PUBLISHES it on a Redis channel;
 * every instance (including the publisher) is SUBSCRIBED and forwards it to its own SSE clients.
 * That is how a citizen connected to instance A sees a change made through instance B.
 *
 * Pub/Sub keeps no history: a browser that was disconnected reloads the current state over REST.
 */
@Component
@ConditionalOnProperty(name = "healthlink.redis.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class RedisLiveUpdateBroker implements LiveUpdateBroker, MessageListener {
    public static final String CHANNEL = "healthlink:equipment-status";

    private final StringRedisTemplate redis;
    private final SseConnectionRegistry connections;
    private final JsonMapper json;

    @Override
    public void publish(LiveAvailabilityEvent event) {
        redis.convertAndSend(CHANNEL, json.writeValueAsString(event));
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            connections.broadcast(json.readValue(message.getBody(), LiveAvailabilityEvent.class));
        } catch (RuntimeException exception) {
            log.warn("Ignoring an unreadable live-update message", exception);
        }
    }
}
