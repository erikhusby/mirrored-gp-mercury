
-------------------------------------------------------
-- For release 1.23
-------------------------------------------------------
alter table product_order add (placed_date DATE);
alter table im_product_order add (placed_date DATE);
alter table product_order_sample add (ledger_quote_id VARCHAR2(255));
CREATE TABLE im_ledger_entry (
  line_number NUMERIC(9) NOT NULL,
  etl_date DATE NOT NULL,
  is_delete CHAR(1) NOT NULL,
  ledger_id NUMERIC(19) NOT NULL,
  product_order_sample_id NUMERIC(19),
  quote_id VARCHAR2(255)
);
