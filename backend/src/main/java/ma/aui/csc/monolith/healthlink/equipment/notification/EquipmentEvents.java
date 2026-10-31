package ma.aui.csc.monolith.healthlink.equipment.notification;

import lombok.RequiredArgsConstructor;
import ma.aui.csc.monolith.healthlink.equipment.data.entity.Equipment;
import ma.aui.csc.monolith.healthlink.equipment.data.entity.EquipmentStatus;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EquipmentEvents {
    private final ApplicationEventPublisher publisher;

    public void statusChanged(Equipment equipment, EquipmentStatus previousStatus) {
        if (previousStatus != equipment.getStatus()) {
            publisher.publishEvent(new EquipmentStatusChanged(equipment.getId(), previousStatus,
                    equipment.getStatus(), equipment.getStatusUpdatedAt()));
        }
    }
}
