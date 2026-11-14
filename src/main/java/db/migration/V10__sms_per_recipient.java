package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import java.util.*;

public class V10__sms_per_recipient extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        var connection = context.getConnection();
        String table = "\ubb38\uc790\uc54c\ub9bc";
        String application = "\uc5ec\ud589\uc2e0\uccad_ID";
        String type = "\uc54c\ub9bc\uc885\ub958";
        String recipient = "\uc218\uc2e0\ubc88\ud638";
        boolean h2 = connection.getMetaData().getDatabaseProductName().equals("H2");
        // V1 used an unnamed unique constraint. Discover its generated name.
        String query = "SELECT tc.CONSTRAINT_NAME, kcu.COLUMN_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc "
            + "JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE kcu ON tc.CONSTRAINT_NAME=kcu.CONSTRAINT_NAME "
            + "AND tc.TABLE_SCHEMA=kcu.TABLE_SCHEMA AND tc.TABLE_NAME=kcu.TABLE_NAME "
            + "WHERE tc.TABLE_SCHEMA=? AND tc.TABLE_NAME=? AND tc.CONSTRAINT_TYPE='UNIQUE'";
        Map<String, Set<String>> constraints = new LinkedHashMap<>();
        try (var statement = connection.prepareStatement(query)) {
            statement.setString(1, h2 ? connection.getSchema() : connection.getCatalog());
            statement.setString(2, table);
            try (var rows = statement.executeQuery()) {
                while (rows.next()) constraints.computeIfAbsent(rows.getString(1), key -> new HashSet<>()).add(rows.getString(2));
            }
        }
        try (var statement = connection.createStatement()) {
            statement.execute("CREATE UNIQUE INDEX uk_notification_recipient ON `" + table + "` (`" + application + "`, `" + type + "`, `" + recipient + "`)");
            for (var constraint : constraints.entrySet()) {
                if (constraint.getValue().equals(Set.of(application, type))) {
                    statement.execute("ALTER TABLE `" + table + "` DROP " + (h2 ? "CONSTRAINT" : "INDEX")
                        + " `" + constraint.getKey().replace("`", "``") + "`");
                }
            }
        }
    }
}

