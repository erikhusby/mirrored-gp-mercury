package org.broadinstitute.gpinformatics.mercury.test;

import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.rapsheet.RapSheetEjb;
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
import java.util.ArrayList;
import java.util.Collection;
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
    RapSheetEjb rapSheetEjb;
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

        final LabEvent pondEnrichmentEvent =
                labEventDao.findList(LabEvent.class, LabEvent_.labEventId, 1043L).iterator()
                        .next();
        final LabVessel testTube = pondEnrichmentEvent.getInPlaceLabVessel();
        final String position =
                testTube.getVesselGeometry().getRowNames()[0] + testTube.getVesselGeometry().getColumnNames()[0];
        final VesselPosition vesselPosition = VesselPosition.getByName(position);
        VesselContainer<?> vesselContainer = testTube.getContainerRole();

        Collection<MercurySample> reworks = rapSheetEjb
                .addRework(testTube, ReworkReason.MACHINE_ERROR, pondEnrichmentEvent.getLabEventType(), "test");
        Assert.assertFalse(reworks.isEmpty(), "No reworks done.");
        MercurySample startingSample = getRandomSample(reworks);

        final ReworkEntry rapSheetEntry = (ReworkEntry) startingSample.getRapSheet().getRapSheetEntries().get(0);
        Assert.assertNotNull(rapSheetEntry.getLabVesselComment().getLabEvent(), "Lab event is required.");
        Assert.assertNotNull(rapSheetEntry.getLabVesselComment().getLabVessel(), "Lab Vessel is required.");
        Assert.assertNotNull(rapSheetEntry.getReworkLevel(), "ReworkLevel cannot be null.");
        Assert.assertNotNull(rapSheetEntry.getReworkReason(), "ReworkReason cannot be null.");
        Assert.assertNotNull(rapSheetEntry.getReworkStep(), "getReworkStep cannot be null.");
        Assert.assertNotNull(rapSheetEntry.getRapSheet(), "rework.getRapSheet cannot be null.");
        Assert.assertNotNull(rapSheetEntry.getRapSheet().getSample(), "RapSheet.sample cannot be null.");
    }

    private <ENTITY_TYPE> ENTITY_TYPE getRandomSample(Collection<ENTITY_TYPE> sampleInstances) {
        Assert.assertFalse(sampleInstances.isEmpty(), "Collection was empty.");
        Random rand = new Random(System.currentTimeMillis());
        final int index = rand.nextInt(sampleInstances.size());
        return (ENTITY_TYPE) sampleInstances.toArray(new ArrayList[sampleInstances.size()])[index];
    }

}
