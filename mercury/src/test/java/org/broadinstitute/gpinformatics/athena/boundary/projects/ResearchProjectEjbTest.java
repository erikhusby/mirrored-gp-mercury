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
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionStatusDetailBean;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionsService;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionsServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class ResearchProjectEjbTest extends ContainerTest {

    private static final String TEST_SAMPLE_1 = "NA1278";
    private static final int TEST_VERSION_1 = 1;
    private static final String TEST_SAMPLE_2 = "HG02922";
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

    @Override
    @AfterMethod
    public void tearDown() throws Exception {

        if(researchProjectDao == null) {
            return;
        }

    }

    public void testPostingSubmissions() throws Exception {
        SubmissionsService submissionsServiceStub = new SubmissionsServiceStub();
        Collection<SubmissionStatusDetailBean> submissionStatus = submissionsServiceStub.getSubmissionStatus("1324");

        UserBean mockUserBean = Mockito.mock(UserBean.class);
        Mockito.when(mockUserBean.getBspUser()).thenReturn(new BSPUserList.QADudeUser("PM", 2423L));

        researchProjectEjb = new ResearchProjectEjb(jiraService, mockUserBean, userList, cohortList, appConfig,
                researchProjectDao, submissionsService);

        ResearchProject testRP = ResearchProjectTestFactory.createTestResearchProject();
        testRP.setJiraTicketKey("RP-TestRP2");

        researchProjectDao.persist(testRP);
        researchProjectDao.flush();

        Map<BassDTO.BassResultColumn, String> bassInfo1 = new HashMap<>();
        bassInfo1.put(BassDTO.BassResultColumn.file_name, "/your/path/testFile1.bam");
        bassInfo1.put(BassDTO.BassResultColumn.version, String.valueOf(TEST_VERSION_1));
        bassInfo1.put(BassDTO.BassResultColumn.sample, TEST_SAMPLE_1);

        Aggregation testAggregation1 = new Aggregation("",TEST_SAMPLE_1, TEST_VERSION_1);

        Collection<SubmissionStatusDetailBean> statusDetailBean=new ArrayList<>();

        SubmissionDto submissionDto1 = new SubmissionDto(new BassDTO(bassInfo1), testAggregation1,
                Collections.<ProductOrder>emptyList(), statusDetailBean);

        Map<BassDTO.BassResultColumn, String> bassInfo2 = new HashMap<>();
        bassInfo2.put(BassDTO.BassResultColumn.file_name, "/your/path/testFile2.bam");
        bassInfo2.put(BassDTO.BassResultColumn.version, String.valueOf(TEST_VERSION_1));
        bassInfo2.put(BassDTO.BassResultColumn.sample, TEST_SAMPLE_2);

        Aggregation testAggregation2 = new Aggregation("",TEST_SAMPLE_2, TEST_VERSION_1);

        SubmissionDto submissionDto2 = new SubmissionDto(new BassDTO(bassInfo2), testAggregation2,
                Collections.<ProductOrder>emptyList(), statusDetailBean);

        BioProject selectedBioProject = new BioProject("Something");
        researchProjectEjb.processSubmissions(testRP.getBusinessKey(), selectedBioProject,
                Arrays.asList(submissionDto1, submissionDto2));

        ResearchProject updatedRP = researchProjectDao.findByBusinessKey(testRP.getBusinessKey());

        Assert.assertTrue(CollectionUtils.isNotEmpty(updatedRP.getSubmissionTrackers()));
        for(SubmissionTracker tracker:updatedRP.getSubmissionTrackers()) {
            Assert.assertTrue(Arrays.asList(TEST_SAMPLE_1, TEST_SAMPLE_2).contains(tracker.getAccessionIdentifier()));
        }

    }

}
