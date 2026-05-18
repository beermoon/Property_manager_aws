# Property Manager 프로젝트 리뷰

> 학습자 관점에서, "왜 그런지"까지 풀어 설명하는 코드 리뷰입니다.
> 부산 해운대 같은 임대 매물을 관리하는 Spring Boot 웹 애플리케이션이군요. 흐름과 의도가 잘 보이는 잘 짠 1인 프로젝트예요. 다만 포트폴리오/실서비스 수준으로 끌어올리려면 몇 가지 손볼 부분이 있어요. 아래에서 하나씩 짚어볼게요.

---

## 1. 한눈에 보는 프로젝트 구조

```
kr.co.choi.property_manager
├── PropertyManagerApplication.java   ← 진입점 (@SpringBootApplication)
├── config/         ← 설정 (정적 리소스 매핑)
├── controller/     ← 웹 요청 진입점
│   └── dto/        ← 요청/응답 데이터 객체
├── domain/         ← JPA 엔티티 + Enum (도메인 모델)
├── infra/          ← 외부 API 클라이언트 (네이버 지오코딩)
├── repository/     ← JPA Repository + Specification
│   └── specs/      ← 동적 검색 조건
└── service/        ← 파일 저장 서비스
```

전형적인 **계층형 아키텍처(Layered Architecture)** 입니다.

| 계층 | 역할 | 비유 |
|------|------|------|
| Controller | HTTP 요청을 받고 응답 | 식당의 홀 서버 |
| Service | 비즈니스 로직 처리 | 주방장 |
| Repository | DB 입출력 | 식자재 창고 담당 |
| Domain (Entity) | 데이터 + 도메인 규칙 | 음식 그 자체 |

**왜 이렇게 나눌까?**
각 계층이 하나의 책임만 갖게 하면, 한 부분을 고쳐도 다른 부분이 망가지지 않아요(이걸 "관심사의 분리"라고 해요). 지금 프로젝트는 이 구조를 잘 따르고 있지만, **Service 계층이 비어있다시피 한 점**은 뒤에서 다시 짚을게요.

---

## 2. 잘한 점 (Keep it up!)

### 2-1. JPA 양방향 연관관계 + 편의 메서드

`Property.java` 안에서 이렇게 했죠.

```java
public void addMemo(Memo memo) {
    this.memos.add(memo);
    memo.setProperty(this);
}
```

**왜 중요한가요?**
JPA에서 양방향 연관관계를 다룰 때, 두 객체가 서로를 알고 있어야 일관성이 유지돼요. `memos.add()`만 하고 `memo.setProperty()`를 빠뜨리면, DB에는 잘 저장되더라도 같은 트랜잭션 안에서 양쪽이 어긋난 상태로 동작할 수 있어요. 이 패턴을 **"연관관계 편의 메서드"** 라고 하는데, JPA 책에서 강조하는 부분을 정확히 적용했어요.

### 2-2. 금액 단위를 영리하게 분리

`Property`에 보면:

```java
private Long toWon(Long man) {
    return man == null ? null : man * 10_000;
}
// ...
this.deposit = toWon(req.getDeposit());
```

그리고 화면에 보여줄 때는 `getDepositMan()`으로 다시 만원 단위로 돌려줘요.

**왜 좋은가요?**
- DB에는 항상 **원** 단위로 저장 → 정렬·범위 검색이 정확해요
- 사용자 입력/표시는 **만원** 단위 → "5,000만원"처럼 자연스럽게 보여요

만약 DB에 그냥 "만원 단위 정수"로 넣어버렸다면, 나중에 단위를 바꾸려 할 때 마이그레이션 지옥에 빠지죠. 단위가 명확한 경계에서만 변환되도록 한 게 좋은 설계 결정이에요.

### 2-3. Specification 패턴으로 동적 검색 구현

`PropertySpecs.java`에서 검색 조건 하나하나를 `Specification<Property>`로 만들고, 컨트롤러에서 `.and()`로 조립해요.

```java
var spec = Specification.where(PropertySpecs.keywordContains(keyword))
        .and(PropertySpecs.regionEq(region))
        .and(PropertySpecs.dealTypeEq(dealType))
        // ...
```

**왜 좋은가요?**
필터가 15개쯤 되는데, 각 조합마다 `@Query` 메서드를 만드는 건 비현실적이에요. Specification은 JPA Criteria API를 좀 더 깔끔하게 쓰게 해줘서, **"있으면 조건 추가, 없으면 무시"** 흐름(`null`이면 `cb.conjunction()` 반환)을 자연스럽게 만들 수 있어요.

### 2-4. UUID로 업로드 파일명 충돌 방지

`FileStorageService.store()`:

```java
String storedName = UUID.randomUUID() + ext;
```

**왜 이렇게?**
사용자가 `사진.jpg`라는 같은 이름의 파일을 두 명이 동시에 올리면? 원본 이름 그대로 저장하면 덮어쓰여요. UUID로 바꾸면 충돌 확률이 사실상 0이 돼요. 그리고 원본 이름은 따로 `originalName`에 저장해 두니, 나중에 다운로드 시켜줄 때 원래 이름 그대로 내려보낼 수도 있어요.

### 2-5. Profile별 설정 분리 + .gitignore 처리

`application.yml`(공통) / `application-local.yml`(개발) / `application-prod.yml`(운영)로 나누고, 로컬·운영 설정은 `.gitignore`에 넣어놨어요.

**왜 좋은가요?**
운영 환경의 DB 비밀번호나 API 키가 Git에 올라가면 큰일이죠. 환경별 설정 분리는 12-factor app의 기본 원칙 중 하나예요. (다만 키 노출 관련해서는 뒤에서 빨간불을 더 켤게요.)

---

## 3. 꼭 고쳐야 할 점 (보안)

여긴 학습자에게 가장 중요한 부분이에요. **포트폴리오에 올린다면 반드시 손봐야 합니다.**

### 3-1. 비밀번호/API 키 평문 노출

`application-prod.yml`에 이렇게 들어가 있어요.

```yaml
spring:
  datasource:
    password: 3772     # 평문!
naver:
  geocoding:
    key: c7FRp9eB5zXLlO7yYP9zNKPXorjqOf9L08DSwHZ4   # 평문!
```

**왜 위험한가요?**
1. `.gitignore`에 등록은 했지만, **이미 한 번이라도 커밋한 적이 있다면 Git 히스토리에 영원히 남아있어요.** `git log --all`로 누구든 볼 수 있죠.
2. 노트북을 잃어버리거나, 실수로 화면을 공유하면 바로 유출이에요.

**고치는 방향:**
- 환경 변수로 분리: `password: ${DB_PASSWORD}` 후 EC2의 환경 변수에 실제 값 주입
- 또는 AWS Parameter Store / Secrets Manager 같은 비밀 관리 서비스 사용
- 이미 노출된 키는 **반드시 폐기(rotate)**: 네이버 클라우드 콘솔에서 키 재발급, DB 비밀번호 변경
- Git 히스토리에서도 지워야 한다면 `git filter-repo` 같은 도구 사용

### 3-2. 매물에 평문 비밀번호 저장 — 매우 위험

`Property` 엔티티에 이런 필드가 있어요:

```java
private String entrancePassword;    // 공동현관 비밀번호
private String housePassword;       // 집 비밀번호
```

**왜 심각한가요?**
- DB가 한 번이라도 유출되면 **타인의 집 비밀번호**가 그대로 노출돼요. 이건 단순 정보 유출이 아니라 **물리적 침입 가능성**이라 법적으로도 매우 무거운 사안이에요(개인정보보호법 위반 + 형사책임 가능성).
- 백업 파일, 로그, 개발자 화면 공유 등 노출 경로가 너무 많아요.

**고치는 방향:**
- 정말 꼭 필요한 정보인지 다시 생각해보세요. (대안: "비밀번호는 별도 메모로 보관" 안내)
- 굳이 저장한다면 **대칭키 암호화** (AES-256-GCM 등)로 저장. Spring의 `Jasypt` 또는 직접 `javax.crypto` 사용.
- 마스터키는 환경 변수에서.
- DB가 SQLite/PostgreSQL인 만큼, DB 단의 암호화(TDE)는 사실상 어려우니 애플리케이션 레이어 암호화가 현실적이에요.

### 3-3. 인증/인가가 전혀 없음

지금은 **누구나** `/properties/{id}/delete` POST를 보내면 매물이 지워져요. `/properties/new`로 등록도 자유롭게 되고요.

**왜 문제일까요?**
이 앱은 부동산 중개사(또는 개인 임대인) 한 사람이 쓰는 도구예요. **소유자만 접근**해야 하는데, 그 보호장치가 0이에요.

**고치는 방향:**
- `spring-boot-starter-security` 추가
- 가장 간단한 시작점: Basic Auth + 단일 사용자 계정
- 좀 더 나아가면: OAuth2(Google 로그인) 또는 폼 로그인 + DB 사용자 테이블
- CSRF 토큰도 함께 활성화

### 3-4. 파일 업로드 검증 부재

`FileStorageService.store()`에서 어떤 파일이든 다 받아요.

```java
String ext = "";
int idx = original.lastIndexOf('.');
if (idx > -1) ext = original.substring(idx);
// ... 그대로 저장
```

**왜 문제인가요?**
- 누군가 `.jsp`, `.html`, 또는 실행 가능한 `.sh`를 올린 뒤, 정적 매핑된 `/uploads/xxx.jsp`로 접근하면? 서버 톰캣 설정에 따라 코드 실행으로 이어질 수 있어요.
- 100GB짜리 파일을 올리면? (현재 `application.yml`에서 `max-file-size: 20MB`로 1차 방어는 되지만, 검증은 더 빡빡해야 해요.)

**고치는 방향:**
- 허용 확장자 화이트리스트: `jpg, jpeg, png, webp, heic`
- MIME 타입까지 검증 (`file.getContentType()` 확인 + 실제 파일 매직 넘버 확인)
- 이미지라면 `ImageIO.read()`로 진짜 이미지인지 검증
- 저장 디렉토리는 정적 리소스 서빙에서 **스크립트 실행이 절대 안 되도록** 설정

### 3-5. Path Traversal 가능성

`FileStorageService.deleteByUrl()`을 보면:

```java
String filename = url.replaceFirst("^/(upload|uploads)/", "");
Path path = rootDir.resolve(filename).normalize();
Files.deleteIfExists(path);
```

**왜 위험할까요?**
지금은 DB의 url을 그대로 받아서 안전해 보이지만, 만약 누군가 url 필드를 조작할 수 있게 되면(다른 경로로 들어오는 입력) `../../etc/passwd` 같은 값으로 다른 파일을 지울 수 있어요.

**고치는 방향:**
```java
Path path = rootDir.resolve(filename).normalize();
if (!path.startsWith(rootDir)) {
    throw new SecurityException("허용되지 않는 경로");
}
```
`normalize()` 이후 `rootDir` 안에 있는지 검사하는 한 줄을 꼭 추가하세요.

---

## 4. 구조적으로 개선할 점

### 4-1. Service 계층의 부재 — Controller가 너무 많은 일을 함

`PropertyController.create()`를 한번 보세요. 65줄 안에 이런 일들이 다 들어 있어요.
- 유효성 검증 (지번주소 비었는지)
- 외부 API 호출 (네이버 지오코딩)
- 엔티티 생성 + 값 세팅
- 좌표 저장
- DB 저장
- 사진 업로드 루프
- 예외 처리 + 폼 재렌더링

**왜 문제인가요?**
- **테스트하기 어려워요.** Controller 테스트는 HTTP 레이어를 같이 띄워야 해서 무겁고 느려요. Service로 분리하면 순수 단위 테스트가 가능해져요.
- **재사용이 안 돼요.** 만약 CSV 일괄 등록 기능을 만들고 싶다면, 같은 로직을 또 짜야 해요.
- **트랜잭션 경계가 애매해져요.** (다음 항목 참고)

**고치는 방향 (예시):**
```java
@Service
public class PropertyService {
    @Transactional
    public Long createWithPhotos(PropertyCreateRequest req, List<MultipartFile> photos) {
        // 1. 검증
        // 2. 지오코딩
        // 3. 엔티티 생성 + 저장
        // 4. 사진 저장
        // 5. ID 반환
    }
}
```
컨트롤러는 입력을 받고 → 서비스 호출 → 결과로 분기하는 얇은 layer가 되어야 해요.

### 4-2. 트랜잭션 경계가 명시되어 있지 않음

지금 `@Transactional`이 코드 어디에도 없어요. Spring Data JPA의 기본값(메서드 단위 트랜잭션)만으로 돌아가고 있죠.

**시나리오:** 매물 등록 시 매물은 저장됐는데 사진 5장 중 3장째에서 IOException이 터지면? 매물은 남고 사진 일부는 저장된 어정쩡한 상태가 돼요.

**고치는 방향:** 위처럼 `PropertyService.createWithPhotos`를 `@Transactional`로 묶어요. **다만 파일 시스템 작업은 트랜잭션 롤백 대상이 아니에요** — DB 트랜잭션 롤백 시 디스크 파일은 따로 정리해주는 보상 로직이 필요해요.

### 4-3. N+1 문제 (조용한 시한폭탄)

`PropertyController.list()`에서 모든 매물을 조회한 뒤, 템플릿에서 `property.photos`, `property.memos`를 돌리면 어떻게 될까요?

JPA의 LAZY 로딩 때문에:
- 매물 100개 조회 → 1번 쿼리
- 각 매물의 photos 접근 → 100번 추가 쿼리
- 각 매물의 memos 접근 → 100번 추가 쿼리
- 합계 **201번** DB 왕복

이걸 **N+1 문제**라고 해요. 매물이 1,000개가 되면 즉시 느려져요.

**고치는 방향:**
- 목록 화면이 사진/메모를 진짜 필요로 하나? 안 쓰면 그대로 두기 (지금 OK일 수도)
- 쓴다면: `@EntityGraph` 또는 fetch join으로 한 번에 가져오기
```java
@EntityGraph(attributePaths = {"photos"})
List<Property> findAll(Specification<Property> spec);
```

> 지금 `list.html`을 잠깐 봤을 때는 사진 썸네일을 안 쓰는 것 같으니, 당장은 문제 없지만 **알고는 있어야** 해요.

### 4-4. DDL-auto: update — 운영에서 위험

```yaml
jpa:
  hibernate:
    ddl-auto: update
```

**왜 위험할까요?**
`update`는 엔티티 변경에 맞춰 자동으로 스키마를 바꿔주지만, **컬럼을 못 지우고**, **타입 변경도 예측 불가**해요. 그리고 가장 무서운 건, 개발자가 "어떻게 바뀐 건지 모르는 채" 운영 DB가 바뀌는 거예요.

**고치는 방향:**
- 운영은 `validate`로 (엔티티-DB 불일치 시 부팅 실패시킴)
- 스키마 변경은 **Flyway** 또는 **Liquibase**로 관리 (마이그레이션 파일 = 코드 = Git 추적)

---

## 5. 자잘하지만 신경 쓰이는 것들

### 5-1. 오타들

- `HomeContorller.java` → `HomeController` (Controller의 r 위치)
- `PropertyType.ThREE_ROOM` → `THREE_ROOM` (대문자 일관성)

오타 자체보다 더 신경 쓰이는 건, IDE가 분명히 빨간줄로 알려줬을 텐데 그대로 두셨다는 점이에요. 작은 신호도 무시하지 않는 습관을 들여보세요.

### 5-2. DTO 필드가 `public`인데 getter/setter도 있음

`PropertyCreateRequest.java`:

```java
public String title;   // public!
// ...
public String getTitle() { return title; }
public void setTitle(String title) { this.title = title; }
```

**왜 거슬리나요?**
- public 필드와 getter/setter를 동시에 두면, **외부에서 우회 접근**이 가능해져요. 캡슐화가 깨져요.
- `private`로 바꾸면 setter를 통하지 않은 변경이 막혀요.
- Lombok이 이미 의존성에 있으니 `@Getter @Setter` 어노테이션 두 줄이면 코드 200줄이 사라져요. ✨

### 5-3. `System.out.println` 으로 로깅

`FileStorageService.deleteByUrl()`에 이렇게 나와요.

```java
System.out.println("[FILE-DELETE] " + path + " deleted=" + deleted);
```

**왜 안 좋은가요?**
- 운영 환경에서 로그 레벨을 조정할 수 없어요(INFO/WARN/ERROR 구분 불가)
- 로그 포맷이 통일 안 됨 (시간, 클래스명, 스레드 등)
- 비동기 로깅이 안 돼서 느림

**고치는 방향:**
```java
private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);
// ...
log.info("file deleted: path={}, deleted={}", path, deleted);
```

### 5-4. 검증 의존성을 추가만 하고 안 씀

`build.gradle`에 `spring-boot-starter-validation`이 있는데, 정작 `PropertyCreateRequest`에 `@NotBlank`, `@NotNull`, `@Positive` 같은 게 하나도 없어요.

지번주소 검증을 Controller 안에서 if문으로 하고 있는데, 이걸 DTO 어노테이션으로 옮기면 코드가 더 명확해져요.

```java
@NotBlank(message = "지번 주소는 필수입니다.")
private String lotAddress;
```

그리고 컨트롤러에 `@Valid` 붙이기.

### 5-5. 글로벌 예외 처리기가 없음

지금은 컨트롤러 메서드마다 try-catch로 처리하고 있어요. `@ControllerAdvice` + `@ExceptionHandler`로 모아두면 코드가 훨씬 깔끔해져요.

### 5-6. GitHub Actions가 `@master` 태그를 씀

```yaml
- uses: appleboy/scp-action@master
```

**왜 위험할까요?**
`@master`는 항상 최신 코드를 가져와요. 만약 그 액션이 어느 날 악의적으로 바뀌면 → 다음 배포 시 그대로 실행돼요. 공급망 공격(supply chain attack)의 전형적인 경로예요.

**고치는 방향:** 특정 버전 또는 commit SHA로 고정:
```yaml
- uses: appleboy/scp-action@v0.1.7
```

### 5-7. `pkill -f '.jar'` 의 위험성

배포 스크립트에서 이렇게 종료하시는데, EC2에 다른 jar 프로세스가 있다면 그것도 죽어요. 시스템 모니터링 등 다른 자바 프로세스가 실행 중이라면 문제가 될 수 있어요.

**고치는 방향:** PID 파일을 쓰거나 `systemd` 서비스로 관리하는 게 정석이에요.

### 5-8. 테스트가 사실상 없음

`PropertyManagerApplicationTests.java` 한 개뿐이고 이것도 컨텍스트 로드만 확인해요. 포트폴리오 차원에서는 **단위 테스트 몇 개라도** 있는 게 좋아요. 예를 들어:
- `PropertySpecs`의 조건 조합 테스트
- `FileStorageService.store()`가 실제로 파일을 쓰는지 + `@TempDir` 사용
- `Property.toWon` / `getDepositMan` 변환 테스트

---

## 6. 학습 로드맵 제안

지금 단계에서 다음 순서로 보강하면, 이 프로젝트가 **포트폴리오 작품**으로 손색없게 돼요.

1. **보안 1순위 (이번 주)**
   - API 키/DB 비밀번호 환경 변수로 빼기 + 키 폐기·재발급
   - 매물의 평문 비밀번호 필드 제거 또는 암호화
2. **Spring Security 도입 (다음 주)**
   - 우선 폼 로그인 + 단일 사용자로 시작
3. **Service 계층 분리 + @Transactional**
   - `PropertyService` 만들기
   - 컨트롤러 단순화
4. **검증 + 글로벌 예외 처리**
   - DTO에 `@NotBlank` 등 적용
   - `@ControllerAdvice`로 에러 페이지 통일
5. **로깅 정비**
   - SLF4J Logger로 통일
   - `application.yml`에 로그 레벨 설정
6. **DB 마이그레이션 도입**
   - Flyway 추가, `ddl-auto: validate`로 전환
7. **테스트 작성**
   - Service 단위 테스트부터 시작

---

## 7. 마치며

이 프로젝트는 "**혼자서 처음부터 끝까지 만들어본 경험**"이라는 점에서 이미 큰 의미가 있어요. JPA 양방향 매핑, Specification으로 동적 검색, 외부 API 통합, 파일 업로드, Thymeleaf 렌더링, GitHub Actions 배포까지 — 웹 개발의 거의 모든 구간을 직접 거쳐본 거니까요.

다만 지금은 "**돌아가게 만드는** 단계"의 코드예요. 다음 단계는 "**누가 봐도 안전하고 깔끔한** 코드"로 다듬는 거예요. 위 항목들을 하나씩 해결해 나가다 보면 자연스럽게 그 단계로 넘어갈 거예요.

가장 먼저 손대야 할 건 **3-1, 3-2 보안 항목**이에요. 다른 건 다 천천히 해도, 키 노출과 평문 비밀번호는 오늘 해결하세요. 🔐

화이팅이에요!
