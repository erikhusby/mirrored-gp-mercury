UNRECOVERABLE LOAD DATA
INFILE 'from command line'
REPLACE INTO TABLE im_array_process
FIELDS TERMINATED BY "," OPTIONALLY ENCLOSED BY '"'
TRAILING NULLCOLS
(
 line_number,
 etl_date DATE "YYYYMMDDHH24MISS",
 is_delete,
 product_order_id,
 batch_name,
 lcset_sample_name,
 sample_name,
 lab_event_id,
 lab_event_type,
 station_name,
 event_date DATE "YYYYMMDDHH24MISS",
 lab_vessel_id,
 position
)
