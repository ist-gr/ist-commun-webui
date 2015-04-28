package gr.com.ist.commun.utils;

import gr.com.ist.commun.core.domain.security.User;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {
    
    public static final String ADMIN_SUFFIX = "_admin";
    public static final String EDITOR_SUFFIX = "_editor";
    public static final String VIEWER_SUFFIX = "_viewer";
    
    public static enum SecurityMethods {
        ADMIN, EDITOR, VIEWER
}
    
    public static User getCurrentUser() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        User user = (User) (authentication == null
                || authentication.getPrincipal() == null
                || !User.class.isAssignableFrom(authentication.getPrincipal().getClass()) ? null
                        : authentication.getPrincipal());
        return user;
    }
    
    public static String convertSecurityMethodToRoleSuffix(SecurityMethods securityMethod) {
        if (securityMethod.equals(SecurityMethods.ADMIN)) {
            return ADMIN_SUFFIX;
        } else if (securityMethod.equals(SecurityMethods.EDITOR)) {
            return EDITOR_SUFFIX;
        } else {
            return VIEWER_SUFFIX;
        }
    }
}