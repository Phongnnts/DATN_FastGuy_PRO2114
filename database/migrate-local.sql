use FastGuyDB;
go
set ansi_nulls on;
set quoted_identifier on;
set ansi_padding on;
set ansi_warnings on;
set arithabort on;
set concat_null_yields_null on;
go

if exists (select 1 from Orders where order_status = 'WAITING_STOCK_CONFIRM')
    throw 51000, 'Legacy WAITING_STOCK_CONFIRM orders require manual stock reconciliation before migration', 1;
go

if col_length('Product', 'available_from') is null alter table Product add available_from time null;
if col_length('Product', 'available_to') is null alter table Product add available_to time null;
if object_id('DeliveryZone', 'U') is not null and col_length('DeliveryZone', 'ward_name') is null alter table DeliveryZone add ward_name nvarchar(100) null;
if col_length('Users', 'loyalty_points') is null alter table Users add loyalty_points int not null constraint DF_Users_LoyaltyPoints default 0;
if col_length('CartItem', 'selected_modifier_option_ids') is null alter table CartItem add selected_modifier_option_ids varchar(500) null;
if col_length('Orders', 'service_fee') is null alter table Orders add service_fee decimal(10,2) not null constraint DF_Orders_ServiceFee default 0;
if col_length('Orders', 'cod_collected_amount') is null alter table Orders add cod_collected_amount decimal(10,2) null;
if col_length('Orders', 'cod_collected_at') is null alter table Orders add cod_collected_at datetime2 null;
if col_length('Orders', 'cancelled_by') is null alter table Orders add cancelled_by varchar(20) null;
if col_length('Orders', 'refund_status') is null alter table Orders add refund_status varchar(20) null;
if col_length('Orders', 'refund_amount') is null alter table Orders add refund_amount decimal(10,2) null;
if col_length('Orders', 'refunded_at') is null alter table Orders add refunded_at datetime2 null;
if col_length('Orders', 'refund_note') is null alter table Orders add refund_note nvarchar(500) null;
if col_length('Orders', 'payos_payment_link_id') is null alter table Orders add payos_payment_link_id varchar(100) null;
if col_length('Orders', 'payos_checkout_url') is null alter table Orders add payos_checkout_url varchar(500) null;
if col_length('Orders', 'idempotency_key') is null alter table Orders add idempotency_key varchar(100) null;
if col_length('Orders', 'request_hash') is null alter table Orders add request_hash varchar(64) null;
if col_length('Orders', 'idempotency_owner') is null alter table Orders add idempotency_owner varchar(100) null;
if col_length('Orders', 'updated_at') is null alter table Orders add updated_at datetime2 null;
if col_length('Users', 'updated_at') is null alter table Users add updated_at datetime2 null;
if object_id('PasswordResetToken', 'U') is not null and col_length('PasswordResetToken', 'updated_at') is null alter table PasswordResetToken add updated_at datetime2 null;
if object_id('Address', 'U') is not null and col_length('Address', 'updated_at') is null alter table Address add updated_at datetime2 null;
if col_length('Cart', 'updated_at') is null alter table Cart add updated_at datetime2 null;
if col_length('CartItem', 'updated_at') is null alter table CartItem add updated_at datetime2 null;
if col_length('Coupon', 'updated_at') is null alter table Coupon add updated_at datetime2 null;
if object_id('WorkShift', 'U') is not null and col_length('WorkShift', 'updated_at') is null alter table WorkShift add updated_at datetime2 null;
if object_id('Notification', 'U') is not null and col_length('Notification', 'updated_at') is null alter table Notification add updated_at datetime2 null;
if object_id('Banner', 'U') is not null and col_length('Banner', 'updated_at') is null alter table Banner add updated_at datetime2 null;
if col_length('OrderItem', 'modifiers_json') is null alter table OrderItem add modifiers_json nvarchar(max) null;
if object_id('OrderItemModifier', 'U') is not null
begin
    exec sp_executesql N'
        update oi set modifiers_json = (
            select om.modifier_option_id as modifierOptionId, om.group_name as groupName,
                   om.option_name as optionName, om.price
            from OrderItemModifier om where om.order_item_id = oi.order_item_id for json path
        )
        from OrderItem oi
        where (oi.modifiers_json is null or oi.modifiers_json = N''[]'')
          and exists (select 1 from OrderItemModifier om where om.order_item_id = oi.order_item_id);';
end;
update Orders set updated_at = coalesce(updated_at, created_at, getdate()) where updated_at is null;
update Users set updated_at = coalesce(updated_at, created_at, getdate()) where updated_at is null;
if object_id('PasswordResetToken', 'U') is not null exec sp_executesql N'update PasswordResetToken set updated_at = coalesce(updated_at, created_at, getdate()) where updated_at is null';
if object_id('Address', 'U') is not null exec sp_executesql N'update Address set updated_at = coalesce(updated_at, created_at, getdate()) where updated_at is null';
update Cart set updated_at = coalesce(updated_at, created_at, getdate()) where updated_at is null;
update CartItem set updated_at = coalesce(updated_at, created_at, getdate()) where updated_at is null;
update Coupon set updated_at = coalesce(updated_at, created_at, getdate()) where updated_at is null;
if object_id('WorkShift', 'U') is not null exec sp_executesql N'update WorkShift set updated_at = coalesce(updated_at, created_at, getdate()) where updated_at is null';
if object_id('Notification', 'U') is not null exec sp_executesql N'update Notification set updated_at = coalesce(updated_at, created_at, getdate()) where updated_at is null';
if object_id('Banner', 'U') is not null exec sp_executesql N'update Banner set updated_at = coalesce(updated_at, created_at, getdate()) where updated_at is null';
update OrderItem set modifiers_json = N'[]' where modifiers_json is null;
go

if exists (
    select 1 from sys.indexes i
    where i.name = 'UQ_Orders_IdempotencyKey' and i.object_id = object_id('Orders')
      and (i.is_unique = 0 or i.has_filter = 0 or i.filter_definition <> N'([idempotency_key] IS NOT NULL)'
           or not exists (select 1 from sys.index_columns ic join sys.columns c on c.object_id = ic.object_id and c.column_id = ic.column_id
                          where ic.object_id = i.object_id and ic.index_id = i.index_id and ic.key_ordinal = 1 and c.name = 'idempotency_key'))
)
    drop index UQ_Orders_IdempotencyKey on Orders;
if not exists (select 1 from sys.indexes where name = 'UQ_Orders_IdempotencyKey' and object_id = object_id('Orders'))
    create unique index UQ_Orders_IdempotencyKey on Orders(idempotency_key) where idempotency_key is not null;
go

if object_id('PaymentAttempt', 'U') is null
begin
    create table PaymentAttempt (
        payment_attempt_id int identity primary key,
        order_id int not null unique references Orders(order_id),
        provider varchar(20) not null,
        provider_reference varchar(100) null,
        checkout_url varchar(500) null,
        amount decimal(10,2) not null,
        status varchar(20) not null,
        lease_token varchar(36) null,
        created_at datetime2 not null default getdate(),
        updated_at datetime2 not null default getdate()
    );
end;
if col_length('PaymentAttempt', 'lease_token') is null alter table PaymentAttempt add lease_token varchar(36) null;
go

if object_id('InventoryReservation', 'U') is null
begin
    create table InventoryReservation (
        reservation_id int identity primary key,
        order_id int not null references Orders(order_id),
        variant_id int not null references ProductVariant(variant_id),
        quantity int not null check (quantity > 0),
        status varchar(20) not null check (status in ('RESERVED', 'CONSUMED', 'RELEASED')),
        created_at datetime2 not null default getdate(),
        updated_at datetime2 not null default getdate(),
        constraint UQ_InventoryReservation_Order_Variant unique(order_id, variant_id)
    );
end;
if object_id('InventoryTransaction', 'U') is null
begin
    create table InventoryTransaction (
        inventory_transaction_id int identity primary key,
        order_id int not null references Orders(order_id),
        variant_id int not null references ProductVariant(variant_id),
        transaction_type varchar(20) not null check (transaction_type in ('RESERVE', 'RELEASE', 'CONSUME', 'RETURN', 'ADJUSTMENT')),
        quantity int not null check (quantity > 0),
        created_at datetime2 not null default getdate()
    );
end;
insert into InventoryReservation(order_id, variant_id, quantity, status)
select oi.order_id, oi.variant_id, sum(oi.quantity),
       case when o.order_status in ('PENDING', 'CONFIRMED') then 'RESERVED' else 'CONSUMED' end
from OrderItem oi
join Orders o on o.order_id = oi.order_id
where oi.variant_id is not null
  and o.order_status in ('PENDING', 'CONFIRMED', 'PREPARING', 'READY', 'PICKED_UP')
  and not exists (select 1 from InventoryReservation r where r.order_id = oi.order_id and r.variant_id = oi.variant_id)
group by oi.order_id, oi.variant_id, o.order_status;

insert into InventoryTransaction(order_id, variant_id, transaction_type, quantity)
select r.order_id, r.variant_id, case when r.status = 'RESERVED' then 'RESERVE' else 'CONSUME' end, r.quantity
from InventoryReservation r
where not exists (select 1 from InventoryTransaction t where t.order_id = r.order_id and t.variant_id = r.variant_id);
go

if object_id('Review', 'U') is null
begin
    create table Review (
        review_id int identity primary key,
        user_id int not null references Users(user_id),
        order_id int not null references Orders(order_id),
        rating int not null,
        comment nvarchar(1000) null,
        created_at datetime2 not null constraint DF_Review_CreatedAt default getdate(),
        updated_at datetime2 not null constraint DF_Review_UpdatedAt default getdate(),
        constraint CK_Review_Rating check (rating between 1 and 5)
    );
end;
if col_length('Review', 'updated_at') is null
    alter table Review add updated_at datetime2 null constraint DF_Review_UpdatedAt default getdate();
update Review set updated_at = coalesce(updated_at, created_at, getdate()) where updated_at is null;
if not exists (select 1 from sys.check_constraints where name = 'CK_Review_Rating' and parent_object_id = object_id('Review'))
    alter table Review with check add constraint CK_Review_Rating check (rating between 1 and 5);
if not exists (select 1 from sys.indexes where name = 'UQ_Review_User_Order' and object_id = object_id('Review'))
    create unique index UQ_Review_User_Order on Review(user_id, order_id);
go

if object_id('PasswordResetToken', 'U') is null
begin
    create table PasswordResetToken (
        reset_token_id int identity primary key,
        user_id int not null references Users(user_id),
        token_hash varchar(64) not null unique,
        expires_at datetime2 not null,
        used_at datetime2 null,
        created_at datetime2 not null default getdate(),
        updated_at datetime2 not null default getdate()
    );
end;
if not exists (select 1 from sys.indexes where name = 'IX_PasswordResetToken_User' and object_id = object_id('PasswordResetToken'))
    create index IX_PasswordResetToken_User on PasswordResetToken(user_id);
go

if object_id('ProductModifierGroup', 'U') is null
begin
    create table ProductModifierGroup (
        modifier_group_id int identity primary key,
        product_id int not null references Product(product_id),
        name nvarchar(255) not null,
        min_selections int not null constraint DF_ProductModifierGroup_Min default 0,
        max_selections int not null constraint DF_ProductModifierGroup_Max default 1,
        is_active bit not null constraint DF_ProductModifierGroup_Active default 1,
        sort_order int not null constraint DF_ProductModifierGroup_Sort default 0,
        constraint CK_ProductModifierGroup_Selections check (min_selections >= 0 and max_selections >= min_selections)
    );
end;

if object_id('ProductModifierOption', 'U') is null
begin
    create table ProductModifierOption (
        modifier_option_id int identity primary key,
        modifier_group_id int not null references ProductModifierGroup(modifier_group_id),
        name nvarchar(255) not null,
        price decimal(10,2) not null constraint DF_ProductModifierOption_Price default 0,
        is_active bit not null constraint DF_ProductModifierOption_Active default 1,
        sort_order int not null constraint DF_ProductModifierOption_Sort default 0,
        constraint CK_ProductModifierOption_Price check (price >= 0)
    );
end;

if object_id('ProductCombo', 'U') is null
begin
    create table ProductCombo (
        combo_id int identity primary key,
        product_id int not null unique references Product(product_id),
        is_active bit not null constraint DF_ProductCombo_Active default 1
    );
end;

if object_id('ProductComboItem', 'U') is null
begin
    create table ProductComboItem (
        combo_item_id int identity primary key,
        combo_id int not null references ProductCombo(combo_id),
        product_id int not null references Product(product_id),
        variant_id int not null references ProductVariant(variant_id),
        quantity int not null constraint DF_ProductComboItem_Quantity default 1,
        sort_order int not null constraint DF_ProductComboItem_Sort default 0,
        constraint CK_ProductComboItem_Quantity check (quantity > 0)
    );
end;

if object_id('LoyaltyTransaction', 'U') is null
begin
    create table LoyaltyTransaction (
        loyalty_transaction_id int identity primary key,
        user_id int not null references Users(user_id),
        order_id int not null references Orders(order_id),
        transaction_type varchar(20) not null,
        points int not null,
        created_at datetime2 default getdate(),
        updated_at datetime2 default getdate()
    );
end;

if not exists (select 1 from sys.indexes where name = 'UQ_LoyaltyTransaction_Order_Type' and object_id = object_id('LoyaltyTransaction'))
    create unique index UQ_LoyaltyTransaction_Order_Type on LoyaltyTransaction(order_id, transaction_type);
if not exists (select 1 from sys.indexes where name = 'IX_LoyaltyTransaction_User_Created' and object_id = object_id('LoyaltyTransaction'))
    create index IX_LoyaltyTransaction_User_Created on LoyaltyTransaction(user_id, created_at);

if object_id('WorkShift', 'U') is null
begin
    create table WorkShift (
        shift_id int identity primary key,
        user_id int not null references Users(user_id),
        shift_date date not null,
        start_time time not null,
        end_time time not null,
        check_in_at datetime2 null,
        check_out_at datetime2 null,
        status varchar(20) constraint DF_WorkShift_Status default 'SCHEDULED',
        created_at datetime2 default getdate()
    );
end;

if not exists (select 1 from sys.indexes where name = 'IX_WorkShift_User_Date' and object_id = object_id('WorkShift'))
    create index IX_WorkShift_User_Date on WorkShift(user_id, shift_date);
if not exists (select 1 from sys.indexes where name = 'IX_WorkShift_Date' and object_id = object_id('WorkShift'))
    create index IX_WorkShift_Date on WorkShift(shift_date);

if object_id('OrderItemModifier', 'U') is null
begin
    create table OrderItemModifier (
        order_item_modifier_id int identity primary key,
        order_item_id int not null references OrderItem(order_item_id),
        modifier_option_id int null references ProductModifierOption(modifier_option_id),
        group_name nvarchar(255) not null,
        option_name nvarchar(255) not null,
        price decimal(10,2) not null,
        constraint CK_OrderItemModifier_Price check (price >= 0)
    );
end;

if not exists (select 1 from sys.indexes where name = 'IX_OrderItemModifier_OrderItem' and object_id = object_id('OrderItemModifier'))
    create index IX_OrderItemModifier_OrderItem on OrderItemModifier(order_item_id);

if object_id('SupportTicket', 'U') is null
begin
    create table SupportTicket (
        ticket_id int identity primary key,
        user_id int null references Users(user_id),
        order_id int null references Orders(order_id),
        subject nvarchar(255) not null,
        category varchar(30) not null,
        description nvarchar(2000) not null,
        status varchar(20) not null constraint DF_SupportTicket_Status default 'OPEN',
        staff_id int null references Users(user_id),
        resolution nvarchar(2000) null,
        created_at datetime2 default getdate(),
        updated_at datetime2 default getdate(),
        resolved_at datetime2 null,
        constraint CK_SupportTicket_Category check (category in ('MISSING_ITEM', 'COLD_FOOD', 'WRONG_ITEM', 'LATE_DELIVERY', 'OTHER')),
        constraint CK_SupportTicket_Status check (status in ('OPEN', 'PROCESSING', 'RESOLVED'))
    );
end;

if col_length('SupportTicket', 'updated_at') is null
    alter table SupportTicket add updated_at datetime2 null constraint DF_SupportTicket_UpdatedAt default getdate();

if not exists (select 1 from sys.indexes where name = 'IX_SupportTicket_User' and object_id = object_id('SupportTicket'))
    create index IX_SupportTicket_User on SupportTicket(user_id, created_at);
if not exists (select 1 from sys.indexes where name = 'IX_SupportTicket_Status' and object_id = object_id('SupportTicket'))
    create index IX_SupportTicket_Status on SupportTicket(status, created_at);

if object_id('Notification', 'U') is null
begin
    create table Notification (
        notification_id int identity primary key,
        user_id int null references Users(user_id),
        role_name varchar(50) null,
        title nvarchar(255) not null,
        message nvarchar(1000) null,
        type varchar(50) null,
        target_url varchar(500) null,
        is_read bit not null constraint DF_Notification_IsRead default 0,
        created_at datetime2 not null constraint DF_Notification_CreatedAt default getdate(),
        updated_at datetime2 not null default getdate()
    );
end;

if not exists (select 1 from sys.indexes where name = 'IX_Notification_User_Read' and object_id = object_id('Notification'))
    create index IX_Notification_User_Read on Notification(user_id, is_read, created_at);
if not exists (select 1 from sys.indexes where name = 'IX_Notification_Role_Read' and object_id = object_id('Notification'))
    create index IX_Notification_Role_Read on Notification(role_name, is_read, created_at);

if object_id('OrderStatusHistory', 'U') is null
begin
    create table OrderStatusHistory (
        history_id int identity primary key,
        order_id int not null references Orders(order_id),
        actor_user_id int null references Users(user_id),
        actor_role varchar(50) null,
        from_status varchar(30) null,
        to_status varchar(30) not null,
        note nvarchar(500) null,
        created_at datetime2 not null constraint DF_OrderStatusHistory_CreatedAt default getdate()
    );
end;

if not exists (select 1 from sys.indexes where name = 'IX_OrderStatusHistory_Order' and object_id = object_id('OrderStatusHistory'))
    create index IX_OrderStatusHistory_Order on OrderStatusHistory(order_id, created_at);

if object_id('FavoriteProduct', 'U') is null
begin
    create table FavoriteProduct (
        favorite_id int identity primary key,
        user_id int not null references Users(user_id),
        product_id int not null references Product(product_id),
        created_at datetime2 not null constraint DF_FavoriteProduct_CreatedAt default getdate()
    );
end;

if not exists (select 1 from sys.indexes where name = 'UQ_FavoriteProduct_User_Product' and object_id = object_id('FavoriteProduct'))
    create unique index UQ_FavoriteProduct_User_Product on FavoriteProduct(user_id, product_id);

if not exists (select 1 from sys.indexes where name = 'IX_OrderItem_Order' and object_id = object_id('OrderItem'))
    create index IX_OrderItem_Order on OrderItem(order_id);
if not exists (select 1 from sys.indexes where name = 'IX_OrderItem_Variant' and object_id = object_id('OrderItem'))
    create index IX_OrderItem_Variant on OrderItem(variant_id);
if not exists (select 1 from sys.check_constraints where name = 'CK_OrderItem_Quantity' and parent_object_id = object_id('OrderItem'))
    alter table OrderItem add constraint CK_OrderItem_Quantity check (quantity > 0);
if not exists (select 1 from sys.check_constraints where name = 'CK_OrderItem_Amount' and parent_object_id = object_id('OrderItem'))
    alter table OrderItem add constraint CK_OrderItem_Amount check (unit_price >= 0 and total_price >= 0);

declare @cartIndex sysname;
select @cartIndex = i.name
from sys.indexes i
where i.object_id = object_id('CartItem') and i.name = 'idx_cartitem_variant';
if @cartIndex is not null exec('drop index idx_cartitem_variant on CartItem');
if not exists (select 1 from sys.indexes where name = 'idx_cartitem_variant_modifiers' and object_id = object_id('CartItem'))
    create unique index idx_cartitem_variant_modifiers on CartItem(cart_id, variant_id, selected_modifier_option_ids);

if not exists (select 1 from ShippingConfig where config_key = 'business_open_time') insert into ShippingConfig(config_key, config_value) values ('business_open_time', '08:00');
if not exists (select 1 from ShippingConfig where config_key = 'business_close_time') insert into ShippingConfig(config_key, config_value) values ('business_close_time', '22:00');
if not exists (select 1 from ShippingConfig where config_key = 'service_fee') insert into ShippingConfig(config_key, config_value) values ('service_fee', '0');

if object_id('DeliveryZone', 'U') is not null update DeliveryZone set is_active = 1 where is_active is null;
update Banner set is_active = 1 where is_active is null;
update Coupon set is_active = 1, is_public = 1 where is_active is null or is_public is null;
update ProductVariant set is_default = 1 where variant_name = N'Mặc định' and (is_default = 0 or is_default is null);
if col_length('Users', 'role_id') is not null and object_id('Role', 'U') is not null
begin
    exec sp_executesql N'
        update Users set role_id = (select role_id from Role where role_name = ''ADMIN'') where email = ''admin@fastguy.com'';
        update Users set role_id = (select role_id from Role where role_name = ''STAFF'') where email in (''staff1@fastguy.com'', ''staff2@fastguy.com'');
        update Users set role_id = (select role_id from Role where role_name = ''USER'') where email in (''user1@gmail.com'', ''user2@gmail.com'');';
end;
update Orders set payment_method = 'COD' where payment_method = 'CASH';
update Orders set payment_method = 'BANK_TRANSFER' where payment_method = 'BANKING';
go

-- P0: CartItemModifier normalization
if not exists (select 1 from information_schema.tables where table_name = 'CartItemModifier')
begin
    create table CartItemModifier (
        cart_item_modifier_id int identity(1,1) primary key,
        cart_item_id int not null foreign key references CartItem(cart_item_id) on delete cascade,
        modifier_option_id int not null foreign key references ProductModifierOption(modifier_option_id)
    );
    create index IX_CartItemModifier_Item on CartItemModifier(cart_item_id);

    -- backfill existing CSV data
    declare @cartItemId int, @optionIds nvarchar(500), @pos int, @val nvarchar(20);
    declare cur cursor local fast_forward for
        select cart_item_id, selected_modifier_option_ids
        from CartItem where selected_modifier_option_ids is not null and selected_modifier_option_ids != '';
    open cur;
    fetch next from cur into @cartItemId, @optionIds;
    while @@fetch_status = 0
    begin
        set @optionIds = @optionIds + ',';
        while len(@optionIds) > 0
        begin
            set @pos = charindex(',', @optionIds);
            set @val = left(@optionIds, @pos - 1);
            set @optionIds = stuff(@optionIds, 1, @pos, '');
            if isnumeric(@val) = 1
                insert into CartItemModifier (cart_item_id, modifier_option_id)
                values (@cartItemId, cast(@val as int));
        end
        fetch next from cur into @cartItemId, @optionIds;
    end
    close cur;
    deallocate cur;
end
go

if object_id('CouponRedemption', 'U') is null
begin
    create table CouponRedemption (
        redemption_id int identity primary key,
        coupon_id int not null references Coupon(coupon_id),
        user_id int not null references Users(user_id),
        order_id int null references Orders(order_id),
        claimed_at datetime2 null,
        used_at datetime2 null,
        discount_amount decimal(18,2) null,
        created_at datetime2 null default getdate(),
        updated_at datetime2 null default getdate()
    );
end;
if object_id('ClaimedCoupon', 'U') is not null
begin
    insert into CouponRedemption(coupon_id, user_id, claimed_at, created_at, updated_at)
    select cc.coupon_id, cc.user_id, min(coalesce(cc.claimed_at, getdate())), min(coalesce(cc.claimed_at, getdate())), getdate()
    from ClaimedCoupon cc
    where exists (select 1 from Coupon c where c.coupon_id = cc.coupon_id)
      and exists (select 1 from Users u where u.user_id = cc.user_id)
      and not exists (select 1 from CouponRedemption cr where cr.coupon_id = cc.coupon_id and cr.user_id = cc.user_id)
    group by cc.coupon_id, cc.user_id;
end;
if object_id('CouponUsage', 'U') is not null
begin
    update cr set order_id = x.order_id, used_at = x.used_at, discount_amount = x.discount_amount, updated_at = getdate()
    from CouponRedemption cr
    join (select *, row_number() over(partition by coupon_id, user_id order by used_at desc, coupon_usage_id desc) rn from CouponUsage where user_id is not null) x
      on x.coupon_id = cr.coupon_id and x.user_id = cr.user_id and x.rn = 1
    where cr.used_at is null and exists (select 1 from Orders o where o.order_id = x.order_id);
    insert into CouponRedemption(coupon_id, user_id, order_id, claimed_at, used_at, discount_amount, created_at, updated_at)
    select x.coupon_id, x.user_id, x.order_id, x.used_at, x.used_at, x.discount_amount, x.used_at, getdate()
    from (select *, row_number() over(partition by coupon_id, user_id order by used_at desc, coupon_usage_id desc) rn from CouponUsage where user_id is not null) x
    where x.rn = 1
      and exists (select 1 from Coupon c where c.coupon_id = x.coupon_id)
      and exists (select 1 from Users u where u.user_id = x.user_id)
      and exists (select 1 from Orders o where o.order_id = x.order_id)
      and not exists (select 1 from CouponRedemption cr where cr.coupon_id = x.coupon_id and cr.user_id = x.user_id);
end;
;with ranked as (select redemption_id, row_number() over(partition by user_id, coupon_id order by case when used_at is null then 0 else 1 end, used_at desc, redemption_id) rn from CouponRedemption)
delete from ranked where rn > 1;
;with ranked as (select redemption_id, row_number() over(partition by order_id order by used_at desc, redemption_id) rn from CouponRedemption where order_id is not null)
update cr set order_id = null, used_at = null, discount_amount = null, updated_at = getdate() from CouponRedemption cr join ranked r on r.redemption_id = cr.redemption_id where r.rn > 1;
if not exists (select 1 from sys.indexes where name = 'UQ_CouponRedemption_User_Coupon' and object_id = object_id('CouponRedemption')) create unique index UQ_CouponRedemption_User_Coupon on CouponRedemption(user_id, coupon_id);
if not exists (select 1 from sys.indexes where name = 'UQ_CouponRedemption_Order' and object_id = object_id('CouponRedemption')) create unique index UQ_CouponRedemption_Order on CouponRedemption(order_id) where order_id is not null;
-- Legacy tables retain redemption history that the one-row-per-user model cannot represent.
-- Drop them only after an audited archival migration is available.
go
