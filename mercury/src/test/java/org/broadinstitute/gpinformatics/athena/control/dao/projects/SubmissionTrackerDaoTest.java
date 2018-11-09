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

package org.broadinstitute.gpinformatics.athena.control.dao.projects;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.entity.project.SubmissionTracker;
import org.broadinstitute.gpinformatics.infrastructure.metrics.AggregationTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.Aggregation;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.LevelOfDetection;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionBioSampleBean;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionDto;
import org.broadinstitute.gpinformatics.infrastructure.test.StubbyContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

@Test(groups = TestGroups.STUBBY)
@Dependent
public class SubmissionTrackerDaoTest extends StubbyContainerTest {

    public SubmissionTrackerDaoTest(){}

    @Inject
    SubmissionTrackerDao submissionTrackerDao;
    @Inject
    private ResearchProjectDao researchProjectDao;
    private ResearchProject researchProject;

    private static final String RP_ID = "RP-SubmissionTrackerDaoTest";
    private static final String PDO_ID = "PDO-SubmissionTrackerDaoTest";
    public static final int DEFAULT_VERSION = 1;

    private String sampleName;

    @BeforeMethod(alwaysRun = true)
    public void setUp() throws Exception {
        if (submissionTrackerDao == null) {
            return;
        }
        sampleName = "NewSample_" + System.currentTimeMillis();
        researchProject = researchProjectDao.findByBusinessKey(RP_ID);
        if (researchProject == null) {
            researchProject = createResearchProject(RP_ID);
        }
    }

    @AfterMethod
    public void cleanUpTrackers() throws Exception {
        if (submissionTrackerDao == null) {
            return;
        }
    }

    public void testFindSubmissionTrackersNoneExist() throws Exception {
        SubmissionDto submissionDto = getSubmissionDto(PDO_ID, RP_ID, sampleName, 1, SubmissionBioSampleBean.ON_PREM,
            Aggregation.DATA_TYPE_EXOME);
        List<SubmissionTracker> submissionTrackers =
                submissionTrackerDao.findSubmissionTrackers(Collections.singleton(submissionDto));
        assertThat(submissionTrackers, emptyCollectionOf(SubmissionTracker.class));
    }

    public void testFindSubmissionTrackersWithResult() throws Exception {
        SubmissionDto submissionDto =
                getSubmissionDto(PDO_ID, RP_ID, sampleName, DEFAULT_VERSION, null, Aggregation.DATA_TYPE_EXOME);
        SubmissionTracker submissionTracker = addTracker(submissionDto);

        persistTrackers(Collections.singleton(submissionTracker));

        List<SubmissionTracker> submissionTrackers =
                submissionTrackerDao.findSubmissionTrackers(Collections.singleton(submissionDto));
        assertThat(submissionTrackers, hasSize(1));

        assertThat(submissionTrackers.iterator().next().getSubmissionTuple(), equalTo(submissionTracker.getSubmissionTuple()));
    }

    public void testFindSubmissionTrackersWithNewVersion() throws Exception {
        SubmissionDto submissionDto1 =
            getSubmissionDto(PDO_ID, RP_ID, sampleName, DEFAULT_VERSION, SubmissionBioSampleBean.ON_PREM,
                Aggregation.DATA_TYPE_EXOME);
        SubmissionTracker submissionTracker1 = addTracker(submissionDto1);

        int newVersion = DEFAULT_VERSION + 1;
        SubmissionDto submissionDto2 =
            getSubmissionDto(PDO_ID, RP_ID, sampleName, newVersion, SubmissionBioSampleBean.ON_PREM,
                Aggregation.DATA_TYPE_EXOME);
        SubmissionTracker submissionTracker2 = addTracker(submissionDto2);

        persistTrackers(Arrays.asList(submissionTracker1, submissionTracker2));

        List<SubmissionTracker> submissionTrackers =
                submissionTrackerDao.findSubmissionTrackers(Collections.singleton(submissionDto1));
        assertThat(submissionTrackers, hasSize(2));

        submissionTracker1 = submissionTrackers.get(0);
        submissionTracker2 = submissionTrackers.get(1);

        // versions should be different.
        assertThat(submissionTracker1.getVersion(), not(equalTo(submissionTracker2.getVersion())));
    }

    public void testSubmissionTrackersWithDifferentDataTypes(){
        // SubmissionTracker 1
        SubmissionDto submissionDto1 = getSubmissionDto(PDO_ID, RP_ID, sampleName, DEFAULT_VERSION,
            SubmissionBioSampleBean.ON_PREM, Aggregation.DATA_TYPE_EXOME);
        SubmissionTracker submissionTracker1 = addTracker(submissionDto1);

        // SubmissionTracker 2
        SubmissionDto submissionDto2 =
            getSubmissionDto(PDO_ID, RP_ID, sampleName, DEFAULT_VERSION,
                SubmissionBioSampleBean.ON_PREM, Aggregation.DATA_TYPE_RNA);
        SubmissionTracker submissionTracker2 = addTracker(submissionDto2);

        persistTrackers(Arrays.asList(submissionTracker1, submissionTracker2));
    }

    public void testFindSubmissionTrackersWithDifferentLocations() throws Exception {
        // SubmissionTracker 1
        SubmissionDto submissionDto1 = getSubmissionDto(PDO_ID, RP_ID, sampleName, DEFAULT_VERSION,
            SubmissionBioSampleBean.ON_PREM, Aggregation.DATA_TYPE_EXOME);
        SubmissionTracker submissionTracker1 = addTracker(submissionDto1);

        // SubmissionTracker 2
        SubmissionDto submissionDto2 =
            getSubmissionDto(PDO_ID, RP_ID, sampleName, DEFAULT_VERSION, SubmissionBioSampleBean.GCP,
                Aggregation.DATA_TYPE_EXOME);
        SubmissionTracker submissionTracker2 = addTracker(submissionDto2);

        persistTrackers(Arrays.asList(submissionTracker1, submissionTracker2));

        List<SubmissionTracker> submissionTrackers =
                submissionTrackerDao.findSubmissionTrackers(Collections.singleton(submissionDto1));
        assertThat(submissionTrackers, hasSize(1));
        submissionTracker1 = submissionTrackers.get(0);

        submissionTrackers =
                submissionTrackerDao.findSubmissionTrackers(Collections.singleton(submissionDto2));
        assertThat(submissionTrackers, hasSize(1));
        submissionTracker2 = submissionTrackers.get(0);
        // Tuples should be different.
        assertThat(submissionTracker1.getSubmissionTuple(), not(equalTo(submissionTracker2.getSubmissionTuple())));
    }


    private void persistTrackers(Collection<SubmissionTracker> submissionTrackers) {
        for (SubmissionTracker tracker : submissionTrackers) {
            submissionTrackerDao.persist(tracker);
            submissionTrackerDao.flush();
        }
        submissionTrackerDao.clear();
    }

    private SubmissionDto getSubmissionDto(String productOrderId, final String project, final String sampleName,
                                           final int version, String processingLocation, String dataType) {
        ProductOrder productOrder = ProductOrderTestFactory.createDummyProductOrder(1, productOrderId);
        ResearchProject researchProject = ResearchProjectTestFactory.createTestResearchProject(project);
        researchProject.addProductOrder(productOrder);

        Aggregation aggregation = AggregationTestFactory
            .buildAggregation(project, productOrderId, sampleName, version, 1d, new LevelOfDetection(1d, 3d), dataType,
                2d, 3l, 4d, processingLocation);
        /*
         * TODO: Allow this relationship to be set for these tests.
         * While it's not necessary for the current implementation of the behavior being tested here, this relationship
         * really should be set. However, ProductOrderTestFactory isn't meant to rely on a database connection; it is
         * currently being used for database-free tests. Consequently it creates a Product and a PriceItem for the
         * ProductOrder. When this test attempts to persist a tracker, the persist cascades through the ResearchProject,
         * ProductOrder, Product, and PriceItem where it runs into a unique constraint (SYS_C00173064) on PriceItem
         * platform/category/name.
         *
         * Adding a Product parameter to createDummyProductOrder would allow each test to decide whether or not to use a
         * persisted product.
         */
//        productOrder.setResearchProject(researchProject);
        return new SubmissionDto(aggregation, null);
    }

    private SubmissionTracker addTracker(SubmissionDto submissionDto) {
        SubmissionTracker submissionTracker = new SubmissionTracker(submissionDto.getAggregationProject(),
            submissionDto.getSampleName(), String.valueOf(submissionDto.getVersion()), submissionDto.getFileType(),
            submissionDto.getProcessingLocation(), submissionDto.getDataType());
        submissionTracker.setResearchProject(researchProject);
        return submissionTracker;
    }

    private ResearchProject createResearchProject(String jiraKey) {
        ResearchProject testResearchProject = ResearchProjectTestFactory.createTestResearchProject();
        testResearchProject.setJiraTicketKey(jiraKey);
        researchProjectDao.persist(testResearchProject);
        researchProjectDao.flush();

        return testResearchProject;
    }
}
