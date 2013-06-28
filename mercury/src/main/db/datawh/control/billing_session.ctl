UNRECOVERABLE LOAD DATA
INFILE 'from command line'
REPLACE INTO TABLE im_billing_session
FIELDS TERMINATED BY "," OPTIONALLY ENCLOSED BY '"'
TRAILING NULLCOLS
(
 line_number,
 etl_date DATE "YYYYMMDDHH24MISS",
 is_delete,
 ledger_id,
 billing_session_id,
 billed_date DATE "YYYYMMDDHH24MISS",
 billing_session_type
)
