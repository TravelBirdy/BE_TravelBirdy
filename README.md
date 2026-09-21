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

실행 가능한 통합테스트 7개는 PlaceController, PlaceDomain, SavedPlaceController, TourApiPlaceImportService, CommunityDomain, PostDomain, SavedRouteDomainIntegrationTest입니다. 기본 ApplicationContext 테스트도 같은 격리를 사용합니다. `src/mysqlIntegrationTest`는 이전부터 기본 Gradle source set에 포함되지 않은 별도 과거 코드이며 이번 전환 범위에 포함되지 않습니다.
