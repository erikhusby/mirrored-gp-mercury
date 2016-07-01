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
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.UserTokenInput;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.broadinstitute.gpinformatics.mercury.presentation.TestCoreActionBeanContext;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject.RegulatoryDesignation.CLINICAL_DIAGNOSTICS;
import static org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject.RegulatoryDesignation.GENERAL_CLIA_CAP;
import static org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject.RegulatoryDesignation.RESEARCH_ONLY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@Test(groups = TestGroups.DATABASE_FREE)
public class ResearchProjectActionBeanTest {
    @BeforeMethod(alwaysRun = true)
    public void setUp() throws Exception {
    }

    @DataProvider(name = "submissionDataProvider")
    public Iterator<Object[]> submissionDataProvider() {
        List<Object[]> testCases = new ArrayList<>();

        // Test different project roles
        testCases.add(new Object[]{RoleType.PM, RESEARCH_ONLY, IS_PROJECT_PM.TRUE, SUBMISSION_ALLOWED.TRUE});
        testCases.add(new Object[]{RoleType.PM, RESEARCH_ONLY, IS_PROJECT_PM.FALSE, SUBMISSION_ALLOWED.FALSE});

        // Test non-PM roles
        for (RoleType roleType : RoleType.values()) {
            if (roleType != RoleType.PM) {
                testCases.add(new Object[]{roleType, RESEARCH_ONLY, IS_PROJECT_PM.TRUE, SUBMISSION_ALLOWED.FALSE});
                testCases.add(new Object[]{roleType, RESEARCH_ONLY, IS_PROJECT_PM.FALSE, SUBMISSION_ALLOWED.FALSE});

            }
        }
        // Test different regulatory designations
        testCases.add(new Object[]{RoleType.PM, GENERAL_CLIA_CAP, IS_PROJECT_PM.TRUE, SUBMISSION_ALLOWED.FALSE});
        testCases.add(new Object[]{RoleType.PM, CLINICAL_DIAGNOSTICS, IS_PROJECT_PM.TRUE, SUBMISSION_ALLOWED.FALSE});

        return testCases.iterator();
    }


    @Test(dataProvider = "submissionDataProvider")
    public void testSubmissionsValidation(RoleType userRole,
                                          ResearchProject.RegulatoryDesignation regulatoryDesignation,
                                          IS_PROJECT_PM userIsProjectPM, SUBMISSION_ALLOWED submissionAllowed)
            throws Exception {
        BSPUserList bspUserList = Mockito.mock(BSPUserList.class);
        UserTokenInput broadPiList = new UserTokenInput(bspUserList);
        UserBean userBean = Mockito.mock(UserBean.class);
        BspUser qaDudeUser = new BSPUserList.QADudeUser(userRole.name(), 1L);
        Mockito.when(userBean.getBspUser()).thenReturn(qaDudeUser);
        Mockito.when(userBean.isPMUser()).thenReturn(userRole == RoleType.PM);

        ResearchProject researchProject = ResearchProjectTestFactory.createTestResearchProject();
        researchProject.setRegulatoryDesignation(regulatoryDesignation);

        if (userIsProjectPM.booleanValue()) {
            researchProject.addPeople(RoleType.PM, Collections.singleton(qaDudeUser));
        }

        ResearchProjectActionBean actionBean = new ResearchProjectActionBean();
        actionBean.setUserBean(userBean);
        actionBean.setBroadPiList(broadPiList);
        actionBean.setBspUserList(bspUserList);
        actionBean.setEditResearchProject(researchProject);
        actionBean.setContext(new TestCoreActionBeanContext());

        assertThat(actionBean.validateViewOrPostSubmissions(false), is(submissionAllowed.booleanValue()));
        assertThat(actionBean.getValidationErrors().isEmpty(), is(submissionAllowed.booleanValue()));
    }

    private enum SUBMISSION_ALLOWED {
        TRUE, FALSE;

        public boolean booleanValue() {
            return this == TRUE;
        }
    }

    private enum IS_PROJECT_PM {
        TRUE, FALSE;

        public boolean booleanValue() {
            return this == TRUE;
        }
    }
}
