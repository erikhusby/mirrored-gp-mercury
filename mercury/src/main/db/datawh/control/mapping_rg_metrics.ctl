LOAD DATA
APPEND INTO TABLE mapping_rg_metrics
FIELDS TERMINATED BY "," OPTIONALLY ENCLOSED BY '"'
TRAILING NULLCOLS
(
  run_name,
  run_date DATE "YYYYMMDDHH24MISS",
  analysis_name,
  read_group,
  sample_alias,
  total_reads,
  num_dup_marked_reads,
  num_dup_marked_removed,
  num_unique_reads,
  num_reads_mate_sequenced,
  num_reads_wo_mate_sequenced,
  num_qc_failed_reads,
  num_mapped_reads,
  num_unq_mapped_reads,
  num_unmapped_reads,
  num_singleton_reads,
  num_paired_reads,
  num_properly_paired_reads,
  num_not_properly_paired_reads,
  mapq_40_inf,
  mapq_30_40,
  mapq_20_30,
  mapq_10_20,
  mapq_0_10,
  mapq_na,
  reads_indel_r1,
  reads_indel_r2,
  soft_clipped_bases_r1,
  soft_clipped_bases_r2,
  total_alignments,
  secondary_alignments,
  supplementary_alignments,
  est_read_length,
  avg_seq_coverage,
  insert_length_mean,
  insert_length_std,
  mapping_rg_metric_id "SEQ_MAP_RG_METRIC.NEXTVAL"
)