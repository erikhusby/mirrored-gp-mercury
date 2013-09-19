package org.broadinstitute.gpinformatics.infrastructure.security;

/**
 * Application Context helps define a targeted instance of mercury.  Currently there are 2 mercury deployments, it is
 * conceivable that this list can grow in the future.
 */
public enum ApplicationContext {
    CRSP("CRSP Mercury"),
    RESEARCH("Research Mercury");

    private final String description;


    ApplicationContext(String description) {

        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
