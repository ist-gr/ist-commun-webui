package gr.com.ist.commun.web.controller;

import gr.com.ist.commun.core.domain.security.audit.AuditEvent;
import gr.com.ist.commun.core.service.security.audit.AuditEventService;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * REST controller for getting the audit events.
 */
@Controller
@RequestMapping("/audits")
public class AuditsController {

    @Inject
    private AuditEventService auditEventService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
//        binder.registerCustomEditor(LocalDateTime.class, new LocaleDateTimeEditor("yyyy-MM-dd", false));
    }

    @RequestMapping(value = "/all",
            method = RequestMethod.GET,
            produces = "application/json")
//    @RolesAllowed(AuthoritiesConstants.ADMIN) TODO
    public List<AuditEvent> findAll() {
        return auditEventService.findAll();
    }

    @RequestMapping(value = "/byDates",
            method = RequestMethod.GET,
            produces = "application/json")
//    @RolesAllowed(AuthoritiesConstants.ADMIN) TODO
    public List<AuditEvent> findByDates(@RequestParam(value = "fromDate") Date fromDate,
                                    @RequestParam(value = "toDate") Date toDate) {
        return auditEventService.findByDates(fromDate, toDate);
    }
}
