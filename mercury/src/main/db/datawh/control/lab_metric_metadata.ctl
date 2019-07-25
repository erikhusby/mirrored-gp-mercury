UNRECOVERABLE LOAD DATA
REPLACE INTO TABLE im_lab_metric_metadata
FIELDS TERMINATED BY "," OPTIONALLY ENCLOSED BY '"'
TRAILING NULLCOLS
(
  line_number,
  etl_date DATE "YYYYMMDDHH24MISS",
  is_delete,
  lab_metric_id,
  metadata_id,
  metadata_key,
  value,
  date_value,
  number_value
)

