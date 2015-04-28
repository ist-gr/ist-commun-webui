package gr.com.ist.commun.web.view;

import java.util.Map;

import net.sf.jasperreports.engine.JRExporter;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.engine.export.JRHtmlExporter;
import net.sf.jasperreports.engine.export.JRHtmlExporterParameter;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.export.JRXlsExporter;
import net.sf.jasperreports.engine.export.ooxml.JRDocxExporter;
import net.sf.jasperreports.engine.export.ooxml.JRPptxExporter;

import org.springframework.core.io.Resource;
import org.springframework.web.servlet.view.jasperreports.ConfigurableJasperReportsView;

public class ContentNegotiatingJasperReportsView extends ConfigurableJasperReportsView {
    public ContentNegotiatingJasperReportsView() {
        super();
        // setting to not null to keep onInit() happy
        setExporterClass(JRExporter.class);
    }
    
    @Override
    protected JasperReport loadReport() {
        String url = getUrl();
        if (url == null) {
            return null;
        }

        //XXX: extensions and media types should come from configuration
        if (getBeanName().endsWith(".pdf")) {
            this.setExporterClass(JRPdfExporter.class);
            this.setContentType("application/pdf");
            this.setUseWriter(false);
        } else if (getBeanName().endsWith(".xls")) {
            this.setExporterClass(JRXlsExporter.class);
            this.setContentType("application/vnd.ms-excel");
            this.setUseWriter(false);
        } else if (getBeanName().endsWith(".html")) {
            this.setExporterClass(JRHtmlExporter.class);
            @SuppressWarnings("unchecked")
            Map<JRHtmlExporterParameter, Object> exporterParameters = (Map<JRHtmlExporterParameter, Object>) this.getExporterParameters();
            exporterParameters.put(JRHtmlExporterParameter.IS_USING_IMAGES_TO_ALIGN, Boolean.FALSE);
            this.setExporterParameters(exporterParameters);
            this.setContentType("text/html;charset=UTF-8");
            this.setUseWriter(true);
        } else if (getBeanName().endsWith(".csv")) {
            this.setExporterClass(JRCsvExporter.class);
            this.setContentType("text/csv;charset=UTF-8");
            this.setUseWriter(true);
        } else if (getBeanName().endsWith(".docx")) {
            this.setExporterClass(JRDocxExporter.class);
            this.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            this.setUseWriter(false);
        } else if (getBeanName().endsWith(".pptx")) {
            this.setExporterClass(JRPptxExporter.class);
            this.setContentType("application/vnd.openxmlformats-officedocument.presentationml.presentation");
            this.setUseWriter(false);
        } else if (getBeanName().endsWith(".xml")) {
            this.setExporterClass(JRPptxExporter.class);
            this.setContentType("application/xml;charset=UTF-8");
            this.setUseWriter(true);
        } else {
            return null;
        }
        
        // cut the media type extension from the url
        url = url.replaceFirst("\\.[^\\.]+\\.([^\\.]+)$", ".$1");
        
        // TODO: Avoid loading the report once for each supported type
        Resource mainReport = getApplicationContext().getResource(url);
        if (!mainReport.exists()) {
            return null;
        }
        return loadReport(mainReport);
    }
    
}
