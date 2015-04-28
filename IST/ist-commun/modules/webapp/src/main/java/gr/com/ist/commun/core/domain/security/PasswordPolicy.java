package gr.com.ist.commun.core.domain.security;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.hibernate.envers.Audited;

/**
 * See http://en.wikipedia.org/wiki/Password_policy
 * 
 *
 * @version $Rev$ $Date$
 */
@Entity
@Audited
public class PasswordPolicy extends EntityBase {

    @Column(unique = true)
    private Boolean isActive;
    @NotNull
    @Min(value=4)
    private Integer minimumLength;
    @NotNull
    @Max(value=20)
    private Integer maximumLength;
    
    private Integer minNumberOfDigits;
    
    private Integer minNumberOfLowerCase;
    
    private Integer minNumberOfCapitalCase;
    
    private Integer minNumberOfSpecials;

    /**
     * @return the isActive
     */
    public Boolean getIsActive() {
        return isActive;
    }
    /**
     * @param isActive the isActive to set
     */
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    /**
     * @return the minimumLength
     */
    public Integer getMinimumLength() {
        return minimumLength;
    }
    /**
     * @param minimumLength the minimumLength to set
     */
    public void setMinimumLength(Integer minimumLength) {
        this.minimumLength = minimumLength;
    }
    /**
     * @return the maximumLength
     */
    public Integer getMaximumLength() {
        return maximumLength;
    }
    /**
     * @param maximumLength the maximumLength to set
     */
    public void setMaximumLength(Integer maximumLength) {
        this.maximumLength = maximumLength;
    }
    /**
     * 
     * @return the minimum number of digits a password should contain
     */
    public Integer getMinNumberOfDigits() {
        return minNumberOfDigits;
    }
    /**
     * the minimum number of digits a password should contain
     * @param minNumberOfDigits
     */
    public void setMinNumberOfDigits(Integer minNumberOfDigits) {
        this.minNumberOfDigits = minNumberOfDigits;
    }
    /**
     * 
     * @return the minimum number of lower case characters a password should contain
     */
    public Integer getMinNumberOfLowerCase() {
        return minNumberOfLowerCase;
    }
    /**
     * the minimum number of lower case characters a password should contain
     * @param minNumberOfLowerCase
     */
    public void setMinNumberOfLowerCase(Integer minNumberOfLowerCase) {
        this.minNumberOfLowerCase = minNumberOfLowerCase;
    }
    /**
     * the minimum number of capital case characters a password should contain
     * @return
     */
    public Integer getMinNumberOfCapitalCase() {
        return minNumberOfCapitalCase;
    }
    /**
     * the minimum number of capital case characters a password should contain
     * @param minNumberOfCapitalCase
     */
    public void setMinNumberOfCapitalCase(Integer minNumberOfCapitalCase) {
        this.minNumberOfCapitalCase = minNumberOfCapitalCase;
    }
    /**
     * the minimum number of special characters a password should contain
     * @return
     */
    public Integer getMinNumberOfSpecials() {
        return minNumberOfSpecials;
    }
    /**
     * the minimum number of special characters a password should contain
     * @param minNumberOfSpecials
     */
    public void setMinNumberOfSpecials(Integer minNumberOfSpecials) {
        this.minNumberOfSpecials = minNumberOfSpecials;
    }
    
    @PreUpdate
    @PrePersist
    void setIsActiveToNullWhenFalse() {
        if (this.isActive != null && this.isActive.equals(Boolean.FALSE)) {
            this.isActive = null;
        }
    }

    /*
     * TODO consider adding more like: 
     * <ul> 
     * <li>case sensitive</li>
     * <li>the use of both upper- and
     * lower-case letters</li> 
     * <li>inclusion of one
     * or more numerical digits</li> 
     * <li>inclusion of special characters, e.g.
     * @, #, $ etc.</li>
     * <li>password duration</li>
     * <li>create the password for the users or let the user select one of a limited number of displayed choices.</li> 
     * </ul>
     */
    
    
}
