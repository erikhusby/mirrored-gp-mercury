package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventMetadata;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * dbfree unit test of entity etl.
 */

@Test(groups = TestGroups.DATABASE_FREE)
public class EventMetadataEtlDbFreeTest {
    private final String etlDateStr = ExtractTransform.formatTimestamp(new Date());
    private final long entityId = 1122334455L;
    private LabEventMetadata.LabEventMetadataType metaType = LabEventMetadata.LabEventMetadataType.QcFailed;
    private String metaValue = "TheMetaValue";
    private Long eventOneId = Long.valueOf(1);
    private Long eventTwoId = Long.valueOf(2);

    private LabEventMetadataEtl tst;

    private LabEventMetadata obj = EasyMock.createMock(LabEventMetadata.class);
    private final AuditReaderDao auditReader = EasyMock.createMock(AuditReaderDao.class);
    private final LabEventDao dao = EasyMock.createMock(LabEventDao.class);
    private LabEvent eventOne = EasyMock.createMock(LabEvent.class);
    private LabEvent eventTwo = EasyMock.createMock(LabEvent.class);
    private final Object[] mocks = new Object[]{auditReader, dao, obj, eventOne, eventTwo};

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void beforeMethod() {
        EasyMock.reset(mocks);

        tst = new LabEventMetadataEtl(dao);
        tst.setAuditReaderDao(auditReader);
    }

    public void testEtlFlags() throws Exception {
        EasyMock.expect(obj.getLabEventMetadataId()).andReturn(entityId);
        EasyMock.replay(mocks);

        Assert.assertEquals(tst.entityClass, LabEventMetadata.class);
        Assert.assertEquals(tst.baseFilename, "event_metadata");
        Assert.assertEquals(tst.entityId(obj), (Long) entityId);

        EasyMock.verify(mocks);
    }

    public void testCantMakeEtlRecord() throws Exception {
        EasyMock.expect(dao.findById(LabEventMetadata.class, -1L)).andReturn(null);
        EasyMock.replay(mocks);
        Assert.assertEquals(tst.dataRecords(etlDateStr, false, -1L).size(), 0);
        EasyMock.verify(mocks);
    }

    public void testCantMakeEtlRecord2() throws Exception {
        EasyMock.expect(dao.findById(LabEventMetadata.class, entityId)).andReturn(obj);
        EasyMock.expect(obj.getLabEvents()).andReturn(null);
        EasyMock.replay(mocks);
        Assert.assertEquals(tst.dataRecords(etlDateStr, false, entityId).size(), 0);
        EasyMock.verify(mocks);
    }

    public void testEtl() throws Exception {
        EasyMock.expect(dao.findById(LabEventMetadata.class, entityId)).andReturn(obj);

        EasyMock.expect(eventOne.getLabEventId()).andReturn(eventOneId);
        EasyMock.expect(eventTwo.getLabEventId()).andReturn(eventTwoId);
        Set<LabEvent> events = new HashSet<>();
        events.add(eventOne);
        events.add(eventTwo);
        EasyMock.expect(obj.getLabEvents()).andReturn(events).times(2);
        EasyMock.expect(obj.getLabEventMetadataId()).andReturn(entityId).times(2);
        EasyMock.expect(obj.getLabEventMetadataType()).andReturn(metaType).times(2);
        EasyMock.expect(obj.getValue()).andReturn(metaValue).times(2);

        EasyMock.replay(mocks);

        Collection<String> records = tst.dataRecords(etlDateStr, false, entityId);
        EasyMock.verify(mocks);

        Assert.assertEquals(records.size(), 2);
        verifyRecords(records);
    }


    private void verifyRecords(Collection<String> records) {
        boolean foundEventOne = false;
        boolean foundEventTwo = false;
        for (Iterator<String> iter = records.iterator(); iter.hasNext(); ) {
            String[] parts = iter.next().split(",", 7);
            int i = 0;
            Assert.assertEquals(parts[i++], etlDateStr);
            Assert.assertEquals(parts[i++], "F");
            Assert.assertEquals(parts[i++], String.valueOf(entityId));
            Long eventId = Long.valueOf(parts[i++]);
            if (eventId.compareTo(eventOneId) == 0) {
                foundEventOne = true;
            } else if (eventId.compareTo(eventTwoId) == 0) {
                foundEventTwo = true;
            }
            Assert.assertEquals(parts[i++], metaType.name());
            Assert.assertEquals(parts[i++], metaValue);
            Assert.assertEquals(parts.length, i);
        }
        if (!foundEventOne || !foundEventTwo) {
            Assert.fail("Expected two separate events to be exported");
        }
    }

}
