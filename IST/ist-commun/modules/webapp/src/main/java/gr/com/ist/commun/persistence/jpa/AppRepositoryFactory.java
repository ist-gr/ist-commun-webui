package gr.com.ist.commun.persistence.jpa;

import java.io.Serializable;

import javax.persistence.EntityManager;

import org.springframework.core.GenericTypeResolver;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.data.repository.history.support.RevisionEntityInformation;

/**
 * Repository factory.
 * 
 *
 */
public class AppRepositoryFactory<T, I extends Serializable> extends JpaRepositoryFactory {

    private final RevisionEntityInformation revisionEntityInformation;
    
    /**
     * Creates a new {@link AppRepositoryFactory} using the given {@link EntityManager} and revision entity class.
     * 
     * @param entityManager must not be {@literal null}.
     * @param revisionEntityClass can be {@literal null}, will default to {@link AppRevisionEntity}.
     */
    public AppRepositoryFactory(EntityManager entityManager, Class<?> revisionEntityClass) {
        super(entityManager);
        revisionEntityClass = revisionEntityClass == null ? AppRevisionEntity.class : revisionEntityClass;
        this.revisionEntityInformation = AppRevisionEntity.class.equals(revisionEntityClass) ? new AppRevisionEntityInformation()
                : new ReflectionRevisionEntityInformation(revisionEntityClass);
        
    }

    /* 
     * (non-Javadoc)
     * @see org.springframework.data.jpa.repository.support.JpaRepositoryFactory#getTargetRepository(org.springframework.data.repository.core.RepositoryMetadata, javax.persistence.EntityManager)
     */
    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected <T, ID extends Serializable> SimpleJpaRepository<?, ?> getTargetRepository(RepositoryMetadata metadata,
            EntityManager entityManager) {

        JpaEntityInformation<T, Serializable> entityInformation = (JpaEntityInformation<T, Serializable>) getEntityInformation(metadata
                .getDomainType());
        return new AppRepositoryImpl(entityInformation, revisionEntityInformation, entityManager);
    }

    @Override
    protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
        return AppRepositoryImpl.class;
    }
    
    /* (non-Javadoc)
     * @see org.springframework.data.repository.core.support.RepositoryFactorySupport#getRepository(java.lang.Class, java.lang.Object)
     */
    @Override
    public <T> T getRepository(Class<T> repositoryInterface, Object customImplementation) {

        if (RevisionRepository.class.isAssignableFrom(repositoryInterface)) {

            Class<?>[] typeArguments = GenericTypeResolver.resolveTypeArguments(repositoryInterface,
                    RevisionRepository.class);
            Class<?> revisionNumberType = typeArguments[2];

            if (!revisionEntityInformation.getRevisionNumberType().equals(revisionNumberType)) {
                throw new IllegalStateException(String.format(
                        "Configured a revision entity type of %s with a revision type of %s "
                                + "but the repository interface is typed to a revision type of %s!", repositoryInterface,
                        revisionEntityInformation.getRevisionNumberType(), revisionNumberType));
            }
        }

        return super.getRepository(repositoryInterface, customImplementation);
    }        
}