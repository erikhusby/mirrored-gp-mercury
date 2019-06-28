UNRECOVERABLE LOAD DATA
INFILE 'from command line'
REPLACE INTO TABLE im_lab_vessel
FIELDS TERMINATED BY "," OPTIONALLY ENCLOSED BY '"'
TRAILING NULLCOLS
(
 line_number,
 etl_date DATE "YYYYMMDDHH24MISS",
 is_delete,
 lab_vessel_id,
 label,
 lab_vessel_type,
 name,
 created_on DATE "YYYYMMDDHH24MISS",
 volume
)
