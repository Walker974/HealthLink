package ma.aui.csc.monolith.healthlink.ministry.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import ma.aui.csc.monolith.healthlink.common.security.StaffAccess;
import ma.aui.csc.monolith.healthlink.equipment.data.entity.EquipmentStatus;
import ma.aui.csc.monolith.healthlink.ministry.data.MinistryDtos.AvailabilityFigures;
import ma.aui.csc.monolith.healthlink.ministry.data.MinistryDtos.AvailabilitySummaryResponse;
import ma.aui.csc.monolith.healthlink.ministry.data.MinistryDtos.RegionAvailability;
import ma.aui.csc.monolith.healthlink.ministry.data.RegionHospitalCount;
import ma.aui.csc.monolith.healthlink.ministry.data.RegionStatusCount;
import ma.aui.csc.monolith.healthlink.ministry.data.repository.MinistryReportRepository;
import ma.aui.csc.monolith.healthlink.organization.data.entity.Region;
import ma.aui.csc.monolith.healthlink.organization.data.repository.RegionRepository;
import ma.aui.csc.monolith.healthlink.staff.data.entity.StaffRole;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regional and national availability for Ministry officers. Figures are computed from the live
 * equipment table at request time, so they always agree with what citizens see on the map.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MinistryService {
    private final MinistryReportRepository reports;
    private final RegionRepository regions;
    private final StaffAccess staffAccess;

    @PreAuthorize("hasRole('MINISTRY_OFFICER')")
    public AvailabilitySummaryResponse summary(String imagingTypeCode) {
        staffAccess.requireRole(StaffRole.MINISTRY_OFFICER);
        String type = imagingTypeCode == null || imagingTypeCode.isBlank()
                ? null : imagingTypeCode.strip().toUpperCase(Locale.ROOT);

        Map<Long, Map<EquipmentStatus, Long>> machines = new HashMap<>();
        for (RegionStatusCount row : reports.countMachinesByRegionAndStatus(type)) {
            machines.computeIfAbsent(row.regionId(), id -> new EnumMap<>(EquipmentStatus.class))
                    .merge(row.status(), row.machines(), Long::sum);
        }
        Map<Long, Long> hospitals = new HashMap<>();
        for (RegionHospitalCount row : reports.countHospitalsByRegion()) {
            hospitals.put(row.regionId(), row.hospitals());
        }

        List<RegionAvailability> rows = new ArrayList<>();
        Map<EquipmentStatus, Long> nationalMachines = new EnumMap<>(EquipmentStatus.class);
        long nationalHospitals = 0;
        for (Region region : regions.findAll(Sort.by("name", "id"))) {
            if (!region.isActive()) {
                continue;
            }
            Map<EquipmentStatus, Long> counts = machines.getOrDefault(region.getId(), Map.of());
            long regionHospitals = hospitals.getOrDefault(region.getId(), 0L);
            counts.forEach((status, count) -> nationalMachines.merge(status, count, Long::sum));
            nationalHospitals += regionHospitals;
            rows.add(new RegionAvailability(region.getId(), region.getCode(), region.getName(),
                    figures(regionHospitals, counts)));
        }
        return new AvailabilitySummaryResponse(type, Instant.now(), figures(nationalHospitals, nationalMachines), rows);
    }

    private static AvailabilityFigures figures(long hospitals, Map<EquipmentStatus, Long> counts) {
        long available = counts.getOrDefault(EquipmentStatus.AVAILABLE, 0L);
        long outOfService = counts.getOrDefault(EquipmentStatus.OUT_OF_SERVICE, 0L);
        long underMaintenance = counts.getOrDefault(EquipmentStatus.UNDER_MAINTENANCE, 0L);
        long total = available + outOfService + underMaintenance;
        Double percent = total == 0 ? null : Math.round(available * 1000.0 / total) / 10.0;
        return new AvailabilityFigures(hospitals, total, available, outOfService, underMaintenance, percent);
    }
}
