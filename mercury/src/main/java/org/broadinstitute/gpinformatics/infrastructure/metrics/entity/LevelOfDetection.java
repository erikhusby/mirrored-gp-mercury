/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2014 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure.metrics.entity;

import javax.annotation.Nonnull;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityResult;
import javax.persistence.Id;
import javax.persistence.NamedNativeQuery;
import javax.persistence.SqlResultSetMapping;
import java.io.Serializable;

@Entity
@SqlResultSetMapping(name = "implicit", entities = @EntityResult(entityClass = LevelOfDetection.class))
@NamedNativeQuery(name = LevelOfDetection.LOD_QUERY_NAME, query = LevelOfDetection.LOD_QUERY,
        resultSetMapping = "implicit")
public class LevelOfDetection implements Serializable {
    public static final String LOD_QUERY_NAME = "lodQuery";
    public static final String LOD_QUERY =
            "SELECT min(fp.LOD_EXPECTED_SAMPLE) AS min, max(fp.LOD_EXPECTED_SAMPLE) AS max, a.sample, a.project,a.version  FROM METRICS.aggregation a "
            + "JOIN METRICS.AGGREGATION_READ_GROUP r ON r.AGGREGATION_ID = a.id "
            + "JOIN METRICS.PICARD_ANALYSIS p ON p.FLOWCELL_BARCODE = r.FLOWCELL_BARCODE AND p.LANE = r.LANE AND p.LIBRARY_NAME = r.LIBRARY_NAME "
            + "JOIN METRICS.PICARD_FINGERPRINT fp ON fp.PICARD_ANALYSIS_ID = p.id "
            + "WHERE a.project=:project AND a.sample=:sample AND a.version=:version AND a.library IS NULL GROUP BY a.sample, a.project, a.version";

    @Id
    private String project;
    @Id
    private String sample;
    @Id
    @Column(name="version")
    private Integer version;

    private Double min;
    private Double max;

    public LevelOfDetection(@Nonnull Double min, @Nonnull Double max) {
        this(null, null, null, min, max);
    }

    public LevelOfDetection(String project, String sample, Integer version, @Nonnull Double min, @Nonnull Double max) {
        if (min > max) {
            throw new IllegalStateException(
                    String.format("value of min(%f) can not be larger than that of max(%f)", min, max));
        }

        this.project = project;
        this.sample = sample;
        this.version = version;
        this.min = min;
        this.max = max;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getSample() {
        return sample;
    }

    public void setSample(String sample) {
        this.sample = sample;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public void setMin(Double min) {
        this.min = min;
    }

    public void setMax(Double max) {
        this.max = max;
    }

    public LevelOfDetection() {
    }

    public Double getMin() {
        return min;
    }

    public Double getMax() {
        return max;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LevelOfDetection)) {
            return false;
        }

        LevelOfDetection that = (LevelOfDetection) o;

        if (max != null ? !max.equals(that.max) : that.max != null) {
            return false;
        }
        if (min != null ? !min.equals(that.min) : that.min != null) {
            return false;
        }
        if (project != null ? !project.equals(that.project) : that.project != null) {
            return false;
        }
        if (sample != null ? !sample.equals(that.sample) : that.sample != null) {
            return false;
        }
        return !(version != null ? !version.equals(that.version) : that.version != null);

    }

    @Override
    public int hashCode() {
        int result = project != null ? project.hashCode() : 0;
        result = 31 * result + (sample != null ? sample.hashCode() : 0);
        result = 31 * result + (version != null ? version.hashCode() : 0);
        result = 31 * result + (min != null ? min.hashCode() : 0);
        result = 31 * result + (max != null ? max.hashCode() : 0);
        return result;
    }

    public String displayString() {
        return String.format("%2.2f/%2.2f", min, max);
    }
}
