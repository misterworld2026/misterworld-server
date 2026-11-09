# 6단계 음성 기능 검증

## 실행 방법

저장소 루트에서 Python 의존성을 설치하고 로컬 MySQL을 실행한 뒤 아래 명령을 실행한다.

```bash
cd voice-service
.venv/bin/python -m unittest discover -s tests -v
.venv/bin/python -m evaluation.evaluate
.venv/bin/python -m evaluation.evaluate --cases evaluation/holdout_cases.json
.venv/bin/python -m evaluation.integration_smoke
```

`evaluate`는 실제 Qwen3 모델을 사용하므로 첫 실행 때 모델 다운로드·로딩이 필요하다. `--output /path/report.json`으로 케이스별 결과를 저장할 수 있다. `integration_smoke`는 새 임시 MySQL DB에 `main`의 Flyway 마이그레이션을 적용하고 테스트 상품·일정·옵션 및 회원을 생성한 뒤 Spring과 Python 서버의 공개 API를 호출한다. 테스트가 끝나면 서버와 임시 DB를 정리한다. DB 접속값은 `VOICE_TEST_DB_USER`·`VOICE_TEST_DB_PASSWORD` 또는 `DB_USERNAME`·`DB_PASSWORD`로 지정한다. 정상 전사까지 확인할 WAV 파일이 있으면 `--audio /absolute/path/sample.wav`를 추가한다.

## 판정 기준

- `cases.json`은 구현을 점검하며 사용한 22개 **개발 세트**, `holdout_cases.json`은 별도로 작성한 12개 **새 표현 세트**다.
- 문장 하나에서 투어·일정·등급·호텔·교통·식사의 상태와 값, 추가 옵션 전체가 기대값과 일치해야 1건 성공으로 센다. `resolved`에 허용 표현이 여러 개 있으면 그중 하나면 된다. 기록되지 않은 필드는 `NOT_MENTIONED`여야 한다.
- 성공률은 `성공 문장 수 / 전체 문장 수`다. HTTP 200 비율이나 개별 필드의 평균 정확도가 아니다.
- 이 세트는 **텍스트 해석만** 평가한다. 실제 음성 명령 성공률에는 녹음, 전사, 사용자의 문장 확인, 상품 DB 대조까지 포함해야 한다.

## 2026-11-09 로컬 결과

| 측정 | 결과 | 해석 |
| --- | --- | --- |
| 개발 세트 첫 실행 | 13/22 (59.1%) | JSON 끝의 불필요한 문자와 옵션 분류 오류를 발견했다. |
| 개발 세트 수정 후 | 22/22 (100%) | 동일 문장으로 재측정했으므로 일반화 성능의 증거로 쓰지 않는다. |
| 새 표현 세트 첫 실행 | 10/12 (83.3%) | 한 문장은 호텔 표현을 줄여 쓰고 추가 옵션에 중복 기입했으며, 한 문장은 식사 옵션을 추가 옵션에 중복 기입했다. |

새 표현 세트 측정 뒤 **중복 옵션 제거**를 추가하고 단위 테스트로 확인했다. 그 수정 뒤 모델 전체 재측정은 자동 승인 검토의 사용량 한도로 실행되지 못했다. 따라서 위 10/12는 수정 **전** 버전의 독립 측정값이다.

`integration_smoke`에서 Spring→Python→DB 미리보기, 잘못된 현재 선택, 권한 없는 요청, 손상된 음성 파일, 합성 음성의 정상 전사, 신청 데이터 미생성을 확인했다. 합성 음성은 1.6초짜리 한국어 WAV였으며, 실제 고객 녹음 정확도를 대표하지 않는다. 테스트용 DB는 정리됐고 기존 DB 값은 변경하지 않았다.

## 실제 음성 목표치 평가에 필요한 것

1. 서로 다른 사람이 녹음한 한국어 문장과 정답 선택값을 모은다. 발화자, 녹음 환경, 상품·등급·옵션 종류를 다양하게 구성한다.
2. 각 녹음을 Spring의 `/api/v1/voice/transcriptions`에 보내고, 전사 문장을 사용자가 확인·수정하는 화면 흐름을 기록한다.
3. 확인된 문장을 `/api/v1/voice/interpretations`로 보내 실제 DB 선택 ID와 `fields`, `nextAction`까지 정답과 비교한다.
4. 사전에 정한 문장 단위 성공 기준으로 `성공 건수 / 전체 건수`를 계산한다. 목표는 80%다. 실패 문장과 원인(STT, 해석, 후보 대조, 화면)을 따로 남긴다.

현재 저장소에는 React 프론트엔드가 없으므로 녹음·확인·수정 UI와 실제 고객 녹음 평가는 프론트엔드 코드가 합쳐진 뒤 수행해야 한다.
