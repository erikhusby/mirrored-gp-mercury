UNRECOVERABLE LOAD DATA
INFILE 'from command line'
REPLACE INTO TABLE im_po_billable_item
FIELDS TERMINATED BY "," OPTIONALLY ENCLOSED BY '"'
TRAILING NULLCOLS
(
 product_order_sample_id,
 price_item_id,
 item_count,
 etl_date DATE "YYYYMMDDHH24MISS",
 is_delete
)
