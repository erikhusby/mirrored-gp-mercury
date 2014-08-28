package org.broadinstitute.gpinformatics.mercury.entity.sample;

public class TubeTransferException extends Exception {

    private ManifestRecord.ErrorStatus errorStatus;

    public TubeTransferException(ManifestRecord.ErrorStatus errorStatus) {
        this.errorStatus = errorStatus;
    }

    public ManifestRecord.ErrorStatus getErrorStatus() {
        return errorStatus;
    }
}
