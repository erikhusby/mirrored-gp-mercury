package org.broadinstitute.gpinformatics.mercury.boundary.zims;


import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfig;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchService;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchServiceImpl;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.IlluminaSequencingRunDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.zims.LibraryBean;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaChamber;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaRun;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.enterprise.inject.Alternative;
import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


// temporary test before SampleDataFetcher and Buick Gpui.  Gets an existing run, then sets various fields (regulatory designation, etc. in the db),
// calls the pipeline API, and checks the outputs
@Test(groups = TestGroups.ALTERNATIVES,enabled = true)
public class CrspPipelineAPITemporaryIntegrationTest extends Arquillian {

    // these SM ids will have a collaborator sample id of a positive control (NA12878)
    private static final Collection<String> FAKE_POSITIVE_CONTROLS = Arrays.asList("SM-5K46D","SM-5861W");

    private static final String TEST_RUN = "140312_SL-HDD_0362_AFCH7G8HADXX";

    private static final String TEST_LANE = "2";


    @Inject IlluminaRunResource illuminaRunResource;

    @Inject
    IlluminaSequencingRunDao sequencingRunDao;

    @Inject
    ResearchProjectDao researchProjectDao;

    @Inject
    LabBatchDao batchDao;

    @org.jboss.arquillian.container.test.api.Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWarWithAlternatives(MockBspService.class);
    }

    @BeforeMethod
    public void setUp() {
        if (illuminaRunResource != null) {
            ZimsIlluminaRun pipelineRun = illuminaRunResource.getMercuryRun(TEST_RUN);
            Set<ResearchProject> projectsToConvertToCrsp  = new HashSet<>();
            Set<Long> bucketEntriesToDelete = new HashSet<>();

            for (ZimsIlluminaChamber lane : pipelineRun.getLanes()) {
                if (TEST_LANE.equals(lane.getName())) {
                    for (LibraryBean libraryBean : lane.getLibraries()) {
                        ResearchProject researchProject = researchProjectDao.findByBusinessKey(libraryBean.getResearchProjectId());
                        researchProject.setRegulatoryDesignation(ResearchProject.RegulatoryDesignation.GENERAL_CLIA_CAP);

                        System.out.println(libraryBean.getSampleId());

                        LabBatch batch = batchDao.findByName(libraryBean.getLcSet());

                        for (BucketEntry bucketEntry : batch.getBucketEntries()) {
                            for (SampleInstanceV2 sampleInstanceV2 : bucketEntry.getLabVessel().getSampleInstancesV2()) {
                                if (FAKE_POSITIVE_CONTROLS.contains(sampleInstanceV2.getMercuryRootSampleName())) {
                                    bucketEntriesToDelete.add(bucketEntry.getBucketEntryId());
                                }
                                projectsToConvertToCrsp.add(bucketEntry.getProductOrder().getResearchProject());
                            }
                        }
                    }
                }
            }
            for (ResearchProject project : projectsToConvertToCrsp) {
                project.setRegulatoryDesignation(ResearchProject.RegulatoryDesignation.GENERAL_CLIA_CAP);
            }
            // this is necessary because of the unmodifiable nature of LabVessel.bucketEntries
            Assert.assertTrue(bucketEntriesToDelete.isEmpty(),"Manual data change is required.  Temporarily uncomment this line and delete the bucket entires printed below.");
            for (Long bucketEntryId : bucketEntriesToDelete) {
                System.out.println("Delete bucket entry " + bucketEntryId);
            }
        }
    }


    // todo arz when sample data fetcher goes live, change the samples to have mercury
    // metadata instead of bsp.
    public void testIt() {
        boolean foundCrspLane = false;
        ZimsIlluminaRun runWithCrspLane = illuminaRunResource.getMercuryRun(TEST_RUN);

        boolean foundPositiveControl = false;
        ResearchProject controlsResearchProject = researchProjectDao.findByBusinessKey(
                new CrspPipelineUtils(Deployment.DEV).getResearchProjectForCrspPositiveControls());
        Assert.assertNotNull(controlsResearchProject);

        for (ZimsIlluminaChamber lane : runWithCrspLane.getLanes()) {
            Set<String> lcSets = new HashSet<>();
            for (LibraryBean libraryBean : lane.getLibraries()) {
                lcSets.add(libraryBean.getLcSet());
            }
            if (TEST_LANE.equals(lane.getName())) {
                foundCrspLane = true;
                for (LibraryBean libraryBean : lane.getLibraries()) {
                    if (Boolean.TRUE.equals(libraryBean.isPositiveControl())) {
                        foundPositiveControl = true;
                        Assert.assertEquals(libraryBean.getCollaboratorParticipantId(),libraryBean.getCollaboratorSampleId());
                        Assert.assertEquals(libraryBean.getCollaboratorSampleId(),"NA12878_LCSET-5071");
                        Assert.assertNull(libraryBean.getProject());
                        Assert.assertEquals(libraryBean.getResearchProjectId(),
                                            controlsResearchProject.getBusinessKey());
                        Assert.assertEquals(libraryBean.getResearchProjectName(),controlsResearchProject.getTitle());
                        // todo arz change metadata to be mercury metadata, test that
                        Assert.assertEquals(libraryBean.getMetadataSource(), MercurySample.BSP_METADATA_SOURCE);

                    }
                    Assert.assertTrue(libraryBean.getLsid().contains("crsp"));
                    Assert.assertEquals(libraryBean.getRootSample(),libraryBean.getSampleId());
                }
            }
        }
        Assert.assertTrue(foundCrspLane);
        Assert.assertTrue(foundPositiveControl,"Did not find any positive controls.");

    }

    /**
     * Defers lookups to dev BSP, unless the sample ids queried
     * are in {@link #FAKE_POSITIVE_CONTROLS}, in which case
     * the returned collaborator sample id is NA12878}
     */
    @Alternative
    public static class MockBspService implements BSPSampleSearchService {

        @Override
        public List<Map<BSPSampleSearchColumn, String>> runSampleSearch(Collection<String> sampleIDs,
                                                                        BSPSampleSearchColumn... resultColumns) {

            BSPSampleSearchServiceImpl bspSampleSearchService = new BSPSampleSearchServiceImpl(BSPConfig.produce(
                    Deployment.DEV));

            List<Map<BSPSampleSearchColumn, String>> results = bspSampleSearchService.runSampleSearch(sampleIDs,resultColumns);

            for (Map<BSPSampleSearchColumn, String> result : results) {
                String sampleId = result.get(BSPSampleSearchColumn.SAMPLE_ID);
                if (FAKE_POSITIVE_CONTROLS.contains(sampleId)) {
                    result.remove(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID);
                    result.remove(BSPSampleSearchColumn.COLLABORATOR_PARTICIPANT_ID);
                    result.put(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID,"NA12878");
                    result.put(BSPSampleSearchColumn.COLLABORATOR_PARTICIPANT_ID,"NA12878");
                }
            }
            return results;
        }
    }

    /**
     * Useful for manual testing of pipeline API from the browser
     */
    //@RunAsClient
    @Test(enabled = false)
    public void test_start_server_for_manual_testing() {
        try {
            System.out.println("Starting up");
            Thread.sleep(1000 * 60 * 60 * 96);
        }
        catch(InterruptedException e) {
            System.out.println("Shutting down");
        }
    }

}
