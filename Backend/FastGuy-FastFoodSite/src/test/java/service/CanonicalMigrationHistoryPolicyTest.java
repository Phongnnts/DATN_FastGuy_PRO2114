package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class CanonicalMigrationHistoryPolicyTest {
    @Test
    void freshCanonicalSchemasIdentifyTheirMigrationBaseline() throws Exception {
        String preflight = Files.readString(Path.of("../../database/migrations/000_preflight_history.sql"));
        String ddl = preflight.substring(preflight.indexOf("CREATE TABLE dbo.SchemaMigrationHistory"), preflight.indexOf("        );", preflight.indexOf("CREATE TABLE dbo.SchemaMigrationHistory")) + 10).lines().map(line -> line.stripLeading()).reduce((a, b) -> a + "\n" + b).orElseThrow();
        for (String file : new String[] { "init.sql", "DB_FastGuy.sql" }) {
            String source = Files.readString(Path.of("../../database/" + file));
            String normalized = source.lines().map(line -> line.stripLeading()).reduce((a, b) -> a + "\n" + b).orElseThrow();
            assertTrue(normalized.contains(ddl), file + " exact history schema");
            for (String id : new String[] { "000_preflight_history", "059_shift_schedule_order_timeout", "060_operating_finance" }) assertEquals(1, occurrences(source, "('" + id + "', N'Canonical fresh schema baseline')"), file + " " + id);
        }
        String init = Files.readString(Path.of("../../database/init.sql"));
        assertTrue(init.contains("('SchemaMigrationHistory')"));
        assertTrue(init.contains("IF @ExpectedTableCount <> 37"));
    }

    private static int occurrences(String source, String token) {
        return (source.length() - source.replace(token, "").length()) / token.length();
    }
}
