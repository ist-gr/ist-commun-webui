package gr.com.ist.commun.core.domain.security;

import gr.com.ist.commun.core.domain.TimePeriod;
import gr.com.ist.commun.core.service.TimeService;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.envers.Audited;
import org.hibernate.validator.constraints.Email;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Represents a user who has access to the system. 
 * 
 * @author apostolakis.antonis@gmail.com
 *
 */
@Entity
@Configurable(dependencyCheck=true)
@SuppressWarnings("serial")
@Table(name="USERS")
@Audited
public class User extends EntityBase implements UserDetails {
    static final String ADMIN_AUTHORITY_NAME = "ADMIN";

//    @Embeddable
//	public static class UserPreferences {
//		@Lob
//		private byte[] userPicture; 
//		private Theme defaultTheme;
//		private LandingPage defaultLandingPage;
//		private Application defaultApplication;
//		private Language defaultLanguage;
//		private Integer maximumListViewRecords;
//		private String defaultViews;
//		private String defaultDashboards;
//	}

    // ====== services ======

	@Transient
	@JsonIgnore
    private TimeService timeService;

    // ====== fields ======
    
    @Column(unique = true, length=50, nullable=false)
    private String username;
    @Column(length=60, nullable=false)
    private String password;
    private Date passwordExpiresOn;
    @Column(name="IS_DISABLED")
    private Boolean disabled;
    @Column(name="IS_LOCKED")
    private Boolean locked;
    /**
     * the time period that the user account is valid for
     */
    @Embedded
    private TimePeriod validFor;
    
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "USER_ROLE_GROUPS",
        joinColumns = @JoinColumn(name = "USER_ID", referencedColumnName = "ID"), 
        inverseJoinColumns = @JoinColumn(name = "ROLE_GROUP_ID", referencedColumnName = "ID"))
    @JsonIgnoreProperties({"createdBy", "lastModifiedBy", "includedRoleGroups", "includedAuthorities"})
    private Set<RoleGroup> roles = new HashSet<RoleGroup>();
    @Column(name="IS_ADMIN")
    private Boolean admin;
    
    @Column(length=30)
    private String firstName;
    @Column(length=30)
    private String lastName;
    @Column(length=60)
    private String fullName;
    @Column(length=50)
    @Email
    private String email;
    @Column(length=50, nullable=true)
    private String avatar;
    
    @Transient
    private Set<? extends Role> grantedAuthorities;
    
    // ====== methods ======
    
    public TimeService getTimeService() {
        return timeService;
    }
    @Autowired(required=true)
    public void setTimeService(TimeService timeService) {
        this.timeService = timeService;
    }
    @JsonIgnore
    public Set<? extends Role> getGrantedAuthorities() {
        return grantedAuthorities;
    }
    @JsonIgnore
    public void setGrantedAuthorities(Set<? extends Role> grantedAuthorities) {
        this.grantedAuthorities = grantedAuthorities;
    }
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (this.admin != null && this.admin) {
            Set<SimpleGrantedAuthority> authorities = new HashSet<SimpleGrantedAuthority>();
            authorities.add(new SimpleGrantedAuthority(ADMIN_AUTHORITY_NAME));
            return authorities;
        }
        return grantedAuthorities;
    }
    @Override
    public String getPassword() {
        return this.password;
    }
    @Override
    public String getUsername() {
        return this.username;
    }
    @Override
    @JsonIgnore
    public boolean isAccountNonExpired() {
        return validFor == null 
                || ((validFor.getStartDateTime() == null || validFor.getStartDateTime().getTime() <= timeService.currentTimeMillis())
                        && (validFor.getEndDateTime() == null || validFor.getEndDateTime().getTime() >= timeService.currentTimeMillis()));
    }
    
    @Override
    @JsonIgnore
    public boolean isAccountNonLocked() {
        return locked == null || Boolean.FALSE.equals(locked);
    }
    @Override
    @JsonIgnore
    public boolean isCredentialsNonExpired() {
        return passwordExpiresOn == null || passwordExpiresOn.getTime() > timeService.currentTimeMillis();
    }
    @Override
    @JsonIgnore
    public boolean isEnabled() {
        return disabled == null || Boolean.FALSE.equals(disabled);
    }
    public Date getPasswordExpiresOn() {
        return passwordExpiresOn;
    }
    public void setPasswordExpiresOn(Date passwordExpiresOn) {
        this.passwordExpiresOn = passwordExpiresOn;
    }
    public Boolean isDisabled() {
        return disabled;
    }
    public Boolean getDisabled() {
        return disabled;
    }
    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }
    public Boolean isLocked() {
        return locked;
    }
    public void setLocked(Boolean locked) {
        this.locked = locked;
    }
    public TimePeriod getValidFor() {
        return validFor;
    }
    public void setValidFor(TimePeriod validFor) {
        this.validFor = validFor;
    }
    public Set<RoleGroup> getRoles() {
        return roles;
    }
    public void setRoles(Set<RoleGroup> roles) {
        this.roles = roles;
    }
    public Boolean isAdmin() {
        return admin;
    }
    public Boolean getAdmin() {
        return admin;
    }
    public void setAdmin(Boolean admin) {
        this.admin = admin;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public void setPassword(String password) {
        if (password != null) {
            this.password = password;
        }
    }
    public String getFirstName() {
		return firstName;
	}
	public void setFirstName(String firstName) {
		boolean fnIsCalculated = areEqual(this.firstName, this.lastName, this.fullName);
		this.firstName = firstName;
		if (fnIsCalculated) {
			this.fullName = calculatedFn(this.firstName, this.lastName);
		}
	}
	private boolean areEqual(String firstName, String lastName, String fullName) {
		String fn = calculatedFn(firstName, lastName);
		return fn == fullName || (fn != null && fn.equals(fullName));  
	}
	private String calculatedFn(String firstName, String lastName) {
		return firstName + " " + lastName;
 	}
    public String getLastName() {
		return lastName;
	}
	public void setLastName(String lastName) {
		boolean fnIsCalculated = areEqual(this.firstName, this.lastName, this.fullName);
		this.lastName = lastName;
		if (fnIsCalculated) {
			this.fullName = calculatedFn(this.firstName, this.lastName);
		}
	}
    public String getFullName() {
		return fullName;
	}
	public void setFullName(String fullName) {
		this.fullName = fullName;
	}
    public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public Boolean getLocked() {
		return locked;
	}

    /**
     * @return the avatar
     */
    public String getAvatar() {
        return avatar;
    }

    /**
     * @param avatar the avatar to set
     */
    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public static final java.util.regex.Pattern BCRYPT_PATTERN = java.util.regex.Pattern.compile("\\A\\$2a?\\$\\d\\d\\$[./0-9A-Za-z]{53}");
    @PrePersist
    @PreUpdate
    void encryptPasswordField() {
        // Workaround for cases where the caller erroneously calls the setter with the already encoded password
        // XXX We hope that the BCRYPT_PATTERN will remain so in the future (we have a unit test for possible regression)
        if (!BCRYPT_PATTERN.matcher(password).matches()) {
            PasswordEncoder encoder = new BCryptPasswordEncoder();
            String encodedPassword = encoder.encode(password);
            this.password = encodedPassword;
        }
    }
    
    
}
