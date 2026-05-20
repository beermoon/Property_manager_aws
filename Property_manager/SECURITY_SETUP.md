# 보안 설정 가이드 (Spring Security 5단계)

> 인증 미적용 상태에서는 누구나 `DELETE /api/properties/1`을 보낼 수 있었어요.
> 이 단계에서 **폼 로그인(MVC)** + **HTTP Basic(REST)** + **환경변수 단일 사용자**를
> 도입해 모든 접근에 인증을 강제합니다.

---

## 1. 새로 추가된 환경변수

| 환경변수 | 설명 | 사용 환경 |
|---------|------|----------|
| `APP_USERNAME` | 앱 사용자명 | 로컬 + 운영 |
| `APP_PASSWORD` | 앱 사용자 비밀번호 (평문) | 로컬 + 운영 |

> 환경변수에 평문이 들어가는 게 거슬릴 수 있는데, 부팅 시점에 BCrypt로
> 해시화되어 메모리에 저장됩니다. yml 파일이나 코드에 평문이 박혀있는 것보다
> 훨씬 안전해요. 더 안전하게 하려면 환경변수에 이미 해시된 값을 두고
> SecurityConfig를 거기에 맞게 바꾸면 됩니다 (학습 노트 참고).

---

## 2. 로컬 개발 환경 (IntelliJ)

### 2-1. Run Configuration에 추가

기존 환경변수들과 함께 다음 두 개를 추가해 주세요.

| Name | Value (예시) |
|------|------|
| `APP_USERNAME` | `admin` |
| `APP_PASSWORD` | `MyVeryStrongPasswordHere!` |

> ⚠️ 단순한 비밀번호(`1234`, `password` 등)는 절대 피하세요. 운영에서는
> 20자 이상의 임의 문자열을 권장합니다.

### 2-2. 작동 확인

`Run`으로 띄운 뒤:

1. 브라우저에서 `http://localhost:8080/properties` 접속
2. **로그인 페이지로 리다이렉트** 되면 ✓
3. 환경변수에 넣은 아이디/비밀번호 입력 → 매물 목록 페이지로 이동

API 테스트:
```bash
# 인증 없이 → 401 Unauthorized
curl -i http://localhost:8080/api/properties
# → HTTP/1.1 401

# Basic 인증으로 → 200 OK
curl -i -u admin:MyVeryStrongPasswordHere! http://localhost:8080/api/properties
# → HTTP/1.1 200 OK + JSON
```

### 2-3. 로그아웃

`/logout`으로 POST 요청(Spring Security 기본). 또는 헤더에 로그아웃 링크를
추가해도 좋아요(아직 추가 안 했음 — 다음 단계 자유).

---

## 3. 운영 환경 (GitHub Actions → EC2)

> 워크스페이스에서 `.github/workflows/deploy.yml`이 보이지 않아 직접 수정은
> 하지 못했어요. 아래 내용을 deploy.yml에 추가하시거나, 다른 배포 방식에
> 동일한 환경변수를 주입해 주세요.

### 3-1. GitHub Secrets 추가

저장소 **Settings → Secrets and variables → Actions** 에서:

| Name | Value |
|------|-------|
| `APP_USERNAME` | 운영용 사용자명 |
| `APP_PASSWORD` | 운영용 비밀번호 (강력하게!) |

### 3-2. deploy.yml의 서버 재시작 step에 export 추가

```yaml
script: |
  pkill -f '.jar' || true
  export SPRING_PROFILES_ACTIVE=prod
  export DB_USERNAME='${{ secrets.DB_USERNAME }}'
  export DB_PASSWORD='${{ secrets.DB_PASSWORD }}'
  export NAVER_MAPS_JS_KEY_ID='${{ secrets.NAVER_MAPS_JS_KEY_ID }}'
  export NAVER_GEOCODING_KEY_ID='${{ secrets.NAVER_GEOCODING_KEY_ID }}'
  export NAVER_GEOCODING_KEY='${{ secrets.NAVER_GEOCODING_KEY }}'
  export APP_USERNAME='${{ secrets.APP_USERNAME }}'      # ← 추가
  export APP_PASSWORD='${{ secrets.APP_PASSWORD }}'      # ← 추가
  nohup java -jar /home/ubuntu/app/build/libs/*.jar > app.log 2>&1 &
```

---

## 4. 무엇이 바뀌었나

### 4-1. 보호되는 자원

| URL 패턴 | 인증 방식 | 비고 |
|---------|----------|------|
| `/api/**` | HTTP Basic | stateless, CSRF off |
| `/properties/**`, `/`, 그 외 | 폼 로그인 (세션) | CSRF on |
| `/css/**`, `/js/**` | 비인증 허용 | 정적 리소스 |
| `/upload/**`, `/uploads/**` | 비인증 허용 | 업로드된 이미지 |
| `/login`, `/error` | 비인증 허용 | 로그인 자체 |

### 4-2. 새로 생긴 파일

- `config/SecurityConfig.java` — Security 핵심 설정
- `controller/AuthController.java` — `/login` GET → login.html
- `templates/login.html` — 로그인 폼

### 4-3. 바뀐 파일

- `build.gradle` — `spring-boot-starter-security` 의존성
- `application.yml` — `app.security.username/password` 추가

---

## 5. 작동 확인 체크리스트

- [ ] `APP_USERNAME`, `APP_PASSWORD` 환경변수가 IntelliJ Run Configuration에 등록됨
- [ ] `http://localhost:8080/properties` 접속 시 `/login`으로 리다이렉트
- [ ] 올바른 자격증명으로 로그인 시 매물 목록으로 이동
- [ ] 잘못된 자격증명으로 로그인 시 "아이디 또는 비밀번호가 올바르지 않습니다" 메시지
- [ ] `curl http://localhost:8080/api/properties` → 401
- [ ] `curl -u admin:비밀번호 http://localhost:8080/api/properties` → 200 + JSON
- [ ] GitHub Secrets에 `APP_USERNAME`, `APP_PASSWORD` 등록됨
- [ ] EC2 배포 후 로그인 정상 동작

---

## 6. 자주 묻는 질문

**Q. 왜 폼 로그인과 HTTP Basic을 분리했나요?**
A. 브라우저로 접근할 때는 폼 로그인이 자연스럽고(세션 유지, 로그아웃 가능), 외부 API 호출(curl, 모바일 앱)에서는 매 요청에 자격증명을 넣는 게 표준이에요. 한 앱에 두 종류의 클라이언트가 있으니 인증 방식도 둘로 나눈 거죠.

**Q. `BCryptPasswordEncoder`가 뭔가요?**
A. 비밀번호를 해시화하는 표준 알고리즘이에요. 같은 입력에 대해 매번 다른 해시(salt 포함)를 만들고, 의도적으로 느려서(약 100ms) 무차별 대입 공격(brute force)을 비효율적으로 만들어요. Spring Security가 기본으로 권장하는 인코더입니다.

**Q. 환경변수에 평문 비밀번호 두는 게 안전한가요?**
A. yml 파일이나 코드에 박혀있는 것보다는 안전해요. 환경변수는 기본적으로 해당 프로세스에만 노출되니까요. 더 안전하게 하려면:
1. 환경변수에 이미 BCrypt 해시된 값을 저장
2. `SecurityConfig`에서 `encoder.encode(password)` 대신 그 해시를 직접 사용
이 방식은 메모리에도 평문이 안 떠다녀요. 다만 해시 값을 미리 생성해야 하는 번거로움이 있어요.

**Q. 로그아웃 버튼이 화면에 없는데요?**
A. 아직 안 만들었어요. 헤더에 추가하려면 Thymeleaf에 다음 같은 form을 넣으면 돼요:
```html
<form method="post" th:action="@{/logout}" style="display:inline">
    <button type="submit">로그아웃</button>
</form>
```
CSRF가 켜져있어서 GET이 아닌 POST로 요청해야 해요.

**Q. CSRF가 뭔가요?**
A. Cross-Site Request Forgery — 다른 사이트가 사용자의 세션을 이용해 우리 서버에 요청을 보내는 공격이에요. Spring Security는 폼 제출 시 hidden CSRF 토큰을 검증해서 막아요. Thymeleaf는 form 안에 자동으로 토큰을 주입해 주니 별도 작업 불필요해요.
