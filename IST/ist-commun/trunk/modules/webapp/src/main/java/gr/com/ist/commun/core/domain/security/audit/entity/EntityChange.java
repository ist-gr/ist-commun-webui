package gr.com.ist.commun.core.domain.security.audit.entity;

import java.util.List;
import java.util.UUID;

import org.hibernate.envers.RevisionType;

public class EntityChange {
    
    private String entityName;
    
    private UUID entityId;
    
    private RevisionType revisionType;

    private List<PropertyChange> propertyChanges;
    
    public String getEntityName() {
        return entityName;
    }
    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }
    public UUID getEntityId() {
        return entityId;
    }
    public void setEntityId(UUID entityId) {
        this.entityId = entityId;
    }
    public RevisionType getRevisionType() {
        return revisionType;
    }
    public void setRevisionType(RevisionType revisionType) {
        this.revisionType = revisionType;
    }
    public List<PropertyChange> getPropertyChanges() {
        return propertyChanges;
    }
    public void setPropertyChanges(List<PropertyChange> propertyChanges) {
        this.propertyChanges = propertyChanges;
    }
}
