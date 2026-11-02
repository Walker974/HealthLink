package ma.aui.csc.monolith.healthlink.availability.live;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;
import ma.aui.csc.monolith.healthlink.availability.data.AvailabilityDtos.LiveAvailabilityEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.databind.json.JsonMapper;

/**
 * The open Server-Sent Events connections of THIS application instance.
 *
 * SSE is a long-lived HTTP response that the server keeps writing to. Browsers open it with
 * EventSource and reconnect automatically when it closes, so connections are capped in time
 * and in number, and dead ones are dropped as soon as a write fails.
 */
@Component
@Slf4j
public class SseConnectionRegistry {
    static final long CONNECTION_TIMEOUT_MS = Duration.ofMinutes(30).toMillis();

    private final Set<SseEmitter> emitters = ConcurrentHashMap.newKeySet();
    private final JsonMapper json;
    private final int maxConnections;
    private final String instanceId;

    public SseConnectionRegistry(JsonMapper json,
                                 @Value("${healthlink.live.max-connections:1000}") int maxConnections,
                                 @Value("${healthlink.instance-id:local}") String instanceId) {
        this.json = json;
        this.maxConnections = maxConnections;
        this.instanceId = instanceId;
    }

    public SseEmitter open() {
        if (emitters.size() >= maxConnections) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Too many live connections; retry later");
        }
        SseEmitter emitter = new SseEmitter(CONNECTION_TIMEOUT_MS);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> {
            emitters.remove(emitter);
            emitter.complete();
        });
        emitter.onError(error -> emitters.remove(emitter));
        // The first event confirms the subscription and tells the browser how fast to reconnect.
        send(emitter, SseEmitter.event().name("connected").reconnectTime(5000)
                .data(json.writeValueAsString(Map.of("instance", instanceId)), MediaType.APPLICATION_JSON));
        return emitter;
    }

    public void broadcast(LiveAvailabilityEvent event) {
        String payload = json.writeValueAsString(event);
        for (SseEmitter emitter : emitters) {
            send(emitter, SseEmitter.event().name("equipment-status").data(payload, MediaType.APPLICATION_JSON));
        }
    }

    /** Comment lines keep proxies from closing idle connections and reveal dead clients. */
    @Scheduled(fixedDelayString = "${healthlink.live.heartbeat-interval-ms:25000}")
    public void heartbeat() {
        for (SseEmitter emitter : emitters) {
            send(emitter, SseEmitter.event().comment("keep-alive"));
        }
    }

    public int openConnections() {
        return emitters.size();
    }

    private void send(SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (IOException | IllegalStateException exception) {
            emitters.remove(emitter);
            log.debug("Dropped a closed live-update connection");
        }
    }
}
