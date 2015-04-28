package gr.com.ist.commun.core.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ValidationErrorsExceptionMessage {
    private static final Logger LOG = LoggerFactory.getLogger(ValidationErrorsException.class);
    
    private final List<ValidationError> errors = new ArrayList<ValidationError>();

    public ValidationErrorsExceptionMessage(ValidationErrorsException violationException,
            MessageSourceAccessor messageSourceAccessor) {

        List<ObjectError> validationErrors = violationException.getErrors().getAllErrors();
        Locale currentLocale =  LocaleContextHolder.getLocale();

        for (final ObjectError error: validationErrors) {
            FieldError fieldError = error instanceof FieldError ? (FieldError) error : null;
            final List<Object> args = new ArrayList<Object>();
            args.add(error.getObjectName());
            args.add(fieldError == null ? null : fieldError.getField());
            args.add(fieldError == null ? null : fieldError.getRejectedValue());
            if (null != error.getArguments()) {
                for (Object o : error.getArguments()) {
                    args.add(o);
                }
            }
            MessageSourceResolvable messageSourceResolvable = new MessageSourceResolvable() {
                @Override
                public String getDefaultMessage() {
                    return error.getDefaultMessage();
                }
                @Override
                public String[] getCodes() {
                    return error.getCodes();
                }
                @Override
                public Object[] getArguments() {
                    return args.toArray();
                }
            };
            String localizedErrorMessage = null;
            try {
                localizedErrorMessage = messageSourceAccessor.getMessage(messageSourceResolvable, currentLocale);
            } catch (NoSuchMessageException e) {
                // XXX Should log a warning since localized message was not found and no default message was provided?
            }
            if (fieldError != null) {
                this.errors.add(new ValidationError(error.getObjectName(), fieldError.getField(), localizedErrorMessage, fieldError.getCode(), fieldError.getCodes(), error.getArguments(), fieldError.getRejectedValue()));
            } else {
                this.errors.add(new ValidationError(error.getObjectName(), null, localizedErrorMessage, error.getCode(), error.getCodes(), error.getArguments(), null));
            }
        }
    }
    
    @JsonProperty("errors")
    public List<ValidationError> getErrors() {
        return errors;
    }

    public static class ValidationError {

        private final String entity;
        private final String property;
        private final String message;
        private String code;
        private String[] codes;
        private Object[] arguments;
        private final Object invalidValue;

        public ValidationError(String entity, String fieldPath, String message, String code, String[] codes, Object[] arguments, Object invalidValue) {
            this.entity = entity;
            this.property = fieldPath;
            this.message = message;
            this.code = code;
            this.codes = codes;
            this.arguments = arguments;
            this.invalidValue = invalidValue;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String[] getCodes() {
            return codes;
        }

        public void setCodes(String[] codes) {
            this.codes = codes;
        }

        public Object[] getArguments() {
            return arguments;
        }

        public void setArguments(Object[] arguments) {
            this.arguments = arguments;
        }

        public String getEntity() {
            return entity;
        }

        public String getProperty() {
            return property;
        }

        public String getMessage() {
            return message;
        }

        public Object getInvalidValue() {
            return invalidValue;
        }

    }

}
