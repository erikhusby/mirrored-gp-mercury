package org.broadinstitute.gpinformatics.mercury.control.hsa.state;

import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;

public class ReadGroupUtil {

    private static final String RGID_FORMAT = "%s.%d";
    private static final String RGPU_FORMAT = "%s.%d.%s";
    private static final String RG_ID_SS_FORMAT = "%s_%d_%s";

    public static String createRgId(IlluminaFlowcell flowcell, int lane) {
        return String.format(RGID_FORMAT, flowcell.getLabel(), lane);
    }

    /**
     * Dragen's don't accept a period in the sample sheet for SMs so must output RGID with '_' and
     * replace later in fastqs to match the pipeline.
     */
    public static String createSampleSheetId(String flowcell, int lane, String sampleKey) {
        return String.format(RG_ID_SS_FORMAT, flowcell, lane, sampleKey);
    }

    public static String createRgPu(String flowcell, int lane, String index) {
        return String.format(RGPU_FORMAT, flowcell, lane, index);
    }
}
