package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Calendar;
import java.util.Date;

/**
 * dbfree unit test of AbandonVessel and AbandonVesselPosition etl.
 */

@Test(groups = TestGroups.DATABASE_FREE)
public class ArrayProcessFlowEtlDbFreeTest {

    private String etlDateStr;
    private Date eventDate;

    private ArrayProcessFlowEtl arrayProcessFlowEtl;

    private final LabEventDao dao = EasyMock.createMock(LabEventDao.class);

    private final Object[] mocks = new Object[]{dao };

    public ArrayProcessFlowEtlDbFreeTest(){
        Calendar calendar = Calendar.getInstance();
        etlDateStr = ExtractTransform.formatTimestamp(calendar.getTime());
        calendar.add(Calendar.MINUTE, -2);
        eventDate = calendar.getTime();
    }

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void beforeMethod() {
        EasyMock.reset(mocks);

        arrayProcessFlowEtl = new ArrayProcessFlowEtl(dao);
    }

    public void testCantMakeEtlRecords() throws Exception {
        EasyMock.expect(dao.findById(LabEvent.class, -1L)).andReturn(null);
        EasyMock.replay(mocks);

        Assert.assertEquals(arrayProcessFlowEtl.dataRecords(etlDateStr, false, -1L).size(), 0);
        EasyMock.verify(mocks);
    }

    public void testNotAnInfiniumEvent() throws Exception {
        LabEvent notAnInfiniumEvent = new LabEvent(LabEventType.A_BASE, eventDate, "OZ", 1l, 1L, "Python Deck");
        EasyMock.expect(dao.findById(LabEvent.class, 1L)).andReturn(notAnInfiniumEvent);
        EasyMock.replay(mocks);

        Assert.assertEquals(arrayProcessFlowEtl.dataRecords(etlDateStr, false, 1L).size(), 0);
        EasyMock.verify(mocks);
    }

}
