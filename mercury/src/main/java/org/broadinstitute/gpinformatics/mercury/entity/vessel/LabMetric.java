package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.hibernate.envers.Audited;

import javax.annotation.Nullable;
import javax.persistence.CascadeType;
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

    public interface Decider {
        LabMetricDecision.Decision makeDecision(LabVessel labVessel, LabMetric labMetric);
    }

    public enum MetricType {
        INITIAL_PICO("Initial Pico", true, new Decider() {
            @Override
            public LabMetricDecision.Decision makeDecision(LabVessel labVessel, LabMetric labMetric) {
                if (labVessel.getVolume() != null) {
                    if (labMetric.getValue().multiply(labVessel.getVolume()).compareTo(new BigDecimal("250")) == 1) {
                        return LabMetricDecision.Decision.PASS;
                    }
                }
                return LabMetricDecision.Decision.FAIL;
            }
        }),
        FINGERPRINT_PICO("Fingerprint Pico", true, new Decider() {
            @Override
            public LabMetricDecision.Decision makeDecision(LabVessel labVessel, LabMetric labMetric) {
                if (labMetric.getValue().compareTo(new BigDecimal("9.99")) == 1 &&
                        labMetric.getValue().compareTo(new BigDecimal("60.01")) == -1) {
                    return LabMetricDecision.Decision.PASS;
                }
                return LabMetricDecision.Decision.FAIL;
            }
        }),
        SHEARING_PICO("Shearing Pico", true, new Decider() {
            @Override
            public LabMetricDecision.Decision makeDecision(LabVessel labVessel, LabMetric labMetric) {
                if (labMetric.getValue().compareTo(new BigDecimal("1.49")) == 1 &&
                        labMetric.getValue().compareTo(new BigDecimal("5.01")) == -1) {
                    return LabMetricDecision.Decision.PASS;
                }
                return LabMetricDecision.Decision.FAIL;
            }
        }),
        POND_PICO("Pond Pico", true, new Decider() {
            @Override
            public LabMetricDecision.Decision makeDecision(LabVessel labVessel, LabMetric labMetric) {
                if (labMetric.getValue().compareTo(new BigDecimal("25")) == 1) {
                    return LabMetricDecision.Decision.PASS;
                }
                return LabMetricDecision.Decision.FAIL;
            }
        }),
        CATCH_PICO("Catch Pico", true, new Decider() {
            @Override
            public LabMetricDecision.Decision makeDecision(LabVessel labVessel, LabMetric labMetric) {
                if (labMetric.getValue().compareTo(new BigDecimal("2")) == 1) {
                    return LabMetricDecision.Decision.PASS;
                }
                return LabMetricDecision.Decision.FAIL;
            }
        }),
        FINAL_LIBRARY_SIZE("Final Library Size", false, null),
        ECO_QPCR("ECO QPCR", true, null);

        private String displayName;
        private boolean uploadEnabled;
        private static final Map<String, MetricType> mapNameToType = new HashMap<>();
        private Decider decider;

        static {
            for (MetricType metricType : MetricType.values()) {
                mapNameToType.put(metricType.getDisplayName(), metricType);
            }
        }

        MetricType(String displayName, boolean uploadEnabled, Decider decider) {
            this.displayName = displayName;
            this.uploadEnabled = uploadEnabled;
            this.decider = decider;
        }

        public String getDisplayName() {
            return displayName;
        }

        @Nullable
        public Decider getDecider() {
            return decider;
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

    //todo jmt convert to enum?
    private String vesselPosition;

    private Date createdDate;

    /** This is actually OneToOne, but using ManyToOne to avoid N+1 selects */
    @ManyToOne(cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)
    private LabMetricDecision labMetricDecision;

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

    public BigDecimal getTotalNg() {
        if (labVessel.getVolume() != null) {
            return value.multiply(labVessel.getVolume());
        }
        return null;
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

    public LabMetricDecision getLabMetricDecision() {
        return labMetricDecision;
    }

    public void setLabMetricDecision(LabMetricDecision labMetricDecision) {
        this.labMetricDecision = labMetricDecision;
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

    /** These define the concentration range in ug/ml (ng/ul) for acceptable fingerprinting. */
    public static final BigDecimal INITIAL_PICO_LOW_THRESHOLD = new BigDecimal("3.4");
    public static final BigDecimal INITIAL_PICO_HIGH_THRESHOLD = new BigDecimal("60.0");

    /**
     * Determines initial pico disposition from the sample's quant.
     *
     * @return  -1, 0, or +1 indicating that concentration is
     *          below range, in range, or above range, respectively.
     */
    public int initialPicoDispositionRange() {
        if (value == null || value.compareTo(INITIAL_PICO_LOW_THRESHOLD) < 0) {
            return -1;
        } else if (value.compareTo(INITIAL_PICO_HIGH_THRESHOLD) > 0) {
            return 1;
        }
        return 0;
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
