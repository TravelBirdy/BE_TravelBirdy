# 05. Spring Security 6.4 / CORS

Reference:
- `src/main/java/com/travelbird/global/config/CorsProperties.java`
- `src/main/java/com/travelbird/global/config/SecurityConfig.java`

## 확정 설정

### Allowed Origins
환경변수 `CORS_ALLOWED_ORIGINS`에서 explicit origin 목록을 주입한다.

예:

```text
http://localhost:8081,http://localhost:19006
```

`allowCredentials=true`이므로 `*` 금지.

### Allowed Methods

- GET
- POST
- PUT
- DELETE
- PATCH
- OPTIONS

### Allowed Headers

- Authorization
- Content-Type
- Accept
- Origin

`X-Internal-AI-Key`는 브라우저용 Header가 아니므로 CORS 허용 목록에 넣지 않는다.

### Exposed Headers

- Location

현재 공개 API 계약이 custom response header에 의존하지 않기 때문에 불필요한 노출을 추가하지 않는다.

### Credentials

```text
true
```

### Max Age

```text
3600 seconds
```

### Preflight

```text
OPTIONS /**
```

는 인증 없이 통과한다.

## Optional authentication 주의

OpenAPI에서 선택적 인증인 API는 URL 레벨에서 permitAll이어도
Bearer Token이 전달되면 JWT Filter가 SecurityContext를 채워야 한다.

즉 JWT Filter는:

- Authorization Header가 없으면 그대로 chain 진행
- Bearer Token이 있으면 검증
- 잘못된 Token이 전달됐을 때는 인증 오류 처리

방식을 사용한다.

## Internal Callback

`/internal/ai-callbacks/trip-recommendations`는 JWT가 아니라
`X-Internal-AI-Key` 계약을 사용한다.

따라서 실제 구현에서는 전용 `OncePerRequestFilter` 또는 동일 수준의 인증 컴포넌트를 추가한 뒤
해당 Endpoint를 Bearer 인증 대상에서 분리한다.

CORS는 이 Internal Endpoint 인증을 대체하지 않는다.
