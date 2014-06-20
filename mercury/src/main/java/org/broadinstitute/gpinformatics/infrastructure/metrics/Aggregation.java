package org.broadinstitute.gpinformatics.infrastructure.metrics;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.SecondaryTable;
import javax.persistence.Table;

/**
 * An aggregation, including metrics, on: project sample version [dataType]. dataType only applies for aggregations by
 * Mercury research project.
 */
@Entity
@Table(name = "AGGREGATION", schema = "METRICS")
@SecondaryTable(name = "AGGREGATION_CONTAM", schema = "METRICS",
        pkJoinColumns = @PrimaryKeyJoinColumn(name = "AGGREGATION_ID"))
public class Aggregation {

    @Id
    private long id;

    /*
     * Project, sample, version, and (for aggregations by Mercury research project) dataType are a unique key (for rows
     * where library is null). However, it doesn't seem worthwhile to define this in metadata for this read-only entity,
     * especially given the wrinkle of the library attribute.
     */

    private String project;
    private String sample;
    private int version;
    private String dataType;

    /**
     * Library is not needed by Mercury, but does need to be mapped here to allow queries to perform null checks.
     */
    private String library;

    @Column(table = "AGGREGATION_CONTAM", name = "PCT_CONTAMINATION")
    private Double percentContamination;

    public long getId() {
        return id;
    }

    public String getProject() {
        return project;
    }

    public String getSample() {
        return sample;
    }

    public int getVersion() {
        return version;
    }

    public String getDataType() {
        return dataType;
    }

    public Double getPercentContamination() {
        return percentContamination;
    }
}
