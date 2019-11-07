LOAD DATA
APPEND INTO TABLE vc_run_metrics
FIELDS TERMINATED BY "," OPTIONALLY ENCLOSED BY '"'
TRAILING NULLCOLS
(
  run_name,
  run_date DATE "YYYYMMDDHH24MISS",
  analysis_name,
  analysis_node,
  dragen_version,
  sample_alias,
  metric_type,
  num_samples,
  reads_processed,
  child_sample,
  total,
  biallelic,
  multiallelic,
  snps,
  insertions_hom,
  insertions_het,
  deletions_hom,
  deletions_het,
  indels,
  chr_x_number_of_snps,
  chr_y_number_of_snps,
  chr_x_y_ratio,
  snp_transitions,
  snp_tranversions,
  ti_tv_ratio,
  heterozygous,
  homozygous,
  het_hom_ratio,
  in_dbsnp,
  not_in_dbsnp,
  vc_run_metric_id "SEQ_VC_RUN_METRIC.NEXTVAL"
)