# Upstream PR Batch 0 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Establish the official branch base, immutable source refs, complete 33-PR disposition ledger, and reproducible frontend/backend baselines before product-code integration.

**Architecture:** Keep all work on `integration/upstream-prs` in the approved linked worktree. Configure `upstream`, fetch every audited PR into a stable remote-tracking ref, merge official `dev`, then record source metadata and baseline evidence in a repository document. No open-PR product behavior is introduced in this batch.

**Tech Stack:** Git, PowerShell, Markdown, Node 16.20.2, npm 8.19.4, Jest/CRACO, TypeScript 4.5, Azul Zulu OpenJDK 8u492-b09, Apache Maven 3.9.16.

---

### Task 1: Configure Official Remote and Fetch Immutable Refs

**Files:**
- No repository files changed.

- [x] **Step 1: Verify the isolated branch is clean**

Run:

```powershell
git status --short --branch
git branch --show-current
```

Expected: clean status on `integration/upstream-prs`.

- [x] **Step 2: Add and validate the official remote**

Run:

```powershell
git remote add upstream https://github.com/running-elephant/datart.git
git remote get-url upstream
```

Expected: `https://github.com/running-elephant/datart.git`.

- [x] **Step 3: Fetch official branches and all audited PR heads**

Run:

```powershell
git fetch upstream master dev
$prs = 2368,2366,2365,2360,2359,2356,2354,2350,2346,2330,2313,2309,2305,2287,2283,2277,2263,2261,2236,2203,2198,2196,2192,2189,2172,2170,2165,2131,2089,2033,2016,1969,1816
foreach ($pr in $prs) {
  git fetch upstream "+refs/pull/$pr/head:refs/remotes/upstream/pr/$pr"
  if ($LASTEXITCODE -ne 0) { throw "Failed to fetch PR #$pr" }
}
```

Expected: all 33 refs exist under `refs/remotes/upstream/pr/`.

- [x] **Step 4: Verify count and audited head SHAs**

Run:

```powershell
$refs = git for-each-ref --format='%(refname:short) %(objectname)' refs/remotes/upstream/pr
if ($refs.Count -ne 33) { throw "Expected 33 PR refs, found $($refs.Count)" }
$refs
```

Expected: exactly 33 refs. The head SHAs must match the source metadata written in Task 3.

### Task 2: Merge the Official `dev` Base

**Files:**
- Modify through merge: `frontend/package-lock.json`
- Modify through merge: `frontend/src/app/pages/DashBoardPage/actions/widgetAction.ts`

- [x] **Step 1: Verify official branch relationship**

Run:

```powershell
git log --left-right --cherry-pick --oneline upstream/master...upstream/dev
git diff --stat upstream/master...upstream/dev
```

Expected: both tips contain the dashboard null-safety change; `dev` contributes its branch history and lock-file integrity update while `master` retains later security documentation.

- [x] **Step 2: Merge `upstream/dev` without flattening history**

Run:

```powershell
git merge --no-ff upstream/dev -m "merge: sync official dev baseline"
```

Expected: a merge commit with no unresolved conflicts.

- [x] **Step 3: Verify the functional delta**

Run:

```powershell
git diff HEAD^1..HEAD -- frontend/src/app/pages/DashBoardPage/actions/widgetAction.ts frontend/package-lock.json
git diff --check HEAD^1..HEAD
```

Expected: optional chaining remains present in `dataChartMap[boardId]?.[datachartId]`; the merge's file delta is limited to the audited lock-file integrity update.

### Task 3: Create the Complete Disposition Ledger

**Files:**
- Create: `docs/upstream-prs/2026-07-28-disposition.md`

- [x] **Step 1: Create the ledger header and all 33 source rows**

Create a Markdown document with this schema:

```markdown
# Official Open PR Disposition Ledger

Snapshot: 2026-07-28
Official repository: https://github.com/running-elephant/datart
Integration branch: integration/upstream-prs

| PR | Source SHA | Base | Disposition | Integration commit | Verification | Rationale |
| --- | --- | --- | --- | --- | --- | --- |
```

Populate one row for each PR in this exact order:

```text
2368 2366 2365 2360 2359 2356 2354 2350 2346 2330 2313
2309 2305 2287 2283 2277 2263 2261 2236 2203 2198 2196
2192 2189 2172 2170 2165 2131 2089 2033 2016 1969 1816
```

Use this exact base/disposition/batch mapping:

```text
PR    Base    Disposition  Owner or delivery batch
2368  master  direct       Batch 2
2366  dev     absorbed     #2350
2365  dev     adapted      Batch 4
2360  dev     direct       Batch 1
2359  dev     direct       Batch 1
2356  dev     adapted      Batch 1
2354  master  adapted      Batch 1
2350  master  adapted      Batch 2
2346  dev     direct       Batch 2
2330  master  absorbed     #2350
2313  dev     direct       Batch 2
2309  dev     absorbed     #2350
2305  master  rejected     Unix-only export breaks Windows
2287  master  adapted      Batch 4
2283  master  adapted      Batch 1
2277  dev     direct       Batch 1
2263  dev     direct       Batch 1
2261  dev     direct       Batch 3
2236  dev     adapted      Batch 5
2203  dev     adapted      Batch 4
2198  master  adapted      Batch 2
2196  master  direct       Batch 2
2192  master  adapted      Batch 4
2189  dev     adapted      Batch 1
2172  master  rejected     Empty PR with zero changed files
2170  dev     adapted      Batch 1
2165  master  adapted      Batch 5
2131  dev     adapted      Batch 1
2089  master  direct       Batch 2
2033  dev     adapted      Batch 3
2016  dev     adapted      Batch 5
1969  dev     adapted      Batch 5
1816  dev     rejected     Negative grid values are valid behavior
```

Use the actual fetched ref SHA for `Source SHA`. For accepted PRs, set `Integration commit` and `Verification` to `pending Batch N` with the mapped batch number. For absorbed PRs, name the owning PR. For rejected PRs, use `not applicable` and the rationale above.

- [x] **Step 2: Verify ledger completeness and source integrity**

Run:

```powershell
$path = 'docs/upstream-prs/2026-07-28-disposition.md'
$rows = Get-Content $path | Where-Object { $_ -match '^\| #[0-9]+ \|' }
if ($rows.Count -ne 33) { throw "Expected 33 ledger rows, found $($rows.Count)" }
$numbers = $rows | ForEach-Object { [regex]::Match($_, '#[0-9]+').Value }
if (($numbers | Sort-Object -Unique).Count -ne 33) { throw 'Duplicate PR number in ledger' }
foreach ($row in $rows) {
  $number = [regex]::Match($row, '#([0-9]+)').Groups[1].Value
  $sha = (git rev-parse "refs/remotes/upstream/pr/$number").Trim()
  if ($row -notmatch [regex]::Escape($sha)) { throw "Ledger SHA mismatch for PR #$number" }
}
```

Expected: 33 unique rows and every source SHA matches its fetched ref.

### Task 4: Establish Reproducible Frontend Baseline

**Files:**
- Modify: `docs/upstream-prs/2026-07-28-disposition.md`

- [x] **Step 1: Install locked dependencies with the pinned runtime**

Run from `frontend`:

```powershell
npx --yes -p node@16.20.2 -p npm@8.19.4 npm ci --legacy-peer-deps
```

Expected: install succeeds without modifying `package.json` or `package-lock.json`.

- [x] **Step 2: Run TypeScript checking**

Run:

```powershell
npx --yes -p node@16.20.2 -p npm@8.19.4 npm run checkTs
```

Expected: exit code 0.

- [x] **Step 3: Run the complete Jest baseline**

Run:

```powershell
npx --yes -p node@16.20.2 -p npm@8.19.4 npm run test -- --watchAll=false --runInBand --silent '--testMatch=**/src/**/__tests__/**/*.{spec,test}.{js,jsx,ts,tsx}'
```

Expected: 83 suites pass, 651 tests pass, 5 tests skip, and 4 snapshots pass.

- [x] **Step 4: Run the production build**

Run:

```powershell
npx --yes -p node@16.20.2 -p npm@8.19.4 npm run build
```

Expected: exit code 0 and no tracked build artifacts.

- [x] **Step 5: Record exact frontend evidence in the ledger**

Add a `Batch 0 Baseline` section containing the runtime versions, commands, pass counts, build result, and the existing npm audit totals: 13 low, 118 moderate, 74 high, and 21 critical vulnerabilities.

### Task 5: Establish Reproducible Backend Baseline

**Files:**
- Modify: `docs/upstream-prs/2026-07-28-disposition.md`
- No Maven binary is stored in the repository.

- [x] **Step 1: Install task-local Java 8 and Maven 3.9.16**

Run:

```powershell
$mavenVersion = '3.9.16'
$mavenRoot = Join-Path $env:TEMP "codex-datart-maven-$mavenVersion"
$mavenZip = Join-Path $mavenRoot "apache-maven-$mavenVersion-bin.zip"
$mavenUrl = "https://downloads.apache.org/maven/maven-3/$mavenVersion/binaries/apache-maven-$mavenVersion-bin.zip"
$checksumUrl = "$mavenUrl.sha512"
New-Item -ItemType Directory -Path $mavenRoot -Force | Out-Null
Invoke-WebRequest -UseBasicParsing -Uri $mavenUrl -OutFile $mavenZip
$expected = ((Invoke-WebRequest -UseBasicParsing -Uri $checksumUrl).Content.Trim() -split '\s+')[0].ToLowerInvariant()
$actual = (Get-FileHash -Algorithm SHA512 -LiteralPath $mavenZip).Hash.ToLowerInvariant()
if ($actual -ne $expected) { throw "Maven SHA-512 mismatch: expected $expected, got $actual" }
Expand-Archive -LiteralPath $mavenZip -DestinationPath $mavenRoot -Force
$env:Path = "$(Join-Path $mavenRoot "apache-maven-$mavenVersion\bin");$env:Path"
mvn --version
```

Use task-local Azul Zulu OpenJDK 8u492-b09 for this repository and expose Node
16.20.2/npm 8.19.4 on `PATH` for Maven's embedded frontend build.

Expected:

```text
Apache Maven 3.9.16
Java version: 1.8.0_492
```

- [x] **Step 2: Run all backend tests**

Run from the repository root:

```powershell
mvn test
```

Expected: `BUILD SUCCESS` with zero test failures.

- [x] **Step 3: Run backend packaging**

Run:

```powershell
mvn -DskipTests package
```

Expected: `BUILD SUCCESS` and no tracked artifacts.

- [x] **Step 4: Record exact backend evidence in the ledger**

Add Maven/Java versions, test summary, packaging result, and any baseline warnings under `Batch 0 Baseline`. If a baseline command fails, record the exact failure and resolve the environment or repository issue before adopting backend PR code.

### Task 6: Verify and Commit Batch 0 Evidence

**Files:**
- Create: `docs/upstream-prs/2026-07-28-disposition.md`

- [x] **Step 1: Verify repository hygiene**

Run:

```powershell
git diff --check
git status --short
rg -n 'TBD|TODO|FIXME|pending Batch 0' docs/upstream-prs/2026-07-28-disposition.md
```

Expected: no whitespace errors, no unexpected tracked files, and no unresolved Batch 0 placeholders.

- [x] **Step 2: Commit the ledger**

Run:

```powershell
git add docs/upstream-prs/2026-07-28-disposition.md
git commit -m "docs: record official PR disposition baseline"
```

Expected: one documentation commit after the official `dev` merge commit.

- [x] **Step 3: Verify Batch 0 final state**

Run:

```powershell
git status --short --branch
git log -5 --oneline --decorate
git for-each-ref --format='%(refname:short)' refs/remotes/upstream/pr | Measure-Object
```

Expected: clean `integration/upstream-prs`, committed baseline evidence, and exactly 33 fetched PR refs.
