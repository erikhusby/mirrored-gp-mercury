/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2016 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure.metrics.entity;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "PICARD_FINGERPRINT", schema = "METRICS")
public class PicardFingerprint {
    @Id
    @Column(name = "PICARD_ANALYSIS_ID")
    private Long picardAnalysisId;

    @Column(name = "LOD_EXPECTED_SAMPLE")
    private double lodExpectedSample;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(insertable = false, updatable = false, name = "PICARD_ANALYSIS_ID", referencedColumnName = "ID")
    private PicardAnalysis picardAnalysis;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || (!OrmUtil.proxySafeIsInstance(o, PicardFingerprint.class))) {
            return false;
        }

        if (!(o instanceof PicardFingerprint)) {
            return false;
        }

        PicardFingerprint that = OrmUtil.proxySafeCast(o, PicardFingerprint.class);

        return new EqualsBuilder()
                .append(lodExpectedSample, that.lodExpectedSample)
                .append(picardAnalysisId, that.picardAnalysisId)
                .append(picardAnalysis, that.picardAnalysis)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(picardAnalysisId)
                .append(lodExpectedSample)
                .append(picardAnalysis)
                .toHashCode();
    }
}

