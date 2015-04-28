package gr.com.ist.commun.core.repository.security.audit;

import gr.com.ist.commun.core.domain.security.audit.AuditEvent;
import gr.com.ist.commun.core.domain.security.audit.AuditEventConverter;
import gr.com.ist.commun.core.domain.security.audit.AuditUtils;
import gr.com.ist.commun.core.domain.security.audit.PersistentAuditEvent;

import java.util.Date;
import java.util.List;

import org.joda.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wraps an implementation of Spring Boot's AuditEventRepository.
 */
@Configuration
public class CustomAuditEventRepository {

    @Bean
    public AuditEventRepository auditEventRepository() {
        return new AuditEventRepository() {

            @Autowired
            private PersistenceAuditEventsRepository persistenceAuditEventRepository;
            
            @Autowired
            private AuditEventConverter auditEventConverter;
            
            @Override
            public List<AuditEvent> find(String principal, Date after) {
                final List<PersistentAuditEvent> persistentAuditEvents;
                if (principal == null && after == null) {
                    persistentAuditEvents = persistenceAuditEventRepository.findAll();
                } else if (after == null) {
                    persistentAuditEvents = persistenceAuditEventRepository.findByPrincipal(AuditUtils.getPrincipal());
                } else {
                    persistentAuditEvents =
                            persistenceAuditEventRepository.findByPrincipalAndAuditEventDateGreaterThan(AuditUtils.getPrincipal(), new LocalDateTime(after));
                }

                return auditEventConverter.convertToAuditEvent(persistentAuditEvents);
            }

            @Override
            public void add(AuditEvent event) {
                PersistentAuditEvent persistentAuditEvent = new PersistentAuditEvent();
                persistentAuditEvent.setPrincipal(AuditUtils.getPrincipal());
                persistentAuditEvent.setAuditEventType(event.getType());
                persistentAuditEvent.setAuditEventDate(event.getTimestamp());
                persistentAuditEvent.setData(auditEventConverter.convertDataToStrings(event.getData()));

                persistenceAuditEventRepository.save(persistentAuditEvent);
            }

        };
    }
}
