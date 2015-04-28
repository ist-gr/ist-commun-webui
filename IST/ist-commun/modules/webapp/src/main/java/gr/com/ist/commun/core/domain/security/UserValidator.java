package gr.com.ist.commun.core.domain.security;

import static gr.com.ist.commun.utils.JpaUtils.propName;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import gr.com.ist.commun.core.repository.security.PasswordPoliciesRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class UserValidator implements Validator {

    @Autowired
    private PasswordPoliciesRepository passwordPoliciesRepository;

    @Override
    public boolean supports(Class<?> clazz) {
        return User.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        Assert.notNull(target);
        Assert.isAssignable(User.class, target.getClass());

        PasswordPolicy passwordPolicy = passwordPoliciesRepository.findByIsActive(true);
        User user = (User) target;
        String newPassword = user.getPassword();
        //If new password is BCRYPT encoded it means that user's password field is not being updated so there is no need to validate.
        if (!User.BCRYPT_PATTERN.matcher(newPassword).matches() && passwordPolicy != null){
            Integer minLength = passwordPolicy.getMinimumLength();
            Integer maxLength = passwordPolicy.getMaximumLength();
            if (newPassword.length() > maxLength || newPassword.length() < minLength) {
                errors.rejectValue(propName(QUser.user.password), "invalid.password", new String[] { minLength.toString(), maxLength.toString() }, "Invalid password");
            }
        }
        if (passwordPolicy != null) {
            if (passwordPolicy.getMinNumberOfCapitalCase() != null) {
                int minCapitalCounter = 0;
                for (int i = 0; i < newPassword.length(); i++) {
                    if (Character.isUpperCase(newPassword.charAt(i))) {
                        minCapitalCounter++;
                    }
                }
                if (minCapitalCounter < passwordPolicy.getMinNumberOfCapitalCase()) {
                    errors.rejectValue(propName(QUser.user.password), "invalid.password.capital", new String[] { passwordPolicy.getMinNumberOfCapitalCase().toString() }, "Invalid number of capital case characters");
                }
            }

            if (passwordPolicy.getMinNumberOfLowerCase() != null) {
                int minLowerCounter = 0;
                for (int i = 0; i < newPassword.length(); i++) {
                    if (Character.isLowerCase(newPassword.charAt(i))) {
                        minLowerCounter++;
                    }
                }
                if (minLowerCounter < passwordPolicy.getMinNumberOfLowerCase()) {
                    errors.rejectValue(propName(QUser.user.password), "invalid.password.lower", new String[] { passwordPolicy.getMinNumberOfLowerCase().toString() }, "Invalid number of lower case characters");
                }
            }

            if (passwordPolicy.getMinNumberOfDigits() != null) {
                int minNumericCounter = 0;
                for (int i = 0; i < newPassword.length(); i++) {
                    if (Character.isDigit(newPassword.charAt(i))) {
                        minNumericCounter++;
                    }
                }
                if (minNumericCounter < passwordPolicy.getMinNumberOfDigits()) {
                    errors.rejectValue(propName(QUser.user.password), "invalid.password.digit", new String[] { passwordPolicy.getMinNumberOfDigits().toString() }, "Invalid number of digit characters");
                }
            }
            if (passwordPolicy.getMinNumberOfSpecials() != null) {
                int minSpecialCharsCounter = 0;
                String specialCharsRegex = "[^\\dA-Za-z ]";
                Pattern pattern = Pattern.compile(specialCharsRegex);
                Matcher m = pattern.matcher(newPassword);
                while (m.find()) {
                    minSpecialCharsCounter++;
                }
                if (minSpecialCharsCounter < passwordPolicy.getMinNumberOfSpecials()) {
                    errors.rejectValue(propName(QUser.user.password), "invalid.password.specialChar", new String[] { passwordPolicy.getMinNumberOfSpecials().toString() }, "Invalid number of special characters");
                }
            }

        }
    }
}
