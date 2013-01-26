package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a measurement of the contents of a LabVessel, e.g. Pico quantification.
 */
@Entity
@Audited
@Table(schema = "mercury")
public class LabMetric {

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

    public enum MetricType {
//        FRAGMENT_SIZE,
//        VOLUME,
        BSP_PICO("BSP Pico"),
        PRE_FLIGHT_PRE_NORM_PICO("Pre Flight Pre Norm Pico"),
        PRE_FLIGHT_POST_NORM_PICO("Pre Flight Post Norm Pico"),
        POND_PICO("Pond Pico"),
        CATCH_PICO("Catch Pico"),
        FINAL_LIBRARY_SIZE("Final Library Size"),
        POST_NORMALIZATION_PICO("Post-Normalization Pico"),
        TSCA_PICO("TSCA Pico");

        private String displayName;
        private static final Map<String, MetricType> mapNameToType = new HashMap<String, MetricType>();
        static {
            for (MetricType metricType : MetricType.values()) {
                mapNameToType.put(metricType.getDisplayName(), metricType);
            }
        }

        MetricType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static MetricType getByDisplayName(String displayName) {
            MetricType mappedMetricType = mapNameToType.get(displayName);
            if(mappedMetricType == null) {
                throw new RuntimeException("Failed to find MetricType for name " + displayName);
            }
            return mappedMetricType;
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    @SequenceGenerator(name = "SEQ_LAB_METRIC", schema = "mercury", sequenceName = "SEQ_LAB_METRIC")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_LAB_METRIC")
    @Id
    private Long labMetricId;

    /** The run that generated this metric */
    @ManyToOne
    private LabMetricRun labMetricRun;

    /** The value of the metric */
    private BigDecimal value;

    /** The type of the value.  This could be in LabMetricRun, rather than denormalized here, but having it here allows
     * for metrics that are generated without runs */
    private MetricType metricType;

    /** The unit of the value */
    private LabUnit labUnit;

    @ManyToOne
    private LabVessel labVessel;

    /** For JPA */
    protected LabMetric() {
    }

    public LabMetric(BigDecimal value, MetricType metricType, LabUnit labUnit) {
        this.value = value;
        this.metricType = metricType;
        this.labUnit = labUnit;
    }

    public BigDecimal getValue() {
        return value;
    }

    public MetricType getName() {
        return metricType;
    }

    public LabUnit getUnits() {
        return labUnit;
    }

    public LabMetricRun getLabMetricRun() {
        return labMetricRun;
    }

    public void setLabMetricRun(LabMetricRun labMetricRun) {
        this.labMetricRun = labMetricRun;
    }

    /*
    public Float convertTo(LabUnit otherUnit);

    public boolean isInRange(LabMetricRange range);
*/
}
