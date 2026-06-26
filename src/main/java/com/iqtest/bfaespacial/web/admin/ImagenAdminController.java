package com.iqtest.bfaespacial.web.admin;

import com.iqtest.bfaespacial.web.config.WebMvcConfig;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;

/**
 * §19 Q3 image upload (ROLE_ADMIN). Saves to app.imagenes.upload-dir; returns the /img/... URL
 * the admin pastes into reactivo.enunciado_imagen_url. Validates type + size; no path traversal.
 */
@RestController
@RequestMapping("/admin/imagenes")
public class ImagenAdminController {

    private static final long MAX_BYTES = 2 * 1024 * 1024; // 2MB
    private static final Set<String> TIPOS = Set.of("image/png", "image/jpeg", "image/webp");
    private static final Set<String> SUBDIRS = Set.of("s1a", "s2", "s1b");

    private final Path uploadDir;

    public ImagenAdminController(WebMvcConfig webMvcConfig) {
        this.uploadDir = webMvcConfig.getUploadDir();
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> upload(@RequestParam("file") MultipartFile file,
                                                      @RequestParam(value = "tipoSubtest", required = false) String tipoSubtest) {
        if (file == null || file.isEmpty()) return bad("Archivo vacío");
        if (file.getSize() > MAX_BYTES) return bad("Máximo 2MB");

        String ct = file.getContentType();
        byte[] head = headBytes(file);
        if (ct == null || !TIPOS.contains(ct) || !esImagen(head)) {
            return bad("Solo se permiten imágenes PNG, JPEG o WEBP");
        }

        String safe = sanitizar(file.getOriginalFilename());
        if (safe.isBlank()) return bad("Nombre de archivo inválido");

        String sub = (tipoSubtest != null && SUBDIRS.contains(tipoSubtest.toLowerCase())) ? tipoSubtest.toLowerCase() : "";
        Path dir = uploadDir.resolve(sub).normalize();
        Path target = dir.resolve(safe).normalize();
        // Defence in depth: the resolved path must stay inside upload-dir.
        if (!target.startsWith(uploadDir)) return bad("Ruta no permitida");

        try {
            Files.createDirectories(dir);
            try (var in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "No se pudo guardar", "status", 500));
        }

        String url = "/img/" + (sub.isEmpty() ? "" : sub + "/") + safe;
        return ResponseEntity.ok(Map.of("url", url));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<Map<String, Object>> tooBig(MaxUploadSizeExceededException e) {
        return bad("Máximo 2MB");
    }

    private static ResponseEntity<Map<String, Object>> bad(String msg) {
        return ResponseEntity.badRequest().body(Map.of("error", msg, "status", 400));
    }

    /** Strip any path, keep a safe basename. "../../etc/passwd" -> "passwd". */
    private static String sanitizar(String original) {
        if (original == null || original.isBlank()) return "";
        String base = Paths.get(original).getFileName().toString();
        base = base.replaceAll("[^A-Za-z0-9._-]", "_");
        base = base.replaceFirst("^\\.+", ""); // no leading dots
        return base;
    }

    private static byte[] headBytes(MultipartFile file) {
        try (var in = file.getInputStream()) {
            return in.readNBytes(12);
        } catch (IOException e) {
            return new byte[0];
        }
    }

    private static boolean esImagen(byte[] b) {
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return true; // PNG
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return true; // JPEG
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') return true; // WEBP
        return false;
    }
}
