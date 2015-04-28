package gr.com.ist.commun.core.repository.security.audit;

import gr.com.ist.commun.core.domain.security.audit.entity.AppEntityRevision;
import gr.com.ist.commun.core.repository.QueryDslProjectionPredicateExecutor;
import gr.com.ist.commun.core.repository.SearchableByTerm;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = false)
public interface AppEntityRevisionRepository extends JpaRepository<AppEntityRevision, Long> , QueryDslProjectionPredicateExecutor<AppEntityRevision>, AppEntityRevisionRepositoryCustom, SearchableByTerm<AppEntityRevision>  {
}
