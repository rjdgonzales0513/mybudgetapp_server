# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Spring Boot 3.5 / Java 21 REST backend for a household budget app (the React/Vite frontend lives in a separate repo and runs on `localhost:5173`). Persists to PostgreSQL via Spring Data JPA, with the schema owned by Flyway. Serves on port **8081**.

## Commands

Use the Maven wrapper (`./mvnw` in Git Bash, `.\mvnw.cmd` in PowerShell).

```bash
./mvnw spring-boot:run                 # run locally (needs Postgres on localhost:5432, db "mybudgetapp")
./mvnw clean package                   # build jar into target/ (also what the Dockerfile runs)
./mvnw test                            # run tests
./mvnw test -Dtest=ClassName#method    # run a single test
docker build -t mybudgetappserver .    # image name expected by docker-compose.yml
docker compose up                      # postgres + backend + frontend (frontend image built from the other repo)
```

Tests need no database: services are tested with Mockito, and controllers with `@WebMvcTest` + `@Import(SecurityConfig.class)`. Use `@MockitoBean`, not the deprecated `@MockBean`. GitHub Actions (`.github/workflows/ci.yml`) runs `./mvnw verify` on PRs and pushes to `main`. No linter/formatter is configured.

Local DB credentials are in `src/main/resources/application.properties`; containers override them with `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` env vars.

Further docs: `docs/API.md`, `docs/ARCHITECTURE.md`, `docs/DEPLOYMENT.md`. Keep them in sync when changing endpoints or business rules.

## Architecture

Standard layered layout under `com.rjdg.mybudget`: `controller` → `service` → `repository` (Spring Data interfaces using derived query methods) → `model` (JPA entities with Lombok). Request bodies are Java `record`s in `dto`.

- **Schema is Flyway-managed** (`spring.jpa.hibernate.ddl-auto=none`). Any entity change needs a new migration in `src/main/resources/db/migration/` (`V2__...sql`, etc.); never edit `V1__initial_table.sql`. V1 also seeds the default categories (`needs`, `invest`, `emergency`, `leisure`).
- **IDs**: `Category` and `Account` use client-supplied string IDs (slugs like `needs`); `Transaction` uses a DB-generated `BIGSERIAL`.
- **Transaction business rules live in `TransactionService.addTransaction`**: `type` is a plain String, `EXPENSE` or `INCOME` (case-insensitive on input, stored upper-case). Adding a transaction mutates the linked `Account.balance` — expenses subtract (and require a category plus sufficient balance, else 400), income adds and has no category. The method is `@Transactional`, so a failed request leaves the balance untouched.
- **Reports** (`BudgetService.getMonthlyReport`) are computed in memory from transactions in a `YearMonth` (`?month=YYYY-MM`). `totalSpent` counts only expenses. `PdfStatementGenerator` renders the same `MonthlyReport` to PDF with raw PDFBox text positioning (no templating).
- **Errors** are raised as `ResponseStatusException` from services; there is no global exception handler. `server.error.include-message=always` puts the reason in the response body.
- **Validation**: request DTOs carry `jakarta.validation` constraints, enforced via `@Valid` on controller params (`spring-boot-starter-validation`).
- **Security** (`security/SecurityConfig`): `/api/**` and `/error` are `permitAll` (`/error` must stay open or every error surfaces as 403), CSRF disabled, CORS allows only `http://localhost:5173`. The jjwt dependencies are present but JWT auth is not implemented yet.

## Deployment

- `.gitlab-ci.yml`: on `main`, builds the Docker image and pushes `$CI_REGISTRY_IMAGE/mybudgetapp_server:latest` to the GitLab registry, then SSHes into an EC2 host and runs `kubectl set image deployment/backend -n mybudgetapp ...`.
- `devsecops/` holds the k8s manifests (namespace `mybudgetapp`, backend/frontend deployments, in-cluster Postgres + PVC, ingress routing `/api` → backend and `/` → frontend). `backend.yaml` points at an AWS RDS instance; `mybudgetapp.yaml` is an alternate backend deployment using `postgresmybudgetapp-secret`.
