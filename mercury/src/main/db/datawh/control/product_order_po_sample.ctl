UNRECOVERABLE LOAD DATA
INFILE 'from command line'
REPLACE INTO TABLE im_product_order_po_sample
FIELDS TERMINATED BY "," OPTIONALLY ENCLOSED BY '"'
TRAILING NULLCOLS
(
 product_order_id,
 product_order_sample_id,
 etl_date DATE "YYYYMMDDHH24MISS",
 is_delete
)