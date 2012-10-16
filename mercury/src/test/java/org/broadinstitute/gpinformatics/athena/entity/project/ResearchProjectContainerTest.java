package org.broadinstitute.gpinformatics.athena.entity.project;

import junit.framework.Assert;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.Test;

import java.io.IOException;

/**
 * @author Scott Matthews
 *         Date: 10/12/12
 *         Time: 7:50 AM
 */
@Test (groups = TestGroups.EXTERNAL_INTEGRATION)
public class ResearchProjectContainerTest extends ContainerTest{

    public void testJiraSubmission() throws IOException {

        ResearchProject dummy = ResearchProjectTest.createDummyResearchProject();

        dummy.submit();

        Assert.assertTrue ( StringUtils.isNotEmpty(dummy.getJiraTicketKey()) );
    }

}
