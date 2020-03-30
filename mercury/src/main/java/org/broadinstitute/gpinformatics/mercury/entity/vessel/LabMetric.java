package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.common.MathUtils;
import org.broadinstitute.gpinformatics.mercury.control.vessel.PicoTripleReworkEval;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.hibernate.envers.Audited;

import javax.annotation.Nullable;
import javax.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
        Bp("Bp"),
        RQS("Rqs"),
        PERCENTAGE("%"),
        GENDER("XX/XY"),
        NUMBER(""),
        UL("uL"),
        NM("nM");

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

    public abstract static class Decider {
        public abstract LabMetricDecision makeDecision(LabVessel labVessel, LabMetric labMetric, long decidingUser);

        /**
         * Enhanced Decider logic: <br/>
         * Using multiple reads, builds a LabMetric, associates it with a sample vessel (tube) and attaches a Decision and ReworkDisposition
         *
         * @param tube           The sample tube associated with the multiple reads to attach the LabMetric->Decision->ReworkDisposition to.
         * @param tubePosition   To attach to LabMetric (simple String)
         * @param runStarted     Date of LabMetric
         * @param concList       List of measurements from the multiple wells transferred from a tube
         * @param metricType     For LabMetric (or other logic)
         * @param decidingUser   For LabMetric
         * @param maxPercentDiff Max allowable difference between the multiple reads - triggers reworks (optional, can be hardcoded)
         */
        public Optional<LabMetric> makeCurveFit2Decision(LabVessel tube, String tubePosition, Date runStarted, List<BigDecimal> concList,
                                                         LabMetric.MetricType metricType, Long decidingUser, float maxPercentDiff) {
            return Optional.empty();
        }
    }

    public enum MetricType {
        INITIAL_PICO("Initial Pico", true, Category.CONCENTRATION, LabUnit.NG_PER_UL, new Decider() {
            /**
             * Fine grained logic is performed in makeCurveFit2Decision(), this only validates a CRSP requirement
             * of 250ng minimum DNA
             */
            @Override
            public LabMetricDecision makeDecision(LabVessel labVessel, LabMetric labMetric, long decidingUser) {
                // There may already be a metricDecision and we've got to overwrite it and keep disposition and note if so
                LabMetricDecision metricDecision = labMetric.getLabMetricDecision();

                boolean wasFailure = false;
                boolean isClinical = false;
                String note = null;

                if (labVessel.getVolume() != null) {
                    BigDecimal totalNg = labMetric.getValue().multiply(labVessel.getVolume());
                    if (totalNg.compareTo(new BigDecimal("250")) < 0 ) {
                        note = "Total Ng " + MathUtils.scaleTwoDecimalPlaces(totalNg) + " less than 250";
                        wasFailure = true;
                    }
                } else {
                    note = "No volume recorded for vessel " + labVessel.getLabel();
                    wasFailure = true;
                }

                if (!wasFailure) {
                    // No failure - simply return whatever was already attached to metric
                    return metricDecision;
                }

                // check if clinical
                for (SampleInstanceV2 si : labVessel.getSampleInstancesV2()) {
                    if (si.getSingleProductOrderSample() != null) {
                        ResearchProject project = si.getSingleProductOrderSample().getProductOrder().getResearchProject();
                        if (project != null && (
                                ResearchProject.RegulatoryDesignation.CLINICAL_DIAGNOSTICS.equals(project.getRegulatoryDesignation())
                                        || ResearchProject.RegulatoryDesignation.GENERAL_CLIA_CAP.equals(project.getRegulatoryDesignation()))) {
                            isClinical = true;
                            break;
                        }
                    }
                }

                if (!isClinical) {
                    // Non-clinical - Ignore criteria and simply return whatever was already attached to metric
                    return metricDecision;
                }

                if (metricDecision == null) {
                    metricDecision = new LabMetricDecision(LabMetricDecision.Decision.FAIL_ACCEPTANCE_CRITERIA, new Date(), decidingUser, labMetric);
                    metricDecision.setNote(note);
                } else {
                    // Don't overwrite rework disposition or note
                    metricDecision.setDecision(LabMetricDecision.Decision.FAIL_ACCEPTANCE_CRITERIA);
                    String existingNote = metricDecision.getNote();
                    metricDecision.setNote(existingNote == null ? note : note + "  " + existingNote);
                }

                return metricDecision;
            }

            @Override
            public Optional<LabMetric> makeCurveFit2Decision(LabVessel tube, String tubePosition, Date runStarted, List<BigDecimal> concList,
                                                             LabMetric.MetricType metricType, Long decidingUser, float maxPercentDiff) {
                return PicoTripleReworkEval.evalBadTriplicate(tube, tubePosition, runStarted, concList,
                        metricType, decidingUser, maxPercentDiff);
            }
        }),
        INITIAL_RIBO("Initial Ribo", true, Category.CONCENTRATION,LabUnit.NG_PER_UL, new Decider() {
            @Override
            public LabMetricDecision makeDecision(LabVessel labVessel, LabMetric labMetric, long decidingUser) {
                LabMetricDecision decision = null;
                if (labVessel.getVolume() != null) {
                    BigDecimal totalNg = labMetric.getValue().multiply(labVessel.getVolume());
                    if (totalNg.compareTo(new BigDecimal("250")) == 1) {
                        decision = new LabMetricDecision(LabMetricDecision.Decision.PASS, new Date(), decidingUser, labMetric);
                    } else {
                        decision = new LabMetricDecision(LabMetricDecision.Decision.FAIL, new Date(), decidingUser, labMetric);
                        decision.setNote("Total Ng " + MathUtils.scaleTwoDecimalPlaces(totalNg) + " is less than 250");
                    }
                } else {
                    decision = new LabMetricDecision(LabMetricDecision.Decision.FAIL, new Date(), decidingUser, labMetric);
                    decision.setNote("No volume recorded for vessel " + labVessel.getLabel());
                }
                return decision;
            }
        }),
        FINGERPRINT_PICO("Fingerprint Pico", true, Category.CONCENTRATION, LabUnit.NG_PER_UL, new Decider() {
            @Override
            public LabMetricDecision makeDecision(LabVessel labVessel, LabMetric labMetric, long decidingUser) {
                LabMetricDecision decision = null;
                if (labMetric.getValue().compareTo(new BigDecimal("9.99")) == 1 &&
                        labMetric.getValue().compareTo(new BigDecimal("60.01")) == -1) {
                    decision = new LabMetricDecision(LabMetricDecision.Decision.PASS, new Date(), decidingUser, labMetric);
                } else {
                    decision = new LabMetricDecision(LabMetricDecision.Decision.FAIL, new Date(), decidingUser, labMetric);
                    decision.setNote("Concentration " + MathUtils.scaleTwoDecimalPlaces(labMetric.getValue()) + " is not between 9.99 and 60.01.");
                }
                return decision;
            }
        }),
        PLATING_PICO("Plating Pico", true, Category.CONCENTRATION, LabUnit.NG_PER_UL, new Decider() {
            @Override
            public LabMetricDecision makeDecision(LabVessel labVessel, LabMetric labMetric, long decidingUser) {
                // There may already be a metricDecision and we've got to overwrite it and keep disposition and note if so
                LabMetricDecision metricDecision = labMetric.getLabMetricDecision();
                LabMetricDecision.Decision decision = null;
                String note = null;

                if (labMetric.getValue().compareTo(new BigDecimal("5.5")) == 1) {
                    decision = LabMetricDecision.Decision.PASS;
                } else {
                    decision = LabMetricDecision.Decision.FAIL;
                    note = "Concentration " + MathUtils.scaleTwoDecimalPlaces(labMetric.getValue()) + " is less than 5.5 " + LabUnit.NG_PER_UL.getDisplayName();
                }

                if (metricDecision == null) {
                    metricDecision = new LabMetricDecision(decision, new Date(), decidingUser, labMetric);
                    metricDecision.setNote(note);
                } else {
                    // Don't overwrite rework disposition or note
                    if (decision == LabMetricDecision.Decision.FAIL) {
                        metricDecision.setDecision(decision);
                        String existingNote = metricDecision.getNote();
                        metricDecision.setNote(existingNote == null ? note : note + "  " + existingNote);
                    }
                }

                return metricDecision;
            }

            @Override
            public Optional<LabMetric> makeCurveFit2Decision(LabVessel tube, String tubePosition, Date runStarted, List<BigDecimal> concList,
                                                             LabMetric.MetricType metricType, Long decidingUser, float maxPercentDiff) {
                return PicoTripleReworkEval.evalBadTriplicate(tube, tubePosition, runStarted, concList,
                        metricType, decidingUser, maxPercentDiff);
            }
        }),
        SHEARING_PICO("Shearing Pico", true, Category.CONCENTRATION, LabUnit.NG_PER_UL, new Decider() {
            @Override
            public LabMetricDecision makeDecision(LabVessel labVessel, LabMetric labMetric, long decidingUser) {
                LabMetricDecision decision;
                if (labMetric.getValue().compareTo(new BigDecimal("1.49")) == 1 &&
                        labMetric.getValue().compareTo(new BigDecimal("5.01")) == -1) {
                    decision = new LabMetricDecision(LabMetricDecision.Decision.PASS, new Date(), decidingUser, labMetric);
                } else {
                    decision = new LabMetricDecision(LabMetricDecision.Decision.FAIL, new Date(), decidingUser, labMetric);
                    decision.setNote("Concentration " + MathUtils.scaleTwoDecimalPlaces(labMetric.getValue()) + " is not between 1.49 and 5.01 " + LabUnit.NG_PER_UL.getDisplayName());
                }
                return decision;
            }
        }),
        PLATING_RIBO("Plating Ribo", true, Category.CONCENTRATION, LabUnit.NG_PER_UL, new Decider() {
            @Override
            public LabMetricDecision makeDecision(LabVessel labVessel, LabMetric labMetric, long decidingUser) {
                LabMetricDecision decision;
                if (labMetric.getValue().compareTo(new BigDecimal("3")) == 1) {
                    decision = new LabMetricDecision(LabMetricDecision.Decision.PASS, new Date(), decidingUser, labMetric);
                } else {
                    decision = new LabMetricDecision(LabMetricDecision.Decision.FAIL, new Date(), decidingUser, labMetric);
                    decision.setNote("Concentration " + MathUtils.scaleTwoDecimalPlaces(labMetric.getValue()) + " is less than 3 " + LabUnit.NG_PER_UL.getDisplayName());
                }
                return decision;
            }
        }),
        POND_PICO("Pond Pico", true, Category.CONCENTRATION, LabUnit.NG_PER_UL, new Decider() {
            @Override
            public LabMetricDecision makeDecision(LabVessel labVessel, LabMetric labMetric, long decidingUser) {
                LabMetricDecision decision;
                if (labMetric.getValue().compareTo(new BigDecimal("25")) == 1) {
                    decision = new LabMetricDecision(LabMetricDecision.Decision.PASS, new Date(), decidingUser, labMetric);
                } else {
                    decision = new LabMetricDecision(LabMetricDecision.Decision.FAIL, new Date(), decidingUser, labMetric);
                    decision.setNote("Concentration " + MathUtils.scaleTwoDecimalPlaces(labMetric.getValue()) + " is less than 25 " + LabUnit.NG_PER_UL.getDisplayName());
                }
                return decision;
            }
        }),
        CDNA_ENRICHED_PICO("cDNA Enriched Pico", true, Category.CONCENTRATION, LabUnit.NG_PER_UL, new Decider() {
            @Override
            public LabMetricDecision makeDecision(LabVessel labVessel, LabMetric labMetric, long decidingUser) {
                LabMetricDecision decision;
                if (labMetric.getValue().compareTo(new BigDecimal("5")) == 1) {
                    decision = new LabMetricDecision(LabMetricDecision.Decision.PASS, new Date(), decidingUser, labMetric);
                } else {
                    decision = new LabMetricDecision(LabMetricDecision.Decision.FAIL, new Date(), decidingUser, labMetric);
                    decision.setNote("Concentration " + MathUtils.scaleTwoDecimalPlaces(labMetric.getValue()) + " is less than 5 " + LabUnit.NG_PER_UL.getDisplayName());
                }
                return decision;
            }
        }),
        CATCH_PICO("Catch Pico", true, Category.CONCENTRATION, LabUnit.NG_PER_UL, new Decider() {
            @Override
            public LabMetricDecision makeDecision(LabVessel labVessel, LabMetric labMetric, long decidingUser) {
                LabMetricDecision decision;
                if (labMetric.getValue().compareTo(new BigDecimal("2")) == 1) {
                    decision = new LabMetricDecision(LabMetricDecision.Decision.PASS, new Date(), decidingUser, labMetric);
                } else {
                    decision = new LabMetricDecision(LabMetricDecision.Decision.FAIL, new Date(), decidingUser, labMetric);
                    decision.setNote("Concentration " + MathUtils.scaleTwoDecimalPlaces(labMetric.getValue()) + " is less than 2 " + LabUnit.NG_PER_UL.getDisplayName());
                }
                return decision;
            }
        }),
        FINAL_LIBRARY_SIZE("Final Library Size", false, Category.DNA_LENGTH, LabUnit.Bp, null),
        ECO_QPCR("ECO QPCR", true, Category.CONCENTRATION, LabUnit.NM, null),
        VIIA_QPCR("VIIA QPCR", true, Category.CONCENTRATION, LabUnit.NM, null),
        VVP_VOLUME("VVP Volume", false, Category.VOLUME, LabUnit.UL, null),
        XL20_VOLUME("XL20 Volume", false, Category.VOLUME, LabUnit.UL, null),
        INITIAL_RNA_CALIPER("Initial RNA Caliper", true, Category.QUALITY, LabUnit.NG_PER_UL, new Decider() {
            @Override
            public LabMetricDecision makeDecision(LabVessel labVessel, LabMetric labMetric, long decidingUser) {
                LabMetricDecision.Decision decision = LabMetricDecision.Decision.PASS;
                String decisionNote = null;
                LabMetricDecision.NeedsReview needsReview = LabMetricDecision.NeedsReview.FALSE;
                for (Metadata metadata : labMetric.getMetadataSet()) {
                    if (metadata.getKey() == Metadata.Key.DV_200) {
                        if(metadata.getNumberValue().compareTo(BigDecimal.ZERO) <= 0 ||
                           metadata.getNumberValue().compareTo(BigDecimal.ONE) >= 0 ) {
                            decision = LabMetricDecision.Decision.REPEAT;
                            decisionNote = "DV200 not in accepted 0-1 range.";
                        }
                    }else if(metadata.getKey() == Metadata.Key.LOWER_MARKER_TIME) {
                        if(metadata.getNumberValue().compareTo(BigDecimal.valueOf(28)) < 0 ||
                           metadata.getNumberValue().compareTo(BigDecimal.valueOf(33)) > 0 ) {
                            decisionNote = "Lower Marker Time not in accepted 28-33 range.";
                            decision = LabMetricDecision.Decision.REPEAT;
                        }
                    }else if(metadata.getKey() == Metadata.Key.NA) {
                        if (metadata.getValue().equals(String.valueOf(Boolean.TRUE))) {
                            decisionNote = "NA";
                            decision = LabMetricDecision.Decision.REPEAT;
                            needsReview = LabMetricDecision.NeedsReview.TRUE;
                        }
                    }
                }

                return new LabMetricDecision(decision, new Date(), decidingUser, labMetric, decisionNote, needsReview);
            }
        }),

        // Fingerprinting Metrics
        FLUIDIGM_FINGERPRINTING("Fluidigm Fingerprinting", false, Category.QUALITY, LabUnit.NUMBER, null),
        FLUIDIGM_GENDER("Fluidigm Gender", false, Category.PERCENTAGE, LabUnit.GENDER, null),
        AUTOCALL_CONFIDENCE("Autocall Confidence", false, Category.DNA_LENGTH, LabUnit.NUMBER, null),
        HAPMAP_CONCORDANCE_LOD("HapMap Concordance LOD", false, Category.QUALITY, LabUnit.NUMBER, new Decider() {
            @Override
            public LabMetricDecision makeDecision(LabVessel labVessel, LabMetric labMetric, long decidingUser) {
                return new LabMetricDecision(labMetric.getValue().doubleValue() > 3.0 ?
                        LabMetricDecision.Decision.PASS : LabMetricDecision.Decision.FAIL,
                        new Date(), decidingUser, labMetric);
            }
        }),
        CALL_RATE_Q17("Q17 Call Rate", false, Category.PERCENTAGE, LabUnit.PERCENTAGE, new Decider() {
            @Override
            public LabMetricDecision makeDecision(LabVessel labVessel, LabMetric labMetric, long decidingUser) {
                LabMetricDecision.Decision decision = LabMetricDecision.Decision.PASS;
                if (labMetric.getValue().doubleValue() < 75) {
                    decision = LabMetricDecision.Decision.FAIL;
                }

                return new LabMetricDecision(decision, new Date(), decidingUser, labMetric);
            }
        }),
        CALL_RATE_Q20("Q20 Call Rate", false, Category.PERCENTAGE, LabUnit.PERCENTAGE, new Decider() {
            @Override
            public LabMetricDecision makeDecision(LabVessel labVessel, LabMetric labMetric, long decidingUser) {
                LabMetricDecision.Decision decision = LabMetricDecision.Decision.PASS;
                if (labMetric.getValue().doubleValue() < 75) {
                    decision = LabMetricDecision.Decision.FAIL;
                }

                return new LabMetricDecision(decision, new Date(), decidingUser, labMetric);
            }
        }),
        ROX_ASSAY_RAW_DATA_COUNT("ROX Assay Raw Data Count", false, Category.NUMBER, LabUnit.NUMBER, new Decider() {
            @Override
            public LabMetricDecision makeDecision(LabVessel labVessel, LabMetric labMetric, long decidingUser) {
                return null;
            }
        }),
        ROX_SAMPLE_RAW_DATA_COUNT("ROX Sample Raw Data Count", false, Category.NUMBER, LabUnit.NUMBER, new Decider() {
            @Override
            public LabMetricDecision makeDecision(LabVessel labVessel, LabMetric labMetric, long decidingUser) {
                return null;
            }
        }),
        ROX_SAMPLE_RAW_DATA_MEAN("ROX Sample Raw Data Mean", false, Category.NUMBER, LabUnit.NUMBER, new Decider() {
            @Override
            public LabMetricDecision makeDecision(LabVessel labVessel, LabMetric labMetric, long decidingUser) {
                return null;
            }
        }),
        ROX_SAMPLE_RAW_DATA_MEDIAN("ROX Sample Raw Data Median", false, Category.NUMBER, LabUnit.NUMBER, new Decider() {
            @Override
            public LabMetricDecision makeDecision(LabVessel labVessel, LabMetric labMetric, long decidingUser) {
                return null;
            }
        }),
        ROX_SAMPLE_RAW_DATA_STD_DEV("ROX Sample Raw Data Std Dev", false, Category.NUMBER, LabUnit.NUMBER, new Decider() {
            @Override
            public LabMetricDecision makeDecision(LabVessel labVessel, LabMetric labMetric, long decidingUser) {
                return null;
            }
        }),

        ROX_SAMPLE_BKGD_DATA_COUNT("ROX Sample Bkgd Data Count", false, Category.NUMBER, LabUnit.NUMBER, new Decider() {
            @Override
            public LabMetricDecision makeDecision(LabVessel labVessel, LabMetric labMetric, long decidingUser) {
                return null;
            }
        }),
        ROX_SAMPLE_BKGD_DATA_MEAN("ROX Sample BKGD Data Mean", false, Category.NUMBER, LabUnit.NUMBER, new Decider() {
            @Override
            public LabMetricDecision makeDecision(LabVessel labVessel, LabMetric labMetric, long decidingUser) {
                return null;
            }
        }),
        ROX_SAMPLE_BKGD_DATA_MEDIAN("ROX Sample BKGD Data Median", false, Category.NUMBER, LabUnit.NUMBER, new Decider() {
            @Override
            public LabMetricDecision makeDecision(LabVessel labVessel, LabMetric labMetric, long decidingUser) {
                return null;
            }
        }),
        ROX_SAMPLE_BKGD_DATA_STD_DEV("ROX Sample BKGD Data Std Dev", false, Category.NUMBER, LabUnit.NUMBER, new Decider() {
            @Override
            public LabMetricDecision makeDecision(LabVessel labVessel, LabMetric labMetric, long decidingUser) {
                return null;
            }
        });

        private String displayName;
        private boolean uploadEnabled;
        private static final Map<String, MetricType> mapNameToType = new HashMap<>();
        private Category category;
        private Decider decider;
        private LabUnit labUnit;

        static {
            for (MetricType metricType : MetricType.values()) {
                mapNameToType.put(metricType.getDisplayName(), metricType);
            }
        }

        MetricType(String displayName, boolean uploadEnabled, Category category, LabUnit labUnit, Decider decider) {
            this.displayName = displayName;
            this.uploadEnabled = uploadEnabled;
            this.category = category;
            this.labUnit = labUnit;
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

        public LabUnit getLabUnit() {
            return labUnit;
        }

        public Category getCategory() {
            return category;
        }

        /**
         * Whether this MetricType represents a concentration
         */
        public enum Category {
            VOLUME,
            NUMBER,
            CONCENTRATION,
            DNA_LENGTH,
            QUALITY,
            PERCENTAGE,
            GENDER
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
    @JoinColumn(name = "LAB_METRIC_RUN")
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
    @JoinColumn(name = "LAB_VESSEL")
    private LabVessel labVessel;

    @Enumerated(EnumType.STRING)
    private VesselPosition vesselPosition;

    private Date createdDate;

    @ManyToMany(cascade = CascadeType.PERSIST)
    @JoinTable(name = "lab_metric_metadata", schema = "mercury",
            joinColumns = @JoinColumn(name = "LAB_METRIC_ID"),
            inverseJoinColumns = @JoinColumn(name = "METADATA_ID"))
    private Set<Metadata> metadataSet = new HashSet<>();

    /** This is actually OneToOne, but using ManyToOne to avoid N+1 selects */
    @ManyToOne(cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)
    @JoinColumn(name = "LAB_METRIC_DECISION")
    private LabMetricDecision labMetricDecision;

    /**
     * For JPA
     */
    protected LabMetric() {
    }

    public LabMetric(BigDecimal value, MetricType metricType, LabUnit labUnit, VesselPosition vesselPosition,
                     Date createdDate) {
        this.value = value;
        this.metricType = metricType;
        this.labUnit = labUnit;
        this.vesselPosition = vesselPosition;
        this.createdDate = createdDate;
    }

    @Nullable
    public BigDecimal getTotalNg() {
        for (Metadata metadata : metadataSet) {
            if (metadata.getKey() == Metadata.Key.TOTAL_NG) {
                return MathUtils.scaleTwoDecimalPlaces(metadata.getNumberValue());
            }
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

    /**
     * For fixups only.
     */
    void setMetricType(MetricType metricType) {
        this.metricType = metricType;
    }

    public LabUnit getUnits() {
        return labUnit;
    }

    public void setLabUnit(LabUnit labUnit) {
        this.labUnit = labUnit;
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

    public VesselPosition getVesselPosition() {
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

    public Set<Metadata> getMetadataSet() {
        return metadataSet;
    }

    /**
     * Provides sorting based upon create date, and in the case of duplicate dates, id
     * @param labMetric
     * @return
     */
    @Override
    public int compareTo(LabMetric labMetric) {
        CompareToBuilder compareToBuilder = new CompareToBuilder();
        if (getCreatedDate() != null) {
            compareToBuilder.append(getCreatedDate(), labMetric.getCreatedDate());
        }
        compareToBuilder.append(getLabMetricId(), labMetric.getLabMetricId());
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
