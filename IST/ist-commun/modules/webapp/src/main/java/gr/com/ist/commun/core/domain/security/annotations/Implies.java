package gr.com.ist.commun.core.domain.security.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation to be used when an {@link UrlAuthority} inside a {@link HasAnyRole} 
 * needs to define included roles
 * 
 * Example: @HasAnyRole(
                urlAuthority = {
                        @UrlAuthority(role = "statistics", method = SecurityUtils.VIEWER_SUFFIX), 
                        @UrlAuthority(role = "aircraftMovementReports", method = SecurityUtils.EDITOR_SUFFIX)},
                implies = {
                        @Implies(entityAuthority = @EntityAuthority(type = AircraftMovement.class, method = SecurityUtils.EDITOR_SUFFIX))
                }
                )
 * In the above example the urlAuthorities statistics and aircraftMovementReports will both have as included role the entityAuthority
 * aircraftMovements_viewer
 * 
 * @see {@link EntityAuthority}
 *
 * @version $Rev$ $Date$
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface Implies {
    
    EntityAuthority entityAuthority();
    
}
