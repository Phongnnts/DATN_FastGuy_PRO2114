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
                "FG-OPS-SKU-", "FG-OPS-ING-", "FG-OPS-REC-", "@ExpectedProducts int=40", "@ExpectedIngredients int=40",
                "@ExpectedRecipeLines int=200", "@ExpectedOrders int=180"}) {
            assertTrue(seed.contains(token), token);
        }
        for (String token : new String[] {
                "@ExpectedProducts int=40", "@ExpectedIngredients int=40",
                "@ExpectedRecipeLines int=200", "@ExpectedOrders int=180",
                "Presentation demo seed validation passed"}) {
            assertTrue(validator.contains(token), token);
        }
        assertTrue(seed.contains("LEN(order_code)=10 AND order_code LIKE 'FG-ORD-[0-9][0-9][0-9]'"));
        assertTrue(seed.contains("LEN(sku)=10 AND sku LIKE 'FG-SKU-[0-9][0-9][0-9]'"));
        assertTrue(seed.contains("LEN(inventory_code)=10 AND inventory_code LIKE 'FG-ING-[0-9][0-9][0-9]'"));
        assertTrue(!seed.contains("DELETE FROM dbo.Coupon WHERE code LIKE 'FG-CPN-%'"));
        assertTrue(!seed.contains("WHERE order_code LIKE 'FG-ORD-%'"));
        assertTrue(validator.contains("(VALUES(1,6),(2,6),(3,6),(4,6),(5,6),(6,5),(7,5))"));
        assertTrue(validator.contains("Expected category distribution 6-6-6-6-6-5-5"));
        assertTrue(validator.contains("Expected variant recipe scaling"));
        assertTrue(validator.contains("Expected COD reconciliation states"));
        assertTrue(validator.contains("Expected receipts and stock counts"));
        for (String category : new String[] {"Bánh mì", "Burger", "Pizza", "Cơm", "Mì", "Gà rán", "Nước uống"}) {
            assertTrue(seed.contains("N'" + category + "'"), category);
        }
        assertTrue(validator.contains("Forbidden presentation wording in visible fields"));
        assertTrue(validator.contains("Expected seven natural menu categories"));
        for (String token : new String[] {
                "@ExpectedAdmins int=2", "@ExpectedStaff int=6", "@ExpectedShippers int=4",
                "@ExpectedCustomers int=20", "@ExpectedOrders int=180", "dbo.Address",
                "favorite_ids_json", "dbo.Cart", "dbo.CartItem", "dbo.CouponRedemption",
                "dbo.ProductModifierGroup", "dbo.ProductModifierOption", "dbo.OperatingExpense",
                "dbo.FixedAsset", "dbo.Banner", "dbo.ActivityLog"}) {
            assertTrue(seed.contains(token), token);
            assertTrue(validator.contains(token), token);
        }
        assertTrue(seed.contains("@ExpectedVariants int=100"));
        assertTrue(validator.contains("Expected two or three variants per product"));
        assertTrue(validator.contains("Expected variant recipe scaling"));
        assertTrue(validator.contains("Expected 30 inclusive operating dates"));
        assertTrue(validator.contains("Expected customer engagement coverage"));
        assertTrue(validator.contains("Expected finance, merchandising and audit coverage"));
    }
}
