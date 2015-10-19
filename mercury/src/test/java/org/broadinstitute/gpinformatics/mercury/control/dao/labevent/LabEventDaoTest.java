package org.broadinstitute.gpinformatics.mercury.control.dao.labevent;

import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventReagent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.GenericReagent;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * Test persisting LabEvents, including reagents.
 */
@Test(groups = TestGroups.STUBBY)
public class LabEventDaoTest extends ContainerTest {

    @Inject
    private LabEventDao labEventDao;

    public void testReagents() {
        Date eventDate = new Date();
        LabEvent labEvent = new LabEvent(LabEventType.A_BASE, eventDate, "PERIPHERAL_VISION_MAN", 1L, 101L, "Bravo");
        String barcode = Long.toString(System.currentTimeMillis());
        BigDecimal volume = new BigDecimal("1.2");
        labEvent.addReagentVolume(new GenericReagent("ETOH", barcode, null),
                volume);

        labEventDao.persist(labEvent);
        labEventDao.flush();
        labEventDao.clear();

        List<LabEvent> labEvents = labEventDao.findByDate(eventDate, eventDate);
        Assert.assertEquals(labEvents.size(), 1);
        LabEvent labEvent1 = labEvents.get(0);
        Assert.assertEquals(labEvent1.getEventDate(), eventDate);
        Assert.assertEquals(labEvent1.getReagents().size(), 1);
        Assert.assertEquals(labEvent1.getLabEventReagents().size(), 1);
        LabEventReagent labEventReagent = labEvent1.getLabEventReagents().iterator().next();
        Assert.assertEquals(labEventReagent.getVolume(), volume);
        Assert.assertEquals(labEventReagent.getReagent().getLot(), barcode);
    }
}