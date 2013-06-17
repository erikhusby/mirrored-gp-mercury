UNRECOVERABLE LOAD DATA
INFILE 'from command line'
REPLACE INTO TABLE im_sequencing_run
FIELDS TERMINATED BY "," OPTIONALLY ENCLOSED BY '"'
TRAILING NULLCOLS
(
 line_number,
 etl_date DATE "YYYYMMDDHH24MISS",
 is_delete,
 sequencing_run_id,
 run_name,
 barcode,
 registration_date DATE "YYYYMMDDHH24MISS",
 instrument.
 setup_read_structure,
 actual_read_structure
)
