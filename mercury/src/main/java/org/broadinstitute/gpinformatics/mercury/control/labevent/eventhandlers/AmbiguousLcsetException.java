package org.broadinstitute.gpinformatics.mercury.control.labevent.eventhandlers;

/** The exception thrown when processing a lab event causes a vessel to have an ambiguous lcset. */
public class AmbiguousLcsetException extends RuntimeException {

    public AmbiguousLcsetException(String s) {
        super(s);
    }
}
