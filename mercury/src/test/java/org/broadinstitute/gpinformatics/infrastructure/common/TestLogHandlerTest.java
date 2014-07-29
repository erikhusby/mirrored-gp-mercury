/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2014 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure.common;


import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

@Test(groups = TestGroups.DATABASE_FREE)
public class TestLogHandlerTest {
    private Logger logger = null;
    private TestLogHandler handler = null;

    @BeforeMethod(alwaysRun = true)
    public void setUp() {
        logger = Logger.getLogger(this.getClass().getName());
        handler = new TestLogHandler();
        handler.setLevel(Level.ALL);
        logger.setUseParentHandlers(false);
        logger.addHandler(handler);
        logger.setLevel(Level.ALL);
    }


    public void testEmpty() throws Exception {
        Assert.assertTrue(handler.getLogs().isEmpty());
    }

    public void testGetLogs() throws Exception {
        String testMessage = "A New Message!";
        logger.info(testMessage);
        Assert.assertEquals(handler.getLogs().size(), 1);
        LogRecord logRecord = Objects.requireNonNull(TestUtils.getFirst((handler.getLogs())));
        Assert.assertEquals(logRecord.getMessage(), testMessage);
    }

    public void testMessageDoesnotMatch() throws Exception {
        String testMessage = "A New Message!";
        logger.info(testMessage);
        Assert.assertFalse(handler.messageMatches("I don't"));
    }

    public void testMessageMatches() throws Exception {
        String testMessage = "A New Message!";
        logger.info(testMessage);
        Assert.assertTrue(handler.messageMatches(testMessage));
    }

    public void testMessagePartialMatch() throws Exception {
        String testMessage = "A New Message!";
        logger.info(testMessage);

        Assert.assertTrue(handler.messageMatches(".*New Message.*$"));
    }

    public void testMessageWrongLogLevel() throws Exception {
        String testMessage = "A New Message!";
        logger.info(testMessage);

        LogRecord logRecord = Objects.requireNonNull(TestUtils.getFirst((handler.getLogs())));
        Assert.assertNotEquals(logRecord.getLevel(), Level.WARNING);
    }

    public void testMessageLogLevel() throws Exception {
        String testMessage = "A New Message!";
        logger.warning(testMessage);

        LogRecord logRecord = Objects.requireNonNull(TestUtils.getFirst((handler.getLogs())));
        Assert.assertEquals(logRecord.getLevel(), Level.WARNING);
    }
}
