package gr.com.ist.commun.core.validation;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * This validator handles JSR-303 validations and can delegate extra validation
 * to configured validators. If no validators are configured it automatically
 * includes all beans from the application context which implement the
 * Validator interface.
 * 
 */

public class CompositeValidator extends LocalValidatorFactoryBean implements ApplicationContextAware, InitializingBean {
    private Validator[] validators;
    private ApplicationContext applicationContext;

    @Override
    public void validate(final Object target, final Errors errors) {
        super.validate(target, errors);
        if (validators == null) {
            return;
        }
        for (Validator v : validators) {
            if (v.supports(target.getClass())) {
                v.validate(target, errors);
            }
        }
    }

    public void setValidators(Validator[] validators) {
        this.validators = validators;
    }

    /* (non-Javadoc)
     * @see org.springframework.validation.beanvalidation.LocalValidatorFactoryBean#setApplicationContext(org.springframework.context.ApplicationContext)
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        super.setApplicationContext(applicationContext);
        this.applicationContext = applicationContext;
    }

    /* (non-Javadoc)
     * @see org.springframework.validation.beanvalidation.LocalValidatorFactoryBean#afterPropertiesSet()
     */
    @Override
    public void afterPropertiesSet() {
        super.afterPropertiesSet();
        if (this.applicationContext != null && this.validators == null) {
            Collection<Validator> validators = applicationContext.getBeansOfType(Validator.class).values();
            // set this.validators to all registered validators except myself 
            ArrayList<Validator> filteredValidators = new ArrayList<Validator>();
            for (Validator v : validators) {
                if (!v.getClass().equals(this.getClass())) {
                    filteredValidators.add(v);
                }
            }
            this.validators = filteredValidators.toArray(new Validator[0]);
        }
    }
}
