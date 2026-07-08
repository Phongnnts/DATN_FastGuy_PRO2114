if object_id('FavoriteProduct', 'U') is null
begin
    create table FavoriteProduct
    (
        favorite_id int identity primary key,
        user_id int not null references Users(user_id),
        product_id int not null references Product(product_id),
        created_at datetime2 default getdate(),
        constraint UQ_FavoriteProduct_User_Product unique (user_id, product_id)
    )
end
go

if not exists (
    select 1 from sys.indexes where name = 'UQ_Review_User_Order_Product' and object_id = object_id('Review')
)
begin
    create unique index UQ_Review_User_Order_Product
    on Review(user_id, order_id, product_id)
end
go
