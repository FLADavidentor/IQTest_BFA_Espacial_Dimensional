package com.iqtest.bfaespacial.web.api;

import com.iqtest.bfaespacial.administracion.catalogo.OpcionReactivoRepository;
import com.iqtest.bfaespacial.administracion.catalogo.ReactivoRepository;
import com.iqtest.bfaespacial.common.SubtestCerradoException;
import com.iqtest.bfaespacial.domain.EjecucionSubtest;
import com.iqtest.bfaespacial.domain.Intento;
import com.iqtest.bfaespacial.domain.enums.TipoSubtest;
import com.iqtest.bfaespacial.evaluacion.aplicacion.EjecucionSubtestRepository;
import com.iqtest.bfaespacial.evaluacion.aplicacion.SubtestService;
import com.iqtest.bfaespacial.evaluacion.aplicacion.SubtestService.VistaActual;
import com.iqtest.bfaespacial.evaluacion.gestion.IntentoRepository;
import com.iqtest.bfaespacial.evaluacion.sincronizacion.SincronizacionService;
import com.iqtest.bfaespacial.integracion.SesionIQTestClient;
import com.iqtest.bfaespacial.web.api.dto.SubtestDtos.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** REST API consumed by the React SPA (§9). All endpoints require ROLE_ESTUDIANTE. */
@RestController
@RequestMapping("/api")
public class SubtestApiController {

    private final SesionIQTestClient sesion;
    private final IntentoRepository intentoRepo;
    private final SubtestService subtestService;
    private final SincronizacionService sincronizacionService;
    private final ReactivoRepository reactivoRepo;
    private final OpcionReactivoRepository opcionRepo;
    private final EjecucionSubtestRepository ejecucionRepo;

    public SubtestApiController(SesionIQTestClient sesion, IntentoRepository intentoRepo,
                                SubtestService subtestService, SincronizacionService sincronizacionService,
                                ReactivoRepository reactivoRepo, OpcionReactivoRepository opcionRepo,
                                EjecucionSubtestRepository ejecucionRepo) {
        this.sesion = sesion;
        this.intentoRepo = intentoRepo;
        this.subtestService = subtestService;
        this.sincronizacionService = sincronizacionService;
        this.reactivoRepo = reactivoRepo;
        this.opcionRepo = opcionRepo;
        this.ejecucionRepo = ejecucionRepo;
    }

    @GetMapping("/subtest/current")
    public SubtestActual current() {
        return buildActual(intentoActual());
    }

    /** Student clicked "Comenzar" (after the consigna): start the timer now (P1-A). */
    @PostMapping("/subtest/iniciar")
    public SubtestActual iniciar() {
        Intento intento = intentoActual();
        subtestService.comenzarSubtest(intento.getId());
        return buildActual(intento);
    }

    private SubtestActual buildActual(Intento intento) {
        VistaActual va = subtestService.vistaActual(intento.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sin subtest en curso"));
        EjecucionSubtest e = va.ejecucion();

        List<Item> items = reactivoRepo
                .findByVersionFormularioIdAndTipoSubtestAndActivoTrueOrderByOrden(
                        intento.getVersionFormulario().getId(), e.getTipoSubtest())
                .stream()
                .map(r -> new Item(r.getId(), r.getOrden(), r.getEnunciadoImagenUrl(), r.getEnunciadoTexto(),
                        opcionRepo.findByReactivoIdOrderByEtiqueta(r.getId()).stream()
                                // RN-BFA-08: expose only id + etiqueta, never es_correcta
                                .map(o -> new Opcion(o.getId(), o.getEtiqueta()))
                                .toList()))
                .toList();

        return new SubtestActual(e.getId(), e.getTipoSubtest().name(), items,
                va.tiempoRestanteSeg(), e.getEstado().name());
    }

    @GetMapping("/subtest/tiempo-restante")
    public Tiempo tiempoRestante() {
        Intento intento = intentoActual();
        VistaActual va = subtestService.vistaActual(intento.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sin subtest"));
        return new Tiempo(va.tiempoRestanteSeg(), va.ejecucion().getTipoSubtest().name(),
                va.ejecucion().getEstado().name());
    }

    @PostMapping("/respuesta")
    public ResponseEntity<Void> registrar(@RequestBody RespuestaRequest req) {
        // Authz: the target execution must belong to this student's intento (prevent IDOR).
        Intento intento = intentoActual();
        EjecucionSubtest ejec = ejecucionRepo.findById(req.ejecucionSubtestId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ejecución no encontrada"));
        if (!ejec.getIntento().getId().equals(intento.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ejecución ajena");
        }
        try {
            subtestService.registrarRespuesta(req.ejecucionSubtestId(), req.reactivoId(), req.opcionReactivoId());
            return ResponseEntity.ok().build();
        } catch (SubtestCerradoException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build(); // 409 closed
        }
    }

    @PostMapping("/respuesta/sync")
    public SyncResult sync(@RequestBody SyncRequest req) {
        Intento intento = intentoActual();
        VistaActual va = subtestService.vistaActual(intento.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sin subtest en curso"));
        var pendientes = req.respuestas().stream()
                .map(s -> new SincronizacionService.RespuestaPendiente(s.reactivoId(), s.opcionReactivoId()))
                .toList();
        var r = sincronizacionService.sincronizar(va.ejecucion().getId(), pendientes);
        return new SyncResult(r.sincronizadas(), r.rechazadas());
    }

    @PostMapping("/subtest/cerrar")
    public CerrarResult cerrar() {
        Intento intento = intentoActual();
        VistaActual va = subtestService.vistaActual(intento.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sin subtest en curso"));
        TipoSubtest actual = va.ejecucion().getTipoSubtest();
        // cerrar advances to the next subtest (PENDIENTE) internally.
        subtestService.cerrar(va.ejecucion().getId(), false);

        return subtestService.siguiente(actual)
                .map(next -> new CerrarResult(next.name()))
                .orElse(new CerrarResult("COMPLETADO"));
    }

    private Intento intentoActual() {
        return intentoRepo.findByCifAndPeriodoAcademico(sesion.cifActual(), sesion.periodoActual())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Intento no encontrado"));
    }
}
