package org.broadinstitute.gpinformatics.mercury.control.dao.labevent;

import org.apache.commons.lang.time.DateUtils;
import org.broadinstitute.gpinformatics.infrastructure.test.StubbyContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventReagent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.GenericReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Test persisting LabEvents, including reagents.
 */
@Test(groups = TestGroups.STUBBY)
@Dependent
public class LabEventDaoTest extends StubbyContainerTest {

    public LabEventDaoTest(){}

    @Inject
    private LabEventDao labEventDao;

    public void testReagents() {
        Date eventDate = new Date();
        LabEvent labEvent = new LabEvent(LabEventType.A_BASE, eventDate, "PERIPHERAL_VISION_MAN", 1L, 101L, "Bravo");
        String barcode = Long.toString(System.currentTimeMillis());
        BigDecimal volume = new BigDecimal("1.2");
        Reagent reagent = new GenericReagent("ETOH", barcode, null);
        labEvent.addReagentVolume(reagent, volume);

        labEventDao.persist(labEvent);
        labEventDao.flush();
        labEventDao.clear();

        List<LabEvent> labEvents = labEventDao.findByDate(eventDate, eventDate);
        Assert.assertEquals(labEvents.size(), 1);
        LabEvent labEvent1 = labEvents.get(0);
        Assert.assertTrue(DateUtils.truncatedEquals(labEvent1.getEventDate(), eventDate, Calendar.SECOND));
        Assert.assertEquals(labEvent1.getReagents().size(), 1);
        Assert.assertEquals(labEvent1.getLabEventReagents().size(), 1);
        LabEventReagent labEventReagent = labEvent1.getLabEventReagents().iterator().next();
        Assert.assertEquals(labEventReagent.getVolume(), volume);
        Assert.assertEquals(labEventReagent.getReagent().getLot(), barcode);
        Assert.assertTrue(
                DateUtils.truncatedEquals(labEventReagent.getReagent().getFirstUse()
                        , eventDate
                        , Calendar.SECOND), "Reagent first use does not match event date" );
    }

    public void testReagentWithMetadata() {
        Date eventDate = new Date();
        LabEvent labEvent = new LabEvent(LabEventType.ICE_1S_TBAIT_PICK, eventDate, "PERIPHERAL_VISION_MAN", 1L, 101L, "Bravo");
        String barcode = Long.toString(System.currentTimeMillis());
        String name = "Rapid Capture Kit Box 4 (Bait)";
        Reagent bait = new GenericReagent(name, barcode, null);
        Set<Metadata> metadataSet = new HashSet<>();
        Metadata metadata = new Metadata(Metadata.Key.BAIT_WELL, "A1");
        metadataSet.add(metadata);
        labEvent.addReagentMetadata(bait, metadataSet);
        labEventDao.persist(labEvent);
        labEventDao.flush();
        labEventDao.clear();

        List<LabEvent> labEvents = labEventDao.findByDate(eventDate, eventDate);
        Assert.assertEquals(labEvents.size(), 1);
        LabEvent labEvent1 = labEvents.get(0);
        Assert.assertTrue(DateUtils.truncatedEquals(labEvent1.getEventDate(), eventDate, Calendar.SECOND));
        Assert.assertEquals(labEvent1.getReagents().size(), 1);
        Assert.assertEquals(labEvent1.getLabEventReagents().size(), 1);
        LabEventReagent labEventReagent = labEvent1.getLabEventReagents().iterator().next();
        Assert.assertEquals(labEventReagent.getReagent().getLot(), barcode);
        Assert.assertEquals(labEventReagent.getReagent().getName(), name);
        Assert.assertTrue(
                DateUtils.truncatedEquals(labEventReagent.getReagent().getFirstUse()
                        , eventDate
                        , Calendar.SECOND), "Reagent first use does not match event date" );
        metadataSet = labEventReagent.getMetadata();
        Assert.assertEquals(metadataSet.size(), 1);
        Metadata metadata1 = metadataSet.iterator().next();
        Assert.assertEquals(metadata1.getKey(), Metadata.Key.BAIT_WELL);
        Assert.assertEquals(metadata1.getStringValue(), "A1");
    }
}
