package gr.com.ist.commun.core.domain.security.audit.entity;

import com.fasterxml.jackson.annotation.JsonFilter;

@JsonFilter("collectionModificationFilter")
public class PropertyChange {

    private String path;

    private Object oldValue;

    private Object newValue;

    private Object added;

    private Object removed;

    public PropertyChange() {}

    public PropertyChange(String path, Object newValue, Object oldValue) {
        super();
        this.path = path;
        this.newValue = newValue;
        this.oldValue = oldValue;
    }
    
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Object getOldValue() {
        return oldValue;
    }

    public void setOldValue(Object oldValue) {
        this.oldValue = oldValue;
    }

    public Object getNewValue() {
        return newValue;
    }

    public void setNewValue(Object newValue) {
        this.newValue = newValue;
    }

    public Object getAdded() {
        return added;
    }

    public void setAdded(Object added) {
        this.added = added;
    }

    public Object getRemoved() {
        return removed;
    }

    public void setRemoved(Object removed) {
        this.removed = removed;
    }
}