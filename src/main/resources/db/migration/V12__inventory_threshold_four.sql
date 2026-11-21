-- Restore the low-stock threshold to four without changing quantities or timestamps.
UPDATE `물품재고`
SET `재고상태` = CASE
    WHEN `재고수량` = 0 THEN 'OUT_OF_STOCK'
    WHEN `재고수량` <= 4 THEN 'LOW_STOCK'
    ELSE 'NORMAL'
END;
