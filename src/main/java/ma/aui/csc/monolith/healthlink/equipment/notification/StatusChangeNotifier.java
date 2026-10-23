package ma.aui.csc.monolith.healthlink.equipment.notification;

import ma.aui.csc.monolith.healthlink.equipment.data.entity.Equipment;
import ma.aui.csc.monolith.healthlink.equipment.data.entity.EquipmentStatus;

/**
 * "Someone must be told when a machine changes status."
 *
 * This is an INTERFACE on purpose (same idea as the Stripe/PayPal example):
 * EquipmentService depends on this contract, not on a concrete class.
 * Today: LoggingStatusChangeNotifier writes a log line.
 * Later: a RedisStatusChangeNotifier will publish to Redis Pub/Sub (live map),
 * and EquipmentService will NOT change a single line.
 */
public interface StatusChangeNotifier {

    void statusChanged(Equipment equipment, EquipmentStatus previousStatus);
}
