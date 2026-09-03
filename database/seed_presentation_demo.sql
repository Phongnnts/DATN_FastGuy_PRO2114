SET NOCOUNT ON;
SET XACT_ABORT ON;
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;
IF DB_NAME()<>N'DemoDatabase' THROW 51800, 'Presentation demo seed target must be DemoDatabase', 1;
IF TRY_CONVERT(int,SESSION_CONTEXT(N'FASTGUY_ALLOW_PRESENTATION_DEMO_SEED'))<>1 THROW 51801, 'Set FASTGUY_ALLOW_PRESENTATION_DEMO_SEED=1 for this session', 1;
IF NOT EXISTS(SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id='065_warehouse_operations_redesign') THROW 51802, 'Migration 065 is required', 1;

DECLARE @ExpectedProducts int=20;
DECLARE @ExpectedIngredients int=20;
DECLARE @ExpectedRecipeLines int=40;
DECLARE @ExpectedOrders int=45;
DECLARE @ExpectedRefundOrders int=3;
DECLARE @ExpectedCodSettlements int=4;
DECLARE @ExpectedDemoShifts int=9;
DECLARE @ExpectedPayRates int=2;

BEGIN TRY
 BEGIN TRANSACTION;

 DECLARE @Now datetime2(0)=SYSDATETIME();
 DECLARE @ActorId int=(SELECT TOP(1) user_id FROM dbo.Users WHERE role_name='ADMIN' AND status='ACTIVE' ORDER BY user_id);
 DECLARE @StaffId int=(SELECT TOP(1) user_id FROM dbo.Users WHERE role_name='STAFF' AND status='ACTIVE' ORDER BY user_id);
 DECLARE @ShipperId int=(SELECT TOP(1) user_id FROM dbo.Users WHERE role_name='SHIPPER' AND status='ACTIVE' ORDER BY user_id);
 IF @ActorId IS NULL OR @StaffId IS NULL OR @ShipperId IS NULL THROW 51803, 'Presentation seed requires active ADMIN, STAFF and SHIPPER users', 1;

 DECLARE @OwnedOrders TABLE(order_id int PRIMARY KEY);
 INSERT @OwnedOrders SELECT order_id FROM dbo.Orders WHERE order_code LIKE 'DEMO-PRES-ORD-%';
 DECLARE @OwnedProducts TABLE(product_id int PRIMARY KEY);
 INSERT @OwnedProducts SELECT product_id FROM dbo.Product WHERE name LIKE N'DEMO-PRES-PROD-%';
 DECLARE @OwnedVariants TABLE(variant_id int PRIMARY KEY);
 INSERT @OwnedVariants SELECT variant_id FROM dbo.ProductVariant WHERE sku LIKE 'DEMO-PRES-SKU-%';
 DECLARE @OwnedItems TABLE(inventory_item_id int PRIMARY KEY);
 INSERT @OwnedItems SELECT inventory_item_id FROM dbo.InventoryItem WHERE inventory_code LIKE 'DEMO-PRES-ING-%';
 DECLARE @OwnedReceipts TABLE(goods_receipt_id int PRIMARY KEY);
 INSERT @OwnedReceipts SELECT goods_receipt_id FROM dbo.GoodsReceipt WHERE invoice_number LIKE N'DEMO-PRES-REC-%';
 DECLARE @OwnedCounts TABLE(stock_count_id int PRIMARY KEY);
 INSERT @OwnedCounts SELECT stock_count_id FROM dbo.StockCount WHERE created_by=@ActorId AND EXISTS(SELECT 1 FROM dbo.StockCountItem sci JOIN @OwnedItems oi ON oi.inventory_item_id=sci.inventory_item_id WHERE sci.stock_count_id=StockCount.stock_count_id);
 DECLARE @OwnedShifts TABLE(shift_id int PRIMARY KEY);
 INSERT @OwnedShifts SELECT shift_id FROM dbo.WorkShift WHERE attendance_note LIKE N'DEMO-PRES-SHIFT%';

 DELETE FROM dbo.CodSettlement WHERE shift_id IN(SELECT shift_id FROM @OwnedShifts);
 DELETE FROM dbo.WorkShift WHERE shift_id IN(SELECT shift_id FROM @OwnedShifts);
 DELETE FROM dbo.StaffPayRate WHERE user_id IN(@StaffId,@ShipperId) AND effective_from=DATEADD(day,-30,CAST(@Now AS date));
 DELETE FROM dbo.Review WHERE order_id IN(SELECT order_id FROM @OwnedOrders);
 DELETE FROM dbo.LoyaltyTransaction WHERE order_id IN(SELECT order_id FROM @OwnedOrders);
 DELETE FROM dbo.CouponRedemption WHERE order_id IN(SELECT order_id FROM @OwnedOrders);
 DELETE FROM dbo.PaymentAttempt WHERE order_id IN(SELECT order_id FROM @OwnedOrders);
 DELETE FROM dbo.OrderStatusHistory WHERE order_id IN(SELECT order_id FROM @OwnedOrders);
 DELETE FROM dbo.InventoryTransaction WHERE order_id IN(SELECT order_id FROM @OwnedOrders) OR goods_receipt_id IN(SELECT goods_receipt_id FROM @OwnedReceipts) OR stock_count_id IN(SELECT stock_count_id FROM @OwnedCounts) OR inventory_item_id IN(SELECT inventory_item_id FROM @OwnedItems);
 DELETE FROM dbo.InventoryReservationItem WHERE reservation_id IN(SELECT reservation_id FROM dbo.InventoryReservation WHERE order_id IN(SELECT order_id FROM @OwnedOrders));
 DELETE FROM dbo.InventoryReservation WHERE order_id IN(SELECT order_id FROM @OwnedOrders);
 DELETE FROM dbo.OrderItem WHERE order_id IN(SELECT order_id FROM @OwnedOrders);
 DELETE FROM dbo.StockCountItem WHERE stock_count_id IN(SELECT stock_count_id FROM @OwnedCounts);
 DELETE FROM dbo.StockCount WHERE stock_count_id IN(SELECT stock_count_id FROM @OwnedCounts);
 DELETE FROM dbo.GoodsReceiptItem WHERE goods_receipt_id IN(SELECT goods_receipt_id FROM @OwnedReceipts);
 DELETE FROM dbo.GoodsReceipt WHERE goods_receipt_id IN(SELECT goods_receipt_id FROM @OwnedReceipts);
 DELETE FROM dbo.Orders WHERE order_id IN(SELECT order_id FROM @OwnedOrders);
 DELETE FROM dbo.RecipeItem WHERE recipe_id IN(SELECT recipe_id FROM dbo.Recipe WHERE variant_id IN(SELECT variant_id FROM @OwnedVariants));
 DELETE FROM dbo.Recipe WHERE variant_id IN(SELECT variant_id FROM @OwnedVariants);
 DELETE FROM dbo.VariantInventoryItem WHERE variant_id IN(SELECT variant_id FROM @OwnedVariants) OR inventory_item_id IN(SELECT inventory_item_id FROM @OwnedItems);
 DELETE FROM dbo.ProductVariant WHERE variant_id IN(SELECT variant_id FROM @OwnedVariants);
 DELETE FROM dbo.Product WHERE product_id IN(SELECT product_id FROM @OwnedProducts);
 DELETE FROM dbo.InventoryItem WHERE inventory_item_id IN(SELECT inventory_item_id FROM @OwnedItems);
 DELETE FROM dbo.Category WHERE description=N'DEMO-PRES-CATEGORY';

 INSERT dbo.Category(name,description,sort_order,status) VALUES(N'Thực đơn trình diễn',N'DEMO-PRES-CATEGORY',90,'ACTIVE');
 DECLARE @CategoryId int=SCOPE_IDENTITY();

 DECLARE @Products TABLE(n int PRIMARY KEY,name nvarchar(255),price decimal(18,2));
 ;WITH n AS(SELECT 1 n UNION ALL SELECT n+1 FROM n WHERE n<20)
 INSERT @Products SELECT n,CONCAT(N'DEMO-PRES-PROD-',RIGHT('00'+CONVERT(varchar(2),n),2),N' Món trình diễn'),30000+n*2500 FROM n;
 INSERT dbo.Product(category_id,name,description,base_price,gallery_images,is_new,spice_level,status,available_from,available_to,created_at,updated_at)
 SELECT @CategoryId,name,CONCAT(N'Dữ liệu trình diễn số ',n),price,N'[]',IIF(n<=5,1,0),n%4,IIF(n=20,'UNAVAILABLE','AVAILABLE'),'08:00','22:00',DATEADD(day,-30,@Now),@Now FROM @Products;
 INSERT dbo.ProductVariant(product_id,variant_name,price,original_price,sku,quantity_available,inventory_mode,weight,length,width,height,is_default,status,created_at,updated_at)
 SELECT p.product_id,N'Tiêu chuẩn',x.price,IIF(x.n%4=0,x.price+10000,NULL),CONCAT('DEMO-PRES-SKU-',RIGHT('00'+CONVERT(varchar(2),x.n),2)),NULL,'INGREDIENT',400+x.n*10,20,20,10,1,IIF(x.n=20,'UNAVAILABLE','AVAILABLE'),DATEADD(day,-30,@Now),@Now
 FROM @Products x JOIN dbo.Product p ON p.name=x.name;

 DECLARE @Ingredients TABLE(n int PRIMARY KEY,code varchar(30),unit varchar(10),qty decimal(19,4),cost decimal(19,4));
 ;WITH n AS(SELECT 1 n UNION ALL SELECT n+1 FROM n WHERE n<20)
 INSERT @Ingredients SELECT n,CONCAT('DEMO-PRES-ING-',RIGHT('00'+CONVERT(varchar(2),n),2)),CASE n%3 WHEN 0 THEN 'G' WHEN 1 THEN 'ML' ELSE 'PIECE' END,5000+n*250,10+n*5 FROM n;
 INSERT dbo.InventoryItem(name,item_type,base_unit,inventory_code,count_frequency,average_unit_cost,on_hand_quantity,reserved_quantity,minimum_quantity,active,created_at,updated_at)
 SELECT CONCAT(N'Nguyên liệu trình diễn ',n),'INGREDIENT',unit,code,IIF(n%2=0,'DAILY','WEEKLY'),cost,qty,0,500,1,DATEADD(day,-30,@Now),@Now FROM @Ingredients;
 INSERT dbo.Recipe(variant_id,yield_quantity,active,created_at,updated_at)
 SELECT variant_id,1,1,DATEADD(day,-30,@Now),@Now FROM dbo.ProductVariant WHERE sku LIKE 'DEMO-PRES-SKU-%';
 INSERT dbo.RecipeItem(recipe_id,inventory_item_id,quantity)
 SELECT r.recipe_id,i.inventory_item_id,CASE x.line_no WHEN 1 THEN 1 ELSE 25 END
 FROM @Products p CROSS JOIN(VALUES(1),(2))x(line_no)
 JOIN dbo.ProductVariant v ON v.sku=CONCAT('DEMO-PRES-SKU-',RIGHT('00'+CONVERT(varchar(2),p.n),2))
 JOIN dbo.Recipe r ON r.variant_id=v.variant_id
 JOIN dbo.InventoryItem i ON i.inventory_code=CONCAT('DEMO-PRES-ING-',RIGHT('00'+CONVERT(varchar(2),CASE WHEN x.line_no=1 THEN p.n ELSE p.n%20+1 END),2));

 IF EXISTS(SELECT 1 FROM dbo.Coupon WHERE code='DEMO-PRES-CPN-10')
  UPDATE dbo.Coupon SET type='PERCENT',value=10,min_order=50000,max_discount=20000,max_uses=100,expires_at=DATEADD(day,30,@Now),is_active=1,is_public=1,updated_at=@Now WHERE code='DEMO-PRES-CPN-10';
 ELSE
  INSERT dbo.Coupon(code,type,value,min_order,max_discount,max_uses,used_count,expires_at,is_active,is_public,created_at,updated_at)
  VALUES('DEMO-PRES-CPN-10','PERCENT',10,50000,20000,100,0,DATEADD(day,30,@Now),1,1,@Now,@Now);

 DECLARE @Orders TABLE(n int PRIMARY KEY,order_id int,status varchar(30),payment_method varchar(50),payment_status varchar(20),created_at datetime2(0),amount decimal(18,2));
 ;WITH n AS(SELECT 1 n UNION ALL SELECT n+1 FROM n WHERE n<45)
 INSERT dbo.Orders(order_code,idempotency_key,customer_name,customer_phone,customer_address,total_amount,shipping_fee,service_fee,final_amount,cod_collected_amount,cod_collected_at,shipping_provider,payment_method,payment_status,paid_at,order_status,status_entered_at,confirmed_at,ready_at,picked_up_at,delivered_at,cancelled_at,failure_reason,delivery_attempt_count,delivery_attempt_limit,delivery_failure_code,delivery_failed_at,returned_to_store_at,cancelled_by,coupon_code,discount_amount,delivery_note,created_at,updated_at)
 OUTPUT CONVERT(int,RIGHT(inserted.order_code,3)),inserted.order_id,inserted.order_status,inserted.payment_method,inserted.payment_status,inserted.created_at,inserted.final_amount INTO @Orders(n,order_id,status,payment_method,payment_status,created_at,amount)
 SELECT CONCAT('DEMO-PRES-ORD-',RIGHT('000'+CONVERT(varchar(3),n),3)),CONCAT('DEMO-PRES-IDEMP-',RIGHT('000'+CONVERT(varchar(3),n),3)),CONCAT(N'Khách trình diễn ',n),CONCAT('090000',RIGHT('0000'+CONVERT(varchar(4),n),4)),CONCAT(N'Địa chỉ trình diễn ',n),
        30000+(((n-1)%20)+1)*2500,15000,0,45000+(((n-1)%20)+1)*2500,NULL,NULL,'GHN',IIF(n%3=0,'BANK_TRANSFER','COD'),
        CASE WHEN n<=7 THEN 'PAID' WHEN n%10=0 THEN 'FAILED' WHEN n%10=1 THEN 'UNPAID' ELSE 'PAID' END,
        CASE WHEN n<=7 OR n%10 NOT IN(0,1) THEN DATEADD(minute,10,DATEADD(day,-((n-1)%30),@Now)) END,
        CASE WHEN n<=7 THEN 'DELIVERED' ELSE CASE n%10 WHEN 0 THEN 'CANCELLED' WHEN 1 THEN 'PENDING' WHEN 2 THEN 'CONFIRMED' WHEN 3 THEN 'PREPARING' WHEN 4 THEN 'READY' WHEN 5 THEN 'ASSIGNED' WHEN 6 THEN 'PICKED_UP' WHEN 7 THEN 'DELIVERY_FAILED' WHEN 8 THEN 'RETURNED_TO_STORE' ELSE 'DELIVERED' END END,
        DATEADD(hour,2,DATEADD(day,-((n-1)%30),@Now)),
        CASE WHEN n<=7 OR n%10 IN(2,3,4,5,6,7,8,9) THEN DATEADD(minute,15,DATEADD(day,-((n-1)%30),@Now)) END,
        CASE WHEN n<=7 OR n%10 IN(4,5,6,7,8,9) THEN DATEADD(minute,30,DATEADD(day,-((n-1)%30),@Now)) END,
        CASE WHEN n<=7 OR n%10 IN(6,7,8,9) THEN DATEADD(minute,45,DATEADD(day,-((n-1)%30),@Now)) END,
        CASE WHEN n<=7 OR n%10=9 THEN DATEADD(hour,2,DATEADD(day,-((n-1)%30),@Now)) END,
        CASE WHEN n>7 AND n%10=0 THEN DATEADD(hour,1,DATEADD(day,-((n-1)%30),@Now)) END,
        CASE WHEN n>7 AND n%10 IN(7,8) THEN N'Khách không liên lạc được' END,
        IIF(n>7 AND n%10 IN(7,8),1,0),2,CASE WHEN n>7 AND n%10 IN(7,8) THEN 'CUSTOMER_UNREACHABLE' END,
        CASE WHEN n>7 AND n%10 IN(7,8) THEN DATEADD(hour,1,DATEADD(day,-((n-1)%30),@Now)) END,
        CASE WHEN n>7 AND n%10=8 THEN DATEADD(hour,3,DATEADD(day,-((n-1)%30),@Now)) END,
        CASE WHEN n>7 AND n%10=0 THEN 'CUSTOMER' END,NULL,0,N'Dữ liệu trình diễn',DATEADD(day,-((n-1)%30),@Now),@Now FROM n;

 INSERT dbo.OrderItem(order_id,product_id,variant_id,product_name,variant_name,quantity,unit_price,total_price,unit_cost_snapshot,total_cost_snapshot,modifiers_json)
 SELECT o.order_id,p.product_id,v.variant_id,p.name,v.variant_name,1,v.price,v.price,v.price*.35,v.price*.35,N'[]'
 FROM @Orders o JOIN dbo.ProductVariant v ON v.sku=CONCAT('DEMO-PRES-SKU-',RIGHT('00'+CONVERT(varchar(2),((o.n-1)%20)+1),2)) JOIN dbo.Product p ON p.product_id=v.product_id;
 INSERT dbo.OrderStatusHistory(order_id,actor_user_id,actor_role,from_status,to_status,note,created_at)
 SELECT order_id,@ActorId,'SYSTEM',NULL,status,N'DEMO-PRES-HISTORY',created_at FROM @Orders;
 INSERT dbo.PaymentAttempt(order_id,provider,provider_reference,amount,status,created_at,updated_at)
 SELECT order_id,IIF(payment_method='BANK_TRANSFER','PAYOS','COD'),CONCAT('DEMO-PRES-PAY-',RIGHT('000'+CONVERT(varchar(3),n),3)),amount,CASE payment_status WHEN 'PAID' THEN 'PAID' WHEN 'FAILED' THEN 'FAILED' ELSE 'PENDING' END,created_at,@Now FROM @Orders;
 INSERT dbo.LoyaltyTransaction(user_id,order_id,transaction_type,points,created_at)
 SELECT @ActorId,order_id,'EARN',10+n,created_at FROM @Orders WHERE status='DELIVERED';
 INSERT dbo.Review(user_id,order_id,product_id,rating,comment,is_featured,homepage_consent,created_at,updated_at)
 SELECT @ActorId,o.order_id,oi.product_id,3+o.n%3,N'Đánh giá trình diễn',IIF(o.n%4=0,1,0),IIF(o.n%4=0,1,0),o.created_at,@Now FROM @Orders o JOIN dbo.OrderItem oi ON oi.order_id=o.order_id WHERE o.status='DELIVERED';

 UPDATE dbo.Orders SET refund_status='PENDING',refund_note=N'Yêu cầu hoàn tiền trình diễn' WHERE order_code='DEMO-PRES-ORD-009';
 UPDATE dbo.Orders SET refund_status='REFUNDED',refund_amount=final_amount,refunded_at=DATEADD(hour,4,delivered_at),refund_note=N'Đã hoàn tiền trình diễn',refund_processed_by=@ActorId,refund_reference=N'DEMO-PRES-REFUND-019' WHERE order_code='DEMO-PRES-ORD-019';
 UPDATE dbo.Orders SET refund_status='REJECTED',refund_note=N'Từ chối do không đủ bằng chứng',refund_processed_by=@ActorId WHERE order_code='DEMO-PRES-ORD-029';

 INSERT dbo.StaffPayRate(user_id,effective_from,regular_hourly_rate,overtime_hourly_rate,created_by,created_at)
 VALUES(@StaffId,DATEADD(day,-30,CAST(@Now AS date)),30000,45000,@ActorId,@Now),(@ShipperId,DATEADD(day,-30,CAST(@Now AS date)),28000,42000,@ActorId,@Now);
 DECLARE @ShiftDates TABLE(n int PRIMARY KEY,shift_date date);
 ;WITH n AS(SELECT 1 n UNION ALL SELECT n+1 FROM n WHERE n<30),available AS(SELECT TOP(4) ROW_NUMBER() OVER(ORDER BY n) rn,DATEADD(day,-n,CAST(@Now AS date)) shift_date FROM n WHERE NOT EXISTS(SELECT 1 FROM dbo.WorkShift w WHERE w.shift_date=DATEADD(day,-n,CAST(@Now AS date))) ORDER BY n)
 INSERT @ShiftDates SELECT rn,shift_date FROM available;
 IF (SELECT COUNT(*) FROM @ShiftDates)<>4 THROW 51809, 'Presentation seed requires four collision-free shift dates', 1;
 INSERT dbo.WorkShift(user_id,shift_date,start_time,end_time,shift_code,check_in_source,check_out_source,staff_role_snapshot,check_in_at,check_out_at,status,attendance_status,approved_minutes,approved_overtime_minutes,attendance_note,approved_by,approved_at,pay_snapshot_status,regular_hourly_rate_snapshot,overtime_hourly_rate_snapshot,regular_pay_amount,overtime_pay_amount,total_pay_amount,created_at,updated_at)
 SELECT @ShipperId,shift_date,'08:00','12:00','MORNING','MANUAL','MANUAL','NON_STAFF',DATEADD(hour,8,CAST(shift_date AS datetime2)),DATEADD(hour,12,CAST(shift_date AS datetime2)),'CHECKED_OUT','APPROVED',240,30,CONCAT(N'DEMO-PRES-SHIFT-COD-',n),@ActorId,DATEADD(hour,13,CAST(shift_date AS datetime2)),'CALCULATED',28000,42000,112000,21000,133000,@Now,@Now FROM @ShiftDates;
 INSERT dbo.WorkShift(user_id,shift_date,start_time,end_time,shift_code,staff_role_snapshot,status,attendance_status,attendance_note,created_at,updated_at)
 SELECT @StaffId,shift_date,'12:00','16:00','AFTERNOON','STAFF','SCHEDULED','PENDING',CONCAT(N'DEMO-PRES-SHIFT-STAFF-',n),@Now,@Now FROM @ShiftDates
 UNION ALL SELECT @StaffId,(SELECT shift_date FROM @ShiftDates WHERE n=1),'16:00','21:00','EVENING','STAFF','SCHEDULED',NULL,N'DEMO-PRES-SHIFT-STAFF-5',@Now,@Now;
 INSERT dbo.CodSettlement(shipper_id,shift_id,received_by,status,expected_amount,submitted_amount,verified_amount,reason,submitted_at,verified_at,created_at,updated_at)
 SELECT @ShipperId,w.shift_id,CASE x.n WHEN 1 THEN NULL ELSE @ActorId END,CASE x.n WHEN 1 THEN 'SUBMITTED' WHEN 2 THEN 'SETTLED' WHEN 3 THEN 'SHORT' ELSE 'OVER' END,100000,100000,CASE x.n WHEN 1 THEN NULL WHEN 2 THEN 100000 WHEN 3 THEN 90000 ELSE 110000 END,CASE x.n WHEN 3 THEN N'DEMO-PRES-COD thiếu tiền' WHEN 4 THEN N'DEMO-PRES-COD thừa tiền' END,DATEADD(hour,13,CAST(w.shift_date AS datetime2)),CASE WHEN x.n=1 THEN NULL ELSE DATEADD(hour,14,CAST(w.shift_date AS datetime2)) END,@Now,@Now
 FROM @ShiftDates x JOIN dbo.WorkShift w ON w.shift_date=x.shift_date AND w.user_id=@ShipperId AND w.attendance_note=CONCAT(N'DEMO-PRES-SHIFT-COD-',x.n);

 INSERT dbo.GoodsReceipt(supplier_name,invoice_number,received_at,status,created_by,approved_by,created_at,approved_at)
 VALUES(N'Nhà cung cấp trình diễn',N'DEMO-PRES-REC-001',DATEADD(day,-15,@Now),'APPROVED',@ActorId,@ActorId,DATEADD(day,-15,@Now),DATEADD(day,-15,@Now));
 DECLARE @ReceiptId int=SCOPE_IDENTITY();
 INSERT dbo.GoodsReceiptItem(goods_receipt_id,inventory_item_id,purchase_quantity,purchase_unit,conversion_factor,base_quantity,purchase_unit_price,line_total,average_cost_before,average_cost_after)
 SELECT @ReceiptId,inventory_item_id,100,N'Đơn vị',1,100,average_unit_cost,100*average_unit_cost,average_unit_cost,average_unit_cost FROM dbo.InventoryItem WHERE inventory_code LIKE 'DEMO-PRES-ING-%';
 INSERT dbo.InventoryTransaction(inventory_item_id,transaction_type,quantity,quantity_before,quantity_after,reference_type,reference_id,unit_cost_snapshot,total_cost,goods_receipt_id,created_by,created_at)
 SELECT inventory_item_id,'RECEIPT',100,on_hand_quantity-100,on_hand_quantity,'GOODS_RECEIPT',N'DEMO-PRES-REC-001',average_unit_cost,100*average_unit_cost,@ReceiptId,@ActorId,DATEADD(day,-15,@Now) FROM dbo.InventoryItem WHERE inventory_code LIKE 'DEMO-PRES-ING-%';
 INSERT dbo.InventoryTransaction(inventory_item_id,transaction_type,quantity,quantity_before,quantity_after,reference_type,reference_id,reason_code,note,unit_cost_snapshot,total_cost,created_by,created_at)
 SELECT TOP(4) inventory_item_id,'WASTE',-5,on_hand_quantity+5,on_hand_quantity,'DEMO_SEED',inventory_code,'SPOILAGE',N'Hao hụt trình diễn',average_unit_cost,5*average_unit_cost,@ActorId,DATEADD(day,-5,@Now) FROM dbo.InventoryItem WHERE inventory_code LIKE 'DEMO-PRES-ING-%' ORDER BY inventory_code;

 INSERT dbo.StockCount(count_date,frequency,status,created_by,approved_by,created_at,approved_at) VALUES(CAST(@Now AS date),'DAILY','APPROVED',@ActorId,@ActorId,@Now,@Now);
 DECLARE @CountId int=SCOPE_IDENTITY();
 INSERT dbo.StockCountItem(stock_count_id,inventory_item_id,theoretical_quantity,actual_quantity,variance_quantity,unit_cost_snapshot,reserved_quantity_snapshot,variance_cost,reason_code,note)
 SELECT @CountId,inventory_item_id,on_hand_quantity,on_hand_quantity,0,average_unit_cost,reserved_quantity,0,'MATCHED',N'Kiểm kê trình diễn' FROM dbo.InventoryItem WHERE inventory_code LIKE 'DEMO-PRES-ING-%';
 INSERT dbo.InventoryTransaction(inventory_item_id,transaction_type,quantity,quantity_before,quantity_after,reference_type,reference_id,reason_code,note,unit_cost_snapshot,total_cost,stock_count_id,created_by,created_at)
 SELECT TOP(1) inventory_item_id,'ADJUSTMENT',1,on_hand_quantity-1,on_hand_quantity,'STOCK_COUNT',CONVERT(varchar(100),@CountId),'COUNT_CORRECTION',N'Điều chỉnh kiểm kê trình diễn',average_unit_cost,average_unit_cost,@CountId,@ActorId,@Now FROM dbo.InventoryItem WHERE inventory_code LIKE 'DEMO-PRES-ING-%' ORDER BY inventory_code;

 IF (SELECT COUNT(*) FROM dbo.Product WHERE name LIKE N'DEMO-PRES-PROD-%')<>@ExpectedProducts THROW 51804, 'Unexpected presentation product count', 1;
 IF (SELECT COUNT(*) FROM dbo.InventoryItem WHERE inventory_code LIKE 'DEMO-PRES-ING-%')<>@ExpectedIngredients THROW 51805, 'Unexpected presentation ingredient count', 1;
 IF (SELECT COUNT(*) FROM dbo.RecipeItem ri JOIN dbo.Recipe r ON r.recipe_id=ri.recipe_id JOIN dbo.ProductVariant v ON v.variant_id=r.variant_id WHERE v.sku LIKE 'DEMO-PRES-SKU-%')<>@ExpectedRecipeLines THROW 51806, 'Unexpected presentation recipe line count', 1;
 IF (SELECT COUNT(*) FROM dbo.Orders WHERE order_code LIKE 'DEMO-PRES-ORD-%')<>@ExpectedOrders THROW 51807, 'Unexpected presentation order count', 1;
 COMMIT;
END TRY
BEGIN CATCH
 IF XACT_STATE()<>0 ROLLBACK;
 THROW;
END CATCH;
