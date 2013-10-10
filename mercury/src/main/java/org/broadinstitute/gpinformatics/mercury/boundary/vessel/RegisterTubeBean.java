package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class RegisterTubeBean extends org.broadinstitute.gpinformatics.mercury.boundary.vessel.generated.RegisterTubeBean {
    public RegisterTubeBean(@Nonnull String barcode, @Nullable String well, @Nullable String sampleId) {
        this.barcode = barcode;
        this.well = well;
        this.sampleId = sampleId;
    }
}
