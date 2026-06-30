package com.iqtest.bfaespacial.controller;

import com.iqtest.bfaespacial.service.UsuarioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/usuarios")
public class UsuarioAdminController {

    private final UsuarioService service;

    public UsuarioAdminController(UsuarioService service) {
        this.service = service;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("usuarios", service.listar());
        return "admin/usuarios";
    }

    @PostMapping
    public String crear(@RequestParam String username,
                        @RequestParam String password,
                        @RequestParam String rol) {
        service.crear(username, password, rol);
        return "redirect:/admin/usuarios";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id) {
        service.cambiarActivo(id);
        return "redirect:/admin/usuarios";
    }
}
