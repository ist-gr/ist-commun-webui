package gr.com.ist.commun.core.repository.security;

import java.util.Set;

import gr.com.ist.commun.core.domain.security.AuthorityRole;
import gr.com.ist.commun.core.repository.SearchableByTerm;

public interface AuthoritiesRepository extends SearchableByTerm<AuthorityRole> {
    public Set<AuthorityRole> getAuthorities();
    public Set<AuthorityRole> getIncludedAuthoritiesByParentName(String name);
}
