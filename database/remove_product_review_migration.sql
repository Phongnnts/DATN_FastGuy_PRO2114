if exists (select 1 from sys.indexes where name = 'UQ_Review_User_Order_Product' and object_id = object_id('Review'))
    drop index UQ_Review_User_Order_Product on Review
go

if exists (select 1 from sys.indexes where name = 'UQ_Review_User_Order' and object_id = object_id('Review'))
    drop index UQ_Review_User_Order on Review
go

if exists (select 1 from sys.foreign_keys where name = 'FK__Review__product_id')
    alter table Review drop constraint FK__Review__product_id
go

if exists (select 1 from sys.foreign_keys where parent_object_id = object_id('Review') and referenced_object_id = object_id('Product'))
begin
    declare @fk nvarchar(200)
    select @fk = name from sys.foreign_keys where parent_object_id = object_id('Review') and referenced_object_id = object_id('Product')
    exec('alter table Review drop constraint ' + @fk)
end
go

if exists (select 1 from sys.columns where object_id = object_id('Review') and name = 'product_id')
    alter table Review drop column product_id
go

alter table Review add constraint UQ_Review_User_Order unique (user_id, order_id)
go
