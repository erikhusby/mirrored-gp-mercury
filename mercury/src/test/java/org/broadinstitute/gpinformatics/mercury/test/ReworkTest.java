package org.broadinstitute.gpinformatics.mercury.test;

import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.rapsheet.ReworkEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent_;
import org.broadinstitute.gpinformatics.mercury.entity.rapsheet.ReworkEntry;
import org.broadinstitute.gpinformatics.mercury.entity.rapsheet.ReworkReason;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
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
    ReworkEjb reworkEjb;
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
        final List<LabEvent> eventList =
                labEventDao.findList(LabEvent.class, LabEvent_.labEventType, LabEventType.SAMPLE_IMPORT);
        Assert.assertFalse(eventList.isEmpty(), "No Events fount for " + LabEventType.SAMPLE_IMPORT.name());
        LabEvent sampleImport = eventList.iterator().next();

        final LabVessel testTube = sampleImport.getInPlaceLabVessel();
        Collection<MercurySample> reworks = new ArrayList<MercurySample>();
        try {
            reworks = reworkEjb.addRework(testTube, ReworkReason.MACHINE_ERROR, sampleImport.getLabEventType(), "test");
        } catch (ValidationException e) {
            Assert.fail(e.getMessage());
        }
        Assert.assertFalse(reworks.isEmpty(), "No reworks done.");
    }
}
