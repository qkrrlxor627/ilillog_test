# 일일로그 (ilog) 백엔드

1일 1로그 학습 기록 플랫폼 백엔드. 회원 / 게시글 / 해시태그 MVP.

- Java 25, Spring Boot 4.1.1, Spring Data JPA (Hibernate 7), PostgreSQL 17, Flyway
- Spring Security + JWT (HS256, Access Token 단독 — Refresh 구조는 팀 결정 대기)
- 작업 규칙·API 명세·미결 사항(D-xx)은 `CLAUDE.md` 참고

## 로컬 실행

사전 준비: Docker, JDK(아무 버전 17+). JDK 25 는 Gradle toolchain 이 자동으로 내려받는다.

```bash
# 1) PostgreSQL 기동 (로컬에 이미 5432 를 쓰는 PostgreSQL 이 있으면 export DB_PORT=55432 후 실행)
docker compose up -d

# 2) 애플리케이션 실행 (JWT_SECRET 은 32자 이상)
export JWT_SECRET="$(openssl rand -base64 48)"
./gradlew bootRun --args='--spring.profiles.active=local'
```

- API: `http://localhost:8080/api/v1`
- Swagger UI: `http://localhost:8080/swagger-ui` (prod 프로필에서는 비활성)
- 헬스체크: `http://localhost:8080/actuator/health`

개인 설정은 `src/main/resources/application-personal.yml`(`.gitignore`)에 두면 local 프로필에서 자동으로 읽는다.

## 빌드·테스트

```bash
./gradlew spotlessApply     # 코드 포맷 (google-java-format AOSP)
./gradlew clean build       # spotlessCheck + 테스트 + JaCoCo 커버리지 게이트
```

- 테스트는 Testcontainers(PostgreSQL)를 사용하므로 **Docker 가 실행 중이어야 한다.**
- 커버리지 기준 `[잠정]`: 전체 라인 70%, `com.ilog.*.service` 라인 80%
- 리포트: `build/reports/tests/test/index.html`, `build/reports/jacoco/test/html/index.html`

## 환경변수 목록

| 이름 | 필수 | 기본값 | 설명 |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | O | - | `local` / `dev` / `prod` (테스트는 `test`) |
| `JWT_SECRET` | O | - | JWT HS256 서명 키. 32자 이상. 레포·yml 에 커밋 금지 |
| `JWT_ACCESS_TTL` | X | `1800` | Access Token 유효 시간(초) |
| `DB_URL` | dev/prod O | local: `jdbc:postgresql://localhost:${DB_PORT}/ilog` | JDBC URL |
| `DB_PORT` | X | `5432` | local 전용. docker compose 호스트 포트 (로컬 PostgreSQL 과 충돌 시 변경) |
| `DB_USERNAME` | dev/prod O | local: `ilog` | DB 사용자 |
| `DB_PASSWORD` | dev/prod O | local: `ilog` | DB 비밀번호 |
| `CORS_ALLOWED_ORIGINS` | X | local: `http://localhost:5173` | 허용 origin, 쉼표 구분 |
| `HEALTH_CHECK_URL_DEV` | CI | - | `scripts/health-check.sh` develop 대상 URL |
| `HEALTH_CHECK_URL_PROD` | CI | - | `scripts/health-check.sh` main 대상 URL |

비밀값은 환경변수 / Jenkins Credentials / AWS Parameter Store 중 하나로만 주입한다.

## 패키지 구조

```
com.ilog
├── global    # config, security(JWT·필터), response(ApiResponse·PageResponse), error, entity, policy(잠정 상수)
├── auth      # 로그인·로그아웃·임시 비밀번호
├── member    # 회원가입·중복 확인·내 정보
├── post      # 게시글 CRUD + 목록/검색
└── hashtag   # 게시글 해시태그 등록·삭제
```

## API 요약 (`/api/v1`)

| 기능 | Method | URL | 인증 |
|---|---|---|---|
| 회원가입 | POST | `/users` | 🔓 |
| 이메일 중복 확인 | GET | `/users/email-availability?email=` | 🔓 |
| 닉네임 중복 확인 | GET | `/users/nickname-availability?nickname=` | 🔓/🔒 |
| 로그인 | POST | `/auth/tokens` | 🔓 |
| 로그아웃 | DELETE | `/auth/tokens` | 🔒 |
| 임시 비밀번호 발급 | POST | `/auth/temporary-passwords` | 🔓 |
| 비밀번호 재확인 + 개인정보 | POST | `/users/me/password-verification` | 🔒 |
| 닉네임 수정 | PATCH | `/users/me` | 🔒 |
| 비밀번호 변경 | PATCH | `/users/me/password` | 🔒 |
| 회원 탈퇴 | POST | `/users/me/withdrawal` | 🔒 |
| 게시글 등록 | POST | `/posts` | 🔒 |
| 게시글 목록 + 검색 | GET | `/posts` | 🔒 |
| 게시글 상세 | GET | `/posts/{postId}` | 🔒 |
| 게시글 수정 | PATCH | `/posts/{postId}` | 🔒 |
| 게시글 삭제 | DELETE | `/posts/{postId}` | 🔒 |
| 해시태그 등록 | POST | `/posts/{postId}/hashtags` | 🔒 |
| 해시태그 삭제 | DELETE | `/posts/{postId}/hashtags/{hashtagId}` | 🔒 |
