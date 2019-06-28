package org.broadinstitute.gpinformatics.mercury.entity.storage;

import java.util.List;

public class NotInStorageException extends RuntimeException {

    private final List<String> missingVessels;

    public NotInStorageException(String s, List<String> missingVessels) {
        super(s);
        this.missingVessels = missingVessels;
    }

    public List<String> getMissingVessels() {
        return missingVessels;
    }
}