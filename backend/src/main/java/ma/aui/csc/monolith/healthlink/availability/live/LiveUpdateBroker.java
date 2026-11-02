package ma.aui.csc.monolith.healthlink.availability.live;

import ma.aui.csc.monolith.healthlink.availability.data.AvailabilityDtos.LiveAvailabilityEvent;

/**
 * Delivers a committed change to every application instance, which then pushes it to its own
 * browsers. Two implementations: in-memory (one instance) or Redis Pub/Sub (several instances).
 */
public interface LiveUpdateBroker {
    void publish(LiveAvailabilityEvent event);
}
