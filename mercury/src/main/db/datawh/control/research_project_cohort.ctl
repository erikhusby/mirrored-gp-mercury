UNRECOVERABLE LOAD DATA
INFILE 'from command line'
REPLACE INTO TABLE im_research_project_cohort
FIELDS TERMINATED BY "," OPTIONALLY ENCLOSED BY '"'
TRAILING NULLCOLS
(
 etl_date DATE "YYYYMMDDHH24MISS",
 is_delete,
 research_project_cohort_id,
 research_project_id
)