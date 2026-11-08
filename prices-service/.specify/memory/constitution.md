<!--
Sync Impact Report
- Version change: 1.1.0 -> 1.2.0
- Modified principles: I. Contract-First API Design; II. Domain-First Architecture
- Updated sections: Additional Constraints, Development Workflow
- Added sections: none
- Removed sections: none
- Follow-up TODOs: none
-->

# Prices Service Constitution

## Core Principles

### I. Contract-First API Design
The Prices Service MUST define the REST contract before implementation and treat the OpenAPI schema as the source of truth for request, response, and error shapes. OpenAPI Generator MUST generate the contract interfaces using `interfaceOnly=true`; controllers MUST implement those generated interfaces. All endpoints and generated DTOs MUST match the API specification. A single global `@RestControllerAdvice` MUST produce every RFC 9457 `ProblemDetail` response, with a consistent `title`, `status`, `detail`, and `instance`.

### II. Domain-First Architecture
The service MUST use hexagonal architecture with explicit ports and adapters organized into `domain`, `application`, and `infrastructure` packages. Core pricing logic MUST live in `domain`, remain independent from Spring, JPA, Jackson, and HTTP concerns, and follow SOLID principles. The `domain` and `application` layers MUST define business rules, use cases, and ports; `infrastructure` MUST provide adapters. ArchUnit tests MUST automatically verify that `domain` has no dependencies on Spring, JPA, or Jackson. The business rule that resolves the applicable price MUST be explicit, testable, and deterministic.

### III. Test-First Delivery
Every functional behavior MUST start from a failing automated test, and implementation MUST proceed through red-green-refactor discipline. Acceptance, web, and domain tests MUST cover the business rules for price selection, invalid input, and missing record scenarios before release.

### IV. Deterministic Pricing Rules
The service MUST evaluate price applicability by date range and priority rules in a single, reproducible decision path. A match is valid only when `startDate <= applicationDate <= endDate`; if multiple tariffs qualify, the one with the highest priority wins, and ties MUST be resolved deterministically by the higher `priceList` value. When no tariff matches, the service MUST return 404 instead of silently applying a fallback price.

### V. Safe Validation and Observability
Validation of required parameters and type formatting MUST happen before business logic; malformed requests MUST return 400 with field-level details when available. Internal exceptions MUST be abstracted behind problem responses, and operational logs MUST capture enough context for diagnosis without exposing internals to clients.

## Additional Constraints

The Prices Service MUST operate within the following guardrails:

- Java 21, Maven, and Spring Boot 4.1.x are the required build environment.
- The project MUST use a memory-backed H2 database initialized with the four baseline price records described in the product specification.
- Monetary values MUST use `BigDecimal`, ISO-4217 currency values, and local date-time semantics unless a documented exception is approved.
- The public API MUST expose `GET /api/v1/prices` with `applicationDate`, `productId`, and `brandId` and MUST return the applicable price payload for a successful match.
- API contract generation MUST be done from the OpenAPI source, with generated interfaces treated as the contract boundary and any controller-only divergence treated as a compliance issue.
- `mvn verify` MUST enforce Spotless formatting, a JaCoCo line coverage minimum of 80%, and ArchUnit architecture rules.
- Error handling MUST remain consistent across all layers, with validation errors distinguished from domain failures and application errors documented as ProblemDetail payloads.
- All implementation work MUST preserve the documented assumptions of the project: inclusive date ranges, timezone-free local timestamps, deterministic priority resolution, and explicit 404 behavior for unmatched data.

## Development Workflow

The delivery workflow MUST be aligned with the project specification and technical plan:

- Requirements and assumptions are tracked in the seed specification and plan documents and MUST be honored when the implementation is created or amended.
- Every feature change MUST include the relevant tests first and then an implementation that satisfies those tests without broadening scope.
- Commits MUST be small and focused and use the `test:`, `feat:`, or `refactor:` prefixes as applicable.
- Architecture reviews MUST confirm that the domain remains independent from frameworks, and cross-layer boundaries MUST follow the documented hexagonal pattern.
- Code quality gates MUST include formatting checks, test coverage thresholds, and verification that no API contract drift occurs between the generated interface and the controller behavior.
- Pull requests MUST state the business rule being changed, the relevant test coverage, and any trade-offs against the constitution so reviewers can verify compliance.

## Governance

This constitution supersedes ad hoc practices for the Prices Service. Any change to governance, architecture constraints, testing expectations, or public API rules MUST be documented, versioned, and reviewed before adoption. The project MUST prefer small, explicit amendments over silent reinterpretation of existing principles.

All implementation work MUST verify compliance with this constitution before merge. When a proposed decision conflicts with a principle, the team MUST resolve the conflict in favor of the constitution or amend the constitution explicitly in a separate governance change.

**Version**: 1.2.0 | **Ratified**: 2026-11-08 | **Last Amended**: 2026-11-08
