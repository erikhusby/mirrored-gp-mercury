UNRECOVERABLE LOAD DATA
INFILE 'from command line'
REPLACE INTO TABLE im_workflow_config
FIELDS TERMINATED BY "," OPTIONALLY ENCLOSED BY '"'
TRAILING NULLCOLS
(
 line_number,
 etl_date DATE "YYYYMMDDHH24MISS",
 is_delete,
 workflow_config_id,
 effective_date DATE "YYYYMMDDHH24MISS",
 workflow_name,
 workflow_version,
 process_name,
 process_version,
 step_name,
 event_name
)
