UNRECOVERABLE LOAD DATA
INFILE 'from command line'
REPLACE INTO TABLE im_product_order_sample_bill
FIELDS TERMINATED BY "," OPTIONALLY ENCLOSED BY '"'
TRAILING NULLCOLS
(
 line_number,
 etl_date DATE "YYYYMMDDHH24MISS",
 is_delete,
 product_order_sample_id,
 is_billed
)
