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
 workflow_id,
 process_id,
 product_order_id,
 sample_name,
 lab_batch_id,
 station_name,
 lab_vessel_id,
 event_date DATE "YYYYMMDDHH24MISS"
)
