UNRECOVERABLE LOAD DATA
INFILE 'from command line'
REPLACE
INTO TABLE im_pdo_regulatory_infos
FIELDS TERMINATED BY "," OPTIONALLY ENCLOSED BY '"'
TRAILING NULLCOLS
(
 line_number,
 etl_date DATE "YYYYMMDDHH24MISS",
 is_delete,
 product_order_id,
 regulatory_info_id,
 identifier,
 type,
 name
)

