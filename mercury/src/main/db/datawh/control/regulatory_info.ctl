UNRECOVERABLE LOAD DATA
INFILE 'from command line'
REPLACE
INTO TABLE im_regulatory_info
FIELDS TERMINATED BY "," OPTIONALLY ENCLOSED BY '"'
TRAILING NULLCOLS
(
 line_number,
 etl_date DATE "YYYYMMDDHH24MISS",
 is_delete,
 regulatory_info_id,
 identifier,
 type,
 name
)
