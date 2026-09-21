# Current checkpoint — 2026-09-22 PR20 review follow-up

- **Task**: Align PR20 with latest main and the direct MySQL 8 policy; preserve other Parts except the explicitly requested PhotoMap connection setup.
- **Status**: Latest main aa46c70 merged (4e7537e). PhotoMap now uses the common LocalMySqlContextCustomizerFactory; fixtures/assertions unchanged.
- **Changed**: README explains full replacement of Testcontainers, personal TEST_DB_* settings/permissions and schema isolation; PhotoMap connection annotations/imports/container block removed; docs/tasks excluded from PR; this status and daily-log retained.
- **Contract**: No new production/API/migration changes. PR19 PostCreateIntegrationTest remains a follow-up in its owning PR. Part2 local DBML now includes V13's existing idx_ai_job_user_requested (user_id, requested_at); that ignored local DBML is not added wholesale to this PR.
- **Tests**: Fresh clean compileTestJava PASS; clean test 302/302 PASS, failures/errors/skipped 0. PhotoMap 14/14 PASS; previous seven MySQL integration suites 99/99 PASS. Source/build scan finds no remaining active Testcontainers usage or dependency. No skipped tests or weakened assertions.
- **Decision**: The user-specified C:/TravelBirdy/.worktrees/task9-post-community/docs/harness/02-backend-rules.md test-policy paragraph was updated locally; all other Harness files and existing changes there were preserved. This separate local document is not part of the PR repository.
- **Deferred**: PR19 conversion belongs to the subsequent PR. No merge of PR20 into main was performed.
- **Next**: Review PR20 with the updated README and actual MySQL test results.

---
# Current checkpoint — 2026-09-21 local MySQL conversion

- **Task**: Move all prior Part2 integration commits to chun9930, delete the previous branch, and run every integration test on local MySQL 8 without Docker/Testcontainers/H2.
- **Status**: Implementation and verification complete; all changes pushed to chun9930 and published as PR #20: https://github.com/TravelBirdy/BE_TravelBirdy/pull/20 (chun9930 -> main).
- **Changed**: Test commit `a8c5b19d0e0d534416d1fbb95df039aa5d6f5cf0` (`test: run integration suites on isolated local MySQL`), 17 files: seven integration test connection blocks; five support/safety-test classes; test spring.factories; build.gradle; README; .env.example; task packet. No test body/assertion weakened or skipped. Existing config/Flyway commits 3844fd5, db80f3d, 015ecea retained. Team main(PR15) and PR14 original history reused without new Readers.
- **Contract**: No production/API/authoritative source/DBML/migration changes in this conversion. DATETIME, error_message VARCHAR(500), retention_status VARCHAR(30) and V7/V13/V14 repairs preserved. The user's explicit local-MySQL policy supersedes earlier Testcontainers instructions for this task.
- **Tests**: clean compileJava PASS; clean compileTestJava PASS; Trip 20/20; AI 39/39; internal authentication 4/4; seven integration suites 99/99; safety regressions 7/7. Final unfiltered clean test: 288 total, 288 passed, 0 failures/errors/skipped. ApplicationContext test also uses the same isolated local DB. Every context runs real Flyway migrations V1/V2/V3/V7/V13/V14 and JPA validate.
- **Decision**: Class-specific Spring context cache key creates fresh travelbird_test_UUID database; test-only env TEST_DB_HOST/PORT/USERNAME/PASSWORD, no DB_URL fallback. Existing method rollback maintained. Whole context and pools close before owned schema deletion. Exact UUID-name guard and successful CREATE ownership required; creation/cleanup failure paths tested and independently reviewed.
- **Deferred**: The pre-existing src/mysqlIntegrationTest tree is not in the current Gradle source sets and was not modified. No remaining test failure or policy choice.
- **Next**: Review PR #20 through the normal team workflow; no merge was requested or performed.

## Actual MySQL results and development DB protection

| Suite | Passed | Failed / skipped |
| --- | ---: | ---: |
| PlaceControllerIntegrationTest | 4 | 0 / 0 |
| PlaceDomainIntegrationTest | 6 | 0 / 0 |
| SavedPlaceControllerIntegrationTest | 13 | 0 / 0 |
| TourApiPlaceImportServiceIntegrationTest | 7 | 0 / 0 |
| CommunityDomainIntegrationTest | 38 | 0 / 0 |
| PostDomainIntegrationTest | 9 | 0 / 0 |
| SavedRouteDomainIntegrationTest | 22 | 0 / 0 |

MySQL server: 8.0.36. Focused integration run created/dropped 7/7 schemas. Final run created/dropped 8/8 actual schemas including the application context suite. Information_schema confirmed zero remaining generated travelbird_test_UUID schemas. Development travelbird was inspected using a read-only JDBC transaction before/after; both snapshots contained 0 tables/0 rows and identical SHA-256 e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855. No development DB creation/deletion/migration/test writes occurred.

Removed all three dependencies: spring-boot-testcontainers, org.testcontainers:junit-jupiter, org.testcontainers:mysql. Source/build search finds no remaining specified Testcontainers usage or H2 dependency. All original test fixtures/assertions compare identically after removing container setup.

Remote codex/part2-main-integration-20260920 was deleted only after its complete ancestry was pushed to chun9930; its local branch was also deleted while files were preserved. Original divergent local chun9930 was retained as codex/chun9930-local-preserved-20260921. Current worktree: C:/final_TravelBirdy/part2/.worktrees/chun9930-local-mysql. Local evidence: C:/final_TravelBirdy/part2-validation-20260921/local-final-summary.json and local-reports-final/.
