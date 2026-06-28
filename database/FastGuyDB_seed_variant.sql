use FastGuyDB;
go

-- T?o variant m?c d?nh cho t?t c? Product
insert into ProductVariant (product_id, variant_name, price, is_default, status, created_at)
select product_id, N'M?c d?nh', base_price, 1, status, getdate()
from Product
where not exists (
    select 1 from ProductVariant pv where pv.product_id = Product.product_id and pv.is_default = 1
);
go

-- Migrate ProductOption sang ProductVariant
if object_id('ProductOption', 'U') is not null
begin
    insert into ProductVariant (product_id, variant_name, price, is_default, quantity_available, status, created_at)
    select 
        po.product_id,
        po.option_name,
        isnull(p.base_price, 0) + isnull(po.extra_price, 0),
        0,
        po.quantity_available,
        'AVAILABLE',
        getdate()
    from ProductOption po
    join Product p on po.product_id = p.product_id
    where not exists (
        select 1 from ProductVariant pv 
        where pv.product_id = po.product_id and pv.variant_name = po.option_name
    );
end
go

-- Gán variant m?c d?nh cho CartItem c?
update ci
set ci.variant_id = pv.variant_id
from CartItem ci
join ProductVariant pv on ci.product_id = pv.product_id
where pv.is_default = 1 and ci.variant_id is null;
go

-- Gán variant m?c d?nh cho OrderItem c?
update oi
set
    oi.variant_id = pv.variant_id,
    oi.variant_name = pv.variant_name
from OrderItem oi
join ProductVariant pv on oi.product_id = pv.product_id
where pv.is_default = 1 and oi.variant_id is null;
go

-- Verify
select 'ProductVariant' as tbl, count(*) as cnt from ProductVariant
union all
select 'CartItem has variant', count(*) from CartItem where variant_id is not null
union all
select 'OrderItem has variant', count(*) from OrderItem where variant_id is not null;

select 'Seed complete!' as status;
go
