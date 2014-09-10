package org.broadinstitute.gpinformatics.mercury.boundary.manifest;

import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestRecord;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestStatus;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

/**
 * Tests if the argument has an error message consistent with the ManifestStatus against which it is to be matched
 */
public class ManifestStatusErrorMatcher extends BaseMatcher<ManifestStatus> {

    private final ManifestRecord.ErrorStatus errorStatus;

    private ManifestStatusErrorMatcher(ManifestRecord.ErrorStatus errorStatus) {
        this.errorStatus = errorStatus;
    }

    public static ManifestStatusErrorMatcher hasError(ManifestRecord.ErrorStatus errorStatus) {
        return new ManifestStatusErrorMatcher(errorStatus);
    }

    @Override
    public boolean matches(Object item) {
        ManifestStatus manifestStatus = (ManifestStatus) item;
        for (String errorMessage : manifestStatus.getErrorMessages()) {
            if (errorMessage.contains(errorStatus.getBaseMessage())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("ManifestStatus for " + errorStatus);
    }
}
