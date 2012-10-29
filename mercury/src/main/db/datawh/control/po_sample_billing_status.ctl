UNRECOVERABLE LOAD DATA
INFILE 'from command line'
REPLACE INTO TABLE im_po_sample_billing_status
FIELDS TERMINATED BY "," OPTIONALLY ENCLOSED BY '"'
TRAILING NULLCOLS
(
 product_order_sample_id,
 status_date DATE "YYYY-MM-DDHH24MISS",
 billing_status,
 etl_date DATE "YYYYMMDDHH24MISS",
 is_delete
)