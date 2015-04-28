package gr.com.ist.commun.web.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping(value="/error")
public class ErrorController {

    private static final Logger LOG = LoggerFactory.getLogger(ErrorController.class);

    @RequestMapping(value= {"401", "403"})
    @ResponseBody
    public Map<String, Object> handleSecurity(HttpServletRequest request) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("status", request.getAttribute("javax.servlet.error.status_code"));
        String reason = (String) request.getAttribute("javax.servlet.error.message");
        map.put("reason", reason);

        return map;
    }

    @RequestMapping(value= {"500"})
    @ResponseBody
    public Map<String, Object> handleServerError(HttpServletRequest request) {
        Map<String, Object> map = new HashMap<String, Object>();
        UUID refId = UUID.randomUUID();
        map.put("status", request.getAttribute("javax.servlet.error.status_code"));
        Exception exc = (Exception) request.getAttribute("javax.servlet.error.exception");
        map.put("reason", "Internal server error. Please contact the system administrator. Log reference id is: " + refId);

        if (exc != null) {
            LOG.error("Http response 500 due to an internal server error. Log reference id is: " + refId, exc);
        }
        return map;
    }
}