package org.broadinstitute.gpinformatics.mercury.infrastructure.deployment;


/**
 * STUBBY substitutes stub (not mock!) implementations of external resources.  DEV, TEST, QA, and PROD are all real
 * implementations.
 */
public enum Deployment {
    DEV,
    TEST,
    QA,
    PROD,
    STUBBY
}
