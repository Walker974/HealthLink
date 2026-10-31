package ma.aui.csc.monolith.healthlink.organization.data.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ma.aui.csc.monolith.healthlink.organization.data.entity.Department;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    Page<Department> findByHospital_Id(Long hospitalId, Pageable pageable);
}