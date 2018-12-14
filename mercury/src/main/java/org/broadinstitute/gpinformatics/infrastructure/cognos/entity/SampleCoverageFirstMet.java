package org.broadinstitute.gpinformatics.infrastructure.cognos.entity;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Date;

/**
 */
@Entity
@Table(schema = "COGNOS", name = "SLXRE_SAMPLE_CVRG_FIRST_MET")
public class SampleCoverageFirstMet {

    @Embeddable
    public static class SampleCoverageFirstMetId implements Serializable {
        private String pdoName;
        private String externalSampleId;

        @Override
        public boolean equals( Object obj){

            if (obj == null) { return false; }
            if (obj == this) { return true; }
            if (obj.getClass() != getClass()) {
                return false;
            }
            SampleCoverageFirstMetId rhs = (SampleCoverageFirstMetId) obj;
            return new EqualsBuilder()
                    .appendSuper(super.equals(obj))
                    .append(pdoName, rhs.pdoName)
                    .append(externalSampleId, rhs.externalSampleId)
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 37).
                    append(pdoName).
                    append(externalSampleId).
                    toHashCode();
        }
    }

    @EmbeddedId
    private SampleCoverageFirstMetId id;

    private String productPartNumber;

    private String aggregationProject;

    private Date dcfm;

    public String getPdoName() {
        return id.pdoName;
    }

    public String getExternalSampleId() {
        return id.externalSampleId;
    }

    public String getProductPartNumber() {
        return productPartNumber;
    }

    public String getAggregationProject() {
        return aggregationProject;
    }

    public Date getDcfm() {
        return dcfm;
    }
}

