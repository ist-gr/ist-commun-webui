package gr.com.ist.commun.core.domain.security.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Base annotation to be used in order to define security access control to controllers.
 * 
 * <pre>
 * Example: {@literal @}SecuredWith(
 *       hasAnyRole = {@literal @}HasAnyRole(
 *             urlAuthority = { {@literal @}UrlAuthority(role = "statistics", method = SecurityUtils.VIEWER_SUFFIX), 
 *                              {@literal @}UrlAuthority(role = "statistics/flights", method = SecurityUtils.VIEWER_SUFFIX)},
 *               implies = {
 *                               {@literal @}Implies(entityAuthority = {@literal @}EntityAuthority(type = Airline.class, method = SecurityUtils.VIEWER_SUFFIX)),
 *                               {@literal @}Implies(entityAuthority = {@literal @}EntityAuthority(type = Airport.class, method = SecurityUtils.VIEWER_SUFFIX))
 *               }))
 * </pre>
 * 
 * @see HasAnyRole
 *
 * @version $Rev$ $Date$
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface SecuredWith {
    
    HasAnyRole[] hasAnyRole() default {};
}