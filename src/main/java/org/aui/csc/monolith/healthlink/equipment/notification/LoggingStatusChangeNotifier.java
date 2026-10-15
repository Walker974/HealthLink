package org.aui.csc.monolith.healthlink.equipment.notification;

import lombok.extern.slf4j.Slf4j;
import org.aui.csc.monolith.healthlink.equipment.entity.Equipment;
import org.aui.csc.monolith.healthlink.equipment.entity.EquipmentStatus;
import org.springframework.stereotype.Component;

/**
 * First implementation of StatusChangeNotifier: writes one log line.
 *
 * @Component: "Spring, create ONE object of this class at startup and keep it
 * in the container (a bean)". When EquipmentService asks for a
 * StatusChangeNotifier, Spring finds this bean and injects it.
 *
 * @Slf4j (Lombok): creates the "log" object used below.
 */
@Slf4j
@Component
public class LoggingStatusChangeNotifier implements StatusChangeNotifier {

    @Override
    public void statusChanged(Equipment equipment, EquipmentStatus previousStatus) {
        // Privacy: only technical ids and statuses, no personal data in logs.
        log.info("Equipment {} status changed: {} -> {}",
                equipment.getId(), previousStatus, equipment.getStatus());
    }
}
