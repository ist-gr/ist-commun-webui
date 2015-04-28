package gr.com.ist.commun.core.repository.security.audit;

import gr.com.ist.commun.core.domain.TimePeriod;
import gr.com.ist.commun.core.domain.security.User;
import gr.com.ist.commun.core.domain.security.audit.AuditEvent;

import java.io.Serializable;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
@Deprecated
public interface AuditableJpaRepository<T, ID extends Serializable> {
    Page<AuditEvent> findEntityCreateAuditEvents(TimePeriod period, User actor, Pageable pageable);
}
