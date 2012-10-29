UNRECOVERABLE LOAD DATA
INFILE 'from command line'
REPLACE INTO TABLE im_research_project_status
FIELDS TERMINATED BY "," OPTIONALLY ENCLOSED BY '"'
TRAILING NULLCOLS
(
 research_project_id,
 status_date DATE "YYYYMMDDHH24MISS",
 status,
 etl_date DATE "YYYYMMDDHH24MISS",
 is_delete
)