package gr.com.ist.commun.core.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface SearchableByTerm<T> {
    Page<T> searchTerm(String term, Pageable pageable);
}
