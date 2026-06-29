package com.iqtest.bfaespacial.controller;

import com.iqtest.bfaespacial.service.VersionFormularioService;
import com.iqtest.bfaespacial.service.VersionFormularioService.ActivacionRequiereConfirmacion;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** UC8-10 admin catalog (versions). ROLE_ADMIN. Single active per year enforced at service level. */
@Controller
@RequestMapping("/admin/versiones")
public class VersionAdminController {

    private final VersionFormularioService service;

    public VersionAdminController(VersionFormularioService service) {
        this.service = service;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("versiones", service.listar());
        return "admin/versiones";
    }

    @PostMapping
    public String crear(@RequestParam Short anio, @RequestParam Short numeroVersion,
                        @RequestParam(defaultValue = "false") boolean activa) {
        service.crear(anio, numeroVersion, activa);
        return "redirect:/admin/versiones";
    }

    @PostMapping("/{id}/activar")
    public String activar(@PathVariable Long id,
                          @RequestParam(defaultValue = "false") boolean confirmar,
                          RedirectAttributes ra) {
        try {
            service.activar(id, confirmar);
        } catch (ActivacionRequiereConfirmacion e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/versiones";
    }
}


