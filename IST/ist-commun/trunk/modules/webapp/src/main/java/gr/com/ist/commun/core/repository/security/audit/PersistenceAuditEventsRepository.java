package gr.com.ist.commun.core.repository.security.audit;

import gr.com.ist.commun.core.domain.security.audit.PersistentAuditEvent;

import java.util.Date;
import java.util.List;

import org.joda.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * Spring Data JPA repository for the PersistentAuditEvent entity.
 */
@RepositoryRestResource(exported = false)
public interface PersistenceAuditEventsRepository extends JpaRepository<PersistentAuditEvent, String> {

    List<PersistentAuditEvent> findByPrincipal(String principal);

    List<PersistentAuditEvent> findByPrincipalAndAuditEventDateGreaterThan(String principal, LocalDateTime after);

    @Query("select p from PersistentAuditEvent p where p.auditEventDate >= ?1 and p.auditEventDate <= ?2")
    List<PersistentAuditEvent> findByDates(Date fromDate, Date toDate);
}
