package gr.com.ist.commun.core.domain.security;

import java.util.Set;

import org.springframework.security.core.GrantedAuthority;

public abstract class Role extends EntityBase implements GrantedAuthority {
    
    private static final long serialVersionUID = -168441409651459900L;

    abstract public Set<? extends Role> getIncludedRoles();

    abstract void setIncludedRoles(Set<? extends Role> includedRoles);
    
    abstract public boolean isSystemDefined();
}
