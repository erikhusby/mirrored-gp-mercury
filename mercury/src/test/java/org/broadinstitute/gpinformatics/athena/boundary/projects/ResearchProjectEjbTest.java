package org.broadinstitute.gpinformatics.athena.boundary.projects;

import org.apache.commons.collections4.CollectionUtils;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.entity.project.SubmissionTracker;
import org.broadinstitute.gpinformatics.infrastructure.bass.BassDTO;
import org.broadinstitute.gpinformatics.infrastructure.bioproject.BioProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPCohortList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AppConfig;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.Aggregation;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionDto;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionLibraryDescriptor;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionStatusDetailBean;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionsService;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class ResearchProjectEjbTest extends Arquillian {
    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    private static final String TEST_SAMPLE_1 = "4377315018_E";
    private static final int TEST_VERSION_1 = 1;
    private static final String TEST_SAMPLE_2 = "4304714212_K";
    private ResearchProjectEjb researchProjectEjb;
    @Inject
    private ResearchProjectDao researchProjectDao;

    @Inject
    private JiraService jiraService;
    @Inject
    private BSPUserList userList;
    @Inject
    private BSPCohortList cohortList;
    @Inject
    private AppConfig appConfig;
    @Inject
    private SubmissionsService submissionsService;

    @BeforeMethod
    public void setUp() throws Exception {
        if(researchProjectDao == null) {
            return;
        }
    }

    @AfterMethod
    public void tearDown() throws Exception {
        if(researchProjectDao == null) {
            return;
        }
    }

    public void testPostingSubmissions() throws Exception {
        UserBean mockUserBean = Mockito.mock(UserBean.class);
        Mockito.when(mockUserBean.getBspUser()).thenReturn(new BSPUserList.QADudeUser("PM", 2423L));

        researchProjectEjb = new ResearchProjectEjb(jiraService, mockUserBean, userList, cohortList, appConfig,
                researchProjectDao, submissionsService);

        ResearchProject testRP = ResearchProjectTestFactory.createTestResearchProject();
        testRP.setJiraTicketKey("RP-TestRP2");

        researchProjectDao.persist(testRP);
        researchProjectDao.flush();

        Map<BassDTO.BassResultColumn, String> bassInfo1 = new HashMap<>();
        bassInfo1.put(BassDTO.BassResultColumn.path, "/your/path/testFile1.bam");
        bassInfo1.put(BassDTO.BassResultColumn.version, String.valueOf(TEST_VERSION_1));
        bassInfo1.put(BassDTO.BassResultColumn.sample, TEST_SAMPLE_1);

        Aggregation testAggregation1 = new Aggregation("",TEST_SAMPLE_1, TEST_VERSION_1);

        SubmissionStatusDetailBean statusDetailBean=null;

        SubmissionDto submissionDto1 = new SubmissionDto(new BassDTO(bassInfo1), testAggregation1,
                Collections.<ProductOrder>emptyList(), statusDetailBean);

        Map<BassDTO.BassResultColumn, String> bassInfo2 = new HashMap<>();
        bassInfo2.put(BassDTO.BassResultColumn.path, "/your/path/testFile2.bam");
        bassInfo2.put(BassDTO.BassResultColumn.version, String.valueOf(TEST_VERSION_1));
        bassInfo2.put(BassDTO.BassResultColumn.sample, TEST_SAMPLE_2);

        Aggregation testAggregation2 = new Aggregation("",TEST_SAMPLE_2, TEST_VERSION_1);

        SubmissionDto submissionDto2 = new SubmissionDto(new BassDTO(bassInfo2), testAggregation2,
                Collections.<ProductOrder>emptyList(), statusDetailBean);

        BioProject selectedBioProject = new BioProject("PRJNA75723");
        researchProjectEjb.processSubmissions(testRP.getBusinessKey(), selectedBioProject,
                Arrays.asList(submissionDto1, submissionDto2), testRP.getSubmissionRepository(),
                submissionsService.findSubmissionTypeByKey(SubmissionLibraryDescriptor.WHOLE_GENOME_NAME));

        ResearchProject updatedRP = researchProjectDao.findByBusinessKey(testRP.getBusinessKey());

        Assert.assertTrue(CollectionUtils.isNotEmpty(updatedRP.getSubmissionTrackers()));
        for(SubmissionTracker tracker:updatedRP.getSubmissionTrackers()) {
            Assert.assertTrue(Arrays.asList(TEST_SAMPLE_1, TEST_SAMPLE_2).contains(tracker.getSubmittedSampleName()));
        }

    }

}
