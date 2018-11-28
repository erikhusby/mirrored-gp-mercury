UNRECOVERABLE LOAD DATA
INFILE 'from command line'
REPLACE INTO TABLE im_fct_load
FIELDS TERMINATED BY "," OPTIONALLY ENCLOSED BY '"'
TRAILING NULLCOLS
(
   line_number,
   etl_date       DATE "YYYYMMDDHH24MISS",
   is_delete,
   batch_starting_vessel_id,
   flowcell_barcode
)