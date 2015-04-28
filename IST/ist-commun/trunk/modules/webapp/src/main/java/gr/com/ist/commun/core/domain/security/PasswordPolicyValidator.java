package gr.com.ist.commun.core.domain.security;

import static gr.com.ist.commun.utils.JpaUtils.propName;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class PasswordPolicyValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return PasswordPolicy.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        Assert.notNull(target);
        Assert.isAssignable(PasswordPolicy.class, target.getClass());
        PasswordPolicy passwordPolicy = (PasswordPolicy) target;
        //TODO: make it a global error when task "Support reporting global validation errors from rest API" is fixed
        if (passwordPolicy.getMaximumLength() < passwordPolicy.getMinimumLength()) {
            errors.rejectValue(propName(QPasswordPolicy.passwordPolicy.maximumLength),"conflictingLengths", "Maximum password length should not be less than minimum password length");
        }
        Integer sumOfMinRequirements = (passwordPolicy.getMinNumberOfCapitalCase() == null ? 0 : passwordPolicy.getMinNumberOfCapitalCase()) +
                (passwordPolicy.getMinNumberOfDigits() == null ? 0 : passwordPolicy.getMinNumberOfDigits()) +
                (passwordPolicy.getMinNumberOfLowerCase() == null ? 0 : passwordPolicy.getMinNumberOfLowerCase()) +
                (passwordPolicy.getMinNumberOfSpecials() == null ? 0 : passwordPolicy.getMinNumberOfSpecials());
        
        if (sumOfMinRequirements > passwordPolicy.getMinimumLength()) {
            errors.rejectValue(propName(QPasswordPolicy.passwordPolicy.minimumLength), "conflictingPolicies", "The sum of minimum requirements should be less than or equal to minimum password length");
        }
    }
}
