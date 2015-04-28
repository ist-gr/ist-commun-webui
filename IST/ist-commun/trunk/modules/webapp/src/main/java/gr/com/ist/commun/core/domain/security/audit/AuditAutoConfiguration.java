/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gr.com.ist.commun.core.domain.security.audit;

import gr.com.ist.commun.core.domain.security.audit.entity.custom.EntityBaseToStringConverter;
import gr.com.ist.commun.core.domain.security.audit.listener.AuditListener;
import gr.com.ist.commun.core.domain.security.audit.listener.AuthenticationAuditListener;
import gr.com.ist.commun.core.domain.security.audit.listener.AuthorizationAuditListener;
import gr.com.ist.commun.core.domain.security.audit.listener.EnversConfigurationListener;
import gr.com.ist.commun.core.repository.security.audit.AuditEventRepository;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link AuditEvent}s.
 * 
 * @author Dave Syer
 */
@Configuration
public class AuditAutoConfiguration {

//    @Autowired(required = false)
//    private final AuditEventRepository auditEventRepository = new InMemoryAuditEventRepository();
	@Autowired
	private AuditEventRepository auditEventRepository;

	@Bean
	public AuditListener auditListener() throws Exception {
		return new AuditListener(this.auditEventRepository);
	}

	@Bean
//	@ConditionalOnClass(name = "org.springframework.security.authentication.event.AbstractAuthenticationEvent")
	public AuthenticationAuditListener authenticationAuditListener() throws Exception {
		return new AuthenticationAuditListener();
	}

	@Bean
//	@ConditionalOnClass(name = "org.springframework.security.access.event.AbstractAuthorizationEvent")
	public AuthorizationAuditListener authorizationAuditListener() throws Exception {
		return new AuthorizationAuditListener();
	}
	 
//	@ConditionalOnMissingBean(AuditEventRepository.class)
//	protected static class AuditEventRepositoryConfiguration {
//		@Bean
//		public AuditEventRepository auditEventRepository() throws Exception {
//			return new InMemoryAuditEventRepository();
//		}
//	}

	@Bean
	public EnversConfigurationListener enversConfigurationListener(){
	    return new EnversConfigurationListener();
	}
	
	@Bean
    public ConversionService conversionService() {
        ConversionServiceFactoryBean bean = new ConversionServiceFactoryBean();
        bean.setConverters(getConverters());
        bean.afterPropertiesSet();
        return bean.getObject();
    }

    @SuppressWarnings("rawtypes")
    private Set<Converter> getConverters() {
        Set<Converter> converters = new HashSet<Converter>();
        converters.add(new EntityBaseToStringConverter());
        return converters;
    }
}
