package ma.aui.csc.monolith.healthlink.audit.data.repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import ma.aui.csc.monolith.healthlink.audit.data.entity.AuditRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class AuditRecordRepository {
    private final EntityManager entityManager;

    @Transactional(propagation = Propagation.MANDATORY)
    public void append(AuditRecord record) {
        entityManager.persist(record);
    }

    public Page<AuditRecord> findByEquipmentId(Long equipmentId, Pageable pageable) {
        var records = entityManager.createQuery("""
                SELECT a FROM AuditRecord a WHERE a.equipmentId = :equipmentId
                ORDER BY a.occurredAt ASC, a.id ASC
                """, AuditRecord.class)
                .setParameter("equipmentId", equipmentId)
                .setFirstResult(Math.toIntExact(pageable.getOffset())).setMaxResults(pageable.getPageSize())
                .getResultList();
        long total = entityManager.createQuery(
                        "SELECT COUNT(a) FROM AuditRecord a WHERE a.equipmentId = :equipmentId", Long.class)
                .setParameter("equipmentId", equipmentId).getSingleResult();
        return new PageImpl<>(records, pageable, total);
    }
}
