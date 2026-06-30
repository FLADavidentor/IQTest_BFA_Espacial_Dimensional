# Walkthrough: Implementación de Todos los Casos de Uso y Verificación Visual

Se han implementado, compilado y verificado con éxito todos los casos de uso, restricciones y controles de seguridad definidos en la especificación del proceso RUP.

---

## Cambios Implementados

### 1. Migraciones y Esquema de Base de Datos
*   **[V7__usuario_schema.sql](file:///f:/Dev/Repos/IQTest_BFA_Espacial_Dimensional/src/main/resources/db/migration/V7__usuario_schema.sql):** Creación de la tabla `usuario` y precarga (seed) de cuentas por defecto (`admin`/`admin123`, `evaluador`/`evaluador123`, `estudiante`/`estudiante123`) con encriptación BCrypt (fuerza 12). Añadido de la columna `alerta_consistencia` en la tabla `resultado`.
*   **[V8__seed_test_reactivos.sql](file:///f:/Dev/Repos/IQTest_BFA_Espacial_Dimensional/src/main/resources/db/migration/V8__seed_test_reactivos.sql):** Precarga de los 110 reactivos geométricos (dibujados y texturizados en B y N de alta fidelidad) y sus 550 opciones con respuesta correcta predefinida usando la sintaxis de evasión de restricciones `OVERRIDING SYSTEM VALUE`.

### 2. Gestión de Usuarios y Roles (RF-ESP-26)
*   **[Usuario.java](file:///f:/Dev/Repos/IQTest_BFA_Espacial_Dimensional/src/main/java/com/iqtest/bfaespacial/model/Usuario.java):** Entidad JPA de base de datos para usuarios.
*   **[UsuarioRepository.java](file:///f:/Dev/Repos/IQTest_BFA_Espacial_Dimensional/src/main/java/com/iqtest/bfaespacial/repository/UsuarioRepository.java):** Repositorio para buscar credenciales.
*   **[UsuarioService.java](file:///f:/Dev/Repos/IQTest_BFA_Espacial_Dimensional/src/main/java/com/iqtest/bfaespacial/service/UsuarioService.java):** Servicio que implementa `UserDetailsService` de Spring Security respaldado por la base de datos y métodos CRUD.
*   **[UsuarioAdminController.java](file:///f:/Dev/Repos/IQTest_BFA_Espacial_Dimensional/src/main/java/com/iqtest/bfaespacial/controller/UsuarioAdminController.java):** Controlador de rutas `/admin/usuarios` para crear y activar/desactivar cuentas.
*   **[usuarios.html](file:///f:/Dev/Repos/IQTest_BFA_Espacial_Dimensional/src/main/resources/templates/admin/usuarios.html):** Interfaz premium estilo dark-mode con efectos translúcidos (glassmorphism) para gestión de usuarios.

### 3. Carga e Importación de Baremos vía CSV (RF-ESP-25 / UC-05)
*   **[BaremoService.java](file:///f:/Dev/Repos/IQTest_BFA_Espacial_Dimensional/src/main/java/com/iqtest/bfaespacial/service/BaremoService.java#L33):** Método `importarCSV` que reemplaza de forma atómica todas las conversiones de baremos y registra la auditoría.
*   **[BaremoAdminController.java](file:///f:/Dev/Repos/IQTest_BFA_Espacial_Dimensional/src/main/java/com/iqtest/bfaespacial/controller/BaremoAdminController.java#L36):** Controlador multipart `/admin/baremos/upload`.
*   **[baremos.html](file:///f:/Dev/Repos/IQTest_BFA_Espacial_Dimensional/src/main/resources/templates/admin/baremos.html):** Interfaz para adjuntar y cargar archivos CSV de baremos.

### 4. Historial de Evaluaciones y Resultados (RF-ESP-27 / UC-11)
*   **[ResultadosController.java](file:///f:/Dev/Repos/IQTest_BFA_Espacial_Dimensional/src/main/java/com/iqtest/bfaespacial/controller/ResultadosController.java#L38):** Rutas para listar el historial general en `/resultados`.
*   **[lista.html](file:///f:/Dev/Repos/IQTest_BFA_Espacial_Dimensional/src/main/resources/templates/resultados/lista.html):** Vista premium dark-mode para listar intentos históricos con CIF, periodo académico, percentiles calculados, estado de consistencia interna y enlaces directos.

### 5. Consistencia Interna y Patrones de Respuesta (RF-ESP-22 / BR-14)
*   **[Resultado.java](file:///f:/Dev/Repos/IQTest_BFA_Espacial_Dimensional/src/main/java/com/iqtest/bfaespacial/model/Resultado.java#L49):** Mapeo del campo `alertaConsistencia` en base de datos.
*   **[CalificacionService.java](file:///f:/Dev/Repos/IQTest_BFA_Espacial_Dimensional/src/main/java/com/iqtest/bfaespacial/service/CalificacionService.java#L92):** Heurísticas de consistencia interna:
    *   **Alta Repetición:** Alerta si >85% de las respuestas eligen la misma opción (ej: marcar todo "A").
    *   **Patrón Consecutivo:** Alerta si hay una racha consecutiva de >=12 respuestas idénticas.
*   **[ResultadosDashboardController.java](file:///f:/Dev/Repos/IQTest_BFA_Espacial_Dimensional/src/main/java/com/iqtest/bfaespacial/controller/ResultadosDashboardController.java#L56):** Expone las alertas de consistencia a los consumidores de integración externa.

### 6. Estilizado OMR Impreso y Textura de Papel (Retroalimentación Visual)
*   **[subtest.css](file:///f:/Dev/Repos/IQTest_BFA_Espacial_Dimensional/src/main/resources/static/css/subtest.css):** Hoja de estilos que imita un examen en papel fotocopiado, aplicando Courier, ocultando los inputs nativos y creando burbujas OMR que se rellenan con textura de grafito gris oscuro al seleccionarse.

---

## Resultados de Verificación

### 1. Pruebas Unitarias Automatizadas
*   Se creó **[ConsistencyCheckTest.java](file:///f:/Dev/Repos/IQTest_BFA_Espacial_Dimensional/src/test/java/com/iqtest/bfaespacial/service/ConsistencyCheckTest.java)** para validar las alertas:
    *   `testAltaRepeticionAlert()`
    *   `testPatronConsecutivoAlert()`
    *   `testNormalNoAlert()`
*   Resultados de compilación y ejecución de test de Maven:
    ```bash
    [INFO] Running com.iqtest.bfaespacial.service.ConsistencyCheckTest
    [INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.661 s
    ```

### 2. Verificación de Seguridad y Navegación E2E (Script Automatizado)
Hemos implementado el script **[automate.js](file:///f:/Dev/Repos/IQTest_BFA_Espacial_Dimensional/automate.js)** que inicia base de datos, Spring Boot y simula la experiencia completa mediante Puppeteer:
*   **[✓] CSRF Pass:** Comprobó que el token `XSRF-TOKEN` es generado y enviado por el servidor en las llamadas SPA.
*   **[✓] RBAC Pass:** Confirmó que el estudiante recibe un **HTTP 403 Forbidden** si intenta acceder a rutas administrativas como `/admin/usuarios`.
*   **[✓] OMR Bubble Pass:** Seleccionó una opción y tomó una captura detallada que documenta el relleno con efecto grafito.

---

## Evidencia Visual de los Pasos (Carrusel)

A continuación se muestra la secuencia capturada en tiempo de ejecución:

````carousel
![1. Pantalla de Login (Monospace / Estilo papel fotocopiado)](file:///f:/Dev/Repos/IQTest_BFA_Espacial_Dimensional/screenshots/1_login_screen.png)
<!-- slide -->
![2. Panel de Administración de Reactivos (Sesión Administrador)](file:///f:/Dev/Repos/IQTest_BFA_Espacial_Dimensional/screenshots/2_admin_reactivos.png)
<!-- slide -->
![3. Panel de Configuración de Baremos y Percentiles](file:///f:/Dev/Repos/IQTest_BFA_Espacial_Dimensional/screenshots/3_admin_baremos.png)
<!-- slide -->
![4. ABM y Control de Usuarios (Estilo Premium Glassmorphic)](file:///f:/Dev/Repos/IQTest_BFA_Espacial_Dimensional/screenshots/4_admin_usuarios.png)
<!-- slide -->
![5. Dashboard de Inicio del Estudiante (Inicio de Evaluación)](file:///f:/Dev/Repos/IQTest_BFA_Espacial_Dimensional/screenshots/5_estudiante_dashboard.png)
<!-- slide -->
![6. RBAC Check - 403 Forbidden al Estudiante intentando acceder a Admin](file:///f:/Dev/Repos/IQTest_BFA_Espacial_Dimensional/screenshots/6_rbac_denied_check.png)
<!-- slide -->
![7. Consignas e Instrucciones del Subtest S1A](file:///f:/Dev/Repos/IQTest_BFA_Espacial_Dimensional/screenshots/7_estudiante_consignas.png)
<!-- slide -->
![8. Cuadrícula de Preguntas del Subtest (Preguntas e Imágenes Geométricas en B y N)](file:///f:/Dev/Repos/IQTest_BFA_Espacial_Dimensional/screenshots/8_subtest_screen.png)
<!-- slide -->
![9. Detalle OMR - Burbuja rellenada con lápiz grafito](file:///f:/Dev/Repos/IQTest_BFA_Espacial_Dimensional/screenshots/9_subtest_marcado.png)
````
