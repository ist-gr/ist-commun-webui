package gr.com.ist.commun.persistence.jpa;

import gr.com.ist.commun.core.domain.security.QRoleGroup;
import gr.com.ist.commun.core.domain.security.RoleGroup;
import gr.com.ist.commun.core.repository.SearchableByTerm;
import gr.com.ist.commun.core.repository.security.RoleGroupsRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.mysema.query.types.expr.BooleanExpression;

@Component("roleGroupsRepositoryImpl")
public class JpaRoleGroupsCustomRepository implements SearchableByTerm<RoleGroup> {
    @Autowired
    RoleGroupsRepository roleGroupsRepository;
    
    @Override
    public Page<RoleGroup> searchTerm(String term, Pageable pageable) {
        QRoleGroup role = QRoleGroup.roleGroup;
        BooleanExpression predicate = null;
        if (term == null) {
            predicate = null;
        } else {
            predicate = role.name.containsIgnoreCase(term);
        }
        //TODO: Search into included roles also (including authorities)
        return roleGroupsRepository.findAll(predicate, pageable);
    }

}
