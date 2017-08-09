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
import org.broadinstitute.gpinformatics.infrastructure.bass.BassDTO;
import org.broadinstitute.gpinformatics.infrastructure.bass.BassFileType;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionDto;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

@Test(groups = TestGroups.STUBBY)
public class SubmissionTrackerDaoTest extends ContainerTest {
    @Inject
    SubmissionTrackerDao submissionTrackerDao;
    @Inject
    private ResearchProjectDao researchProjectDao;
    private ResearchProject researchProject;

    private static final String TEST_FILE = "/some/test/file";

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
        SubmissionDto submissionDto = getSubmissionDto(PDO_ID, "P123", sampleName, 1, BassFileType.BAM, TEST_FILE);
        List<SubmissionTracker> submissionTrackers =
                submissionTrackerDao.findSubmissionTrackers(Collections.singleton(submissionDto));
        assertThat(submissionTrackers, emptyCollectionOf(SubmissionTracker.class));
    }

    public void testFindSubmissionTrackersWithResult() throws Exception {
        SubmissionDto submissionDto =
                getSubmissionDto(PDO_ID, "P123", sampleName, DEFAULT_VERSION, BassFileType.BAM, null);
        SubmissionTracker submissionTracker = addTracker(submissionDto);

        persistTrackers(Collections.singleton(submissionTracker));

        List<SubmissionTracker> submissionTrackers =
                submissionTrackerDao.findSubmissionTrackers(Collections.singleton(submissionDto));
        assertThat(submissionTrackers, hasSize(1));

        assertThat(submissionTrackers.iterator().next().getTuple(),
                equalTo(submissionDto.getBassDTO().getTuple()));
    }

    public void testFindSubmissionTrackersWithNewVersion() throws Exception {
        SubmissionDto submissionDto1 =
                getSubmissionDto(PDO_ID, "P123", sampleName, DEFAULT_VERSION, BassFileType.BAM, null);
        SubmissionTracker submissionTracker1 = addTracker(submissionDto1);

        int newVersion = DEFAULT_VERSION + 1;
        SubmissionDto submissionDto2 = getSubmissionDto(PDO_ID, "P123", sampleName, newVersion, BassFileType.BAM, null);
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

    public void testFindSubmissionTrackersWithDifferentFileType() throws Exception {
        BassFileType bam = BassFileType.BAM;
        BassFileType picard = BassFileType.PICARD;

        // SubmissionTracker 1
        SubmissionDto submissionDto1 = getSubmissionDto(PDO_ID, "P123", sampleName, DEFAULT_VERSION, bam, null);
        SubmissionTracker submissionTracker1 = addTracker(submissionDto1);

        // SubmissionTracker 2
        SubmissionDto submissionDto2 = getSubmissionDto(PDO_ID, "P123", sampleName, DEFAULT_VERSION, picard, null);
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
        assertThat(submissionTracker1.getTuple(), not(equalTo(submissionTracker2.getTuple())));
    }


    private void persistTrackers(Collection<SubmissionTracker> submissionTrackers) {
        for (SubmissionTracker tracker : submissionTrackers) {
            submissionTrackerDao.persist(tracker);
        }
        submissionTrackerDao.clear();
    }

    private SubmissionDto getSubmissionDto(ProductOrder productOrder, Map<BassDTO.BassResultColumn, String> bassInfo) {
        return new SubmissionDto(new BassDTO(bassInfo), null, Collections.singleton(productOrder), null);
    }

    private SubmissionDto getSubmissionDto(String productOrderId, final String project, final String sampleName,
                                           final int version, final BassFileType fileType, final String path) {
        ProductOrder productOrder = ProductOrderTestFactory.createDummyProductOrder(1, productOrderId);
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
        return getSubmissionDto(productOrder, new HashMap<BassDTO.BassResultColumn, String>() {{
                    put(BassDTO.BassResultColumn.project, project);
                    put(BassDTO.BassResultColumn.sample, sampleName);
                    put(BassDTO.BassResultColumn.file_type, fileType == null ? null : fileType.getBassValue());
                    put(BassDTO.BassResultColumn.version, String.valueOf(version));
                    put(BassDTO.BassResultColumn.path, path);
                }}
        );
    }

    private SubmissionTracker addTracker(SubmissionDto submissionDto) {
        SubmissionTracker submissionTracker = new SubmissionTracker(submissionDto.getAggregationProject(),
                submissionDto.getSampleName(), String.valueOf(submissionDto.getVersion()), submissionDto.getFileType());
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
