package service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class PresentationDemoSeedPolicyTest {
    private static final Path DATABASE = Path.of("../../database");

    @Test
    void presentationSeedIsGuardedIdempotentAndSizedForDemo() throws Exception {
        String seed = Files.readString(DATABASE.resolve("seed_presentation_demo.sql"));
        String validator = Files.readString(DATABASE.resolve("seed_presentation_demo_validate.sql"));

        for (String source : new String[] {seed, validator}) {
            assertTrue(source.contains("DB_NAME()<>N'DemoDatabase'"));
            assertTrue(source.contains("065_warehouse_operations_redesign"));
        }
        for (String token : new String[] {
                "FASTGUY_ALLOW_PRESENTATION_DEMO_SEED", "SET XACT_ABORT ON", "SET QUOTED_IDENTIFIER ON",
                "SET ANSI_NULLS ON", "SET ANSI_WARNINGS ON", "SET ARITHABORT ON", "BEGIN TRANSACTION",
                "DEMO-PRES-", "@ExpectedProducts int=20", "@ExpectedIngredients int=20",
                "@ExpectedRecipeLines int=40", "@ExpectedOrders int=45"}) {
            assertTrue(seed.contains(token), token);
        }
        for (String token : new String[] {
                "@ExpectedProducts int=20", "@ExpectedIngredients int=20",
                "@ExpectedRecipeLines int=40", "@ExpectedOrders int=45",
                "@ExpectedDeliveredLast7Days int=7", "@ExpectedDeliveredToday int=1",
                "Presentation demo seed validation passed"}) {
            assertTrue(validator.contains(token), token);
        }
        assertTrue(seed.contains("CASE WHEN seq.n<=7 THEN 'DELIVERED'"));
        assertTrue(seed.contains("IF EXISTS(SELECT 1 FROM dbo.Coupon WHERE code='DEMO-PRES-CPN-10')"));
        assertTrue(!seed.contains("DELETE FROM dbo.Coupon WHERE code LIKE 'DEMO-PRES-CPN-%'"));
        assertTrue(validator.contains("delivered_at>=DATEADD(day,-6,CAST(SYSDATETIME() AS date))"));
        assertTrue(validator.contains("CAST(delivered_at AS date)=CAST(SYSDATETIME() AS date)"));
        assertTrue(validator.contains("COUNT(DISTINCT oi.product_id)"));
        for (String token : new String[] {
                "@ExpectedRefundOrders int=3", "@ExpectedCodSettlements int=4",
                "@ExpectedDemoShifts int=9", "@ExpectedPayRates int=2",
                "Ca dữ liệu vận hành COD số", "Ca dữ liệu vận hành nhân viên số"}) {
            assertTrue(seed.contains(token), token);
        }
        for (String token : new String[] {
                "@ExpectedRefundOrders int=3", "@ExpectedCodSettlements int=4",
                "@ExpectedDemoShifts int=9", "@ExpectedPayRates int=2"}) {
            assertTrue(validator.contains(token), token);
        }
        for (String category : new String[] {"Bánh mì", "Burger", "Pizza", "Cơm", "Mì", "Gà rán", "Nước uống"}) {
            assertTrue(seed.contains("N'" + category + "'"), category);
        }
        assertTrue(validator.contains("Forbidden presentation wording in visible fields"));
        assertTrue(validator.contains("Expected seven natural menu categories"));
    }
}
