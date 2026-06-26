CURRENT_PHASE: 0 — Scaffold & Pipeline
LAST_COMPLETED_GATE: (none yet — Phase 0 in progress)
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
