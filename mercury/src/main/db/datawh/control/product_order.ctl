UNRECOVERABLE LOAD DATA
INFILE 'from command line'
REPLACE INTO TABLE im_product_order
FIELDS TERMINATED BY "," OPTIONALLY ENCLOSED BY '"'
TRAILING NULLCOLS
(
 line_number,
 etl_date DATE "YYYYMMDDHH24MISS",
 is_delete,
 product_order_id,
 research_project_id,
 product_id,
 status,
 created_date DATE "YYYYMMDDHH24MISS",
 modified_date DATE "YYYYMMDDHH24MISS",
 title,
 quote_id,
 jira_ticket_key,
 owner,
 placed_date DATE "YYYYMMDDHH24MISS",
 skip_regulatory_reason,
 sap_order_number,
 array_chip_type,
 call_rate_threshold,
 reg_info_ids,
 order_type
)
