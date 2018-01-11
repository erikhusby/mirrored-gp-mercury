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

import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.entity.person.RoleType;
import org.broadinstitute.gpinformatics.infrastructure.MockServerTest;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.security.Role;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionsService;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.broadinstitute.gpinformatics.mercury.presentation.TestCoreActionBeanContext;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.hamcrest.Matchers;
import org.mockito.Mockito;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.HttpStatusCode;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@Test(groups = TestGroups.DATABASE_FREE)
public class ResearchProjectActionBeanSubmissionsTest extends MockServerTest {

    public void testLoadSubmissionData() throws Exception {
        ResearchProjectActionBean actionBean = new ResearchProjectActionBean();
        TestCoreActionBeanContext testContext = new TestCoreActionBeanContext();
        actionBean.setContext(testContext);
        HttpResponse serverUnavailable =
                HttpResponse.response().withStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR_500.code());
        SubmissionsService submissionsService = serviceWithResponse(serverUnavailable);
        actionBean.setSubmissionsService(submissionsService);
        try {
            actionBean.initSubmissions();
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
        SubmissionsService submissionsService = serviceWithResponse(serverUnavailable);
        actionBean.setSubmissionsService(submissionsService);
        try {
            boolean validationPassed = actionBean.validateViewOrPostSubmissions();
            assertThat(validationPassed, Matchers.not(true));
        } catch (Exception e) {
            Assert.fail("No Exception should have been thrown");
        }
    }

    @DataProvider(name = "loginUsersDataProvider")
    public Iterator<Object[]> loginUsersDataProvider() {
        List<Object[]> testCases = new ArrayList<>();
        BSPUserList.QADudeUser bspUser = new BSPUserList.QADudeUser(RoleType.BROAD_SCIENTIST.name(), 1L);
        BSPUserList.QADudeUser anotherUser = new BSPUserList.QADudeUser(RoleType.BROAD_SCIENTIST.name(), 2L);

        // Developers always have access
        for (RoleType roleType : RoleType.values()) {
            testCases.add(new Object[]{Role.Developer, Pair.of(roleType, anotherUser), true});
            testCases.add(new Object[]{Role.Developer, Pair.of(roleType, bspUser), true});
        }

        // These cases will always have access to the submission tab
        for (Role role : EnumSet.of(Role.PM, Role.GPProjectManager, Role.Developer)) {
            testCases.add(new Object[]{role, Pair.of(RoleType.PM, bspUser), true});
        }

        EnumSet<RoleType> rolesTypesWillAlwaysFail = EnumSet.complementOf(EnumSet.of(RoleType.PM));
        EnumSet<Role> allRolesExcludingDevelopers = EnumSet.complementOf(EnumSet.of(Role.Developer));

        // These cases will never have access to the submission tab
        for (RoleType roleType : rolesTypesWillAlwaysFail) {
            for (Role role : allRolesExcludingDevelopers) {
                testCases.add(new Object[]{role, Pair.of(roleType, bspUser), false});
            }
        }

        // These cases will fail because the login user is not a PM on the project
        for (RoleType roleType : RoleType.values()) {
            for (Role role : allRolesExcludingDevelopers) {
                testCases.add(new Object[]{role, Pair.of(roleType, anotherUser), false});
            }
        }

        return testCases.iterator();
    }

    @Test(dataProvider = "loginUsersDataProvider")
    public void testSubmissionsTabAccess(Role loginUserRole, Map.Entry<RoleType, BspUser> projectUsers,
                                         boolean willPass) {
        ResearchProjectActionBean actionBean = new ResearchProjectActionBean();
        actionBean.setEditResearchProject(ResearchProjectTestFactory.createTestResearchProject());
        actionBean.getEditResearchProject().clearPeople();
        for (RoleType roleType : RoleType.values()) {
            assertThat(actionBean.getEditResearchProject().getPeople(roleType).length, is(0));
        }

        TestCoreActionBeanContext testContext = new TestCoreActionBeanContext();
        actionBean.setContext(testContext);

        UserBean userBean = Mockito.mock(UserBean.class);
        actionBean.setUserBean(userBean);

        BspUser loginUser = new BSPUserList.QADudeUser(loginUserRole.name(), 1L);

        actionBean.getEditResearchProject()
            .addPeople(projectUsers.getKey(), Collections.singleton(projectUsers.getValue()));


        Mockito.when(userBean.getBspUser()).thenReturn(loginUser);
        Mockito.when(userBean.isGPPMUser()).thenReturn(loginUserRole == Role.GPProjectManager);
        Mockito.when(userBean.isDeveloperUser()).thenReturn(loginUserRole == Role.Developer);
        Mockito.when(userBean.isPDMUser()).thenReturn(loginUserRole == Role.PDM);
        Mockito.when(userBean.isPMUser()).thenReturn(loginUserRole == Role.PM);

        boolean hasSubmissionsTabAccess = ResearchProjectActionBean
            .hasSubmissionsTabAccess(actionBean.getUserBean(), actionBean.getEditResearchProject());
        assertThat(hasSubmissionsTabAccess, is(willPass));
    }

}

