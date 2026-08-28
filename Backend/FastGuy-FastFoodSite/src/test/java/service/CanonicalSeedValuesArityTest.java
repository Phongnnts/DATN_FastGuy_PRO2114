package service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class CanonicalSeedValuesArityTest {
    @Test
    void everyInitialOrderRowMatchesItsInsertColumnCount() throws Exception {
        String sql = Files.readString(Path.of("../../database/init.sql"));
        int insert = sql.indexOf("INSERT dbo.Orders (order_id");
        int values = sql.indexOf("VALUES", insert);
        String columns = sql.substring(sql.indexOf('(', insert) + 1, sql.indexOf(')', insert));
        String constructor = sql.substring(values + 6, sql.indexOf(';', values));
        int expected = count(columns);
        List<String> rows = rows(constructor);
        assertEquals(8, rows.size());
        for (int i = 0; i < rows.size(); i++) assertEquals(expected, count(rows.get(i)), "Orders row " + (i + 1));
        assertEquals(-1, sql.indexOf("reservation.quantity"));
        assertEquals(-1, sql.indexOf("reservation.variant_id"));
        assertEquals(-1, sql.indexOf("transaction_row.variant_id"));
    }

    private static List<String> rows(String value) {
        List<String> rows = new ArrayList<>();
        int depth = 0, start = -1;
        boolean quoted = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\'' && !(quoted && i + 1 < value.length() && value.charAt(i + 1) == '\'')) quoted = !quoted;
            else if (quoted && c == '\'' && i + 1 < value.length() && value.charAt(i + 1) == '\'') i++;
            else if (!quoted && c == '(') { if (depth++ == 0) start = i + 1; }
            else if (!quoted && c == ')' && --depth == 0) rows.add(value.substring(start, i));
        }
        return rows;
    }

    private static int count(String value) {
        int count = 1, depth = 0;
        boolean quoted = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\'' && quoted && i + 1 < value.length() && value.charAt(i + 1) == '\'') i++;
            else if (c == '\'') quoted = !quoted;
            else if (!quoted && c == '(') depth++;
            else if (!quoted && c == ')') depth--;
            else if (!quoted && depth == 0 && c == ',') count++;
        }
        return count;
    }
}
