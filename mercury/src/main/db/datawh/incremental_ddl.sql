-------------------------------------------------------
-- For release 1.25
-------------------------------------------------------

alter table im_product_order_sample_risk add risk_types VARCHAR2(255);
alter table im_product_order_sample_risk add risk_messages VARCHAR2(500);

alter table product_order_sample add risk_types VARCHAR2(255);
alter table product_order_sample add risk_messages VARCHAR2(500);