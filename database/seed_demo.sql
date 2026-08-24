USE FastGuyDB;
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO
IF DB_NAME() <> N'FastGuyDB' THROW 51600, 'Demo seed must run against FastGuyDB.', 1;
IF SCHEMA_ID(N'dbo') IS NULL THROW 51601, 'Required dbo schema is missing.', 1;
IF '$(FASTGUY_ALLOW_DEMO_SEED)' <> '1' THROW 51602, 'Local demo seed blocked. Run sqlcmd with -v FASTGUY_ALLOW_DEMO_SEED=1.', 1;
IF OBJECT_ID(N'dbo.SchemaMigrationHistory', N'U') IS NULL THROW 51603, 'SchemaMigrationHistory is missing.', 1;
IF NOT EXISTS (SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id = '040_production_hardening') THROW 51604, 'Run migration 040_production_hardening first.', 1;
IF NOT EXISTS (SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id = '042_login_bruteforce_lock') THROW 51605, 'Run migration 042_login_bruteforce_lock first.', 1;
IF NOT EXISTS (SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id = '043_inventory_adjustment_audit') THROW 51606, 'Run migration 043_inventory_adjustment_audit first.', 1;
IF NOT EXISTS (SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id = '050_product_scoped_reviews') THROW 51614, 'Run migration 050_product_scoped_reviews first.', 1;
IF COL_LENGTH(N'dbo.ProductVariant', N'inventory_mode') IS NULL OR OBJECT_ID(N'dbo.InventoryItem', N'U') IS NULL THROW 51615, 'Run migration 052_ingredient_inventory_phase_1 first.', 1;
IF COL_LENGTH(N'dbo.Users', N'failed_login_attempts') IS NULL OR COL_LENGTH(N'dbo.Users', N'locked_until') IS NULL THROW 51607, 'Latest Users schema is missing.', 1;
IF COL_LENGTH(N'dbo.InventoryTransaction', N'created_by') IS NULL OR COL_LENGTH(N'dbo.InventoryTransaction', N'reason_code') IS NULL OR COL_LENGTH(N'dbo.InventoryTransaction', N'note') IS NULL OR COL_LENGTH(N'dbo.InventoryTransaction', N'quantity_before') IS NULL OR COL_LENGTH(N'dbo.InventoryTransaction', N'quantity_after') IS NULL THROW 51608, 'Latest InventoryTransaction schema is missing.', 1;
BEGIN TRY
    BEGIN TRANSACTION;

    DECLARE @Now datetime2(0) = GETDATE();
    DECLARE @Today date = CAST(@Now AS date);
    DECLARE @DemoPasswordHash varchar(255) = 'pbkdf2$120000$cIKZ7vyW8OayQzvnslRXqA==$BIeWj2zHjvoHTjEU8+cEQ74RG1VOzkdMT5CyTSLTp80=';

    DECLARE @N TABLE (n int NOT NULL PRIMARY KEY);
    WITH n AS (
        SELECT 1 AS n
        UNION ALL
        SELECT n + 1 FROM n WHERE n < 200
    )
    INSERT @N(n) SELECT n FROM n OPTION (MAXRECURSION 200);

    DECLARE @Categories TABLE (n int PRIMARY KEY, name nvarchar(255), description nvarchar(500));
    INSERT @Categories VALUES
        (1,N'FG-DEMO Burger',N'Burger thủ công cho dữ liệu trình diễn'),
        (2,N'FG-DEMO Gà rán',N'Gà giòn và món từ gà'),
        (3,N'FG-DEMO Pizza',N'Pizza nướng mới mỗi ngày'),
        (4,N'FG-DEMO Mì và cơm',N'Món no phong vị Việt Á'),
        (5,N'FG-DEMO Ăn nhẹ',N'Món ăn kèm và khai vị'),
        (6,N'FG-DEMO Đồ uống',N'Đồ uống mát lạnh'),
        (7,N'FG-DEMO Tráng miệng',N'Món ngọt sau bữa ăn'),
        (8,N'FG-DEMO Combo',N'Combo tiết kiệm cho nhóm');
    INSERT dbo.Category(name,description,sort_order,status)
    SELECT c.name,c.description,100+c.n,'ACTIVE'
    FROM @Categories c
    WHERE NOT EXISTS (SELECT 1 FROM dbo.Category x WHERE x.name=c.name);

    INSERT dbo.Users(role_name,email,phone,password_hash,full_name,avatar_url,status,loyalty_points,favorite_ids_json,created_at,updated_at,failed_login_attempts,locked_until)
    SELECT CASE WHEN n.n<=58 THEN 'USER' WHEN n.n<=64 THEN 'STAFF' ELSE 'SHIPPER' END,
           CONCAT('fg-demo-',RIGHT(CONCAT('000',n.n),3),'@fastguy.local'),
           CONCAT('0898',RIGHT(CONCAT('000000',n.n),6)),
           @DemoPasswordHash,
           CASE WHEN n.n<=58 THEN CONCAT(N'Khách Demo ',RIGHT(CONCAT('000',n.n),3)) WHEN n.n<=64 THEN CONCAT(N'Nhân viên Demo ',RIGHT(CONCAT('000',n.n-58),2)) ELSE CONCAT(N'Tài xế Demo ',RIGHT(CONCAT('000',n.n-64),2)) END,
           CONCAT('/images/avatars/fg-demo-',RIGHT(CONCAT('000',n.n),3),'.jpg'),
           'ACTIVE',CASE WHEN n.n<=58 THEN n.n*25 ELSE 0 END,N'[]',DATEADD(day,-n.n,@Now),@Now,0,NULL
    FROM @N n
    WHERE n.n<=70
      AND NOT EXISTS (SELECT 1 FROM dbo.Users u WHERE u.email=CONCAT('fg-demo-',RIGHT(CONCAT('000',n.n),3),'@fastguy.local'));

    INSERT dbo.Address(user_id,recipient_name,phone,street,ward_name,district_name,province_name,ghn_province_id,ghn_district_id,ghn_ward_code,city,is_default,created_at,updated_at)
    SELECT u.user_id,u.full_name,u.phone,CONCAT(n.n,N' Đường FG-DEMO, Phường Bến Nghé'),N'Bến Nghé',N'Quận 1',N'TP. Hồ Chí Minh',202,1442,'20107',N'TP. Hồ Chí Minh',1,DATEADD(day,-n.n,@Now),@Now
    FROM @N n
    JOIN dbo.Users u ON u.email=CONCAT('fg-demo-',RIGHT(CONCAT('000',n.n),3),'@fastguy.local')
    WHERE n.n<=58 AND NOT EXISTS (SELECT 1 FROM dbo.Address a WHERE a.user_id=u.user_id AND a.is_default=1);

    DECLARE @ProductNames TABLE (n int PRIMARY KEY, name nvarchar(255), description nvarchar(1000));
    INSERT @ProductNames
    SELECT n,CONCAT(N'FG-DEMO ',CASE (n-1)%8 WHEN 0 THEN N'Burger Đặc Biệt ' WHEN 1 THEN N'Gà Giòn Cay ' WHEN 2 THEN N'Pizza Phô Mai ' WHEN 3 THEN N'Cơm Sốt Việt ' WHEN 4 THEN N'Khoai Giòn ' WHEN 5 THEN N'Trà Trái Cây ' WHEN 6 THEN N'Bánh Ngọt ' ELSE N'Combo Bạn Bè ' END,RIGHT(CONCAT('00',n),2)),
           CONCAT(N'Món demo số ',n,N', hình ảnh và giá phù hợp màn hình trình diễn.')
    FROM @N WHERE n<=70;
    INSERT dbo.Product(category_id,name,description,base_price,image_url,gallery_images,status,available_from,available_to,created_at,updated_at)
    SELECT c.category_id,p.name,p.description,29000+((p.n-1)%10)*10000,CONCAT('/images/products/fg-demo-',RIGHT(CONCAT('00',p.n),2),'.jpg'),N'[]',CASE WHEN p.n%17=0 THEN 'UNAVAILABLE' ELSE 'AVAILABLE' END,'08:00','22:00',DATEADD(day,-p.n,@Now),@Now
    FROM @ProductNames p
    JOIN @Categories dc ON dc.n=((p.n-1)%8)+1
    JOIN dbo.Category c ON c.name=dc.name
    WHERE NOT EXISTS (SELECT 1 FROM dbo.Product x WHERE x.name=p.name);

    INSERT dbo.ProductVariant(product_id,variant_name,price,original_price,sku,quantity_available,weight,length,width,height,is_default,status,created_at,updated_at)
    SELECT p.product_id,v.variant_name,p.base_price+v.extra,CASE WHEN n.n%5=0 THEN p.base_price+v.extra+10000 END,
           CONCAT('FG-DEMO-SKU-',RIGHT(CONCAT('00',n.n),2),'-',v.suffix),80+(n.n%40),400+v.extra/100,20,20,10,v.is_default,p.status,DATEADD(day,-n.n,@Now),@Now
    FROM @N n
    JOIN @ProductNames pn ON pn.n=n.n
    JOIN dbo.Product p ON p.name=pn.name
    CROSS APPLY (VALUES(N'Tiêu chuẩn',CAST(0 AS decimal(18,2)),'STD',CAST(1 AS bit)),(N'Phần lớn',CAST(15000 AS decimal(18,2)),'L',CAST(0 AS bit))) v(variant_name,extra,suffix,is_default)
    WHERE n.n<=70 AND NOT EXISTS (SELECT 1 FROM dbo.ProductVariant x WHERE x.sku=CONCAT('FG-DEMO-SKU-',RIGHT(CONCAT('00',n.n),2),'-',v.suffix));

    UPDATE dbo.ProductVariant SET inventory_mode='FINISHED_GOOD' WHERE sku='FG-DEMO-SKU-01-L';
    IF NOT EXISTS (SELECT 1 FROM dbo.InventoryItem WHERE name=N'FG-DEMO Bột mì')
        INSERT dbo.InventoryItem(name,item_type,base_unit,on_hand_quantity,reserved_quantity,minimum_quantity,active) VALUES(N'FG-DEMO Bột mì','INGREDIENT','G',25000,0,5000,1);
    DECLARE @IngredientVariantId int=(SELECT variant_id FROM dbo.ProductVariant WHERE sku='FG-DEMO-SKU-01-STD');
    DECLARE @IngredientItemId int=(SELECT inventory_item_id FROM dbo.InventoryItem WHERE name=N'FG-DEMO Bột mì');
    IF NOT EXISTS (SELECT 1 FROM dbo.Recipe WHERE variant_id=@IngredientVariantId)
        INSERT dbo.Recipe(variant_id,yield_quantity,active) VALUES(@IngredientVariantId,1,1);
    IF NOT EXISTS (SELECT 1 FROM dbo.RecipeItem WHERE recipe_id=(SELECT recipe_id FROM dbo.Recipe WHERE variant_id=@IngredientVariantId) AND inventory_item_id=@IngredientItemId)
        INSERT dbo.RecipeItem(recipe_id,inventory_item_id,quantity) VALUES((SELECT recipe_id FROM dbo.Recipe WHERE variant_id=@IngredientVariantId),@IngredientItemId,120);
    UPDATE dbo.ProductVariant SET inventory_mode='INGREDIENT' WHERE variant_id=@IngredientVariantId;
    IF NOT EXISTS (SELECT 1 FROM dbo.VariantInventoryItem WHERE variant_id=(SELECT variant_id FROM dbo.ProductVariant WHERE sku='FG-DEMO-SKU-01-L'))
    BEGIN
        INSERT dbo.InventoryItem(name,item_type,base_unit,on_hand_quantity,reserved_quantity,minimum_quantity,active) VALUES(N'FG-DEMO Thành phẩm Burger lớn','FINISHED_GOOD','PIECE',40,0,5,1);
        INSERT dbo.VariantInventoryItem(variant_id,inventory_item_id) VALUES((SELECT variant_id FROM dbo.ProductVariant WHERE sku='FG-DEMO-SKU-01-L'),SCOPE_IDENTITY());
    END;

    DECLARE @MissingVariantItems TABLE(variant_id int PRIMARY KEY,inventory_item_id int);
    MERGE dbo.InventoryItem target USING(SELECT v.variant_id,CONCAT(N'FG-DEMO Thành phẩm ',v.sku) name,CONVERT(decimal(19,4),COALESCE(v.quantity_available,0)) quantity FROM dbo.ProductVariant v WHERE v.sku LIKE 'FG-DEMO-SKU-%' AND v.inventory_mode<>'INGREDIENT' AND NOT EXISTS(SELECT 1 FROM dbo.VariantInventoryItem m WHERE m.variant_id=v.variant_id)) source ON 1=0 WHEN NOT MATCHED THEN INSERT(name,item_type,base_unit,on_hand_quantity,reserved_quantity,minimum_quantity,active) VALUES(source.name,'FINISHED_GOOD','PIECE',source.quantity,0,5,1) OUTPUT source.variant_id,inserted.inventory_item_id INTO @MissingVariantItems;
    INSERT dbo.VariantInventoryItem(variant_id,inventory_item_id) SELECT variant_id,inventory_item_id FROM @MissingVariantItems;

    INSERT dbo.ProductModifierGroup(product_id,name,min_selections,max_selections,is_active,sort_order)
    SELECT p.product_id,N'FG-DEMO Tùy chọn thêm',0,2,1,90
    FROM @N n JOIN @ProductNames pn ON pn.n=n.n JOIN dbo.Product p ON p.name=pn.name
    WHERE n.n<=30 AND NOT EXISTS (SELECT 1 FROM dbo.ProductModifierGroup g WHERE g.product_id=p.product_id AND g.name=N'FG-DEMO Tùy chọn thêm');
    INSERT dbo.ProductModifierOption(modifier_group_id,name,price,is_active,sort_order)
    SELECT g.modifier_group_id,o.name,o.price,1,o.sort_order
    FROM dbo.ProductModifierGroup g
    CROSS APPLY (VALUES(N'FG-DEMO Thêm phô mai',10000,1),(N'FG-DEMO Thêm sốt',5000,2),(N'FG-DEMO Không hành',0,3)) o(name,price,sort_order)
    WHERE g.name=N'FG-DEMO Tùy chọn thêm' AND NOT EXISTS (SELECT 1 FROM dbo.ProductModifierOption x WHERE x.modifier_group_id=g.modifier_group_id AND x.name=o.name);

    INSERT dbo.Coupon(code,type,value,min_order,max_discount,max_uses,used_count,expires_at,is_active,is_public,created_at,updated_at)
    SELECT CONCAT('FG-DEMO-',RIGHT(CONCAT('00',n),2)),CASE n%3 WHEN 1 THEN 'PERCENT' WHEN 2 THEN 'FIXED' ELSE 'FREE_SHIPPING' END,
           CASE n%3 WHEN 1 THEN 10 WHEN 2 THEN 15000 ELSE 0 END,50000,CASE n%3 WHEN 1 THEN 30000 WHEN 2 THEN 15000 ELSE 25000 END,0,0,DATEADD(day,180,@Now),1,1,DATEADD(day,-30,@Now),@Now
    FROM @N WHERE n<=6 AND NOT EXISTS (SELECT 1 FROM dbo.Coupon c WHERE c.code=CONCAT('FG-DEMO-',RIGHT(CONCAT('00',n),2)));

    INSERT dbo.Banner(title,subtitle,image_url,link,sort_order,is_active,created_at,updated_at)
    SELECT CONCAT(N'FG-DEMO Ưu đãi ',n),CONCAT(N'Khám phá thực đơn demo nổi bật số ',n),CONCAT('/images/banners/fg-demo-',n,'.jpg'),'/menu',100+n,1,DATEADD(day,-n,@Now),@Now
    FROM @N WHERE n<=5 AND NOT EXISTS (SELECT 1 FROM dbo.Banner b WHERE b.title=CONCAT(N'FG-DEMO Ưu đãi ',n));

    INSERT dbo.Cart(user_id,session_id,created_at,updated_at)
    SELECT u.user_id,NULL,DATEADD(day,-1,@Now),@Now
    FROM @N n JOIN dbo.Users u ON u.email=CONCAT('fg-demo-',RIGHT(CONCAT('000',n.n),3),'@fastguy.local')
    WHERE n.n<=20 AND NOT EXISTS (SELECT 1 FROM dbo.Cart c WHERE c.user_id=u.user_id);
    INSERT dbo.CartItem(cart_id,product_id,variant_id,quantity,unit_price,modifiers_json,created_at,updated_at)
    SELECT c.cart_id,p.product_id,v.variant_id,1+(n.n%2),v.price,N'[]',DATEADD(hour,-n.n,@Now),@Now
    FROM @N n
    JOIN dbo.Users u ON u.email=CONCAT('fg-demo-',RIGHT(CONCAT('000',n.n),3),'@fastguy.local')
    JOIN dbo.Cart c ON c.user_id=u.user_id
    JOIN @ProductNames pn ON pn.n=((n.n-1)%70)+1
    JOIN dbo.Product p ON p.name=pn.name
    JOIN dbo.ProductVariant v ON v.product_id=p.product_id AND v.is_default=1
    WHERE n.n<=20 AND NOT EXISTS (SELECT 1 FROM dbo.CartItem ci WHERE ci.cart_id=c.cart_id AND ci.variant_id=v.variant_id);

    DECLARE @Staff TABLE (n int PRIMARY KEY,user_id int);
    DECLARE @Shipper TABLE (n int PRIMARY KEY,user_id int);
    INSERT @Staff SELECT n.n-58,u.user_id FROM @N n JOIN dbo.Users u ON u.email=CONCAT('fg-demo-',RIGHT(CONCAT('000',n.n),3),'@fastguy.local') WHERE n.n BETWEEN 59 AND 64;
    INSERT @Shipper SELECT n.n-64,u.user_id FROM @N n JOIN dbo.Users u ON u.email=CONCAT('fg-demo-',RIGHT(CONCAT('000',n.n),3),'@fastguy.local') WHERE n.n BETWEEN 65 AND 70;

    INSERT dbo.WorkShift(user_id,shift_date,start_time,end_time,check_in_at,check_out_at,status,created_at,updated_at)
    SELECT x.user_id,DATEADD(day,d.day_offset,@Today),d.start_time,d.end_time,
           CASE WHEN d.day_offset<0 THEN DATEADD(hour,DATEPART(hour,d.start_time),CAST(DATEADD(day,d.day_offset,@Today) AS datetime2)) WHEN d.day_offset=0 THEN DATEADD(hour,DATEPART(hour,d.start_time),CAST(@Today AS datetime2)) END,
           CASE WHEN d.day_offset<0 THEN DATEADD(hour,DATEPART(hour,d.end_time),CAST(DATEADD(day,d.day_offset,@Today) AS datetime2)) END,
           CASE WHEN d.day_offset<0 THEN 'CHECKED_OUT' WHEN d.day_offset=0 THEN 'CHECKED_IN' ELSE 'SCHEDULED' END,@Now,@Now
    FROM (SELECT user_id,n FROM @Staff UNION ALL SELECT user_id,n FROM @Shipper) x
    CROSS APPLY (VALUES(-1,CAST('08:00' AS time),CAST('16:00' AS time)),(0,CAST('08:00' AS time),CAST('16:00' AS time)),(1,CAST('14:00' AS time),CAST('22:00' AS time))) d(day_offset,start_time,end_time)
    WHERE NOT EXISTS (SELECT 1 FROM dbo.WorkShift w WHERE w.user_id=x.user_id AND w.shift_date=DATEADD(day,d.day_offset,@Today) AND w.start_time=d.start_time);

    INSERT dbo.Orders(order_code,idempotency_key,request_hash,idempotency_owner,user_id,customer_name,customer_phone,customer_address,to_province_name,to_district_name,to_ward_name,total_amount,shipping_fee,service_fee,final_amount,cod_collected_amount,cod_collected_at,shipping_provider,expected_delivery_time,payment_method,payment_status,payos_payment_link_id,payos_checkout_url,order_status,staff_id,shipper_id,assigned_at,confirmed_at,ready_at,picked_up_at,paid_at,delivered_at,cancelled_at,failure_reason,cancelled_by,coupon_code,discount_amount,delivery_note,created_at,updated_at)
    SELECT CONCAT('FG-DEMO-ORDER-',RIGHT(CONCAT('000',n.n),3)),CONCAT('FG-DEMO-IDEMP-',RIGHT(CONCAT('000',n.n),3)),REPLICATE(LOWER(SUBSTRING('abcdef0123456789',(n.n%16)+1,1)),64),CONCAT('FG-DEMO-USER-',RIGHT(CONCAT('000',((n.n-1)%58)+1),3)),u.user_id,u.full_name,u.phone,CONCAT(((n.n-1)%58)+1,N' Đường FG-DEMO, Quận 1'),N'TP. Hồ Chí Minh',N'Quận 1',N'Bến Nghé',v.price,15000,0,v.price+15000,
           CASE WHEN s.status='DELIVERED' AND n.n%2=1 THEN v.price+15000 END,CASE WHEN s.status='DELIVERED' AND n.n%2=1 THEN DATEADD(hour,2,DATEADD(day,-((n.n-1)%180),@Now)) END,'GHN',DATEADD(hour,2,DATEADD(day,-((n.n-1)%180),@Now)),
           CASE WHEN n.n%2=0 THEN 'BANK_TRANSFER' ELSE 'COD' END,CASE WHEN s.status='DELIVERED' OR (n.n%2=0 AND s.status NOT IN ('PENDING','CANCELLED')) THEN 'PAID' ELSE 'UNPAID' END,
           CASE WHEN n.n%2=0 THEN CONCAT('FG-DEMO-PAY-',RIGHT(CONCAT('000',n.n),3)) END,CASE WHEN n.n%2=0 THEN CONCAT('https://pay.payos.vn/web/FG-DEMO-',RIGHT(CONCAT('000',n.n),3)) END,s.status,
           CASE WHEN s.status IN ('CONFIRMED','PREPARING','READY','ASSIGNED','PICKED_UP','DELIVERED') THEN st.user_id END,
           CASE WHEN s.status IN ('ASSIGNED','PICKED_UP','DELIVERED') THEN sh.user_id END,
           CASE WHEN s.status IN ('ASSIGNED','PICKED_UP','DELIVERED') THEN DATEADD(minute,40,DATEADD(day,-((n.n-1)%180),@Now)) END,
           CASE WHEN s.status IN ('CONFIRMED','PREPARING','READY','ASSIGNED','PICKED_UP','DELIVERED') THEN DATEADD(minute,10,DATEADD(day,-((n.n-1)%180),@Now)) END,
           CASE WHEN s.status IN ('READY','ASSIGNED','PICKED_UP','DELIVERED') THEN DATEADD(minute,30,DATEADD(day,-((n.n-1)%180),@Now)) END,
           CASE WHEN s.status IN ('PICKED_UP','DELIVERED') THEN DATEADD(minute,50,DATEADD(day,-((n.n-1)%180),@Now)) END,
           CASE WHEN s.status='DELIVERED' OR (n.n%2=0 AND s.status NOT IN ('PENDING','CANCELLED')) THEN DATEADD(hour,1,DATEADD(day,-((n.n-1)%180),@Now)) END,
           CASE WHEN s.status='DELIVERED' THEN DATEADD(hour,2,DATEADD(day,-((n.n-1)%180),@Now)) END,
           CASE WHEN s.status='CANCELLED' THEN DATEADD(minute,15,DATEADD(day,-((n.n-1)%180),@Now)) END,
           CASE WHEN s.status='CANCELLED' THEN N'Khách thay đổi kế hoạch' END,CASE WHEN s.status='CANCELLED' THEN 'CUSTOMER' END,NULL,0,N'Dữ liệu giao hàng FG-DEMO',DATEADD(day,-((n.n-1)%180),@Now),@Now
    FROM @N n
    CROSS APPLY (VALUES(CASE (n.n-1)%8 WHEN 0 THEN 'PENDING' WHEN 1 THEN 'CONFIRMED' WHEN 2 THEN 'PREPARING' WHEN 3 THEN 'READY' WHEN 4 THEN 'ASSIGNED' WHEN 5 THEN 'PICKED_UP' WHEN 6 THEN 'DELIVERED' ELSE 'CANCELLED' END)) s(status)
    JOIN dbo.Users u ON u.email=CONCAT('fg-demo-',RIGHT(CONCAT('000',((n.n-1)%58)+1),3),'@fastguy.local')
    JOIN @ProductNames pn ON pn.n=((n.n-1)%70)+1
    JOIN dbo.Product p ON p.name=pn.name
    JOIN dbo.ProductVariant v ON v.product_id=p.product_id AND v.is_default=1
    JOIN @Staff st ON st.n=((n.n-1)%6)+1
    JOIN @Shipper sh ON sh.n=((n.n-1)%6)+1
    WHERE n.n<=120 AND NOT EXISTS (SELECT 1 FROM dbo.Orders o WHERE o.order_code=CONCAT('FG-DEMO-ORDER-',RIGHT(CONCAT('000',n.n),3)));

    INSERT dbo.OrderItem(order_id,product_id,variant_id,product_name,variant_name,quantity,unit_price,total_price,modifiers_json)
    SELECT o.order_id,p.product_id,v.variant_id,p.name,v.variant_name,1,v.price,v.price,N'[]'
    FROM @N n JOIN dbo.Orders o ON o.order_code=CONCAT('FG-DEMO-ORDER-',RIGHT(CONCAT('000',n.n),3))
    JOIN @ProductNames pn ON pn.n=((n.n-1)%70)+1 JOIN dbo.Product p ON p.name=pn.name JOIN dbo.ProductVariant v ON v.product_id=p.product_id AND v.is_default=1
    WHERE n.n<=120 AND NOT EXISTS (SELECT 1 FROM dbo.OrderItem oi WHERE oi.order_id=o.order_id);

    INSERT dbo.OrderStatusHistory(order_id,actor_user_id,actor_role,from_status,to_status,note,created_at)
    SELECT o.order_id,NULL,'SYSTEM',NULL,o.order_status,N'FG-DEMO trạng thái hiện tại.',o.created_at
    FROM dbo.Orders o WHERE o.order_code LIKE 'FG-DEMO-ORDER-%' AND NOT EXISTS (SELECT 1 FROM dbo.OrderStatusHistory h WHERE h.order_id=o.order_id);

    INSERT dbo.InventoryReservation(order_id,status,created_at,updated_at)
    SELECT o.order_id,CASE WHEN o.order_status IN ('PENDING','CONFIRMED') THEN 'RESERVED' WHEN o.order_status='CANCELLED' THEN 'RELEASED' ELSE 'CONSUMED' END,o.created_at,@Now
    FROM dbo.Orders o WHERE o.order_code LIKE 'FG-DEMO-ORDER-%' AND NOT EXISTS (SELECT 1 FROM dbo.InventoryReservation r WHERE r.order_id=o.order_id);
    INSERT dbo.InventoryReservationItem(reservation_id,inventory_item_id,quantity)
    SELECT r.reservation_id,d.inventory_item_id,SUM(d.quantity) FROM dbo.Orders o JOIN dbo.OrderItem oi ON oi.order_id=o.order_id JOIN dbo.InventoryReservation r ON r.order_id=o.order_id CROSS APPLY (SELECT ri.inventory_item_id,CONVERT(decimal(19,4),oi.quantity)*ri.quantity/recipe.yield_quantity quantity FROM dbo.Recipe recipe JOIN dbo.RecipeItem ri ON ri.recipe_id=recipe.recipe_id WHERE recipe.variant_id=oi.variant_id AND recipe.active=1 UNION ALL SELECT m.inventory_item_id,CONVERT(decimal(19,4),oi.quantity) FROM dbo.VariantInventoryItem m WHERE m.variant_id=oi.variant_id) d
    WHERE o.order_code LIKE 'FG-DEMO-ORDER-%' AND NOT EXISTS (SELECT 1 FROM dbo.InventoryReservationItem existing WHERE existing.reservation_id=r.reservation_id AND existing.inventory_item_id=d.inventory_item_id) GROUP BY r.reservation_id,d.inventory_item_id;

    INSERT dbo.InventoryTransaction(order_id,inventory_item_id,transaction_type,quantity,created_at,created_by,reason_code,note,quantity_before,quantity_after)
    SELECT o.order_id,ri.inventory_item_id,'RESERVE',ri.quantity,o.created_at,NULL,NULL,N'FG-DEMO reserve',NULL,NULL FROM dbo.Orders o JOIN dbo.InventoryReservation r ON r.order_id=o.order_id JOIN dbo.InventoryReservationItem ri ON ri.reservation_id=r.reservation_id
    WHERE o.order_code LIKE 'FG-DEMO-ORDER-%' AND NOT EXISTS (SELECT 1 FROM dbo.InventoryTransaction t WHERE t.order_id=o.order_id AND t.inventory_item_id=ri.inventory_item_id AND t.transaction_type='RESERVE');
    INSERT dbo.InventoryTransaction(order_id,inventory_item_id,transaction_type,quantity,created_at,created_by,reason_code,note,quantity_before,quantity_after)
    SELECT o.order_id,ri.inventory_item_id,CASE WHEN o.order_status='CANCELLED' THEN 'RELEASE' ELSE 'CONSUME' END,ri.quantity,DATEADD(minute,30,o.created_at),NULL,NULL,N'FG-DEMO inventory completion',NULL,NULL FROM dbo.Orders o JOIN dbo.InventoryReservation r ON r.order_id=o.order_id JOIN dbo.InventoryReservationItem ri ON ri.reservation_id=r.reservation_id
    WHERE o.order_code LIKE 'FG-DEMO-ORDER-%' AND o.order_status NOT IN ('PENDING','CONFIRMED') AND NOT EXISTS (SELECT 1 FROM dbo.InventoryTransaction t WHERE t.order_id=o.order_id AND t.inventory_item_id=ri.inventory_item_id AND t.transaction_type=CASE WHEN o.order_status='CANCELLED' THEN 'RELEASE' ELSE 'CONSUME' END);

    INSERT dbo.PaymentAttempt(order_id,provider,provider_reference,checkout_url,amount,status,lease_token,created_at,updated_at)
    SELECT o.order_id,'PAYOS',CONCAT('FG-DEMO-PAY-',RIGHT(o.order_code,3)),CONCAT('https://pay.payos.vn/web/',o.order_code),o.final_amount,CASE WHEN o.payment_status='PAID' THEN 'PAID' WHEN o.order_status='CANCELLED' THEN 'CANCELLED' ELSE 'READY' END,NULL,o.created_at,@Now
    FROM dbo.Orders o WHERE o.order_code LIKE 'FG-DEMO-ORDER-%' AND o.payment_method='BANK_TRANSFER' AND NOT EXISTS (SELECT 1 FROM dbo.PaymentAttempt p WHERE p.order_id=o.order_id);

    INSERT dbo.CouponRedemption(coupon_id,user_id,order_id,claimed_at,used_at,discount_amount,created_at,updated_at)
    SELECT c.coupon_id,o.user_id,o.order_id,DATEADD(day,-1,o.created_at),o.created_at,0,DATEADD(day,-1,o.created_at),@Now
    FROM @N n JOIN dbo.Orders o ON o.order_code=CONCAT('FG-DEMO-ORDER-',RIGHT(CONCAT('000',n.n),3)) JOIN dbo.Coupon c ON c.code=CONCAT('FG-DEMO-',RIGHT(CONCAT('00',((n.n-1)%6)+1),2))
    WHERE n.n<=30 AND NOT EXISTS (SELECT 1 FROM dbo.CouponRedemption r WHERE r.order_id=o.order_id) AND NOT EXISTS (SELECT 1 FROM dbo.CouponRedemption r WHERE r.user_id=o.user_id AND r.coupon_id=c.coupon_id);

    INSERT dbo.LoyaltyTransaction(user_id,order_id,transaction_type,points,created_at)
    SELECT o.user_id,o.order_id,'EARN',CASE WHEN CONVERT(int,o.final_amount/1000)>0 THEN CONVERT(int,o.final_amount/1000) ELSE 1 END,o.delivered_at
    FROM dbo.Orders o WHERE o.order_code LIKE 'FG-DEMO-ORDER-%' AND o.order_status='DELIVERED' AND o.user_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.LoyaltyTransaction l WHERE l.order_id=o.order_id AND l.transaction_type='EARN');

    INSERT dbo.Review(user_id,order_id,product_id,rating,comment,created_at,updated_at)
    SELECT DISTINCT o.user_id,o.order_id,oi.product_id,4+(o.order_id%2),CASE o.order_id%3 WHEN 0 THEN N'Món ngon, đóng gói đẹp.' WHEN 1 THEN N'Giao nhanh và phục vụ thân thiện.' ELSE N'Trải nghiệm tốt, sẽ đặt lại.' END,DATEADD(hour,4,o.delivered_at),DATEADD(hour,4,o.delivered_at)
    FROM dbo.Orders o JOIN dbo.OrderItem oi ON oi.order_id=o.order_id WHERE o.order_code LIKE 'FG-DEMO-ORDER-%' AND o.order_status='DELIVERED' AND o.user_id IS NOT NULL AND oi.product_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.Review r WHERE r.user_id=o.user_id AND r.order_id=o.order_id AND r.product_id=oi.product_id);

    IF EXISTS (SELECT 1 FROM dbo.Orders o WHERE o.order_code LIKE 'FG-DEMO-ORDER-%' AND o.final_amount<>o.total_amount+o.shipping_fee+o.service_fee-o.discount_amount) THROW 51609, 'FG-DEMO order totals are invalid.', 1;
    IF EXISTS (SELECT 1 FROM dbo.OrderItem oi JOIN dbo.Orders o ON o.order_id=oi.order_id WHERE o.order_code LIKE 'FG-DEMO-ORDER-%' AND oi.total_price<>oi.unit_price*oi.quantity) THROW 51610, 'FG-DEMO order item totals are invalid.', 1;
    IF EXISTS (SELECT 1 FROM dbo.Orders o OUTER APPLY (SELECT TOP (1) h.to_status FROM dbo.OrderStatusHistory h WHERE h.order_id=o.order_id ORDER BY h.created_at DESC,h.history_id DESC) h WHERE o.order_code LIKE 'FG-DEMO-ORDER-%' AND (h.to_status IS NULL OR h.to_status<>o.order_status)) THROW 51611, 'FG-DEMO latest status history is invalid.', 1;
    IF EXISTS (SELECT 1 FROM dbo.Review r JOIN dbo.Orders o ON o.order_id=r.order_id WHERE o.order_code LIKE 'FG-DEMO-ORDER-%' AND o.order_status<>'DELIVERED') THROW 51612, 'FG-DEMO review references non-delivered order.', 1;
    IF EXISTS (SELECT 1 FROM dbo.PaymentAttempt p JOIN dbo.Orders o ON o.order_id=p.order_id WHERE o.order_code LIKE 'FG-DEMO-ORDER-%' AND o.payment_method<>'BANK_TRANSFER') THROW 51613, 'FG-DEMO payment attempt references non-bank-transfer order.', 1;

    COMMIT TRANSACTION;

    SELECT v.table_name,v.seed_rows
    FROM (VALUES
        ('Users',(SELECT COUNT_BIG(*) FROM dbo.Users WHERE email LIKE 'fg-demo-%@fastguy.local')),
        ('Address',(SELECT COUNT_BIG(*) FROM dbo.Address a JOIN dbo.Users u ON u.user_id=a.user_id WHERE u.email LIKE 'fg-demo-%@fastguy.local')),
        ('Category',(SELECT COUNT_BIG(*) FROM dbo.Category WHERE name LIKE N'FG-DEMO %')),
        ('Product',(SELECT COUNT_BIG(*) FROM dbo.Product WHERE name LIKE N'FG-DEMO %')),
        ('ProductVariant',(SELECT COUNT_BIG(*) FROM dbo.ProductVariant WHERE sku LIKE 'FG-DEMO-SKU-%')),
        ('ProductModifierGroup',(SELECT COUNT_BIG(*) FROM dbo.ProductModifierGroup WHERE name LIKE N'FG-DEMO %')),
        ('ProductModifierOption',(SELECT COUNT_BIG(*) FROM dbo.ProductModifierOption WHERE name LIKE N'FG-DEMO %')),
        ('Cart',(SELECT COUNT_BIG(*) FROM dbo.Cart c JOIN dbo.Users u ON u.user_id=c.user_id WHERE u.email LIKE 'fg-demo-%@fastguy.local')),
        ('CartItem',(SELECT COUNT_BIG(*) FROM dbo.CartItem ci JOIN dbo.Cart c ON c.cart_id=ci.cart_id JOIN dbo.Users u ON u.user_id=c.user_id WHERE u.email LIKE 'fg-demo-%@fastguy.local')),
        ('Coupon',(SELECT COUNT_BIG(*) FROM dbo.Coupon WHERE code LIKE 'FG-DEMO-%')),
        ('Banner',(SELECT COUNT_BIG(*) FROM dbo.Banner WHERE title LIKE N'FG-DEMO %')),
        ('Orders',(SELECT COUNT_BIG(*) FROM dbo.Orders WHERE order_code LIKE 'FG-DEMO-ORDER-%')),
        ('OrderItem',(SELECT COUNT_BIG(*) FROM dbo.OrderItem i JOIN dbo.Orders o ON o.order_id=i.order_id WHERE o.order_code LIKE 'FG-DEMO-ORDER-%')),
        ('OrderStatusHistory',(SELECT COUNT_BIG(*) FROM dbo.OrderStatusHistory h JOIN dbo.Orders o ON o.order_id=h.order_id WHERE o.order_code LIKE 'FG-DEMO-ORDER-%')),
        ('InventoryReservation',(SELECT COUNT_BIG(*) FROM dbo.InventoryReservation r JOIN dbo.Orders o ON o.order_id=r.order_id WHERE o.order_code LIKE 'FG-DEMO-ORDER-%')),
        ('InventoryTransaction',(SELECT COUNT_BIG(*) FROM dbo.InventoryTransaction t JOIN dbo.Orders o ON o.order_id=t.order_id WHERE o.order_code LIKE 'FG-DEMO-ORDER-%')),
        ('PaymentAttempt',(SELECT COUNT_BIG(*) FROM dbo.PaymentAttempt p JOIN dbo.Orders o ON o.order_id=p.order_id WHERE o.order_code LIKE 'FG-DEMO-ORDER-%')),
        ('CouponRedemption',(SELECT COUNT_BIG(*) FROM dbo.CouponRedemption r JOIN dbo.Orders o ON o.order_id=r.order_id WHERE o.order_code LIKE 'FG-DEMO-ORDER-%')),
        ('LoyaltyTransaction',(SELECT COUNT_BIG(*) FROM dbo.LoyaltyTransaction l JOIN dbo.Orders o ON o.order_id=l.order_id WHERE o.order_code LIKE 'FG-DEMO-ORDER-%')),
        ('Review',(SELECT COUNT_BIG(*) FROM dbo.Review r JOIN dbo.Orders o ON o.order_id=r.order_id WHERE o.order_code LIKE 'FG-DEMO-ORDER-%')),
        ('WorkShift',(SELECT COUNT_BIG(*) FROM dbo.WorkShift w JOIN dbo.Users u ON u.user_id=w.user_id WHERE u.email LIKE 'fg-demo-%@fastguy.local'))
    ) v(table_name,seed_rows)
    ORDER BY v.table_name;
    SELECT 'fg-demo-001@fastguy.local' AS customer_email,'fg-demo-059@fastguy.local' AS staff_email,'fg-demo-065@fastguy.local' AS shipper_email,'123456' AS demo_password;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO
