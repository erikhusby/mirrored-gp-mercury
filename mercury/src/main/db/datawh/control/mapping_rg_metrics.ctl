LOAD DATA
APPEND INTO TABLE mapping_rg_metrics
FIELDS TERMINATED BY "," OPTIONALLY ENCLOSED BY '"'
TRAILING NULLCOLS
(
  read_group,
  run_date DATE "YYYYMMDDHH24MISS",
  sample_alias,
  analysis_name,
  analysis_node,
  dragen_version,
  total_reads_rg,
  num_dup_marked_reads,
  num_dup_marked_removed,
  num_unique_reads,
  num_reads_mate_sequenced,
  num_reads_wo_mate_sequenced,
  num_qc_failed_reads,
  num_mapped_reads,
  num_mapped_reads_r1,
  num_mapped_reads_r2,
  num_unq_mapped_reads,
  num_unmapped_reads,
  num_singleton_reads,
  num_paired_reads,
  num_properly_paired_reads,
  num_not_properly_paired_reads,
  paired_reads_map_diff_chrom,
  paired_reads_map_diff_chr_q10,
  mapq_40_inf,
  mapq_30_40,
  mapq_20_30,
  mapq_10_20,
  mapq_0_10,
  mapq_na,
  reads_indel_r1,
  reads_indel_r2,
  total_bases,
  total_bases_r1,
  total_bases_r2,
  mapped_bases_r1,
  mapped_bases_r2,
  soft_clipped_bases_r1,
  soft_clipped_bases_r2,
  mismatched_bases_r1,
  mismatched_bases_r2,
  mismatched_bases_excl_r1,
  mismatched_bases_excl_r2,
  q30_bases,
  q30_bases_r1,
  q30_bases_r2,
  q30_bases_excl_dupes,
  total_alignments,
  secondary_alignments,
  supplementary_alignments,
  est_read_length,
  avg_cov_genome,
  insert_length_mean,
  insert_length_median,
  insert_length_stddev
)