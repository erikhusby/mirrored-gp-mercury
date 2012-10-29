UNRECOVERABLE LOAD DATA
INFILE 'from command line'
REPLACE INTO TABLE im_product_order_sample
FIELDS TERMINATED BY "," OPTIONALLY ENCLOSED BY '"'
TRAILING NULLCOLS
(
 product_order_sample_id,
 sample_name,
 billing_status,
 etl_date DATE "YYYYMMDDHH24MISS",
 is_delete
)