UNRECOVERABLE LOAD DATA
INFILE 'from command line'
REPLACE INTO TABLE im_research_project
FIELDS TERMINATED BY "," OPTIONALLY ENCLOSED BY '"'
TRAILING NULLCOLS
(
 line_number,
 etl_date DATE "YYYYMMDDHH24MISS",
 is_delete,
 research_project_id,
 current_status,
 created_date DATE "YYYYMMDDHH24MISS",
 title,
 irb_not_engaged,
 jira_ticket_key,
 parent_research_project_id,
 root_research_project_id
)
