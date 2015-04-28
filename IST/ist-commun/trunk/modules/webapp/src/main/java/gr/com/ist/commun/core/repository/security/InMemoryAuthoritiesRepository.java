package gr.com.ist.commun.core.repository.security;

import gr.com.ist.commun.core.domain.security.AuthorityRole;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class InMemoryAuthoritiesRepository implements AuthoritiesRepository {
    
    private class AuthorityRoleComparator implements Comparator<AuthorityRole> {

        @Override
        public int compare(AuthorityRole auth1, AuthorityRole auth2) {
           return auth1.getLocalizedAuthority().compareTo(auth2.getLocalizedAuthority());
        }
        
    }
    @Autowired
    MessageSource messageSource;
    
    private Set<AuthorityRole> authorities;

    @Override
    public Page<AuthorityRole> searchTerm(String term, Pageable pageable) {
        Locale currentLocale = LocaleContextHolder.getLocale();
        for (AuthorityRole authRole : authorities) {
            authRole.setLocalizedAuthority(messageSource.getMessage(authRole, currentLocale) + " (" + authRole.getAuthority() + ")");
        }
        if (term == null) {
            List<AuthorityRole> allAuthorityRoles = new ArrayList<AuthorityRole>();
            allAuthorityRoles.addAll(authorities);
            Collections.sort(allAuthorityRoles,new AuthorityRoleComparator());
            return new PageImpl<AuthorityRole>(allAuthorityRoles);
        } else {
            List<AuthorityRole> authorityRolePage = new ArrayList<AuthorityRole>();
            for (AuthorityRole authorityRole : authorities) {
                if (authorityRole.getLocalizedAuthority().toLowerCase().contains(term.toLowerCase())) {
                    authorityRolePage.add(authorityRole);
                }
            }
            Collections.sort(authorityRolePage,new AuthorityRoleComparator());
            return new PageImpl<AuthorityRole>(authorityRolePage);
        }
    }

    @Override
    public Set<AuthorityRole> getAuthorities() {
        return authorities;
    }
    
    @Override
    public Set<AuthorityRole> getIncludedAuthoritiesByParentName(String name) {
        for (AuthorityRole authority : authorities) {
            if (authority.getAuthority().equals(name)) {
                return authority.getIncludedAuthorities();
            }
        }
        return null;
    }

    public void setAuthorities(Set<AuthorityRole> authorities) {
        this.authorities = authorities;
    }
}
