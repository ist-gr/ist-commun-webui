package gr.com.ist.commun.core.repository.security;

import gr.com.ist.commun.core.domain.security.RoleGroup;
import gr.com.ist.commun.core.repository.SearchableByTerm;
import gr.com.ist.commun.persistence.jpa.AppRepository;

import java.util.UUID;

public interface RoleGroupsRepository extends AppRepository<RoleGroup, UUID, Long>, SearchableByTerm<RoleGroup> {
}
