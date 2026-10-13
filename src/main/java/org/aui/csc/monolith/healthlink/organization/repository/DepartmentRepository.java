package org.aui.csc.monolith.healthlink.organization.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.aui.csc.monolith.healthlink.organization.entity.Department;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    Page<Department> findByHospital_Id(Long hospitalId, Pageable pageable);
}