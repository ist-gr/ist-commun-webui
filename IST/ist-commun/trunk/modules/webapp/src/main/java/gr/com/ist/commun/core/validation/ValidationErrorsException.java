package gr.com.ist.commun.core.validation;

import org.springframework.util.Assert;
import org.springframework.validation.Errors;

@SuppressWarnings("serial")
public class ValidationErrorsException extends RuntimeException {
    
    private Errors errors;
    private Object partialResult;  
    
    public ValidationErrorsException(Errors errors, Object partialResult) {
        Assert.notNull(errors, "Errors must not be null");
        this.errors = errors;
        this.partialResult = partialResult;
    }

    public Errors getErrors() {
        return errors;
    }

    public Object getPartialResult() {
        return partialResult;
    }

    /**
     * Returns diagnostic information about the errors held in this object.
     */
    @Override
    public String getMessage() {
        return this.errors.toString();
    }

    @Override
    public boolean equals(Object other) {
        return (this == other || this.errors.equals(other));
    }

    @Override
    public int hashCode() {
        return this.errors.hashCode();
    }
    
}
