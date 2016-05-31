package org.broadinstitute.gpinformatics.infrastructure.cognos.entity;

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

