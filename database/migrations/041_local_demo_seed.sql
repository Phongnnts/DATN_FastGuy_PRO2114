USE FastGuyDB;
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;
GO
IF TRY_CONVERT(int, SESSION_CONTEXT(N'FASTGUY_ALLOW_DEMO_SEED')) <> 1 THROW 51500, 'Local demo seed blocked. Set SESSION_CONTEXT FASTGUY_ALLOW_DEMO_SEED=1 explicitly.', 1;
IF NOT EXISTS (SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id='040_production_hardening') THROW 51501, 'Run 040_production_hardening.sql first.', 1;
BEGIN TRY
    BEGIN TRANSACTION;
    DECLARE @staff int=(SELECT TOP (1) user_id FROM dbo.Users WHERE role_name='STAFF' AND status='ACTIVE' ORDER BY user_id);
    DECLARE @shipper int=(SELECT TOP (1) user_id FROM dbo.Users WHERE role_name='SHIPPER' AND status='ACTIVE' ORDER BY user_id);
    DECLARE @product int, @variant int, @price decimal(18,2), @productName nvarchar(255), @variantName nvarchar(255), @today date=CAST(GETDATE() AS date), @now datetime2(0)=GETDATE();
    SELECT TOP (1) @product=p.product_id,@variant=v.variant_id,@price=v.price,@productName=p.name,@variantName=v.variant_name FROM dbo.Product p JOIN dbo.ProductVariant v ON v.product_id=p.product_id WHERE p.status='AVAILABLE' AND v.status='AVAILABLE' ORDER BY v.is_default DESC,p.product_id,v.variant_id;
    IF @staff IS NULL OR @shipper IS NULL OR @variant IS NULL THROW 51502, 'Demo seed requires active STAFF, SHIPPER, product and variant.', 1;

    IF NOT EXISTS (SELECT 1 FROM dbo.WorkShift WHERE user_id=@staff AND shift_date=@today AND status='CHECKED_IN')
       AND NOT EXISTS (SELECT 1 FROM dbo.WorkShift WHERE user_id=@staff AND shift_date=@today AND status<>'CANCELLED' AND start_time<'23:59:59' AND end_time>'00:00:00')
        INSERT dbo.WorkShift(user_id,shift_date,start_time,end_time,check_in_at,status,created_at,updated_at) VALUES(@staff,@today,'00:00:00','23:59:59',CAST(@today AS datetime2),'CHECKED_IN',@now,@now);
    IF NOT EXISTS (SELECT 1 FROM dbo.WorkShift WHERE user_id=@shipper AND shift_date=@today AND status='CHECKED_IN')
       AND NOT EXISTS (SELECT 1 FROM dbo.WorkShift WHERE user_id=@shipper AND shift_date=@today AND status<>'CANCELLED' AND start_time<'23:59:59' AND end_time>'00:00:00')
        INSERT dbo.WorkShift(user_id,shift_date,start_time,end_time,check_in_at,status,created_at,updated_at) VALUES(@shipper,@today,'00:00:00','23:59:59',CAST(@today AS datetime2),'CHECKED_IN',@now,@now);

    IF NOT EXISTS (SELECT 1 FROM dbo.ProductModifierGroup WHERE product_id=@product)
    BEGIN
        INSERT dbo.ProductModifierGroup(product_id,name,min_selections,max_selections,is_active,sort_order) VALUES(@product,N'FG MVP demo options',0,1,1,999);
        DECLARE @group int=SCOPE_IDENTITY();
        INSERT dbo.ProductModifierOption(modifier_group_id,name,price,is_active,sort_order) VALUES(@group,N'No extra',0,1,1),(@group,N'Demo extra',5000,1,2);
    END;

    DECLARE @targets TABLE(status varchar(30) PRIMARY KEY, code varchar(50), staff_id int NULL, shipper_id int NULL, assigned_at datetime2(0) NULL, confirmed_at datetime2(0) NULL, ready_at datetime2(0) NULL, picked_up_at datetime2(0) NULL);
    INSERT @targets VALUES
      ('PENDING','FG-MVP-PENDING',NULL,NULL,NULL,NULL,NULL,NULL),
      ('PREPARING','FG-MVP-PREPARING',@staff,NULL,NULL,DATEADD(minute,-30,@now),NULL,NULL),
      ('ASSIGNED','FG-MVP-ASSIGNED',@staff,@shipper,DATEADD(minute,-10,@now),DATEADD(minute,-40,@now),DATEADD(minute,-20,@now),NULL),
      ('PICKED_UP','FG-MVP-PICKED-UP',@staff,@shipper,DATEADD(minute,-20,@now),DATEADD(minute,-50,@now),DATEADD(minute,-30,@now),DATEADD(minute,-10,@now));
    IF NOT EXISTS (SELECT 1 FROM dbo.Orders WHERE order_status='READY' AND shipper_id IS NULL) INSERT @targets VALUES('READY','FG-MVP-READY-UNASSIGNED',@staff,NULL,NULL,DATEADD(minute,-30,@now),DATEADD(minute,-10,@now),NULL);

    DECLARE @status varchar(30),@code varchar(50),@sid int,@shid int,@assigned datetime2(0),@confirmed datetime2(0),@ready datetime2(0),@picked datetime2(0),@order int;
    DECLARE c CURSOR LOCAL FAST_FORWARD FOR SELECT status,code,staff_id,shipper_id,assigned_at,confirmed_at,ready_at,picked_up_at FROM @targets t WHERE NOT EXISTS (SELECT 1 FROM dbo.Orders o WHERE o.order_status=t.status) AND NOT EXISTS (SELECT 1 FROM dbo.Orders o WHERE o.order_code=t.code);
    OPEN c; FETCH NEXT FROM c INTO @status,@code,@sid,@shid,@assigned,@confirmed,@ready,@picked;
    WHILE @@FETCH_STATUS=0
    BEGIN
        INSERT dbo.Orders(order_code,user_id,customer_name,customer_phone,customer_address,total_amount,shipping_fee,service_fee,final_amount,shipping_provider,payment_method,payment_status,order_status,staff_id,shipper_id,assigned_at,confirmed_at,ready_at,picked_up_at,discount_amount,internal_note,created_at,updated_at)
        VALUES(@code,NULL,N'FG MVP Demo','0900000000',N'Local demo address',@price,0,0,@price,'GHN','COD','UNPAID',@status,@sid,@shid,@assigned,@confirmed,@ready,@picked,0,N'[SYSTEM:FG-MVP-DEMO] Local-only synthetic order.',DATEADD(hour,-1,@now),@now);
        SET @order=SCOPE_IDENTITY();
        INSERT dbo.OrderItem(order_id,product_id,variant_id,product_name,variant_name,quantity,unit_price,total_price,modifiers_json) VALUES(@order,@product,@variant,@productName,@variantName,1,@price,@price,N'[]');
        INSERT dbo.OrderStatusHistory(order_id,actor_user_id,actor_role,from_status,to_status,note,created_at) VALUES(@order,NULL,'SYSTEM',NULL,@status,N'FG MVP local demo snapshot.',DATEADD(hour,-1,@now));
        FETCH NEXT FROM c INTO @status,@code,@sid,@shid,@assigned,@confirmed,@ready,@picked;
    END;
    CLOSE c; DEALLOCATE c;
    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF CURSOR_STATUS('local','c')>=0 CLOSE c;
    IF CURSOR_STATUS('local','c')>-3 DEALLOCATE c;
    IF XACT_STATE()<>0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO
