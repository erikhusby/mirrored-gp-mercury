UNRECOVERABLE LOAD DATA
INFILE 'from command line'
REPLACE INTO TABLE im_price_item
FIELDS TERMINATED BY "," OPTIONALLY ENCLOSED BY '"'
TRAILING NULLCOLS
(
 etl_date DATE "YYYYMMDDHH24MISS",
 is_delete,
 price_item_id,
 platform,
 category,
 price_item_name,
 quote_server_id,
 price,
 units
)
