package gr.com.ist.commun.core.repository.security;

import gr.com.ist.commun.core.domain.security.User;
import gr.com.ist.commun.core.repository.SearchableByTerm;
import gr.com.ist.commun.persistence.jpa.AppRepository;

import java.util.UUID;

import org.springframework.data.repository.query.Param;

public interface UsersRepository extends AppRepository<User, UUID, Long>, SearchableByTerm<User> {
    User findByUsername(@Param("username") String value);
}
