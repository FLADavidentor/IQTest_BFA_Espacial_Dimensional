package com.iqtest.bfaespacial.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Authenticated landing page. Every role redirects here after login.
 * The controller inspects the user's granted authority and sends them
 * to the correct home page — no routing logic lives in the security layer.
 */
@Controller
public class LandingController {

    @GetMapping("/")
    public String landing(Authentication auth) {
        for (GrantedAuthority ga : auth.getAuthorities()) {
            String role = ga.getAuthority();
            if (role.equals("ROLE_ADMIN")) {
                return "redirect:/admin/reactivos";
            }
            if (role.equals("ROLE_EVALUADOR")) {
                return "redirect:/resultados";
            }
        }
        // Default: ESTUDIANTE (or any other role)
        return "redirect:/evaluacion/inicio";
    }
}
