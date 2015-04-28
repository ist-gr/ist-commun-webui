package gr.com.ist.commun.core.domain.security.annotations;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import gr.com.ist.commun.utils.SecurityUtils.SecurityMethods;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation to be used in order to declare 
 * a rest repository's Entity type and method 
 * security.
 * Example: {@literal @}EntityAuthority(type=Country.class, method = SecurityMethods.VIEWER)
 * With the above declaration the controller class or a controller's method is granted access
 * to country_viewer authority
 * 
 * @version $Rev$ $Date$
 */
@Target({METHOD})
@Retention(RUNTIME)
public @interface EntityAuthority {
    
    Class<?> type();
    SecurityMethods method();
}
