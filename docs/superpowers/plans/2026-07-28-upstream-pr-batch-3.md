# Upstream PR Batch 3 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans and superpowers:test-driven-development for each task. Use superpowers:verification-before-completion before updating the disposition ledger.

**Goal:** Adopt the Story page-selection improvement from #2261 and a maintainable pivot-table diagonal-header feature based on #2033, with state, localization, persistence-default, rendering, and visual-shape regression coverage.

**Architecture:** Preserve #2261 as two direct upstream commits after a component-level regression test. Adapt #2033 to the current AntV S2 API by injecting a corner renderer only when the style toggle is enabled; leave the native S2 corner renderer untouched when disabled. Keep diagonal geometry in a pure shape-description helper so visual output can be snapshot-tested without relying on JSDOM canvas pixels.

**Tech Stack:** React 17, TypeScript 4, Ant Design 4, AntV S2 1.19, Jest 26, Testing Library 12, Node 16.20.2, npm 8.19.4, Java 8, Maven 3.9.16.

---

## Runtime Setup

Use the same Node 16, relative Jest `testMatch`, Java 8, and Maven paths documented in the Batch 2 plan. Focused Jest commands run with `CI=true`, `--watchAll=false`, `--runInBand`, and `--runTestsByPath`.

### Task 1: Show Existing Story Pages in the Add Modal (#2261, direct)

**Files:**
- Create: `frontend/src/app/pages/StoryBoardPage/components/__tests__/StoryPageAddModal.test.tsx`
- Cherry-pick: `72bee4d4` and `0faca75e`

- [x] **Step 1: Write modal state and localization tests**

Mock only Ant Design `Modal`/`Table` presentation while rendering the real `StoryPageAddModal` hooks. Start closed with one existing `StoryPage`, then open the modal. Assert the existing `relId` is selected, its checkbox is disabled, a new dashboard remains selectable, and the localized `viz.board.setting.addStoryPage` title is used. Select the existing and new IDs, confirm, and assert `onSelectedPages` receives only the new ID.

- [x] **Step 2: Run the red test**

Expected: FAIL because the current modal has no `sortedPages` input, clears all selections, does not disable existing pages, submits every selected ID, and uses a literal English title.

- [x] **Step 3: Apply both direct upstream commits**

Preserve the test and cherry-pick the two PR commits in order. Verify only Story toolbar/editor/modal wiring and the English/Chinese translation keys are changed; do not accept unrelated lock or formatting churn.

- [x] **Step 4: Verify and commit the regression test**

Run the focused test and `npm run checkTs`. Commit the test as `test: cover existing Story pages in add modal`.

### Task 2: Add a Safe Pivot Diagonal Header (#2033, adapted)

**Files:**
- Modify: `frontend/src/app/components/ChartGraph/PivotSheetChart/PivotSheetChart.tsx`
- Modify: `frontend/src/app/components/ChartGraph/PivotSheetChart/config.ts`
- Modify: `frontend/src/app/components/ChartGraph/PivotSheetChart/__tests__/PivotSheetChart.test.jsx`

- [x] **Step 1: Write configuration and visual-shape tests**

Assert the style configuration exposes `enableSlash` as a checkbox with default `false`, and both Chinese/English chart-local translations contain `slashHeader`. Add a pure shape-description snapshot covering background polygon, diagonal line, column label (excluding `$$extra$$`), and row label. Assert the corner-renderer factory returns `undefined` when disabled and emits the snapshotted shapes to a mock S2 node when enabled.

- [x] **Step 2: Run the red tests**

Expected: FAIL because the toggle, renderer, translations, and shape helper do not exist.

- [x] **Step 3: Implement against the current S2 surface**

Add `enableSlash` to the existing style group with default `false`. Export a small `buildSlashCornerShapes` helper and an enabled-only renderer factory. Use the public group/node `addShape` surface for polygon, line, and two text shapes. In `getOptions`, add `cornerHeader` only when enabled; omit the property when disabled so S2 retains its native renderer. Merge theme text/cell values without mutating the source theme.

- [x] **Step 4: Verify and commit with provenance**

Run the focused pivot test and `npm run checkTs`. Commit as `feat: add pivot table diagonal header` with PR #2033, source SHA `9a1a9f9f8876656af6be2c7c1b538262a4abe160`, and `Co-authored-by: ruanwe <88fantasy@gmail.com>`.

### Task 3: Run Batch Gates and Record Evidence

**Files:**
- Modify: `docs/upstream-prs/2026-07-28-disposition.md`
- Modify: `docs/superpowers/plans/2026-07-28-upstream-pr-batch-3.md`

- [x] **Step 1: Run focused and full frontend gates**

Run both touched Jest files together, `npm run checkTs`, the complete Jest suite with the relative `testMatch` override, and `npm run build:all`. Record suite/test/snapshot totals.

- [x] **Step 2: Run repository gates**

Run Java 8 `mvn test` and offline `mvn -DskipTests package` with Node 16 on `PATH`. Restore generated lock-file drift and record Surefire totals plus final install ZIP size/SHA-256.

- [x] **Step 3: Update the two ledger rows**

Replace pending entries for #2261 and #2033 with exact integration commits and focused/full gate evidence. Add a Batch 3 evidence section.

- [x] **Step 4: Verify and commit Batch 3 evidence**

Run `git diff --check`, verify no Batch 3 row or plan checkbox remains pending, verify no unexpected tracked artifact remains, and commit as `docs: record Batch 3 PR integrations`.
