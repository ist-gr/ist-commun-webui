package gr.com.ist.commun.core.domain.security.audit.entity;

import gr.com.ist.commun.core.domain.security.User;

import java.util.Date;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

public class RevisionChange {

    @JsonIgnoreProperties({"createdBy", "lastModifiedBy", "roles", "authorities"})
    private User who;

    private Date when;

    private long id;
    
    private Set<String> modifiedEntityNames;

    private List<EntityChange> entityChanges;
    
    private int totalEntityChanges;

    public User getWho() {
        return who;
    }
    public void setWho(User who) {
        this.who = who;
    }
    public Date getWhen() {
        return when;
    }
    public void setWhen(Date when) {
        this.when = when;
    }
    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }
    public Set<String> getModifiedEntityNames() {
        return modifiedEntityNames;
    }
    public void setModifiedEntityNames(Set<String> modifiedEntityNames) {
        this.modifiedEntityNames = modifiedEntityNames;
    }
    public List<EntityChange> getEntityChanges() {
        return entityChanges;
    }
    public void setEntityChanges(List<EntityChange> entityChanges) {
        this.entityChanges = entityChanges;
    }
    public int getTotalEntityChanges() {
        return totalEntityChanges;
    }
    public void setTotalEntityChanges(int totalEntityChanges) {
        this.totalEntityChanges = totalEntityChanges;
    }
}