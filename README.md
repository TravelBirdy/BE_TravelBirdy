BE_TravelBirdy
## 로컬 MySQL 통합테스트

Java 21과 로컬 MySQL 8.0.36을 사용합니다. Docker, Testcontainers, H2는 사용하지 않습니다.
현재 PowerShell 프로세스에 다음 환경변수를 설정한 뒤 실행합니다. 비밀번호를 명령 기록에 남기지 않도록 입력 프롬프트를 사용하세요.

```powershell
$env:TEST_DB_HOST = '127.0.0.1'
$env:TEST_DB_PORT = '3306'
$env:TEST_DB_USERNAME = Read-Host 'MySQL 테스트 사용자'
$credential = Read-Host 'MySQL 테스트 비밀번호' -AsSecureString
$env:TEST_DB_PASSWORD = [System.Net.NetworkCredential]::new('', $credential).Password
.\gradlew.bat clean test
```

테스트 계정에는 `travelbird_test_`로 시작하는 테스트 schema를 생성·삭제하고 테이블/인덱스/제약을 생성·조회·수정할 권한이 필요합니다. 실제 계정과 비밀번호는 커밋하지 않습니다. `.env`는 자동으로 읽지 않으며 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`로 대체하지 않습니다. 누락된 테스트 자격증명은 명확한 오류로 실패합니다.

공통 `src/test/resources/META-INF/spring.factories` 설정이 모든 `@SpringBootTest`에 적용됩니다. 클래스마다 무작위 `travelbird_test_<32자리 UUID>` schema를 생성하고, 실제 Flyway 전체 migration과 Hibernate `validate`를 실행합니다. 개발 DB `travelbird`는 테스트 접속 대상으로 받지 않습니다. 기존 테스트별 트랜잭션 롤백을 유지하고, 클래스 종료 시 Spring context와 pool을 종료한 뒤 직접 생성한 schema만 삭제합니다. 생성 실패 시 기존 DB를 인수하거나 지우지 않습니다. JVM 비정상 강제 종료 시 정리 hook이 실행되지 않을 수 있으므로 로그의 생성·삭제 schema 목록으로 잔존 여부를 확인할 수 있습니다.

실행 가능한 통합테스트 8개는 PlaceController, PlaceDomain, SavedPlaceController, TourApiPlaceImportService, CommunityDomain, PostDomain, SavedRouteDomainIntegrationTest, PhotoMapDomainIntegrationTest입니다. 기본 ApplicationContext 테스트도 같은 격리를 사용합니다. `src/mysqlIntegrationTest`는 이전부터 기본 Gradle source set에 포함되지 않은 별도 과거 코드이며 이번 전환 범위에 포함되지 않습니다.

### 테스트 정책과 이번 PR의 범위

개발·통합테스트·배포의 DB 기준은 MySQL 8입니다. 이번 변경은 Testcontainers의 보조 실행 옵션을 추가하는 것이 아니라, 통합테스트 실행 기반을 Docker/Testcontainers에서 개발자가 직접 구성한 MySQL 8로 전환합니다. Testcontainers 역시 실제 MySQL을 실행하지만, 이 프로젝트에서는 Docker를 사용할 수 없는 환경에서도 전체 테스트를 실행하기 위해 직접 연결하는 방식을 사용합니다. Testcontainers 의존성을 다시 추가하거나 H2로 대체하지 않습니다.

각 개발자는 자신의 로컬 MySQL 8 계정을 사용합니다. 계정·비밀번호를 팀에서 공유하지 않으며 위의 `TEST_DB_HOST`, `TEST_DB_PORT`, `TEST_DB_USERNAME`, `TEST_DB_PASSWORD` 설정 방법만 공유합니다. MySQL 서버가 지정 host/port에서 실행 중인지 확인하고, 관리자는 테스트 계정에 `travelbird_test_*` 전용 schema 범위의 CREATE/DROP 및 migration·테스트에 필요한 DDL/DML 권한을 부여합니다. 개발 DB `travelbird` 권한은 테스트에 필요하지 않습니다. Flyway clean이나 개발 DB 초기화는 사용하지 않습니다.

PR #16의 PhotoMap 테스트는 최신 main 병합 후 공통 `LocalMySqlContextCustomizerFactory`를 사용하도록 연결부만 전환했습니다. 기존 fixture와 assertion은 유지합니다. 아직 main에 병합되지 않은 PR #19의 `PostCreateIntegrationTest`는 이번 변경 대상에 포함하지 않으며, 해당 PR 반영 시 같은 공통 설정을 사용해야 합니다. 다른 Part의 비즈니스 로직과 API 계약은 변경하지 않습니다.

V13의 `idx_ai_job_user_requested (user_id, requested_at)`는 기존 DB index이며, 공유 DBML(`docs/database/travelbird.dbml`)의 누락된 index 표기만 동일하게 보완합니다. 새로운 migration이나 DB 제약은 추가하지 않습니다. 프로젝트 규칙에 따라 `docs/harness/08-current-status.md`와 `daily-log`를 유지하고, 작업 계획용 `docs/tasks` 문서는 PR에서 제외합니다.
공유 테스트 정책은 docs/harness/02-backend-rules.md에 기록합니다. 사용자 요청에 따라 이 Harness 파일과 DBML을 PR에 포함하며 다른 로컬 작업 문서는 추가하지 않습니다.
