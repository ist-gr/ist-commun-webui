package gr.com.ist.commun.seqgen;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class SequenceGenerator {
   
    @Id
    @Column(length=254)
    private String key;
    private Integer lastAssignedValue;
    
    public String getKey() {
        return key;
    }
    public void setKey(String key) {
        this.key = key;
    }
    public Integer getLastAssignedValue() {
        return lastAssignedValue;
    }
    public void setLastAssignedValue(Integer lastAssignedValue) {
        this.lastAssignedValue = lastAssignedValue;
    }
}
