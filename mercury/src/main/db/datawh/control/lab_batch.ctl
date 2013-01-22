UNRECOVERABLE LOAD DATA
INFILE 'from command line'
REPLACE INTO TABLE im_lab_batch
FIELDS TERMINATED BY "," OPTIONALLY ENCLOSED BY '"'
TRAILING NULLCOLS
(
 line_number,
 etl_date DATE "YYYYMMDDHH24MISS",
 is_delete,
 lab_batch_id,
 batch_name,
 is_active,
 created_on DATE "YYYYMMDDHH24MISS",
 due_date DATE "YYYYMMDDHH24MISS"
)
