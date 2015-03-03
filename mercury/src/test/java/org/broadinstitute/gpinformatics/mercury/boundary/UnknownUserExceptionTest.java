package org.broadinstitute.gpinformatics.mercury.boundary;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.Test;

import static org.broadinstitute.gpinformatics.infrastructure.matchers.ExceptionMessageMatcher.containsMessage;
import static org.broadinstitute.gpinformatics.infrastructure.matchers.ExceptionMessageMatcher.containsMessageIgnoringCase;
import static org.hamcrest.MatcherAssert.assertThat;

@Test(groups = TestGroups.DATABASE_FREE)
public class UnknownUserExceptionTest {

    /**
     * Test that the message returned from the exception is useful. Specifically it should contain the unknown username
     * and the word "unknown" to indicate the nature of the error.
     */
    public void testMessage() {
        UnknownUserException exception = new UnknownUserException("test_user");
        assertThat(exception, containsMessageIgnoringCase("unknown"));
        assertThat(exception, containsMessage("test_user"));
    }
}