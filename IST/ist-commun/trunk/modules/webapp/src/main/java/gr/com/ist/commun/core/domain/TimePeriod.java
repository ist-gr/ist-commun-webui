package gr.com.ist.commun.core.domain;

import java.util.Date;

import javax.persistence.Embeddable;

/**
 * A base / value object used to represent a period of time, between two time points.
 * 
 * @see http://www.tmforum.org/Models/Frameworx13/content/_3E3F0EC000E93C2319F50258-content.html
 *
 * @version $Rev$ $Date$
 */
@Embeddable
public class TimePeriod {
	private Date startDateTime;
    private Date endDateTime;
    public TimePeriod() {
		super();
	}
	public TimePeriod(Date startDateTime, Date endDateTime) {
		super();
		this.startDateTime = startDateTime;
		this.endDateTime = endDateTime;
	}
    public Date getStartDateTime() {
        return startDateTime;
    }
    public void setStartDateTime(Date startDateTime) {
        this.startDateTime = startDateTime;
    }
    public Date getEndDateTime() {
        return endDateTime;
    }
    public void setEndDateTime(Date endDateTime) {
        this.endDateTime = endDateTime;
    }
    
    /**
     * Given two TimePeriod objects it returns true if they are overlapping
     * 
     * @param validityPeriod
     */
    public boolean overlaps(TimePeriod other) {
        long ast = this.getStartDateTime() == null ? Long.MIN_VALUE : this.getStartDateTime().getTime();
        long aet = this.getEndDateTime() == null ? Long.MAX_VALUE : this.getEndDateTime().getTime();
        long bst = other.getStartDateTime() == null ? Long.MIN_VALUE : other.getStartDateTime().getTime();
        long bet = other.getEndDateTime() == null ? Long.MAX_VALUE : other.getEndDateTime().getTime();
        return (ast<bet && bst<aet);
    }
    @Override
    public String toString() {
        return startDateTime + "-" + endDateTime;
    }
    public boolean includes(Date aDate) {
        long st = this.getStartDateTime() == null ? Long.MIN_VALUE : this.getStartDateTime().getTime();
        long et = this.getEndDateTime() == null ? Long.MAX_VALUE : this.getEndDateTime().getTime();
        return st <= aDate.getTime() &&  
                aDate.getTime() <= et;
    }
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((endDateTime == null) ? 0 : endDateTime.hashCode());
        result = prime * result + ((startDateTime == null) ? 0 : startDateTime.hashCode());
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
        TimePeriod other = (TimePeriod) obj;
        if (endDateTime == null) {
            if (other.endDateTime != null)
                return false;
        } else if (!endDateTime.equals(other.endDateTime))
            return false;
        if (startDateTime == null) {
            if (other.startDateTime != null)
                return false;
        } else if (!startDateTime.equals(other.startDateTime))
            return false;
        return true;
    }
}
