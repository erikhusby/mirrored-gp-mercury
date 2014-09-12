package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import clover.org.apache.commons.collections.comparators.NullComparator;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a measurement of the contents of a LabVessel, e.g. Pico quantification.
 */
@Entity
@Audited
@Table(schema = "mercury")
public class LabMetric implements Comparable<LabMetric> {

    public enum LabUnit {
        NG_PER_UL("ng/uL"),
        UG_PER_ML("ug/mL"),
        UG("ug"),
        MG("mg"),
        ML("mL"),
        KBp("KBp"),
        MBp("KBp"),
        GBp("GBp"),
        Bp("Bp");

        private String displayName;
        private static final Map<String, LabUnit> mapNameToUnit = new HashMap<>();

        static {
            for (LabUnit unit : LabUnit.values()) {
                mapNameToUnit.put(unit.getDisplayName(), unit);
            }
        }

        LabUnit(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static LabUnit getByDisplayName(String displayName) {
            LabUnit mappedUnit = mapNameToUnit.get(displayName);
            if (mappedUnit == null) {
                throw new RuntimeException("Failed to find LabUnit for name " + displayName);
            }
            return mappedUnit;
        }
    }

    public enum MetricType {
        BSP_PICO("BSP Pico", false),
        PRE_FLIGHT_PRE_NORM_PICO("Pre Flight Pre Norm Pico", false),
        PRE_FLIGHT_POST_NORM_PICO("Pre Flight Post Norm Pico", false),
        POND_PICO("Pond Pico", true),
        CATCH_PICO("Catch Pico", true),
        FINAL_LIBRARY_SIZE("Final Library Size", false),
        POST_NORMALIZATION_PICO("Post-Normalization Pico", false),
        TSCA_PICO("TSCA Pico", false),
        ECO_QPCR("ECO QPCR", true);

        private String displayName;
        private boolean uploadEnabled;
        private static final Map<String, MetricType> mapNameToType = new HashMap<>();

        static {
            for (MetricType metricType : MetricType.values()) {
                mapNameToType.put(metricType.getDisplayName(), metricType);
            }
        }

        MetricType(String displayName, boolean uploadEnabled) {
            this.displayName = displayName;
            this.uploadEnabled = uploadEnabled;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static MetricType getByDisplayName(String displayName) {
            MetricType mappedMetricType = mapNameToType.get(displayName);
            if (mappedMetricType == null) {
                throw new RuntimeException("Failed to find MetricType for name " + displayName);
            }
            return mappedMetricType;
        }

        public static List<MetricType> getUploadSupportedMetrics() {
            List<MetricType> metricTypes = new ArrayList<>();
            for (MetricType value : values()) {
                if (value.uploadEnabled) {
                    metricTypes.add(value);
                }
            }

            return metricTypes;
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    @SequenceGenerator(name = "SEQ_LAB_METRIC", schema = "mercury", sequenceName = "SEQ_LAB_METRIC")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_LAB_METRIC")
    @Id
    private Long labMetricId;

    /**
     * The run that generated this metric
     */
    @ManyToOne
    private LabMetricRun labMetricRun;

    /**
     * The value of the metric
     */
    private BigDecimal value;

    /**
     * The type of the value.  This could be in LabMetricRun, rather than denormalized here, but having it here allows
     * for metrics that are generated without runs
     */
    @Enumerated(EnumType.STRING)
    private MetricType metricType;

    /**
     * The unit of the value
     */
    @Enumerated(EnumType.STRING)
    private LabUnit labUnit;

    @ManyToOne(fetch = FetchType.LAZY)
    private LabVessel labVessel;


    private String vesselPosition;

    private Date createdDate;

    /**
     * For JPA
     */
    protected LabMetric() {
    }

    public LabMetric(BigDecimal value, MetricType metricType, LabUnit labUnit, String vesselPosition,
                     Date createdDate) {
        this.value = value;
        this.metricType = metricType;
        this.labUnit = labUnit;
        this.vesselPosition = vesselPosition;
        this.createdDate = createdDate;
    }

    public Long getLabMetricId() {
        return labMetricId;
    }

    public BigDecimal getValue() {
        return value;
    }

    /**
     * For fixups only.
     */
    public void setValue(BigDecimal value) {
        this.value = value;
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

    public LabVessel getLabVessel() {
        return labVessel;
    }

    public void setLabVessel(LabVessel labVessel) {
        this.labVessel = labVessel;
    }

    public String getVesselPosition() {
        return vesselPosition;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    @Override
    public int compareTo(LabMetric labMetric) {
        CompareToBuilder compareToBuilder = new CompareToBuilder();
        if (getCreatedDate() != null) {
            compareToBuilder.append(getCreatedDate(), labMetric.getCreatedDate());
        } else {
            compareToBuilder.append(getLabMetricId(), labMetric.getLabMetricId());
        }
        return compareToBuilder.build();
    }

    public static class LabMetricRunDateComparator
            implements Comparator<LabMetric> {
        @Override
        public int compare(LabMetric labMetric1, LabMetric labMetric2) {
            return labMetric2.getLabMetricRun().getRunDate()
                    .compareTo(labMetric1.getLabMetricRun().getRunDate());
        }
    }
}
