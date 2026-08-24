SET NOCOUNT ON;
SET XACT_ABORT ON;

IF DB_NAME() NOT IN (N'FastGuyDB', N'FastGuyDB_Inventory052_Test')
    THROW 51630, 'Ingredient seed validator target is not approved.', 1;

IF EXISTS (
    SELECT 1
    FROM dbo.ProductVariant v
    LEFT JOIN dbo.Recipe r ON r.variant_id = v.variant_id AND r.active = 1
    OUTER APPLY (SELECT COUNT(*) AS line_count FROM dbo.RecipeItem ri WHERE ri.recipe_id = r.recipe_id) lines
    WHERE v.status = 'AVAILABLE'
      AND (v.inventory_mode <> 'INGREDIENT' OR r.recipe_id IS NULL OR lines.line_count < 4)
) THROW 51631, 'Available variant recipe coverage is incomplete.', 1;

IF EXISTS (
    SELECT recipe_id, inventory_item_id
    FROM dbo.RecipeItem
    GROUP BY recipe_id, inventory_item_id
    HAVING COUNT(*) > 1
) THROW 51632, 'Duplicate recipe ingredient found.', 1;

IF EXISTS (
    SELECT 1
    FROM dbo.RecipeItem ri
    JOIN dbo.InventoryItem i ON i.inventory_item_id = ri.inventory_item_id
    WHERE ri.quantity <= 0 OR i.item_type <> 'INGREDIENT' OR i.active = 0
) THROW 51633, 'Recipe ingredient is invalid.', 1;

IF EXISTS (
    SELECT 1
    FROM dbo.RecipeItem ri
    JOIN dbo.InventoryItem i ON i.inventory_item_id = ri.inventory_item_id
    WHERE i.base_unit = 'PIECE' AND ri.quantity <> FLOOR(ri.quantity)
) THROW 51634, 'Piece-based recipe quantity must be an integer.', 1;

IF EXISTS (
    SELECT 1
    FROM dbo.ProductVariant v
    JOIN dbo.Recipe r ON r.variant_id = v.variant_id AND r.active = 1
    CROSS APPLY (
        SELECT MIN(FLOOR((i.on_hand_quantity - i.reserved_quantity) / ri.quantity)) AS servings
        FROM dbo.RecipeItem ri
        JOIN dbo.InventoryItem i ON i.inventory_item_id = ri.inventory_item_id
        WHERE ri.recipe_id = r.recipe_id
    ) capacity
    WHERE v.status = 'AVAILABLE' AND (capacity.servings IS NULL OR capacity.servings <= 0)
) THROW 51635, 'Available variant has no ingredient capacity.', 1;

IF (
    SELECT COUNT(*)
    FROM dbo.Product p
    JOIN dbo.ProductVariant v ON v.product_id = p.product_id
    JOIN dbo.Recipe r ON r.variant_id = v.variant_id AND r.active = 1
    JOIN dbo.RecipeItem ri ON ri.recipe_id = r.recipe_id
    WHERE p.name = N'BBQ Bacon Burger'
) < 6 THROW 51636, 'BBQ Bacon Burger multi-ingredient recipe is missing.', 1;

PRINT 'Ingredient inventory demo seed validation passed';

SELECT
    (SELECT COUNT_BIG(*) FROM dbo.InventoryItem WHERE item_type = 'INGREDIENT') AS ingredient_count,
    (SELECT COUNT_BIG(*) FROM dbo.ProductVariant WHERE inventory_mode = 'INGREDIENT') AS ingredient_variant_count,
    (SELECT COUNT_BIG(*) FROM dbo.Recipe WHERE active = 1) AS active_recipe_count,
    (SELECT COUNT_BIG(*) FROM dbo.RecipeItem) AS recipe_line_count;

SELECT TOP (20) p.name AS product_name, v.variant_name, i.name AS ingredient_name,
       ri.quantity, i.base_unit,
       FLOOR((i.on_hand_quantity - i.reserved_quantity) / ri.quantity) AS ingredient_capacity
FROM dbo.Product p
JOIN dbo.ProductVariant v ON v.product_id = p.product_id
JOIN dbo.Recipe r ON r.variant_id = v.variant_id AND r.active = 1
JOIN dbo.RecipeItem ri ON ri.recipe_id = r.recipe_id
JOIN dbo.InventoryItem i ON i.inventory_item_id = ri.inventory_item_id
WHERE p.name IN (N'BBQ Bacon Burger', N'Classic Beef Burger')
ORDER BY p.name, v.variant_id, i.name;
