package gr.com.ist.commun.core.domain.security.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import gr.com.ist.commun.core.domain.security.RepositoryAwareFilterInvocationSecurityMetadataSource;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation to define security access control on a controller or on 
 * controller methods.
 * 
 * Example: {@literal @}HasAnyRole(
                urlAuthority = {
                        {@literal @}UrlAuthority(role = "statistics", method = SecurityUtils.VIEWER_SUFFIX), 
                        {@literal @}UrlAuthority(role = "aircraftMovementReports", method = SecurityUtils.EDITOR_SUFFIX)},
                implies = {
                        {@literal @}Implies(entityAuthority = {@literal @}EntityAuthority(type = AircraftMovement.class, method = SecurityUtils.EDITOR_SUFFIX))
                }
                )
 * The above configuration adds authorities statistics and aircraftMovementReports to System defined authorities in {@link RepositoryAwareFilterInvocationSecurityMetadataSource}
 * and adds entityAuthority aircraftMovements_editor as included authority to the urlAuthority roles.
 * 
 * @see EntityAuthority
 * @see UrlAuthority
 * @see Implies
 * @version $Rev$ $Date$
 */

@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface HasAnyRole {

    EntityAuthority[] entityAuthority() default {};
    UrlAuthority[] urlAuthority() default {};
    Implies[] implies() default {};
    
}
