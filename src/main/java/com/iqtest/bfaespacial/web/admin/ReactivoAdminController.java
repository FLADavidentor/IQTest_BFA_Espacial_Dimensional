package com.iqtest.bfaespacial.web.admin;

import com.iqtest.bfaespacial.administracion.catalogo.ReactivoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** UC8-10 admin catalog (items). ROLE_ADMIN. Write CRUD: remaining Phase-4 slice. */
@Controller
public class ReactivoAdminController {

    private final ReactivoService service;

    public ReactivoAdminController(ReactivoService service) {
        this.service = service;
    }

    @GetMapping("/admin/reactivos")
    public String listar(Model model) {
        model.addAttribute("reactivos", service.listar());
        return "admin/reactivos";
    }
}
