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
     *  This means the test group requires external resources, but does not require a container
     *  (IE, calls web services) to run properly.
     */
    public static final String EXTERNAL_INTEGRATION = "ExternalIntegration";

    /** Uses stubs */
    public static final String STUBBY = "Stubby";

    /** Uses standard code (no stubs, no alternatives) */
    public static final String STANDARD = "Standard";

    /** Uses CDI alternatives, can't be combined into a suite */
    public static final String ALTERNATIVES = "Alternatives";

    /** "Test" used only to perform a fixup on production data. */
    public static final String FIXUP = "Fixup";

    /** Other types of tests */
    public static final String MULTITHREADED = "Multithreaded";

    /** Long running ALTERNATIVES tests. */
    public static final String LONG_RUNNING = "LongRunning";

    /**
     * This means ... ?
     */
    @SuppressWarnings("UnusedDeclaration")
    public static final String UI = "UI";
}
