package gr.com.ist.commun.persistence.jpa;

import gr.com.ist.commun.core.domain.security.EntityBase;
import gr.com.ist.commun.core.domain.security.RepositoryAwareFilterInvocationSecurityMetadataSource;
import gr.com.ist.commun.core.domain.security.Role;
import gr.com.ist.commun.core.domain.security.audit.entity.AppEntityRevision;
import gr.com.ist.commun.core.domain.security.audit.entity.EmbedabblePropertyChangeAuditVisitor;
import gr.com.ist.commun.core.domain.security.audit.entity.EntityChange;
import gr.com.ist.commun.core.domain.security.audit.entity.PropertyChange;
import gr.com.ist.commun.core.domain.security.audit.entity.QAppEntityRevision;
import gr.com.ist.commun.core.repository.SearchableByTerm;
import gr.com.ist.commun.core.repository.security.audit.AppEntityRevisionRepository;
import gr.com.ist.commun.core.repository.security.audit.AppEntityRevisionRepositoryCustom;
import gr.com.ist.commun.utils.SecurityUtils;
import gr.com.ist.commun.utils.SecurityUtils.SecurityMethods;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.persistence.Embedded;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;
import javax.persistence.Transient;

import org.apache.commons.beanutils.PropertyUtils;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.entities.mapper.relation.lazy.proxy.CollectionProxy;
import org.hibernate.envers.query.AuditEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.stereotype.Component;

import com.google.common.base.Objects;
import com.mysema.query.types.expr.BooleanExpression;

import de.danielbechler.diff.ObjectDiffer;
import de.danielbechler.diff.ObjectDifferFactory;
import de.danielbechler.diff.node.Node;

@Component("appEntityRevisionRepositoryImpl")
public class AppEntityRevisionRepositoryImpl implements AppEntityRevisionRepositoryCustom , SearchableByTerm<AppEntityRevision> {
    private static final Logger LOG = LoggerFactory.getLogger(AppEntityRevisionRepositoryImpl.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private AppEntityRevisionRepository appEntityRevisionRepository;

    @Autowired
    private RepositoryAwareFilterInvocationSecurityMetadataSource repositoryAwareFilterInvocationSecurityMetadataSource;

    @Override
    public Page<AppEntityRevision> complexSearch(AppRevisionCriteria criteria, Pageable pageable) {

        QAppEntityRevision appEntityRevision = QAppEntityRevision.appEntityRevision;

        BooleanExpression predicate = null;

        if (criteria.getWho() != null) {
            predicate = appEntityRevision.user.eq(criteria.getWho());
        }
        if (criteria.getDateFrom() != null) {
            predicate = appEntityRevision.timestamp.goe(criteria.getDateFrom().getTime()).and(predicate);
        }
        if (criteria.getDateTo() != null) {
            predicate = appEntityRevision.timestamp.loe(criteria.getDateTo().getTime()).and(predicate);
        }
        if (criteria.getEntityName() != null) {
            predicate = appEntityRevision.modifiedEntityNames.any().equalsIgnoreCase(criteria.getEntityName()).and(predicate);
        }
        predicate = applySecurityPredicate(appEntityRevision, predicate);
        return appEntityRevisionRepository.findAll(predicate, pageable);
    }

    @Override
    public Page<AppEntityRevision> searchTerm(String term, Pageable pageable) {
        QAppEntityRevision appEntityRevision = QAppEntityRevision.appEntityRevision;
        BooleanExpression predicate = null;
        if (term != null) {
            try {
                long revisionId = Integer.parseInt(term);
                predicate = appEntityRevision.id.eq(revisionId);
            } catch (NumberFormatException e) {
                predicate = appEntityRevision.user.fullName.containsIgnoreCase(term);
            }
        }
        predicate = applySecurityPredicate(appEntityRevision, predicate);
        return appEntityRevisionRepository.findAll(predicate, pageable);
    }
    
    @Override
    public boolean isRevisionAccesible(AppEntityRevision appEntityRevision) {
        if (SecurityUtils.getCurrentUser().isAdmin()!=null && SecurityUtils.getCurrentUser().isAdmin())
            return true;
        else {
            for(String modifiedEntity:appEntityRevision.getModifiedEntityNames())
                if (!getAccessibleEntities().contains(modifiedEntity))
                    return false;
            return true;
        }
    }
    
    private BooleanExpression applySecurityPredicate(QAppEntityRevision appEntityRevision, BooleanExpression predicate) {
        if (SecurityUtils.getCurrentUser().isAdmin()!=null && SecurityUtils.getCurrentUser().isAdmin()) {
            return predicate;
        }
        predicate = appEntityRevision.modifiedEntityNames.any().in(getAccessibleEntities()).and(predicate);
        return predicate;
    }
    
    private Set<String> getAccessibleEntities() {
        Set<String> entities = new HashSet<String>();
        for (ResourceMetadata metadata : repositoryAwareFilterInvocationSecurityMetadataSource.getResources()) {
            for (Role role : SecurityUtils.getCurrentUser().getGrantedAuthorities()) {
                if (role.getAuthority().equals(metadata.getPath().toString().substring(1) + SecurityUtils.convertSecurityMethodToRoleSuffix(SecurityMethods.VIEWER))) {
                    entities.add(metadata.getDomainType().getName());
                }
            }
        }
        return entities;
    }

  /*
   * (non-Javadoc)
   * @see org.springframework.data.repository.history.RevisionRepository#findLastChangeRevision(java.io.Serializable)
   */
  @SuppressWarnings("unchecked")
  public List<EntityChange> getEntityChangesForRevision(final long revisionId, Pageable pageable, List<EntityBase> changedEntities) {
      List<EntityChange> entityChangeSet = new ArrayList<EntityChange>();
      Set<Class<?>> updatedEntitiesTypes = new HashSet<>();
      for (EntityBase entityBase : changedEntities) {
          updatedEntitiesTypes.add(entityBase.getClass());
      }
      List<Object[]> resultList = new ArrayList<Object[]>();
      AuditReader reader = AuditReaderFactory.get(entityManager);
      for (Class<?> type : updatedEntitiesTypes) {
          resultList.addAll(reader.createQuery().forRevisionsOfEntity(type, type.getName(), false, true).add(AuditEntity.revisionNumber().eq(revisionId)).getResultList());
      }
      int startIndex = Math.max(0, pageable.getPageNumber() * pageable.getPageSize());
      int endIndex = Math.min(changedEntities.size(), (pageable.getPageNumber() + 1) * pageable.getPageSize());
      resultList = (startIndex <= endIndex) ? resultList.subList(startIndex, endIndex) : Collections.<Object[]> emptyList();
      for (Object[] result : resultList) {
          final EntityChange entityChange = new EntityChange();
          entityChange.setRevisionType((RevisionType) result[2]);
          entityChange.setEntityName(result[0].getClass().getName());
          final List<PropertyChange> propertyChanges = new ArrayList<PropertyChange>();
          final UUID entityId = ((EntityBase) result[0]).getId();
          entityChange.setEntityId(entityId);
          List<Number> revisions = reader.getRevisions(result[0].getClass(), entityId);
          if (revisions.isEmpty()) {
              break;
          }
          Object newEntity = result[0];
          Object oldEntity = null;
          int indexOFcurrentRevision = revisions.indexOf(revisionId);
          if (indexOFcurrentRevision > 0) {
              Number previousRevision = revisions.get(indexOFcurrentRevision - 1);
              oldEntity = reader.find(result[0].getClass(), entityId, previousRevision);
          }
          Class<?> inspectedClass = newEntity.getClass();
          while (inspectedClass != null) {
              Field[] fields = inspectedClass.getDeclaredFields();
              for (final Field field : fields) {
                  if (!isAnnotadedWith(field, Transient.class) && !Modifier.isStatic(field.getModifiers())) {
                      propertyChanges.addAll(getPropertyChanges(entityChange.getRevisionType(), field, newEntity, oldEntity, entityId, revisionId));
                  }
              }
              inspectedClass = inspectedClass.getSuperclass();
          }
          entityChange.setPropertyChanges(propertyChanges);
          entityChangeSet.add(entityChange);
      }
      return entityChangeSet;
  }
  
  private List<PropertyChange> getPropertyChanges(RevisionType revisionType, Field field, Object newEntity, Object oldEntity, UUID entityID, long revisionId) {
      List<PropertyChange> propertyChanges = new ArrayList<PropertyChange>();
      try {
          Object newProperty = null;
          newProperty = PropertyUtils.getProperty(newEntity,field.getName()); 
          Object oldProperty = null;
          if (oldEntity != null)
              oldProperty = PropertyUtils.getProperty(oldEntity, field.getName());
          if (isAnnotadedWith(field, Embedded.class)) {
              propertyChanges = getPropertyChangesForEmbeddable(field, newProperty, oldProperty);
          } else if (newProperty instanceof CollectionProxy) {
              Collection<Object> newUnproxied = new ArrayList<>();
              Collection<Object> oldUnproxied = new ArrayList<>();
              @SuppressWarnings("unchecked")
              Collection<Object> newProxied = (Collection<Object>) newProperty;
              newUnproxied.addAll(newProxied);
              if  (oldProperty != null) {
                  @SuppressWarnings("unchecked")
                  Collection<Object> oldProxied = (Collection<Object>) oldProperty;
                  oldUnproxied.addAll(oldProxied);
              }
              if (newUnproxied.size() > 0 && oldUnproxied.size() == 0) {
                  propertyChanges.add(new PropertyChange(field.getName(), newUnproxied, null));
              } else if (newUnproxied.size() == 0 && oldUnproxied.size() > 0) {
                  propertyChanges.add(new PropertyChange(field.getName(), null, oldUnproxied));
              } else {
                  Collection<Object> removed = findMissing(newUnproxied, oldUnproxied);
                  Collection<Object> added = findMissing(oldUnproxied, newUnproxied); 
                  PropertyChange collectionModificationPropertyChange = new PropertyChange();
                  if (removed.size() > 0)
                      collectionModificationPropertyChange.setRemoved(removed);
                  if (added.size() > 0)
                      collectionModificationPropertyChange.setAdded(added);
                  if (added.size() > 0 || removed.size() > 0) {
                      collectionModificationPropertyChange.setPath(field.getName());
                      propertyChanges.add(collectionModificationPropertyChange);
                  }
              }
          }
          else {
              try {
                  if (newProperty != null && oldProperty != null && newProperty instanceof EntityBase) {
                      if(newProperty instanceof EntityBase) {
                          if (!((EntityBase) newProperty).getId().equals(((EntityBase)oldProperty).getId())) {
                              propertyChanges.add(new PropertyChange(field.getName(), newProperty, oldProperty));
                          }
                      } else {
                          if(!newProperty.equals(oldProperty)) {
                              propertyChanges.add(new PropertyChange(field.getName(), newProperty, oldProperty));
                          }
                      }
                  } else if (!Objects.equal(newProperty, oldProperty)) {
                      propertyChanges.add(new PropertyChange(field.getName(), newProperty, oldProperty));
                  }
              } catch (EntityNotFoundException e) {
                  LOG.warn("Field {} is not included in the list of property changes because of {}", field.getName(), e);
              }
          }
      } catch ( IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
          throw new RuntimeException(e);
      }
      return propertyChanges;
  }
  
  private Collection<Object> findMissing(Collection<Object> newCollection, Collection<Object> oldCollection) {
      Collection<Object> missing = new ArrayList<>();
      for (Object oldObj : oldCollection) {
          if (!newCollection.contains(oldObj)) {
              missing.add(oldObj);
          }
      }
      return missing;
  }

  private List<PropertyChange> getPropertyChangesForEmbeddable(Field field, Object newProperty, Object oldProperty) {
      ObjectDiffer objectDiffer = ObjectDifferFactory.getInstance();
      final Node root = objectDiffer.compare(newProperty, oldProperty);
      final EmbedabblePropertyChangeAuditVisitor visitor = new EmbedabblePropertyChangeAuditVisitor(field.getName(), newProperty, oldProperty);
      root.visit(visitor);
      return visitor.getChanges();
  }
  
  private boolean isAnnotadedWith(Field field, Class<?> annotationClass) {
      final Annotation[] annotations = field.getAnnotations();
      for (Annotation annotation : annotations) {
          if (annotation.annotationType().equals(annotationClass)) {
              return true;
          }
      }
      return false;
  }
   
  @Override
  public List<EntityBase> findEntitiesChangedAtRevision(long revisionNumber) {
      AuditReader reader = AuditReaderFactory.get(entityManager);
      @SuppressWarnings("unchecked")
      List<EntityBase> entities = (List<EntityBase>) ((List<?>)  reader.getCrossTypeRevisionChangesReader().findEntities(revisionNumber));
      return entities;
  }
  
}
