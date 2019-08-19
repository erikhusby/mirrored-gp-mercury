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
  sample_alias,
  num_perfect_reads,
  num_of_reads,
  num_of_pct_idx_reads,
  num_of_one_mismatch_idx_reads,
  num_of_q30_bases_pf,
  mean_quality_score_pf,
  sequencing_run_metric_id "SEQ_SEQ_RUN_METRIC.NEXTVAL"
)