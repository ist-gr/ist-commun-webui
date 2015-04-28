package gr.com.ist.commun.core.domain.security.audit.listener;

import java.util.Set;

import gr.com.ist.commun.core.domain.security.EntityBase;

import org.hibernate.envers.Audited;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ScannedGenericBeanDefinition;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AssignableTypeFilter;

public class EnversConfigurationListener implements ApplicationListener<ContextRefreshedEvent> {
    
    private static final Logger LOG = LoggerFactory.getLogger(EnversConfigurationListener.class);
    
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter(new AssignableTypeFilter(EntityBase.class));
// FIXME: Should enable the following logic
//        Set<BeanDefinition> components = provider.findCandidateComponents(gr.hcaa.avis.core.domain.DomainConfiguration.class.getPackage().getName());
//        components.addAll(provider.findCandidateComponents(gr.com.ist.commun.core.domain.DomainConfiguration.class.getPackage().getName()));
        Set<BeanDefinition> components = provider.findCandidateComponents(gr.com.ist.commun.core.domain.DomainConfiguration.class.getPackage().getName());
        for (BeanDefinition component : components) {
            if (component instanceof ScannedGenericBeanDefinition) {
                AnnotationMetadata annotationMetadata = ((ScannedGenericBeanDefinition) component).getMetadata();
                if (!annotationMetadata.hasAnnotation(Audited.class.getName())) {
                    LOG.warn("DomainObject:{} is NOT audited. You have to add @Audited annotation to enable auditing.", component.getBeanClassName());
                }
            }
        }
    }
}
