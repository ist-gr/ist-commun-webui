package gr.com.ist.commun.core.domain.security.audit.entity;


import gr.com.ist.commun.core.domain.security.User;

import java.util.Date;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.envers.DefaultTrackingModifiedEntitiesRevisionEntity;
import org.hibernate.envers.ModifiedEntityNames;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

@Entity
@RevisionEntity(AppRevisionListener.class)
@Table(name="REVISIONS")
@SequenceGenerator(name="seq",sequenceName="APP_ENTITY_REVISION_SEQ")
public class AppEntityRevision {
    
    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE,generator="seq")
    @RevisionNumber
    private long id;

    @RevisionTimestamp
    private long timestamp;

    public AppEntityRevision() {
       
    }

    @ManyToOne
    private User user;

    @ElementCollection(fetch=FetchType.EAGER)
    @JoinTable(name = "REVISION_ENTITIES", joinColumns = @JoinColumn(name = "REV"))
    @Column(name = "ENTITY_NAME")
    @ModifiedEntityNames
    private Set<String> modifiedEntityNames;
    
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    @Transient
    public Date getRevisionDate() {
        return new Date(timestamp);
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
    
    public Set<String> getModifiedEntityNames() {
        return modifiedEntityNames;
    }

    public void setModifiedEntityNames(Set<String> modifiedEntityNames) {
        this.modifiedEntityNames = modifiedEntityNames;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DefaultTrackingModifiedEntitiesRevisionEntity)) return false;
        if (!super.equals(o)) return false;

        AppEntityRevision that = (AppEntityRevision) o;

        if (modifiedEntityNames != null ? !modifiedEntityNames.equals(that.modifiedEntityNames)
                                        : that.modifiedEntityNames != null) return false;

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (modifiedEntityNames != null ? modifiedEntityNames.hashCode() : 0);
        return result;
    }

    public String toString() {
        return this.getClass().getSimpleName()+"(" + super.toString() + ", modifiedEntityNames = " + modifiedEntityNames + ")";
    }
    
}
