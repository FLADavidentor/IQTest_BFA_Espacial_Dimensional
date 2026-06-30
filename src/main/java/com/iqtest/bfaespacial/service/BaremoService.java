package com.iqtest.bfaespacial.service;

import com.iqtest.bfaespacial.model.Baremo;
import com.iqtest.bfaespacial.model.BaremoId;
import com.iqtest.bfaespacial.model.FactorEspacial;
import com.iqtest.bfaespacial.repository.BaremoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BaremoService {

    private final BaremoRepository repo;
    private final AuditoriaService auditoria;

    public BaremoService(BaremoRepository repo, AuditoriaService auditoria) {
        this.repo = repo;
        this.auditoria = auditoria;
    }

    public List<Baremo> listar() {
        return repo.findAll();
    }

    /** Edit a single percentile cell for (factor, puntuacionDirecta). */
    @Transactional
    public void actualizarPercentil(FactorEspacial factor, Short puntuacionDirecta, Short percentil) {
        Baremo b = repo.findById(new BaremoId(factor, puntuacionDirecta))
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe baremo %s/%d".formatted(factor, puntuacionDirecta)));
        b.setPercentil(percentil);
        repo.save(b);
    }

    /** Replaces the entire baremo table with contents from a CSV stream (RF-ESP-25 / UC-05). */
    @Transactional(rollbackFor = Exception.class)
    public void importarCSV(java.io.InputStream in, String cifActor) throws java.io.IOException {
        java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
        String line;
        List<Baremo> nuevos = new java.util.ArrayList<>();
        boolean first = true;
        while ((line = br.readLine()) != null) {
            if (line.trim().isEmpty()) continue;
            String[] parts = line.split("[,;]");
            if (parts.length < 3) continue;
            if (first && (parts[0].equalsIgnoreCase("factor") || parts[0].equalsIgnoreCase("factor_espacial"))) {
                first = false;
                continue;
            }
            first = false;
            try {
                FactorEspacial factor = FactorEspacial.valueOf(parts[0].trim().toUpperCase());
                Short pd = Short.parseShort(parts[1].trim());
                Short percentil = Short.parseShort(parts[2].trim());
                Baremo b = new Baremo();
                b.setFactor(factor);
                b.setPuntuacionDirecta(pd);
                b.setPercentil(percentil);
                nuevos.add(b);
            } catch (Exception e) {
                throw new IllegalArgumentException("Error al parsear línea de CSV: " + line, e);
            }
        }
        if (!nuevos.isEmpty()) {
            repo.deleteAll();
            repo.saveAll(nuevos);
            // 0L represent generic system/admin action
            auditoria.registrar(0L, cifActor, "BAREMO_IMPORTADO", "Filas cargadas=" + nuevos.size());
        }
    }
}


