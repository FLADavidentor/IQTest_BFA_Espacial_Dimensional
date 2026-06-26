package com.iqtest.bfaespacial.web.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * §19 Q3: resolve item image URLs through a single configurable base
 * (app.imagenes.base-url). Switching local filesystem <-> CDN/S3 is a config change,
 * not a code change. Absolute (http/https) stored URLs pass through unchanged.
 */
@Component
public class ImagenResolver {

    private final String baseUrl;

    public ImagenResolver(@Value("${app.imagenes.base-url:/img}") String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public String resolve(String stored) {
        if (stored == null || stored.isBlank()) return stored;
        if (stored.startsWith("http://") || stored.startsWith("https://")) return stored;
        return baseUrl + (stored.startsWith("/") ? stored : "/" + stored);
    }
}
