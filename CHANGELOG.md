# Changelog

All notable changes to this project are documented in this file.
Format based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Added
- Project scaffold: Spring Boot 4.1, Java 21, MySQL, JWT auth, four roles
  (Admin, Compliance Officer, Reviewer, Client).
- JWT authentication (`/api/auth/register`, `/api/auth/login`) with BCrypt
  password hashing; `X-User-Role` header cross-checked against the token's
  user on every protected request (header can never grant extra access on
  its own).
- Default admin account seeded on first startup (`AdminSeeder`).
- Admin user management: create staff accounts, list/filter by role,
  activate/deactivate.
- KYC request lifecycle with two request types, **Individual** and
  **Business/Entity**, each with their own mandatory-field set.
- Draft-based flow: client creates a request in `DRAFT` state, edits freely,
  and only mandatory fields/documents are validated at formal submission
  (`POST /api/kyc/{id}/submit`), after which the request becomes read-only.
- `BeneficialOwner` child entity for Business/Entity requests.
- Admin-managed **client → reviewer → compliance officer** routing
  (`UserMapping` / `/api/admin/mappings`). A client's requests are visible
  to and actionable only by their assigned reviewer and compliance officer.
- Multi-stage rejection chain: reviewer reject sends the form back to the
  **client**; compliance officer reject sends it back to the **reviewer**
  (the previous stage) rather than the client, who re-decides.
- Mandatory rejection reason on every reject action (`DecisionRequest.reason`).
- `formVersion` on each KYC request, incremented every time the client
  resubmits after a rejection.

### Changed
- Removed the document-upload subsystem (`Document` entity, repository,
  controller, upload/delete endpoints) — form-data only, no file storage.
- Removed duplicate `reviewer_id` / `compliance_officer_id` columns from
  `kyc_requests`; the actor is derived from `UserMapping` instead, since
  enforcement guarantees they're always the same.
- `KycStatus.REJECTED` (terminal) replaced by `RESUBMISSION_REQUIRED` /
  `RETURNED_TO_REVIEWER` — only `APPROVED` now ends the workflow.

### Known trade-offs
- If an admin remaps a client after requests were already decided, historical
  requests resolve to the new reviewer/compliance officer, not whoever
  actually made the decision. A dedicated decision-history table would be
  needed for audit-grade traceability.
- `spring.jpa.hibernate.ddl-auto=update` does not drop columns or relax
  constraints on schema changes; migrations during development were applied
  manually. Recommend switching to Flyway/Liquibase before production.
