package com.iqtest.bfaespacial.web.admin;

import com.iqtest.bfaespacial.domain.enums.TipoSubtest;

/** Form backing for reactivo create/edit. */
public class ReactivoForm {
    private Long id;
    private Long versionFormularioId;
    private TipoSubtest tipoSubtest;
    private Short orden;
    private String enunciadoImagenUrl;
    private String enunciadoTexto;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getVersionFormularioId() { return versionFormularioId; }
    public void setVersionFormularioId(Long v) { this.versionFormularioId = v; }
    public TipoSubtest getTipoSubtest() { return tipoSubtest; }
    public void setTipoSubtest(TipoSubtest t) { this.tipoSubtest = t; }
    public Short getOrden() { return orden; }
    public void setOrden(Short o) { this.orden = o; }
    public String getEnunciadoImagenUrl() { return enunciadoImagenUrl; }
    public void setEnunciadoImagenUrl(String u) { this.enunciadoImagenUrl = u; }
    public String getEnunciadoTexto() { return enunciadoTexto; }
    public void setEnunciadoTexto(String t) { this.enunciadoTexto = t; }
}
