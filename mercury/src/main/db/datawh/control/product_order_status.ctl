UNRECOVERABLE LOAD DATA
INFILE 'from command line'
REPLACE INTO TABLE im_product_order_status
FIELDS TERMINATED BY "," OPTIONALLY ENCLOSED BY '"'
TRAILING NULLCOLS
(
 product_order_id,
 status_date DATE "YYYYMMDDHH24MISS",
 status,
 etl_date DATE "YYYYMMDDHH24MISS",
 is_delete
)