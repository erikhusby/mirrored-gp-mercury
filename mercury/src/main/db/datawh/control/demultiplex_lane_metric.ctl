LOAD DATA
APPEND INTO TABLE SEQ_DEMULTIPLEX_RUN_METRIC
FIELDS TERMINATED BY "," OPTIONALLY ENCLOSED BY '"'
TRAILING NULLCOLS
(
  run_name,
  flowcell,
  run_date DATE "YYYYMMDDHH24MISS",
  dragen_version,
  analysis_version,
  analysis_name,
  analysis_node,
  lane,
  orphan_rate,
  sequencing_lane_metric_id "SEQ_SEQ_RUN_METRIC.NEXTVAL"
)