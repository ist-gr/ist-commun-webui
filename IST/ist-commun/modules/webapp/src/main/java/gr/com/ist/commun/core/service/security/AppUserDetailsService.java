package gr.com.ist.commun.core.service.security;

import gr.com.ist.commun.core.domain.security.AuthorityRole;
import gr.com.ist.commun.core.domain.security.Role;
import gr.com.ist.commun.core.domain.security.User;
import gr.com.ist.commun.core.repository.security.AuthoritiesRepository;
import gr.com.ist.commun.core.repository.security.UsersRepository;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.security.core.SpringSecurityMessageSource;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class AppUserDetailsService implements UserDetailsService {
    private static Logger LOG = LoggerFactory.getLogger(AppUserDetailsService.class);
    
    @Autowired
    private UsersRepository usersRepository;
    
    @Autowired
    private AuthoritiesRepository authoritiesRepository;

    private MessageSourceAccessor messages = SpringSecurityMessageSource.getAccessor();
    
    @Override
    public User loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = usersRepository.findByUsername(username);
        if (user == null) {
            LOG.debug("Users repository returned no results for user '" + username + "'");
            throw new UsernameNotFoundException(
                    messages.getMessage("JdbcDaoImpl.notFound", new Object[]{username}, "Username {0} not found"));
        }
        // TODO Find a way to avoid iterating through the user roles every time this method is invoked 
        Set<AuthorityRole> authorities = new HashSet<AuthorityRole>();
        //find all the complex roles of a user if any, and replace them with the included leaf roles
        Set<AuthorityRole> extraAuths = new HashSet<AuthorityRole>();
        if (user.getRoles() != null) {
            accumulateAuthorities(user.getRoles(), authorities);

            //FIXME: remove hardcoded references and find a way to auto scan custom controlers for references
            for (AuthorityRole authority : authorities) {
                if (authority.getAuthority().equals("aircraftMovements_viewer")) {
                    extraAuths = authoritiesRepository.getIncludedAuthoritiesByParentName("aircraftMovements_editor");
                }
            }
            authorities.addAll(extraAuths);
            user.setGrantedAuthorities(authorities);
        }
        return user;
    }

    private void accumulateAuthorities(Set<? extends Role> roles, Set<AuthorityRole> authorities) {
        for (Role role : roles) {
            if (role.getIncludedRoles() != null && !role.getIncludedRoles().isEmpty() && !role.isSystemDefined()) {
                accumulateAuthorities(role.getIncludedRoles(), authorities);
            } else if (role.isSystemDefined()){
                for (AuthorityRole authRole : authoritiesRepository.getAuthorities()) {
                    if (authRole.getAuthority().equals(((AuthorityRole) role).getAuthority())) {
                        authorities.add(authRole);
                        if (authRole.getIncludedAuthorities() != null && !authRole.getIncludedAuthorities().isEmpty()) {
                            accumulateAuthorities(authRole.getIncludedAuthorities(), authorities);
                        } else {
                            authorities.add(authRole);
                        }
                    }
                }
            }
        }
    }
}
