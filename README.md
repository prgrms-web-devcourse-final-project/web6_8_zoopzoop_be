 WEB6_8_ZOOPZOOP_BE
사용자 맞춤형 자료 추천과 시각화 아카이빙을 결합한 협업 플랫폼 - 서버 파트

---

## 목차
1. [소개](#소개)
2. [핵심 기능](#핵심-기능)
3. [기술 스택](#기술-스택)
4. [프로젝트 구조](#프로젝트-구조)
5. [설치 및 실행](#설치-및-실행)
6. [추가 자료](#추가-자료)

---

## 💡소개
본 프로젝트는 웹 서핑 중 발견한 정보를 **효율적으로 수집, 요약, 정리**하고, 이를 팀 단위로 **공유 및 브레인스토밍**까지 이어갈 수 있는 **지식 관리 및 협업 플랫폼**을 목표로 합니다.

**홈페이지:** [ZoopZoop](https://www.zoopzoop.kro.kr/)

FE Repository: [Link](https://github.com/prgrms-web-devcourse-final-project/WEB5_6_ZOOPZOOP_FE)

Chrome Extension Repository: [Link](https://github.com/prgrms-web-devcourse-final-project/WEB5_6_ZOOPS_TENSION_FE)

---

## 📄핵심 기능

#### A. 정보 수집
- **크롬 확장자(Extension) 제공**
  - 웹 페이지에서 원하는 부분 선택 후 저장
  - 제목, 본문 요약, URL, 태그, 썸네일을 카드뷰 형태로 개인 아카이브에 저장
- **직접 URL 저장**
  - 사용자가 원하는 웹 페이지 URL을 개인 아카이브에 직접 추가 가능

#### B. 개인 아카이브
- **카테고리 분류**
  - 폴더 생성 후 데이터를 카테고리별로 분류 가능
- **대시보드**
  - 수집한 정보 카드 형태로 한눈에 확인

#### C. 협업 기능 (공유 아카이브)
- 팀원 초대 → 동일한 대시보드 공유
- 팀원이 공유한 데이터에 댓글 작성 가능 → 브레인스토밍 지원
- 여러 카드 배열/연결 → **지식 맵(마인드맵) 형태 구성**
- 공통 수정 및 실시간 동기화 (Liveblocks 사용)
- **AI 추천 기능** → 공유된 URL 태그 기반 맞춤형 뉴스 추천

#### D. AI 기반 맞춤형 뉴스 추천
- 뉴스 API 연동 → 특정 키워드 관련 최신 뉴스 수집
- **오늘의 뉴스 추천**
- 개인 & 공유 아카이브 내용 기반 **맞춤형 뉴스 추천**

---

## 🔧기술 스택
- **Framework & Language:** Spring Boot 3.5.5, Java 21
- **Database:** MySQL (production), H2 (development)
- **Security & Auth:** Spring Security, OAuth2, JWT
- **AI:** Spring AI, Groq
- **Testing:** JUnit, Mockito
- **Deployment:** Docker
- **Utilities & External Services:**
  - Messaging: RabbitMQ
  - Caching & TTL: Redis
  - Documentation: Swagger
  - Monitoring: Sentry
  - Crawling: Jsoup
  - Storage: AWS S3
  - Search Engine: Elastic Search

---

## 📁프로젝트 구조
```
src/main/java/org/tuna/zoopzoop/backend
├── domain
│   └── archive                     # 아카이브
│       ├── archive                 # 아카이브 로직
│       └── folder                  # 아카이브 내 폴더 로직
│   ├── auth                        # 인증/인가 비즈니스 로직
│   ├── dashboard                   # Liveblocks 그래프 데이터
│   ├── datasource                  # 자료 데이터 크롤링 및 저장
│   ├── home                        # 백엔드 홈 화면 컨트롤러
│   ├── member                      # 사용자
│   ├── news                        # 뉴스 조회 API
│   ├── space                       # 스페이스(협업 공간) 관련
│       ├── archive                 # 스페이스 공용 아카이브
│       ├── membership              # 스페이스 권한 관리
│       └── space                   # 스페이스 비즈니스 로직
│   └── SSE                         # SSE 연결
└── global
    ├── aspect                      # AOP 공통 로직 (로깅/응답 등)
    ├── aws                         # AWS 관련 유틸리티
    ├── clients                     # 외부 API 클라이언트
    ├── config                      # 각종 환경 설정(JWT, ElasticSearch, Redis, RabbitMQ 등)
    ├── exception                   # 글로벌 예외 처리
    ├── headlessBrowser             # 크롤링용 헤드리스 브라우저
    ├── initData                    # 테스트용 초기 데이터
    ├── jpa                         # 공용 엔티티
    ├── rsData                      # RsData 응답 객체
    ├── security                    # Spring Security
    ├── springDoc                   # OpenAPI/Swagger & 문서화
    ├── test                        # 모니터링 테스트
    └── webMvc                      # 공통 WebMVC 설정
```

---

## 🔌설치 및 실행

```
# 1. 환경 설정
# application-secrets.yml.template를 참고하여 application-secrets.yml을 작성합니다.

# 2. 의존성 설치
./gradlew build

# 3. RabbitMQ, Elastic Search 컨테이너 실행
# 별도로 로컬 환경에 Redis가 설치되어 있어야 합니다.
docker compose up -d
docker ps // 정상적으로 실행 중인지 확인.

# 4. 로컬 서버 실행
java -jar build/libs/backend-0.0.1-SNAPSHOT.jar

# 5. 정상 작동 확인
# API 문서: http://localhost:8080/swagger-ui.html
```

---

## 📑추가 자료
[**백엔드 개발 컨벤션**](./DEV_GUIDE.md)
