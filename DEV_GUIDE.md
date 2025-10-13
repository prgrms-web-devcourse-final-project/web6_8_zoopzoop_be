# ZoopZoop 백엔드 개발 컨벤션

---

## 1. 기본 규칙

- **언어** : Java
- **변수명 규칙** : camelCase
- **Merge 방식** : Squash & Merge

---

## 2. 이슈 템플릿

### 분류

- `design` : UI 관련 (프론트엔드)
- `fix` : 버그 수정
- `feat` : 기능 추가
- `refactor` : 리팩토링
- `chore` : 문서, 환경 설정 등

### 이슈 네이밍 규칙

- `[분류] 작업 제목`  
- EX: `[feat] 로그인 쿠키 설정`

---

## 3. 지라 티켓

- 레이블은 기존과 동일하게 사용
- 티켓 네이밍 규칙: `[BE] 분류 : 작업 제목`  
- EX: `[BE] feat : 로그인 쿠키 설정`

---

## 4. 브랜치 전략

### 네이밍 규칙

- 형식: `{분류}#{이슈 번호}`  
- EX: `feat#19` (19번 이슈에서 파생된 브랜치)

### 전략

- **main** : 메인 서버 자동 배포, 고정 브랜치
- **develop** : 내부 테스트 서버 자동 배포, 고정 브랜치
- **feature** : 각 기능 개발마다 생성/삭제
- **hotfix** : 긴급 수정 시 생성/삭제

### 지라 연동

- 형식: `분류/지라 디폴트 생성 브랜치명`  
- EX: `feat/OPS-87-FE-필터링`

---

## 5. 커밋 메시지 규칙

- 분류
  - `design` : UI
  - `fix` : 버그 수정
  - `feat` : 기능 추가
  - `refactor` : 리팩토링
  - `chore` : 문서, 환경 설정
  - `docs` : 주석, 문서화 처리
  - `new`  : 새로운 파일 생성

- 복수 성격인 경우 핵심 키워드 하나만 사용
- 지라 연동: `분류/지라 티켓 키 : 내용`  
- EX: `feat/PROJ-123 : implement login service`

---

## 6. PR 템플릿

### 네이밍

- PR 이름: 이슈 이름  
- EX: `[feat] 로그인 쿠키 설정`

### 코드 리뷰

- **develop**: 페어 개발자 검토 (부재 시 팀장 대행), 자동 CI
- **main**: 팀장 주도 확인, 자동 CI/CD
- 지라 연동: `[분류/지라 티켓 키] 이슈 이름`  
  예: `[feat/PROJ-123] 로그인 쿠키 설정`

---

## 7. 티켓 상태 관리

- `Backlog` : 시작 전
- `Ready` : 작업자 지정, 시작 가능
- `In progress` : 개발 중
- `In review` : PR 작성 및 검토 중
- `Done` : 완료

### 작업 순서

1. Issue 생성 → 상태: `Backlog`, Labels/Projects 설정
2. Assignee 지정 → 상태 변경: `Ready`
3. 개발 시작 → 상태: `In progress`, 브랜치 생성
4. 브랜치에서 작업 진행
5. PR 생성 → 상태: `In review`
6. PR 머지 후 → Issue & PR `Done`, 브랜치 삭제

---

## 8. 폴더 구조 규칙

```text
com
└── back
    ├── domain
    │   ├── member
    │   └── team
    │       ├── repository
    │       ├── service
    │       ├── controller
    │       ├── entity
    │       └── dto
    └── global
```
- domain 하위 depth는 1 유지 (필요 시 리팩토링)

---

## 9. DTO 규칙
### 규칙
- Controller 단에서 request/response body와 매칭되는 경우 DTO 사용
- Response body는 최소 정보 전달 (id 등 내부 키값은 숨김)
- Service 단에서는 DTO 재활용 지양
- Controller DTO와 별도 정의
- 필요한 경우 주석 충실히 작성

### 네이밍
- reqBodyFor~ : Request Body DTO
- resBodyFor~ : Response Body DTO

---

## 10. 테스트 코드 컨벤션

- 형식: given-when-then

  - Controller 단에서 단위 테스트 작성
  - Service 핵심 메서드 단위 테스트 작성
  - 예외 케이스, 엣지 케이스 포함

---

## 11. 예외 처리 방식

- 에러 코드: 개발 진행 중 판단

- 핵심 에러는 global 핸들러 사용

---

## 12. 보안 처리

- Spring Security 사용

- Secrets 값 관리

- 개발 환경: application-secret.yml (gitignore)

- 운영 환경: Github Secrets, CI/CD에서 컨테이너 환경 변수로 입력

---

## 13. 문서화 컨벤션

- OpenAPI: Swagger 사용

- Controller, DTO, Entity에 API 어노테이션 충실

- 주석 처리: Javadoc 스타일

```java
/**
 * @param aa
 * @param bb
 * @param cc
 */
```

---

## 14. HTTP 응답 양식
```json
{
  "status": 200,
  "msg": "사용자 정보를 조회했습니다.",
  "data": {
    "name": "$name",
    "profileUrl": "$profileUrl"
  }
}
```
-  성공/실패 관계 없이 RsData 형태로 반환
