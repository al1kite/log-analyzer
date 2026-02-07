## Summary
<!-- 이 PR이 무엇을 변경하는지 한 줄 요약 -->

## Context
<!-- 왜 이 변경이 필요한지 / 배경 -->
- Issue:
- Background:

## Changes
<!-- 변경 사항을 계층별로 간결하게 -->
- Domain:
  - 
- Application:
  - 
- Infrastructure:
  - 
- API:
  - 
- Config / Build:
  - 

---

## Code Convention Checklist
> 본 PR은 프로젝트 코드 컨벤션을 기준으로 검토합니다.  
> 세부 기준은 `docs/CODE_CONVENTION.md`를 따릅니다.

### 1. 객체 생성 & 수명 관리
- [ ] 생성 로직은 생성자 대신 **정적 팩터리 메서드** 또는 **빌더**로 캡슐화했다
- [ ] 매개변수가 많은 객체는 **Builder 패턴**을 사용했다
- [ ] 의존 객체는 **생성자 주입**으로 전달했다
- [ ] 자원 사용 시 **try-with-resources**를 적용했다

### 2. 불변성 & 스레드 안정성
- [ ] 도메인 객체는 **불변(record / @Value)** 으로 설계했다
- [ ] 내부 상태는 외부에서 수정할 수 없도록 보호했다
- [ ] 동시 접근 가능 구조에는 **thread-safe 컬렉션**을 사용했다

### 3. 메서드 계약
- [ ] public 메서드는 **매개변수 유효성 검사**를 수행한다
- [ ] `null` 대신 **Optional 또는 빈 컬렉션**을 반환한다
- [ ] 컬렉션/배열은 **방어적 복사** 또는 불변 래핑을 적용했다
- [ ] 실패 케이스는 **Fail-Fast**로 드러난다

### 4. 계층 분리
- [ ] Domain 레이어는 **외부 의존성(프레임워크/IO)** 이 없다
- [ ] 외부 시스템 연동은 Infrastructure 레이어에 격리했다
- [ ] Application 레이어는 오케스트레이션 책임만 가진다

### 5. 예외 & 로깅
- [ ] 예외 메시지는 **행동 가능한 정보**를 포함한다
- [ ] 성공/실패 로그 레벨을 구분했다
- [ ] 민감 정보는 로그에 남기지 않았다

---

## Behavior Change
<!-- 사용자/클라이언트 관점에서의 동작 변화 -->
- Before:
- After:

## Test
```bash
./gradlew test
./gradlew build
```

## Risk & Rollback
- Risk:
- Rollback: