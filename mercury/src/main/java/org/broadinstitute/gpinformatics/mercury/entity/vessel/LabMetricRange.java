package org.broadinstitute.gpinformatics.mercury.entity.vessel;

/**
 * Different lab steps in different workflows
 * have expected metric ranges.
 *
 * People also want to query on metric ranges.
 */
public interface LabMetricRange {

    public Enum getUnits();

    public LabMetric.MetricType getMetricName();

    public Float getMin();

    public Float getMax();
}
