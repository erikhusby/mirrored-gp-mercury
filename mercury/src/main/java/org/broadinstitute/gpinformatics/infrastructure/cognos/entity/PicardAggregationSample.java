/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2018 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure.cognos.entity;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


@Entity
@Table(schema = "COGNOS", name = "SLXRE2_PAGG_SAMPLE")
public class PicardAggregationSample implements Serializable {
    private static final long serialVersionUID = -5462724587465812910L;

    @EmbeddedId
    private PicardAggregationSamplePk picardAggregationSamplePk;
    @Column(name = "PROJECT", nullable = false, insertable = false, updatable = false)
    private String project;
    @Column(name = "SAMPLE", nullable = false, insertable = false, updatable = false)
    private String sample;
    @Column(name = "DATA_TYPE", nullable = false, insertable = false, updatable = false)
    private String dataType;
    @Column(name = "RESEARCH_PROJECT_NUMBER", nullable = false, insertable = false, updatable = false)
    private String researchProject;
    @Column(name = "PRODUCT_ORDER_KEY", nullable = false, insertable = false, updatable = false)
    private String productOrder;

    public PicardAggregationSample() {
    }

    public PicardAggregationSample(String researchProject, String project, String productOrders, String sample,
                                   String dataType) {
        this.researchProject = researchProject;
        this.project = project;
        this.productOrder = productOrders;
        this.sample = sample;
        this.dataType = dataType;
    }

    public String getProject() {
        return project;
    }

    public String getSample() {
        return sample;
    }

    public String getDataType() {
        return dataType;
    }

    public String getResearchProject() {
        return researchProject;
    }

    public List<String> getProductOrderList() {
        if (StringUtils.isBlank(productOrder)) {
            return Collections.emptyList();
        }
        return Arrays.stream(productOrder.split(",")).map(String::trim).collect(Collectors.toList());
    }

    /*
    If we use more of these columns we will be able to reduce the number of joins off of aggregation. See: GPLIM-5415
    COGNOS.SLXRE2_PAGG_SAMPLE.PROJECT
    COGNOS.SLXRE2_PAGG_SAMPLE.SAMPLE
    COGNOS.SLXRE2_PAGG_SAMPLE.DATA_TYPE
    COGNOS.SLXRE2_PAGG_SAMPLE.TIMESTAMP
    COGNOS.SLXRE2_PAGG_SAMPLE.PRODUCT_ORDER_KEY
    COGNOS.SLXRE2_PAGG_SAMPLE.PRODUCT
    COGNOS.SLXRE2_PAGG_SAMPLE.PRODUCT_PART_NUMBER
    COGNOS.SLXRE2_PAGG_SAMPLE.PRODUCT_ORDER_SAMPLE
    COGNOS.SLXRE2_PAGG_SAMPLE.RESEARCH_PROJECT_NAME
    COGNOS.SLXRE2_PAGG_SAMPLE.RESEARCH_PROJECT_NUMBER
    COGNOS.SLXRE2_PAGG_SAMPLE.GSSR_ID
    COGNOS.SLXRE2_PAGG_SAMPLE.LAST_RUN_END_DATE
    COGNOS.SLXRE2_PAGG_SAMPLE.INDIVIDUAL_NAME
    COGNOS.SLXRE2_PAGG_SAMPLE.ON_RISK
    COGNOS.SLXRE2_PAGG_SAMPLE.RISK_TYPES
    COGNOS.SLXRE2_PAGG_SAMPLE.SAMPLE_IDS
    COGNOS.SLXRE2_PAGG_SAMPLE.LSID
    COGNOS.SLXRE2_PAGG_SAMPLE.LCSET
    COGNOS.SLXRE2_PAGG_SAMPLE.WORKFLOW
    COGNOS.SLXRE2_PAGG_SAMPLE.ANALYSIS_TYPE
    COGNOS.SLXRE2_PAGG_SAMPLE.ANALYSIS_ID
    COGNOS.SLXRE2_PAGG_SAMPLE.ANALYSIS_START
    COGNOS.SLXRE2_PAGG_SAMPLE.ANALYSIS_END
    COGNOS.SLXRE2_PAGG_SAMPLE.VERSION
    COGNOS.SLXRE2_PAGG_SAMPLE.N_LANES
    COGNOS.SLXRE2_PAGG_SAMPLE.AGGREGATED_LANES
    COGNOS.SLXRE2_PAGG_SAMPLE.WR_ID
    COGNOS.SLXRE2_PAGG_SAMPLE.AL_TOTAL_READS
    COGNOS.SLXRE2_PAGG_SAMPLE.AL_PF_READS
    COGNOS.SLXRE2_PAGG_SAMPLE.AL_PF_NOISE_READS
    COGNOS.SLXRE2_PAGG_SAMPLE.AL_PF_READS_ALIGNED
    COGNOS.SLXRE2_PAGG_SAMPLE.AL_PF_HQ_ALIGNED_READS
    COGNOS.SLXRE2_PAGG_SAMPLE.AL_PF_HQ_ALIGNED_BASES
    COGNOS.SLXRE2_PAGG_SAMPLE.AL_PF_HQ_ALIGNED_Q20_BASES
    COGNOS.SLXRE2_PAGG_SAMPLE.AL_PF_ALIGNED_BASES
    COGNOS.SLXRE2_PAGG_SAMPLE.AL_PF_MISMATCH_RATE
    COGNOS.SLXRE2_PAGG_SAMPLE.AL_PCT_CHIMERAS
    COGNOS.SLXRE2_PAGG_SAMPLE.AL_PCT_ADAPTER
    COGNOS.SLXRE2_PAGG_SAMPLE.AP_BAD_CYCLES
    COGNOS.SLXRE2_PAGG_SAMPLE.AP_MEAN_READ_LENGTH
    COGNOS.SLXRE2_PAGG_SAMPLE.AP_PCT_PF_READS
    COGNOS.SLXRE2_PAGG_SAMPLE.AP_PCT_PF_READS_ALIGNED
    COGNOS.SLXRE2_PAGG_SAMPLE.AP_PCT_READS_ALIGNED_IN_PAIRS
    COGNOS.SLXRE2_PAGG_SAMPLE.AP_PCT_STRAND_BALANCE
    COGNOS.SLXRE2_PAGG_SAMPLE.AP_PF_HQ_ALIGNED_BASES
    COGNOS.SLXRE2_PAGG_SAMPLE.AP_PF_HQ_ALIGNED_Q20_BASES
    COGNOS.SLXRE2_PAGG_SAMPLE.AP_PF_HQ_ALIGNED_READS
    COGNOS.SLXRE2_PAGG_SAMPLE.AP_PF_HQ_MEDIAN_MISMATCHES
    COGNOS.SLXRE2_PAGG_SAMPLE.AP_PF_HQ_ERROR_RATE
    COGNOS.SLXRE2_PAGG_SAMPLE.AP_PF_NOISE_READS
    COGNOS.SLXRE2_PAGG_SAMPLE.AP_PF_READS
    COGNOS.SLXRE2_PAGG_SAMPLE.AP_PF_READS_ALIGNED
    COGNOS.SLXRE2_PAGG_SAMPLE.AP_READS_ALIGNED_IN_PAIRS
    COGNOS.SLXRE2_PAGG_SAMPLE.AP_TOTAL_READS
    COGNOS.SLXRE2_PAGG_SAMPLE.AP_PF_ALIGNED_BASES
    COGNOS.SLXRE2_PAGG_SAMPLE.AP_PF_MISMATCH_RATE
    COGNOS.SLXRE2_PAGG_SAMPLE.AP_PCT_ADAPTER
    COGNOS.SLXRE2_PAGG_SAMPLE.ANP_BAD_CYCLES
    COGNOS.SLXRE2_PAGG_SAMPLE.ANP_MEAN_READ_LENGTH
    COGNOS.SLXRE2_PAGG_SAMPLE.ANP_PCT_PF_READS
    COGNOS.SLXRE2_PAGG_SAMPLE.ANP_PCT_PF_READS_ALIGNED
    COGNOS.SLXRE2_PAGG_SAMPLE.ANP_PCT_READS_ALIGNED_IN_PAIRS
    COGNOS.SLXRE2_PAGG_SAMPLE.ANP_PCT_STRAND_BALANCE
    COGNOS.SLXRE2_PAGG_SAMPLE.ANP_PF_HQ_ALIGNED_BASES
    COGNOS.SLXRE2_PAGG_SAMPLE.ANP_PF_HQ_ALIGNED_Q20_BASES
    COGNOS.SLXRE2_PAGG_SAMPLE.ANP_PF_HQ_ALIGNED_READS
    COGNOS.SLXRE2_PAGG_SAMPLE.ANP_PF_HQ_MEDIAN_MISMATCHES
    COGNOS.SLXRE2_PAGG_SAMPLE.ANP_PF_HQ_ERROR_RATE
    COGNOS.SLXRE2_PAGG_SAMPLE.ANP_PF_NOISE_READS
    COGNOS.SLXRE2_PAGG_SAMPLE.ANP_PF_READS
    COGNOS.SLXRE2_PAGG_SAMPLE.ANP_PF_READS_ALIGNED
    COGNOS.SLXRE2_PAGG_SAMPLE.ANP_READS_ALIGNED_IN_PAIRS
    COGNOS.SLXRE2_PAGG_SAMPLE.ANP_TOTAL_READS
    COGNOS.SLXRE2_PAGG_SAMPLE.ANP_PF_ALIGNED_BASES
    COGNOS.SLXRE2_PAGG_SAMPLE.ANP_PF_MISMATCH_RATE
    COGNOS.SLXRE2_PAGG_SAMPLE.ANP_PCT_ADAPTER
    COGNOS.SLXRE2_PAGG_SAMPLE.PF_READS
    COGNOS.SLXRE2_PAGG_SAMPLE.HS_BAIT_DESIGN_EFFICIENCY
    COGNOS.SLXRE2_PAGG_SAMPLE.HS_BAIT_SET
    COGNOS.SLXRE2_PAGG_SAMPLE.HS_BAIT_TERRITORY
    COGNOS.SLXRE2_PAGG_SAMPLE.HS_FOLD_80_BASE_PENALTY
    COGNOS.SLXRE2_PAGG_SAMPLE.HS_FOLD_ENRICHMENT
    COGNOS.SLXRE2_PAGG_SAMPLE.HS_GENOME_SIZE
    COGNOS.SLXRE2_PAGG_SAMPLE.HS_LIBRARY_SIZE
    COGNOS.SLXRE2_PAGG_SAMPLE.HS_MEAN_BAIT_COVERAGE
    COGNOS.SLXRE2_PAGG_SAMPLE.HS_MEAN_TARGET_COVERAGE
    COGNOS.SLXRE2_PAGG_SAMPLE.HS_NEAR_BAIT_BASES
    COGNOS.SLXRE2_PAGG_SAMPLE.HS_OFF_BAIT_BASES
    COGNOS.SLXRE2_PAGG_SAMPLE.HS_ON_BAIT_BASES
    COGNOS.SLXRE2_PAGG_SAMPLE.HS_ON_BAIT_VS_SELECTED
    COGNOS.SLXRE2_PAGG_SAMPLE.HS_ON_TARGET_BASES
    COGNOS.SLXRE2_PAGG_SAMPLE.HS_PCT_OFF_BAIT
    COGNOS.SLXRE2_PAGG_SAMPLE.HS_PCT_PF_READS
    COGNOS.SLXRE2_PAGG_SAMPLE.HS_PCT_PF_UQ_READS
    COGNOS.SLXRE2_PAGG_SAMPLE.HS_PCT_PF_UQ_READS_ALIGNED
    COGNOS.SLXRE2_PAGG_SAMPLE.HS_PCT_SELECTED_BASES
    COGNOS.SLXRE2_PAGG_SAMPLE.HS_PCT_TARGET_BASES_10X
    COGNOS.SLXRE2_PAGG_SAMPLE.HS_PCT_TARGET_BASES_20X
    COGNOS.SLXRE2_PAGG_SAMPLE.HS_PCT_TARGET_BASES_2X
    COGNOS.SLXRE2_PAGG_SAMPLE.HS_PCT_TARGET_BASES_30X
    COGNOS.SLXRE2_PAGG_SAMPLE.HS_PCT_TARGET_BASES_40X
    COGNOS.SLXRE2_PAGG_SAMPLE.HS_PCT_TARGET_BASES_50X
    COGNOS.SLXRE2_PAGG_SAMPLE.HS_PCT_TARGET_BASES_100X
    COGNOS.SLXRE2_PAGG_SAMPLE.HS_PCT_USABLE_BASES_ON_BAIT
    COGNOS.SLXRE2_PAGG_SAMPLE.HS_PCT_USABLE_BASES_ON_TARGET
    COGNOS.SLXRE2_PAGG_SAMPLE.HS_PF_UNIQUE_READS
    COGNOS.SLXRE2_PAGG_SAMPLE.HS_PF_UQ_BASES_ALIGNED
    COGNOS.SLXRE2_PAGG_SAMPLE.HS_PF_UQ_READS_ALIGNED
    COGNOS.SLXRE2_PAGG_SAMPLE.HS_TARGET_TERRITORY
    COGNOS.SLXRE2_PAGG_SAMPLE.HS_TOTAL_READS
    COGNOS.SLXRE2_PAGG_SAMPLE.HS_ZERO_CVG_TARGETS_PCT
    COGNOS.SLXRE2_PAGG_SAMPLE.SAMPLE_HS_PCT_TARGET_BASES_20X
    COGNOS.SLXRE2_PAGG_SAMPLE.SNP_NUM_IN_DBSNP
    COGNOS.SLXRE2_PAGG_SAMPLE.SNP_PCT_DBSNP
    COGNOS.SLXRE2_PAGG_SAMPLE.SNP_TOTAL_SNPS
    COGNOS.SLXRE2_PAGG_SAMPLE.INITIATIVE_NAME
    COGNOS.SLXRE2_PAGG_SAMPLE.HS_PENALTY_10X
    COGNOS.SLXRE2_PAGG_SAMPLE.HS_PENALTY_20X
    COGNOS.SLXRE2_PAGG_SAMPLE.HS_PENALTY_30X
    COGNOS.SLXRE2_PAGG_SAMPLE.HS_PENALTY_40X
    COGNOS.SLXRE2_PAGG_SAMPLE.HS_PENALTY_50X
    COGNOS.SLXRE2_PAGG_SAMPLE.HS_PENALTY_100X
    COGNOS.SLXRE2_PAGG_SAMPLE.RNA_PF_ALIGNED_BASES
    COGNOS.SLXRE2_PAGG_SAMPLE.RNA_RIBOSOMAL_BASES
    COGNOS.SLXRE2_PAGG_SAMPLE.RNA_CODING_BASES
    COGNOS.SLXRE2_PAGG_SAMPLE.RNA_UTR_BASES
    COGNOS.SLXRE2_PAGG_SAMPLE.RNA_INTRONIC_BASES
    COGNOS.SLXRE2_PAGG_SAMPLE.RNA_INTERGENIC_BASES
    COGNOS.SLXRE2_PAGG_SAMPLE.RNA_CORRECT_STRAND_READS
    COGNOS.SLXRE2_PAGG_SAMPLE.RNA_INCORRECT_STRAND_READS
    COGNOS.SLXRE2_PAGG_SAMPLE.RNA_PCT_RIBOSOMAL_BASES
    COGNOS.SLXRE2_PAGG_SAMPLE.RNA_PCT_CODING_BASES
    COGNOS.SLXRE2_PAGG_SAMPLE.RNA_PCT_UTR_BASES
    COGNOS.SLXRE2_PAGG_SAMPLE.RNA_PCT_INTRONIC_BASES
    COGNOS.SLXRE2_PAGG_SAMPLE.RNA_PCT_INTERGENIC_BASES
    COGNOS.SLXRE2_PAGG_SAMPLE.RNA_PCT_MRNA_BASES
    COGNOS.SLXRE2_PAGG_SAMPLE.RNA_PCT_CORRECT_STRAND_READS
    COGNOS.SLXRE2_PAGG_SAMPLE.RNA_PF_BASES
    COGNOS.SLXRE2_PAGG_SAMPLE.RNA_PCT_USABLE_BASES
    COGNOS.SLXRE2_PAGG_SAMPLE.RNA_MEDIAN_CV_COVERAGE
    COGNOS.SLXRE2_PAGG_SAMPLE.RNA_MEDIAN_5PRIME_BIAS
    COGNOS.SLXRE2_PAGG_SAMPLE.RNA_MEDIAN_3PRIME_BIAS
    COGNOS.SLXRE2_PAGG_SAMPLE.RNA_MEDIAN_5PRIM_TO_3PRIM_BIAS
    COGNOS.SLXRE2_PAGG_SAMPLE.INS_MEDIAN_INSERT_SIZE
    COGNOS.SLXRE2_PAGG_SAMPLE.INS_MIN_INSERT_SIZE
    COGNOS.SLXRE2_PAGG_SAMPLE.INS_MAX_INSERT_SIZE
    COGNOS.SLXRE2_PAGG_SAMPLE.INS_MEAN_INSERT_SIZE
    COGNOS.SLXRE2_PAGG_SAMPLE.INS_STANDARD_DEVIATION
    COGNOS.SLXRE2_PAGG_SAMPLE.INS_READ_PAIRS
    COGNOS.SLXRE2_PAGG_SAMPLE.INS_WIDTH_OF_10_PERCENT
    COGNOS.SLXRE2_PAGG_SAMPLE.INS_WIDTH_OF_20_PERCENT
    COGNOS.SLXRE2_PAGG_SAMPLE.INS_WIDTH_OF_30_PERCENT
    COGNOS.SLXRE2_PAGG_SAMPLE.INS_WIDTH_OF_40_PERCENT
    COGNOS.SLXRE2_PAGG_SAMPLE.INS_WIDTH_OF_50_PERCENT
    COGNOS.SLXRE2_PAGG_SAMPLE.INS_WIDTH_OF_60_PERCENT
    COGNOS.SLXRE2_PAGG_SAMPLE.INS_WIDTH_OF_70_PERCENT
    COGNOS.SLXRE2_PAGG_SAMPLE.INS_WIDTH_OF_80_PERCENT
    COGNOS.SLXRE2_PAGG_SAMPLE.INS_WIDTH_OF_90_PERCENT
    COGNOS.SLXRE2_PAGG_SAMPLE.INS_WIDTH_OF_99_PERCENT
    COGNOS.SLXRE2_PAGG_SAMPLE.INS_PAIR_ORIENTATION
    COGNOS.SLXRE2_PAGG_SAMPLE.PCR_CUSTOM_AMPLICON_SET
    COGNOS.SLXRE2_PAGG_SAMPLE.PCR_GENOME_SIZE
    COGNOS.SLXRE2_PAGG_SAMPLE.PCR_AMPLICON_TERRITORY
    COGNOS.SLXRE2_PAGG_SAMPLE.PCR_TARGET_TERRITORY
    COGNOS.SLXRE2_PAGG_SAMPLE.PCR_TOTAL_READS
    COGNOS.SLXRE2_PAGG_SAMPLE.PCR_PF_READS
    COGNOS.SLXRE2_PAGG_SAMPLE.PCR_PF_UNIQUE_READS
    COGNOS.SLXRE2_PAGG_SAMPLE.PCR_PCT_PF_READS
    COGNOS.SLXRE2_PAGG_SAMPLE.PCR_PCT_PF_UQ_READS
    COGNOS.SLXRE2_PAGG_SAMPLE.PCR_PF_UQ_READS_ALIGNED
    COGNOS.SLXRE2_PAGG_SAMPLE.PCR_PCT_PF_UQ_READS_ALIGNED
    COGNOS.SLXRE2_PAGG_SAMPLE.PCR_PF_UQ_BASES_ALIGNED
    COGNOS.SLXRE2_PAGG_SAMPLE.PCR_ON_AMPLICON_BASES
    COGNOS.SLXRE2_PAGG_SAMPLE.PCR_NEAR_AMPLICON_BASES
    COGNOS.SLXRE2_PAGG_SAMPLE.PCR_OFF_AMPLICON_BASES
    COGNOS.SLXRE2_PAGG_SAMPLE.PCR_ON_TARGET_BASES
    COGNOS.SLXRE2_PAGG_SAMPLE.PCR_PCT_AMPLIFIED_BASES
    COGNOS.SLXRE2_PAGG_SAMPLE.PCR_PCT_OFF_AMPLICON
    COGNOS.SLXRE2_PAGG_SAMPLE.PCR_ON_AMPLICON_VS_SELECTED
    COGNOS.SLXRE2_PAGG_SAMPLE.PCR_MEAN_AMPLICON_COVERAGE
    COGNOS.SLXRE2_PAGG_SAMPLE.PCR_MEAN_TARGET_COVERAGE
    COGNOS.SLXRE2_PAGG_SAMPLE.PCR_FOLD_ENRICHMENT
    COGNOS.SLXRE2_PAGG_SAMPLE.PCR_ZERO_CVG_TARGETS_PCT
    COGNOS.SLXRE2_PAGG_SAMPLE.PCR_FOLD_80_BASE_PENALTY
    COGNOS.SLXRE2_PAGG_SAMPLE.PCR_PCT_TARGET_BASES_2X
    COGNOS.SLXRE2_PAGG_SAMPLE.PCR_PCT_TARGET_BASES_10X
    COGNOS.SLXRE2_PAGG_SAMPLE.PCR_PCT_TARGET_BASES_20X
    COGNOS.SLXRE2_PAGG_SAMPLE.PCR_PCT_TARGET_BASES_30X
    COGNOS.SLXRE2_PAGG_SAMPLE.PCR_AT_DROPOUT
    COGNOS.SLXRE2_PAGG_SAMPLE.PCR_GC_DROPOUT
    COGNOS.SLXRE2_PAGG_SAMPLE.PCT_CONTAMINATION
    COGNOS.SLXRE2_PAGG_SAMPLE.RRBS_READS_ALIGNED
    COGNOS.SLXRE2_PAGG_SAMPLE.RRBS_NON_CPG_BASES
    COGNOS.SLXRE2_PAGG_SAMPLE.RRBS_NON_CPG_CONVERTED_BASES
    COGNOS.SLXRE2_PAGG_SAMPLE.RRBS_PCT_NONCPG_BASES_CONVERTD
    COGNOS.SLXRE2_PAGG_SAMPLE.RRBS_CPG_BASES_SEEN
    COGNOS.SLXRE2_PAGG_SAMPLE.RRBS_CPG_BASES_CONVERTED
    COGNOS.SLXRE2_PAGG_SAMPLE.RRBS_PCT_CPG_BASES_CONVERTED
    COGNOS.SLXRE2_PAGG_SAMPLE.RRBS_MEAN_CPG_COVERAGE
    COGNOS.SLXRE2_PAGG_SAMPLE.RRBS_MEDIAN_CPG_COVERAGE
    COGNOS.SLXRE2_PAGG_SAMPLE.RRBS_READS_WITH_NO_CPG
    COGNOS.SLXRE2_PAGG_SAMPLE.RRBS_READS_IGNORED_SHORT
    COGNOS.SLXRE2_PAGG_SAMPLE.RRBS_READS_IGNORED_MISMATCHES
    COGNOS.SLXRE2_PAGG_SAMPLE.BC_PF_READS
    COGNOS.SLXRE2_PAGG_SAMPLE.BC_TOTAL_READS
    COGNOS.SLXRE2_PAGG_SAMPLE.BC_PF_BASES
    COGNOS.SLXRE2_PAGG_SAMPLE.WGS_GENOME_TERRITORY
    COGNOS.SLXRE2_PAGG_SAMPLE.WGS_MEAN_COVERAGE
    COGNOS.SLXRE2_PAGG_SAMPLE.WGS_SD_COVERAGE
    COGNOS.SLXRE2_PAGG_SAMPLE.WGS_MEDIAN_COVERAGE
    COGNOS.SLXRE2_PAGG_SAMPLE.WGS_MAD_COVERAGE
    COGNOS.SLXRE2_PAGG_SAMPLE.WGS_PCT_EXC_MAPQ
    COGNOS.SLXRE2_PAGG_SAMPLE.WGS_PCT_EXC_DUPE
    COGNOS.SLXRE2_PAGG_SAMPLE.WGS_PCT_EXC_UNPAIRED
    COGNOS.SLXRE2_PAGG_SAMPLE.WGS_PCT_EXC_BASEQ
    COGNOS.SLXRE2_PAGG_SAMPLE.WGS_PCT_EXC_OVERLAP
    COGNOS.SLXRE2_PAGG_SAMPLE.WGS_PCT_EXC_CAPPED
    COGNOS.SLXRE2_PAGG_SAMPLE.WGS_PCT_EXC_TOTAL
    COGNOS.SLXRE2_PAGG_SAMPLE.WGS_PCT_5X
    COGNOS.SLXRE2_PAGG_SAMPLE.WGS_PCT_10X
    COGNOS.SLXRE2_PAGG_SAMPLE.WGS_PCT_20X
    COGNOS.SLXRE2_PAGG_SAMPLE.WGS_PCT_30X
    COGNOS.SLXRE2_PAGG_SAMPLE.WGS_PCT_40X
    COGNOS.SLXRE2_PAGG_SAMPLE.WGS_PCT_50X
    COGNOS.SLXRE2_PAGG_SAMPLE.WGS_PCT_60X
    COGNOS.SLXRE2_PAGG_SAMPLE.WGS_PCT_70X
    COGNOS.SLXRE2_PAGG_SAMPLE.WGS_PCT_80X
    COGNOS.SLXRE2_PAGG_SAMPLE.WGS_PCT_90X
    COGNOS.SLXRE2_PAGG_SAMPLE.WGS_PCT_100X
    COGNOS.SLXRE2_PAGG_SAMPLE.MIN_LOD
    COGNOS.SLXRE2_PAGG_SAMPLE.MAX_LOD
    COGNOS.SLXRE2_PAGG_SAMPLE.LCSET_TYPE
    COGNOS.SLXRE2_PAGG_SAMPLE.WGS_PCT_15X
    COGNOS.SLXRE2_PAGG_SAMPLE.WGS_PCT_25X
    COGNOS.SLXRE2_PAGG_SAMPLE.RWGS_GENOME_TERRITORY
    COGNOS.SLXRE2_PAGG_SAMPLE.RWGS_MEAN_COVERAGE
    COGNOS.SLXRE2_PAGG_SAMPLE.RWGS_SD_COVERAGE
    COGNOS.SLXRE2_PAGG_SAMPLE.RWGS_MEDIAN_COVERAGE
    COGNOS.SLXRE2_PAGG_SAMPLE.RWGS_MAD_COVERAGE
    COGNOS.SLXRE2_PAGG_SAMPLE.RWGS_PCT_EXC_MAPQ
    COGNOS.SLXRE2_PAGG_SAMPLE.RWGS_PCT_EXC_DUPE
    COGNOS.SLXRE2_PAGG_SAMPLE.RWGS_PCT_EXC_UNPAIRED
    COGNOS.SLXRE2_PAGG_SAMPLE.RWGS_PCT_EXC_BASEQ
    COGNOS.SLXRE2_PAGG_SAMPLE.RWGS_PCT_EXC_OVERLAP
    COGNOS.SLXRE2_PAGG_SAMPLE.RWGS_PCT_EXC_CAPPED
    COGNOS.SLXRE2_PAGG_SAMPLE.RWGS_PCT_EXC_TOTAL
    COGNOS.SLXRE2_PAGG_SAMPLE.RWGS_PCT_5X
    COGNOS.SLXRE2_PAGG_SAMPLE.RWGS_PCT_10X
    COGNOS.SLXRE2_PAGG_SAMPLE.RWGS_PCT_20X
    COGNOS.SLXRE2_PAGG_SAMPLE.RWGS_PCT_30X
    COGNOS.SLXRE2_PAGG_SAMPLE.RWGS_PCT_40X
    COGNOS.SLXRE2_PAGG_SAMPLE.RWGS_PCT_50X
    COGNOS.SLXRE2_PAGG_SAMPLE.RWGS_PCT_60X
    COGNOS.SLXRE2_PAGG_SAMPLE.RWGS_PCT_70X
    COGNOS.SLXRE2_PAGG_SAMPLE.RWGS_PCT_80X
    COGNOS.SLXRE2_PAGG_SAMPLE.RWGS_PCT_90X
    COGNOS.SLXRE2_PAGG_SAMPLE.RWGS_PCT_100X
    COGNOS.SLXRE2_PAGG_SAMPLE.RWGS_PCT_15X
    COGNOS.SLXRE2_PAGG_SAMPLE.RWGS_PCT_25X
    COGNOS.SLXRE2_PAGG_SAMPLE.LCSET_PROTOCOL
    COGNOS.SLXRE2_PAGG_SAMPLE.LCSET_SEQ_TECHNOLOGY
    COGNOS.SLXRE2_PAGG_SAMPLE.LCSET_TOPOFF
    COGNOS.SLXRE2_PAGG_SAMPLE.SOURCE
    COGNOS.SLXRE2_PAGG_SAMPLE.PDO_TITLE
    COGNOS.SLXRE2_PAGG_SAMPLE.AP_PF_INDEL_RATE
    COGNOS.SLXRE2_PAGG_SAMPLE.ANP_PF_INDEL_RATE
    COGNOS.SLXRE2_PAGG_SAMPLE.BSP_ORIGINAL_MATERIAL_TYPE
    COGNOS.SLXRE2_PAGG_SAMPLE.BSP_ROOT_MATERIAL_TYPE
    COGNOS.SLXRE2_PAGG_SAMPLE.N_HISEQ_POOL_TEST_LANES
    COGNOS.SLXRE2_PAGG_SAMPLE.N_AGGREGATED_RG
    COGNOS.SLXRE2_PAGG_SAMPLE.WGS_PCT_1X
    COGNOS.SLXRE2_PAGG_SAMPLE.WGS_HET_SNP_SENSITIVITY
    COGNOS.SLXRE2_PAGG_SAMPLE.WGS_HET_SNP_Q
    COGNOS.SLXRE2_PAGG_SAMPLE.PROCESSING_LOCATION
    COGNOS.SLXRE2_PAGG_SAMPLE.SAMPLE_TYPE
    COGNOS.SLXRE2_PAGG_SAMPLE.INDEX_TYPE
    COGNOS.SLXRE2_PAGG_SAMPLE.SAMPLE_LOD
     */

    @Embeddable
    public static class PicardAggregationSamplePk implements Serializable {
        private static final long serialVersionUID = -7350061514732278517L;

        @Column(name = "PROJECT", nullable = false, insertable = false, updatable = false)
        private String project;
        @Column(name = "SAMPLE", nullable = false, insertable = false, updatable = false)
        private String sample;
        @Column(name = "DATA_TYPE", nullable = false, insertable = false, updatable = false)
        private String dataType;

        public String getProject() {
            return project;
        }

        public String getSample() {
            return sample;
        }

        public String getDataType() {
            return dataType;
        }

        @Override
        @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || (!OrmUtil.proxySafeIsInstance(o, PicardAggregationSamplePk.class))) {
                return false;
            }

            if (!(o instanceof PicardAggregationSamplePk)) {
                return false;
            }

            PicardAggregationSamplePk that = OrmUtil.proxySafeCast(o, PicardAggregationSamplePk.class);

            return new EqualsBuilder()
                .append(getProject(), that.getProject())
                .append(getSample(), that.getSample())
                .append(getDataType(), that.getDataType())
                .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 37)
                .append(getProject())
                .append(getSample())
                .append(getDataType())
                .toHashCode();
        }
    }
}

