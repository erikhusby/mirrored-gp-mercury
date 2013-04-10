
-------------------------------------------------------
-- For release 1.20
-------------------------------------------------------
alter table product_order_sample add (is_abandoned CHAR(1) generated always as
 (case when delivery_status = 'ABANDONED' then 'T' else 'F' end) virtual);
