# FXNow - 실시간 환율 변환기

실시간 환율 조회, 변환, 차트 분석, 알림 기능을 제공하는 웹 애플리케이션입니다.

## 🏗️ 프로젝트 구조

```
FXNow/
├── frontend/                 # React + TypeScript 프론트엔드
│   ├── src/
│   │   ├── components/       # 재사용 가능한 UI 컴포넌트
│   │   ├── pages/           # 페이지 컴포넌트
│   │   ├── services/        # API 호출 서비스
│   │   ├── types/           # TypeScript 타입 정의
│   │   ├── utils/           # 유틸리티 함수
│   │   ├── App.tsx          # 메인 앱 컴포넌트
│   │   └── main.tsx         # 앱 진입점
│   ├── package.json
│   └── vite.config.ts
├── src/                     # Java + Spring Boot 백엔드
│   ├── main/java/com/txnow/
│   │   ├── api/             # REST API 컨트롤러
│   │   ├── application/     # 애플리케이션 서비스
│   │   ├── domain/          # 도메인 모델
│   │   └── infrastructure/  # 인프라 계층
│   └── test/                # 백엔드 테스트
├── build.gradle             # Gradle 빌드 설정
└── README.md
```

## 🚀 빠른 시작

### 전체 개발 환경 실행

```bash
# 백엔드 실행 (포트 8080)
./gradlew bootRun

# 프론트엔드 실행 (포트 3000)
cd frontend
npm install
npm run dev
```

### 개별 실행

#### 백엔드 (Spring Boot)
```bash
# 의존성 설치 및 빌드
./gradlew build

# 개발 서버 실행
./gradlew bootRun

# 테스트 실행
./gradlew test
```

#### 프론트엔드 (React + Vite)
```bash
cd frontend

# 의존성 설치
npm install

# 개발 서버 실행
npm run dev

# 빌드
npm run build

# 타입 체크
npm run type-check
```

## 🛠️ 기술 스택

### Backend
- **Language**: Java 21
- **Framework**: Spring Boot 3.2
- **Database**: MySQL
- **Cache**: Redis
- **Build Tool**: Gradle

### Frontend
- **Language**: TypeScript
- **Framework**: React 18
- **Build Tool**: Vite
- **Styling**: Tailwind CSS
- **Icons**: Lucide React

## 📋 주요 기능

### ✅ 구현 완료
- [x] 실시간 환율 조회
- [x] 통화 변환 계산기
- [x] 반응형 UI 디자인
- [x] 통화 선택기 (검색 기능 포함)

### 🚧 개발 중
- [ ] 환율 차트 (일/주/월/년)
- [ ] 환율 알림 설정
- [ ] 다중 통화 변환
- [ ] 즐겨찾기 통화

### 📈 향후 계획
- [ ] 사용자 인증
- [ ] 환율 히스토리
- [ ] 모바일 앱
- [ ] API Rate Limiting

## 🔌 API 명세

### 환율 관련 API
```
GET    /api/v1/exchange/rates          # 현재 환율 조회
POST   /api/v1/exchange/convert        # 환율 변환
GET    /api/v1/exchange/history        # 환율 히스토리
GET    /api/v1/currencies              # 지원 통화 목록
```

자세한 API 문서는 [API 명세서](api-specification.md)를 참조하세요.

## 🧪 테스트

```bash
# 백엔드 테스트
./gradlew test

# 프론트엔드 테스트 (예정)
cd frontend
npm test
```

## 📦 배포

### Docker (예정)
```bash
# 전체 스택 실행
docker-compose up -d
```

### 수동 배포
```bash
# 백엔드 빌드
./gradlew build

# 프론트엔드 빌드
cd frontend
npm run build
```

## 🤝 기여 가이드

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다.

## 📞 연락처

프로젝트 관련 문의사항이 있으시면 이슈를 등록해 주세요.

---

**개발 환경 요구사항**
- Java 21+
- Node.js 18+
- MySQL 8.0+
- Redis 7.0+
