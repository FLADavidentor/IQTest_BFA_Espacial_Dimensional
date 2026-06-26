CURRENT_PHASE: P2-COMPLETE
LAST_COMPLETED_GATE: Dashboard JSON contract
OPEN_BLOCKERS: Real IQTest session (Phase 6), real images (client), real item content (client)
STUBS_ACTIVE: SesionIQTestClient (Phase 6), placeholder images, seeded test items
BAREMO: REAL DATA LOADED (V6 from Normas Nac del BFA.10.xlsx) — 119 rows (S1=27, S2=35, ST=57). No longer a blocker.

## P0/P1/P2 remediation (all gates passed)
- P0-A image fallback: ReactivoCard onError placeholder + enunciado_texto (V3). Vitest.
- P0-B admin CRUD: reactivos create/edit/soft-delete (V4 activo), versiones single-active toggle, baremos inline edit. Live gates + VersionAdminIT.
- P1-A consigna + deferred start: PENDIENTE->EN_CURSO on Comenzar (V5 fecha_inicio nullable). ConsignaTimerIT + live (NULL before, set after).
- P1-B audit trail: 7 event types wired. AuditoriaFlujoIT.
- P1-C progress: "Item X de N" last-position. Vitest.
- P2-A timer: 1s scheduler + SSE /api/subtest/timer-events. TimerLatencyIT (<2s) + live SSE cerrado push.
- P2-B JSON format: app.integracion.json-format CAMEL|SNAKE (default SNAKE). Snake+Camel ITs.
- Tests: 18 java + 3 vitest, all green.

## Notes (current)
- Flyway: V1 schema, V2 config seed, V3 reactivo.enunciado_texto, V4 reactivo.activo, V5 ejecucion fecha_inicio nullable. Real baremo data -> later migration when Excel arrives.
- Timer: @Scheduled default 1000ms (app.timer.fixed-delay-ms); SSE pushes closure (<1s latency, RN-BFA-04 met).
- CSRF intentionally disabled for /api/** (session-cookie SPA, same-origin); CSRF on for Thymeleaf forms.
- Testcontainers needs api.version=1.44 (Docker 29) via surefire systemPropertyVariables. Tests: ./mvnw test (surefire includes *IT); one shared PG container (AbstractPostgresIT). Frontend: vitest gated in mvn build.
- No users table in §5 — admin roles via SecurityConfig in-memory (dev) // STUB Phase 6.
- Do NOT start Phase 6 (real auth/images/baremo import) until client provides: (1) IQTest session contract, (2) authorized images from Coordinación de Psicología, (3) confirmed official Normas_Nac_del_BFA_10.xlsx.

## Open-question defaults applied (§19)
1. Baremo gap -> next lower non-null percentile (PercentilService.GAP_STRATEGY)
2. Session CIF -> HttpSession attribute "cif" (app.iqtest.session-attr)
3. Image storage -> enunciado_imagen_url stored as-is
4. Dashboard token -> static Bearer in application-dev.yml (app.integracion.token)
5. Active version -> admin manual `activa` flag; current year + activa=true

## Environment notes
- mvn not installed system-wide; bootstrapped Apache Maven 3.9.11 at /home/devuser/.tools. Project ships Maven Wrapper (./mvnw).
- No local Postgres; tests use Testcontainers, local run uses docker-compose (PG16).
