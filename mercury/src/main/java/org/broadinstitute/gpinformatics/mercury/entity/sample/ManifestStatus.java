package org.broadinstitute.gpinformatics.mercury.entity.sample;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * An object used to represent the state of a {@link ManifestSession} at a particular point in time.
 */
public class ManifestStatus {
    private final int samplesInManifest;
    private final int samplesEligibleInManifest;
    private final int samplesSuccessfullyScanned;
    private final int samplesQuarantined;

    private final Set<String> errorMessages = new HashSet<>();

    public ManifestStatus(int samplesInManifest, int samplesEligibleInManifest, int samplesSuccessfullyScanned,
                          Collection<String> errorMessages, int samplesQuarantined) {
        this.samplesInManifest = samplesInManifest;
        this.samplesEligibleInManifest = samplesEligibleInManifest;
        this.samplesSuccessfullyScanned = samplesSuccessfullyScanned;
        this.samplesQuarantined = samplesQuarantined;
        this.errorMessages.addAll(errorMessages);
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

    public int getSamplesQuarantined() {
        return samplesQuarantined;
    }

    public void addError(String errorMessage) {
        errorMessages.add(errorMessage);
    }

    public Set<String> getErrorMessages() {
        return errorMessages;
    }
}
