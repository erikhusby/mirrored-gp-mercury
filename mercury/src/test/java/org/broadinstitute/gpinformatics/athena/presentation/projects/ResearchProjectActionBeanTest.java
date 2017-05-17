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
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.person.RoleType;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.UserTokenInput;
import org.broadinstitute.gpinformatics.infrastructure.MockServerTest;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionLibraryDescriptor;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionRepository;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionsService;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionsWillAlwaysWorkSubmissionsService;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.broadinstitute.gpinformatics.mercury.presentation.TestCoreActionBeanContext;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.hamcrest.Matchers;
import org.mockito.Mockito;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.HttpStatusCode;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject.RegulatoryDesignation.CLINICAL_DIAGNOSTICS;
import static org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject.RegulatoryDesignation.GENERAL_CLIA_CAP;
import static org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject.RegulatoryDesignation.RESEARCH_ONLY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

@Test(groups = TestGroups.DATABASE_FREE)
public class ResearchProjectActionBeanTest extends MockServerTest {

    private static final String TEST_REPOSITORY_NAME = "myRepoName";
    private static final String TEST_REPOSITORY_DESCRIPTION = "myRepoDescription";

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
        actionBean.setSubmissionsService(new SubmissionsWillAlwaysWorkSubmissionsService());
        actionBean.setUserBean(userBean);
        actionBean.setBroadPiList(broadPiList);
        actionBean.setBspUserList(bspUserList);
        actionBean.setEditResearchProject(researchProject);
        actionBean.setContext(new TestCoreActionBeanContext());
        actionBean.initSubmissions();
        assertThat(actionBean.validateViewOrPostSubmissions(false), is(submissionAllowed.booleanValue()));
        assertThat(actionBean.getValidationErrors().isEmpty(), is(submissionAllowed.booleanValue()));
    }


    public void testSubmissionLibraryNotOverwritten() throws Exception {
        SubmissionsService submissionsService = setupInitSubmissionsMocks();
        ResearchProjectActionBean actionBean = setupInitSubmissionsProject(submissionsService);
        String selectedLibrary = "RNA Seq";
        actionBean.setSelectedSubmissionLibraryDescriptor(selectedLibrary);
        actionBean.initSubmissions();
        Mockito.verify(submissionsService, Mockito.atLeastOnce()).findLibraryDescriptorTypeByKey(Mockito.matches(selectedLibrary));
        assertThat(actionBean.getSelectedSubmissionLibraryDescriptor(), is(selectedLibrary));
    }

    public void testSubmissionRepositoryNotOverwritten() throws Exception {
        SubmissionsService submissionsService = setupInitSubmissionsMocks();
        ResearchProjectActionBean actionBean = setupInitSubmissionsProject(submissionsService);
        String selectedRepository = TEST_REPOSITORY_NAME;
        actionBean.setSelectedSubmissionRepository(selectedRepository);
        actionBean.initSubmissions();
        assertThat(actionBean.getSelectedSubmissionRepository(), is(selectedRepository));
    }

    public void testRepositoryIsNotNullAndSelectedIsBlank() throws Exception {
        SubmissionsService submissionsService = setupInitSubmissionsMocks();
        ResearchProjectActionBean actionBean = setupInitSubmissionsProject(submissionsService);
        SubmissionRepository submissionRepository =
                new SubmissionRepository(TEST_REPOSITORY_NAME, TEST_REPOSITORY_DESCRIPTION);
        actionBean.setSubmissionRepository(submissionRepository);
        actionBean.initSubmissions();

        assertThat(actionBean.getSubmissionRepository(), Matchers.equalTo(submissionRepository));
        assertThat(actionBean.getSelectedSubmissionRepository(), Matchers.nullValue());

    }

    public void testSelectedIsNotNullAndRepositoryIsNull() throws Exception {
        SubmissionsService submissionsService = setupInitSubmissionsMocks();
        ResearchProjectActionBean actionBean = setupInitSubmissionsProject(submissionsService);

        actionBean.setSelectedSubmissionRepository(TEST_REPOSITORY_NAME);
        actionBean.setSubmissionRepository(null);
        actionBean.initSubmissions();

        assertThat(actionBean.getSubmissionRepository(), nullValue());
        assertThat(actionBean.getSelectedSubmissionRepository(), Matchers.equalTo(TEST_REPOSITORY_NAME));
    }

    private ResearchProjectActionBean setupInitSubmissionsProject(SubmissionsService submissionsService) {
        ResearchProject researchProject = ResearchProjectTestFactory.createTestResearchProject();
        ResearchProjectActionBean actionBean = new ResearchProjectActionBean();
        actionBean.setEditResearchProject(researchProject);
        actionBean.setContext(new TestCoreActionBeanContext());
        actionBean.setSubmissionsService(submissionsService);
        return actionBean;
    }

    private SubmissionsService setupInitSubmissionsMocks() {
        SubmissionsService mockService = Mockito.mock(SubmissionsService.class);
        Mockito.when(mockService.getSubmissionRepositories()).thenReturn(Arrays.asList(
                new SubmissionRepository(SubmissionRepository.DEFAULT_REPOSITORY_NAME,
                        SubmissionRepository.DEFAULT_REPOSITORY_DESCRIPTOR),
                new SubmissionRepository("GDC_PROTECTED", "Genomics Data Commons (GDC) Controlled Access submissions")
        ));
        Mockito.when(mockService.getSubmissionLibraryDescriptors()).thenReturn(Arrays.asList(
                new SubmissionLibraryDescriptor("Whole Genome", "Human Whole Genome"),
                new SubmissionLibraryDescriptor("RNA Seq", "RNA Sequencing")
        ));
        return mockService;
    }

    public void testRpWithDraftPdoDoestThrowsException() {
        ResearchProject researchProject = ResearchProjectTestFactory.createTestResearchProject();

        ProductOrder productOrder = new ProductOrder("New PDO " + System.currentTimeMillis(), "No comment", "MMMAC1");
        productOrder.setResearchProject(researchProject);
        assertThat(productOrder.getProduct(), nullValue());

        ResearchProjectActionBean actionBean = new ResearchProjectActionBean();
        assertThat(productOrder.getOrderStatus(), is(ProductOrder.OrderStatus.Draft));

        SubmissionLibraryDescriptor defaultSubmissionType = actionBean.findDefaultSubmissionType(researchProject);
        assertThat(defaultSubmissionType, nullValue());
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

    public void testSubmissionServiceDownDoesntPreventRpsBeingLoaded() throws Exception {
        ResearchProjectActionBean actionBean = new ResearchProjectActionBean();
        TestCoreActionBeanContext testContext = new TestCoreActionBeanContext();

        actionBean.setContext(testContext);
        HttpResponse serverUnavailable =
                HttpResponse.response().withStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR_500.code());

        actionBean.setSubmissionsService(serviceWithResponse(serverUnavailable));
        actionBean.initSubmissions();
        assertThat(actionBean.getFormattedMessages(), contains(ResearchProjectActionBean.SUBMISSIONS_UNAVAILABLE));
    }
}
