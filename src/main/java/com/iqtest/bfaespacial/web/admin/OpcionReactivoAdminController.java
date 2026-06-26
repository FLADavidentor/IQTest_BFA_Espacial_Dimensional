package com.iqtest.bfaespacial.web.admin;

import com.iqtest.bfaespacial.administracion.catalogo.OpcionReactivoService;
import com.iqtest.bfaespacial.administracion.catalogo.ReactivoService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Admin UI for answer options of a reactivo. ROLE_ADMIN. */
@Controller
@RequestMapping("/admin/reactivos/{reactivoId}/opciones")
public class OpcionReactivoAdminController {

    private final OpcionReactivoService opcionService;
    private final ReactivoService reactivoService;

    public OpcionReactivoAdminController(OpcionReactivoService opcionService, ReactivoService reactivoService) {
        this.opcionService = opcionService;
        this.reactivoService = reactivoService;
    }

    @GetMapping
    public String listar(@PathVariable Long reactivoId, Model model) {
        model.addAttribute("reactivo", reactivoService.obtener(reactivoId));
        model.addAttribute("opciones", opcionService.listar(reactivoId));
        model.addAttribute("etiquetas", new String[]{"A", "B", "C", "D", "E"});
        return "admin/opciones";
    }

    @PostMapping
    public String agregar(@PathVariable Long reactivoId, @RequestParam String etiqueta,
                          @RequestParam(defaultValue = "false") boolean correcta, RedirectAttributes ra) {
        try {
            opcionService.agregar(reactivoId, etiqueta, correcta);
        } catch (DataIntegrityViolationException e) {
            ra.addFlashAttribute("error", "La etiqueta " + etiqueta + " ya existe para este reactivo");
        }
        return "redirect:/admin/reactivos/" + reactivoId + "/opciones";
    }

    @PostMapping("/{opcionId}/correcta")
    public String marcarCorrecta(@PathVariable Long reactivoId, @PathVariable Long opcionId) {
        opcionService.marcarCorrecta(opcionId);
        return "redirect:/admin/reactivos/" + reactivoId + "/opciones";
    }

    @PostMapping("/{opcionId}/eliminar")
    public String eliminar(@PathVariable Long reactivoId, @PathVariable Long opcionId) {
        opcionService.eliminar(opcionId);
        return "redirect:/admin/reactivos/" + reactivoId + "/opciones";
    }
}
