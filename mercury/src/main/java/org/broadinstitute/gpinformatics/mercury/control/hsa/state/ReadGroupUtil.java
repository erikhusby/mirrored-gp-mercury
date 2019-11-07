package org.broadinstitute.gpinformatics.mercury.control.hsa.state;

import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;

public class ReadGroupUtil {

    private static final String RGID_FORMAT = "%s_%d_%s";

    public static String createRgId(IlluminaFlowcell flowcell, int lane, MercurySample mercurySample) {
        return String.format(RGID_FORMAT, flowcell.getLabel(), lane, mercurySample.getSampleKey());
    }

    public static String createRgId(String flowcell, int lane, String sampleKey) {
        return String.format(RGID_FORMAT, flowcell, lane, sampleKey);
    }

    public static String parseSmFromRgId(String rgId) {
        return rgId.split("_")[2];
    }
}
