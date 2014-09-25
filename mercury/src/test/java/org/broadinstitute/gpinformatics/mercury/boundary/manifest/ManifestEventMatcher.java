package org.broadinstitute.gpinformatics.mercury.boundary.manifest;

import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestEvent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestRecord;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

/**
 * Tests if the argument has an error message consistent with one of the messages in the manifest event against which
 * it is to be matched
 */
public class ManifestEventMatcher extends BaseMatcher<Iterable<ManifestEvent>> {

    private final ManifestRecord.ErrorStatus errorStatus;

    private ManifestEventMatcher(ManifestRecord.ErrorStatus errorStatus) {
        this.errorStatus = errorStatus;
    }

    public static ManifestEventMatcher hasEventError(ManifestRecord.ErrorStatus errorStatus) {
        return new ManifestEventMatcher(errorStatus);
    }

    @Override
    public boolean matches(Object o) {
        @SuppressWarnings("unchecked")
        Iterable<ManifestEvent> events = (Iterable<ManifestEvent>) o;
        for (ManifestEvent event : events) {
            if (event.getMessage().contains(errorStatus.getBaseMessage())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("Manifest Event with " + errorStatus);
    }
}
