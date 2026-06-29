use FastGuyDB;
go

-- 1. ProductVariant: thêm weight/dimensions
if col_length('ProductVariant', 'weight') is null
    alter table ProductVariant add weight decimal(10,2) default 500;
go
if col_length('ProductVariant', 'length') is null
    alter table ProductVariant add length decimal(10,2) default 20;
go
if col_length('ProductVariant', 'width') is null
    alter table ProductVariant add width decimal(10,2) default 20;
go
if col_length('ProductVariant', 'height') is null
    alter table ProductVariant add height decimal(10,2) default 10;
go
-- 2. Orders: thêm GHN tracking fields
if col_length('Orders', 'ghn_order_code') is null
    alter table Orders add ghn_order_code varchar(50);
go
if col_length('Orders', 'ghn_tracking_url') is null
    alter table Orders add ghn_tracking_url varchar(500);
go
if col_length('Orders', 'ghn_status') is null
    alter table Orders add ghn_status varchar(30);
go
if col_length('Orders', 'from_district_id') is null
    alter table Orders add from_district_id int;
go
if col_length('Orders', 'from_ward_code') is null
    alter table Orders add from_ward_code varchar(50);
go

-- 3. ShippingConfig: bang moi
if object_id('ShippingConfig', 'U') is null
begin
    create table ShippingConfig
    (
        config_id    int identity primary key,
        config_key   varchar(100) not null unique,
        config_value varchar(500) not null
    );

    insert into ShippingConfig (config_key, config_value) values
    ('ghn_from_district_id', '1442'),
    ('ghn_from_ward_code', '20107'),
    ('default_weight', '500'),
    ('default_length', '20'),
    ('default_width', '20'),
    ('default_height', '10'),
    ('default_service_type_id', '2');
end
go

select 'GHN migration complete!' as status;
go
