package com.iqtest.bfaespacial.service;

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
        // Already a usable URL: absolute (CDN) or root-relative (e.g. the /img/... path the
        // upload endpoint returns, served by the static handler at the same origin).
        if (stored.startsWith("http://") || stored.startsWith("https://") || stored.startsWith("/")) {
            return stored;
        }
        // Bare key (e.g. "s1a/1.png") -> prefix the configured base (local or CDN host).
        return baseUrl + "/" + stored;
    }
}

