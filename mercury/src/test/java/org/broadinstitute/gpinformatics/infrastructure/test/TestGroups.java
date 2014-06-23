package org.broadinstitute.gpinformatics.infrastructure.test;

/**
 * The defined list of the TestNG test groups you should put into your unit tests.
 */
public class TestGroups {
    /**
     * This means the test group does not require a database.
     */
    public static final String DATABASE_FREE = "DatabaseFree";

    /**
     *  This means the test group requires a container and external resources to run properly.
     */
    public static final String EXTERNAL_INTEGRATION = "ExternalIntegration";

    public static final String STUBBY = "Stubby";

    /**
     * This means ... ?
     */
    @SuppressWarnings("UnusedDeclaration")
    public static final String UI = "UI";
}