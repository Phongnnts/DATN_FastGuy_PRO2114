package service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class ActivityLogDatabaseSourcePolicyTest {
    private static final Path DATABASE = Path.of("../../database");

    @Test
    void activityLogR7HarnessRewritesRuntimeUrlBeforeBackendOrFixtureCommands() throws Exception {
        String harness = Files.readString(Path.of("../../scripts/run-staff-dispatch-real-e2e.ps1"));
        int rewrite = harness.indexOf("Set-DatabaseNameInUrl $env:DB_URL $env:FASTGUY_E2E_DB_NAME");
        assertTrue(rewrite >= 0, "ActivityLogR7 must rewrite DB_URL to the disposable database");
        assertTrue(rewrite < harness.indexOf("& mvn.cmd package"), "DB_URL rewrite must precede backend build");
        assertTrue(rewrite < harness.indexOf("Invoke-Fixture 'seed'"), "DB_URL rewrite must precede fixture seed");
        assertTrue(harness.contains("(?i)(databaseName\\s*=\\s*)[^;?&]+"), "JDBC databaseName variant");
        assertTrue(harness.contains("(?i)(/database/)[^;?&/]+"), "URL path database variant");
    }

    @Test
    void activityLogMigrationAndCanonicalSchemasFollowR7Policy() throws Exception {
        String migration = Files.readString(DATABASE.resolve("migrations/063_activity_log.sql"));
        String validator = Files.readString(DATABASE.resolve("migrations/063_validate.sql"));
        String runbook = Files.readString(DATABASE.resolve("migrations/RUNBOOK.md"));

        for (String source : new String[] {migration, validator}) {
            for (String database : new String[] {"FastGuyDB", "FastGuyDB_ActivityLog063_Test", "FastGuyDB_ActivityLog063_RestoreTest"}) {
                assertTrue(source.contains("N'" + database + "'"), database);
            }
        }
        for (String token : new String[] {
                "063_activity_log", "sys.sp_getapplock", "BEGIN TRANSACTION", "SET XACT_ABORT ON",
                "CREATE TABLE dbo.ActivityLog", "activity_log_id bigint IDENTITY(1,1)",
                "FK_ActivityLog_ActorUser", "CK_ActivityLog_ActionType", "CK_ActivityLog_TargetType",
                "CK_ActivityLog_Summary", "ISJSON(metadata_json)",
                "IX_ActivityLog_CreatedAt", "IX_ActivityLog_ActionType_CreatedAt",
                "IX_ActivityLog_ActorUser_CreatedAt"}) {
            assertTrue(migration.contains(token), token);
        }
        for (String token : new String[] {
                "063_activity_log", "sys.columns", "sys.foreign_keys", "sys.indexes",
                "sys.check_constraints", "is_not_trusted=0",
                "(N'created_at',N'datetime2',0,6,0,0,0)"}) {
            assertTrue(validator.contains(token), token);
        }
        for (String canonical : new String[] {"init.sql", "DB_FastGuy.sql"}) {
            String schema = Files.readString(DATABASE.resolve(canonical));
            assertTrue(schema.contains("CREATE TABLE dbo.ActivityLog"), canonical);
            assertTrue(schema.contains("IX_ActivityLog_ActionType_CreatedAt"), canonical);
        }
        assertTrue(runbook.contains("063_activity_log.sql"));
        assertTrue(runbook.contains("063_validate.sql"));
    }

    @Test
    void operationsFinanceCleanupRemovesActivityLogsBeforeFixtureUsersWhenTableExists() throws Exception {
        String source = Files.readString(Path.of("src/test/java/integration/OperationsFinanceIT.java"));
        int usersPresent = source.indexOf("if(!users.isEmpty())");
        int tableGuard = source.indexOf("if(hasTable(em,\"ActivityLog\"))", usersPresent);
        int activityLogs = source.indexOf("DELETE ActivityLog WHERE actor_user_id IN (:ids)", tableGuard);
        int fixtureUsers = source.indexOf("DELETE Users WHERE user_id IN (:ids)", activityLogs);

        assertTrue(usersPresent >= 0, "cleanup must require fixture users");
        assertTrue(tableGuard > usersPresent, "older baselines must skip missing ActivityLog");
        assertTrue(activityLogs > tableGuard, "cleanup must delete fixture users' ActivityLog rows");
        assertTrue(fixtureUsers > activityLogs, "ActivityLog rows must be deleted before fixture Users");
    }
}
