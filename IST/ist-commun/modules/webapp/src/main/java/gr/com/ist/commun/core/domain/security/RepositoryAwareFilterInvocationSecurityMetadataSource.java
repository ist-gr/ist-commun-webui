package gr.com.ist.commun.core.domain.security;


import gr.com.ist.commun.core.domain.security.annotations.EntityAuthority;
import gr.com.ist.commun.core.domain.security.annotations.HasAnyRole;
import gr.com.ist.commun.core.domain.security.annotations.Implies;
import gr.com.ist.commun.core.domain.security.annotations.SecuredWith;
import gr.com.ist.commun.core.domain.security.annotations.UrlAuthority;
import gr.com.ist.commun.core.repository.security.InMemoryAuthoritiesRepository;
import gr.com.ist.commun.utils.SecurityUtils;
import gr.com.ist.commun.utils.SecurityUtils.SecurityMethods;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.access.expression.SecurityExpressionHandler;
import org.springframework.security.web.FilterInvocation;
import org.springframework.security.web.access.expression.ExpressionBasedFilterInvocationSecurityMetadataSource;
import org.springframework.security.web.access.intercept.FilterInvocationSecurityMetadataSource;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;


@Component("repositoryAwareFilterInvocationSecurityMetadataSource")
@SuppressWarnings("unchecked")
public class RepositoryAwareFilterInvocationSecurityMetadataSource implements FilterInvocationSecurityMetadataSource, InitializingBean, ApplicationContextAware {

    static final Logger LOG = LoggerFactory.getLogger(RepositoryAwareFilterInvocationSecurityMetadataSource.class);
    private final LinkedHashMap<RequestMatcher, Collection<ConfigAttribute>> requestMap = new LinkedHashMap<>();
    private ApplicationContext ctx;
    private ExpressionBasedFilterInvocationSecurityMetadataSource delegate;

    private static final Class<? extends Annotation> ELEMENT_COLLECTION_ANNOTATION = ElementCollection.class;
    private static final Class<Annotation>[] ASSOCIATION_ANNOTATIONS = new Class[] { ManyToOne.class, OneToMany.class, ManyToMany.class, OneToOne.class };
    private static final Class<? extends Annotation> EMBEDDED_ANNOTATION = Embedded.class;
    private static final Class<? extends Annotation> EMBEDDABLE_ANNOTATION = Embeddable.class;

    @Autowired
    private SecurityExpressionHandler<FilterInvocation> expressionHandler;

    @Autowired
    private InMemoryAuthoritiesRepository inMemoryAuthoritiesRepository;

    @Autowired
    private ResourceMappings resourceMappings;
    
    @Autowired
    MessageSource messageSource;
    
    private Set<ResourceMetadata> resources = new HashSet<ResourceMetadata>();

    public Collection<ConfigAttribute> getAllConfigAttributes() {
        return this.delegate.getAllConfigAttributes();
    }

    public Collection<ConfigAttribute> getAttributes(Object object) {
        return this.delegate.getAttributes(object);
    }

    public boolean supports(Class<?> clazz) {
        return this.delegate.supports(clazz);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.inMemoryAuthoritiesRepository.setAuthorities(new HashSet<AuthorityRole>());
        Set<AuthorityRole> systemRoles = new HashSet<AuthorityRole>();
        //Expose general system administrator first
        AuthorityRole generalAdmin = new AuthorityRole();
        generalAdmin.setAuthority(User.ADMIN_AUTHORITY_NAME);
        systemRoles.add(generalAdmin);
        // ResourceMetadata return x2 the rest api URLs so we need to exclude the duplicates
        for (ResourceMetadata resourceMetadata : resourceMappings) {
            if (resourceMetadata.isExported()) {
                resources.add(resourceMetadata);
            }
        }

        for (ResourceMetadata resourceMetadata : resources) {
            String url = resourceMetadata.getPath().toString().substring(1);
            List<String> references = new ArrayList<String>();
            //recursive method that supports @Embedded and @ElementCollection annotated fields in repository entities
            accummulateReferences(resourceMetadata.getDomainType(), references);

            Set<ConfigAttribute> viewerAuthorityExpression = new HashSet<ConfigAttribute>();
            Set<ConfigAttribute> editorAuthorityExpression = new HashSet<ConfigAttribute>();
            Set<ConfigAttribute> adminAuthorityExpression = new HashSet<ConfigAttribute>();

            Set<String> referencedRoles = new HashSet<String>();
            for (String reference : references) {
                String path = getRepositoryNameFromEntity(reference);
                if (path != null) {
                    referencedRoles.add(path + SecurityUtils.convertSecurityMethodToRoleSuffix(SecurityMethods.VIEWER));
                }
            }

            fillInMemoryAuthoritiesSet(referencedRoles, systemRoles, url);

            SecurityConfig viewerConf = new SecurityConfig("hasAnyRole('" + url + SecurityUtils.convertSecurityMethodToRoleSuffix(SecurityMethods.VIEWER) + "', '" + User.ADMIN_AUTHORITY_NAME + "')");
            SecurityConfig editorConf = new SecurityConfig("hasAnyRole('" + url + SecurityUtils.convertSecurityMethodToRoleSuffix(SecurityMethods.EDITOR) + "', '" + User.ADMIN_AUTHORITY_NAME + "')");
            SecurityConfig adminConf = new SecurityConfig("hasAnyRole('" + url + SecurityUtils.convertSecurityMethodToRoleSuffix(SecurityMethods.ADMIN) + "', '" + User.ADMIN_AUTHORITY_NAME + "')");

            viewerAuthorityExpression.add(viewerConf);
            editorAuthorityExpression.add(editorConf);
            adminAuthorityExpression.add(adminConf);

            requestMap.put(new AntPathRequestMatcher("/api/" + url + "/**", "GET"), viewerAuthorityExpression);
            for (String verb : new String[] { "PUT", "POST" }) {
                requestMap.put(new AntPathRequestMatcher("/api/" + url + "/**", verb), editorAuthorityExpression);
            }
            requestMap.put(new AntPathRequestMatcher("/api/" + url + "/**", "DELETE"), adminAuthorityExpression);
        }
        setupControllerSecurity(requestMap, resources);
        fillRequestMapWithGlobalAppUrls(requestMap);
        this.delegate = new ExpressionBasedFilterInvocationSecurityMetadataSource(requestMap, expressionHandler);
        Set<AuthorityRole> allAuths = this.inMemoryAuthoritiesRepository.getAuthorities();
        mergeDuplicateRoles(allAuths, systemRoles);
        //LOG.debug("{}", allAuths);
        this.inMemoryAuthoritiesRepository.setAuthorities(allAuths);
        checkExistanceOfAuthoritiesInPropertiesFile(allAuths);
    }

    private void checkExistanceOfAuthoritiesInPropertiesFile(Set<AuthorityRole> allAuths) {
        Locale currentLocale = LocaleContextHolder.getLocale();
        List<String> authMissingFromPropertyFile = new ArrayList<String>();
        for (AuthorityRole auth : allAuths) {
             if (messageSource.getMessage(auth.getAuthority(), null, null, currentLocale) == null) {
                 authMissingFromPropertyFile.add(auth.getAuthority());
             }
        }
        if (!authMissingFromPropertyFile.isEmpty()) {
            throw new RuntimeException("Following authorities were not translated: " + authMissingFromPropertyFile);
        }
    }
        
    /**
     * Basic method that sets up URL expression-based security on controllers and controller methods if annotated with
     * @SecuredWith annotation.
     * 
     * @param requestMap
     * @param resources
     */
    private void setupControllerSecurity(LinkedHashMap<RequestMatcher, Collection<ConfigAttribute>> requestMap, Set<ResourceMetadata> resources) {
        Map<String, Object> controllers = ctx.getBeansWithAnnotation(Controller.class);
        Map<String, Object> restControllers = ctx.getBeansWithAnnotation(RestController.class);
        controllers.putAll(restControllers);
        Set<AuthorityRole> allRoles = new HashSet<AuthorityRole>();
        for (Object controller : controllers.values()) {
            Set<AuthorityRole> controllerRoles = new HashSet<AuthorityRole>();
            Set<String> controllerUrls = new HashSet<String>();
            if (controller.getClass().isAnnotationPresent(RequestMapping.class)) {
                for (String url : controller.getClass().getAnnotation(RequestMapping.class).value()) {
                    controllerUrls.add(normalize(url));
                }
            }
            if (controller.getClass().isAnnotationPresent(SecuredWith.class)) {
                for (HasAnyRole hasAnyRole : controller.getClass().getAnnotation(SecuredWith.class).hasAnyRole()) {
                    securedWithAnnotationProcesor(hasAnyRole, controllerRoles);
                }
            } 
            for (Method method : controller.getClass().getDeclaredMethods()) {
                if (method.isAnnotationPresent(SecuredWith.class)) {
                    Set<String> methodUrls = new HashSet<String>();
                    Set<String> verbs = new HashSet<String>();
                    Set<AuthorityRole> methodRoles = new HashSet<AuthorityRole>();
                    if (method.isAnnotationPresent(RequestMapping.class)) {
                        if (method.getAnnotation(RequestMapping.class).value().length > 0){
                            for (String methodUrl : method.getAnnotation(RequestMapping.class).value()) {
                                if (controllerUrls.size() > 0) {
                                    for (String controllerUrl : controllerUrls) {
                                        methodUrls.add(normalize(controllerUrl) + normalize(methodUrl));;
                                    }
                                } else {
                                    methodUrls.add(normalize(methodUrl));
                                }
                            }
                        } else {
                            for (String url : controller.getClass().getAnnotation(RequestMapping.class).value()) {
                                methodUrls.add(normalize(url));
                            }
                        }
                        for (RequestMethod verb : method.getAnnotation(RequestMapping.class).method()) {
                            verbs.add(verb.name());
                        }
                    }
                    for (HasAnyRole methodHasAnyRole : method.getAnnotation(SecuredWith.class).hasAnyRole()) {
                        securedWithAnnotationProcesor(methodHasAnyRole, methodRoles);
                    }
                    constructSecurityExpressionsForCustomControllers(methodUrls, methodRoles, new HashSet<ConfigAttribute>(), verbs);
                    mergeDuplicateRoles(allRoles, methodRoles);
                }
            }
            constructSecurityExpressionsForCustomControllers(controllerUrls, controllerRoles, new HashSet<ConfigAttribute>(), null);
            mergeDuplicateRoles(allRoles, controllerRoles);
            mergeDuplicateRoles(this.inMemoryAuthoritiesRepository.getAuthorities(), allRoles);
        }
    }

    /**
     * Normalizes controller and controller methods @RequestMapping values to the format /***
     * 
     * @param url
     * @return
     */
    private String normalize(String url) {
        if (!url.startsWith("/")) 
            url =  "/" + url;
        if(url.endsWith("/")) 
            url =  url.substring(0, url.length()-1);
        return url;
    }

    /**
     * Merges duplicate authorities. 
     * When duplicate roles exist in two sets, included roles are copied to one 
     * role in order to contain all included roles. The set containing all roles 
     * and included roles is the target set.
     * 
     * @param authorityRoleSetTarget
     * @param authorityRoleSetSrc
     */
    private void mergeDuplicateRoles(Set<AuthorityRole> authorityRoleSetTarget, Set<AuthorityRole> authorityRoleSetSrc) {
        authorityRoleSetTarget.addAll(authorityRoleSetSrc);
        for (AuthorityRole targetRole : authorityRoleSetTarget) {
            for (AuthorityRole srcRole : authorityRoleSetSrc) {
                if (srcRole.getAuthority().equals(targetRole.getAuthority())) {
                    if (targetRole.getIncludedRoles() != null && srcRole.getIncludedAuthorities() != null) {
                        targetRole.getIncludedAuthorities().addAll(srcRole.getIncludedAuthorities());
                    } else if (srcRole.getIncludedAuthorities() != null) {
                        targetRole.setIncludedRoles(srcRole.getIncludedRoles());
                    }
                }
            }
        }
    }

    /**
     * Constructs Spring EL security expressions needed in order to secure URLs of custom controllers.
     * 
     * @param urls
     * @param roles
     * @param authExpression
     * @param verbs
     */
    private void constructSecurityExpressionsForCustomControllers(Set<String> urls, Set<AuthorityRole> roles, Set<ConfigAttribute> authExpression, Set<String> verbs) {
        for (String url : urls) {
            String expression = "hasAnyRole('";
            Iterator<AuthorityRole> rolesIt = roles.iterator();
            if (roles.size() == 1) {
                expression += rolesIt.next().getAuthority() + "','" + User.ADMIN_AUTHORITY_NAME + "')";
            } else if (roles.size() > 1) {
                expression += rolesIt.next().getAuthority();
                while (rolesIt.hasNext()) {
                    expression += "','" + rolesIt.next().getAuthority();
                }
                expression += "','" + User.ADMIN_AUTHORITY_NAME + "')";
            } else {
                break;
            }
            SecurityConfig config = new SecurityConfig(expression);
            authExpression.add(config);
            if (verbs != null && verbs.size() > 0) {
                for (String verb : verbs) {
                    requestMap.put(new AntPathRequestMatcher("/api" + url + "/**", verb), authExpression);
                    LOG.info("Secured: /api" + url + "/** path with: " + expression + ", httpMethod: " + verb);
                }
            } else {
                requestMap.put(new AntPathRequestMatcher("/api" + url + "/**"), authExpression);
                LOG.info("Secured: /api" + url + "/** path with: " + expression);
            }

        }
    }

    /**
     * Processes all @SecuredWith annotations and child elements such as 
     * @HasAnyRole, @EntityAuthority, @Implies, @UrlAuthority
     * 
     * @param hasAnyRole
     * @param roles
     */
    private void securedWithAnnotationProcesor(HasAnyRole hasAnyRole, Set<AuthorityRole> roles) {

        for (EntityAuthority entityAuthority : hasAnyRole.entityAuthority()) {
            String path = getRepositoryNameFromEntity(entityAuthority.type().getSimpleName());
            if (path != null) {
                AuthorityRole role = new AuthorityRole();
                role.setAuthority(path + SecurityUtils.convertSecurityMethodToRoleSuffix(entityAuthority.method()));
                roles.add(role);
            }
        }
        for (UrlAuthority urlAuthority : hasAnyRole.urlAuthority()) {
            AuthorityRole role = new AuthorityRole();
            role.setAuthority(urlAuthority.role() + SecurityUtils.convertSecurityMethodToRoleSuffix(urlAuthority.method()));
            Set<AuthorityRole> includedRoles = new HashSet<AuthorityRole>();
            for (Implies imply : hasAnyRole.implies()) {
                AuthorityRole includedRole = new AuthorityRole();
                for (ResourceMetadata resource : resourceMappings) {
                    if (imply.entityAuthority().type().equals(resource.getDomainType())) {
                        includedRole.setAuthority(resource.getPath().toString().substring(1) + SecurityUtils.convertSecurityMethodToRoleSuffix(imply.entityAuthority().method()));
                        includedRoles.add(includedRole);
                    }
                }
            }
            role.setIncludedRoles(includedRoles);
            for (AuthorityRole existingRole : roles) {
                if (existingRole.getAuthority().equals(role.getAuthority())) {
                    if (existingRole.getIncludedRoles() != null) {
                        existingRole.getIncludedAuthorities().addAll(role.getIncludedAuthorities());
                    } else {
                        existingRole.setIncludedRoles(role.getIncludedRoles());
                    }
                }
            }
            roles.add(role);
        }
    }
    /**
     * Given an entity type it returns the Spring Data Rest 
     * repository name the corresponds to the entity
     * 
     * @param type
     * @return
     */
    public String getRepositoryNameFromEntity(String type) {
        for (ResourceMetadata resource : resourceMappings) {
            if (type.equals(resource.getDomainType().getSimpleName())) {
                return resource.getPath().toString().substring(1);
            }
        }
        return null;
    }

    /**
     * Recursive method that finds all referenced entities within a domain class and adds them to
     * a list. It scans also for @Embedded annotations and searches recursively within Embeddables 
     * for any referenced entities.
     *  
     * @param domainClass
     * @param references
     * @throws ClassNotFoundException
     */
    private void accummulateReferences(Class<?> domainClass, List<String> references) throws ClassNotFoundException {
        for (Field field : domainClass.getDeclaredFields()) {
            // Check for associations inside the entity (OneToOne, ManyToOne, etc)
            for (Class<Annotation> associationAnnotation : ASSOCIATION_ANNOTATIONS) {
                if (field.isAnnotationPresent(associationAnnotation)) {
                    if (field.getGenericType() instanceof ParameterizedType) {
                        for (Type type : ((ParameterizedType) field.getGenericType()).getActualTypeArguments()) {
                            references.add(((Class<?>) type).getSimpleName());
                        }
                    } else {
                        references.add(((Class<?>) field.getGenericType()).getSimpleName());
                    }
                }
            }
            // Check for embedded fields in the entity
            if (field.isAnnotationPresent(EMBEDDED_ANNOTATION)) {
                if (field.getGenericType() instanceof ParameterizedType) {
                    for (Type type : ((ParameterizedType) field.getGenericType()).getActualTypeArguments()) {
                        accummulateReferences((Class<?>) type, references);
                    }
                } else {
                    accummulateReferences((Class<?>) field.getGenericType(), references);
                }
            }
            // Check for ElementCollections. If the class of the field is an embeddable search for references in there. Else, do nothing
            if (field.isAnnotationPresent(ELEMENT_COLLECTION_ANNOTATION)) {
                if (field.getGenericType() instanceof ParameterizedType) {
                    for (Type type : ((ParameterizedType) field.getGenericType()).getActualTypeArguments()) {
                        if (((Class<?>) type).isAnnotationPresent(EMBEDDABLE_ANNOTATION)) {
                            accummulateReferences((Class<?>) type, references);
                        }
                    }
                } else {
                    if (((Class<?>) field.getGenericType()).isAnnotationPresent(EMBEDDABLE_ANNOTATION)) {
                        accummulateReferences((Class<?>) field.getGenericType(), references);
                    }
                }
            }
        }
    }

    private void fillInMemoryAuthoritiesSet(Set<String> referencedRoles, Set<AuthorityRole> systemRoles, String url) {

        AuthorityRole viewAuthority = new AuthorityRole();
        viewAuthority.setAuthority(url + SecurityUtils.convertSecurityMethodToRoleSuffix(SecurityMethods.VIEWER));
        systemRoles.add(viewAuthority);
        AuthorityRole editAuthority = new AuthorityRole();
        editAuthority.setId(UUID.randomUUID());


        editAuthority.setAuthority(url + SecurityUtils.convertSecurityMethodToRoleSuffix(SecurityMethods.EDITOR));
        Set<AuthorityRole> editIncludedRoles = new HashSet<AuthorityRole>();
        for (String referencedRole : referencedRoles) {
            AuthorityRole included = new AuthorityRole();
            included.setAuthority(referencedRole);
            editIncludedRoles.add(included);
        }
        editIncludedRoles.add(viewAuthority);
        editAuthority.setIncludedRoles(editIncludedRoles);
        systemRoles.add(editAuthority);
        AuthorityRole adminAuthority = new AuthorityRole();
        adminAuthority.setAuthority(url + SecurityUtils.convertSecurityMethodToRoleSuffix(SecurityMethods.ADMIN));
        Set<AuthorityRole> adminIncludedRoles = new HashSet<AuthorityRole>();
        adminIncludedRoles.add(editAuthority);
        adminAuthority.setIncludedRoles(adminIncludedRoles);
        systemRoles.add(adminAuthority);
    }
    //TODO: Find a way to integrate security.xml with current implementation in order to avoid setting generic security parameters in here
    private void fillRequestMapWithGlobalAppUrls(LinkedHashMap<RequestMatcher, Collection<ConfigAttribute>> requestMap) {
        Set<ConfigAttribute> isAuthenticatedExpression = new HashSet<ConfigAttribute>();

        SecurityConfig isAuthenticatedConf = new SecurityConfig("isAuthenticated()");
        isAuthenticatedExpression.add(isAuthenticatedConf);
        requestMap.put(new AntPathRequestMatcher("/api/security/userDetails"), isAuthenticatedExpression);
        /*XXX: The following line overrides default repository security settings in order to allow any 
         * authenticated user to edit his profile*/
        requestMap.put(new AntPathRequestMatcher("/api/passwordPolicies/**","GET"), isAuthenticatedExpression);
        requestMap.put(new AntPathRequestMatcher("/api/**"), isAuthenticatedExpression);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.ctx = applicationContext;
    }
    /**
     * Returns a new ResourceMetadata set with eliminated duplicates
     * 
     * @return
     */
    public Set<ResourceMetadata> getResources() {
        return resources;
    }
}
