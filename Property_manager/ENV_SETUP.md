# 환경변수 설정 가이드

> 보안을 위해 API 키와 DB 비밀번호를 yml 파일에서 분리하고 환경변수로 주입하도록 변경했어요.
> 이 문서는 **로컬 개발 환경**과 **EC2 운영 환경** 두 가지를 모두 다룹니다.

---

## 1. 필요한 환경변수 목록

| 환경변수 | 설명 | 사용 환경 |
|----------|------|----------|
| `NAVER_MAPS_JS_KEY_ID` | 네이버 지도 JS API의 Client ID | 로컬 + 운영 |
| `NAVER_GEOCODING_KEY_ID` | 네이버 지오코딩 API의 Client ID | 로컬 + 운영 |
| `NAVER_GEOCODING_KEY` | 네이버 지오코딩 API의 Client Secret | 로컬 + 운영 |
| `DB_USERNAME` | PostgreSQL 사용자명 (기본값 `property_user`) | 운영 |
| `DB_PASSWORD` | PostgreSQL 비밀번호 | 운영 |

> 로컬은 SQLite를 쓰니까 DB 관련 환경변수는 필요 없어요.

---

## 2. 노출된 키 폐기 — 가장 먼저 해야 할 일

`application-local.yml`과 `application-prod.yml`이 `.gitignore`에 등록돼 있긴 하지만, **과거에 한 번이라도 커밋된 적이 있다면 이미 노출된 상태**예요. Git 히스토리는 영구히 남거든요.

### 2-1. 히스토리 확인

프로젝트 폴더에서 아래 명령을 실행해보세요.

```bash
git log --all --oneline -- src/main/resources/application-local.yml
git log --all --oneline -- src/main/resources/application-prod.yml
```

**결과 해석:**
- 출력이 비어있으면 → 커밋된 적 없음 (다행)
- 커밋 해시가 줄줄이 보이면 → **이미 노출**. 키 폐기 + 히스토리 정리 필요

### 2-2. 노출됐다면 할 일

1. **네이버 클라우드 플랫폼** 콘솔 → API 키 폐기 후 재발급
   - Maps API, Geocoding API 두 가지 모두
2. **EC2의 PostgreSQL 비밀번호 변경**
   ```sql
   ALTER USER property_user WITH PASSWORD '새로운_비밀번호';
   ```
3. **Git 히스토리에서 제거** (선택, 그러나 권장)
   ```bash
   # git filter-repo 설치 (brew install git-filter-repo)
   git filter-repo --path src/main/resources/application-local.yml --invert-paths
   git filter-repo --path src/main/resources/application-prod.yml --invert-paths
   git push origin --force --all
   ```
   > ⚠️ 협업 중이라면 팀원과 협의 후에. 강제 푸시는 모든 클론을 망가뜨려요.

---

## 3. 로컬 개발 환경 설정 (IntelliJ)

IntelliJ Run Configuration에 환경변수를 등록하면 IDE로 실행할 때만 자동으로 주입돼요.

### 3-1. 설정 방법

1. 우측 상단의 **Run/Debug Configuration** 드롭다운 → **Edit Configurations…** 클릭
2. 좌측에서 `PropertyManagerApplication` 선택 (없으면 `+` 버튼 → Application 추가)
3. **Environment variables** 항목 옆 폴더 모양 아이콘 클릭
4. 아래 값들을 추가:

| Name | Value |
|------|-------|
| `NAVER_MAPS_JS_KEY_ID` | `(재발급 받은 새 키 ID)` |
| `NAVER_GEOCODING_KEY_ID` | `(재발급 받은 새 키 ID, 보통 위와 동일)` |
| `NAVER_GEOCODING_KEY` | `(재발급 받은 새 Client Secret)` |

5. **VM options** 또는 **Active profiles**에 `local`이 설정돼 있는지 확인
   - Active profiles 필드에 `local` 입력
   - 또는 VM options에 `-Dspring.profiles.active=local`

### 3-2. 작동 확인

설정 후 `Run` 버튼으로 실행했을 때 콘솔에 다음과 같이 뜨면 성공이에요.

```
The following profiles are active: local
Started PropertyManagerApplication in X seconds
```

만약 다음과 같은 에러가 뜨면 환경변수가 안 들어간 거예요.

```
Could not resolve placeholder 'NAVER_GEOCODING_KEY' in value "${NAVER_GEOCODING_KEY}"
```

→ Run Configuration의 Environment variables를 다시 확인하세요.

### 3-3. gradlew로 실행할 때

터미널에서 `./gradlew bootRun`으로 돌리시려면 환경변수를 셸 세션에 export 해야 해요.
// test
```bash
export NAVER_MAPS_JS_KEY_ID='실제값'
export NAVER_GEOCODING_KEY_ID='실제값'
export NAVER_GEOCODING_KEY='실제값'
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```

매번 export하기 귀찮으면 `~/.zshrc`(또는 `~/.bash_profile`)에 넣어두면 자동으로 적용돼요. 다만 그 파일들이 다른 곳에 백업되지 않도록 주의하세요.

---

## 4. 운영 환경 설정 (GitHub Actions → EC2)

`.github/workflows/deploy.yml`을 수정해서 GitHub Secrets에 등록된 값을 EC2 셸 환경변수로 전달하도록 했어요.

### 4-1. GitHub Secrets 등록

GitHub 저장소 페이지에서:

1. **Settings** → **Secrets and variables** → **Actions**
2. **New repository secret** 버튼
3. 아래 시크릿들을 모두 추가:

| Name | Value |
|------|-------|
| `EC2_HOST` | EC2 인스턴스 IP 또는 도메인 |
| `EC2_USERNAME` | 보통 `ubuntu` |
| `EC2_SSH_KEY` | EC2 접속용 .pem 파일의 **전체 내용** |
| `DB_USERNAME` | DB 사용자명 (예: `property_user`) |
| `DB_PASSWORD` | **재발급 받은 새** DB 비밀번호 |
| `NAVER_MAPS_JS_KEY_ID` | 재발급 받은 새 키 |
| `NAVER_GEOCODING_KEY_ID` | 재발급 받은 새 키 |
| `NAVER_GEOCODING_KEY` | 재발급 받은 새 Client Secret |

### 4-2. 배포 흐름

이제 `main` 브랜치에 push하면 GitHub Actions가:
1. Java 17로 빌드
2. EC2에 jar 업로드
3. 기존 프로세스 종료 (`pkill -f '.jar'`)
4. **환경변수와 함께** 새 jar 실행
5. `SPRING_PROFILES_ACTIVE=prod`로 `application-prod.yml` 적용

### 4-3. 운영 환경에서 직접 실행해야 할 때

EC2에 SSH 접속해서 수동으로 띄울 때도 같은 환경변수가 필요해요. 매번 export하는 게 불편하면 `~/.bashrc`에 추가하거나, **systemd 서비스 파일**(다음 단계에서 다룰 예정)에 `Environment=` 항목으로 박아두는 방식이 정석이에요.

---

## 5. 작동 확인 체크리스트

이 단계를 마친 뒤 아래 항목을 차례로 확인해 보세요.

- [ ] `application-local.yml`과 `application-prod.yml`에 평문 키/비밀번호가 **하나도** 남아있지 않다
- [ ] 노출됐던 네이버 API 키는 **재발급** 받았다
- [ ] 노출됐던 DB 비밀번호는 **변경**했다
- [ ] IntelliJ에서 `Run` 버튼으로 앱이 정상 실행된다
- [ ] 매물 등록 시 지오코딩(주소 → 좌표 변환)이 동작한다
- [ ] GitHub Secrets에 모든 항목이 등록돼 있다
- [ ] 다음 배포(push) 시 EC2에서 앱이 정상 기동된다

---

## 6. 자주 묻는 질문

**Q. 왜 `application-local.yml`도 환경변수로 바꿔야 하나요? 로컬은 어차피 안 보이잖아요?**
A. 가장 큰 이유는 "**실수로 커밋되는 경우**"예요. `.gitignore`에 있어도 `git add -f`로 강제 추가되면 들어가요. 또 노트북을 분실하거나 화면을 공유할 때, 키가 코드 파일에 있는 것과 환경변수에 있는 것은 위험도가 달라요. 코드는 다른 사람에게 보여주는 일이 잦지만, 환경변수는 일부러 보여줘야만 보여요.

**Q. `${DB_USERNAME:property_user}` 이건 무슨 문법이에요?**
A. Spring의 placeholder 문법으로 **"환경변수가 있으면 그 값을, 없으면 기본값 `property_user`를 쓰라"** 는 뜻이에요. DB 사용자명처럼 노출돼도 큰 문제 없는 값은 기본값을 두면 편해요. 비밀번호는 기본값을 두면 안 돼요(없으면 부팅 실패하게 해야 함).

**Q. `.env` 파일 방식이 더 편하지 않나요?**
A. 그것도 좋은 방법이에요. 다만 추가 라이브러리가 필요하고(`spring-dotenv`), 학습 단계에서는 환경변수의 본질을 직접 경험하는 게 더 유익해요. 익숙해진 뒤 도입해도 늦지 않아요.

**Q. AWS Secrets Manager 같은 건 안 쓰나요?**
A. 다음 단계예요. 지금은 "키를 코드에서 분리"가 목표고, 이후 단계에서 "비밀 관리를 전문 서비스에 위임"으로 넘어가면 돼요. 한 번에 다 하면 본질이 흐려져요.
