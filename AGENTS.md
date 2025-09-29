# Repository Guidelines

## Project Structure & Module Organization
- Spring Boot backend lives in `src/main/java/com/txnow` with `api`, `application`, `domain`, `infrastructure` layers; shared config is `src/main/resources/application.yml`.
- Tests mirror the backend packages under `src/test/java`, and static assets stay in `src/main/resources`.
- The React + TypeScript client resides in `frontend/src` (`components`, `hooks`, `pages`, `services`, `types`, `utils`), with generated bundles in `frontend/dist` (ignored).
- Architectural notes and API contracts are in `docs/`; review before changing endpoints.

## Build, Test, and Development Commands
- `./gradlew bootRun` spins up the API on port 8080; use `./gradlew build` for a clean compile plus tests, or `./gradlew test` when iterating quickly.
- `cd frontend && npm install` to sync packages, `npm run dev` for the Vite dev server (port 5173), and `npm run build` for a typed production bundle in `frontend/dist`.

## Coding Style & Naming Conventions
- Target Java 21, four-space indentation, lowercase packages, constructor injection, and Lombok annotations already adopted in the backend.
- REST routes stay kebab-cased (`/api/exchange-rates`); DTO records belong in `api/.../dto`, enums stay descriptive (`Currency`).
- React modules use ES modules, two-space indentation, PascalCase components, camelCase hooks/utilities, and Tailwind utility classes for styling.
- Centralize configuration in `application.yml` or dedicated frontend config modules instead of scattering literals.

## Testing Guidelines
- Backend unit tests use JUnit 5 with Mockito; place new specs in `src/test/java/...` with a `*Test` suffix and mock external systems (BOK API, Redis).
- Cover edge cases around empty rate data, invalid currency codes, and decimal precision before merging.
- Frontend testing is not wired yet; if you add Vitest/Testing Library, commit the supporting npm scripts and colocation (`*.test.tsx`) with components.

## Commit & Pull Request Guidelines
- Follow the existing conventional prefixes (`feat:`, `fix:`, `chore:`), keep subjects under 72 characters, and expand in the body when behavior changes.
- Prefer separating backend and frontend edits into distinct commits and mention touched modules in the subject.
- Pull requests should include a short summary, test evidence (`./gradlew test`, screenshots), linked issues, and updates to `docs/` when contracts shift.

## Security & Configuration Tips
- Keep secrets out of version control; override `bok.api.key` and similar values via environment variables or an ignored `application-local.yml`.
- Update the allowed origins in `application.yml` whenever the frontend dev port changes, and note the change in the PR.
- Validate contract changes through the bundled Swagger UI (`/swagger-ui.html`) before requesting review.
