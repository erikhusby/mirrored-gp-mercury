package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen;

import org.broadinstitute.gpinformatics.athena.presentation.Displayable;

public enum PicardTool implements Displayable {
    CHECK_FINGERPRINT("CheckFingerprint", "Check Fingerprint"),
    CROSSCHECK_FINGERPRINT( "CrosscheckFingerprints", "Crosscheck Fingerprint");

    private final String toolName;
    private final String displayName;

    PicardTool(String toolName, String displayName) {
        this.toolName = toolName;
        this.displayName = displayName;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    public String getToolName() {
        return toolName;
    }
}
