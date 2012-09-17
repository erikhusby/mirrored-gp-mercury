package org.broadinstitute.sequel.boundary.pmbridge;


public class ResearchProjectNotFoundException extends RuntimeException {

    ResearchProjectNotFoundException(String message) {
        super(message);
    }


    ResearchProjectNotFoundException(String message, Exception e) {
        super(message, e);
    }
}
