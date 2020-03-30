UNRECOVERABLE LOAD DATA
INFILE 'from command line'
REPLACE INTO TABLE im_event_metadata
FIELDS TERMINATED BY "," OPTIONALLY ENCLOSED BY '"'
TRAILING NULLCOLS
(
  line_number,
  etl_date DATE "YYYYMMDDHH24MISS",
  is_delete,
  metadata_id,
  lab_event_id,
  metadata_type,
  value
)
