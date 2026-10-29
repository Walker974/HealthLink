package ma.aui.csc.monolith.healthlink.equipment.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class CommittedStatusChangeListener {
    private final StatusChangeNotifier notifier;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStatusChanged(EquipmentStatusChanged event) {
        try {
            notifier.statusChanged(event);
        } catch (RuntimeException exception) {
            log.error("Notification failed for committed equipment change {}", event.equipmentId(), exception);
        }
    }
}
