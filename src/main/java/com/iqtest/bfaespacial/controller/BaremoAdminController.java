package com.iqtest.bfaespacial.controller;
import com.iqtest.bfaespacial.model.Baremo;

import com.iqtest.bfaespacial.service.BaremoService;
import com.iqtest.bfaespacial.model.FactorEspacial;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/** UC8-10 admin catalog (baremo) — inline percentile edit. ROLE_ADMIN. */
@Controller
@RequestMapping("/admin/baremos")
public class BaremoAdminController {

    private final BaremoService service;

    public BaremoAdminController(BaremoService service) {
        this.service = service;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("baremos", service.listar());
        return "admin/baremos";
    }

    @PostMapping
    public String editar(@RequestParam FactorEspacial factor,
                         @RequestParam Short puntuacionDirecta,
                         @RequestParam Short percentil) {
        service.actualizarPercentil(factor, puntuacionDirecta, percentil);
        return "redirect:/admin/baremos";
    }
}


