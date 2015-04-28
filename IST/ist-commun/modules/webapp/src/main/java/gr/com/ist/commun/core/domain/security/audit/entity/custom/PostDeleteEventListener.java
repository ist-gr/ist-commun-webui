package gr.com.ist.commun.core.domain.security.audit.entity.custom;

import org.hibernate.envers.configuration.AuditConfiguration;
import org.hibernate.envers.event.EnversPostDeleteEventListenerImpl;
import org.hibernate.event.spi.PostDeleteEvent;
import org.springframework.core.convert.ConversionService;

public class PostDeleteEventListener extends EnversPostDeleteEventListenerImpl {

    private static final long serialVersionUID = 1L;
    
    private ConversionService conversionService;
       
    public void setConversionService(ConversionService conversionService) {
        this.conversionService = conversionService;
    }
    
    @Override
    public void onPostDelete(PostDeleteEvent event) {
        if (conversionService == null) {
            this.conversionService = StaticContextAccessor.getBean("conversionService");
        }
        String entityName = event.getPersister().getEntityName();
        if (getAuditConfiguration().getEntCfg().isVersioned(entityName)) {
            checkIfTransactionInProgress(event.getSession());
        }
// FIXME
//        Set<PersistentAuditData> changeSet = new HashSet<PersistentAuditData>();
//        changeSet.add(new PersistentAuditData(event.getPersister().getEntityName(), conversionService.convert(event.getId(), String.class), null));
//        for (int i = 0; i < event.getPersister().getPropertyNames().length; ++i) {
//            if (conversionService.canConvert(event.getPersister().getPropertyNames()[i].getClass(), String.class)) {
//                String convertedDeletedValue = conversionService.convert(event.getDeletedState()[i], String.class);
//                changeSet.add(new PersistentAuditData(event.getPersister().getEntityName() + "." + event.getPersister().getPropertyNames()[i], convertedDeletedValue, null));
//            }
//        }
//        
//        PersistentAuditEvent auditEvent = new PersistentAuditEvent();
//        auditEvent.setAuditEventType("ENTITY_DELETE");
//        auditEvent.setPrincipal(AuditUtils.getPrincipal());
//        event.getSession().persist(auditEvent);
//        
//        for (PersistentAuditData auditData : changeSet) {
//            auditData.setPersistentAuditEvent(auditEvent);
//            event.getSession().persist(auditData);
//        }
    }

    protected PostDeleteEventListener(AuditConfiguration enversConfiguration) {
        super(enversConfiguration);
    }
}
