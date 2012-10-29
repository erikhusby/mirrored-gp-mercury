UNRECOVERABLE LOAD DATA
INFILE 'from command line'
REPLACE INTO TABLE im_research_project_irb
FIELDS TERMINATED BY "," OPTIONALLY ENCLOSED BY '"'
TRAILING NULLCOLS
(
 research_project_id,
 research_project_irb_id,
 research_project_irb,
 research_project_irb_type,
 etl_date DATE "YYYYMMDDHH24MISS",
 is_delete
)