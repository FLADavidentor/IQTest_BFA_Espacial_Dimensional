package com.iqtest.bfaespacial.web;
import com.iqtest.bfaespacial.model.TipoSubtest;

import com.iqtest.bfaespacial.AbstractPostgresIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** §19 Q3 upload gates: valid image saved, non-image rejected, no path traversal. */
@AutoConfigureMockMvc
class ImagenUploadIT extends AbstractPostgresIT {

    static Path TMP;
    static {
        try { TMP = Files.createTempDirectory("bfa-img-test"); }
        catch (IOException e) { throw new RuntimeException(e); }
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("app.imagenes.upload-dir", () -> TMP.toString());
    }

    @Autowired MockMvc mockMvc;

    private static final byte[] PNG = {
            (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0, 1, 2, 3 };

    @Test @WithMockUser(roles = "ADMIN")
    void pngValido_seGuarda_yQuedaDentro() throws Exception {
        var file = new MockMultipartFile("file", "item_1.png", "image/png", PNG);
        mockMvc.perform(multipart("/admin/imagenes/upload").file(file).param("tipoSubtest", "s1a").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("/img/s1a/item_1.png"));
        assertThat(Files.exists(TMP.resolve("s1a/item_1.png"))).isTrue();
    }

    @Test @WithMockUser(roles = "ADMIN")
    void noImagen_400_sinEscribir() throws Exception {
        var exe = new MockMultipartFile("file", "malo.exe", "application/octet-stream",
                new byte[]{ 'M', 'Z', 0, 0, 0 });
        mockMvc.perform(multipart("/admin/imagenes/upload").file(exe).with(csrf()))
                .andExpect(status().isBadRequest());
        assertThat(Files.exists(TMP.resolve("malo.exe"))).isFalse();
    }

    @Test @WithMockUser(roles = "ADMIN")
    void traversalEnNombre_seSanea_quedaDentro() throws Exception {
        var file = new MockMultipartFile("file", "../../evil.png", "image/png", PNG);
        mockMvc.perform(multipart("/admin/imagenes/upload").file(file).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("/img/evil.png"));
        // landed inside upload-dir, not in any parent
        assertThat(Files.exists(TMP.resolve("evil.png"))).isTrue();
        assertThat(Files.exists(TMP.getParent().resolve("evil.png"))).isFalse();
    }
}
