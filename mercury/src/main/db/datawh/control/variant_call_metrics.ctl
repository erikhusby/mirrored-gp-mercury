LOAD DATA
APPEND INTO TABLE variant_call_metrics
FIELDS TERMINATED BY "," OPTIONALLY ENCLOSED BY '"'
TRAILING NULLCOLS
(
  run_name,
  run_date DATE "YYYYMMDDHH24MISS",
  analysis_name,
  metric_type,
  sample_alias,
  total,
  biallelic,
  multiallelic,
  snps,
  indels,
  mnps,
  chr_x_number_of_snps,
  chr_y_number_of_snps,
  snp_transitions,
  snp_tranversions,
  heterozygous,
  homozygous,
  in_dbsnp,
  not_in_dbsnp,
  vc_call_metrics_id "SEQ_VC_METRIC.NEXTVAL"
)