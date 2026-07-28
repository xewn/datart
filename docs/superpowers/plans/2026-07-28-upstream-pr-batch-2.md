# Upstream PR Batch 2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Use superpowers:test-driven-development for every behavior change and superpowers:verification-before-completion before recording the batch as complete.

**Goal:** Adopt the seven frontend bug-fix PRs assigned to Batch 2, absorb three duplicate date-controller PRs, and prove component lifecycle, event cleanup, chart conversion, and time-range behavior with deterministic Jest coverage.

**Architecture:** Keep each user-visible behavior independently revertible. Direct PRs retain their upstream implementation and authorship after a red regression test. The adapted #2350 work is split into date-controller semantics and stable listener identity. The adapted #2198 work uses a small reusable React hook so chart-event replacement and cleanup can be tested without mounting the full shared-chart page. No upstream lock-file, branch-baseline, or unrelated source changes are accepted.

**Tech Stack:** React 17, TypeScript 4, Redux Toolkit, Jest 26, Testing Library 12, Enzyme 3, Moment 2, Node 16.20.2, npm 8.19.4, Java 8, Maven 3.9.16.

---

## Runtime Setup

Run frontend commands from `frontend` with the verified Batch 0 Node runtime:

```powershell
$nodeBin='F:\Users\Administrator\Cache\NpmCache\_npx\c449be5f412b5172\node_modules\.bin'
$env:Path="$nodeBin;$env:Path"
$testMatch='--testMatch=**/src/**/__tests__/**/*.{spec,test}.{js,jsx,ts,tsx}'
```

Run Maven gates from the repository root with:

```powershell
$env:JAVA_HOME='F:\Users\Administrator\Cache\Temp\codex-datart-zulu8u492\expanded\zulu8.94.0.17-ca-jdk8.0.492-win_x64'
$env:Path="$env:JAVA_HOME\bin;$nodeBin;$env:Path"
$mvn='F:\Users\ADMINI~1\Cache\Temp\codex-datart-maven-3.9.16\apache-maven-3.9.16\bin\mvn.cmd'
```

Focused Jest commands use `npm run test -- --watchAll=false --runInBand --silent $testMatch --runTestsByPath <paths>` to avoid the linked-worktree absolute `testMatch` defect documented in the integration design.

### Task 1: Clear Removed Join-Table State (#2368, direct)

**Files:**
- Create: `frontend/src/app/pages/MainPage/pages/ViewPage/Main/StructView/components/__tests__/SelectDataSource.test.tsx`
- Cherry-pick: `6b3f35d11dbe443a84220ef4b9397b08502e5ff0`

- [ ] **Step 1: Write the stale-state regression test**

Render `SelectDataSource` in `JOINS` mode with mocked Redux selectors and an initial `joinTable` containing a table and columns. Rerender with the same source but no join table. Assert the previously selected table label disappears and the translated select-table label is shown. Retain a second assertion proving a replacement join table still renders normally.

- [ ] **Step 2: Run the red test**

Run the focused Jest file. Expected: FAIL because the effect only sets state when `joinTable.table` is present and leaves the old selection behind when the relation is removed.

- [ ] **Step 3: Apply the upstream commit and verify green**

Preserve the test, cherry-pick `refs/remotes/upstream/pr/2368`, restore the test, and rerun. The production diff must only add the explicit `JOINS` empty-state reset and the property-access cleanup from the upstream commit.

- [ ] **Step 4: Commit the regression test**

Commit as `test: cover removed join table state`.

### Task 2: Preserve Dashboard URL Date Values (#2350, adapted; absorbs #2366, #2330, #2309)

**Files:**
- Create: `frontend/src/app/pages/DashBoardPage/utils/__tests__/widget.test.ts`
- Modify: `frontend/src/app/pages/DashBoardPage/utils/widget.ts`

- [ ] **Step 1: Write date-controller tests**

Extract a small exported `applyControllerUrlValue(content, value)` seam and test the intended behavior through it: range time uses distinct start and end values, a one-value range uses the start as the end, and a single-time controller formats the first URL value rather than the array object. Also cover an absent optional second range value without throwing.

- [ ] **Step 2: Run the red test**

Run the focused Jest file. Expected: FAIL because range end currently always receives index 0 and the single-time path passes the full array to `formatTime`.

- [ ] **Step 3: Implement the unified date behavior**

Move only the controller-date mutation from `getWidgetMap` into the tested seam. Use `_value?.[1] ?? _value?.[0]` for range end and `_value?.[0]` for single time. Keep non-date controller behavior unchanged. This implements #2350 and records #2366, #2330, and #2309 as absorbed rather than applying their stale branches.

- [ ] **Step 4: Verify and commit with provenance**

Rerun the focused test and commit as `fix: preserve dashboard URL date values` with the source commit `9875ce2507b5ef6ee844f78040c7166239d7578a`, an `Upstream-PR: running-elephant/datart#2350` trailer, absorbed-PR trailers for #2366/#2330/#2309, and the relevant upstream co-authors.

### Task 3: Remove Chart Selection Listeners Reliably (#2350, adapted)

**Files:**
- Modify: `frontend/src/app/models/__tests__/ChartSelectionManager.test.ts`
- Modify: `frontend/src/app/models/ChartSelectionManager.ts`

- [ ] **Step 1: Strengthen listener-identity tests**

Use one manager instance to attach and remove window listeners, then assert each `removeEventListener` callback is the same function object passed to the matching `addEventListener`. Do the same for ZRender `on`/`off`. Retain existing behavioral assertions for keyboard and click handlers.

- [ ] **Step 2: Run the red tests**

Expected: FAIL because every `.bind(this)` call creates a new function, so removal cannot detach the registered handler.

- [ ] **Step 3: Store stable bound handler references**

Bind the window and ZRender handlers once per manager instance and reuse those references for attach/remove. Keep the handlers private and preserve existing selection semantics.

- [ ] **Step 4: Verify and commit with provenance**

Run the focused manager tests and commit as `fix: retain chart listener identities` with PR #2350, source SHA `e910efdab3710a3946ed880c244d8b9ca0d4ffbf`, and `Co-authored-by: Candy <zyy9803@foxmail.com>`.

### Task 4: Avoid Resize Events on Loading-Only Changes (#2346, direct)

**Files:**
- Create: `frontend/src/app/components/ChartIFrameContainer/__tests__/ChartIFrameLifecycleAdapter.test.tsx`
- Cherry-pick: `6af405c69f8777de1307b57cb3a1e87535f15840`

- [ ] **Step 1: Write the lifecycle regression test**

Mock `useFrame`, `ChartIFrameResourceLoader`, and `ChartIFrameEventBroker`. Render the adapter to a successful mounted state, record published Resize events, then rerender with only `isLoadingData` changing false to true and back to false. Assert loading changes do not add a Resize event; separately rerender with a changed width and assert exactly one Resize is published.

- [ ] **Step 2: Run red, apply upstream, and run green**

Expected baseline failure: the false transition republishes Resize because `isLoadingData` is in the resize effect dependency list. Preserve the test, cherry-pick the direct PR, restore the test, and rerun expecting PASS.

- [ ] **Step 3: Commit the regression test**

Commit as `test: cover chart resize dependencies`.

### Task 5: Guard Missing Basic-Table Rows (#2313, direct)

**Files:**
- Modify: `frontend/src/app/components/ChartGraph/BasicTableChart/__tests__/BasicTableChart.test.jsx`
- Cherry-pick: `f7dbe0f7dc7c794119597f0af549bd4a37186018`

- [ ] **Step 1: Write missing and present row tests**

Build a minimal chart-data-set double for `getFlatColumns`. Invoke the generated column's `onCell` with an out-of-range row index and assert `{}` is returned without reading cell data. Invoke it with a valid row and assert the normal cell metadata/event path remains populated.

- [ ] **Step 2: Run red, apply upstream, and run green**

Expected baseline failure: `row.getCell` throws for the missing row. Preserve the test, cherry-pick the direct PR, restore the test, and rerun expecting PASS.

- [ ] **Step 3: Commit the regression test**

Commit as `test: cover missing basic table rows`.

### Task 6: Refresh Shared-Chart Mouse Events Safely (#2198, adapted)

**Files:**
- Create: `frontend/src/app/hooks/useChartMouseEvents.ts`
- Create: `frontend/src/app/hooks/__tests__/useChartMouseEvents.test.tsx`
- Modify: `frontend/src/app/pages/SharePage/Chart/ChartPreviewBoardForShare.tsx`

- [ ] **Step 1: Write hook lifecycle tests**

Use a tiny React harness with mock chart objects. Assert initial render registers one event array, rerender with identical chart/events does not register again, replacing the chart clears the old chart with `registerMouseEvents([])` before registering the new chart, changing the memoized event array replaces it once, and unmount clears the current chart.

- [ ] **Step 2: Run the red test**

Expected: FAIL because registration is currently tied to the initial data-fetch callback and has no stable replacement/cleanup lifecycle.

- [ ] **Step 3: Implement stable registration**

Add the focused hook using `useEffect([chart, events])`. In `ChartPreviewBoardForShare`, build the click-event array with `useMemo`/stable callback dependencies and pass it to the hook. Remove registration from the fetch callback. Ensure event callbacks see current preview/filter/drill state and repeated renders do not accumulate listeners.

- [ ] **Step 4: Verify and commit with provenance**

Run the hook test and any touched share-chart test. Commit as `fix: refresh shared chart mouse events` with PR #2198, source SHA `60815c73df40a9722e3386cb9010a10ccd1bf476`, and `Co-authored-by: elaine <766205010@qq.com>`.

### Task 7: Restore Date-Level Fields for Embedded Charts (#2196, direct)

**Files:**
- Create: `frontend/src/app/pages/ChartWorkbenchPage/slice/__tests__/thunks.test.ts`
- Cherry-pick: `f90068338676e201939cc410dac817deff80dfa1`

- [ ] **Step 1: Write thunk payload tests**

Execute `fetchChartAction` with an embedded `backendChart` containing date metadata and a config. Assert its fulfilled payload regenerates date-level computed fields while retaining non-date computed fields and all other chart properties. Cover a backend chart without config. Mock the request path and assert a chart fetched by `chartId` still goes through the existing `convertToChartDto` path unchanged.

- [ ] **Step 2: Run red, apply upstream, and run green**

Expected baseline failure: embedded charts are returned without regenerated date-level computed fields. Preserve the test, cherry-pick the direct PR, restore the test, and rerun expecting PASS.

- [ ] **Step 3: Commit the regression test**

Commit as `test: cover embedded chart date fields`.

### Task 8: Correct Last-Month and Last-Year Ranges (#2089, direct)

**Files:**
- Modify: `frontend/src/app/utils/__tests__/time.test.ts`
- Cherry-pick: `95023762062e02c4687bab9bdf8f446e1f15412d`

- [ ] **Step 1: Add fixed-clock range tests**

Use Jest modern fake timers and a fixed local time. Assert `LAST_1_MONTH` starts one month earlier at start-of-day and ends today at end-of-day. Assert `LAST_1_YEAR` starts one year earlier and ends today. Include a leap-day case and verify the optional output format is honored.

- [ ] **Step 2: Run red, apply upstream, and run green**

Expected baseline failure: the current implementation returns the current calendar month/year rather than the rolling last month/year. Preserve the test, cherry-pick the direct PR, resolve only the stale `dateFormat` call-site difference, restore the test, and rerun expecting PASS.

- [ ] **Step 3: Commit the regression test**

Commit as `test: cover recommended month and year ranges`.

### Task 9: Run Batch Gates and Record Evidence

**Files:**
- Modify: `docs/upstream-prs/2026-07-28-disposition.md`
- Modify: `docs/superpowers/plans/2026-07-28-upstream-pr-batch-2.md`

- [ ] **Step 1: Run all focused Batch 2 tests together**

Run the eight touched/new Jest files in one `--runTestsByPath` command under Node 16. Expected: zero failures and zero errors.

- [ ] **Step 2: Run frontend gates**

Run `npm run checkTs`, the full non-watch Jest suite with the relative `testMatch` override, and `npm run build:all`. Expected: TypeScript, all suites, the Rollup task bundle, and the production React build succeed.

- [ ] **Step 3: Run repository gates**

Run `& $mvn test` and `& $mvn -o -DskipTests package` with Java 8 and Node 16. Expected: all 9 Reactor modules succeed and the install ZIP is regenerated.

- [ ] **Step 4: Update all Batch 2 ledger rows**

Replace pending entries for #2368, #2350, #2346, #2313, #2198, #2196, and #2089 with exact commits and gate evidence. Point absorbed #2366, #2330, and #2309 to the #2350 date integration commit and record their focused coverage.

- [ ] **Step 5: Verify and commit Batch 2 evidence**

Restore only known generated `frontend/package-lock.json` drift, run `git diff --check`, verify no Batch 2 table row remains pending, verify no plan checkbox remains open, record final suite totals and ZIP SHA-256, and commit as `docs: record Batch 2 PR integrations`.
