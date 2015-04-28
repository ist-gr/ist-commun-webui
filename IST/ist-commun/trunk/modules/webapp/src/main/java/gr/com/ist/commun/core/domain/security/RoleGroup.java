package gr.com.ist.commun.core.domain.security;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;

import org.hibernate.envers.Audited;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity(name = "ROLE_GROUPS")
@Audited
public class RoleGroup extends Role {

    private static final long serialVersionUID = 6278053476098780474L;

    @Column(name="ROLE_NAME", nullable=false, length=50, unique=true)
    private String name;
    
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "ROLE_ROLEGROUPS",
        joinColumns = @JoinColumn(name = "ROLE_GROUP_ID", referencedColumnName = "ID"), 
        inverseJoinColumns = @JoinColumn(name = "INCLUDED_ROLE_GROUP_ID", referencedColumnName = "ID"))
    private Set<RoleGroup> includedRoleGroups;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(name = "ROLE_AUTHORITIES",
    joinColumns = @JoinColumn(name = "ROLE_GROUP_ID", referencedColumnName = "ID"))
    private Set<AuthorityRole> includedAuthorities;
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    @JsonIgnore
    public boolean isSystemDefined() {
        return false;
    }

    @JsonIgnore
    @Override
    public String getAuthority() {
        return null;
    }

    @Override
    @JsonIgnore
    public Set<Role> getIncludedRoles() {
        Set<Role> allRoles = new HashSet<Role>();
        if (includedRoleGroups != null) {
            allRoles.addAll(includedRoleGroups);
        }
        if (includedAuthorities != null) {
            allRoles.addAll(includedAuthorities);
        }
        return allRoles;
    }

    @Override
    @JsonIgnore
    void setIncludedRoles(Set<? extends Role> includedRoles) {
        Set<RoleGroup> roleGroups = new HashSet<RoleGroup>();
        Set<AuthorityRole> authorities = new HashSet<AuthorityRole>(); 
        for (Role role : includedRoles) {
            if (role instanceof RoleGroup) {
                roleGroups.add((RoleGroup) role); 
            } else {
                authorities.add((AuthorityRole) role);
            }
        }
        this.includedRoleGroups = roleGroups;
        this.includedAuthorities = authorities;
    }

    public Set<RoleGroup> getIncludedRoleGroups() {
        return includedRoleGroups;
    }

    public void setIncludedRoleGroups(Set<RoleGroup> includedRoleGroups) {
        this.includedRoleGroups = includedRoleGroups;
    }

    public Set<AuthorityRole> getIncludedAuthorities() {
        return includedAuthorities;
    }

    public void setIncludedAuthorities(Set<AuthorityRole> includedAuthorities) {
        this.includedAuthorities = includedAuthorities;
    }

}
