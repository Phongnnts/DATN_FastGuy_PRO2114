package utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
