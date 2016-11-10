/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2016 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.athena.presentation.projects;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.entity.person.RoleType;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionsService;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.broadinstitute.gpinformatics.mercury.presentation.TestCoreActionBeanContext;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.hamcrest.Matchers;
import org.mockito.Mockito;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.HttpStatusCode;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;

@Test(groups = TestGroups.DATABASE_FREE)
public class ResearchProjectActionBeanSubmissionsTest {
    private ClientAndServer mockServer;

    @BeforeTest
    public void startMockServer() {
        mockServer = ClientAndServer.startClientAndServer(getPortList());
    }

    private Integer[] getPortList() {
        List<Integer> portList = new ArrayList<>();
        while (portList.size() < 20) {
            portList.add(6000+portList.size());
        }
        return portList.toArray(new Integer[portList.size()]);
    }

    @AfterTest(alwaysRun = true)
    public void stopMockServer() {
        mockServer.stop();
    }

    public void testLoadSubmissionData() throws Exception {
        ResearchProjectActionBean actionBean = new ResearchProjectActionBean();
        TestCoreActionBeanContext testContext = new TestCoreActionBeanContext();
        actionBean.setContext(testContext);
        HttpResponse serverUnavailable =
                HttpResponse.response().withStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR_500.code());
        SubmissionsService submissionsService =
                MockSubmissionsService.serviceWithResponse(mockServer, serverUnavailable);
        actionBean.setSubmissionsService(submissionsService);
        try {
            actionBean.loadSubmissionData();
            assertThat(actionBean.getFormattedMessages(),
                    Matchers.contains(ResearchProjectActionBean.SUBMISSIONS_UNAVAILABLE));
        } catch (Exception e) {
            Assert.fail("No Exception should have been thrown");
        }
    }

    public void testValidateViewOrPostSubmissions() {
        ResearchProjectActionBean actionBean = new ResearchProjectActionBean();
        actionBean.setEditResearchProject(ResearchProjectTestFactory.createTestResearchProject());
        TestCoreActionBeanContext testContext = new TestCoreActionBeanContext();
        actionBean.setContext(testContext);
        UserBean userBean = Mockito.mock(UserBean.class);
        BspUser qaDudeUser = new BSPUserList.QADudeUser(RoleType.PM.name(), 1L);
        Mockito.when(userBean.getBspUser()).thenReturn(qaDudeUser);
        Mockito.when(userBean.isPMUser()).thenReturn(true);
        actionBean.setUserBean(userBean);

        HttpResponse serverUnavailable =
                HttpResponse.response().withStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR_500.code());
        SubmissionsService submissionsService =
                MockSubmissionsService.serviceWithResponse(mockServer, serverUnavailable);
        actionBean.setSubmissionsService(submissionsService);
        try {
            boolean validationPassed = actionBean.validateViewOrPostSubmissions();
            assertThat(validationPassed, Matchers.not(true));
            assertThat(actionBean.getFormattedMessages(),
                    Matchers.contains(ResearchProjectActionBean.SUBMISSIONS_UNAVAILABLE));
        } catch (Exception e) {
            Assert.fail("No Exception should have been thrown");
        }
    }
}

