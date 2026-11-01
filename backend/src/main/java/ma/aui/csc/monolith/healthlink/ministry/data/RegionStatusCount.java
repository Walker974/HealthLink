package ma.aui.csc.monolith.healthlink.ministry.data;

import ma.aui.csc.monolith.healthlink.equipment.data.entity.EquipmentStatus;

/** Query row: how many machines of a region are in one status. */
public record RegionStatusCount(Long regionId, EquipmentStatus status, Long machines) {}
