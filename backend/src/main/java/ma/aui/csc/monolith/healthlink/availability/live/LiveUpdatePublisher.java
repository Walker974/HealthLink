package ma.aui.csc.monolith.healthlink.availability.live;

import lombok.extern.slf4j.Slf4j;
import ma.aui.csc.monolith.healthlink.availability.data.AvailabilityDtos.LiveAvailabilityEvent;
import ma.aui.csc.monolith.healthlink.equipment.notification.EquipmentStatusChanged;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends a status change to the live map only AFTER the database transaction commits.
 * If the transaction rolls back, nothing is announced. If Redis is down, the change is still
 * saved (PostgreSQL is the source of truth) and browsers catch up on their next reload.
 */
@Component
@Slf4j
public class LiveUpdatePublisher {
    private final LiveUpdateBroker broker;
    private final String instanceId;

    public LiveUpdatePublisher(LiveUpdateBroker broker, @Value("${healthlink.instance-id:local}") String instanceId) {
        this.broker = broker;
        this.instanceId = instanceId;
    }

    @Order(20)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStatusChanged(EquipmentStatusChanged change) {
        try {
            broker.publish(LiveAvailabilityEvent.from(change, instanceId));
        } catch (RuntimeException exception) {
            log.warn("Live update not delivered for committed equipment change {}", change.equipmentId(), exception);
        }
    }
}
