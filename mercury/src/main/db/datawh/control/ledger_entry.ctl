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
 quote_id,
 price_item_id,
 price_item_type,
 quantity,
 billing_session_id,
 billing_message char(500),
 work_complete_date DATE "YYYYMMDDHH24MISS",
 quote_server_work_item,
 sap_delivery_document
)
