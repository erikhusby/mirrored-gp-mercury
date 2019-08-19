LOAD DATA
APPEND INTO TABLE vc_run_metrics
FIELDS TERMINATED BY "," OPTIONALLY ENCLOSED BY '"'
TRAILING NULLCOLS
(
  run_name,
  run_date DATE "YYYYMMDDHH24MISS",
  analysis_node,
  dragen_version,
  num_samples,
  reads_processed,
  child_sample,
  vc_run_metric_id "SEQ_VC_RUN_METRIC.NEXTVAL"
)