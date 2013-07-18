UNRECOVERABLE LOAD DATA
INFILE 'from command line'
REPLACE INTO TABLE im_workflow
FIELDS TERMINATED BY "," OPTIONALLY ENCLOSED BY '"'
TRAILING NULLCOLS
(
 line_number,
 etl_date DATE "YYYYMMDDHH24MISS",
 is_delete,
 workflow_id,
 workflow_name,
 workflow_version,
 effective_date DATE "YYYYMMDDHH24MISS"
)
