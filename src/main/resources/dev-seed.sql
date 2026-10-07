-- 개발용 예시 데이터. MySQL에서 수동으로 실행한다.
-- 가격과 옵션 추가 금액은 성인 1인 기준 원화다.
-- 현재 여행상품 테이블에 목적지/기간 컬럼이 없어 상품명과 설명에 표시한다.
-- 실제 예약, 회원, 재고, 문자 발송 내역은 생성하지 않는다.

START TRANSACTION;

INSERT INTO `여행상품` (`상품명`, `테마`, `상품설명`, `클래식_가격`, `그랜드_가격`, `프리미엄_가격`)
SELECT '푸껫 허니문 5박 7일', 'HONEYMOON',
       '인천 출발 푸껫 허니문. 왕복 항공, 숙박, 공항 이동, 기본 식사와 현지 투어 포함. 클래식 등급은 제공하지 않음.',
       NULL, 2190000, 2790000
WHERE NOT EXISTS (SELECT 1 FROM `여행상품` WHERE `상품명` = '푸껫 허니문 5박 7일');

INSERT INTO `여행상품` (`상품명`, `테마`, `상품설명`, `클래식_가격`, `그랜드_가격`, `프리미엄_가격`)
SELECT '다낭 효도·휴양 3박 5일', 'FILIAL',
       '인천 출발 다낭·호이안 효도 여행. 왕복 항공, 숙박, 공항 이동, 기본 식사와 관광 포함. 클래식 등급은 제공하지 않음.',
       NULL, 1290000, 1790000
WHERE NOT EXISTS (SELECT 1 FROM `여행상품` WHERE `상품명` = '다낭 효도·휴양 3박 5일');

INSERT INTO `여행상품` (`상품명`, `테마`, `상품설명`, `클래식_가격`, `그랜드_가격`, `프리미엄_가격`)
SELECT '홋카이도 골프 3박 4일', 'GOLF',
       '인천 출발 홋카이도 골프 여행. 왕복 항공, 숙박, 공항 이동, 기본 식사와 36홀 라운드 포함.',
       1290000, 1790000, 2290000
WHERE NOT EXISTS (SELECT 1 FROM `여행상품` WHERE `상품명` = '홋카이도 골프 3박 4일');

INSERT INTO `여행상품` (`상품명`, `테마`, `상품설명`, `클래식_가격`, `그랜드_가격`, `프리미엄_가격`)
SELECT '호도협 트레킹 4박 5일', 'TREKKING',
       '인천 출발 중국 호도협 트레킹. 왕복 항공, 숙박, 현지 이동, 기본 식사와 현지 가이드 포함.',
       1590000, 1990000, 2390000
WHERE NOT EXISTS (SELECT 1 FROM `여행상품` WHERE `상품명` = '호도협 트레킹 4박 5일');

-- 해외 일정의 숙박일수는 출발일과 귀국일의 날짜 차이와 다를 수 있다.
INSERT INTO `여행일정` (`여행상품_ID`, `출발일`, `종료일`, `여행상태`, `현재신청인원`)
SELECT p.`여행상품_ID`, s.departure_date, s.arrival_date, 'RECRUITING', 0
FROM `여행상품` AS p
JOIN (
    SELECT '푸껫 허니문 5박 7일' AS product_name, DATE('2027-03-01') AS departure_date, DATE('2027-03-07') AS arrival_date
    UNION ALL SELECT '푸껫 허니문 5박 7일', DATE('2027-04-05'), DATE('2027-04-11')
    UNION ALL SELECT '다낭 효도·휴양 3박 5일', DATE('2027-03-15'), DATE('2027-03-19')
    UNION ALL SELECT '다낭 효도·휴양 3박 5일', DATE('2027-04-19'), DATE('2027-04-23')
    UNION ALL SELECT '홋카이도 골프 3박 4일', DATE('2027-03-22'), DATE('2027-03-25')
    UNION ALL SELECT '홋카이도 골프 3박 4일', DATE('2027-04-26'), DATE('2027-04-29')
    UNION ALL SELECT '호도협 트레킹 4박 5일', DATE('2027-03-29'), DATE('2027-04-02')
    UNION ALL SELECT '호도협 트레킹 4박 5일', DATE('2027-05-03'), DATE('2027-05-07')
) AS s ON s.product_name = p.`상품명`
WHERE NOT EXISTS (
    SELECT 1 FROM `여행일정` AS existing
    WHERE existing.`여행상품_ID` = p.`여행상품_ID`
      AND existing.`출발일` = s.departure_date
      AND existing.`종료일` = s.arrival_date
);

-- 옵션등급이 NULL인 예시는 해당 상품에서 판매하는 모든 등급에서 선택 가능하다는 개발용 규칙이다.
-- 추가금액은 기본 상품 가격에 포함되지 않은 선택 사항이다.
INSERT INTO `여행옵션` (`여행상품_ID`, `옵션종류`, `옵션명`, `옵션등급`, `추가금액`, `옵션설명`)
SELECT p.`여행상품_ID`, o.option_type, o.option_name, NULL, o.additional_price, o.description
FROM `여행상품` AS p
JOIN (
    SELECT '푸껫 허니문 5박 7일' AS product_name, 'HOTEL' AS option_type, '오션뷰 객실 업그레이드' AS option_name, 180000 AS additional_price, '전체 숙박 기간 기준 1인 추가 금액' AS description
    UNION ALL SELECT '푸껫 허니문 5박 7일', 'EXTRA', '커플 스파 1회 추가', 120000, '기본 일정 외 커플 스파 1회'
    UNION ALL SELECT '다낭 효도·휴양 3박 5일', 'TRANSPORT', '전용 차량 이동', 130000, '현지 기본 단체 이동을 전용 차량으로 변경'
    UNION ALL SELECT '다낭 효도·휴양 3박 5일', 'MEAL', '해산물 특식 1회 추가', 70000, '기본 식사 외 특식 1회'
    UNION ALL SELECT '홋카이도 골프 3박 4일', 'EXTRA', '골프 18홀 추가', 250000, '기본 36홀 외 18홀 추가'
    UNION ALL SELECT '홋카이도 골프 3박 4일', 'EXTRA', '골프 클럽 렌탈', 90000, '여행 기간 중 골프 클럽 대여'
    UNION ALL SELECT '호도협 트레킹 4박 5일', 'EXTRA', '트레킹 장비 대여', 50000, '트레킹 폴 및 기본 장비 대여'
    UNION ALL SELECT '호도협 트레킹 4박 5일', 'MEAL', '현지 특식 1회 추가', 60000, '기본 식사 외 특식 1회'
) AS o ON o.product_name = p.`상품명`
WHERE NOT EXISTS (
    SELECT 1 FROM `여행옵션` AS existing
    WHERE existing.`여행상품_ID` = p.`여행상품_ID`
      AND existing.`옵션종류` = o.option_type
      AND existing.`옵션명` = o.option_name
);

COMMIT;
