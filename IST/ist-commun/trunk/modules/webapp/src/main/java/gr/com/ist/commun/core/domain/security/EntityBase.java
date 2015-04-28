package gr.com.ist.commun.core.domain.security;


import static gr.com.ist.commun.utils.SecurityUtils.getCurrentUser;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Version;

import org.apache.commons.lang3.builder.ToStringBuilder;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

@MappedSuperclass
// @JsonIdentityInfo directs Jackson JSON serializer to replace references to entities inherited from EnittyBase with the ids of these entities
@JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class, property="id")
public abstract class EntityBase  {
    ////////////////////
    // standard fields
    ////////////////////
    
    @Id
//    @GenericGenerator(name = "uuid", strategy = "uuid2")
//    @GeneratedValue(generator = "uuid")
    @Column(length=16)
    private UUID id;

    @Version
    private Integer version;

    @JoinColumn(name="CREATED_BY_USER_ID")
    @ManyToOne(targetEntity=User.class, fetch=FetchType.LAZY)
    @JsonIgnoreProperties({"createdBy", "lastModifiedBy", "roles", "authorities"})
    private User createdBy;

    private Date createdOn;

    @JoinColumn(name="LAST_MODIFIED_BY_USER_ID")
    @ManyToOne(targetEntity=User.class, fetch=FetchType.LAZY)
    @JsonIgnoreProperties({"createdBy", "lastModifiedBy", "roles", "authorities"})
    private User lastModifiedBy;

    private Date lastModifiedOn;

    public UUID getId() {
        return this.id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }

    @JsonProperty
    public Integer getVersion() {
        return this.version;
    }

    @JsonIgnore
    public void setVersion(Integer version) {
        this.version = version;
    }

    @JsonProperty
    public User getCreatedBy() {
        return createdBy;
    }

    @JsonIgnore
    public void setCreatedBy(User createdBy) {
        if (this.createdBy == null) {
            this.createdBy = createdBy;
        }
    }

    @JsonProperty
    public Date getCreatedOn() {
        return createdOn;
    }

    @JsonIgnore
    public void setCreatedOn(Date createdOn) {
        if (this.createdOn == null) {
            this.createdOn = createdOn;
        }
    }

    @JsonProperty
    public User getLastModifiedBy() {
        return lastModifiedBy;
    }

    @JsonIgnore
    public void setLastModifiedBy(User lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    @JsonProperty
    public Date getLastModifiedOn() {
        return lastModifiedOn;
    }

    @JsonIgnore
    public void setLastModifiedOn(Date lastModifiedOn) {
        this.lastModifiedOn = lastModifiedOn;
    }

    @PreUpdate
    void populateAuditableOnPreUpdate() {
        lastModifiedOn = new Date();
        lastModifiedBy = getCurrentUser();
    }
   
    @PrePersist
    void populateIdAndAuditableOnPreCreate() {
        if (this.id == null) {
            // TODO consider a statistically guaranteed unique id by using a UUID version 1 (node identifier (MAC address), timestamp and a random seed) e.g. using http://commons.apache.org/sandbox/commons-id/uuid.html 
            this.id = UUID.randomUUID();
        }
        Date now = new Date();
        createdOn = now;
        User currentUser = getCurrentUser();
        createdBy = currentUser;
        lastModifiedBy = currentUser;
        lastModifiedOn = now;
    }
    
    /**
     * hashCode() and equals() are implemented with regard to id only, according
     * to:
     * <ol>
     * <li>definition of entity in:
     * http://en.wikipedia.org/wiki/Domain_driven_design</li>
     * <li>http://stackoverflow.com/questions/5031614/the-jpa-hashcode-equals-
     * dilemma</li>
     * </ol>
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        EntityBase other = (EntityBase) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
	
}
