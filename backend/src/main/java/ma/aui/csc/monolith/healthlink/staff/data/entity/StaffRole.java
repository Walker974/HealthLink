package ma.aui.csc.monolith.healthlink.staff.data.entity;

/**
 * The fixed list of staff roles (an enum = a closed set of values).
 * Later, the same names will be the roles defined in Keycloak.
 */
public enum StaffRole {
    RADIOLOGY_OPERATOR,     // updates equipment status in their department
    MAINTENANCE_TECHNICIAN, // repairs equipment in their hospital
    HOSPITAL_MANAGER,       // manages staff and verifies repairs in their hospital
    MINISTRY_OFFICER,       // reads availability for a region or the country
    PLATFORM_ADMIN          // registers hospitals and manager accounts
}
