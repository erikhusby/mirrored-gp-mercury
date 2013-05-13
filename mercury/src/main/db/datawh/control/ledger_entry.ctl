UNRECOVERABLE LOAD DATA
INFILE 'from command line'
REPLACE INTO TABLE im_ledger_entry
FIELDS TERMINATED BY "," OPTIONALLY ENCLOSED BY '"'
TRAILING NULLCOLS
(
 line_number,
 etl_date DATE "YYYYMMDDHH24MISS",
 is_delete,
 ledger_id,
 product_order_sample_id,
 quote_id
)
