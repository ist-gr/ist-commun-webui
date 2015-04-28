package gr.com.ist.commun.persistence.jpa;


import gr.com.ist.commun.core.domain.security.RepositoryAwareFilterInvocationSecurityMetadataSource;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.EntityManager;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.history.AnnotationRevisionMetadata;
import org.springframework.data.history.Revision;
import org.springframework.data.history.RevisionMetadata;
import org.springframework.data.history.Revisions;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.QueryDslJpaRepository;
import org.springframework.data.jpa.repository.support.Querydsl;
import org.springframework.data.querydsl.EntityPathResolver;
import org.springframework.data.querydsl.SimpleEntityPathResolver;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.history.support.RevisionEntityInformation;
import org.springframework.util.Assert;

import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.types.EntityPath;
import com.mysema.query.types.FactoryExpression;
import com.mysema.query.types.Predicate;
import com.mysema.query.types.path.PathBuilder;


public class AppRepositoryImpl<T, ID extends Serializable, N extends Number & Comparable<N>> extends QueryDslJpaRepository<T, ID>
        implements AppRepository<T, ID, N> /*, AuditableJpaRepository<T, ID> */  {

    //XXX this component should be autowired using Spring.
    RepositoryAwareFilterInvocationSecurityMetadataSource repositoryAwareFilterInvocationSecurityMetadataSource; 
    
    //All instance variables are available in super, but they are private
    private static final EntityPathResolver DEFAULT_ENTITY_PATH_RESOLVER = SimpleEntityPathResolver.INSTANCE;

    private final JpaEntityInformation<T, ID> entityInformation;
    private final RevisionEntityInformation revisionEntityInformation;
    private final EntityManager entityManager;
    
    private final EntityPath<T> path;
    private final PathBuilder<T> builder;
    private final Querydsl querydsl;

    public AppRepositoryImpl(JpaEntityInformation<T, ID> entityInformation, RevisionEntityInformation revisionEntityInformation, EntityManager entityManager) {
        this(entityInformation, revisionEntityInformation, entityManager, DEFAULT_ENTITY_PATH_RESOLVER);
    }
    
    public AppRepositoryImpl(JpaEntityInformation<T, ID> entityInformation, 
            RevisionEntityInformation revisionEntityInformation, EntityManager entityManager,
                                 EntityPathResolver resolver) {

        super(entityInformation, entityManager);
        
        Assert.notNull(revisionEntityInformation);
        
        this.entityInformation = entityInformation;
        this.revisionEntityInformation = revisionEntityInformation;
        this.entityManager = entityManager;
        
        this.path = resolver.createPath(entityInformation.getJavaType());
        this.builder = new PathBuilder<T>(path.getType(), path.getMetadata());
        this.querydsl = new Querydsl(entityManager, builder);
    }

    @Override
    public Page<T> findAll(FactoryExpression<T> factoryExpression, Predicate predicate, Pageable pageable) {
        JPQLQuery countQuery = createQuery(predicate);
        JPQLQuery query = querydsl.applyPagination(pageable, createQuery(predicate));

        Long total = countQuery.count();
        List<T> content = total > pageable.getOffset() ? query.list(factoryExpression) : Collections.<T> emptyList();

        return new PageImpl<T>(content, pageable, total);
    }


    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.history.RevisionRepository#findRevisions(java.io.Serializable)
     */
    @SuppressWarnings("unchecked")
    public Revisions<N, T> findRevisions(ID id) {

        Class<T> type = entityInformation.getJavaType();
        AuditReader reader = AuditReaderFactory.get(entityManager);
        List<? extends Number> revisionNumbers = reader.getRevisions(type, id);

        return revisionNumbers.isEmpty() ? new Revisions<N, T>(Collections.EMPTY_LIST) : getEntitiesForRevisions(
                (List<N>) revisionNumbers, id, reader);
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.history.RevisionRepository#findRevisions(java.io.Serializable, org.springframework.data.domain.Pageable)
     */
    @SuppressWarnings("unchecked")
    public Page<Revision<N, T>> findRevisions(ID id, Pageable pageable) {

        Class<T> type = entityInformation.getJavaType();
        AuditReader reader = AuditReaderFactory.get(entityManager);
        List<Number> revisionNumbers = reader.getRevisions(type, id);

        if (pageable.getOffset() > revisionNumbers.size()) {
            return new PageImpl<Revision<N, T>>(Collections.<Revision<N, T>> emptyList(), pageable, 0);
        }

        int upperBound = pageable.getOffset() + pageable.getPageSize();
        upperBound = upperBound > revisionNumbers.size() ? revisionNumbers.size() : upperBound;

        List<? extends Number> subList = revisionNumbers.subList(pageable.getOffset(), upperBound);
        Revisions<N, T> revisions = getEntitiesForRevisions((List<N>) subList, id, reader);

        return new PageImpl<Revision<N, T>>(revisions.getContent(), pageable, revisionNumbers.size());
    }

    /**
     * Returns the entities in the given revisions for the entitiy with the given id.
     * 
     * @param revisionNumbers
     * @param id
     * @param reader
     * @return
     */
    @SuppressWarnings("unchecked")
    private Revisions<N, T> getEntitiesForRevisions(List<N> revisionNumbers, ID id, AuditReader reader) {

        Class<T> type = entityInformation.getJavaType();
        Map<N, T> revisions = new HashMap<N, T>(revisionNumbers.size());

        Class<?> revisionEntityClass = revisionEntityInformation.getRevisionEntityClass();
        Map<Number, Object> revisionEntities = (Map<Number, Object>) reader.findRevisions(revisionEntityClass,
                new HashSet<Number>(revisionNumbers));

        for (Number number : revisionNumbers) {
            revisions.put((N) number, reader.find(type, id, number));
        }

        return new Revisions<N, T>(toRevisions(revisions, revisionEntities));
    }

    @SuppressWarnings("unchecked")
    private List<Revision<N, T>> toRevisions(Map<N, T> source, Map<Number, Object> revisionEntities) {

        List<Revision<N, T>> result = new ArrayList<Revision<N, T>>();

        for (Entry<N, T> revision : source.entrySet()) {

            N revisionNumber = revision.getKey();
            T entity = revision.getValue();
            RevisionMetadata<N> metadata = (RevisionMetadata<N>) getRevisionMetadata(revisionEntities.get(revisionNumber));
            result.add(new Revision<N, T>(metadata, entity));
        }

        Collections.sort(result);
        return Collections.unmodifiableList(result);
    }

    /**
     * Returns the {@link RevisionMetadata} wrapper depending on the type of the given object.
     * 
     * @param object
     * @return
     */
    private RevisionMetadata<?> getRevisionMetadata(Object object) {
        if (object instanceof AppRevisionEntity) {
            return new AppRevisionMetadata((AppRevisionEntity) object);
        } else {
            return new AnnotationRevisionMetadata<N>(object, RevisionNumber.class, RevisionTimestamp.class);
        }
    }

    @SuppressWarnings("unchecked")
    public Revision<N, T> findPreviousRevisionChange(ID id) {

        Class<T> type = entityInformation.getJavaType();
        AuditReader reader = AuditReaderFactory.get(entityManager);

        List<Number> revisions = reader.getRevisions(type, id);

        if (revisions.isEmpty() || revisions.size() < 2) {
            return null;
        }
        N previousRevision = (N) revisions.get(revisions.size() - 2);

        Class<?> revisionEntityClass = revisionEntityInformation.getRevisionEntityClass();

        Object revisionEntity = reader.findRevision(revisionEntityClass, previousRevision);
        RevisionMetadata<N> metadata = (RevisionMetadata<N>) getRevisionMetadata(revisionEntity);
        return new Revision<N, T>(metadata, reader.find(type, id, previousRevision));
    }

    @Override
    public EntityInformation<T, ID> getEntityInformation() {
        return this.entityInformation;
    }

}