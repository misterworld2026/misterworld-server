package db.migration;

import java.sql.Connection;
import java.util.ArrayList;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V14__payment_inventory_deductions extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        var connection = context.getConnection();
        boolean h2 = connection.getMetaData().getDatabaseProductName().equals("H2");
        try (var sql = connection.createStatement()) {
            sql.execute("ALTER TABLE `재고차감내역` ADD COLUMN `여행신청_ID` BIGINT");
            sql.execute("ALTER TABLE `재고차감내역` ADD CONSTRAINT `fk_inventory_deduction_application` "
                + "FOREIGN KEY (`여행신청_ID`) REFERENCES `여행신청` (`여행신청_ID`)");
            sql.execute("CREATE UNIQUE INDEX `uk_inventory_deduction_application_item` "
                + "ON `재고차감내역` (`여행신청_ID`, `물품재고_ID`)");
            sql.execute("CREATE INDEX `idx_inventory_deduction_schedule` ON `재고차감내역` (`여행일정_ID`)");
            sql.execute("ALTER TABLE `재고차감내역` DROP " + (h2 ? "CONSTRAINT" : "INDEX")
                + " `uk_inventory_deduction_schedule_item`");
            sql.execute("""
                UPDATE `여행일정` s SET `현재신청인원` = (
                    SELECT COALESCE(SUM(a.`신청인원`), 0)
                    FROM `여행신청` a
                    WHERE a.`여행일정_ID` = s.`여행일정_ID`
                      AND a.`신청상태` <> 'CANCELLED' AND a.`결제상태` = 'PAID'
                )
                """);
        }
        migrateActiveDeductions(connection);
    }

    private record LegacyDeduction(long id, long scheduleId, long itemId, int quantity) {}

    private void migrateActiveDeductions(Connection connection) throws Exception {
        var legacy = new ArrayList<LegacyDeduction>();
        try (var sql = connection.createStatement(); var rows = sql.executeQuery("""
            SELECT d.`재고차감내역_ID`, d.`여행일정_ID`, d.`물품재고_ID`, d.`차감수량`
            FROM `재고차감내역` d JOIN `여행일정` s ON s.`여행일정_ID` = d.`여행일정_ID`
            WHERE d.`복구일시` IS NULL
              AND s.`여행상태` IN ('RECRUITING_SCHEDULE', 'CLOSED_SCHEDULE', 'CONFIRMED_SCHEDULE')
            """)) {
            while (rows.next()) legacy.add(new LegacyDeduction(rows.getLong(1), rows.getLong(2),
                rows.getLong(3), rows.getInt(4)));
        }
        // Old entries have no reservation ID. Assign already deducted stock to paid bookings
        // in booking order, and return the remaining stock before processing new payments.
        try (var paid = connection.prepareStatement("""
                 SELECT `여행신청_ID`, `신청인원` FROM `여행신청`
                 WHERE `여행일정_ID` = ? AND `신청상태` <> 'CANCELLED' AND `결제상태` = 'PAID'
                 ORDER BY `여행신청_ID`
                 """);
             var allocate = connection.prepareStatement("""
                 INSERT INTO `재고차감내역` (`여행일정_ID`, `물품재고_ID`, `차감수량`, `여행신청_ID`)
                 VALUES (?, ?, ?, ?)
                 """);
             var release = connection.prepareStatement("""
                 UPDATE `물품재고` SET
                   `재고상태` = CASE WHEN `재고수량` + ? <= 4 THEN 'LOW_STOCK' ELSE 'NORMAL' END,
                   `재고수량` = `재고수량` + ?,
                   `수정일시` = CURRENT_TIMESTAMP
                 WHERE `물품재고_ID` = ?
                 """);
             var retire = connection.prepareStatement("""
                 UPDATE `재고차감내역` SET `복구일시` = CURRENT_TIMESTAMP WHERE `재고차감내역_ID` = ?
                 """)) {
            for (var entry : legacy) {
                int remaining = entry.quantity();
                paid.setLong(1, entry.scheduleId());
                try (var rows = paid.executeQuery()) {
                    while (remaining > 0 && rows.next()) {
                        int quantity = Math.min(remaining, rows.getInt(2));
                        if (quantity == 0) continue;
                        allocate.setLong(1, entry.scheduleId());
                        allocate.setLong(2, entry.itemId());
                        allocate.setInt(3, quantity);
                        allocate.setLong(4, rows.getLong(1));
                        allocate.executeUpdate();
                        remaining -= quantity;
                    }
                }
                if (remaining > 0) {
                    release.setInt(1, remaining);
                    release.setInt(2, remaining);
                    release.setLong(3, entry.itemId());
                    release.executeUpdate();
                }
                retire.setLong(1, entry.id());
                retire.executeUpdate();
            }
        }
    }
}
