package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.broadinstitute.gpinformatics.athena.presentation.Displayable;

public enum LimsFileType implements Displayable {
    QIAGEN_BLOOD_BIOPSY_24("Qiagen Blood 24 Sample Carrier");

    private final String displayName;

    LimsFileType(String displayName) {
        this.displayName = displayName;
    }


    @Override
    public String getDisplayName() {
        return displayName;
    }
}
