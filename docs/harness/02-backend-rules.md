# 02. Backend Rules

## 1. Runtime / Framework

- Java: **21 LTS**
- Spring Boot: **3.4.x**
- Spring Security: **6.4.x**
- Build: Gradle
- Database: MySQL 8.0+
- ORM: Spring Data JPA / Hibernate
- Migration: Flyway

정확한 Spring Boot 3.4 patch version은 최초 프로젝트 생성 시 하나를 pin한다.
`3.4.+`, `latest.release` 같은 동적 버전은 금지한다.

## 2. Required Dependencies

Gradle 기준 필수 범주:

```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'

    runtimeOnly 'com.mysql:mysql-connector-j'

    implementation 'org.flywaydb:flyway-core'
    implementation 'org.flywaydb:flyway-mysql'

    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    testCompileOnly 'org.projectlombok:lombok'
    testAnnotationProcessor 'org.projectlombok:lombok'

    implementation 'org.mapstruct:mapstruct:1.6.3'
    annotationProcessor 'org.mapstruct:mapstruct-processor:1.6.3'

    implementation 'com.querydsl:querydsl-jpa:5.1.0:jakarta'
    annotationProcessor 'com.querydsl:querydsl-apt:5.1.0:jakarta'
    annotationProcessor 'jakarta.annotation:jakarta.annotation-api'
    annotationProcessor 'jakarta.persistence:jakarta.persistence-api'

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.security:spring-security-test'
    testImplementation 'org.springframework.boot:spring-boot-testcontainers'
    testImplementation 'org.testcontainers:junit-jupiter'
    testImplementation 'org.testcontainers:mysql'
}
```

MapStruct compiler policy 권장:

```gradle
tasks.withType(JavaCompile).configureEach {
    options.compilerArgs += [
        '-Amapstruct.defaultComponentModel=spring',
        '-Amapstruct.unmappedTargetPolicy=ERROR'
    ]
}
```

## 3. Package Schema

기준 base package는 `com.travelbird`로 제시한다.
실제 Gradle group이 이미 다르면 **base prefix만 변경**하고 아래 domain-first 구조는 유지한다.

```text
com.travelbird
├─ global
│  ├─ config
│  ├─ security
│  ├─ error
│  └─ common
├─ auth
├─ user
├─ onboarding
├─ file
├─ place
├─ trip
├─ post
├─ community
├─ savedroute
├─ social
├─ event
├─ home
├─ search
└─ ai
```

각 Domain 내부:

```text
<domain>/
├─ controller
├─ dto/
│  ├─ request
│  └─ response
├─ service
├─ entity
├─ repository
│  └─ querydsl
└─ mapper
```

규칙:

- Controller -> Service -> Repository
- Controller에 Business Logic 금지
- Repository에서 권한 정책 판단 금지
- `global`에 domain-specific business logic 금지
- QueryDSL custom repository:
  - `XxxRepositoryCustom`
  - `XxxRepositoryImpl`
- Mapper:
  - `XxxMapper`
- Request/Response:
  - `XxxRequest`
  - `XxxResponse`

## 4. DTO — Java record

불변 Request/Response DTO는 record를 우선한다.

```java
public record RegionSummary(
    String sigunguCode,
    String sigunguName
) {}
```

Bean Validation도 record component에 선언한다.

```java
public record CreateTripRequest(
    @NotBlank String regionCode,
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    @NotNull CompanionType companionType,
    @NotEmpty @Size(max = 3) Set<TravelTheme> themes,
    @NotNull Pace pace
) {}
```

다음은 class 사용 가능:

- PATCH에서 "미전달 vs explicit null"을 별도 상태로 표현해야 하는 DTO
- Framework binding 때문에 mutable state가 실제로 필요한 경우

PATCH의 미전달/null 구분을 `Optional<T>`만으로 해결했다고 가정하지 않는다.

## 5. Lombok

Entity:

허용:
- `@Getter`
- `@NoArgsConstructor(access = AccessLevel.PROTECTED)`
- 필요한 경우 `@Builder`는 private/package factory와 제한적으로 사용

금지:
- `@Data`
- Entity 전체 `@Setter`
- `@EqualsAndHashCode`에 LAZY association 포함
- `@ToString`에 LAZY association 포함

Entity 상태 변경은 의미 있는 method로 수행한다.

```java
public void updateMemo(String memo) {
    this.memo = memo;
}
```

## 6. MapStruct

사용:
- Entity -> Response DTO
- 단순 nested DTO
- 반복적인 field mapping

사용하지 않음:
- 권한 검증
- 상태 전이
- DB 조회
- 비즈니스 기본값 결정
- PATCH의 미전달/null 정책

Mapper 누락 field는 build에서 발견되도록 `unmappedTargetPolicy=ERROR`.

## 7. QueryDSL

사용:
- Cursor Pagination
- Community 검색/필터
- visibility/status/차단 관계 조합
- 날짜 기반 Trip 조회
- Post 인기 조회
- 복합 통계/Photomap

사용하지 않음:
- 단순 `findById`
- 단순 Unique lookup
- 단순 FK lookup

QueryDSL Q-type generated source를 Git에 커밋하지 않는다.

## 8. JPA

- LAZY 기본
- N:M 직접 매핑 금지
- 연결 Entity 사용
- Collection 기본 EAGER 금지
- Entity를 API 밖으로 노출하지 않음
- ownership은 Service에서 검증
- `@Version`:
  - Trip
  - Post
  - AiTripPreview
- DB-level Unique + Service pre-check를 함께 사용
- unique 충돌은 DataIntegrityViolationException을 domain error로 변환

## 9. Flyway

- schema 생성/변경은 `src/main/resources/db/migration`
- 예:
  - `V1__init_schema.sql`
  - `V2__add_place_external_ids.sql`
- 적용 후 기존 migration 수정 금지
- `ddl-auto=validate`
- production에서 `create`, `create-drop`, `update` 금지
- DBML을 바꿨으면 Flyway migration도 반드시 함께 작성

## 10. Testcontainers

DB 관련 테스트는 MySQL과 실제 제약조건을 검증한다.

필수 검증 예:

- `(provider, external_place_id)` unique
- `(trip_id, place_id)` unique
- `(preview_id, place_id)` unique
- active Post per Trip 제약
- report unique
- saved place unique
- Cursor ordering
- Flyway migration startup

H2를 MySQL 대체 검증 DB로 사용하지 않는다.

## 11. Transaction / Concurrency

반드시 transaction boundary를 명확히 둔다.

- Kakao 신규 user + social account
- Trip + TripDays 생성
- Post create/publish + route lock
- Post save/unsave + saveCount
- AI completed validation + Preview save + Job state
- AI Preview apply
- place resolve + external ID mapping

낙관적 락:
- Trip/PATCH
- Post/PATCH
- Preview/PATCH

행 잠금이 필요한 정책:
- 최초 Post publish와 Trip route 수정 경쟁
- Trip cancel과 Post publish 경쟁
- external ID resolve 동시 생성 경쟁

## 12. Logging / Secret

로그 금지:
- JWT raw token
- Refresh Token raw value
- Kakao Access Token
- DB password
- Naver secret
- Internal AI Key
- Presigned S3 URL 전체 query signature

허용:
- userId / tripId / postId / placeId / jobId
- ErrorCode
- request correlation id
