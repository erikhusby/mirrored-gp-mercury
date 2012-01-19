package org.broadinstitute.sequel;

/**
 * Different lab steps in different workflows
 * have expected metric ranges.
 *
 * People also want to query on metric ranges.
 */
public interface LabMetricRange {

    public Enum getUnits();

    public LabMetric.MetricName getMetricName();

    public Float getMin();

    public Float getMax();
}
