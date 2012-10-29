UNRECOVERABLE LOAD DATA
INFILE 'from command line'
REPLACE INTO TABLE im_product_order
FIELDS TERMINATED BY "," OPTIONALLY ENCLOSED BY '"'
TRAILING NULLCOLS
(
 product_order_id,
 product_id,
 status ,
 created_date DATE "YYYYMMDDHH24MISS",
 modified_date DATE "YYYYMMDDHH24MISS",
 title ,
 quote_id ,
 jira_ticket_key ,
 etl_date DATE "YYYYMMDDHH24MISS",
 is_delete
)