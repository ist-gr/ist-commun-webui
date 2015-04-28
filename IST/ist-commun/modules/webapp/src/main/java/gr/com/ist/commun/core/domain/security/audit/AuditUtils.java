package gr.com.ist.commun.core.domain.security.audit;

import gr.com.ist.commun.core.domain.security.User;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class AuditUtils {

    public static String getPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        } else if (authentication.getPrincipal() instanceof String)
            return (String) authentication.getPrincipal();
        else
            return authentication == null ? null : ((User)authentication.getPrincipal()).getUsername();
    }
}
