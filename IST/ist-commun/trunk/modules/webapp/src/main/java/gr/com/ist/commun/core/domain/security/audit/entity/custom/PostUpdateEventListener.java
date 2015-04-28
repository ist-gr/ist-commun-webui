package gr.com.ist.commun.core.domain.security.audit.entity.custom;

import org.hibernate.envers.configuration.AuditConfiguration;
import org.hibernate.envers.event.EnversPostUpdateEventListenerImpl;
import org.hibernate.event.spi.PostUpdateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionService;

//TODO: inject ConversionService using @Configure and aspects. Mind that PostUpdateEventListener is instantiated before spring context is started.
//@Configurable(preConstruction=true,autowire=Autowire.BY_TYPE, dependencyCheck=true)
public class PostUpdateEventListener  extends EnversPostUpdateEventListenerImpl {
   
    private static final long serialVersionUID = 1L;

    protected PostUpdateEventListener(AuditConfiguration enversConfiguration) {
        super(enversConfiguration);
    }

    private ConversionService conversionService;
        
    public void setConversionService(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    private static final Logger LOG = LoggerFactory.getLogger(PostUpdateEventListener.class);
    
    @Override   
    public void onPostUpdate(PostUpdateEvent event) {
        if (conversionService == null) {
            this.conversionService = StaticContextAccessor.getBean("conversionService");
        }
        String entityName = event.getPersister().getEntityName();
        if (getAuditConfiguration().getEntCfg().isVersioned(entityName)) {
            checkIfTransactionInProgress(event.getSession());
        }
// FIXME
//        Set<PersistentAuditData> changeSet = new HashSet<PersistentAuditData>();
//        final Object[] newDbState = postUpdateDBState(event, changeSet);
//        LOG.debug("EntityName:{] - New entity:{}", entityName, event.getEntity());
//        LOG.debug("Old State:{}/New State:{}", Arrays.toString(event.getOldState()), Arrays.toString(newDbState));
//
//        PersistentAuditEvent auditEvent = new PersistentAuditEvent();
//        auditEvent.setAuditEventType("ENTITY_UPDATE");
//        auditEvent.setPrincipal(AuditUtils.getPrincipal());
//        event.getSession().persist(auditEvent);
//
//        for (PersistentAuditData auditData : changeSet) {
//            LOG.debug("{}:{}/{}", new Object[] { auditData.getProperty(), auditData.getOldValue(), auditData.getNewValue() });
//            auditData.setPersistentAuditEvent(auditEvent);
//            event.getSession().persist(auditData);
//        }
//        // super.onPostUpdate(event);
    }

// FIXME
//    private Object[] postUpdateDBState(PostUpdateEvent event, Set<PersistentAuditData> changeSet) {
//        Object[] newDbState = event.getState().clone();
//        if (event.getOldState() != null) {
//            EntityPersister entityPersister = event.getPersister();
//            changeSet.add(new PersistentAuditData(entityPersister.getEntityName(), conversionService.convert(event.getId(), String.class), null));
//            for (int i = 0; i < entityPersister.getPropertyNames().length; ++i) {
//                if (!entityPersister.getPropertyUpdateability()[i]) {
//                    // Assuming that PostUpdateEvent#getOldState() returns database state of the record before modification.
//                    // Otherwise, we would have to execute SQL query to be sure of @Column(updatable = false) column value.
//                    newDbState[i] = event.getOldState()[i];
//                }
//                LOG.debug("I:{},dirty:{} contains:{}", new Object[]{i, event.getDirtyProperties(), Arrays.asList(ArrayUtils.toObject(event.getDirtyProperties())).contains(i)});
//                
//                if (newDbState[i] != null && event.getOldState()[i] != null && Arrays.asList(ArrayUtils.toObject(event.getDirtyProperties())).contains(i)) {
//                    LOG.debug("Updated:{}.{} Type:{} -> New/Old: {}/{}", new Object[] { entityPersister.getEntityName(), entityPersister.getPropertyNames()[i], entityPersister.getPropertyNames()[i].getClass(), newDbState[i], event.getOldState()[i] });
//                    ObjectDiffer objectDiffer = ObjectDifferFactory.getInstance();
//                    final Node root = objectDiffer.compare(newDbState[i], event.getOldState()[i]);
//                    final Node.Visitor visitor = new PrintingVisitor(newDbState[i], event.getOldState()[i]);
//                    
//                    root.visit(visitor);
//                    if (conversionService.canConvert(entityPersister.getPropertyNames()[i].getClass(), String.class)) {
//                        LOG.debug("Conversion supported for class:{}", entityPersister.getPropertyNames()[i].getClass());
//                        // FIXME handle big decimal conversions. These values should be considered the same.Old value 155.886656000000000, new Value:155.886656
//                        String convertedNewValue = conversionService.convert(newDbState[i], String.class);
//                        String convertedOldValue = conversionService.convert(event.getOldState()[i], String.class);
//                        String key = event.getPersister().getEntityName()+"."+entityPersister.getPropertyNames()[i];
//                        changeSet.add(new PersistentAuditData(key, convertedOldValue, convertedNewValue));
//                    } else {
//                        LOG.debug("Conversion Not supportedsupported for class:{},", entityPersister.getPropertyNames()[i].getClass());
//                        
//                    }
//                }
//            }
//        }
//        return newDbState;
//    }
}
