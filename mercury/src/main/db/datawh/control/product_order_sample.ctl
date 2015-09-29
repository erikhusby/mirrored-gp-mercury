UNRECOVERABLE LOAD DATA
INFILE 'from command line'
REPLACE INTO TABLE im_product_order_sample
FIELDS TERMINATED BY "," OPTIONALLY ENCLOSED BY '"'
TRAILING NULLCOLS
(
 line_number,
 etl_date DATE "YYYYMMDDHH24MISS",
 is_delete,
 product_order_sample_id,
 product_order_id,
 sample_name,
 delivery_status,
 sample_position,
 participant_id,
 sample_type,
 sample_receipt DATE "YYYYMMDDHH24MISS",
 original_sample_type
)