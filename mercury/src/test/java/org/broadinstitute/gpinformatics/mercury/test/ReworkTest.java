package org.broadinstitute.gpinformatics.mercury.test;

import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent_;
import org.broadinstitute.gpinformatics.mercury.entity.rapsheet.ReworkEntry;
import org.broadinstitute.gpinformatics.mercury.entity.rapsheet.ReworkLevel;
import org.broadinstitute.gpinformatics.mercury.entity.rapsheet.ReworkReason;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.util.List;
import java.util.Random;

/**
 * Test that samples can go partway through a workflow, be marked for rework, go to a previous
 * step, and then move to completion.
 */
public class ReworkTest extends ContainerTest {
    @Inject
    private UserTransaction utx;

    @Inject
    LabEventDao labEventDao;
    @Inject
    LabVesselDao labVesselDao;
    @Inject
    MercurySampleDao mercurySampleDao;

    @BeforeMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void setUp() throws Exception {
        if (utx == null) {
            return;
        }
        utx.begin();
    }

    @AfterMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void tearDown() throws Exception {
        // Skip if no injections, since we're not running in container.
        if (utx == null) {
            return;
        }
        utx.rollback();
    }

    // Advance to Pond Pico

    // Mark 2 samples rework
    // How to verify marked?  Users want to know how many times a MercurySample has been reworked
    // Advance rest of samples to end
    // Verify that repeated transition is flagged as error on non-reworked samples
    // Re-enter 2 samples at Pre-LC? (Re-entry points in a process are enabled / disabled on a per product basis)
    // Can rework one sample in a pool?  No.
    @Test(enabled = true, groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testRework() {

        final LabEvent pondEntichmentEvent =
                labEventDao.findList(LabEvent.class, LabEvent_.labEventType, LabEventType.POND_ENRICHMENT).iterator()
                        .next();
        final LabVessel testTube = pondEntichmentEvent.getInPlaceLabVessel();
        final String position =
                testTube.getVesselGeometry().getRowNames()[0] + testTube.getVesselGeometry().getColumnNames()[0];
        final VesselPosition vesselPosition = VesselPosition.getByName(position);
        VesselContainer<?> vesselContainer = testTube.getContainerRole();

        List<SampleInstance> sampleInstances;

        if (vesselContainer != null) {
            sampleInstances = vesselContainer.getSampleInstancesAtPositionList(vesselPosition);
        } else {
            sampleInstances = testTube.getSampleInstancesList();
        }

        MercurySample startingSample = getRandomSample(sampleInstances).getStartingSample();
        startingSample.reworkSample(ReworkReason.MACHINE_ERROR,
                ReworkLevel.ONE_SAMPLE_RELEASE_REST_BATCH, pondEntichmentEvent,
                pondEntichmentEvent.getLabEventType(), testTube, vesselPosition, "");

        mercurySampleDao.persist(startingSample);
        mercurySampleDao.flush();

        final ReworkEntry rapSheetEntry = (ReworkEntry)startingSample.getRapSheet().getRapSheetEntries().get(0);
        Assert.assertNotNull(rapSheetEntry.getLabVesselComment().getLabEvent(), "Lab event is required.");
        Assert.assertNotNull(rapSheetEntry.getLabVesselComment().getLabVessel(), "Lab Vessel is required.");
        Assert.assertNotNull(rapSheetEntry.getLabVesselComment().getRapSheetEntries(),
                "Rap Sheet Entries should not be null.");
        Assert.assertFalse(rapSheetEntry.getLabVesselComment().getRapSheetEntries().isEmpty(),
                "Should have some Rap Sheet Entries.");
        Assert.assertTrue(rapSheetEntry.getLabVesselComment().getRapSheetEntries().get(0) instanceof ReworkEntry,
                "Entry should be ReworkEntry.");

        Assert.assertNotNull(rapSheetEntry.getReworkLevel(), "ReworkLevel cannot be null.");
        Assert.assertNotNull(rapSheetEntry.getReworkReason(), "ReworkReason cannot be null.");
        Assert.assertNotNull(rapSheetEntry.getReworkStep(), "getReworkStep cannot be null.");
        Assert.assertNotNull(rapSheetEntry.getRapSheet(), "rework.getRapSheet cannot be null.");
        Assert.assertNotNull(rapSheetEntry.getRapSheet().getSample(), "RapSheet.sample cannot be null.");
    }

    private SampleInstance getRandomSample(List<SampleInstance> sampleInstances) {
        Random rand = new Random(System.currentTimeMillis());
        final int index = rand.nextInt(sampleInstances.size());
        return sampleInstances.get(index);
    }
}
