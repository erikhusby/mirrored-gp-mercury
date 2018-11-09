/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2018 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure.template;

import org.broadinstitute.gpinformatics.infrastructure.deployment.AppConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.mail.Session;
import java.util.Collections;
import java.util.Properties;

import static org.broadinstitute.gpinformatics.infrastructure.matchers.ExceptionMessageMatcher.containsMessage;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;

@Test(groups = TestGroups.DATABASE_FREE)
public class EmailSenderTest {
    private static final boolean IGNORE_EXCEPTION = true;
    private static final boolean FAIL_ON_EXCEPTION = false;

    private EmailSender emailSender;
    private AppConfig appConfig;

    @BeforeMethod
    public void setUp() {
        emailSender = new EmailSender(Session.getDefaultInstance(new Properties()));
        appConfig = AppConfig.produce(Deployment.DEV);
    }

    public void testSendHtmlEmailFailsWhenConfiguredToFail() {
        try {
            emailSender.sendHtmlEmail(appConfig, "", Collections.emptyList(), null, null, true, FAIL_ON_EXCEPTION);
            Assert.fail("null and empty values passed into sendHtmlEmail should have caused it to fail.");
        } catch (Exception e) {
            assertThat(e, instanceOf(InformaticsServiceException.class));
            assertThat(e, containsMessage("Error sending email"));
        }
    }

    public void testSendHtmlEmailSucceedsWhenConfiguredToIgnoreExceptions() {
        try {
            emailSender.sendHtmlEmail(appConfig, "", Collections.emptyList(), null, null, true, IGNORE_EXCEPTION);
        } catch (Exception e) {
            Assert.fail("emailSender.sendHtmlEmail is configured to ignore exceptions but one was caught", e);
        }
    }

}
