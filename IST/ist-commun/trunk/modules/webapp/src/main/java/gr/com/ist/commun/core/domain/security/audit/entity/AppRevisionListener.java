package gr.com.ist.commun.core.domain.security.audit.entity;

import gr.com.ist.commun.core.domain.security.User;

import org.hibernate.envers.RevisionListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class AppRevisionListener implements RevisionListener {
    
    public void newRevision(Object revisionEntity) {
            AppEntityRevision appEntityRevision = (AppEntityRevision) revisionEntity;
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User user = authentication == null ? null : ((User)authentication.getPrincipal());
            appEntityRevision.setUser(user);
    }
}
