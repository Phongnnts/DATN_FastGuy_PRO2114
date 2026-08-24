USE [$(FASTGUY_SEED_DATABASE)];
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

IF DB_NAME() NOT IN (N'FastGuyDB', N'FastGuyDB_Inventory052_Test')
    THROW 51620, 'Ingredient demo seed target is not approved.', 1;
IF NOT EXISTS (SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id = '052_ingredient_inventory_phase_1')
    THROW 51621, 'Run migration 052 first.', 1;

BEGIN TRY
    BEGIN TRANSACTION;

    DECLARE @Items TABLE (
        name nvarchar(255) NOT NULL PRIMARY KEY,
        base_unit varchar(10) NOT NULL,
        on_hand decimal(19,4) NOT NULL,
        minimum_quantity decimal(19,4) NOT NULL
    );

    INSERT @Items(name, base_unit, on_hand, minimum_quantity) VALUES
        (N'Bánh burger', 'PIECE', 600, 80),
        (N'Bánh mì', 'PIECE', 400, 50),
        (N'Bánh tortilla', 'PIECE', 400, 50),
        (N'Bánh tráng', 'PIECE', 500, 60),
        (N'Bột mì', 'G', 80000, 10000),
        (N'Bột pizza', 'G', 70000, 8000),
        (N'Gạo', 'G', 100000, 12000),
        (N'Khoai tây', 'G', 80000, 10000),
        (N'Thịt bò', 'G', 70000, 8000),
        (N'Thịt gà', 'G', 90000, 10000),
        (N'Thịt heo', 'G', 60000, 7000),
        (N'Hải sản', 'G', 45000, 5000),
        (N'Bacon', 'G', 30000, 3500),
        (N'Phô mai lát', 'PIECE', 900, 100),
        (N'Phô mai mozzarella', 'G', 50000, 6000),
        (N'Rau xà lách', 'G', 40000, 5000),
        (N'Rau củ hỗn hợp', 'G', 60000, 7000),
        (N'Cà chua', 'G', 35000, 4000),
        (N'Trứng', 'PIECE', 700, 80),
        (N'Sốt burger', 'ML', 30000, 3500),
        (N'Sốt BBQ', 'ML', 30000, 3500),
        (N'Sốt pizza', 'ML', 30000, 3500),
        (N'Sốt món Á', 'ML', 40000, 5000),
        (N'Dầu ăn', 'ML', 70000, 8000),
        (N'Sữa và kem', 'ML', 35000, 4000),
        (N'Nước pha chế', 'ML', 120000, 15000),
        (N'Đường', 'G', 50000, 6000),
        (N'Đá viên', 'G', 100000, 12000),
        (N'Gia vị', 'G', 25000, 3000),
        (N'Bao bì món ăn', 'PIECE', 1200, 150),
        (N'Ly và ống hút', 'PIECE', 1000, 120),
        (N'Hộp pizza', 'PIECE', 500, 60);

    INSERT dbo.InventoryItem(name, item_type, base_unit, on_hand_quantity, reserved_quantity, minimum_quantity, active)
    SELECT s.name, 'INGREDIENT', s.base_unit, s.on_hand, 0, s.minimum_quantity, 1
    FROM @Items s
    WHERE NOT EXISTS (SELECT 1 FROM dbo.InventoryItem i WHERE i.name = s.name);

    UPDATE i
    SET active = 1,
        minimum_quantity = s.minimum_quantity
    FROM dbo.InventoryItem i
    JOIN @Items s ON s.name = i.name
    WHERE i.item_type = 'INGREDIENT';

    INSERT dbo.Recipe(variant_id, yield_quantity, active)
    SELECT v.variant_id, 1, 1
    FROM dbo.ProductVariant v
    WHERE v.status = 'AVAILABLE'
      AND NOT EXISTS (SELECT 1 FROM dbo.Recipe r WHERE r.variant_id = v.variant_id);

    DECLARE @Seed TABLE (
        variant_id int NOT NULL,
        item_name nvarchar(255) NOT NULL,
        quantity decimal(19,4) NOT NULL,
        PRIMARY KEY (variant_id, item_name)
    );

    ;WITH variants AS (
        SELECT v.variant_id, p.category_id,
               CONVERT(decimal(19,4), CASE
                   WHEN v.variant_name LIKE N'%6%' THEN 6.0
                   WHEN v.variant_name LIKE N'%3%' THEN 3.0
                   WHEN v.variant_name LIKE N'%30cm%' OR v.variant_name LIKE N'% L%' OR v.variant_name LIKE N'%lớn%' THEN 1.4
                   ELSE 1.0
               END) AS factor
        FROM dbo.ProductVariant v
        JOIN dbo.Product p ON p.product_id = v.product_id
        WHERE v.status = 'AVAILABLE'
    )
    INSERT @Seed(variant_id, item_name, quantity)
    SELECT v.variant_id, x.item_name,
           CONVERT(decimal(19,4), x.quantity * CASE
               WHEN v.factor = 1.4 AND x.item_name IN (
                   N'Bánh burger', N'Bánh mì', N'Bánh tortilla', N'Bánh tráng',
                   N'Phô mai lát', N'Trứng', N'Bao bì món ăn', N'Ly và ống hút', N'Hộp pizza'
               ) THEN 1.0
               ELSE v.factor
           END)
    FROM variants v
    CROSS APPLY (
        SELECT item_name, quantity
        FROM (VALUES
            (N'Bánh burger', CONVERT(decimal(19,4), 1)),
            (N'Thịt bò', 120), (N'Bacon', 20), (N'Phô mai lát', 1),
            (N'Rau xà lách', 20), (N'Sốt burger', 25), (N'Bao bì món ăn', 1)
        ) b(item_name, quantity)
        WHERE v.category_id IN (1, 22)
        UNION ALL
        SELECT item_name, quantity FROM (VALUES
            (N'Thịt gà', CONVERT(decimal(19,4), 180)), (N'Bột mì', 35),
            (N'Dầu ăn', 25), (N'Gia vị', 8), (N'Sốt BBQ', 20), (N'Bao bì món ăn', 1)
        ) c(item_name, quantity) WHERE v.category_id IN (2, 23)
        UNION ALL
        SELECT item_name, quantity FROM (VALUES
            (N'Bánh tortilla', CONVERT(decimal(19,4), 1)), (N'Thịt bò', 100),
            (N'Rau củ hỗn hợp', 45), (N'Phô mai mozzarella', 25),
            (N'Sốt burger', 20), (N'Bao bì món ăn', 1)
        ) t(item_name, quantity) WHERE v.category_id = 3
        UNION ALL
        SELECT item_name, quantity FROM (VALUES
            (N'Bột pizza', CONVERT(decimal(19,4), 250)), (N'Phô mai mozzarella', 100),
            (N'Sốt pizza', 70), (N'Thịt gà', 80), (N'Rau củ hỗn hợp', 50),
            (N'Hộp pizza', 1)
        ) pz(item_name, quantity) WHERE v.category_id IN (4, 24)
        UNION ALL
        SELECT item_name, quantity FROM (VALUES
            (N'Khoai tây', CONVERT(decimal(19,4), 180)), (N'Bột mì', 20),
            (N'Dầu ăn', 25), (N'Gia vị', 6), (N'Bao bì món ăn', 1)
        ) s(item_name, quantity) WHERE v.category_id IN (5, 26)
        UNION ALL
        SELECT item_name, quantity FROM (VALUES
            (N'Gạo', CONVERT(decimal(19,4), 180)), (N'Thịt gà', 120),
            (N'Rau củ hỗn hợp', 60), (N'Sốt món Á', 30), (N'Gia vị', 6),
            (N'Bao bì món ăn', 1)
        ) rice(item_name, quantity) WHERE v.category_id IN (6, 7, 8, 25)
        UNION ALL
        SELECT item_name, quantity FROM (VALUES
            (N'Bánh mì', CONVERT(decimal(19,4), 1)), (N'Thịt heo', 100),
            (N'Rau củ hỗn hợp', 45), (N'Sốt burger', 20), (N'Bao bì món ăn', 1)
        ) bm(item_name, quantity) WHERE v.category_id = 9
        UNION ALL
        SELECT item_name, quantity FROM (VALUES
            (N'Bánh tráng', CONVERT(decimal(19,4), 2)), (N'Thịt gà', 90),
            (N'Rau củ hỗn hợp', 70), (N'Sốt món Á', 25), (N'Bao bì món ăn', 1)
        ) roll(item_name, quantity) WHERE v.category_id = 10
        UNION ALL
        SELECT item_name, quantity FROM (VALUES
            (N'Nước pha chế', CONVERT(decimal(19,4), 350)), (N'Đường', 25),
            (N'Đá viên', 180), (N'Ly và ống hút', 1)
        ) drink(item_name, quantity) WHERE v.category_id IN (11, 27)
        UNION ALL
        SELECT item_name, quantity FROM (VALUES
            (N'Bột mì', CONVERT(decimal(19,4), 80)), (N'Đường', 35),
            (N'Sữa và kem', 60), (N'Trứng', 1), (N'Bao bì món ăn', 1)
        ) dessert(item_name, quantity) WHERE v.category_id IN (12, 28)
        UNION ALL
        SELECT item_name, quantity FROM (VALUES
            (N'Gạo', CONVERT(decimal(19,4), 180)), (N'Thịt gà', 180),
            (N'Khoai tây', 120), (N'Nước pha chế', 300),
            (N'Sốt món Á', 30), (N'Bao bì món ăn', 1), (N'Ly và ống hút', 1)
        ) combo(item_name, quantity) WHERE v.category_id = 29
    ) x;

    INSERT dbo.RecipeItem(recipe_id, inventory_item_id, quantity)
    SELECT r.recipe_id, i.inventory_item_id, s.quantity
    FROM @Seed s
    JOIN dbo.Recipe r ON r.variant_id = s.variant_id
    JOIN dbo.InventoryItem i ON i.name = s.item_name AND i.item_type = 'INGREDIENT'
    WHERE NOT EXISTS (
        SELECT 1 FROM dbo.RecipeItem ri
        WHERE ri.recipe_id = r.recipe_id AND ri.inventory_item_id = i.inventory_item_id
    );

    UPDATE v
    SET inventory_mode = CASE WHEN v.status = 'AVAILABLE' THEN 'INGREDIENT' ELSE 'SUSPENDED' END
    FROM dbo.ProductVariant v;

    IF EXISTS (
        SELECT 1
        FROM dbo.ProductVariant v
        LEFT JOIN dbo.Recipe r ON r.variant_id = v.variant_id AND r.active = 1
        OUTER APPLY (SELECT COUNT(*) AS line_count FROM dbo.RecipeItem ri WHERE ri.recipe_id = r.recipe_id) lines
        WHERE v.status = 'AVAILABLE' AND (v.inventory_mode <> 'INGREDIENT' OR r.recipe_id IS NULL OR lines.line_count < 4)
    ) THROW 51622, 'Every available variant must have an active multi-line ingredient recipe.', 1;

    IF EXISTS (
        SELECT 1
        FROM dbo.Recipe r
        JOIN dbo.RecipeItem ri ON ri.recipe_id = r.recipe_id
        JOIN dbo.InventoryItem i ON i.inventory_item_id = ri.inventory_item_id
        WHERE r.active = 1 AND (i.active = 0 OR i.on_hand_quantity - i.reserved_quantity < 0)
    ) THROW 51623, 'Active recipe references unavailable ingredient data.', 1;

    COMMIT TRANSACTION;

    SELECT
        (SELECT COUNT_BIG(*) FROM dbo.InventoryItem WHERE item_type = 'INGREDIENT') AS ingredient_count,
        (SELECT COUNT_BIG(*) FROM dbo.ProductVariant WHERE inventory_mode = 'INGREDIENT') AS ingredient_variant_count,
        (SELECT COUNT_BIG(*) FROM dbo.Recipe WHERE active = 1) AS active_recipe_count,
        (SELECT COUNT_BIG(*) FROM dbo.RecipeItem) AS recipe_line_count;

    SELECT TOP (20) p.name AS product_name, v.variant_name, i.name AS ingredient_name,
           ri.quantity, i.base_unit, i.on_hand_quantity - i.reserved_quantity AS available_quantity
    FROM dbo.Product p
    JOIN dbo.ProductVariant v ON v.product_id = p.product_id
    JOIN dbo.Recipe r ON r.variant_id = v.variant_id
    JOIN dbo.RecipeItem ri ON ri.recipe_id = r.recipe_id
    JOIN dbo.InventoryItem i ON i.inventory_item_id = ri.inventory_item_id
    WHERE p.name IN (N'BBQ Bacon Burger', N'Classic Beef Burger')
    ORDER BY p.name, v.variant_id, i.name;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO
