package gr.com.ist.commun.core.domain.security.audit.entity.custom;


import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.envers.configuration.AuditConfiguration;
import org.hibernate.envers.event.EnversIntegrator;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.service.classloading.spi.ClassLoaderService;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

public class CustomEnversIntegrator extends EnversIntegrator {
        
    @Override
    public void integrate(Configuration configuration, SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
        super.integrate(configuration, sessionFactory, serviceRegistry);
        final AuditConfiguration enversConfiguration = AuditConfiguration.getFor(configuration, serviceRegistry.getService(ClassLoaderService.class));
        EventListenerRegistry listenerRegistry = serviceRegistry.getService(EventListenerRegistry.class);
        listenerRegistry.prependListeners(EventType.POST_UPDATE, new PostUpdateEventListener(enversConfiguration));
        listenerRegistry.prependListeners(EventType.POST_DELETE, new PostDeleteEventListener(enversConfiguration));
    }
}
