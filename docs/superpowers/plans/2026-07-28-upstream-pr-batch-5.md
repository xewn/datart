# Upstream PR Batch 5 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans, superpowers:test-driven-development, and superpowers:systematic-debugging. Use superpowers:verification-before-completion before updating the disposition ledger.

**Goal:** Integrate the useful platform and extension behavior from official PRs #2236, #2165, #2016, and #1969 while preserving the verified Maven assembly, isolating data-source and authentication state, and making advanced calculations deterministic.

**Architecture:** Keep the current dependency-set assembly and add only portable Docker/Compose improvements. Add MongoDB as a normal provider module with source-scoped client lifecycle and deterministic BSON-to-dataframe conversion. Expose custom OAuth2 clients through a validated ServiceLoader SPI that retains normal TLS verification and rejects duplicates. Represent period-over-period calculations in the request model, then execute a fresh stateless calculator per aggregate using alias-based column lookup and immutable copied query parameters.

**Tech Stack:** Java 8, Maven 3.9.16, Spring Boot 2, Spring Data MongoDB, Spring Security OAuth2, ServiceLoader, React 17, TypeScript 4, Ant Design 4, Jest 26, Node 16.20.2, npm 8.19.4, Docker Compose.

---

## Task 1: Add Portable Docker Compose Packaging (#2236, adapted)

- [x] Add an assembly/package assertion for executable launch scripts and included Compose/Docker files.
- [x] Preserve the current Maven frontend and runtime-dependency assembly; do not import the PR's duplicated dependency-copy layout or mirror-pinned Node installer.
- [x] Add Compose deployment, runtime-directory ignores, executable archive modes, and a maintained Java 8 runtime image with local-build defaults and persistent files/logs.
- [x] Validate Compose syntax when Docker is available, inspect the install ZIP, and commit with PR #2236/source provenance and original-author credit.

## Task 2: Add a Source-Isolated MongoDB Document Provider (#2165, adapted)

- [x] Add failing tests for command-response conversion, deterministic column order/types, empty batches, invalid commands, and source-specific client cache keys/reset.
- [x] Add the document-provider module, SPI descriptor, configuration template, driver metadata, server dependency, and localized labels.
- [x] Parse commands before execution, return empty metadata collections instead of null, key clients by source identity and connection properties, and close/reset factories without exposing credentials.
- [x] Run focused module tests and package-content checks, then commit with PR #2165/source provenance and original-author credit.

## Task 3: Add a Fail-Closed Custom OAuth2 Client SPI (#2016, adapted)

- [x] Add failing tests for ServiceLoader discovery, duplicate/blank registration rejection, configured-client binding, unknown-client pass-through, and authentication routing.
- [x] Add the abstract client SPI/factory and adapt DingTalk plus the PR's WeChat client to no-argument providers; preserve standard TLS certificate and hostname verification.
- [x] Make the repository and filters consult one immutable validated registry, reject unconfigured clients, and avoid returning null authentication on provider failures.
- [x] Run focused security tests and commit with PR #2016/source provenance and original-author credit.

## Task 4: Add Deterministic Period-over-Period Calculations (#1969, adapted)

- [x] Add failing backend tests for previous-period and previous-year date keys, dimension isolation, null/missing history, zero denominators, and independent calculator instances.
- [x] Add failing frontend tests for calculation request serialization, alias uniqueness, deduplication, and safe action rendering when no date fields exist.
- [x] Adapt request/config/UI/locales and table/pivot rendering while retaining Batch 3/4 custom-sort and diagonal-header behavior.
- [x] Execute calculations by dataframe alias rather than positional assumptions, deep-copy mutable query lists before supplementary queries, and return null for undefined ratios instead of throwing.
- [x] Run focused Java/Jest tests and `npm run checkTs`, then commit with PR #1969/source provenance and original-author credit.

## Task 5: Run Batch and Final Repository Gates

- [x] Run all Batch 5 focused suites, `npm run checkTs`, full Jest, and `npm run build:all`.
- [x] Run Java 8/Node 16 `mvn test` and offline `mvn -DskipTests package`; restore only known lock drift and hash/inspect the install ZIP.
- [x] Update all four ledger rows and this plan with exact commits and verification evidence; run provenance and worktree hygiene checks.
- [ ] Review the complete integration diff, merge the integration branch into the fork's target branch without discarding user work, and push both the target and integration provenance branch.

## Verification Evidence

- Integration commits: #2236 `eb2b7fd1`, #2165 `1e487014`, #2016 `fe1fc578`, and #1969 `1f6f37ee`.
- Docker/assembly gate: `docker compose config --quiet` exited 0; the install ZIP contains Docker/Compose files and `100755` launch scripts.
- Focused backend gates: MongoDB provider 5/5, OAuth/security 11/11, and period comparison 4/4 passed.
- Focused frontend gates: request builder 23/23, chart helper 117/117, and advanced-calculation availability 1/1 passed; `npm run checkTs` exited 0.
- Full Jest: 92 suites passed; 683 tests passed, 5 skipped, 688 total; 5 snapshots passed.
- Full repository test: Java 8 and Node 16 `mvn test` exited 0; 50 Surefire tests ran, with 49 passed, 1 existing skip, zero failures, and zero errors. The Maven-bound `npm run build:all` succeeded.
- Offline package: `mvn -o -DskipTests package` exited 0 and produced a 185,354,215-byte install ZIP with SHA-256 `14CF06AEAD6638C8345E061990EF103BABD1224D43347AC7352F4215A157DD29`.
- Package inspection: MongoDB and OAuth JARs and their ServiceLoader descriptors are present; tracked lockfile drift was restored and the product worktree was clean before evidence updates.
