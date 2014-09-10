package org.broadinstitute.gpinformatics.mercury.boundary.manifest;

import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestEvent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestRecord;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestStatus;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

/**
 * TODO scottmat fill in javadoc!!!
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
        Iterable<ManifestEvent> events = (Iterable<ManifestEvent>) o;
        for (ManifestEvent event : events) {
            if(event.getMessage().contains(errorStatus.getBaseMessage())){
                return true;
            }
        }

        return false;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("Manivest Event with "+ errorStatus);
    }
}
