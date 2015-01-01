package org.broadinstitute.gpinformatics.athena.boundary.orders;

public class WorkRequestCreationException extends Exception {
    private static final long serialVersionUID = -1763534836988312852L;
    public static final String ERROR = "Error submitting kit request: ";

    public WorkRequestCreationException(Exception ex) {
        super(ERROR + ex.getMessage(), ex);
    }
}
