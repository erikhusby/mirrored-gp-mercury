package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.AbandonVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

/**
 * dbfree unit test of AbandonVessel and AbandonVesselPosition etl.
 */

@Test(groups = TestGroups.DATABASE_FREE)
public class AbandonVesselEtlDbFreeTest {

    private String etlDateStr;

    private final long abandonVesselEntityId = 11223344L;
    private final Long abandonedLabVesselId = new Long(11111);
    private final AbandonVessel.Reason abandonVesselReason = AbandonVessel.Reason.FAILED_QC;
    private Date abandonVesselDate;

    private final VesselPosition abandonPosition = VesselPosition.D04;
    private final AbandonVessel.Reason abandonPositionReason = AbandonVessel.Reason.EQUIPMENT_FAILURE;
    private Date abandonVesselPositionDate;

    private AbandonVesselEtl abandonVesselEtl;

    private final LabVesselDao dao = EasyMock.createMock(LabVesselDao.class);
    private final AbandonVessel abandonVessel = EasyMock.createMock(AbandonVessel.class);
    private final LabVessel abandonedLabVessel = EasyMock.createMock(LabVessel.class);

    private final Object[] mocks = new Object[]{dao, abandonVessel, abandonedLabVessel };

    public AbandonVesselEtlDbFreeTest(){
        Calendar calendar = Calendar.getInstance();
        etlDateStr = ExtractTransform.formatTimestamp(calendar.getTime());
        calendar.add(Calendar.MINUTE, -2);
        abandonVesselDate = calendar.getTime();
        calendar.add(Calendar.MINUTE, -2);
        abandonVesselPositionDate = calendar.getTime();
    }

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void beforeMethod() {
        EasyMock.reset(mocks);

        abandonVesselEtl = new AbandonVesselEtl(dao);
    }

    public void testEtlFlags() throws Exception {
        EasyMock.expect(abandonVessel.getAbandonVesselId()).andReturn(abandonVesselEntityId);
        EasyMock.replay(mocks);

        Assert.assertEquals(abandonVesselEtl.entityClass, AbandonVessel.class);
        Assert.assertEquals(abandonVesselEtl.baseFilename, "abandon_vessel");
        Assert.assertEquals(abandonVesselEtl.entityId(abandonVessel), (Long) abandonVesselEntityId);
        EasyMock.verify(mocks);
    }

    public void testCantMakeEtlRecords() throws Exception {
        EasyMock.expect(dao.findById(AbandonVessel.class, -1L)).andReturn(null);
        EasyMock.replay(mocks);

        Assert.assertEquals(abandonVesselEtl.dataRecords(etlDateStr, false, -1L).size(), 0);
        EasyMock.verify(mocks);
    }

    public void testAbandonVessel() throws Exception {
        EasyMock.expect(dao.findById(AbandonVessel.class, abandonVesselEntityId)).andReturn(abandonVessel);

        EasyMock.expect(abandonVessel.getAbandonVesselId()).andReturn(abandonVesselEntityId);
        EasyMock.expect(abandonVessel.getLabVessel()).andReturn(abandonedLabVessel);
        EasyMock.expect(abandonedLabVessel.getLabVesselId()).andReturn(abandonedLabVesselId);
        EasyMock.expect(abandonVessel.getReason()).andReturn(abandonVesselReason).times(2);
        EasyMock.expect(abandonVessel.getAbandonedOn()).andReturn(abandonVesselDate);
        EasyMock.expect(abandonVessel.getVesselPosition()).andReturn(null);

        EasyMock.replay(mocks);

        Collection<String> records = abandonVesselEtl.dataRecords(etlDateStr, false, abandonVesselEntityId);

        EasyMock.verify(mocks);

        Assert.assertEquals(records.size(), 1);
        String record = records.iterator().next();

        int i = 0;
        String[] parts = record.split(",",7);
        Assert.assertEquals(parts[i++], etlDateStr);
        Assert.assertEquals(parts[i++], "F");
        Assert.assertEquals(parts[i++], String.valueOf(abandonVesselEntityId));
        Assert.assertEquals(parts[i++], String.valueOf(abandonedLabVesselId));
        Assert.assertEquals(parts[i++], abandonVesselReason.toString());
        Assert.assertEquals(parts[i++], ExtractTransform.formatTimestamp(abandonVesselDate));
        Assert.assertEquals(parts[i++], "");
        Assert.assertEquals(parts.length, i);
}
    public void testAbandonVesselPosition() throws Exception {
        EasyMock.expect(dao.findById(AbandonVessel.class, abandonVesselEntityId)).andReturn(abandonVessel);

        EasyMock.expect(abandonVessel.getAbandonVesselId()).andReturn(abandonVesselEntityId);
        EasyMock.expect(abandonVessel.getLabVessel()).andReturn(abandonedLabVessel);
        EasyMock.expect(abandonedLabVessel.getLabVesselId()).andReturn(abandonedLabVesselId);
        EasyMock.expect(abandonVessel.getReason()).andReturn(abandonVesselReason).times(2);
        EasyMock.expect(abandonVessel.getAbandonedOn()).andReturn(abandonVesselDate);
        EasyMock.expect(abandonVessel.getVesselPosition()).andReturn(abandonPosition).times(2);

        EasyMock.replay(mocks);

        Collection<String> records = abandonVesselEtl.dataRecords(etlDateStr, false, abandonVesselEntityId);
        EasyMock.verify(mocks);

        Assert.assertEquals(records.size(), 1);
        String record = records.iterator().next();

        int i = 0;
        String[] parts = record.split(",",7);
        Assert.assertEquals(parts[i++], etlDateStr);
        Assert.assertEquals(parts[i++], "F");
        Assert.assertEquals(parts[i++], String.valueOf(abandonVesselEntityId));
        Assert.assertEquals(parts[i++], String.valueOf(abandonedLabVesselId));
        Assert.assertEquals(parts[i++], abandonVesselReason.toString());
        Assert.assertEquals(parts[i++], ExtractTransform.formatTimestamp(abandonVesselDate));
        Assert.assertEquals(parts[i++], abandonPosition.toString());
        Assert.assertEquals(parts.length, i);
    }

}
