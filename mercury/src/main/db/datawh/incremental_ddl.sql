
-------------------------------------------------------
-- For release 1.19
-------------------------------------------------------
ALTER TABLE product_order_sample MODIFY (on_risk DEFAULT 'F' NOT NULL);
ALTER TABLE product_order_sample MODIFY (is_billed DEFAULT 'F' NOT NULL);
