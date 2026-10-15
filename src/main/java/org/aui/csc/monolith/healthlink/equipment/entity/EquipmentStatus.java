package org.aui.csc.monolith.healthlink.equipment.entity;

/**
 * Operational state of one machine.
 * Only AVAILABLE machines appear as "working" on the public map.
 */
public enum EquipmentStatus {
    AVAILABLE,          // working, can be used
    OUT_OF_SERVICE,     // fault reported, cannot be used
    UNDER_MAINTENANCE   // a technician is repairing it
}
