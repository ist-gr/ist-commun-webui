package gr.com.ist.commun.web.controller;

import gr.com.ist.commun.core.domain.security.annotations.HasAnyRole;
import gr.com.ist.commun.core.domain.security.annotations.SecuredWith;
import gr.com.ist.commun.core.domain.security.annotations.UrlAuthority;
import gr.com.ist.commun.utils.SecurityUtils.SecurityMethods;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Controller for view and managing Log Level at runtime.
 */
@RestController
@RequestMapping("/logs")
@SecuredWith(
        hasAnyRole = @HasAnyRole(
                urlAuthority = {@UrlAuthority(role = "logs", method = SecurityMethods.ADMIN)}))
public class LogsController {

    public static class LoggerDTO {

        private String name;

        private String level;

        public LoggerDTO(Logger logger) {
            this.name = logger.getName();
            this.level = logger.getEffectiveLevel().toString();
        }

        @JsonCreator
        public LoggerDTO() {
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getLevel() {
            return level;
        }

        public void setLevel(String level) {
            this.level = level;
        }

        @Override
        public String toString() {
            return "LoggerDTO{" +
                    "name='" + name + '\'' +
                    ", level='" + level + '\'' +
                    '}';
        }
    }

    @RequestMapping(method = RequestMethod.GET,
            produces = "application/json")
    public List<LoggerDTO> getList() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        List<LoggerDTO> loggers = new ArrayList<>();
        for (ch.qos.logback.classic.Logger logger : context.getLoggerList()) {
            loggers.add(new LoggerDTO(logger));
        }
        return loggers;
    }

    @RequestMapping(method = RequestMethod.PUT)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeLevel(@RequestBody LoggerDTO jsonLogger) {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.getLogger(jsonLogger.getName()).setLevel(Level.valueOf(jsonLogger.getLevel()));
    }
}
