UNRECOVERABLE LOAD DATA
INFILE 'from command line'
REPLACE INTO TABLE im_library_ancestry
FIELDS TERMINATED BY "," OPTIONALLY ENCLOSED BY '"'
TRAILING NULLCOLS
(
   line_number,
   etl_date DATE "YYYYMMDDHH24MISS",
   is_delete,
   ancestor_event_id,
   ancestor_library_id,
   ancestor_library_type,
   ancestor_library_creation DATE "YYYYMMDDHH24MISS",
   child_event_id,
   child_library_id,
   child_library_type,
   child_library_creation DATE "YYYYMMDDHH24MISS"
)

