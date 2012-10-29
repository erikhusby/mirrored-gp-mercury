UNRECOVERABLE LOAD DATA
INFILE 'from command line'
REPLACE INTO TABLE im_research_project_cohort
FIELDS TERMINATED BY "," OPTIONALLY ENCLOSED BY '"'
TRAILING NULLCOLS
(
 research_project_id,
 research_project_cohort_id,
 etl_date DATE "YYYYMMDDHH24MISS",
 is_delete
)