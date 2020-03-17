package org.broadinstitute.gpinformatics.mercury.control.hsa.state;

import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.RunCartridge;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReadGroupUtil {

    private static final String RGID_FORMAT = "%s.%d";
    private static final String RGPU_FORMAT = "%s.%d.%s";
    private static final String RG_ID_SS_FORMAT = "%s_%d_%s";
    private static final Pattern RG_SS_PATTERN = Pattern.compile("([A-Za-z0-9]+)_(\\d+)_(.*)");
    private static final String AGG_RG_METRIC_FORMAT = "%s_Aggregation";

    public static String createRgId(RunCartridge flowcell, int lane) {
        return String.format(RGID_FORMAT, flowcell.getLabel(), lane);
    }

    /**
     * SampleSheet Spec requires Sample_Id col to be alphanumeric, a dash, or underscore.
     */
    public static String createSampleSheetId(String flowcell, int lane, String sampleKey) {
        String rgSampleIdId = convertSampleKeyToSampleSheetId(sampleKey);
        return String.format(RG_ID_SS_FORMAT, flowcell, lane, rgSampleIdId);
    }

    public static String convertSampleKeyToSampleSheetId(String sampleKey) {
        return sampleKey.replaceAll("[^A-Za-z0-9_\\-]", "_");
    }

    public static String createRgPu(String flowcell, int lane, String index) {
        return String.format(RGPU_FORMAT, flowcell, lane, index);
    }

    public static String parseSampleIdFromRgSampleSheet(String rgSampleId) {
        Matcher matcher = RG_SS_PATTERN.matcher(rgSampleId);
        if (matcher.matches()) {
            return matcher.group(3);
        }
        return null;
    }

    public static String toAggregationReadGroupMetric(String sampleKey) {
        return String.format(AGG_RG_METRIC_FORMAT, sampleKey);
    }
}
