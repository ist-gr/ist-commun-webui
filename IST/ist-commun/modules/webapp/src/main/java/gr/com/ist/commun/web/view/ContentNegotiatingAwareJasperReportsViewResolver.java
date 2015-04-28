package gr.com.ist.commun.web.view;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.view.jasperreports.JasperReportsViewResolver;

public class ContentNegotiatingAwareJasperReportsViewResolver extends JasperReportsViewResolver {
    private static Logger LOG = LoggerFactory.getLogger(ContentNegotiatingAwareJasperReportsViewResolver.class);
    
    @Override
    protected boolean canHandle(String viewName, Locale locale) {
        // XXX: extensions and media types should come from configuration and be shared with view
        if (!(viewName.endsWith(".pdf") || viewName.endsWith(".xls") || viewName.endsWith(".html") || viewName.endsWith(".csv") || viewName.endsWith(".docx") || viewName.endsWith(".pptx"))) {
            return false;
        }

        String baseName = viewName.replaceFirst("\\.[^\\.]+$", "");

        Resource resource = this.getApplicationContext().getResource(getPrefix() + baseName + getSuffix());

        if (LOG.isDebugEnabled()) {
            LOG.debug(resource+" exists: "+resource.exists());
        }
        
        return resource.exists();
    }
    
}
