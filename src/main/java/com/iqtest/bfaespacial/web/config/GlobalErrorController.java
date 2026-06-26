package com.iqtest.bfaespacial.web.config;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

/**
 * Unified error dispatch (7-A): framework-level errors (unmapped routes, etc.).
 * /api/** -> JSON {"error","status"}; everything else -> Thymeleaf error page. No stack traces.
 */
@Controller
public class GlobalErrorController implements ErrorController {

    @RequestMapping("/error")
    public Object handle(HttpServletRequest req) {
        Object code = req.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int status = (code instanceof Integer i) ? i : 500;
        Object uri = req.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        Object msg = req.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        String reason = (msg == null || msg.toString().isBlank())
                ? HttpStatus.valueOf(status).getReasonPhrase() : msg.toString();

        if (uri != null && uri.toString().startsWith("/api/")) {
            return ResponseEntity.status(status).body(Map.of("error", reason, "status", status));
        }
        ModelAndView mv = new ModelAndView("error");
        mv.addObject("status", status);
        mv.addObject("error", reason);
        mv.setStatus(HttpStatus.valueOf(status));
        return mv;
    }
}
