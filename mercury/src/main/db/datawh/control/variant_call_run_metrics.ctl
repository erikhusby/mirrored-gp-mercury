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
  total_rate,
  biallelic,
  biallelic_rate,
  multiallelic,
  multiallelic_rate,
  snps,
  snps_rate,
  insertions_hom,
  insertions_hom_rate,
  insertions_het,
  insertions_het_rate,
  deletions_hom,
  deletions_hom_rate,
  deletions_het,
  deletions_het_rate,
  indels,
  indels_rate,
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
  in_dbsnp_rate,
  not_in_dbsnp,
  not_in_dbsnp_rate,
  pct_callability,
  pct_autosome_callability,
  pct_autosome_exo_callability,
  vc_run_metric_id "SEQ_VC_RUN_METRIC.NEXTVAL"
)