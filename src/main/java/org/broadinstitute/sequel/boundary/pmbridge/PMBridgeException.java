package org.broadinstitute.sequel.boundary.pmbridge;


public class PMBridgeException extends RuntimeException {

    public PMBridgeException(Exception e) {
        super(e);
    }
}
