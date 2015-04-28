package gr.com.ist.commun.core.repository.security;

import gr.com.ist.commun.core.domain.security.PasswordPolicy;
import gr.com.ist.commun.persistence.jpa.AppRepository;

import java.util.UUID;

import org.springframework.data.repository.query.Param;

public interface PasswordPoliciesRepository extends AppRepository<PasswordPolicy, UUID, Long> {

    PasswordPolicy findByIsActive(@Param("isActive") Boolean isActive);
}
