package org.broadinstitute.gpinformatics.mercury.entity.sample;

import java.util.HashSet;
import java.util.Set;

/**
 * TODO scottmat fill in javadoc!!!
 */
public class ManifestStatus {
    private final int samplesInManifest;
    private final int samplesEligibleInManifest;
    private final int samplesSuccessfullyScanned;

    private final Set<String> errorMessages = new HashSet<>();

    public ManifestStatus(int samplesInManifest, int samplesEligibleInManifest, int samplesSuccessfullyScanned) {


        this.samplesInManifest = samplesInManifest;
        this.samplesEligibleInManifest = samplesEligibleInManifest;
        this.samplesSuccessfullyScanned = samplesSuccessfullyScanned;
    }


    public int getSamplesInManifest() {
        return samplesInManifest;
    }

    public int getSamplesEligibleInManifest() {
        return samplesEligibleInManifest;
    }

    public int getSamplesSuccessfullyScanned() {
        return samplesSuccessfullyScanned;
    }

    public void addError(String errorMessage) {

        errorMessages.add(errorMessage);
    }

    public Set<String> getErrorMessages() {
        return errorMessages;
    }
}
