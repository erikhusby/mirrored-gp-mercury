UNRECOVERABLE LOAD DATA
INFILE 'from command line'
REPLACE INTO TABLE im_fct_create
FIELDS TERMINATED BY "," OPTIONALLY ENCLOSED BY '"'
TRAILING NULLCOLS
(
   line_number,
   etl_date       DATE "YYYYMMDDHH24MISS",
   is_delete,
   batch_starting_vessel_id,
   designation_id,
   fct_id,
   fct_name,
   fct_type,
   designation_library,
   loading_vessel,
   creation_date  DATE "YYYYMMDDHH24MISS",
   flowcell_type,
   lane,
   concentration,
   is_pool_test
)
