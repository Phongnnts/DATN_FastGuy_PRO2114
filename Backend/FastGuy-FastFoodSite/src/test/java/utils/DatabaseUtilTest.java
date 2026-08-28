package utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Map;

import org.junit.jupiter.api.Test;

class DatabaseUtilTest {
    @Test
    void sendsTimeParametersAsSqlTime() {
        assertEquals("jdbc:sqlserver://localhost;encrypt=false;sendTimeAsDatetime=false",
                DatabaseUtil.sqlServerTimeUrl("jdbc:sqlserver://localhost;encrypt=false"));
        assertEquals("jdbc:sqlserver://localhost;sendTimeAsDatetime=false",
                DatabaseUtil.sqlServerTimeUrl("jdbc:sqlserver://localhost;sendTimeAsDatetime=false"));
        assertEquals("jdbc:sqlserver://localhost;sendTimeAsDatetime=false;encrypt=false",
                DatabaseUtil.sqlServerTimeUrl("jdbc:sqlserver://localhost;sendTimeAsDatetime=true;encrypt=false"));
    }

    @Test
    void omitsCredentialsForIntegratedSecurity() {
        Map<String, String> overrides = DatabaseUtil.connectionOverrides(
                "jdbc:sqlserver://localhost;IntegratedSecurity=TrUe", AppConfig::getDbUser, AppConfig::getDbPassword);

        assertFalse(overrides.containsKey("jakarta.persistence.jdbc.user"));
        assertFalse(overrides.containsKey("jakarta.persistence.jdbc.password"));
    }

    @Test
    void requiresCredentialsForNormalUrl() {
        Map<String, String> overrides = DatabaseUtil.connectionOverrides(
                "jdbc:sqlserver://localhost", () -> "user", () -> "password");

        assertEquals("user", overrides.get("jakarta.persistence.jdbc.user"));
        assertEquals("password", overrides.get("jakarta.persistence.jdbc.password"));
    }
}
