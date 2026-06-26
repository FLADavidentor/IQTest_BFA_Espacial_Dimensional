CURRENT_PHASE: 7 — Polish & Hardening (DONE; Phase 6 deferred by user)
LAST_COMPLETED_GATE: Phase 7 — GlobalExceptionHandler + error.html + include-stacktrace=never (no traces to browser). Security audit: every endpoint role-gated. RN-BFA-06 <3s asserted. 11/11 suite.
NOTE Phase 5: browser walkthrough not run (headless env); verified server-side via SubtestFlujoIT + static asset serving. Vite emits fixed filenames; mvnw clean needed to drop stale chunks from target.
NOTE Phase 6: SKIPPED per user — real IQTest session (§19 Q2) + Dashboard token (§19 Q4) are undefined external contracts. STUBs remain (see STUBS_ACTIVE) as external-dependency blockers.
REMAINING_BEFORE_DEPLOY (non-gate, tracked):
  - Admin write-CRUD forms (create/edit/delete reactivo/version/baremo) — only list views built; routes role-gated.
  - CSRF: intentionally disabled for /api/** (session-cookie SPA, same-origin). Accepted; revisit if cross-origin clients are added.
  - Integracion token uses constant string .equals (STUB Phase 6); real mechanism §19 Q4.
NOTES:
  - Flyway: V1 schema + V2 config seed only. V3 baremo -> deferred to V5 (real Excel). V4 admin -> N/A (no users table in §5; admins via SecurityConfig Phase 4).
  - Testcontainers needs api.version=1.44 (Docker 29 min API) -> set in surefire systemPropertyVariables. Tests run via ./mvnw test (surefire includes *IT).
  - Scoring trigger decoupled: SubtestService publishes IntentoListoParaCalificarEvent on S1B close. CalificacionService listens in Phase 3.
  - Tests share one PG container (AbstractPostgresIT singleton).
OPEN_BLOCKERS:
  - BAREMO_DATA: needs Normas_Nac_del_BFA_10.xlsx (user-provided). Blocks V3 seed + real-percentile gate assertions in Phase 1 & 3. Logic still built/tested with in-test rows.
  - INTEGRATION_CONTRACT: §19 Q2 (session) + Q4 (dashboard token) external. Stubbed to Phase 6.
  - TIMER_LATENCY_CONFLICT: §12/Phase-7 say 5s poll; RN-BFA-04 wants <=1s auto-close latency. Interval is configurable (app.timer.fixed-delay-ms, default 5000). Client must confirm target latency (lower to ~1000 to meet RN-BFA-04).
STUBS_ACTIVE:
  - SesionIQTestClient (dev CIF) — Phase 6
  - Dashboard Bearer token (application-dev.yml) — Phase 6

## Open-question defaults applied (§19)
1. Baremo gap -> next lower non-null percentile (PercentilService.GAP_STRATEGY)
2. Session CIF -> HttpSession attribute "cif" (app.iqtest.session-attr)
3. Image storage -> enunciado_imagen_url stored as-is
4. Dashboard token -> static Bearer in application-dev.yml (app.integracion.token)
5. Active version -> admin manual `activa` flag; current year + activa=true

## Environment notes
- mvn not installed system-wide; bootstrapped Apache Maven 3.9.11 at /home/devuser/.tools. Project ships Maven Wrapper (./mvnw).
- No local Postgres; tests use Testcontainers, local run uses docker-compose (PG16).
