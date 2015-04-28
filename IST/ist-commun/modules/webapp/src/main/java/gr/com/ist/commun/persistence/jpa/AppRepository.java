package gr.com.ist.commun.persistence.jpa;

import gr.com.ist.commun.core.repository.QueryDslProjectionPredicateExecutor;

import java.io.Serializable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.core.EntityInformation;

/**
 * Convenience interface to allow pulling in functionality from various
 * interfaces in one.
 * 
 */
@NoRepositoryBean
public interface AppRepository<T, ID extends Serializable, N extends Number & Comparable<N>>
    extends JpaRepository<T, ID>, QueryDslProjectionPredicateExecutor<T>,
    AppRevisionRepository<T, ID, N> {
    EntityInformation<T, ID> getEntityInformation();
}
