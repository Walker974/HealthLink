package ma.aui.csc.monolith.healthlink.ministry.data.repository;

import java.util.List;

import ma.aui.csc.monolith.healthlink.equipment.data.entity.Equipment;
import ma.aui.csc.monolith.healthlink.ministry.data.RegionHospitalCount;
import ma.aui.csc.monolith.healthlink.ministry.data.RegionStatusCount;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Read-only aggregate queries: counts only, no individual records leave the database. */
public interface MinistryReportRepository extends Repository<Equipment, Long> {

    @Query("""
            SELECT new ma.aui.csc.monolith.healthlink.ministry.data.RegionStatusCount(r.id, e.status, COUNT(e.id))
            FROM Equipment e
            JOIN e.department d
            JOIN d.hospital h
            JOIN h.region r
            JOIN e.imagingType t
            WHERE e.active = true AND d.active = true AND h.active = true AND t.active = true
              AND (:typeCode IS NULL OR t.code = :typeCode)
            GROUP BY r.id, e.status
            """)
    List<RegionStatusCount> countMachinesByRegionAndStatus(@Param("typeCode") String typeCode);

    @Query("""
            SELECT new ma.aui.csc.monolith.healthlink.ministry.data.RegionHospitalCount(r.id, COUNT(h.id))
            FROM Hospital h
            JOIN h.region r
            WHERE h.active = true
            GROUP BY r.id
            """)
    List<RegionHospitalCount> countHospitalsByRegion();
}
