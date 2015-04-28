package gr.com.ist.commun.core.domain.security.audit.entity.custom;

import gr.com.ist.commun.core.domain.security.EntityBase;

import org.springframework.core.convert.converter.Converter;

public class EntityBaseToStringConverter implements Converter<EntityBase,String> {

    @Override
    public String convert(EntityBase entityBase) {
        return entityBase.getId().toString();
    }

}
