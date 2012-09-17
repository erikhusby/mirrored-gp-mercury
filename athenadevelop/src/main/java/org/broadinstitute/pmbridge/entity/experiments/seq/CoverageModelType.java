package org.broadinstitute.pmbridge.entity.experiments.seq;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 5/18/12
 * Time: 11:05 AM
 */
public enum CoverageModelType {

    LANES("Lanes"),
    DEPTH("Depth"),
    TARGETCOVERAGE("Target Coverage"),
    PFREADS("PF Reads"),
    MEANTARGETCOVERAGE("Mean Target Coverage");

    private final String fullName;

    private CoverageModelType(final String fullName) {
        this.fullName = fullName;
    }

    public String getFullName() {
        return fullName;
    }
}
