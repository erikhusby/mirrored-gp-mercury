package org.broadinstitute.gpinformatics.mercury.boundary.pmbridge;


public class ResearchProjectNotFoundException extends RuntimeException {

    ResearchProjectNotFoundException(String message) {
        super(message);
    }


    ResearchProjectNotFoundException(String message, Exception e) {
        super(message, e);
    }
}
