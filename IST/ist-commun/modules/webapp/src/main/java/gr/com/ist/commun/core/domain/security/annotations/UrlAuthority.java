package gr.com.ist.commun.core.domain.security.annotations;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import gr.com.ist.commun.utils.SecurityUtils.SecurityMethods;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation to be used in order to add custom authorities to a controller or
 * to a controller's methods.
 * 
 * Example: {@literal @}UrlAuthority(role = "statistics", method = SecurityUtils.VIEWER_SUFFIX)
 * The above configuration adds role statistics to the System defined roles and grants access to the controller or the method
 * to any user having this role
 * 
 * @version $Rev$ $Date$
 */

@Target({METHOD})
@Retention(RUNTIME)
public @interface UrlAuthority {
    
    String role();
    SecurityMethods method();
    
}
