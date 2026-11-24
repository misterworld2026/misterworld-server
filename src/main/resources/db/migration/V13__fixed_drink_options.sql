-- Fixed one-unit drink options for the honeymoon and trekking demonstration.
-- Existing options are preserved; these rows are ordinary selectable EXTRA options.
INSERT INTO `여행옵션`
    (`여행상품_ID`, `옵션종류`, `옵션명`, `옵션등급`, `추가금액`, `옵션설명`, `기본등급`, `허용투어등급`, `부가혜택코드`)
SELECT p.`여행상품_ID`, 'EXTRA', '커피 1포트', 'GRAND', 5000,
       '커피 1포트 추가', NULL, 'GRAND,PREMIUM', 'COFFEE'
FROM `여행상품` p
WHERE p.`테마` IN ('허니문', '트레킹')
  AND NOT EXISTS (
      SELECT 1 FROM `여행옵션` o
      WHERE o.`여행상품_ID` = p.`여행상품_ID` AND o.`옵션종류` = 'EXTRA'
        AND o.`옵션명` = '커피 1포트'
  );

INSERT INTO `여행옵션`
    (`여행상품_ID`, `옵션종류`, `옵션명`, `옵션등급`, `추가금액`, `옵션설명`, `기본등급`, `허용투어등급`, `부가혜택코드`)
SELECT p.`여행상품_ID`, 'EXTRA', '샴페인 1병 추가', 'GRAND', 30000,
       '기본 제공분과 별도로 샴페인 1병 추가', NULL, 'GRAND,PREMIUM', 'CHAMPAGNE'
FROM `여행상품` p
WHERE p.`테마` IN ('허니문', '트레킹')
  AND NOT EXISTS (
      SELECT 1 FROM `여행옵션` o
      WHERE o.`여행상품_ID` = p.`여행상품_ID` AND o.`옵션종류` = 'EXTRA'
        AND o.`옵션명` = '샴페인 1병 추가'
  );
