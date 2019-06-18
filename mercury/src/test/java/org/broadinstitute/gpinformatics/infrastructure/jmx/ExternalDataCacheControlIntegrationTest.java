package org.broadinstitute.gpinformatics.infrastructure.jmx;


import org.broadinstitute.gpinformatics.infrastructure.common.TestLogHandler;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mocks.ExplodingCache;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

@Test(groups = TestGroups.STANDARD)
public class ExternalDataCacheControlIntegrationTest extends Arquillian {

    private static final String EXPECTED_LOG_MESSAGE_REGEX = ".*" + ExplodingCache.class.getName() + ".*";

    @Inject
    private ExternalDataCacheControl cacheControl;

    private TestLogHandler testLogHandler;
    private Logger cacheControlLogger;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @BeforeTest
    public void setUpTestLogger() {
        cacheControlLogger = Logger.getLogger(ExternalDataCacheControl.class.getName());
        cacheControlLogger.setLevel(Level.ALL);
        testLogHandler = TestLogHandler.newInstance();
        cacheControlLogger.addHandler(testLogHandler);
        testLogHandler.setLevel(Level.ALL);
    }

    @Test
    public void testThatCacheRefreshErrorIsLogged() {
        cacheControl.registerCache(new ExplodingCache());

        doLogMessageAssertionsForError();
    }


    private void doLogMessageAssertionsForError() {
        boolean foundException = false;
        for (LogRecord logRecord : testLogHandler.getLogs()) {
            if (logRecord.getThrown().getMessage().contains(ExplodingCache.EXCEPTION_TEXT)) {
                foundException = true;
            }
        }
        Assert.assertTrue(foundException,"Root exception from the cache was not logged.  It's likely that cache refresh errors will not appear in the log.");
        Assert.assertTrue(testLogHandler.messageMatches(EXPECTED_LOG_MESSAGE_REGEX));
    }
}
