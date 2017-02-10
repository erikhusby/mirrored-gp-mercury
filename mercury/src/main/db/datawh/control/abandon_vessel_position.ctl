UNRECOVERABLE LOAD DATA
INFILE 'from command line'
REPLACE INTO TABLE im_abandon_vessel_position
FIELDS TERMINATED BY "," OPTIONALLY ENCLOSED BY '"'
TRAILING NULLCOLS
(
 line_number,
 etl_date DATE "YYYYMMDDHH24MISS",
 is_delete,
 abandon_id,
 abandon_type,
 abandon_vessel_id,
 vessel_position,
 reason,
 abandoned_on DATE "YYYYMMDDHH24MISS"
)
