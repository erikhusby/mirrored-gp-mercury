
-------------------------------------------------------
-- For release 1.18
-------------------------------------------------------
-- GPLIM-993
DROP INDEX product_order_sample_idx1;
CREATE UNIQUE INDEX product_order_sample_idx1 ON product_order_sample(product_order_id, sample_name, sample_position);
