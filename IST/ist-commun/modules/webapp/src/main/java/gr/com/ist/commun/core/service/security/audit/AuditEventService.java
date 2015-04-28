package gr.com.ist.commun.core.service.security.audit;

import gr.com.ist.commun.core.domain.security.audit.AuditEvent;
import gr.com.ist.commun.core.domain.security.audit.AuditEventConverter;
import gr.com.ist.commun.core.domain.security.audit.PersistentAuditEvent;
import gr.com.ist.commun.core.repository.security.audit.PersistenceAuditEventsRepository;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing audit events.
 * <p/>
 * <p>
 * This is the default implementation to support SpringBoot Actuator AuditEventRepository
 * </p>
 */
@Service
@Transactional
public class AuditEventService {

    @Inject
    private PersistenceAuditEventsRepository persistenceAuditEventsRepository;

    @Inject
    private AuditEventConverter auditEventConverter;

    public List<AuditEvent> findAll() {
        return auditEventConverter.convertToAuditEvent(persistenceAuditEventsRepository.findAll());
    }

    public List<AuditEvent> findByDates(Date fromDate, Date toDate) {
        final List<PersistentAuditEvent> persistentAuditEvents =
                persistenceAuditEventsRepository.findByDates(fromDate, toDate);

        return auditEventConverter.convertToAuditEvent(persistentAuditEvents);
    }
}
