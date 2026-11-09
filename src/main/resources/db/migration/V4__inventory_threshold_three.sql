-- User decision 2026-11-03 supersedes the PDF threshold of four.
-- Reclassify stored status without changing quantities or deleting records.
UPDATE `물품재고`
SET `재고상태` = CASE
    WHEN `재고수량` = 0 THEN 'OUT_OF_STOCK'
    WHEN `재고수량` <= 3 THEN 'LOW_STOCK'
    ELSE 'NORMAL'
END;
