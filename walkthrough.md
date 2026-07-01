# Recorrido Completo — BFA Espacial Dimensional

> Recorrido visual de **todas las pantallas**, **todos los roles**, **todos los estados**, y las **interacciones cruzadas** entre usuarios.

---

## Índice de Roles y Vistas

| Rol | Páginas Disponibles | Landing (`/`) |
|-----|---------------------|---------------|
| **Estudiante** | `/evaluacion/inicio`, `/evaluacion/subtest`, `/evaluacion/completado` | → `/evaluacion/inicio` |
| **Evaluador** | `/resultados` (Historial + Monitoreo), `/resultados/{cif}/{periodo}`, `/resultados/{cif}/{periodo}/respuestas` | → `/resultados` |
| **Administrador** | `/admin/reactivos`, `/admin/versiones`, `/admin/baremos`, `/admin/usuarios`, `/admin/imagenes`, `/admin/opciones`, `/resultados/**` | → `/admin/reactivos` |

---

## 1. Pantalla de Inicio de Sesión

Todos los usuarios ingresan por la misma pantalla. BCrypt fuerza 12 + CSRF cookie. El `LandingController` en `/` redirige según el rol.

![Pantalla de Login](screenshots/1_login_screen.png)

---

## 2. Flujo del Estudiante

### 2.1 Dashboard → Consignas

````carousel
![Dashboard del Estudiante — CIF, período, versión activa](screenshots/5_estudiante_dashboard.png)
<!-- slide -->
![Consignas del Subtest — instrucciones antes de comenzar](screenshots/7_estudiante_consignas.png)
````

### 2.2 Los Tres Subtests (S1A → S2 → S1B)

````carousel
![Subtest S1A — Figuras Idénticas](screenshots/8_subtest_s1a_screen.png)
<!-- slide -->
![Respuesta Marcada — burbuja OMR grafito](screenshots/9_subtest_marcado.png)
<!-- slide -->
![Subtest S2 — Desplazamiento Espacial](screenshots/10_subtest_s2_screen.png)
<!-- slide -->
![Subtest S1B — Ladrillos y Cubos 3D](screenshots/11_subtest_s1b_screen.png)
````

### 2.3 Estados de Conexión y Finalización

````carousel
![Modo Sin Conexión — banner rojo, respuestas en buffer local](screenshots/13_modo_sin_conexion.png)
<!-- slide -->
![Reconexión Exitosa — banner verde, respuestas sincronizadas](screenshots/14_sincronizacion_exitosa.png)
<!-- slide -->
![Evaluación Completada — prueba finalizada, re-ingreso bloqueado](screenshots/12_subtest_completado.png)
````

---

## 3. Flujo del Evaluador

### 3.1 Landing → Historial → Monitoreo

> **Interacción cruzada**: El historial se actualiza cuando un estudiante completa su prueba. El monitoreo muestra estudiantes activos en tiempo real.

````carousel
![Evaluador — Historial con resultados completados](screenshots/21_evaluador_landing.png)
<!-- slide -->
![Evaluador — Monitoreo en Vivo](screenshots/22_evaluador_monitoreo.png)
````

### 3.2 Reporte y Respuestas Detalladas

````carousel
![Evaluador — Reporte Psicométrico (puntuaciones, percentiles, baremo)](screenshots/23_evaluador_reporte.png)
<!-- slide -->
![Evaluador — Detalle de Respuestas ítem por ítem](screenshots/24_evaluador_respuestas.png)
````

### 3.3 Acciones del Evaluador

| Acción | Efecto | Impacto en Estudiante |
|--------|--------|----------------------|
| **Forzar Cierre** | Cierra ejecución, califica parcialmente | Redirigido a `/evaluacion/completado` |
| **Anular** | Elimina todo (cascada) | CIF liberado para nuevo intento |

---

## 4. Flujo del Administrador

### 4.1 Panel de Administración

````carousel
![Admin — Panel de Reactivos](screenshots/15_admin_reactivos_premium.png)
<!-- slide -->
![Admin — Versiones de Formulario](screenshots/16_admin_versiones_premium.png)
<!-- slide -->
![Admin — Baremos (tabla + CSV)](screenshots/3_admin_baremos.png)
<!-- slide -->
![Admin — Usuarios (CRUD + BCrypt)](screenshots/4_admin_usuarios.png)
````

### 4.2 Resultados y Reportes (Vista Admin)

````carousel
![Admin — Historial de Evaluaciones](screenshots/17_resultados_historial.png)
<!-- slide -->
![Admin — Monitoreo en Vivo](screenshots/18_monitoreo_en_vivo.png)
<!-- slide -->
![Admin — Reporte Psicométrico completo](screenshots/19_reporte_psicometrico.png)
<!-- slide -->
![Admin — Detalle de Respuestas ítem por ítem](screenshots/20_detalle_respuestas.png)
````

---

## 5. Interacciones Cruzadas Entre Roles

```mermaid
sequenceDiagram
    participant E as Estudiante
    participant S as Servidor BFA
    participant V as Evaluador
    participant A as Administrador

    E->>S: Login → / → redirect /evaluacion/inicio
    E->>S: Comienza subtest S1A
    V->>S: Ve en Monitoreo: "S1A, 0/27, 300s"
    E->>S: Responde ítems (progreso 14/27)
    V->>S: Ve en Monitoreo: "S1A, 14/27, 120s"
    
    alt Flujo Normal
        E->>S: Completa S1A → S2 → S1B
        S->>S: CalificacionService.calificar()
        V->>S: Ve en Historial: puntuaciones + percentiles
    end
    
    alt Forzar Cierre por Evaluador
        V->>S: POST /resultados/monitoreo/forzar/{id}
        S->>S: Cierra ejecución + califica parcialmente
        E->>S: Redirigido a /evaluacion/completado
    end

    alt Anular por Evaluador
        V->>S: POST /resultados/monitoreo/anular/{id}
        S->>S: DELETE cascada
        E->>S: Puede iniciar nuevo intento
    end

    A->>S: Gestiona reactivos, versiones, baremos, usuarios
    A->>S: También ve resultados y monitorea
```

---

## 6. Checklist de Cobertura

### 6.1 Estados del Estudiante

| # | Estado / Situación | ✓ | Evidencia |
|---|-------------------|---|-----------|
| 1 | Login exitoso → landing → `/evaluacion/inicio` | ✅ | `5_estudiante_dashboard` |
| 2 | Dashboard con CIF, período, versión | ✅ | `5_estudiante_dashboard` |
| 3 | Consignas antes de cada subtest | ✅ | `7_estudiante_consignas` |
| 4 | Visualización de ítem con imagen | ✅ | `8_subtest_s1a_screen` |
| 5 | Selección de respuesta (burbuja OMR grafito) | ✅ | `9_subtest_marcado` |
| 6 | Navegación entre ítems (← →) | ✅ | Botones visibles en captura 8 |
| 7 | Indicador de progreso (Ítem X de Y) | ✅ | Barra superior en captura 8 |
| 8 | Temporizador por subtest | ✅ | `03:00` visible en captura 8 |
| 9 | Transición S1A → S2 → S1B | ✅ | Capturas 8, 10, 11 |
| 10 | Pérdida de conexión (offline) | ✅ | `13_modo_sin_conexion` |
| 11 | Reconexión + sincronización | ✅ | `14_sincronizacion_exitosa` |
| 12 | Prueba completada + bloqueo re-ingreso | ✅ | `12_subtest_completado` |
| 13 | Acceso denegado a zona admin (HTTP 403) | ✅ | `6_rbac_denied_check` |

### 6.2 Estados del Evaluador

| # | Estado / Situación | ✓ | Evidencia |
|---|-------------------|---|-----------|
| 1 | Login → landing → `/resultados` | ✅ | `21_evaluador_landing` |
| 2 | Historial de intentos completados | ✅ | `21_evaluador_landing` |
| 3 | Monitoreo en vivo de estudiantes activos | ✅ | `22_evaluador_monitoreo` |
| 4 | Progreso por ítem de un estudiante | ✅ | Columna "Progreso" en monitoreo |
| 5 | Tiempo restante de un estudiante | ✅ | Columna "Tiempo Restante" |
| 6 | Forzar cierre de intento activo | ✅ | Botón "Forzar Cierre" |
| 7 | Anular/invalidar un intento | ✅ | Botón "Anular" + confirm() |
| 8 | Reporte psicométrico | ✅ | `23_evaluador_reporte` |
| 9 | Detalle de respuestas | ✅ | `24_evaluador_respuestas` |
| 10 | Acceso denegado a zona admin (403) | ✅ | `25_evaluador_rbac_denied` |
| 11 | Sin estudiantes activos (tabla vacía) | ✅ | `22_evaluador_monitoreo` |

### 6.3 Estados del Administrador

| # | Estado / Situación | ✓ | Evidencia |
|---|-------------------|---|-----------|
| 1 | Login → landing → `/admin/reactivos` | ✅ | `15_admin_reactivos_premium` |
| 2 | CRUD de reactivos | ✅ | `15_admin_reactivos_premium` |
| 3 | Gestión de versiones | ✅ | `16_admin_versiones_premium` |
| 4 | Activar/desactivar versión | ✅ | `16_admin_versiones_premium` |
| 5 | Carga masiva de baremos (CSV) | ✅ | `3_admin_baremos` |
| 6 | CRUD de usuarios | ✅ | `4_admin_usuarios` |
| 7 | Subida de imágenes | ✅ | Navegación visible en captura 15 |
| 8 | Acceso a historial | ✅ | `17_resultados_historial` |
| 9 | Acceso a monitoreo en vivo | ✅ | `18_monitoreo_en_vivo` |
| 10 | Reporte psicométrico | ✅ | `19_reporte_psicometrico` |
| 11 | Detalle de respuestas | ✅ | `20_detalle_respuestas` |

### 6.4 Estados del Sistema

| # | Estado / Situación | ✓ | Mecanismo |
|---|-------------------|---|-----------|
| 1 | CSRF habilitado | ✅ | Verificado en automate.js |
| 2 | BCrypt fuerza 12 | ✅ | V7 migration |
| 3 | Unicidad CIF por período | ✅ | IntentoService |
| 4 | Calificación automática | ✅ | CalificacionService (< 3s) |
| 5 | Alerta de consistencia | ✅ | Badge "Válido" |
| 6 | Baremo Nicaragua 1992 | ✅ | Tabla + lookup |
| 7 | Eliminación en cascada | ✅ | MonitoreoService |
| 8 | Auditoría de eventos | ✅ | AuditoriaService |
| 9 | Open-in-view deshabilitado | ✅ | JOIN FETCH |
| 10 | Perfiles dev/prod separados | ✅ | SecurityConfig dual |
| 11 | Página de error personalizada | ✅ | `25_evaluador_rbac_denied` |

---

## 7. Seguridad — Control de Acceso (RBAC)

Cada rol solo puede acceder a sus rutas. Ruta no autorizada → página de error styled con código HTTP.

````carousel
![Estudiante intenta /admin → 403](screenshots/6_rbac_denied_check.png)
<!-- slide -->
![Evaluador intenta /admin → 403](screenshots/25_evaluador_rbac_denied.png)
````

| Ruta | ESTUDIANTE | EVALUADOR | ADMIN |
|------|-----------|-----------|-------|
| `/evaluacion/**` | ✅ | ❌ 403 | ❌ 403 |
| `/resultados/**` | ❌ 403 | ✅ | ✅ |
| `/admin/**` | ❌ 403 | ❌ 403 | ✅ |

---

*Capturas generadas por Puppeteer (`automate.js`) contra Spring Boot + PostgreSQL en ejecución real.*
