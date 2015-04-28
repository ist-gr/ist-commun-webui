package gr.com.ist.commun.core.domain.security;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.persistence.Embeddable;
import javax.persistence.Transient;

import org.springframework.context.MessageSourceResolvable;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Embeddable
public class AuthorityRole extends Role implements MessageSourceResolvable {
    private static final long serialVersionUID = 538978904210746828L;
    private String authority;
    private String localizedAuthority;

    public AuthorityRole() {
        this.setId(UUID.randomUUID());
    }
    
    @JsonIgnore
    @Transient
    private Set<AuthorityRole> includedAuthorities;

    @JsonIgnore
    @Override
    public boolean isSystemDefined() {
        return true;
    }

    @Override
    public String getAuthority() {
        return this.authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    @Override
    @JsonIgnore
    public Set<? extends Role> getIncludedRoles() {
        return includedAuthorities;
    }

    @Override
    @JsonIgnore
    void setIncludedRoles(Set<? extends Role> includedRoles) {
        Set<AuthorityRole> includedAuthorities = new HashSet<AuthorityRole>();
        for (Role role : includedRoles) {
            if (role instanceof AuthorityRole) {
                includedAuthorities.add((AuthorityRole) role);
            }
        }
        this.includedAuthorities = includedAuthorities;
    }

    public Set<AuthorityRole> getIncludedAuthorities() {
        return includedAuthorities;
    }

    public void setIncludedAuthorities(Set<AuthorityRole> includedAuthorities) {
        this.includedAuthorities = includedAuthorities;
    }

    @Override
    public String[] getCodes() {
        return new String[] {this.authority};
    }

    @Override
    public Object[] getArguments() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getDefaultMessage() {
        return this.authority;
    }
    
    public String getLocalizedAuthority() {
        return localizedAuthority;
    }
    
    public void setLocalizedAuthority(String localizedAuthority) {
        this.localizedAuthority = localizedAuthority;
    }
    //XXX: hashCode and equals are not calling super to avoid confusion on entity base id's. If not Maps and Sets will end up containing duplicates
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((authority == null) ? 0 : authority.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (getClass() != obj.getClass())
            return false;
        AuthorityRole other = (AuthorityRole) obj;
        if (authority == null) {
            if (other.authority != null)
                return false;
        } else if (!authority.equals(other.authority))
            return false;
        return true;
    }
    
}
