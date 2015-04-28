package gr.com.ist.commun.core.domain.security;

import static gr.com.ist.commun.utils.JpaUtils.propName;

import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class RoleValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return RoleGroup.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        Assert.notNull(target);
        Assert.isAssignable(RoleGroup.class, target.getClass());//A user can only have a RoleGroup and not an AuthorityRole
        RoleGroup role = (RoleGroup) target;
        StringBuffer path = new StringBuffer();
        path.append(role.getName()); /*TODO should return localized name*/
        // TODO Avoid using getIncludedRoles and use the two separate getters instead
        if (role != null && role.getIncludedRoleGroups() != null) {
            Boolean circularReference = containsCircularReference(role.getIncludedRoleGroups(), role, path);
            if (circularReference) {
                errors.rejectValue(propName(QRoleGroup.roleGroup.includedRoleGroups), "isCircularReference", new Object[]{path},"This assignment of roles contains cyclic references with path: " + path.toString());
            }
        }
    }

    /*We loop inside includedRoleGroups since AuthorityRoles configured by the
     *system have always leaves and hence no circular dependencies may be present.
     */
    private boolean containsCircularReference(Set<RoleGroup> includedRoles, RoleGroup target, StringBuffer path) {
        for (RoleGroup role : includedRoles) {
            if (role.getIncludedRoles() != null && !role.getIncludedRoles().isEmpty()) {
                path.append(" -> " + role.getName());
                if (role.getName().equals(target.getName())) {
                    return true;
                } else {
                    return containsCircularReference(role.getIncludedRoleGroups(), target, path);
                }
            }
        }
        return false;
    }
}
