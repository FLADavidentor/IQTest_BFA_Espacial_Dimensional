CURRENT_PHASE: 3 — Scoring Subsystem
LAST_COMPLETED_GATE: Phase 2 — IntentoServiceIT (conflict) + SubtestServiceIT (answer-after-close) + TimerServiceIT (expiry close) green (5/5 suite)
NOTES:
  - Flyway: V1 schema + V2 config seed only. V3 baremo -> deferred to V5 (real Excel). V4 admin -> N/A (no users table in §5; admins via SecurityConfig Phase 4).
  - Testcontainers needs api.version=1.44 (Docker 29 min API) -> set in surefire systemPropertyVariables. Tests run via ./mvnw test (surefire includes *IT).
  - Scoring trigger decoupled: SubtestService publishes IntentoListoParaCalificarEvent on S1B close. CalificacionService listens in Phase 3.
  - Tests share one PG container (AbstractPostgresIT singleton).
OPEN_BLOCKERS:
  - BAREMO_DATA: needs Normas_Nac_del_BFA_10.xlsx (user-provided). Blocks V3 seed + real-percentile gate assertions in Phase 1 & 3. Logic still built/tested with in-test rows.
  - INTEGRATION_CONTRACT: §19 Q2 (session) + Q4 (dashboard token) external. Stubbed to Phase 6.
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
