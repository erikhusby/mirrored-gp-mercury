
-------------------------------------------------------
-- For release 1.18
-------------------------------------------------------
-- GPLIM-993
DROP INDEX product_order_sample_idx1;
CREATE UNIQUE INDEX product_order_sample_idx1 ON product_order_sample(product_order_id, sample_name, sample_position);
-- GPLIM-1007
ALTER TABLE product_order DROP COLUMN is_deleted;
ALTER TABLE product_order_sample DROP COLUMN is_deleted;
ALTER TABLE product_order_add_on DROP COLUMN is_deleted;
-- GPLIM-896
ALTER TABLE product_order_sample ADD (on_risk CHAR(1) CHECK (on_risk IN ('T','F')));

DROP TABLE im_product_order_sample_fact;
CREATE TABLE im_product_order_sample_risk (
  line_number NUMERIC(9) NOT NULL,
  etl_date DATE NOT NULL,
  is_delete CHAR(1) NOT NULL,
  product_order_sample_id NUMERIC(19) NOT NULL,
  on_risk CHAR(1)
);
-- GPLIM-874
ALTER TABLE im_product_order ADD (owner VARCHAR(40));
ALTER TABLE product_order ADD (owner VARCHAR(40));
-- GPLIM-708
ALTER TABLE product_order_sample ADD (is_billed CHAR(1) CHECK (is_billed IN ('T','F')));

CREATE TABLE im_product_order_sample_bill (
  line_number NUMERIC(9) NOT NULL,
  etl_date DATE NOT NULL,
  is_delete CHAR(1) NOT NULL,
  product_order_sample_id NUMERIC(19) NOT NULL,
  is_billed CHAR(1)
);
