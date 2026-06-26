package com.iqtest.bfaespacial.web.admin;

import com.iqtest.bfaespacial.administracion.catalogo.ReactivoService;
import com.iqtest.bfaespacial.administracion.catalogo.VersionFormularioService;
import com.iqtest.bfaespacial.domain.Reactivo;
import com.iqtest.bfaespacial.domain.enums.TipoSubtest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/** UC8-10 admin catalog (items) — full CRUD with soft-delete. ROLE_ADMIN. */
@Controller
@RequestMapping("/admin/reactivos")
public class ReactivoAdminController {

    private final ReactivoService reactivoService;
    private final VersionFormularioService versionService;

    public ReactivoAdminController(ReactivoService reactivoService, VersionFormularioService versionService) {
        this.reactivoService = reactivoService;
        this.versionService = versionService;
    }

    @GetMapping
    public String listar(Model model) {
        return vista(model, new ReactivoForm());
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        Reactivo r = reactivoService.obtener(id);
        ReactivoForm f = new ReactivoForm();
        f.setId(r.getId());
        f.setVersionFormularioId(r.getVersionFormulario().getId());
        f.setTipoSubtest(r.getTipoSubtest());
        f.setOrden(r.getOrden());
        f.setEnunciadoImagenUrl(r.getEnunciadoImagenUrl());
        f.setEnunciadoTexto(r.getEnunciadoTexto());
        return vista(model, f);
    }

    @PostMapping
    public String guardar(@ModelAttribute ReactivoForm form) {
        reactivoService.guardar(form);
        return "redirect:/admin/reactivos";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id) {
        reactivoService.desactivar(id);
        return "redirect:/admin/reactivos";
    }

    private String vista(Model model, ReactivoForm form) {
        model.addAttribute("reactivos", reactivoService.listar());
        model.addAttribute("versiones", versionService.listar());
        model.addAttribute("tipos", TipoSubtest.values());
        model.addAttribute("form", form);
        return "admin/reactivos";
    }
}
