package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a run of an instrument that generates metrics, e.g. Eco or Viaa7
 */
@Entity
@Audited
@Table(schema = "mercury",
        uniqueConstraints = @UniqueConstraint(name = "UK_LMR_NAME_TYPE", columnNames = {"runName", "runDate"}))
public class LabMetricRun {

    @SuppressWarnings("UnusedDeclaration")
    @SequenceGenerator(name = "SEQ_LAB_METRIC_RUN", schema = "mercury", sequenceName = "SEQ_LAB_METRIC_RUN")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_LAB_METRIC_RUN")
    @Id
    private Long labMetricRunId;

    /** Name of the run, often the LCSET name */
    private String runName;
    /** When the run started */
    private Date runDate;
    /** The type of metric generated by the run */
    @Enumerated(EnumType.STRING)
    private LabMetric.MetricType metricType;

    @OneToMany(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY, mappedBy = "labMetricRun")
    private Set<LabMetric> labMetrics = new HashSet<LabMetric>();

    public LabMetricRun(String runName, Date runDate, LabMetric.MetricType metricType) {
        this.runName = runName;
        this.runDate = runDate;
        this.metricType = metricType;
    }

    /** For JPA */
    protected LabMetricRun() {
    }

    public void addMetric(LabMetric labMetric) {
        this.labMetrics.add(labMetric);
        labMetric.setLabMetricRun(this);
    }


    public String getRunName() {
        return runName;
    }

    public Date getRunDate() {
        return runDate;
    }

    public LabMetric.MetricType getMetricType() {
        return metricType;
    }

    public Set<LabMetric> getLabMetrics() {
        return labMetrics;
    }
}
