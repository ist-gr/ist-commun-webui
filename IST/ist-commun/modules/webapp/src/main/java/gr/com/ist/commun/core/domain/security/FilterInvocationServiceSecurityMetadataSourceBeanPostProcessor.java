package gr.com.ist.commun.core.domain.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.security.web.access.intercept.FilterInvocationSecurityMetadataSource;
import org.springframework.stereotype.Component;

@Component
public class FilterInvocationServiceSecurityMetadataSourceBeanPostProcessor implements BeanPostProcessor {
    @Autowired
    private RepositoryAwareFilterInvocationSecurityMetadataSource metadataSource;

    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        if (bean instanceof FilterInvocationSecurityMetadataSource) {
            return metadataSource;
        }
        return bean;
    }

    public Object postProcessAfterInitialization(Object bean, String beanName) {
        return bean;
    }
}