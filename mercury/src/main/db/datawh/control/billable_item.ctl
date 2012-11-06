UNRECOVERABLE LOAD DATA
INFILE 'from command line'
REPLACE INTO TABLE im_billable_item
FIELDS TERMINATED BY "," OPTIONALLY ENCLOSED BY '"'
TRAILING NULLCOLS
(
 line_number,
 etl_date DATE "YYYYMMDDHH24MISS",
 is_delete,
 billable_item_id,
 product_order_sample_id,
 price_item_id,
 item_count
)
