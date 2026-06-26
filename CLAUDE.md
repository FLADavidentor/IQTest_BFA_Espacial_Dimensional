# IQTest — Módulo BFA Espacial
**Build from scratch. Spring Boot + Thymeleaf + React. PostgreSQL.**

---

## 1. PROJECT CONTEXT

**System:** IQTest — web psychometric evaluation platform  
**Module scope:** BFA Espacial — automates 3 spatial aptitude subtests of the Batería Factorial de Aptitudes (BFA) for university admission at UAM (Universidad Americana, Nicaragua)  
**Client:** MSc. Álvaro Muñoz, Coordinación de Psicología, Facultad de Ciencias Médicas  
**This module is NOT standalone.** It is a component of IQTest. It consumes auth/session from IQTest's auth system and exposes results to IQTest's Dashboard.

---

## 2. BUSINESS RULES (NON-NEGOTIABLE)

| Rule ID | Rule |
|---------|------|
| RN-BFA-01 | One attempt per CIF per `periodo_academico`. UNIQUE(cif, periodo_academico) enforced at DB level. |
| RN-BFA-02 | 3 rotating form versions per year. Version assigned at attempt start. |
| RN-BFA-03 | Subtests are sequential: S1 → S2 → S1b. No backward navigation. |
| RN-BFA-04 | Auto-close subtest on time expiry. Latency tolerance ≤ 1 second. |
| RN-BFA-05 | Student cannot modify answers of a closed subtest. |
| RN-BFA-06 | Results (scores + percentiles) generated < 3s after final subtest closes. |
| RN-BFA-07 | System must handle ≥ 300 concurrent evaluation sessions without degradation. |
| RN-BFA-08 | Correct answers are NEVER exposed to the student. Evaluator sees them (authorized access only). |
| RN-BFA-09 | If connection drops mid-subtest, timer continues server-side. Answers sync on reconnect without restarting timer. |
| RN-BFA-10 | Scoring: pd_s1 = pd_s1a + pd_s1b (GENERATED); pd_st = pd_s1 + pd_s2 (GENERATED). Percentiles from baremo table lookup. |

---

## 3. SUBTEST SPECIFICATIONS

| Subtest | Code | Items | Time | Input Type | Scores |
|---------|------|-------|------|-----------|--------|
| Figuras Idénticas | S1a | 27 | 3 min (180s) | Binary A/B | → pd_s1a |
| Desplazamiento Espacial | S2 | 34 | 5 min (300s) | Binary A/B | → pd_s2 |
| Ladrillos-Cubos | S1b | 49 | 3 min 30s (210s) | Multiple A–E | → pd_s1b |

**Composite scores:**
- `pd_s1` = pd_s1a + pd_s1b (GENERATED column, not stored manually)
- `pd_st` = pd_s1 + pd_s2 (GENERATED column)

**Percentile conversion:** lookup `baremo` table by `(factor, puntuacion_directa)` → `percentil`.  
Factors: `S1`, `S2`, `ST`. Source: Normas Nacionales BFA Nicaragua 1992.

**Gap rule (OPEN — confirm with psychology team):** When a student's direct score falls on a gap entry ("-") in the baremo for S1 at percentiles 65, 45, 35 or S2 at percentiles 15, 10, 5 — what percentile to report is pending confirmation. For now: use next lower non-null percentile.

---

## 4. TECH STACK

```
Backend:  Java 21, Spring Boot 3.x
          Spring Web MVC (REST + Thymeleaf)
          Spring Security (session-based, integrates with IQTest auth)
          Spring Data JPA (Hibernate)
          Spring Scheduling (@Scheduled for server-side timer enforcement)

Frontend: Thymeleaf (SSR for admin, results, and static pages)
          React 18 (SPA embedded for timed subtest execution UI)
          React served via Vite build → static resources bundled into Spring Boot jar

Database: PostgreSQL 16+
          Flyway for migrations

Build:    Maven (single multi-module or standard project)
```

**React is used ONLY for the timed subtest execution flow** (real-time countdown, auto-submit, answer registration). All other pages (login, admin, results, dashboard) are Thymeleaf.

---

## 5. DATABASE SCHEMA

### Enums (PostgreSQL custom types)
```sql
CREATE TYPE tipo_subtest AS ENUM ('S1A', 'S2', 'S1B');
CREATE TYPE estado_intento AS ENUM ('ACTIVO', 'COMPLETADO', 'EXPIRADO', 'INTERRUMPIDO');
CREATE TYPE estado_subtest AS ENUM ('PENDIENTE', 'EN_CURSO', 'COMPLETADO', 'CERRADO_POR_TIEMPO');
CREATE TYPE factor_espacial AS ENUM ('S1', 'S2', 'ST');
```

### Tables

```sql
-- Rotating form versions (3 per year)
version_formulario (
  id               BIGINT PK,
  anio             SMALLINT NOT NULL,
  numero_version   SMALLINT NOT NULL,
  activa           BOOLEAN  NOT NULL DEFAULT false,
  UNIQUE(anio, numero_version)
)

-- Per-subtest configuration (seed data, rarely changes)
configuracion_subtest (
  tipo_subtest      tipo_subtest PK,
  tiempo_limite_seg INTEGER      NOT NULL,  -- S1A=180, S2=300, S1B=210
  cantidad_items    SMALLINT     NOT NULL,  -- S1A=27, S2=34, S1B=49
  tipo_seleccion    VARCHAR(20)  NOT NULL   -- 'BINARIO' | 'MULTIPLE'
)

-- Items (images stored as URLs/paths)
reactivo (
  id                    BIGINT PK,
  version_formulario_id BIGINT       NOT NULL FK→version_formulario,
  tipo_subtest          tipo_subtest NOT NULL FK→configuracion_subtest,
  orden                 SMALLINT     NOT NULL,
  enunciado_imagen_url  TEXT         NOT NULL
)

-- Answer options (normalized)
-- Correct answer: es_correcta=true (unique partial index per reactivo_id)
opcion_reactivo (
  id           BIGINT      PK,
  reactivo_id  BIGINT      NOT NULL FK→reactivo,
  etiqueta     VARCHAR(4)  NOT NULL,  -- 'A', 'B', 'C', 'D', 'E'
  es_correcta  BOOLEAN     NOT NULL DEFAULT false,
  UNIQUE(reactivo_id, etiqueta)
  -- Partial unique index: UNIQUE(reactivo_id) WHERE es_correcta
)

-- One attempt per student per period
intento (
  id                    BIGINT         PK,
  cif                   VARCHAR(20)    NOT NULL,
  periodo_academico     VARCHAR(20)    NOT NULL,
  estado                estado_intento NOT NULL DEFAULT 'ACTIVO',
  fecha_inicio          TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
  fecha_fin             TIMESTAMPTZ,
  version_formulario_id BIGINT         NOT NULL FK→version_formulario,
  UNIQUE(cif, periodo_academico)
)

-- Timed subtest execution within an attempt
ejecucion_subtest (
  id                 BIGINT         PK,
  intento_id         BIGINT         NOT NULL FK→intento,
  tipo_subtest       tipo_subtest   NOT NULL FK→configuracion_subtest,
  fecha_inicio       TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
  fecha_cierre       TIMESTAMPTZ,
  estado             estado_subtest NOT NULL DEFAULT 'PENDIENTE',
  cerrada_por_tiempo BOOLEAN        NOT NULL DEFAULT false,
  UNIQUE(intento_id, tipo_subtest)
)

-- Student's answer per item
respuesta (
  id                   BIGINT      PK,
  ejecucion_subtest_id BIGINT      NOT NULL FK→ejecucion_subtest,
  reactivo_id          BIGINT      NOT NULL FK→reactivo,
  opcion_reactivo_id   BIGINT      FK→opcion_reactivo,  -- NULL if unanswered
  fecha_registro       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  sincronizada         BOOLEAN     NOT NULL DEFAULT true,
  UNIQUE(ejecucion_subtest_id, reactivo_id)
)

-- 1:1 with intento. Computed after all subtests done.
resultado (
  intento_id    BIGINT   PK FK→intento,
  pd_s1a        SMALLINT NOT NULL,
  pd_s1b        SMALLINT NOT NULL,
  pd_s1         SMALLINT GENERATED ALWAYS AS (pd_s1a + pd_s1b) STORED,
  pd_s2         SMALLINT NOT NULL,
  pd_st         SMALLINT GENERATED ALWAYS AS (pd_s1a + pd_s1b + pd_s2) STORED,
  perc_s1       SMALLINT NOT NULL,
  perc_s2       SMALLINT NOT NULL,
  perc_st       SMALLINT NOT NULL,
  fecha_calculo TIMESTAMPTZ NOT NULL DEFAULT NOW()
)

-- Baremo table (Normas Nacionales BFA Nicaragua 1992)
baremo (
  factor              factor_espacial NOT NULL,
  puntuacion_directa  SMALLINT        NOT NULL,
  percentil           SMALLINT        NOT NULL,
  PRIMARY KEY (factor, puntuacion_directa)
)

-- Audit log
registro_auditoria (
  id          BIGINT      PK,
  intento_id  BIGINT      NOT NULL FK→intento,
  cif_actor   VARCHAR(20) NOT NULL,
  accion      VARCHAR(100) NOT NULL,
  fecha_hora  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  detalle     TEXT
)
```

---

## 6. LOGICAL ARCHITECTURE (SPRING BOOT PACKAGE STRUCTURE)

```
com.iqtest.bfaespacial
├── domain/                        # Core domain (entities, enums, value objects)
│   ├── Intento.java
│   ├── EjecucionSubtest.java
│   ├── Reactivo.java
│   ├── OpcionReactivo.java
│   ├── Respuesta.java
│   ├── Resultado.java
│   ├── Baremo.java
│   ├── RegistroAuditoria.java
│   ├── VersionFormulario.java
│   ├── ConfiguracionSubtest.java
│   └── enums/  (TipoSubtest, EstadoIntento, EstadoSubtest, FactorEspacial)
│
├── evaluacion/                    # Subsystem: test application
│   ├── gestion/                   # Use case: Iniciar/Reanudar intento
│   │   ├── IntentoService.java
│   │   └── IntentoRepository.java
│   ├── aplicacion/                # Use case: Realizar subtest cronometrado
│   │   ├── SubtestService.java
│   │   ├── EjecucionSubtestRepository.java
│   │   ├── RespuestaRepository.java
│   │   └── TimerService.java      # Server-side timer enforcement
│   └── sincronizacion/            # Use case: Sincronizar respuestas pendientes
│       └── SincronizacionService.java
│
├── resultados/                    # Subsystem: scoring and review
│   ├── calificacion/              # Use case: Calificar intento
│   │   └── CalificacionService.java
│   ├── percentiles/               # Use case: Convertir a percentiles
│   │   ├── BaremoRepository.java
│   │   └── PercentilService.java
│   └── consulta/                  # Use case: Consultar / Revisar resultados
│       ├── ResultadoRepository.java
│       └── ResultadoService.java
│
├── administracion/                # Subsystem: CRUD catalog + audit
│   ├── catalogo/
│   │   ├── ReactivoService.java
│   │   ├── VersionFormularioService.java
│   │   └── BaremoService.java
│   └── auditoria/
│       └── AuditoriaService.java
│
├── integracion/                   # Subsystem: IQTest boundary
│   ├── ResultadosDashboardController.java  # REST endpoint consumed by Dashboard
│   └── SesionIQTestClient.java             # Consumes IQTest auth session
│
└── web/                           # Controllers + Thymeleaf + React mount points
    ├── estudiante/
    │   ├── IntentoController.java     # GET /evaluacion/inicio → starts/resumes attempt
    │   └── SubtestController.java     # GET /evaluacion/subtest → Thymeleaf shell that mounts React
    ├── evaluador/
    │   └── ResultadosController.java  # GET /resultados/{cif}/{periodo}
    ├── admin/
    │   ├── ReactivoAdminController.java
    │   ├── VersionAdminController.java
    │   └── BaremoAdminController.java
    └── api/                           # REST endpoints consumed by React SPA
        ├── SubtestApiController.java  # GET /api/subtest/current, POST /api/respuesta
        └── TimerApiController.java    # GET /api/subtest/tiempo-restante
```

---

## 7. USE CASES & KEY FLOWS

### UC1: Iniciar o Reanudar Intento
**Actor:** Aspirante/Estudiante  
**Trigger:** Navigates to `/evaluacion/inicio` with valid IQTest session (CIF in session)  
**Flow:**
1. Validate CIF exists in IQTest session. If no valid session → redirect to IQTest login.
2. Check `intento` for UNIQUE(cif, periodo_academico). 
   - If EXISTS and estado=ACTIVO → resume (redirect to current subtest position)
   - If EXISTS and estado=COMPLETADO/EXPIRADO → show "ya completado" page
   - If NOT EXISTS → create new `intento`, assign active `version_formulario` for current year
3. Log to `registro_auditoria`

### UC2: Realizar Subtest Cronometrado
**Actor:** Aspirante  
**Tech:** React SPA inside Thymeleaf shell at `/evaluacion/subtest`  
**Flow:**
1. React loads current subtest data via `GET /api/subtest/current` → returns items + remaining time
2. Server-side timer: `ejecucion_subtest.fecha_inicio` stored at DB. Timer computed as `tiempo_limite_seg - (NOW() - fecha_inicio)`.
3. React polls `GET /api/subtest/tiempo-restante` every 30s (or uses SSE). On expiry → auto-POST close.
4. Student selects answer → `POST /api/respuesta` → upsert `respuesta` record.
5. On time expiry OR student submits last item → `POST /api/subtest/cerrar` → sets `ejecucion_subtest.estado=CERRADO_POR_TIEMPO|COMPLETADO`, `cerrada_por_tiempo=true|false`.
6. Advance to next subtest (S1A→S2→S1B). After S1B close → trigger UC4 (scoring).

### UC3: Sincronizar Respuestas Pendientes
**Trigger:** Connection restored after interruption  
**Flow:** React sends locally buffered answers via `POST /api/respuesta/sync`. Timer not reset. Server validates `ejecucion_subtest` still EN_CURSO before accepting.

### UC4: Calificar Intento y Convertir a Percentiles (INTERNAL)
**Trigger:** Auto-triggered when last subtest closes  
**Flow:**
1. Count correct answers per subtest → compute pd_s1a, pd_s1b, pd_s2
2. Insert `resultado` record (pd_s1 and pd_st are GENERATED)
3. Lookup `baremo` for factors S1, S2, ST → store perc_s1, perc_s2, perc_st
4. Update `intento.estado=COMPLETADO`, set `fecha_fin`
5. Send confirmation to student UI
6. Log to `registro_auditoria`

### UC5: Consultar Resultados
**Actor:** Evaluador/Psicólogo  
**Endpoint:** `GET /resultados/{cif}/{periodo}`  
**Returns:** pd_s1a, pd_s1b, pd_s1, pd_s2, pd_st, perc_s1, perc_s2, perc_st  
**Auth:** Role EVALUADOR required

### UC6: Revisar Respuestas del Aspirante
**Actor:** Evaluador (authorized)  
**Endpoint:** `GET /resultados/{cif}/{periodo}/respuestas`  
**Returns:** Per-item view: item image, student answer, correct answer, is_correct  
**Auth:** Role EVALUADOR required. Correct answer visible; student never sees this.

### UC7: Exponer Resultados al Dashboard IQTest
**Actor:** Dashboard General IQTest (external system)  
**Endpoint:** `GET /api/integracion/resultados/{cif}/{periodo}`  
**Returns:** JSON `{cif, periodo, pd_s1, pd_s2, pd_st, perc_s1, perc_s2, perc_st}`  
**Auth:** API key or service token from IQTest platform

### UC8–10: CRUD Catalog (Admin)
**Actor:** Administrador del Módulo  
**Entities:** reactivo, version_formulario, baremo  
**Pattern:** Standard CRUD via Thymeleaf forms + Spring MVC. All admin pages behind Role ADMIN.

---

## 8. ACTORS & ROLES

| Actor | Spring Security Role | Access |
|-------|---------------------|--------|
| Aspirante/Estudiante | ROLE_ESTUDIANTE | Evaluation flow only. No results, no correct answers. |
| Evaluador/Psicólogo | ROLE_EVALUADOR | Read results + answer review. No admin. |
| Administrador del Módulo | ROLE_ADMIN | Full CRUD on catalog. Audit log read. |
| Dashboard IQTest | Service token | API endpoint for results consumption only. |
| Sistema Autenticación IQTest | External | Provides CIF session. Module does not own auth. |

**Auth model:** Session-based Spring Security. CIF obtained from IQTest session (implementation: shared session cookie OR verify token from IQTest auth endpoint — confirm integration contract with IQTest team before building).

---

## 9. REST API (consumed by React SPA)

All endpoints under `/api/` require authenticated session (ROLE_ESTUDIANTE).

```
GET  /api/subtest/current
     → { subtestType, items: [{id, orden, imagenUrl, opciones: [{id, etiqueta}]}], tiempoRestanteSeg, estado }

GET  /api/subtest/tiempo-restante
     → { tiempoRestanteSeg, subtestType, estado }

POST /api/respuesta
     Body: { ejecucionSubtestId, reactivoId, opcionReactivoId }
     → 200 OK | 409 if subtest already closed | 423 if time expired

POST /api/respuesta/sync
     Body: { respuestas: [{reactivoId, opcionReactivoId, fechaRegistro}] }
     → { sincronizadas: N, rechazadas: M }

POST /api/subtest/cerrar
     → { nextSubtest | 'COMPLETADO' }
```

**Integration endpoint (service-to-service):**
```
GET  /api/integracion/resultados/{cif}/{periodo}
     Auth: Bearer <service-token>
     → { cif, periodo, pd_s1, pd_s2, pd_st, perc_s1, perc_s2, perc_st, fechaCalculo }
```

---

## 10. THYMELEAF PAGES (SSR)

| URL | Controller | Role | Description |
|-----|-----------|------|-------------|
| `/evaluacion/inicio` | IntentoController | ESTUDIANTE | Start/resume screen |
| `/evaluacion/subtest` | SubtestController | ESTUDIANTE | Thymeleaf shell mounting React SPA |
| `/evaluacion/completado` | SubtestController | ESTUDIANTE | Confirmation after last subtest |
| `/resultados/{cif}/{periodo}` | ResultadosController | EVALUADOR | Score + percentile view |
| `/resultados/{cif}/{periodo}/respuestas` | ResultadosController | EVALUADOR | Item-by-item review |
| `/admin/reactivos` | ReactivoAdminController | ADMIN | CRUD items |
| `/admin/versiones` | VersionAdminController | ADMIN | CRUD form versions |
| `/admin/baremos` | BaremoAdminController | ADMIN | CRUD baremo table |

---

## 11. REACT SPA (subtest execution)

**Location:** `src/main/resources/static/react/` (Vite build output)  
**Entry:** React mounts on `<div id="bfa-subtest-app">` injected by Thymeleaf in `/evaluacion/subtest`  
**Thymeleaf passes to React (via data attributes or window globals):**
- `intentoId`, `cif`, `subtestType`, `tiempoRestanteSeg`

**React responsibilities:**
- Display items with images (lazy loaded)
- Countdown timer (synced to server via polling, NOT relied on for enforcement — server enforces)
- Answer selection + immediate POST to `/api/respuesta`
- Local buffer for offline resilience (sessionStorage)
- Auto-submit + advance when timer hits 0 OR all items answered
- Show "tiempo agotado" UI when server closes subtest

**React does NOT:**
- Handle auth
- Show correct answers
- Manage scoring

---

## 12. SERVER-SIDE TIMER ENFORCEMENT

**Critical:** Client timer is UX only. Server enforces closure.

**Implementation:**
1. `ejecucion_subtest.fecha_inicio` set on DB when subtest starts.
2. `TimerService` uses `@Scheduled(fixedDelay = 5000)` to check active executions where `NOW() > fecha_inicio + tiempo_limite_seg`.
3. On expiry: set `estado=CERRADO_POR_TIEMPO`, `cerrada_por_tiempo=true`, `fecha_cierre=NOW()`.
4. If last subtest: trigger `CalificacionService`.
5. React polling detects closed state via `/api/subtest/tiempo-restante` → shows closure UI.

---

## 13. SCORING LOGIC

```java
// In CalificacionService
int pd_s1a = countCorrectAnswers(intentoId, TipoSubtest.S1A);
int pd_s1b = countCorrectAnswers(intentoId, TipoSubtest.S1B);
int pd_s2  = countCorrectAnswers(intentoId, TipoSubtest.S2);
// pd_s1 and pd_st are GENERATED columns in DB

// Percentile lookup
int perc_s1 = baremoRepository.findPercentil(FactorEspacial.S1, pd_s1a + pd_s1b);
int perc_s2 = baremoRepository.findPercentil(FactorEspacial.S2, pd_s2);
int perc_st = baremoRepository.findPercentil(FactorEspacial.ST, pd_s1a + pd_s1b + pd_s2);

// countCorrectAnswers = COUNT(*) FROM respuesta r
//   JOIN opcion_reactivo o ON r.opcion_reactivo_id = o.id
//   WHERE r.ejecucion_subtest_id = ... AND o.es_correcta = true
```

**Baremo gap handling:** if `findPercentil(factor, score)` returns null (gap in baremo), use next lower available score. Log this event to `registro_auditoria`.

---

## 14. NON-FUNCTIONAL REQUIREMENTS

| NFR | Requirement |
|-----|------------|
| Performance | ≥ 300 concurrent sessions without degradation |
| Timer accuracy | Auto-close latency ≤ 1 second from actual expiry |
| Results latency | Score computed < 3s after last subtest close |
| Availability | High availability during admission periods |
| Security | Results and answers are sensitive PII — role-gated access only |
| Browsers | Chrome, Firefox, Edge (current versions). Desktop primary. Mobile desirable. |
| Backups | Periodic DB backups mandatory |
| Audit | All relevant intento events logged to `registro_auditoria` |

---

## 15. SEED DATA (Flyway migrations)

**V1:** Schema DDL (enums + all tables)  
**V2:** `configuracion_subtest` seed:
```sql
INSERT INTO configuracion_subtest VALUES
  ('S1A', 180, 27, 'BINARIO'),
  ('S2',  300, 34, 'BINARIO'),
  ('S1B', 210, 49, 'MULTIPLE');
```
**V3:** `baremo` seed from Normas Nacionales BFA Nicaragua 1992 (load from Excel `Normas_Nac_del_BFA_10.xlsx`).  
**V4:** Admin user seed.

---

## 16. PROJECT STRUCTURE (FILES)

```
bfa-espacial/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/iqtest/bfaespacial/
│   │   │   └── [package structure from section 6]
│   │   └── resources/
│   │       ├── application.yml          # Spring config
│   │       ├── templates/               # Thymeleaf HTML
│   │       ├── static/
│   │       │   ├── react/               # Vite build output (React SPA)
│   │       │   └── css/js/img/
│   │       └── db/migration/            # Flyway SQL files V1–V4
│   └── test/
│       └── java/...
├── frontend/                            # React/Vite source
│   ├── package.json
│   ├── vite.config.js
│   └── src/
│       ├── main.jsx
│       ├── SubtestApp.jsx               # Root component
│       ├── components/
│       │   ├── CountdownTimer.jsx
│       │   ├── ReactivoCard.jsx
│       │   └── ProgressBar.jsx
│       └── api/
│           └── subtestApi.js
└── README.md
```

---

## 17. INTEGRATION CONTRACT (IQTest Platform)

**Assumption:** IQTest provides a session cookie with `cif` claim.  
**Module reads CIF from:** `HttpSession.getAttribute("cif")` OR JWT claim (TBD — confirm with IQTest team).  
**Module exposes to Dashboard:**  
`GET /api/integracion/resultados/{cif}/{periodo}` — secured with Bearer token.  
**Module does NOT implement:** user registration, payment, CIF generation, overall dashboard UI.

---

## 18. WHAT THIS MODULE IS NOT RESPONSIBLE FOR

- Auth / login / CIF generation (→ IQTest platform)
- Other BFA modules (Vocabulario, Razonamiento, Numérico, etc.) (→ other teams)
- EIC, EIG, AE composite scores (→ Dashboard orchestrator)
- Interview scheduling
- Final admission decision
- Vocabulario/Weill/Thurstone/MMPI tests (→ other modules)

---

## 19. OPEN QUESTIONS (resolve before or during build)

1. **Baremo gaps:** What percentile to assign when student score falls on "-" gap row? (S1 gaps at P65, P45, P35; S2 gaps at P15, P10, P5) — confirm with MSc. Álvaro Muñoz.
2. **Session integration:** Does IQTest pass CIF as session attribute, JWT, or via API call? Define before implementing `SesionIQTestClient`.
3. **Image storage:** Where are item images hosted? Local filesystem, S3, or embedded in DB? `enunciado_imagen_url` must resolve at runtime.
4. **Dashboard token:** What auth mechanism does IQTest Dashboard use to call `/api/integracion/resultados`?
5. **Active version_formulario:** How is the "current" version selected? By year only, or is there an admin toggle? Current schema has `activa BOOLEAN` — assume admin sets it manually.

---

## 20. GLOSSARY

| Term | Definition |
|------|-----------|
| CIF | Código de Identificación Fiscal — student's login credential, generated at payment |
| S1a | Puntuación directa Figuras Idénticas (27 items) |
| S1b | Puntuación directa Ladrillos-Cubos (49 items) |
| S1 | S1a + S1b (factor Espacial 1 — rapidez perceptiva) |
| S2 | Puntuación directa Desplazamiento (34 items, factor Espacial 2) |
| ST | S1 + S2 (Total Espacial) |
| Percentil | Baremo-derived position score (Normas Nacionales BFA Nicaragua 1992) |
| Intento | One complete evaluation attempt (all 3 subtests) — unique per CIF per period |
| Ejecución de Subtest | Timed execution of a single subtest within an intento |
| Baremo | Lookup table: direct score → percentile, per factor (S1/S2/ST) |
| Periodo académico | Academic admission period (e.g., "2026-I") |
| IQTest | The parent platform. This module is one component of it. |
