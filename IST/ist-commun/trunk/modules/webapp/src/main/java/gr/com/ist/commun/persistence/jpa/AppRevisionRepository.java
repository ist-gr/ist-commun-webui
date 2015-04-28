package gr.com.ist.commun.persistence.jpa;

import java.io.Serializable;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.history.Revision;
import org.springframework.data.history.Revisions;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * A repository which can access entities held in a variety of {@link Revisions}.
 * 
 */
@NoRepositoryBean
public interface AppRevisionRepository<T, ID extends Serializable, N extends Number & Comparable<N>> {


    /**
     * Returns all {@link Revisions} of an entity with the given id.
     * 
     * @param id must not be {@literal null}.
     * @return
     */
    Revisions<N, T> findRevisions(ID id);

    /**
     * Returns a {@link Page} of revisions for the entity with the given id.
     * 
     * @param id must not be {@literal null}.
     * @param pageable
     * @return
     */
    Page<Revision<N, T>> findRevisions(ID id, Pageable pageable);
}
