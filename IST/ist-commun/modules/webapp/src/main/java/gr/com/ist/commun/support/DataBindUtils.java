package gr.com.ist.commun.support;

import gr.com.ist.commun.core.validation.ValidationErrorsException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.util.Assert;
import org.springframework.validation.AbstractErrors;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DataBinder;
import org.springframework.validation.DefaultMessageCodesResolver;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.MessageCodesResolver;
import org.springframework.validation.ObjectError;
import org.springframework.validation.Validator;


public class DataBindUtils {
    
    public static class StringListErrors extends AbstractErrors {
        private static final long serialVersionUID = 8141826537389141361L;

        @SuppressWarnings("serial")
        private class StringListMessageCodesResolver extends DefaultMessageCodesResolver {
            Map<StringListPosition, String> fieldPositionToNameMap;
            
            public StringListMessageCodesResolver(Map<StringListPosition, String> fieldPositionToNameMap) {
                Assert.notNull(fieldPositionToNameMap);
                this.fieldPositionToNameMap = fieldPositionToNameMap;
            }

            @Override
            public String[] resolveMessageCodes(String errorCode, String objectName, String field, Class<?> fieldType) {
                StringListPosition position = StringListPosition.parse(field).withoutLine();
                // check that field is not a line error
                if (position.getLength() > 0) {
                    String translatedFieldName = fieldPositionToNameMap.get(position);
                    Assert.notNull(translatedFieldName, "oxi kai den bre8hke mapping gia to position: "+position+", fieldPositionToNameMap: "+fieldPositionToNameMap);
                    return super.resolveMessageCodes(errorCode, objectName, translatedFieldName, null);
                } else {
                    return super.resolveMessageCodes(errorCode, objectName, "", null);
                }
            }

        }
        
        public StringListErrors(List<String> target, String name, Map<StringListPosition, String> fieldPositionToNameMap) {
            this.name = name;
            this.target = target;
            messageCodesResolver = new StringListMessageCodesResolver(fieldPositionToNameMap);
        }

        
        private String name;
        private List<String> target;
        private List<ObjectError> globalErrors = new ArrayList<ObjectError>();
        private List<FieldError> fieldErrors = new ArrayList<FieldError>();
        private MessageCodesResolver messageCodesResolver;
        
        /**
         * Set the strategy to use for resolving errors into message codes.
         * Default is DefaultMessageCodesResolver.
         * @see DefaultMessageCodesResolver
         */
        public void setMessageCodesResolver(MessageCodesResolver messageCodesResolver) {
            Assert.notNull(messageCodesResolver, "MessageCodesResolver must not be null");
            this.messageCodesResolver = messageCodesResolver;
        }

        /**
         * Return the strategy to use for resolving errors into message codes.
         */
        public MessageCodesResolver getMessageCodesResolver() {
            return this.messageCodesResolver;
        }
        
        public String[] resolveMessageCodes(String errorCode) {
            return getMessageCodesResolver().resolveMessageCodes(errorCode, getObjectName());
        }

        public String[] resolveMessageCodes(String errorCode, String field) {
            Class<?> fieldType = getFieldType(field);
            return getMessageCodesResolver().resolveMessageCodes(
                    errorCode, getObjectName(), fixedField(field), fieldType);
        }
        
        @Override
        public String getObjectName() {
            return name;
        }

        @Override
        public void reject(String errorCode, Object[] errorArgs, String defaultMessage) {
            globalErrors.add(new ObjectError(name, resolveMessageCodes(errorCode), errorArgs, defaultMessage));
        }

        @Override
        public void rejectValue(String field, String errorCode, Object[] errorArgs, String defaultMessage) {
            fieldErrors.add(new FieldError(name, field, getFieldValue(field), true, resolveMessageCodes(errorCode, field), errorArgs,
                    defaultMessage));
        }

        @Override
        public void addAllErrors(Errors errors) {
            globalErrors.addAll(errors.getAllErrors());
        }

        @Override
        public List<ObjectError> getGlobalErrors() {
            return globalErrors;
        }

        @Override
        public List<FieldError> getFieldErrors() {
            return fieldErrors;
        }

        @Override
        public Object getFieldValue(String field) {
            StringListPosition position = StringListPosition.parse(field);
            // XXX trim() is bad smell since it is done in two places, here and in {#bind}
            return this.target.get(position.getLine()).substring(position.getStartIndex(), position.getLength() + position.getStartIndex()).trim();
        }
        
    }
    
    public static class StringListPosition {
        private int startIndex;
        private int length;
        private int line=-1;
        public StringListPosition(int startIndex, int length) {
            this.startIndex = startIndex;
            this.length = length;
        }
        public StringListPosition withoutLine() {
            return new StringListPosition(this.startIndex, this.length);
        }
        public StringListPosition withLine(Integer lineNo) {
            return new StringListPosition(lineNo, this.startIndex, this.length);
        }
        public StringListPosition(int line, int startIndex, int length) {
            this.line = line;
            this.startIndex = startIndex;
            this.length = length;
        }
        public StringListPosition(int line) {
            this.line = line;
        }
        public int getLine() {
            return this.line;
        }
        public int getStartIndex() {
            return this.startIndex;
        }
        public int getLength() {
            return this.length;
        }
        public static StringListPosition parse(String value) {
            Assert.notNull(value);
            if (value.length() == 0) {
                return new StringListPosition(0, 0);
            }
            String[] lineColumn = value.split(":");
            Assert.isTrue(lineColumn.length == 1 || lineColumn.length == 2, "Expecting line (':' column)");
            String[] fromToStrings = lineColumn[(lineColumn.length == 1) ? 0 : 1].split("-");
            if (fromToStrings.length == 1) {
                int line = Integer.parseInt(lineColumn[0]);
                return new StringListPosition(line-1, 0, 0);
            }
            Assert.isTrue(fromToStrings.length == 2, "Expecting begin index '-' end index. Index are numbered from one, end Index is inclusive");
            int line = lineColumn.length == 2 ? Integer.parseInt(lineColumn[0]) : 0;
            int fromIndex = Integer.parseInt(fromToStrings[0]);
            int toIndex = Integer.parseInt(fromToStrings[1]);
            return new StringListPosition(line-1, fromIndex-1, toIndex - fromIndex + 1);
        }
        @Override
        public String toString() {
            if (this.length == 0) {
                return this.line >= 0 ? String.valueOf(this.line+1) : "";
            }
            return (this.line >= 0 ? ((this.line+1)+":") : "")+(this.startIndex+1)+"-"+(this.length+this.startIndex);
        }
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + length;
            result = prime * result + line;
            result = prime * result + startIndex;
            return result;
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            StringListPosition other = (StringListPosition) obj;
            if (length != other.length)
                return false;
            if (line != other.line)
                return false;
            if (startIndex != other.startIndex)
                return false;
            return true;
        }
    }

    public interface ErrorTranslator {
        public static final DataBindUtils.DefaultErrorTranslator NO_ERROR_TRANSLATION = new DataBindUtils.DefaultErrorTranslator();
        void addGlobalError(ObjectError error, Errors translatedErrors, Object... translationHints);
        void addFieldError(FieldError error, Errors translatedErrors, Object... translationHints);
    }

    public static class DefaultErrorTranslator implements ErrorTranslator {
        @Override
        public void addGlobalError(ObjectError error, Errors translatedErrors, Object... translationHints) {
            translatedErrors.reject(error.getCode(), error.getArguments(), error.getDefaultMessage());
        }
        @Override
        public void addFieldError(FieldError error, Errors translatedErrors, Object... translationHints) {
            if (translatedErrors.getFieldType(error.getField()) != null) {
                translatedErrors.rejectValue(error.getField(), error.getCode(), error.getArguments(), error.getDefaultMessage());
            } else {
                translatedErrors.reject(error.getCode(), error.getArguments(),  error.getDefaultMessage());
            }
            
        }
        public static void addTranslatedErrors(Errors errorsToTranslate, Errors translatedErrors, ErrorTranslator errorTranslator, Object... translationHints) {
            if (errorTranslator == null) {
                errorTranslator = ErrorTranslator.NO_ERROR_TRANSLATION;
            }
            for (ObjectError error : errorsToTranslate.getGlobalErrors()) {
                errorTranslator.addGlobalError(error, translatedErrors, translationHints);
            }
            for (FieldError error : errorsToTranslate.getFieldErrors()) {
                errorTranslator.addFieldError(error, translatedErrors, translationHints);
            }
        }
    }

    /**
     * Sets property values on a target object using as input a string which
     * contains fixed length fields and applying type mismatch validations.
     * 
     * @param bindingDefinition
     *            array of the form: String fieldPath, Integer column width , ....
     *            <ul>
     *            <li>fieldPath specifies the target bean field.
     * fieldPath can be null and signifies that input should be skipped in that
     *       case. </li> <li>
     *       Column width specifies the number of characters to extract from the
     *       source.</li>
     *       </ul>
     * @param source
     *            String containing the values to be bound
     * @param target
     *            JavaBean to bind the values onto
     * @param objectName
     *            the name of the target object
     * @param validator
     *            (Optional) @Validator(s) to use by @DataBinder
     * @throws ValidationErrorsException containing the errors which occured during
     *         binding and the bound target object
     */
    public static void bind(Object[] bindingDefinition, String source, Object target, String objectName, Validator... validators) {
        StringWalker johnnie = new StringWalker(source);
        DataBinder binder = new DataBinder(target, objectName);
        binder.setIgnoreUnknownFields(false);
        MutablePropertyValues propertyValues = new MutablePropertyValues();
        for (int i = 0; i < bindingDefinition.length; i+=2) {
            String propertyName = (String) bindingDefinition[i];
            int width = (int) bindingDefinition[i+1];
            String value = johnnie.walk(width).trim();
            if (propertyName == null) {
                continue;
            }
            propertyValues.add(propertyName, value);
        }
        binder.bind(propertyValues);
        if (validators != null) {
            binder.replaceValidators(validators);
        }
        // validate the target object
        binder.validate();
        // get BindingResult that includes any validation errors
        BindingResult results = binder.getBindingResult();
        if (results.hasErrors()) {
            throw new ValidationErrorsException(results, results.getTarget());
        }
    }

}

