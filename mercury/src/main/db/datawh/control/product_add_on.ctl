UNRECOVERABLE LOAD DATA
INFILE 'from command line'
REPLACE INTO TABLE im_product_add_on
FIELDS TERMINATED BY "," OPTIONALLY ENCLOSED BY '"'
TRAILING NULLCOLS
(
 etl_date DATE "YYYYMMDDHH24MISS",
 is_delete,
 product_id,
 add_on_product_id
)
