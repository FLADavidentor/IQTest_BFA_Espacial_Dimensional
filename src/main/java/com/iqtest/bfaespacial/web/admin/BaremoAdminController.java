package com.iqtest.bfaespacial.web.admin;

import com.iqtest.bfaespacial.administracion.catalogo.BaremoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** UC8-10 admin catalog (baremo). ROLE_ADMIN. Write CRUD: remaining Phase-4 slice. */
@Controller
public class BaremoAdminController {

    private final BaremoService service;

    public BaremoAdminController(BaremoService service) {
        this.service = service;
    }

    @GetMapping("/admin/baremos")
    public String listar(Model model) {
        model.addAttribute("baremos", service.listar());
        return "admin/baremos";
    }
}
