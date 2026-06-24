-- Optional diagnostic only. Does not change data.
-- Shows rows where quantity-like values are not whole numbers, if these columns exist.
select 'equipment_items' as table_name, id, name, quantity_total, quantity_available, quantity_loaned
from equipment_items
where (quantity_total is not null and quantity_total <> round(quantity_total))
   or (quantity_available is not null and quantity_available <> round(quantity_available))
   or (quantity_loaned is not null and quantity_loaned <> round(quantity_loaned))
limit 200;
