UNRECOVERABLE LOAD DATA
INFILE 'from command line'
REPLACE INTO TABLE im_sequencing_sample_fact
FIELDS TERMINATED BY "," OPTIONALLY ENCLOSED BY '"'
TRAILING NULLCOLS
(
 line_number,
 etl_date DATE "YYYYMMDDHH24MISS",
 is_delete,
 sequencing_run_id,
 flowcell_barcode,
 lane,
 molecular_indexing_scheme,
 product_order_id,
 sample_name,
 research_project_id,
 loaded_library_barcode,
 loaded_library_create_date DATE "YYYYMMDDHH24MISS",
 batch_name
)
