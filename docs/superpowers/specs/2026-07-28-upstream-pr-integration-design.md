# Datart Official Open PR Integration Design

## 1. Background

The maintained repository is `https://github.com/xewn/datart`. Its `master`
branch originally matched `running-elephant/datart@1af9c5d3`. The official
repository has stopped active maintenance but still has 33 open pull requests,
created between 2022 and 2025.

The official PRs are not a uniform patch set:

- They target both `master` and `dev`.
- Several PRs modify the same code and supersede one another.
- Some PRs are clean according to GitHub but contain functional defects,
  environment-specific commands, generated databases, or unrelated lock-file
  churn.
- Two PRs are already conflicted, and one PR contains no file changes.
- Most backend changes have no dedicated unit tests in the upstream repository.

This design treats every open PR as an input that must receive an explicit,
auditable disposition. It does not equate GitHub's mergeability flag with
production readiness.

## 2. Goals

1. Account for all 33 official open PRs without silently dropping any of them.
2. Preserve original authorship and upstream PR provenance for adopted work.
3. Integrate accepted behavior into the maintained repository in independently
   testable batches.
4. Correct defects in upstream PR implementations before adoption.
5. Keep `master` usable after every merged batch and provide straightforward
   rollback through isolated commits.
6. Produce a durable PR disposition ledger for future maintainers.

## 3. Non-Goals

- Do not close, modify, or comment on PRs in the official repository.
- Do not apply a general dependency upgrade as part of PR adoption.
- Do not automatically run `npm audit fix` or `npm audit fix --force`.
- Do not preserve faulty code merely to keep an upstream commit byte-for-byte
  identical.
- Do not merge temporary configuration, local credentials, generated H2
  databases, or unrelated package-lock churn from contributor branches.

## 4. Integration Workspace and Branching

All integration work occurs in the linked worktree:

`D:\BI\datart\.worktrees\upstream-pr-integration`

The integration branch is:

`integration/upstream-prs`

The main checkout remains at `D:\BI\datart`. The root `.gitignore` contains
`/.worktrees/`, committed as `144afc01`, so linked worktree contents cannot be
accidentally staged.

The official repository will be configured as remote `upstream`:

`https://github.com/running-elephant/datart.git`

Before adopting open PRs, `upstream/dev` will be merged into the integration
branch with a merge commit. At the audit snapshot, the net `dev` difference
from official `master` is one dashboard null-safety change and one lock-file
integrity update. Merging the branch history gives PRs that target `dev` their
intended base while retaining later `master` commits.

No PR integration commit is made directly in the main checkout. The completed
integration branch is merged into local `master` only after all acceptance
gates pass. The resulting `master` is then pushed to `origin`.

## 5. Disposition Model

Each PR receives exactly one primary disposition:

| Disposition | Meaning |
| --- | --- |
| `direct` | Preserve the upstream author and cherry-pick the focused change, adding tests where absent. |
| `adapted` | Preserve the upstream behavior and provenance, but rewrite unsafe, incomplete, or conflicting details. |
| `absorbed` | Do not apply a duplicate patch; cover its behavior and provenance in another adopted implementation. |
| `rejected` | Do not add the behavior because the PR is empty, platform-breaking, or contradicts valid existing behavior. Record evidence in the ledger. |

Direct cherry-picks retain the original Git author. Adapted commits include
these trailers:

```text
Upstream-PR: running-elephant/datart#<number>
Upstream-Commit: <source-sha>
```

When an adapted implementation materially uses the contributor's code, the
original author is also retained with a valid `Co-authored-by` trailer obtained
from the source commit metadata.

The durable ledger will be written to:

`docs/upstream-prs/2026-07-28-disposition.md`

It will contain the PR number, title, source SHA, target branch, disposition,
integration commit, tests, and rationale.

## 6. PR-by-PR Decisions

| PR | Subject | Disposition | Integration requirement |
| --- | --- | --- | --- |
| #2368 | Reset removed join table state | direct | Add a component or reducer-level regression test proving stale table state is cleared. |
| #2366 | Dashboard jump date parameter | absorbed | Cover the optional first date value in the unified #2350 date-controller fix. |
| #2365 | Custom dashboard dialog sizing | adapted | Rebuild only the feature commits. Exclude the H2 database, local configs, package-lock churn, and temporary commits; fix `width`/`weight` inconsistencies and undefined default access. |
| #2360 | Druid connection recovery defaults | direct | Verify retry count and `breakAfterAcquireFailure=false` in a focused Java test. |
| #2359 | Skip archived sources in schema sync | direct | Test absent, active, and archived source paths, including job deletion behavior. |
| #2356 | Refresh cached JDBC provider after source update | adapted | Compare `JdbcProperties`, refresh atomically, close the replaced provider once, and cover unchanged/changed/concurrent paths. |
| #2354 | Unknown JDBC type fallback | adapted | Preserve the StarRocks null-safety behavior but test known and unknown JDBC families explicitly. |
| #2350 | Date controller and listener leak fixes | adapted | Split date-range semantics and stable event-handler references into separate commits and tests. |
| #2346 | Avoid redundant chart resize on loading changes | direct | Add lifecycle coverage proving resize is not republished solely by loading state changes. |
| #2330 | URL date range controller fix | absorbed | Its two-line behavior is included in the unified #2350 date implementation. |
| #2313 | Guard missing BasicTable row | direct | Add a regression test for an absent row and retain the normal row rendering path. |
| #2309 | URL date controller fix | absorbed | Superseded by the later null-safe date implementation from #2350/#2366. |
| #2305 | Add OpenSSL option with Unix `export` | rejected | The command breaks Windows and is unnecessary for the pinned Node 16 integration runtime. |
| #2287 | Custom table and pivot sorting | adapted | Integrate as a bounded feature with request-builder, SQL-builder, migration, and UI tests. Support only the documented single non-numeric field case. |
| #2283 | Column permission fixes | adapted | Reimplement because upstream review requested changes. Escape column names safely, avoid sentinel SQL strings, and test empty, exact, substring, alias, and aggregate permissions. |
| #2277 | `COUNT DISTINCT` ordering | direct | Add SQL rendering coverage that distinguishes `COUNT` from `COUNT(DISTINCT ...)`. |
| #2263 | Trim CSV values before formatting | direct | Test surrounding whitespace, quoted content, empty cells, and headers. |
| #2261 | Show the added Story page | direct | Add state and modal behavior coverage, including localization keys. |
| #2236 | Docker Compose and build layout | adapted | Use maintained image references and preserve current assembly semantics. Validate backend-only and full packaging profiles plus Compose configuration parsing. |
| #2203 | Dashboard custom button widget | adapted | Add URL, dashboard, and chart navigation tests, migration defaults, serialization coverage, and keyboard-accessible button behavior. |
| #2198 | Shared chart filter/paging event refresh | adapted | Replace mount-only registration with a stable effect and cleanup; test chart replacement and repeated renders without duplicate listeners. |
| #2196 | Restore date-level computed fields | direct | Test backend chart input with and without config and verify existing API-fetched chart conversion is unchanged. |
| #2192 | BigQuery aggregate aliases and adapter | adapted | Keep alias changes dialect-aware rather than changing aliases globally. Add BigQuery adapter metadata and query alias tests. |
| #2189 | Batch query variables by view IDs | adapted | Guard the empty set to avoid `IN ()`, retain deterministic results, and compare query count with the previous loop. |
| #2172 | Empty merge PR | rejected | The PR changes zero files and has no behavior to adopt. |
| #2170 | Variables passed through SQL functions | adapted | Replace the over-broad regex change with narrowly tested parsing for functions, comparisons, `IN`, and malformed expressions. |
| #2165 | MongoDB document data provider | adapted | Treat as a provider feature: validate configuration, resource lifecycle, query/aggregation parsing, service loading, i18n, and module packaging before enabling it. |
| #2131 | Duplicate Excel sheet names | adapted | Use safe, length-limited, case-insensitive unique names and test pre-existing numeric suffixes and invalid Excel characters. |
| #2089 | Previous month/year time ranges | direct | Resolve the stale conflict and add fixed-clock tests for month/year boundaries and leap years. |
| #2033 | Pivot table diagonal header | adapted | Add persisted style configuration, rendering coverage, and a visual regression check for enabled and disabled states. |
| #2016 | OAuth2 client SPI | adapted | Perform a security-focused rewrite with provider discovery, duplicate-client handling, WeChat/DingTalk compatibility, and authentication failure tests. |
| #1969 | Period-over-period calculation | adapted | Rebuild from behavior because the PR conflicts and discussion identifies a potentially missing file. Cover date-present, date-selected, missing baseline, zero denominator, and table/pivot rendering. |
| #1816 | Disallow negative grid values | rejected | Negative values are currently meaningful and the PR provides no product rule justifying removal. |

All 33 open PRs are represented exactly once in this table: 10 are `direct`,
17 are `adapted`, 3 are `absorbed`, and 3 are `rejected`.

## 7. Delivery Batches

### Batch 0: Provenance and Official Branch Base

- Add the `upstream` remote.
- Fetch official `master`, `dev`, and the 33 PR head refs.
- Merge `upstream/dev` into `integration/upstream-prs`.
- Create the initial disposition ledger with immutable source metadata.
- Re-run the frontend and backend baselines before accepting PR code.

### Batch 1: Backend Correctness and Data Safety

PRs: `#2360`, `#2359`, `#2356`, `#2354`, `#2283`, `#2277`, `#2263`,
`#2189`, `#2170`, and `#2131`.

The column-permission fix is prioritized because incorrect behavior can expose
fields to a role that has no selected column permission. JDBC cache refresh is
kept separate from Druid retry settings so either behavior can be reverted
without affecting the other.

### Batch 2: Frontend Bug Fixes

PRs: `#2368`, `#2350`, `#2346`, `#2313`, `#2198`, `#2196`, and `#2089`.

Absorbed PRs `#2366`, `#2330`, and `#2309` are recorded against the #2350
integration commits. Each bug receives a failing regression test before its
implementation is applied.

### Batch 3: Contained User-Facing Features

PRs: `#2261` and `#2033`.

These features are localized to Story and Pivot Table surfaces and can be
verified without changing the query model.

### Batch 4: Cross-Layer Analytics and Dashboard Features

PRs: `#2287`, `#2203`, `#2192`, and `#2365`.

These changes cross configuration types, persistence, request generation, and
rendering. Each is delivered as a separate commit series with migrations for
existing serialized chart/dashboard data where required.

### Batch 5: Platform, Provider, and Security Extensions

PRs: `#2236`, `#2165`, `#2016`, and `#1969`.

These are the largest or highest-risk extensions. Docker/build changes,
MongoDB, OAuth2 SPI, and period-over-period calculation are not combined in a
single commit. Each must pass its own module tests and the full repository gate.

### Final Disposition Batch

Rejected PRs `#2305`, `#2172`, and `#1816` receive ledger entries with evidence
but no product-code commit.

## 8. Testing and Verification

### Frontend Runtime

Use Node `16.20.2` and npm `8.19.4`, matching the lock-file generation era and
the package engine range. The current machine's Node 24 runtime is not used for
acceptance.

The linked worktree exposes a Windows path-normalization issue in the existing
absolute Jest `testMatch`. Until the test configuration is normalized, the
baseline command uses a relative override:

```powershell
npx --yes -p node@16.20.2 -p npm@8.19.4 npm run test -- --watchAll=false --runInBand --silent '--testMatch=**/src/**/__tests__/**/*.{spec,test}.{js,jsx,ts,tsx}'
```

Recorded baseline on 2026-07-28:

- 83 test suites passed.
- 651 tests passed, 5 skipped, 656 total.
- 4 snapshots passed.
- `npm run checkTs` passed.

Every frontend commit must pass its targeted regression test. Every completed
batch must pass:

```powershell
npm run checkTs
npm run test -- --watchAll=false --runInBand --silent '--testMatch=**/src/**/__tests__/**/*.{spec,test}.{js,jsx,ts,tsx}'
npm run build
```

### Backend Runtime

The repository targets Java 8 source compatibility. The machine has Java 17
but no Maven executable. Before backend PR implementation, use a task-local
Apache Maven 3.9.11 distribution so no system-wide installation is required.

Establish and record the unmodified backend baseline with:

```powershell
mvn test
mvn -DskipTests package
```

If either unmodified baseline command fails, stop backend integration and
separate environment failures from repository failures before continuing.

Every backend commit must run the narrow module test that proves its behavior.
Every completed backend batch must run the full `mvn test` gate. Packaging,
provider service loading, and assembly-related batches must also run
`mvn -DskipTests package`.

### UI and Workflow Verification

For user-facing features, automated tests are supplemented with focused local
runtime checks:

- Story page addition and selection.
- Dashboard button URL/dashboard/chart navigation.
- Dashboard drill-through and view-detail dialog sizing.
- Table and pivot custom sorting.
- Pivot diagonal header rendering.
- Period-over-period calculation with and without a date dimension.

Screenshots are captured for visual features at desktop and narrow viewport
sizes. No visual feature is accepted solely because it compiles.

### Security and Data-Access Verification

Column-permission and OAuth2 changes require negative-path tests. A test must
demonstrate that unauthorized columns remain inaccessible and that unknown or
duplicate OAuth2 clients fail closed. These changes are not accepted with only
happy-path coverage.

## 9. Error Handling and Conflict Policy

- Fetch failures do not mutate product code; retry the individual ref and
  record unavailable contributor branches in the ledger.
- A conflicting direct cherry-pick is aborted. The behavior is then applied as
  an adapted commit with explicit provenance rather than leaving conflict
  markers or a partial index.
- Test failures stop the current PR unit. Later PRs are not layered on a red
  batch.
- Generated files and environment-specific changes are removed before tests,
  never after a merge to `master`.
- A behavior that cannot be verified with an automated or reproducible manual
  check remains unmerged and blocks final acceptance. It is never silently
  accepted or treated as completion.

## 10. Commit and Rollback Strategy

Each direct or adapted PR behavior is isolated in one or more conventional
commits. Tests and the implementation they prove are committed together unless
the original upstream commit can be cherry-picked unchanged, in which case the
regression test follows immediately.

Batch merge commits provide rollback boundaries. Reverting one batch must not
remove unrelated accepted behavior. Large features retain separate migration,
backend, frontend, and test commits when that separation improves rollback.

No history rewrite is performed after the integration branch is published.

## 11. Baseline Risks Kept Separate From PR Adoption

The dependency install reports 226 existing npm vulnerabilities: 13 low, 118
moderate, 74 high, and 21 critical. This design records that debt but does not
change dependencies automatically because forced audit fixes can alter React,
build, and chart behavior independently of the upstream PR goal.

The repository's `.nvmrc` points to an obsolete Node LTS line while
`package.json` supports newer versions. Acceptance therefore uses the explicit
Node 16/npm 8 pair above. Runtime modernization and dependency remediation are
follow-up maintenance projects after PR integration.

## 12. Acceptance Criteria

The integration is complete only when all of the following are true:

1. `upstream` is configured and the official `dev` base is represented in the
   integration history.
2. The disposition ledger contains all 33 PRs and every accepted PR links to
   its integration commit and verification evidence.
3. Direct and adapted PR behaviors listed in this design are implemented; all
   absorbed and rejected PRs have explicit rationale.
4. Frontend type checking, the full Jest suite, and the production build pass
   on the final integration branch.
5. Backend tests and packaging pass on the final integration branch.
6. Security-sensitive negative-path tests pass.
7. Required UI workflow checks are completed without layout or interaction
   regressions.
8. The integration worktree and main checkout are clean, with no conflict
   markers or untracked generated artifacts.
9. `integration/upstream-prs` is merged into `master`, and the resulting
   `master` is pushed to `origin`.
