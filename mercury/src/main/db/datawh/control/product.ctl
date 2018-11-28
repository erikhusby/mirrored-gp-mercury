UNRECOVERABLE LOAD DATA
INFILE 'from command line'
REPLACE INTO TABLE im_product
FIELDS TERMINATED BY "," OPTIONALLY ENCLOSED BY '"'
TRAILING NULLCOLS
(
 line_number,
 etl_date DATE "YYYYMMDDHH24MISS",
 is_delete,
 product_id,
 product_name,
 part_number,
 availability_date DATE "YYYYMMDDHH24MISS",
 discontinued_date DATE "YYYYMMDDHH24MISS",
 expected_cycle_time_sec,
 guaranteed_cycle_time_sec,
 samples_per_week,
 is_top_level_product,
 workflow_name,
 product_family_name,
 primary_price_item_id,
 aggregation_data_type,
 external_only_product,
 saved_in_sap
)
