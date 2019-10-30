package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public enum MetricReworkDisposition {
    PASS("Pass", new LabMetric.MetricType[]{LabMetric.MetricType.INITIAL_PICO},
            "Concentration is between 5 and 100, at least 2 reads are within 10% of each other.",
            "No action"),
    UNDILUTED("Undiluted", new LabMetric.MetricType[]{LabMetric.MetricType.INITIAL_PICO},
            "Concentration is below 5",
            "Re-pico undiluted"),
    NORM_IN_TUBE("Norm In Tube", new LabMetric.MetricType[]{LabMetric.MetricType.INITIAL_PICO},
            "Concentration is over 100 and norm to 50 would not cause tube split",
            "Norm to 50"),
    NORM_ADJUSTED_DOWN("Norm Adjusted Down", new LabMetric.MetricType[]{LabMetric.MetricType.INITIAL_PICO},
            "Concentration is over 100 but norm to 50 would cause tube split",
            "Norm to <100 with volume <599"),
    TUBE_SPLIT("Tube Split", new LabMetric.MetricType[]{LabMetric.MetricType.INITIAL_PICO},
            "Concentration is over 100 and cannot norm in tube with vol < 599",
            "Leave 30 uL in stock tube and norm to 50. Transfer remainder to new tube"),
    BAD_TRIP_READS("Bad Trip Reads", new LabMetric.MetricType[]{LabMetric.MetricType.INITIAL_PICO},
            "Less than two reads.", "Re-pico"),
    BAD_TRIP_HIGH("High Bad Trip", new LabMetric.MetricType[]{LabMetric.MetricType.INITIAL_PICO},
            "No two reads are within 10% of each other and concentration is above 40.",
            "Add 50% volume, re-pico"),
    BAD_TRIP_LOW("Low Bad Trip", new LabMetric.MetricType[]{LabMetric.MetricType.INITIAL_PICO},
            "No two reads are within 10% of each other and concentration is below 40",
            "Re-pico"),
    AUTO_SELECT("Auto Select", new LabMetric.MetricType[]{LabMetric.MetricType.INITIAL_PICO},
            "Only two reads are within 10% of each other",
            "No action"),
    BAD_TRIP_OVERFLOW("Bad Trip Overflow", new LabMetric.MetricType[]{LabMetric.MetricType.INITIAL_PICO},
            "No two reads are within 10% of each other, concentration is above 40, and 50% volume cannot be added without causing volume over 599",
            "Leave 30 uL in stock tube and add 15 uL. Transfer remainder volume to new tube. Re-pico active stock and store tube split."),
    TUBE_SPLIT_ADJUSTED_DOWN("Tube Split Adjusted Down", new LabMetric.MetricType[]{LabMetric.MetricType.INITIAL_PICO},
            "Concentration is over 1000 and cannot norm tube split to 50 without overflow",
            "Leave 30 uL in stock tube and norm to < 100. Transfer remainder volume to new tube.  Re-pico active stock and store tube split.");

    MetricReworkDisposition(String displayName, LabMetric.MetricType[] metricGroups, String description, String action) {
        this.displayName = displayName;
        this.metricGroups = metricGroups;
        this.description = description;
        this.action = action;
    }

    private String displayName;
    // TODO JMS Delete? Initial thought was that users would be able to manually assign dispositions
    //  from a list related to a specific metric type
    private LabMetric.MetricType[] metricGroups;
    private String description;
    private String action;

    private static MultiValuedMap<LabMetric.MetricType, MetricReworkDisposition> mapGroupToDispositionType = new ArrayListValuedHashMap<>();
    private static Map<String, MetricReworkDisposition> displayNameMap = new HashMap<>();

    static {
        for (MetricReworkDisposition metricReworkDisposition : MetricReworkDisposition.values()) {
            displayNameMap.put(metricReworkDisposition.getDisplayName(), metricReworkDisposition);
            for (LabMetric.MetricType metricType : metricReworkDisposition.metricGroups) {
                mapGroupToDispositionType.put(metricType, metricReworkDisposition);
            }
        }
    }

    public Collection<MetricReworkDisposition> getGroupDispositionTypes(LabMetric.MetricType metricType) {
        return mapGroupToDispositionType.get(metricType);
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getAction() {
        return action;
    }

    /**
     * Converts from display value passed back in from UI (PicoDispositionActionBean.ListItem)
     */
    public static MetricReworkDisposition fromDisplayValue(String value) {
        return displayNameMap.get(value);
    }
}
