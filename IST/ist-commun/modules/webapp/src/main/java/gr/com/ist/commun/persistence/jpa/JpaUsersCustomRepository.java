package gr.com.ist.commun.persistence.jpa;

import gr.com.ist.commun.core.domain.security.QUser;
import gr.com.ist.commun.core.domain.security.User;
import gr.com.ist.commun.core.repository.SearchableByTerm;
import gr.com.ist.commun.core.repository.security.UsersRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.mysema.query.types.expr.BooleanExpression;

@Component("usersRepositoryImpl")
public class JpaUsersCustomRepository implements SearchableByTerm<User> {
    @Autowired
    UsersRepository usersRepository;
    
    @Override
    public Page<User> searchTerm(String term, Pageable pageable) {
        QUser user = QUser.user;
        BooleanExpression predicate = null;
        if (term == null) {
            predicate = null;
        } else if (term.matches("^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$")) {
            predicate = user.email.containsIgnoreCase(term);
         // TODO replace the following hardcoded regex patterns by retrieve the annotations from respective fields
        } else {
            predicate = user
            .username.containsIgnoreCase(term)
            .or(user.firstName.containsIgnoreCase(term))
            .or(user.lastName.containsIgnoreCase(term))
            .or(user.fullName.containsIgnoreCase(term));
        }
        //TODO: Add search by password expiration date
        return usersRepository.findAll(predicate, pageable);
    }

}
