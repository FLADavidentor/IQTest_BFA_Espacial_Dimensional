package com.iqtest.bfaespacial.web.admin;

import com.iqtest.bfaespacial.administracion.catalogo.VersionFormularioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** UC8-10 admin catalog (versions). ROLE_ADMIN. Write CRUD: remaining Phase-4 slice. */
@Controller
public class VersionAdminController {

    private final VersionFormularioService service;

    public VersionAdminController(VersionFormularioService service) {
        this.service = service;
    }

    @GetMapping("/admin/versiones")
    public String listar(Model model) {
        model.addAttribute("versiones", service.listar());
        return "admin/versiones";
    }
}
