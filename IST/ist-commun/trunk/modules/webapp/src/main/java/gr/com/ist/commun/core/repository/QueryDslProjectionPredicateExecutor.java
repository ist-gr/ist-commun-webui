package gr.com.ist.commun.core.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import com.mysema.query.types.FactoryExpression;
import com.mysema.query.types.Predicate;

@NoRepositoryBean
public interface QueryDslProjectionPredicateExecutor<T>
    extends QueryDslPredicateExecutor<T> {
    /**
     * Returns a {@link org.springframework.data.domain.Page} of entities matching the given {@link com.mysema.query.types.Predicate}.
     * This also uses provided projections (can be JavaBean or constructor or anything supported by QueryDSL)
     * @param constructorExpression this constructor expression will be used for transforming query results
     * @param predicate
     * @param pageable
     * @return
     */
    Page<T> findAll(FactoryExpression<T> factoryExpression, Predicate predicate, Pageable pageable);
}
