UNRECOVERABLE LOAD DATA
INFILE 'from command line'
REPLACE INTO TABLE im_workflow_process
FIELDS TERMINATED BY "," OPTIONALLY ENCLOSED BY '"'
TRAILING NULLCOLS
(
 line_number,
 etl_date DATE "YYYYMMDDHH24MISS",
 is_delete,
 process_id,
 process_name,
 process_version,
 step_name,
 event_name
)
