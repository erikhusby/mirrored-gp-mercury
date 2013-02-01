UNRECOVERABLE LOAD DATA
INFILE 'from command line'
REPLACE INTO TABLE im_event_fact
FIELDS TERMINATED BY "," OPTIONALLY ENCLOSED BY '"'
TRAILING NULLCOLS
(
 line_number,
 etl_date DATE "YYYYMMDDHH24MISS",
 is_delete,
 lab_event_id,
 event_name,
 workflow_config_id,
 product_order_id,
 sample_key,
 lab_batch_id,
 station_name,
 lab_vessel_id,
 event_date DATE "YYYYMMDDHH24MISS"
)
