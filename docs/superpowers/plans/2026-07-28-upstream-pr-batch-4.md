# Upstream PR Batch 4 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans, superpowers:test-driven-development, and superpowers:systematic-debugging. Use superpowers:verification-before-completion before updating the disposition ledger.

**Goal:** Integrate BigQuery support, custom table sorting, configurable interaction-dialog sizing, and the dashboard button widget from official PRs #2192, #2287, #2365, and #2203 without importing stale branch debris or regressing current S2/dashboard behavior.

**Architecture:** Keep database-specific alias constraints inside the BigQuery adapter. Carry custom-sort intent through the existing chart request model into SQL order operators. Extract only the dialog feature commits from #2365 and enforce normalized dimensions at the form boundary. Register the button as a first-class dashboard widget using current widget initialization, mapping, migration, and navigation patterns.

**Tech Stack:** Java 8, Maven 3.9.16, Calcite, React 17, TypeScript 4, Ant Design 4, AntV S2 1.19, Jest 26, Testing Library 12, Node 16.20.2, npm 8.19.4.

---

## Task 1: Add BigQuery Adapter and Dialect-Scoped Aggregate Aliases (#2192, adapted)

**Files:** BigQuery JDBC adapter/driver configuration and focused adapter tests.

- [ ] Add failing tests for BigQuery catalog/schema metadata routing and aggregate aliases that are safe during SQL execution but restored in returned dataframe metadata.
- [ ] Port the BigQuery adapter and driver registration from `6a36e0ab`, including the final comment cleanup from `b381d4b5`.
- [ ] Implement collision-free temporary aggregate aliases only inside the BigQuery adapter; never change frontend or shared `ChartColumn` alias semantics.
- [ ] Run focused JDBC tests and commit with PR #2192/source provenance and original-author credit.

## Task 2: Add Validated Custom Interaction Dialog Sizes (#2365, adapted)

**Files:** interaction form components/types/constants/hooks, widget defaults, dialog display hooks, locales, and component/unit tests.

- [ ] Add failing tests for normalized width/height/content-height defaults and bounds, including content height clamping when dialog height shrinks.
- [ ] Extract the actual feature from `c283633f` and corrections from `7c1071c5`; exclude application configuration, H2 data, npm settings, lock files, package metadata, and POM churn.
- [ ] Centralize dimension normalization so drill-through and view-detail forms persist the same valid shape; keep translated labels and warnings.
- [ ] Run focused tests and `npm run checkTs`, then commit with PR/source provenance and original-author credit.

## Task 3: Add End-to-End Custom Sorting (#2287, adapted)

**Files:** order/SQL builder, chart request/config types and helpers, table/pivot config, field actions/drag UI, locales, and focused Java/Jest tests.

- [ ] Add failing SQL tests for explicit custom-value order and fallback ordering, plus frontend request/helper tests for preserving custom order values.
- [ ] Adapt `337b3ac2` onto current code, retaining the Batch 3 pivot diagonal-header implementation and current drag/drop APIs.
- [ ] Validate empty/duplicate custom values, SQL literal handling, and compatibility with ordinary ASC/DESC/NONE sorting.
- [ ] Run focused Java/Jest tests and `npm run checkTs`, then commit with PR/source provenance and original-author credit.

## Task 4: Add Dashboard Button Widget (#2203, adapted)

**Files:** format control, widget registry/defaults/mapper/types, button widget/config, toolbar entry, migrations/locales, and focused tests.

- [ ] Add failing tests for widget initialization/config persistence and URL/dashboard/data-chart navigation behavior, including safe handling of an empty target.
- [ ] Adapt `bce86d24` to current widget contracts and migration defaults; use the existing icon library and semantic button interaction.
- [ ] Ensure editor/runtime rendering, serialization, target opening mode, keyboard activation, and accessible naming are covered without introducing generated assets.
- [ ] Run focused Jest tests and `npm run checkTs`, then commit with PR/source provenance and original-author credit.

## Task 5: Run Batch Gates and Record Evidence

- [ ] Run all Batch 4 focused suites, `npm run checkTs`, full Jest, and `npm run build:all`.
- [ ] Run Java 8 `mvn test` and offline `mvn -DskipTests package`; restore only known generated lock drift and hash the install ZIP.
- [ ] Update all four ledger rows and this plan with exact commits and verification evidence.
- [ ] Run hygiene/provenance checks and commit as `docs: record Batch 4 PR integrations`.
