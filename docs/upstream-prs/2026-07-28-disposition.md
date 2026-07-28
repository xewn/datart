# Official Open PR Disposition Ledger

Snapshot: 2026-07-28

Official repository: https://github.com/running-elephant/datart

Official master: `1af9c5d3ea46db2366d9daedc76e4d97881738b7`

Official dev: `1e6be99c9c327c1de73309a34e70e20e5b2c2695`

Integration branch: `integration/upstream-prs`

Official dev merge: `9e4d451630278cb4e4a443797ae6bcc829cfa4db`

The official `dev` merge preserves its history and lock-file integrity update.
Both official branch tips already contain the dashboard optional-chaining fix;
later `master` changes, including `SECURITY.md`, remain present after the merge.

| PR | Title | Source SHA | Base | Disposition | Integration commit | Verification | Rationale |
| --- | --- | --- | --- | --- | --- | --- | --- |
| #2368 | Reset removed join table state | `6b3f35d11dbe443a84220ef4b9397b08502e5ff0` | master | direct | `7d9b9b8e`, test `d03a2ca9` | focused component tests; full frontend/Maven gates passed | Focused stale-state fix. |
| #2366 | Dashboard jump date parameter | `7f17c3ff2fd25caa6cd06052c95d594058587d72` | dev | absorbed | `a25e4e53` via #2350 | focused date tests; full frontend/Maven gates passed | Duplicate of the later null-safe date-controller implementation. |
| #2365 | Custom dashboard dialog sizing | `98cd750a9dd22e83814b0cd724d0f4e768fbb8f1` | dev | adapted | pending Batch 4 | pending Batch 4 | Feature is useful, but the branch contains temporary commits, generated H2 data, local config, lock churn, and default-value defects. |
| #2360 | Druid connection recovery defaults | `fe0e8c8e5b3b7a30fa03aeadd5a757665d3d825e` | dev | direct | `70552639`, test `c7887cc6` | focused regression; full test/package passed | Focused recovery behavior with testable configuration. |
| #2359 | Skip archived sources in schema sync | `ce8fb90c7825b0dfcf278d412d01e114ca833648` | dev | direct | `c91458d5`, test `69920ba0` | focused regression; full test/package passed | Prevents unnecessary jobs for archived sources. |
| #2356 | Refresh cached JDBC provider after source update | `7244f9317853652963b157858af2cfe27d69fcfc` | dev | adapted | `6015cae3` | focused concurrency tests; full test/package passed | Required for multi-node consistency, with atomic replacement and lifecycle coverage added. |
| #2354 | Unknown JDBC type fallback | `c3caeec8db318691f297076a502a7c2c6aa6fd9d` | master | adapted | `1cec6735` | focused type-family test; full test/package passed | Preserve null safety while testing known and unknown families. |
| #2350 | Date controller and listener leak fixes | `e910efdab3710a3946ed880c244d8b9ca0d4ffbf` | master | adapted | `a25e4e53`, `4173cc3f` | focused date/listener tests; full frontend/Maven gates passed | Split unrelated date and stable-listener fixes into independently verified commits. |
| #2346 | Avoid redundant chart resize on loading changes | `6af405c69f8777de1307b57cb3a1e87535f15840` | dev | direct | `d6a1243e`, test `db443024` | focused lifecycle test; full frontend/Maven gates passed | Focused lifecycle dependency correction. |
| #2330 | URL date range controller fix | `9875ce2507b5ef6ee844f78040c7166239d7578a` | master | absorbed | `a25e4e53` via #2350 | focused date tests; full frontend/Maven gates passed | Behavior is identical to the date portion of #2350. |
| #2313 | Guard missing BasicTable row | `f7dbe0f7dc7c794119597f0af549bd4a37186018` | dev | direct | `bd5b9026`, test `a1a57c39` | focused table tests; full frontend/Maven gates passed | Prevents a missing row from entering the render loop. |
| #2309 | URL date controller fix | `c7659846d8e229435da5db1d360f60851cd54847` | dev | absorbed | `a25e4e53` via #2350 | focused date tests; full frontend/Maven gates passed | Superseded by the later optional-access date fix. |
| #2305 | Add OpenSSL option with Unix export | `f393b298294e4a4a4d97542787db9a93c38c406a` | master | rejected | not applicable | source review | Unix-only `export` breaks Windows and is unnecessary for the pinned Node 16 runtime. |
| #2287 | Custom table and pivot sorting | `337b3ac26d255e187a252bb41c11902bb228a014` | master | adapted | pending Batch 4 | pending Batch 4 | Cross-layer feature needs request, SQL, migration, and UI coverage. |
| #2283 | Column permission fixes | `fe6bfbc51865fabd44e36f41525259b0bb35d21a` | master | adapted | `74a1f1b4` | focused permission tests; full test/package passed | Upstream review requested changes; permission matching must avoid unsafe sentinels and regex injection. |
| #2277 | COUNT DISTINCT ordering | `145c0557d9523fa64d1f68fff0ba4f7d92e3ae34` | dev | direct | `e221d36d`, test `0e8fd5de` | focused SQL test; full test/package passed | Focused SQL rendering correction. |
| #2263 | Trim CSV values before formatting | `35c5d727ce61aac050489bd58d7e59edebbc17e3` | dev | direct | `47636d41`, test `798eab89` | focused parser tests; full test/package passed | Focused parser behavior with whitespace regression tests. |
| #2261 | Show the added Story page | `0faca75ea799bebf8c239fce5e35c7e8a28aa88d` | dev | direct | pending Batch 3 | pending Batch 3 | Contained Story UI behavior. |
| #2236 | Docker Compose and build layout | `6064f28d687a858a2f394bf48bca4fadb798ba03` | dev | adapted | pending Batch 5 | pending Batch 5 | Build and image references require current validation without changing assembly semantics. |
| #2203 | Dashboard custom button widget | `bce86d2495848058c4a98840114700b7ecd6505c` | dev | adapted | pending Batch 4 | pending Batch 4 | Feature needs migration, serialization, navigation, and accessibility coverage. |
| #2198 | Shared chart event refresh | `60815c73df40a9722e3386cb9010a10ccd1bf476` | master | adapted | `eeda2414` | focused hook lifecycle test; full frontend/Maven gates passed | Mount-only replacement needs stable registration and cleanup. |
| #2196 | Restore date-level computed fields | `f90068338676e201939cc410dac817deff80dfa1` | master | direct | `c3252b54`, test `03c62689` | focused thunk tests; full frontend/Maven gates passed | Focused backend-chart conversion behavior. |
| #2192 | BigQuery aggregate aliases and adapter | `b381d4b564718b14680c05dc14953551e88fe5c1` | master | adapted | pending Batch 4 | pending Batch 4 | Global alias changes must become dialect-aware. |
| #2189 | Batch query variables by view IDs | `bd94783c613c27ef6b3eb4a7ca2d04b6abcb71f0` | dev | adapted | `f045e093` | focused service tests; full test/package passed | Batch query needs an empty-set guard and deterministic coverage. |
| #2172 | Empty merge PR | `f622bdb5794c6f2ba92386dfceaeaa469af4f2a2` | master | rejected | not applicable | source review | PR changes zero files and has no behavior to adopt. |
| #2170 | Variables passed through SQL functions | `da5338edcc18bacb66a8a0daf88db7702650d439` | dev | adapted | `2ff47187` | focused resolver tests; full test/package passed | Upstream regex is over-broad and needs narrow parser tests. |
| #2165 | MongoDB document data provider | `b0b01ac83cd47ccd710ed51e2eb7c90710812588` | master | adapted | pending Batch 5 | pending Batch 5 | Provider requires lifecycle, parsing, loading, packaging, and configuration validation. |
| #2131 | Duplicate Excel sheet names | `dc26f7be11bc4b624195507596227c3006645d83` | dev | adapted | `f815b142` | focused workbook test; full test/package passed | Upstream suffix logic can create secondary collisions and invalid Excel names. |
| #2089 | Previous month and year time ranges | `95023762062e02c4687bab9bdf8f446e1f15412d` | master | direct | `c792b230`, test `7e62eac1` | focused fixed-clock tests; full frontend/Maven gates passed | Behavior is focused but needs stale-conflict resolution and fixed-clock tests. |
| #2033 | Pivot table diagonal header | `9a1a9f9f8876656af6be2c7c1b538262a4abe160` | dev | adapted | pending Batch 3 | pending Batch 3 | Contained visual feature requires persistence and visual verification. |
| #2016 | OAuth2 client SPI | `a4776fc9c6226ad5e74af7606a076de355be0375` | dev | adapted | pending Batch 5 | pending Batch 5 | Security-sensitive extension requires fail-closed discovery and authentication tests. |
| #1969 | Period-over-period calculation | `4a197070628b8e472e39c98ff5784f0a2468aaac` | dev | adapted | pending Batch 5 | pending Batch 5 | PR conflicts and discussion identifies a potentially missing file. |
| #1816 | Disallow negative grid values | `901a8c42cfffb0f433217d4fdf8eb07a49d41c42` | dev | rejected | not applicable | source review | Negative grid values are valid existing behavior and no product rule justifies removal. |

Disposition totals: 10 direct, 17 adapted, 3 absorbed, and 3 rejected.

## Batch 2 Frontend Bug Fixes

- Integrated 7 actionable PRs as 13 implementation/regression commits and mapped 3 duplicate date-controller PRs to the unified #2350 date commit.
- Focused gate: 8 Jest suites and 44 tests passed, with zero failures, zero errors, and zero skips.
- Frontend type gate: `npm run checkTs` exited 0 under Node 16.20.2 and npm 8.19.4.
- Full Jest gate: 88 suites passed; 668 tests passed, 5 skipped, 673 total; 4 snapshots passed.
- Frontend production gate: `npm run build:all` exited 0, covering theme extraction, the Rollup task bundle, and the React production build.
- Full repository test gate: `mvn test` exited 0 under Java 8 and Node 16. All 9 Reactor modules succeeded; 31 backend tests ran, with 30 passed, 1 existing skip, zero failures, and zero errors.
- Package gate: offline `mvn -DskipTests package` exited 0 and all 9 Reactor modules succeeded.
- Install artifact: `datart-server-1.0.0-rc.3-install.zip` is 149,968,344 bytes with SHA-256 `2E7A54D1F2FE98D55C57F9AC6ABC0F8196540F64FC477E3C0384941B2087FF55`.
- Repository hygiene: generated `frontend/package-lock.json` drift was restored, build outputs remain ignored, and the tracked worktree was clean before evidence updates.

## Batch 1 Backend Correctness and Data

- Integrated 10 PRs as 14 implementation and regression-test commits while preserving direct-PR authorship and adapted-PR provenance trailers.
- Focused gate: 12 test classes, 24 tests passed, with zero failures, zero errors, and zero skips.
- Full test gate: `mvn test` exited 0 under Java 8 and Node 16. All 9 Reactor modules succeeded; 31 backend tests ran, with 30 passed, 1 existing skip, zero failures, and zero errors.
- Package gate: offline `mvn -DskipTests package` exited 0 after the Maven dependency cache was populated. All 9 Reactor modules succeeded.
- Install artifact: `datart-server-1.0.0-rc.3-install.zip` is 141,128,536 bytes with SHA-256 `A7E04DD4CA37AE7A8F676B6E5910F513A207D57D2EBEC0CCE0EE3C0FA7285AAE`.
- Repository hygiene: the Maven-generated `frontend/package-lock.json` delta was restored, build outputs remain ignored, and no unexpected tracked artifact remains.

## Batch 0 Baseline

### Frontend

- Runtime: Node 16.20.2 and npm 8.19.4, invoked through task-local `npx` packages.
- Locked install: `npm ci --legacy-peer-deps` succeeded with 2,636 packages added and 2,723 packages audited.
- Existing dependency audit: 13 low, 118 moderate, 74 high, and 21 critical vulnerabilities; no automatic audit fix was applied.
- TypeScript: `npm run checkTs` passed.
- Jest: 83 suites passed; 651 tests passed, 5 skipped, 656 total; 4 snapshots passed.
- Production build: passed; Webpack compiled successfully in 7.25 minutes.
- Existing warnings: bundle size exceeds the React Scripts recommendation, `caniuse-lite` is outdated, and the legacy npm registry key emits a compatibility warning.
- Repository hygiene: generated build and dependency directories remain ignored; no tracked frontend file changed during baseline verification.

### Backend

- Runtime: Apache Maven 3.9.16 with Azul Zulu OpenJDK 8u492-b09. Both distributions were task-local and their publisher checksums passed (Maven SHA-512 and JDK SHA-256).
- Environment diagnosis: the machine's Java 17 failed in Lombok 1.18.18 with a `jdk.compiler` module-access error. Opening the compiler modules exposed the next incompatibility, the removed `jdk.nashorn.api.scripting` package. No project source or POM was changed; Java 8 is required by this baseline.
- Focused verification: `mvn -pl core -am test` passed for `datart-parent` and `datart-core` under Java 8; `datart-core` currently has no tests.
- Full tests: `mvn test` passed all 9 Reactor modules. Seven tests ran: 6 passed and 1 skipped, with zero failures and zero errors.
- Packaging: `mvn -DskipTests package` passed all 9 Reactor modules and produced `datart-server-1.0.0-rc.3-install.zip` (141,124,701 bytes, SHA-256 `F2D99444F91586A3FEFB5BAF3F0A4E68ED24FE000DE929EEE08681712C3C8726`).
- Embedded frontend build: Maven was run with Node 16.20.2 and npm 8.19.4 on `PATH`. The system Node 24 runtime is incompatible with the repository's Webpack/OpenSSL stack and fails with `ERR_OSSL_EVP_UNSUPPORTED`.
- Existing warnings: JavaCC grammar choice conflicts, deprecated or unchecked Java APIs, Rollup circular dependencies and rewritten `this`, TypeScript output-overwrite warnings, an unresolved optional UUID UMD import, and outdated `caniuse-lite` data.
- Repository hygiene: Maven build outputs remain ignored. Its generated `package-lock.json` delta was restored, leaving no tracked product file changed by baseline verification.
