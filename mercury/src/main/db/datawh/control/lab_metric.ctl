UNRECOVERABLE LOAD DATA
INFILE 'from command line'
REPLACE INTO TABLE im_lab_metric
FIELDS TERMINATED BY "," OPTIONALLY ENCLOSED BY '"'
TRAILING NULLCOLS
(
  line_number,
  etl_date DATE "YYYYMMDDHH24MISS",
  is_delete,
  lab_metric_id,
  quant_type,
  quant_units,
  quant_value,
  run_name,
  run_date DATE "YYYYMMDDHH24MISS",
  lab_vessel_id,
  vessel_barcode,
  rack_position,
  decision,
  decision_date DATE "YYYYMMDDHH24MISS",
  decider,
  override_reason,
  rework_disposition,
  decision_note
)

