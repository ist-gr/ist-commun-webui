package gr.com.ist.commun.core.domain.security.audit.entity;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;

public class PropertyChangeAuditJacksonFilter extends SimpleBeanPropertyFilter {

    @Override
    public void serializeAsField(Object pojo, JsonGenerator jgen, SerializerProvider provider, PropertyWriter writer) throws Exception {
        if (include(writer)) {
            if (pojo instanceof PropertyChange) {
                Object removed = ((PropertyChange) pojo).getRemoved();
                Object added = ((PropertyChange) pojo).getAdded();
                if (writer.getName().equals("oldValue") || writer.getName().equals("newValue")) {
                    if (removed == null && added == null) {
                        writer.serializeAsField(pojo, jgen, provider);
                    } 
                } else if ((writer.getName().equals("added"))) {
                    if (added != null) {
                        writer.serializeAsField(pojo, jgen, provider);
                    }
                } else if ((writer.getName().equals("removed"))) {
                    if (removed != null) {
                        writer.serializeAsField(pojo, jgen, provider);
                    }
                } else {
                    writer.serializeAsField(pojo, jgen, provider);
                }
            } else if (!jgen.canOmitFields()) { // since 2.3
                writer.serializeAsOmittedField(pojo, jgen, provider);
            }
        }
    }
    
    @Override
    protected boolean include(BeanPropertyWriter writer) {
        return true;
    }

    @Override
    protected boolean include(PropertyWriter writer) {
        return true;
    }

}
