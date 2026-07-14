use FastGuyDB;
go
set ansi_nulls on;
set quoted_identifier on;
set ansi_padding on;
set ansi_warnings on;
set arithabort on;
set concat_null_yields_null on;
go

if col_length('Product', 'available_from') is null alter table Product add available_from time null;
if col_length('Product', 'available_to') is null alter table Product add available_to time null;
if col_length('DeliveryZone', 'ward_name') is null alter table DeliveryZone add ward_name nvarchar(100) null;
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
        created_at datetime2 default getdate()
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
        resolved_at datetime2 null,
        constraint CK_SupportTicket_Category check (category in ('MISSING_ITEM', 'COLD_FOOD', 'WRONG_ITEM', 'LATE_DELIVERY', 'OTHER')),
        constraint CK_SupportTicket_Status check (status in ('OPEN', 'PROCESSING', 'RESOLVED'))
    );
end;

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
        created_at datetime2 not null constraint DF_Notification_CreatedAt default getdate()
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

update DeliveryZone set is_active = 1 where is_active is null;
update Banner set is_active = 1 where is_active is null;
update Coupon set is_active = 1, is_public = 1 where is_active is null or is_public is null;
update ProductVariant set is_default = 1 where variant_name = N'Mặc định' and (is_default = 0 or is_default is null);
update Users set role_id = (select role_id from Role where role_name = 'ADMIN') where email = 'admin@fastguy.com';
update Users set role_id = (select role_id from Role where role_name = 'STAFF') where email in ('staff1@fastguy.com', 'staff2@fastguy.com');
update Users set role_id = (select role_id from Role where role_name = 'USER') where email in ('user1@gmail.com', 'user2@gmail.com');
update Orders set payment_method = 'COD' where payment_method = 'CASH';
update Orders set payment_method = 'BANK_TRANSFER' where payment_method = 'BANKING';
update Orders
set order_status = 'CANCELLED', cancelled_at = getdate(), cancelled_by = 'SYSTEM', failure_reason = N'Đơn chuyển khoản cũ không có link thanh toán'
where payment_method = 'BANK_TRANSFER' and payment_status = 'UNPAID' and order_status = 'PENDING'
  and (payos_checkout_url is null or payos_checkout_url = '');
go
