package service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class CanonicalDemoSeedPolicyTest {
    @Test
    void canonicalDemoSeedAndValidatorsCoverApprovedSchemaAndReports() throws Exception {
        String source = Files.readString(Path.of("../../database/init.sql"));
        assertTrue(source.contains("DECLARE @ExpectedTableCount int = (SELECT COUNT(*) FROM @RequiredTables)"));
        assertTrue(source.contains("('OperatingExpense')") && source.contains("('FixedAsset')"));
        assertTrue(source.contains("@WeekStart") && source.contains("21"));
        assertTrue(source.contains("COUNT(*) FROM dbo.Orders) BETWEEN 20 AND 40"));
        assertTrue(source.contains("status_entered_at") && source.contains("staff_shift_id"));
        assertTrue(source.contains("unit_cost_snapshot") && source.contains("total_cost_snapshot"));
        assertTrue(source.contains("No active demo order may exceed its state timeout"));
        assertTrue(source.contains("Financial demo must have nonzero revenue, COGS, expense, and depreciation"));
        assertTrue(source.contains("IX_OperatingExpense_ExpenseDate") && source.contains("IX_FixedAsset_Status_DepreciationStartDate"));
        assertFalse(source.contains("reservation.variant_id"));
        assertFalse(source.contains("transaction_row.variant_id"));
        assertFalse(source.contains("reservation quantities do not match order items"));
    }
}
