# Upstream PR Batch 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Adopt the ten backend correctness and data-access PRs assigned to Batch 1 with focused regression tests, safe concurrency semantics, and auditable provenance.

**Architecture:** Keep each PR behavior in an independent commit. Direct PRs are cherry-picked with their original authors after a failing regression test proves the baseline defect; adapted PRs use small package-visible test seams and retain source provenance in commit trailers. Run narrow Maven module tests after each task and the full Java 8/Node 16 Maven gate at batch completion.

**Tech Stack:** Java 8, JUnit Jupiter 5, Mockito, Apache Calcite 1.26, MyBatis annotations, Apache POI 5, Apache Commons CSV 1.8, Druid 1.2.4, Maven 3.9.16.

---

## Runtime Setup

Run Maven commands with the verified Batch 0 toolchain:

```powershell
$env:JAVA_HOME='F:\Users\Administrator\Cache\Temp\codex-datart-zulu8u492\expanded\zulu8.94.0.17-ca-jdk8.0.492-win_x64'
$nodeBin='F:\Users\Administrator\Cache\NpmCache\_npx\c449be5f412b5172\node_modules\.bin'
$env:Path="$env:JAVA_HOME\bin;$nodeBin;$env:Path"
$mvn='F:\Users\ADMINI~1\Cache\Temp\codex-datart-maven-3.9.16\apache-maven-3.9.16\bin\mvn.cmd'
```

### Task 1: Trim CSV Fields (#2263, direct)

**Files:**
- Create: `core/src/test/java/datart/core/common/CSVParseTest.java`
- Cherry-pick: `35c5d727ce61aac050489bd58d7e59edebbc17e3`

- [x] **Step 1: Write the failing CSV test**

Create a JUnit Jupiter test using `@TempDir`, write `name, amount\n Alice , 42 \n`, call `CSVParse.create(path).parse()`, and assert the second row equals `Arrays.asList("Alice", "42")` while an embedded value such as `"Alice Smith"` remains intact.

- [x] **Step 2: Run the red test**

```powershell
& $mvn -pl core -Dtest=CSVParseTest test
```

Expected: FAIL because the parsed values retain surrounding spaces.

- [x] **Step 3: Apply the focused upstream commit**

Temporarily preserve the uncommitted test, cherry-pick `refs/remotes/upstream/pr/2263`, then restore the test. The production change must be exactly `CSVFormat.DEFAULT.withTrim()`.

- [x] **Step 4: Run and commit the green test**

Run the command from Step 2; expect all `CSVParseTest` cases to pass. Commit the test as `test: cover trimmed CSV values`.

### Task 2: Restore Druid Recovery Defaults (#2360, direct)

**Files:**
- Create: `data-providers/jdbc-data-provider/src/test/java/datart/data/provider/jdbc/DataSourceFactoryDruidImplTest.java`
- Cherry-pick: `fe0e8c8e5b3b7a30fa03aeadd5a757665d3d825e`

- [x] **Step 1: Write the failing factory test**

Construct `JdbcProperties` with the H2 driver and an in-memory URL, create the Druid datasource, and assert:

```java
assertFalse(dataSource.isBreakAfterAcquireFailure());
assertEquals(3, dataSource.getConnectionErrorRetryAttempts());
```

Close the datasource in `finally`.

- [x] **Step 2: Verify red, apply upstream, verify green**

Run `& $mvn -pl data-providers/jdbc-data-provider -am -Dtest=DataSourceFactoryDruidImplTest -Dsurefire.failIfNoSpecifiedTests=false test`; expect the baseline assertions to fail. Preserve the test, cherry-pick `refs/remotes/upstream/pr/2360`, restore it, and rerun expecting PASS.

- [x] **Step 3: Commit the regression test**

Commit as `test: cover Druid recovery defaults`.

### Task 3: Fall Back for Unknown JDBC Types (#2354, adapted)

**Files:**
- Create: `data-providers/data-provider-base/src/test/java/datart/data/provider/jdbc/DataTypeUtilsTest.java`
- Modify: `data-providers/data-provider-base/src/main/java/datart/data/provider/jdbc/DataTypeUtils.java`

- [ ] **Step 1: Write and run the failing type-family test**

Assert `Types.INTEGER` maps to `ValueType.NUMERIC`, `Types.TIMESTAMP` maps to `ValueType.DATE`, and `Integer.MIN_VALUE` maps to `ValueType.STRING`. Run `& $mvn -pl data-providers/data-provider-base -am -Dtest=DataTypeUtilsTest -Dsurefire.failIfNoSpecifiedTests=false test`; expect a null-family failure for the unknown value.

- [ ] **Step 2: Add the null-family fallback**

Before the family switch, add:

```java
if (family == null) {
    return ValueType.STRING;
}
```

- [ ] **Step 3: Verify and commit with provenance**

Rerun the focused test and commit as `fix: fall back for unknown JDBC types` with:

```text
Upstream-PR: running-elephant/datart#2354
Upstream-Commit: c3caeec8db318691f297076a502a7c2c6aa6fd9d
Co-authored-by: ghy <ghyghoo8@qq.com>
```

### Task 4: Render COUNT DISTINCT Ordering (#2277, direct)

**Files:**
- Create: `data-providers/data-provider-base/src/test/java/datart/data/provider/calcite/SqlBuilderTest.java`
- Cherry-pick: `145c0557d9523fa64d1f68fff0ba4f7d92e3ae34`

- [ ] **Step 1: Write and run the failing order-node test**

Create an `OrderOperator` for column `user_id`, aggregate `COUNT_DISTINCT`, and descending order. Invoke `createOrderNode` through `ReflectionTestUtils`, render it with `H2Dialect.DEFAULT`, and assert the SQL contains `COUNT(DISTINCT` and ends in `DESC`. Run the data-provider-base focused test; expect the baseline to render `COUNT(user_id)`.

- [ ] **Step 2: Apply the upstream commit and verify**

Preserve the test, cherry-pick `refs/remotes/upstream/pr/2277`, restore the test, and rerun the focused test expecting PASS.

- [ ] **Step 3: Commit the regression test**

Commit as `test: cover count distinct ordering`.

### Task 5: Replace Variables Inside SQL Functions (#2170, adapted)

**Files:**
- Create: `data-providers/data-provider-base/src/test/java/datart/data/provider/jdbc/RegexVariableResolverTest.java`
- Modify: `data-providers/data-provider-base/src/main/java/datart/data/provider/jdbc/RegexVariableResolver.java`

- [ ] **Step 1: Write fallback-regression tests**

Use the issue's essential expression:

```sql
parent_path LIKE concat('%', $VAR$, '%') OR enterprise_code = $VAR$
```

Resolve a string query variable with value `aabbcc`, apply returned replacement pairs in normal resolver order, and assert no `$VAR$` remains, the `concat` call is intact, and the direct equality is still handled as a comparison. Also assert ordinary `age >= $AGE$` behavior remains unchanged.

- [ ] **Step 2: Run the red test**

Run the data-provider-base focused test and expect the variable nested in `concat` to remain unresolved when another direct expression is matched.

- [ ] **Step 3: Implement range-aware fallback**

Keep the existing supported-operator expression matching narrow. Record the matched expression ranges for each variable; create normal `VariablePlaceholder` objects for direct comparison ranges and add one `SimpleVariablePlaceholder` when any occurrence lies outside those ranges. Quote the variable fragment with `Pattern.quote` rather than interpolating regex metacharacters.

- [ ] **Step 4: Verify and commit with provenance**

Run the focused tests and commit as `fix: replace variables nested in SQL functions` with PR `#2170`, source SHA `da5338edcc18bacb66a8a0daf88db7702650d439`, and `Co-authored-by: mayu <qtt-elx1w0hdf@dingtalk.com>`.

### Task 6: Enforce Exact Column Permissions (#2283, adapted)

**Files:**
- Create: `data-providers/data-provider-base/src/test/java/datart/data/provider/ProviderManagerTest.java`
- Create: `server/src/test/java/datart/server/service/impl/DataProviderServiceImplTest.java`
- Modify: `data-providers/data-provider-base/src/main/java/datart/data/provider/ProviderManager.java`
- Modify: `server/src/main/java/datart/server/service/impl/DataProviderServiceImpl.java`

- [ ] **Step 1: Write permission tests**

Cover these cases: `null` and wildcard include sets leave rows unchanged; an empty include set nulls every cell; selecting `id` does not expose `order_id`; `SUM(id)` and `COUNT(DISTINCT id)` are permitted by `id`; a key containing regex metacharacters is compared literally. Separately verify no role-column rows produce wildcard access, while one or more rows whose JSON permissions are all `[]` produce an empty set.

- [ ] **Step 2: Run the red tests**

Run both focused module tests. Expect empty permissions to remain unrestricted and substring keys to be exposed.

- [ ] **Step 3: Implement explicit permission semantics**

Change `excludeColumns` to package visibility. Treat `null` or wildcard as unrestricted, but treat an empty set as deny-all. Replace `contains`/dynamic regex with a literal aggregate parser that accepts only a simple function name and an exact argument, optionally prefixed by `DISTINCT`. Extract a package-visible permission conversion helper in `DataProviderServiceImpl`; no relation rows return wildcard, while present rows with no selected columns return an empty set.

- [ ] **Step 4: Verify and commit with provenance**

Run both tests and commit as `fix: enforce exact column permissions` with PR `#2283`, source SHA `fe6bfbc51865fabd44e36f41525259b0bb35d21a`, and `Co-authored-by: licheng.w.exiao <631948983@qq.com>`.

### Task 7: Refresh JDBC Adapters Atomically (#2356, adapted)

**Files:**
- Create: `data-providers/jdbc-data-provider/src/test/java/datart/data/provider/JdbcDataProviderTest.java`
- Modify: `data-providers/jdbc-data-provider/src/main/java/datart/data/provider/JdbcDataProvider.java`

- [ ] **Step 1: Write cache lifecycle tests**

Use a test subclass overriding a new protected `createDataProvider(JdbcProperties)` seam. Assert unchanged properties reuse the adapter; changed properties create the replacement before closing the old adapter; the old adapter closes exactly once; and concurrent requests for the same changed source create only one replacement.

- [ ] **Step 2: Run the red tests**

Run the jdbc-data-provider focused test. Expect changed properties to continue returning the stale adapter.

- [ ] **Step 3: Implement atomic replacement**

Convert source properties once and use `cachedProviders.compute(sourceId, ...)`. Return the existing adapter when its `JdbcProperties` are equal. Otherwise create the replacement first, close the previous adapter once, and return the replacement. Keep failures from removing a working adapter.

- [ ] **Step 4: Verify and commit with provenance**

Run focused tests and commit as `fix: refresh cached JDBC providers atomically` with PR `#2356`, source SHA `7244f9317853652963b157858af2cfe27d69fcfc`, and `Co-authored-by: kanlon <Canlong2015@126.com>`.

### Task 8: Remove Archived Schema Jobs (#2359, direct)

**Files:**
- Create: `server/src/test/java/datart/server/job/SchemaSyncJobTest.java`
- Cherry-pick: `ce8fb90c7825b0dfcf278d412d01e114ca833648`

- [ ] **Step 1: Write job lifecycle tests**

Install a mocked `ApplicationContext` through `new Application().setApplicationContext(context)`. Mock `SourceService`, `Scheduler`, `JobExecutionContext`, and `JobDetail`. Assert missing and archived sources delete the Quartz job and do not execute schema reads; an active source keeps the job and reaches the sync path.

- [ ] **Step 2: Verify red, apply upstream, verify green**

Run the server focused test; expect archived sources not to delete the job. Preserve the test, cherry-pick `refs/remotes/upstream/pr/2359`, restore it, and rerun expecting PASS.

- [ ] **Step 3: Commit the regression test**

Commit as `test: cover archived schema jobs`.

### Task 9: Batch Dashboard Query Variables (#2189, adapted)

**Files:**
- Create: `server/src/test/java/datart/server/service/impl/VariableServiceImplTest.java`
- Create: `server/src/test/java/datart/server/service/impl/DashboardServiceImplTest.java`
- Modify: `core/src/main/java/datart/core/mappers/ext/VariableMapperExt.java`
- Modify: `server/src/main/java/datart/server/service/VariableService.java`
- Modify: `server/src/main/java/datart/server/service/impl/VariableServiceImpl.java`
- Modify: `server/src/main/java/datart/server/service/impl/DashboardServiceImpl.java`

- [ ] **Step 1: Write batch and empty-set tests**

Assert `VariableServiceImpl.listViewQueryVariablesByViewIds(Collections.emptySet())` returns empty without invoking the mapper. Extract a package-visible dashboard helper and assert a non-empty set triggers exactly one mapper-backed service call while preserving organization variables; an empty set triggers no view-variable call.

- [ ] **Step 2: Run the red tests**

Run the server focused tests and expect the batch API/helper to be absent.

- [ ] **Step 3: Implement guarded batching**

Add the MyBatis `IN` foreach query for non-empty view IDs. Guard null/empty sets in `VariableServiceImpl` before invoking MyBatis. Replace the dashboard loop with one batch call and keep deterministic organization variables first.

- [ ] **Step 4: Verify and commit with provenance**

Run focused tests and commit as `perf: batch dashboard query variables` with PR `#2189`, source SHA `bd94783c613c27ef6b3eb4a7ca2d04b6abcb71f0`, and `Co-authored-by: kanlon <Canlong2015@126.com>`.

### Task 10: Generate Collision-Free Excel Sheet Names (#2131, adapted)

**Files:**
- Create: `server/src/test/java/datart/server/service/impl/AttachmentExcelServiceImplTest.java`
- Modify: `server/src/main/java/datart/server/service/impl/AttachmentExcelServiceImpl.java`

- [ ] **Step 1: Write sheet-name tests**

Exercise a package-visible `uniqueSheetName(Workbook, String, int)` helper with duplicates, a pre-existing suffixed name (`Sales`, `Sales (2)`, `Sales`), case-only duplicates, invalid Excel characters, blank names, and names longer than 31 characters. Assert every result is valid, at most 31 characters, and unique in the workbook.

- [ ] **Step 2: Run the red test**

Run the server focused test and expect the helper to be absent/current duplicate behavior to fail.

- [ ] **Step 3: Implement safe deterministic names**

Sanitize with `WorkbookUtil.createSafeSheetName`, default blanks to `Sheet<n>`, and probe case-insensitively for a free name. Add ` (2)`, ` (3)`, and so on while truncating the base so the final name never exceeds 31 characters. Use the helper immediately before `POIUtils.withSheet`.

- [ ] **Step 4: Verify and commit with provenance**

Run focused tests and commit as `fix: generate unique Excel sheet names` with PR `#2131`, source SHA `dc26f7be11bc4b624195507596227c3006645d83`, and `Co-authored-by: kanlon <Canlong2015@126.com>`.

### Task 11: Update Evidence and Run Batch Gates

**Files:**
- Modify: `docs/upstream-prs/2026-07-28-disposition.md`
- Modify: `docs/superpowers/plans/2026-07-28-upstream-pr-batch-1.md`

- [ ] **Step 1: Run all focused Batch 1 tests together**

Run module-scoped tests for all new test classes with Java 8. Expected: zero failures and zero errors.

- [ ] **Step 2: Run the full backend gate**

```powershell
& $mvn test
```

Expected: all 9 Reactor modules succeed. Maven must use Node 16/npm 8 for the embedded frontend build.

- [ ] **Step 3: Run the package gate**

```powershell
& $mvn -DskipTests package
```

Expected: all modules succeed and the install ZIP is regenerated without tracked build artifacts.

- [ ] **Step 4: Update all ten ledger rows**

Replace `pending Batch 1` in the integration and verification columns for PRs `#2360`, `#2359`, `#2356`, `#2354`, `#2283`, `#2277`, `#2263`, `#2189`, `#2170`, and `#2131` with the exact integration commit and focused/full gate evidence.

- [ ] **Step 5: Verify and commit Batch 1 evidence**

Run `git diff --check`, verify no Batch 1 row remains pending, verify the worktree contains no unexpected tracked artifact, mark every plan checkbox complete, and commit as `docs: record Batch 1 PR integrations`.
