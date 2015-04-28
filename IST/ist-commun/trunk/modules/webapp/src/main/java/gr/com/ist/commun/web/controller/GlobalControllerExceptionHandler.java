package gr.com.ist.commun.web.controller;

import gr.com.ist.commun.core.validation.ValidationErrorsException;
import gr.com.ist.commun.core.validation.ValidationErrorsExceptionMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
class GlobalControllerExceptionHandler extends ResponseEntityExceptionHandler {
    @Autowired
    private MessageSourceAccessor messageSourceAccessor;

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ValidationErrorsException.class)
    @ResponseBody
    public ValidationErrorsExceptionMessage handleValidationErrorsException(ValidationErrorsException exception) {
        return new ValidationErrorsExceptionMessage(exception, messageSourceAccessor);
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public void handleSqlException(DataIntegrityViolationException ex) {
    }
    
}