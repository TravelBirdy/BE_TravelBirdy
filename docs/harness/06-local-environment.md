# 06. Local MySQL / .env / application.yml

## 1. Local MySQL

MySQL 8.0+ 실행 후:

```sql
CREATE DATABASE IF NOT EXISTS travelbird
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
```


## 2. `.env.example`

Git 공유 파일:
`.env.example`

실제 Secret 값은 넣지 않는다.

Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

macOS/Linux:

```bash
cp .env.example .env
```

그 다음 `.env`에 **로컬 실제 값만** 입력한다.

예시 형태:

```properties
DB_URL=jdbc:mysql://localhost:3306/travelbird?serverTimezone=UTC&characterEncoding=UTF-8
DB_USERNAME=<local-user>
DB_PASSWORD=<local-password>
JWT_SECRET=<local-random-secret>
NAVER_CLIENT_ID=<local-client-id>
NAVER_CLIENT_SECRET=<local-secret>
AI_SERVER_BASE_URL=<configured-backend-to-ai-base-url>
TRAVELBIRD_AI_CALLBACK_KEY=<local-internal-key>
CORS_ALLOWED_ORIGINS=http://localhost:8081,http://localhost:19006
AWS_REGION=ap-northeast-2
S3_BUCKET=<local-dev-bucket>
```

위 값은 사용법 예시일 뿐 `.env.example`에는 실제 값이 들어가지 않는다.

## 3. Gitignore

기존 `.gitignore`에 다음을 포함한다.

```gitignore
.env
.env.*
!.env.example
```

`application.yml`과 `.env.example`은 Git에 포함해도 되지만 `.env`는 포함하지 않는다.

## 4. application.yml

실제 파일:
`src/main/resources/application.yml`

Secret은 다음처럼 환경변수를 참조한다.

```yaml
password: ${DB_PASSWORD}
secret: ${JWT_SECRET}
client-secret: ${NAVER_CLIENT_SECRET}
internal-key: ${TRAVELBIRD_AI_CALLBACK_KEY}
```

실제 값을 yml에 직접 적지 않는다.

## 5. Flyway / JPA

Local/Dev/Prod 모두:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
```

로 두고 Schema는 Flyway migration이 생성한다.

`ddl-auto=update`로 DB 구조를 자동 변경하지 않는다.

## 6. Time

- DB/JPA timestamp: UTC
- Hibernate JDBC timezone: UTC
- API 응답: ISO 8601
- Trip 상태/일일 AI 제한: Service에서 `Asia/Seoul`

DB server timezone과 서비스 날짜 정책을 같은 개념으로 섞지 않는다.
