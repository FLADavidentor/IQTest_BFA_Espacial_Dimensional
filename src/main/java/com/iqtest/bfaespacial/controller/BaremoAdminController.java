package com.iqtest.bfaespacial.controller;

import com.iqtest.bfaespacial.model.Baremo;
import com.iqtest.bfaespacial.model.FactorEspacial;
import com.iqtest.bfaespacial.service.BaremoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

/** UC8-10 admin catalog (baremo) — inline percentile edit + CSV upload. ROLE_ADMIN. */
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

    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file,
                         Principal principal,
                         RedirectAttributes ra) {
        if (file.isEmpty()) {
            ra.addFlashAttribute("error", "El archivo de baremos está vacío");
            return "redirect:/admin/baremos";
        }
        try {
            service.importarCSV(file.getInputStream(), principal.getName());
            ra.addFlashAttribute("success", "Baremos importados correctamente");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error al importar baremos: " + e.getMessage());
        }
        return "redirect:/admin/baremos";
    }
}
