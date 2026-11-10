package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.SQLException;

public class V9__application_sms_notification extends BaseJavaMigration {
    private static final String ADD_CHECK = """
        ADD CONSTRAINT chk_sms_notification_type
        CHECK (`알림종류` IN ('APPLICATION_COMPLETED', 'DEPARTURE_CONFIRMED', 'SCHEDULE_CANCELLED'))
        """;

    @Override
    public void migrate(Context context) throws SQLException {
        var connection = context.getConnection();
        boolean mysql = connection.getMetaData().getDatabaseProductName().equals("MySQL");
        String schema = mysql ? connection.getCatalog() : connection.getSchema();
        String oldCheck = findOriginalTypeCheck(connection, schema);
        try (var statement = connection.createStatement()) {
            if (mysql) {
                statement.execute("ALTER TABLE `문자알림` DROP CHECK " + oldCheck + ", " + ADD_CHECK);
            } else {
                statement.execute("ALTER TABLE `문자알림` DROP CONSTRAINT " + oldCheck);
                statement.execute("ALTER TABLE `문자알림` " + ADD_CHECK);
            }
        }
    }

    private String findOriginalTypeCheck(Connection connection, String schema) throws SQLException {
        // V1 used an unnamed CHECK, so its generated name differs across MySQL and H2.
        try (var statement = connection.prepareStatement("""
            SELECT tc.CONSTRAINT_NAME
            FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc
            JOIN INFORMATION_SCHEMA.CHECK_CONSTRAINTS cc
              ON cc.CONSTRAINT_SCHEMA = tc.CONSTRAINT_SCHEMA AND cc.CONSTRAINT_NAME = tc.CONSTRAINT_NAME
            WHERE tc.TABLE_SCHEMA = ? AND tc.TABLE_NAME = '문자알림'
              AND cc.CHECK_CLAUSE LIKE '%DEPARTURE_CONFIRMED%'
              AND cc.CHECK_CLAUSE LIKE '%SCHEDULE_CANCELLED%'
            """)) {
            statement.setString(1, schema);
            try (var checks = statement.executeQuery()) {
                if (!checks.next()) throw new SQLException("Original SMS notification type check was not found");
                String name = checks.getString(1);
                if (checks.next()) throw new SQLException("Multiple SMS notification type checks were found");
                return "`" + name.replace("`", "``") + "`";
            }
        }
    }
}
