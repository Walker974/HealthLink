package ma.aui.csc.monolith.healthlink.availability.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.aui.csc.monolith.healthlink.equipment.notification.EquipmentStatusChanged;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Clears cached map searches after a status change is COMMITTED (never before: a rolled-back
 * change must not affect what citizens see). Runs before live updates are sent, so a browser that
 * reloads after receiving an update gets fresh results. The cache TTL is a second safety net.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AvailabilityCacheInvalidator {
    private final AvailabilityCache cache;

    @Order(10)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStatusChanged(EquipmentStatusChanged event) {
        try {
            cache.clear();
        } catch (RuntimeException exception) {
            log.warn("Could not clear the availability cache after equipment {} changed; entries expire by TTL",
                    event.equipmentId(), exception);
        }
    }
}
