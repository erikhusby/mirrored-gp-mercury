package org.broadinstitute.gpinformatics.infrastructure.deployment;


/**
 * STUBBY substitutes stub (not mock!) implementations of external resources.  DEV, TEST, QA, and PROD are all real
 * implementations.
 */
public enum Deployment {
    DEV,
    TEST,
    QA,
    RC,
    PROD,
    STUBBY,
    /** Used when running tests on the automated build server. */
    AUTO_BUILD,
    DISABLED;

}
