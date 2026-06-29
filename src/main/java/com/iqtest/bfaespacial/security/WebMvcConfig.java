package com.iqtest.bfaespacial.security;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * §19 Q3: serve item images straight off disk from app.imagenes.upload-dir at /img/**.
 * No controller/streaming code. MinIO/CDN swap = change base-url + this location only.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/login").setViewName("login");
    }

    private final Path uploadDir;

    public WebMvcConfig(@Value("${app.imagenes.upload-dir}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    void crearDirectorio() {
        try {
            Files.createDirectories(uploadDir); // avoid 500 on first deploy
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo crear upload-dir: " + uploadDir, e);
        }
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/img/**")
                .addResourceLocations("file:" + uploadDir + "/");
    }

    public Path getUploadDir() {
        return uploadDir;
    }
}

