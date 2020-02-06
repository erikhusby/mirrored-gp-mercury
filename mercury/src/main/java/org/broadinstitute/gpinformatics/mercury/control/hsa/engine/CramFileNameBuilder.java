package org.broadinstitute.gpinformatics.mercury.control.hsa.engine;

import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;

public class CramFileNameBuilder {

    public enum CramFilenameFormat {
        SAMPLE_KEY("Sample Key"),
        COLLABORATOR_SAMPLE_ID("Collaborator Sample ID"),
        AOU_CENTER("Aou Center");

        private final String displayName;

        CramFilenameFormat(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public static String process(MercurySample mercurySample, CramFilenameFormat filenameFormat) {
        switch (filenameFormat) {
        case SAMPLE_KEY:
            return mercurySample.getSampleKey();
        case COLLABORATOR_SAMPLE_ID:
            return mercurySample.getSampleData().getCollaboratorsSampleName();
        case AOU_CENTER:
            // TODO Revision #?
            String collaboratorsSampleName = mercurySample.getSampleData().getCollaboratorsSampleName();
            return String.format("BI_%s_%s_1", collaboratorsSampleName, mercurySample.getSampleKey());
        }

        return null;
    }
}
