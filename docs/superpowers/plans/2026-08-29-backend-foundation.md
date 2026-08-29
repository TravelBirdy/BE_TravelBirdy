# Backend Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first runnable TravelBird Backend foundation: fixed Gradle/Spring Boot project, approved bootstrap decisions, V1 schema migration, global security/error wiring, and foundation tests.

**Architecture:** Keep the domain-first package rule under `com.travelbird`. This slice creates the build system, application entrypoint, configuration validation, database migration, and shared primitives before feature controllers. API behavior stays driven by `docs/source/travelbird-openapi-v10.json`; business behavior stays driven by `docs/source/backend-functional-spec-v10.md`.

**Tech Stack:** Java 21, Spring Boot 3.4.13, Spring Security 6.4.x through Spring Boot dependency management, Gradle 8.14.3 wrapper, MySQL 8.0+, Spring Data JPA, Hibernate, Flyway, Lombok, MapStruct, QueryDSL, JUnit 5, Spring Boot Test, Testcontainers MySQL.

---

## Approved Design Baseline

These six decisions are approved as the next design and implementation baseline.

| Item | Approved decision | Implementation impact |
| --- | --- | --- |
| Spring Boot patch | Use `3.4.13`. | Pin `org.springframework.boot` to `3.4.13`; do not use dynamic versions. |
| Gradle group/base package | Use `com.travelbird`. | Keep current Java package root and set Gradle `group = 'com.travelbird'`. |
| Manual AI Preview place reason | Generate `사용자가 일정 미리보기에 직접 추가한 장소입니다.`. | Do not change PATCH schema. Persist this generated reason when a user manually adds a preview place. |
| Event TourAPI identifier | Use internal `event_id BIGINT AUTO_INCREMENT` plus `tour_api_content_id VARCHAR(50) NOT NULL UNIQUE`. | Public API keeps internal `eventId`; sync upserts by `tour_api_content_id`. |
| FK `ON DELETE` | Use `CASCADE` for owned children, `RESTRICT` for aggregate/cross-domain references, `SET NULL` for nullable surviving references. | Encode each FK explicitly in `V1__init_schema.sql`; keep withdrawal cleanup in services. |
| `UserStatus` | Use `ACTIVE`, `SUSPENDED`, `WITHDRAWN`. | Create `UserStatus`; DB default is `ACTIVE`; non-active users fail with `USER_NOT_ACTIVE`. |

Version evidence checked on 2026-08-29:

- Spring Boot 3.4.13 is published on Maven Central and Spring's 3.4 Gradle plugin documentation shows plugin version `3.4.13`.
- Spring Boot 3.4 documentation supports Gradle 7.6.4+ or 8.4+. Gradle 8.14.3 is a stable 8.x wrapper target.

## Scope Boundary

This is the first implementation slice. It does not implement all v10 endpoints. It creates the foundation that later domain plans will build on.

Included:

- Build files and Gradle wrapper.
- Spring Boot application entrypoint.
- Approved enum/value baseline needed by schema and code.
- DBML alignment for the six approved decisions.
- Initial Flyway V1 migration.
- Migration and configuration tests.
- Harness status and daily log updates after verification.

Excluded:

- Full Auth/Kakao login behavior.
- Full Trip/Post/AI endpoint services.
- Naver, Kakao, S3, and AI real network clients.
- The removed local database helper file is intentionally excluded from this plan.

## File Structure

| Path | Action | Responsibility |
| --- | --- | --- |
| `settings.gradle` | Create | Gradle root project name. |
| `build.gradle` | Create | Java 21, Spring Boot 3.4.13, dependencies, annotation processors, tests. |
| `gradle/wrapper/gradle-wrapper.properties` | Create by wrapper command | Pin Gradle 8.14.3. |
| `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar` | Create by wrapper command | Reproducible Gradle execution. |
| `src/main/java/com/travelbird/TravelBirdApplication.java` | Create | Spring Boot entrypoint. |
| `src/main/java/com/travelbird/user/entity/UserStatus.java` | Create | Approved user lifecycle enum. |
| `src/main/java/com/travelbird/event/entity/Event.java` | Create | Event aggregate with internal ID and TourAPI external ID column. |
| `src/main/resources/db/migration/V1__init_schema.sql` | Create | MySQL schema from DBML with approved FK delete actions. |
| `src/test/java/com/travelbird/TravelBirdApplicationTest.java` | Create | Spring context smoke test using Testcontainers. |
| `src/test/java/com/travelbird/database/FlywayMigrationTest.java` | Create | V1 schema, key constraints, and FK action verification. |
| `src/test/java/com/travelbird/global/config/CorsPropertiesTest.java` | Create | CORS property binding baseline. |
| `docs/database/travelbird.dbml` | Modify | Reflect UserStatus, Event external ID, and FK delete decisions. |
| `docs/harness/08-current-status.md` | Modify | Move the six decisions out of unresolved status. |
| `docs/harness/09-unresolved.md` | Modify | Record that previous six bootstrap questions are approved. |
| `daily-log/2026.08.29.md` | Modify | Record approval, plan creation, and verification evidence. |

## FK Delete Matrix

Use these actions in `V1__init_schema.sql` and DBML references.

| Rule | FKs |
| --- | --- |
| `ON DELETE CASCADE` | `user_social_accounts.user_id`, `refresh_tokens.user_id`, `personality_options.question_id`, `personality_answers.submission_id`, `place_external_ids.place_id`, `saved_places.user_id`, `trip_themes.trip_id`, `trip_hashtags.trip_id`, `trip_days.trip_id`, `trip_places.trip_id`, `trip_places.(trip_id, trip_day_id)`, `trip_place_images.trip_place_id`, `trip_wishlist_places.trip_id`, `post_images.post_id`, `post_places.post_id`, `post_hashtags.post_id`, `post_view_histories.post_id`, `post_daily_metrics.post_id`, `saved_routes.user_id`, `follows.follower_user_id`, `follows.following_user_id`, `user_blocks.blocker_user_id`, `user_blocks.blocked_user_id`, `ai_trip_previews.job_id`, `ai_preview_hashtags.preview_id`, `ai_preview_days.preview_id`, `ai_preview_places.preview_id`, `ai_preview_places.(preview_id, preview_day_id)` |
| `ON DELETE SET NULL` | `posts.representative_file_id`, `ai_recommendation_jobs.target_trip_id`, `ai_trip_previews.applied_trip_id` |
| `ON DELETE RESTRICT` | `personality_questions.test_version`, `personality_submissions.user_id`, `personality_submissions.test_version`, `personality_answers.question_id`, `personality_answers.(question_id, option_id)`, `files.user_id`, `places.sigungu_code`, `saved_places.place_id`, `home_recommended_places.place_id`, `trips.user_id`, `trips.sigungu_code`, `trip_places.place_id`, `trip_place_images.file_id`, `trip_wishlist_places.place_id`, `posts.trip_id`, `post_images.file_id`, `post_places.trip_place_id`, `post_view_histories.viewer_user_id`, `reports.reporter_user_id`, `reports.post_id`, `events.sigungu_code`, `ai_recommendation_jobs.user_id`, `ai_trip_previews.user_id`, `ai_preview_places.place_id` |

## Task 1: Create Gradle Project Files

**Files:**

- Create: `settings.gradle`
- Create: `build.gradle`
- Create by command: `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties`

- [ ] **Step 1: Create `settings.gradle`**

Write exactly:

```groovy
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

rootProject.name = 'travelbird-backend'
```

- [ ] **Step 2: Create `build.gradle`**

Write exactly:

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.4.13'
    id 'io.spring.dependency-management' version '1.1.7'
}

group = 'com.travelbird'
version = '0.0.1-SNAPSHOT'

def querydslVersion = '5.1.0'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom annotationProcessor
    }
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.flywaydb:flyway-core'
    implementation 'org.flywaydb:flyway-mysql'
    implementation 'com.mysql:mysql-connector-j'
    implementation 'org.mapstruct:mapstruct:1.6.3'
    implementation "com.querydsl:querydsl-jpa:${querydslVersion}:jakarta"

    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    annotationProcessor 'org.mapstruct:mapstruct-processor:1.6.3'
    annotationProcessor "com.querydsl:querydsl-apt:${querydslVersion}:jakarta"
    annotationProcessor 'jakarta.persistence:jakarta.persistence-api'
    annotationProcessor 'jakarta.annotation:jakarta.annotation-api'

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.security:spring-security-test'
    testImplementation 'org.testcontainers:junit-jupiter'
    testImplementation 'org.testcontainers:mysql'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

tasks.withType(JavaCompile).configureEach {
    options.compilerArgs += ['-Amapstruct.unmappedTargetPolicy=ERROR']
}

tasks.named('test') {
    useJUnitPlatform()
}
```

- [ ] **Step 3: Generate wrapper**

Run:

```powershell
gradle wrapper --gradle-version 8.14.3 --distribution-type bin
```

Expected: `BUILD SUCCESSFUL`; wrapper files exist.

- [ ] **Step 4: Verify wrapper**

Run:

```powershell
.\gradlew.bat --version
```

Expected: output includes `Gradle 8.14.3`.

## Task 2: Add Application Entrypoint And Smoke Test

**Files:**

- Create: `src/main/java/com/travelbird/TravelBirdApplication.java`
- Create: `src/test/java/com/travelbird/TravelBirdApplicationTest.java`

- [ ] **Step 1: Create application class**

```java
package com.travelbird;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TravelBirdApplication {

    public static void main(String[] args) {
        SpringApplication.run(TravelBirdApplication.class, args);
    }
}
```

- [ ] **Step 2: Create context test**

```java
package com.travelbird;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
class TravelBirdApplicationTest {

    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
        .withDatabaseName("travelbird_test")
        .withUsername("travelbird")
        .withPassword("travelbird");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("app.jwt.secret", () -> "test-secret-test-secret-test-secret-test-secret");
        registry.add("app.naver.client-id", () -> "test-client-id");
        registry.add("app.naver.client-secret", () -> "test-client-secret");
        registry.add("app.ai.server-base-url", () -> "http://localhost:18080");
        registry.add("app.ai.internal-key", () -> "test-internal-ai-key");
        registry.add("app.cors.allowed-origins", () -> "http://localhost:3000");
        registry.add("app.aws.region", () -> "ap-northeast-2");
        registry.add("app.aws.s3.bucket", () -> "travelbird-test");
    }

    @Test
    void contextLoads() {
    }
}
```

- [ ] **Step 3: Run expected red check**

Run:

```powershell
.\gradlew.bat test --tests com.travelbird.TravelBirdApplicationTest
```

Expected before V1 migration exists: failure caused by Flyway missing schema or missing migration. Record the exact output in the daily log.

## Task 3: Align DBML With Approved Decisions

**Files:**

- Modify: `docs/database/travelbird.dbml`

- [ ] **Step 1: Add `user_status` enum**

```dbml
Enum user_status {
  ACTIVE
  SUSPENDED
  WITHDRAWN
}
```

- [ ] **Step 2: Change `users.status`**

Use:

```dbml
status user_status [not null, default: 'ACTIVE', note: 'Approved values: ACTIVE, SUSPENDED, WITHDRAWN. Authentication and token flows reject non-ACTIVE users with USER_NOT_ACTIVE.']
```

- [ ] **Step 3: Change `events` identifiers**

Use:

```dbml
event_id bigint [pk, increment]
tour_api_content_id varchar(50) [not null, unique, note: 'Original TourAPI contentId used for daily sync upsert. Public API continues to expose internal eventId.']
```

- [ ] **Step 4: Add DBML delete actions**

Append DBML delete actions to each `Ref:` line from the FK matrix. Examples:

```dbml
Ref: user_social_accounts.user_id > users.user_id [delete: cascade]
Ref: posts.representative_file_id > files.file_id [delete: set null]
Ref: trips.user_id > users.user_id [delete: restrict]
```

- [ ] **Step 5: Verify stale decision text is gone**

Run:

```powershell
rg -n "미확정|고정하지 않음|TourAPI contentId와 동일|ON DELETE" docs/database/travelbird.dbml
```

Expected: no stale unresolved wording for the six approved decisions.

## Task 4: Create V1 Flyway Schema

**Files:**

- Create: `src/main/resources/db/migration/V1__init_schema.sql`
- Create: `src/test/java/com/travelbird/database/FlywayMigrationTest.java`

- [ ] **Step 1: Create migration directory**

Run:

```powershell
New-Item -ItemType Directory -Force -Path src\main\resources\db\migration
```

Expected: migration directory exists; the removed local database helper file is not recreated.

- [ ] **Step 2: Write V1 schema**

Translate `docs/database/travelbird.dbml` into MySQL DDL in dependency order. Preserve every DBML primary key, unique key, index, nullability rule, enum check constraint, and the FK delete matrix. The changed `users` and `events` tables must include these exact elements:

```sql
CREATE TABLE users (
    user_id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(255) NULL,
    nickname VARCHAR(10) NULL,
    introduction VARCHAR(100) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    role VARCHAR(30) NOT NULL,
    onboarding_completed BOOLEAN NOT NULL DEFAULT FALSE,
    bird_type VARCHAR(30) NULL,
    PRIMARY KEY (user_id),
    CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'WITHDRAWN')),
    CONSTRAINT ck_users_role CHECK (role IN ('ROLE_USER', 'ROLE_ADMIN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE events (
    event_id BIGINT NOT NULL AUTO_INCREMENT,
    tour_api_content_id VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    sigungu_code VARCHAR(5) NOT NULL,
    place_name VARCHAR(255) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    thumbnail_url VARCHAR(2048) NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (event_id),
    CONSTRAINT uk_events_tour_api_content_id UNIQUE (tour_api_content_id),
    INDEX idx_events_period (start_date, end_date, event_id),
    INDEX idx_events_sigungu_period (sigungu_code, start_date, event_id),
    CONSTRAINT fk_events_sigungu FOREIGN KEY (sigungu_code) REFERENCES sigungu_master(sigungu_code) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
```

- [ ] **Step 3: Create migration metadata test**

```java
package com.travelbird.database;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
class FlywayMigrationTest {

    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
        .withDatabaseName("travelbird_test")
        .withUsername("travelbird")
        .withPassword("travelbird");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("app.jwt.secret", () -> "test-secret-test-secret-test-secret-test-secret");
        registry.add("app.naver.client-id", () -> "test-client-id");
        registry.add("app.naver.client-secret", () -> "test-client-secret");
        registry.add("app.ai.server-base-url", () -> "http://localhost:18080");
        registry.add("app.ai.internal-key", () -> "test-internal-ai-key");
        registry.add("app.cors.allowed-origins", () -> "http://localhost:3000");
        registry.add("app.aws.region", () -> "ap-northeast-2");
        registry.add("app.aws.s3.bucket", () -> "travelbird-test");
    }

    @Autowired
    DataSource dataSource;

    @Test
    void migrationCreatesApprovedColumns() throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            assertThat(columnExists(statement, "users", "status")).isTrue();
            assertThat(columnExists(statement, "events", "tour_api_content_id")).isTrue();
            assertThat(uniqueIndexExists(statement, "events", "uk_events_tour_api_content_id")).isTrue();
        }
    }

    @Test
    void migrationCreatesExpectedDeleteRules() throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            assertThat(deleteRule(statement, "fk_user_social_accounts_user")).isEqualTo("CASCADE");
            assertThat(deleteRule(statement, "fk_posts_representative_file")).isEqualTo("SET NULL");
            assertThat(deleteRule(statement, "fk_trips_user")).isEqualTo("RESTRICT");
        }
    }

    private boolean columnExists(Statement statement, String tableName, String columnName) throws Exception {
        try (ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = '" + tableName + "' AND column_name = '" + columnName + "'")) {
            resultSet.next();
            return resultSet.getInt(1) == 1;
        }
    }

    private boolean uniqueIndexExists(Statement statement, String tableName, String indexName) throws Exception {
        try (ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = '" + tableName + "' AND index_name = '" + indexName + "' AND non_unique = 0")) {
            resultSet.next();
            return resultSet.getInt(1) > 0;
        }
    }

    private String deleteRule(Statement statement, String constraintName) throws Exception {
        try (ResultSet resultSet = statement.executeQuery("SELECT delete_rule FROM information_schema.referential_constraints WHERE constraint_schema = DATABASE() AND constraint_name = '" + constraintName + "'")) {
            assertThat(resultSet.next()).isTrue();
            return resultSet.getString(1);
        }
    }
}
```

- [ ] **Step 4: Run migration test**

Run:

```powershell
.\gradlew.bat test --tests com.travelbird.database.FlywayMigrationTest
```

Expected: test passes with MySQL Testcontainers and Flyway enabled.

## Task 5: Add Foundation Domain Types

**Files:**

- Create: `src/main/java/com/travelbird/user/entity/UserStatus.java`
- Create: `src/main/java/com/travelbird/event/entity/Event.java`
- Create: `src/test/java/com/travelbird/event/entity/EventTest.java`

- [ ] **Step 1: Create `UserStatus`**

```java
package com.travelbird.user.entity;

public enum UserStatus {
    ACTIVE,
    SUSPENDED,
    WITHDRAWN;

    public boolean isActive() {
        return this == ACTIVE;
    }
}
```

- [ ] **Step 2: Create `Event` entity**

```java
package com.travelbird.event.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "events")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Long id;

    @Column(name = "tour_api_content_id", nullable = false, length = 50, unique = true)
    private String tourApiContentId;

    @Column(nullable = false)
    private String name;

    @Column(name = "sigungu_code", nullable = false, length = 5)
    private String sigunguCode;

    @Column(name = "place_name", nullable = false)
    private String placeName;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "thumbnail_url", length = 2048)
    private String thumbnailUrl;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Event(String tourApiContentId, String name, String sigunguCode, String placeName, LocalDate startDate, LocalDate endDate, String thumbnailUrl, LocalDateTime updatedAt) {
        this.tourApiContentId = tourApiContentId;
        this.name = name;
        this.sigunguCode = sigunguCode;
        this.placeName = placeName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.thumbnailUrl = thumbnailUrl;
        this.updatedAt = updatedAt;
    }
}
```

- [ ] **Step 3: Create entity test**

```java
package com.travelbird.event.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class EventTest {

    @Test
    void eventUsesInternalIdAndSeparateTourApiContentId() {
        Event event = new Event("2781234", "서울 축제", "11110", "광화문광장", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 3), "https://example.com/event.webp", LocalDateTime.of(2026, 8, 29, 10, 0));

        assertThat(event.getId()).isNull();
        assertThat(event.getTourApiContentId()).isEqualTo("2781234");
    }
}
```

- [ ] **Step 4: Run type test**

Run:

```powershell
.\gradlew.bat test --tests com.travelbird.event.entity.EventTest
```

Expected: test passes.

## Task 6: Verify CORS Configuration

**Files:**

- Create: `src/test/java/com/travelbird/global/config/CorsPropertiesTest.java`
- Modify only if compilation requires it: `src/main/java/com/travelbird/global/config/CorsProperties.java`

- [ ] **Step 1: Create CORS test**

```java
package com.travelbird.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class CorsPropertiesTest {

    @Test
    void allowedOriginsAreExplicitValues() {
        CorsProperties properties = new CorsProperties(List.of("http://localhost:3000"));

        assertThat(properties.allowedOrigins()).containsExactly("http://localhost:3000");
    }
}
```

- [ ] **Step 2: Run CORS test**

Run:

```powershell
.\gradlew.bat test --tests com.travelbird.global.config.CorsPropertiesTest
```

Expected: test passes. If constructor binding differs, change `CorsProperties` and this test together while preserving the existing `SecurityConfig` CORS policy.

## Task 7: Update Harness Status And Daily Log

**Files:**

- Modify: `docs/harness/08-current-status.md`
- Modify: `docs/harness/09-unresolved.md`
- Modify: `daily-log/2026.08.29.md`

- [ ] **Step 1: Update current status**

Replace the unresolved six-item section in `docs/harness/08-current-status.md` with:

```markdown
## D. Approved Design Decisions

The following items were approved by the user on 2026-08-29 and are the baseline for the next implementation stage.

1. Spring Boot patch: `3.4.13`
2. Gradle group/base package: `com.travelbird`
3. Manual AI Preview place `reason`: `사용자가 일정 미리보기에 직접 추가한 장소입니다.`
4. Event original TourAPI ID: internal `event_id` plus unique `tour_api_content_id`
5. FK `ON DELETE`: `CASCADE` for owned children, `RESTRICT` for aggregate/cross-domain references, `SET NULL` for nullable surviving references
6. UserStatus: `ACTIVE`, `SUSPENDED`, `WITHDRAWN`
```

- [ ] **Step 2: Replace unresolved document**

Replace `docs/harness/09-unresolved.md` with:

```markdown
# 09. Unresolved / Explicitly Not Invented

As of 2026-08-29, the previous six backend bootstrap questions have been approved by the user and no longer block the next implementation plan.

## Approved On 2026-08-29

1. Spring Boot patch: `3.4.13`
2. Gradle group/base package: `com.travelbird`
3. Manual AI Preview place `reason`: `사용자가 일정 미리보기에 직접 추가한 장소입니다.`
4. Event original TourAPI ID: internal `event_id` plus unique `tour_api_content_id`
5. FK `ON DELETE`: `CASCADE` for owned children, `RESTRICT` for aggregate/cross-domain references, `SET NULL` for nullable surviving references
6. UserStatus: `ACTIVE`, `SUSPENDED`, `WITHDRAWN`

## Current Unresolved Items

No additional unresolved backend bootstrap decisions are recorded in this file.
```

- [ ] **Step 3: Append daily log entry**

Append a section that records the approval, plan path, changed files, actual verification commands, and that no backend implementation was performed during planning.

## Task 8: Full Verification Before Implementation Handoff

- [ ] **Step 1: Verify removed records stay absent**

Run:

```powershell
Use `rg` to verify removed artifact names are absent from `AGENTS.md`, `README.md`, `docs`, `daily-log`, `src`, and `.env.example`. Do not store those removed names in this plan.
```

Expected: no matches.

- [ ] **Step 2: Verify approved decisions are recorded**

Run:

```powershell
rg -n "3\.4\.13|com\.travelbird|tour_api_content_id|ACTIVE, SUSPENDED, WITHDRAWN|사용자가 일정 미리보기에 직접 추가한 장소입니다" docs/harness docs/superpowers/plans daily-log
```

Expected: matches exist in the plan and status documents.

- [ ] **Step 3: Verify build/tests after implementation**

Run:

```powershell
.\gradlew.bat test
```

Expected: all tests pass. If Docker is unavailable, record the exact Testcontainers failure and do not claim the test suite passes.

- [ ] **Step 4: Verify git state**

Run:

```powershell
git status --short
git log --oneline -5
```

Expected: status shows only intentional changes, and any commits match the task commit messages.

## Self-Review

Spec coverage:

- The six approved decisions are represented in the approved baseline, DBML task, V1 migration task, domain type task, and documentation task.
- Source-of-truth priority is preserved. The OpenAPI and source Markdown documents are read-only in this plan because the six decisions do not require method/path/request/response contract changes.
- Removed helper artifacts are excluded from creation.

Placeholder scan:

- The plan uses concrete file paths, commands, code snippets, and expected verification results.
- The full V1 SQL translation is intentionally constrained by DBML plus the FK matrix, with exact SQL requirements for the approved `users` and `events` changes.

Type consistency:

- `UserStatus` values match DB check values.
- `Event.tourApiContentId` matches `events.tour_api_content_id`.
- Gradle group and Java package both use `com.travelbird`.

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-08-29-backend-foundation.md`. Two execution options:

1. Subagent-Driven (recommended) - Dispatch a fresh subagent per task, review between tasks, faster iteration.
2. Inline Execution - Execute tasks in this session using `superpowers:executing-plans`, batch execution with checkpoints.

Choose one before implementation begins.


