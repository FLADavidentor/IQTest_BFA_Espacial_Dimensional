CURRENT_PHASE: PHASE-7-COMPLETE
LAST_COMPLETED_GATE: Open questions resolved
OPEN_BLOCKERS: Real IQTest session (Phase 6), real images (client), real item content (client)
STUBS_ACTIVE: SesionIQTestClient (Phase 6), placeholder images, seeded test items
RESOLVED_QUESTIONS: Q1, Q3, Q4, Q5
  Q3 detail: local filesystem, Spring static resource handler (/img/** -> upload-dir),
             upload-dir = ${user.home}/bfa-imagenes (configurable), upload endpoint
             POST /admin/imagenes/upload (type+size validated, filename sanitized, no traversal),
             ImagenResolver maps stored URLs. MinIO/CDN swap = change base-url + resource handler only, zero app-code change.
PENDING_EXTERNAL: Q2 (IQTest session contract spec from IQTest team)
BAREMO: REAL DATA LOADED (V6 from Normas Nac del BFA.10.xlsx) — 119 rows (S1=27, S2=35, ST=57). No longer a blocker.

## Phase 7 (Track 2) — all gates passed
- 7-A error handling: GlobalErrorController -> /api {error,status} JSON, else error.html; no stack traces.
- 7-B security: actuator -> ADMIN only; RoleMatrixIT (wrong role -> 403, anon actuator -> redirect).
- 7-C answer-key leak: audited; zero student-reachable es_correcta (DTO is {id,etiqueta}).
- 7-D NFR: timer <2s (TimerLatencyIT); results 111ms < 3s (logged); 10 concurrent POST -> all 200, count=10.
- 7-E a11y: jest-axe zero critical/serious on consigna + subtest view.
- 7-F open questions: Q1/Q5 documented; Q3 base-url + upload/static serving (below); Q4 app.integracion.api-key Bearer. Q2 external.
- Q3 image upload+serving: WebMvcConfig static handler /img/** -> upload-dir (auto-created on startup); ImagenAdminController POST /admin/imagenes/upload (png/jpeg/webp, <=2MB, magic-byte check, sanitized name, traversal-guarded). ImagenUploadIT. Live gates passed.
- Tests: 26 java + 5 vitest, all green.

## P0/P1/P2 remediation (all gates passed)
- P0-A image fallback: ReactivoCard onError placeholder + enunciado_texto (V3). Vitest.
- P0-B admin CRUD: reactivos create/edit/soft-delete (V4 activo), versiones single-active toggle, baremos inline edit. Live gates + VersionAdminIT.
- P1-A consigna + deferred start: PENDIENTE->EN_CURSO on Comenzar (V5 fecha_inicio nullable). ConsignaTimerIT + live (NULL before, set after).
- P1-B audit trail: 7 event types wired. AuditoriaFlujoIT.
- P1-C progress: "Item X de N" last-position. Vitest.
- P2-A timer: 1s scheduler + SSE /api/subtest/timer-events. TimerLatencyIT (<2s) + live SSE cerrado push.
- P2-B JSON format: app.integracion.json-format CAMEL|SNAKE (default SNAKE). Snake+Camel ITs.
- Tests: 18 java + 3 vitest, all green.

## Admin UI completion (post-Phase-7)
- Answer-option admin UI: OpcionReactivoService + /admin/reactivos/{id}/opciones (add/delete/mark-correct, single-correct-per-reactivo enforced). Items are now fully answerable via UI (no SQL). OpcionAdminIT + live.
- Image upload page: GET /admin/imagenes (file picker + JS fetch w/ CSRF header) -> existing /upload endpoint. ImagenAdminController is @Controller + @ResponseBody.
- KNOWN GAP: offline sync (RN-BFA-09) IS wired client-side (postRespuesta fail -> sessionStorage buffer -> window 'online' -> /api/respuesta/sync), but: retry triggers only on the 'online' event (no timed/back-off retry), flushBuffer errors are swallowed, and the CLIENT buffer/flush path has NO automated test (server /sync side is tested).
- Current suite total: 28 java + 5 vitest, all green.

## Notes (current)
- Flyway: V1 schema, V2 config seed, V3 reactivo.enunciado_texto, V4 reactivo.activo, V5 ejecucion fecha_inicio nullable, V6 real baremo data (119 rows from Normas Nac del BFA.10.xlsx).
- Timer: @Scheduled default 1000ms (app.timer.fixed-delay-ms); SSE pushes closure (<1s latency, RN-BFA-04 met).
- CSRF intentionally disabled for /api/** (session-cookie SPA, same-origin); CSRF on for Thymeleaf forms.
- Testcontainers needs api.version=1.44 (Docker 29) via surefire systemPropertyVariables. Tests: ./mvnw test (surefire includes *IT); one shared PG container (AbstractPostgresIT). Frontend: vitest gated in mvn build.
- No users table in §5 — admin roles via SecurityConfig in-memory (dev) // STUB Phase 6.
- Baremo: REAL DATA ALREADY LOADED (V6). Pending from client = only confirmation that Normas Nac del BFA.10.xlsx is the official source; no code work.
- Do NOT start Phase 6 (real auth + authorized real images) until client provides: (1) IQTest session contract, (2) authorized images from Coordinación de Psicología.

## Open-question defaults applied (§19)
1. Baremo gap -> next lower non-null percentile (PercentilService.GAP_STRATEGY)
2. Session CIF -> HttpSession attribute "cif" (app.iqtest.session-attr)
3. Image storage -> enunciado_imagen_url stored as-is
4. Dashboard token -> static Bearer in application-dev.yml (app.integracion.token)
5. Active version -> admin manual `activa` flag; current year + activa=true

## Environment notes
- mvn not installed system-wide; bootstrapped Apache Maven 3.9.11 at /home/devuser/.tools. Project ships Maven Wrapper (./mvnw).
- No local Postgres; tests use Testcontainers, local run uses docker-compose (PG16).
