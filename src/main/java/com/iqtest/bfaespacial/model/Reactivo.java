package com.iqtest.bfaespacial.model;

import com.iqtest.bfaespacial.model.TipoSubtest;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "reactivo")
@Getter @Setter @NoArgsConstructor
public class Reactivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "version_formulario_id", nullable = false)
    private VersionFormulario versionFormulario;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "tipo_subtest", columnDefinition = "tipo_subtest", nullable = false)
    private TipoSubtest tipoSubtest;

    @Column(nullable = false)
    private Short orden;

    @Column(name = "enunciado_imagen_url", nullable = false, columnDefinition = "text")
    private String enunciadoImagenUrl;

    // Optional text fallback (P0-A) shown when the image is unavailable.
    @Column(name = "enunciado_texto", columnDefinition = "text")
    private String enunciadoTexto;

    // Soft-delete flag (P0-B). Inactive items are excluded from subtest delivery.
    @Column(nullable = false)
    private boolean activo = true;
}


