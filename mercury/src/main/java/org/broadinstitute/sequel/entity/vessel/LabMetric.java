package org.broadinstitute.sequel.entity.vessel;

/**
 * The lab wants to add various metrics to things
 * on-the-fly, with minimal overhead.
 *
 * I think this goes way overboard, though.  How
 * do we make metrics type safe, easy to convert
 * between units (ng/ml vs. ug/ml), and easy
 * to maintain?
 */
public interface LabMetric {

    public enum LabUnit {
        NG_PER_ML,
        UG_PER_ML,
        UG,
        MG,
        ML,
        KBp,
        MBp,
        GBp,
        BPp
    }

    public enum MetricName {
        PREFLIGHT_QUANT,
        BSP_QUANT,
        POST_NORM_QUANT,
        FRAGMENT_SIZE,
        VOLUME,
    }

    public Float getValue();

    public MetricName getName();

    public LabUnit getUnits();
    
    public Float convertTo(LabUnit otherUnit);

    public boolean isInRange(LabMetricRange range);
}
