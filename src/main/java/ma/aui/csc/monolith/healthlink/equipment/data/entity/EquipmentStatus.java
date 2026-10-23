package ma.aui.csc.monolith.healthlink.equipment.data.entity;

public enum EquipmentStatus {
    AVAILABLE,          // working, can be used
    OUT_OF_SERVICE,     // fault reported, cannot be used
    UNDER_MAINTENANCE   // a technician is repairing it
}
