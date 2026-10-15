package org.aui.csc.monolith.healthlink.equipment.repository;

import java.util.List;

import org.aui.csc.monolith.healthlink.equipment.entity.Equipment;
import org.aui.csc.monolith.healthlink.equipment.entity.EquipmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Data access for Equipment (the DAO layer).
 *
 * We only write an INTERFACE: Spring Data generates the implementation
 * at startup and registers it as a bean (Inversion of Control).
 * JpaRepository<Equipment, Long> gives save(), findById(), findAll(), ... for free.
 */
public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

    /**
     * STYLE 1 - Derived query: Spring reads the METHOD NAME and builds the query.
     *   find + By + Status  ->  SELECT e FROM Equipment e WHERE e.status = ?1
     */
    List<Equipment> findByStatus(EquipmentStatus status);

    /**
     * STYLE 1 again, following a link with "_":
     *   Department_Id  ->  WHERE e.department.id = ?1
     */
    List<Equipment> findByDepartment_Id(Long departmentId);

    /**
     * STYLE 2 - @Query: the condition is too complex for a method name,
     * so WE write the query in JPQL.
     *
     * JPQL uses ENTITY and FIELD names (Equipment, e.department, h.city),
     * not table/column names. Hibernate translates it into SQL with JOINs.
     *
     * :status, :typeCode, :city are PARAMETERS: values are sent separately
     * from the query text, never pasted into it -> protects against
     * SQL injection (CWE-89). (?1, ?2 is the same idea, by position.)
     *
     * e.active = true -> soft-deleted machines are never shown.
     */
    @Query("""
            SELECT e FROM Equipment e
            JOIN e.department d
            JOIN d.hospital h
            WHERE e.status = :status
              AND e.active = true
              AND e.imagingType.code = :typeCode
              AND h.city = :city
            """)
    List<Equipment> findByStatusAndTypeInCity(
            @Param("status") EquipmentStatus status,
            @Param("typeCode") String typeCode,
            @Param("city") String city
    );
}
