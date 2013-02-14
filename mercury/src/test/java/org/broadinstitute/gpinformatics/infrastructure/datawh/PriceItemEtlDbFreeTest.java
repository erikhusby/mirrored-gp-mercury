package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.products.PriceItemDao;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static org.easymock.EasyMock.*;
import static org.testng.Assert.*;

/**
 * dbfree unit test of ProductOrder etl.
 *
 * @author epolk
 */

@Test(groups = TestGroups.DATABASE_FREE)
public class PriceItemEtlDbFreeTest {
    String etlDateStr = ExtractTransform.secTimestampFormat.format(new Date());
    long entityId = 1122334455L;
    String platform = "Some platform";
    String category = "Some category";
    String name = "Some name";
    String quoteServerId = "QT-9876";
    String price = "1000.01";
    String units = "1.5";

    AuditReaderDao auditReader = createMock(AuditReaderDao.class);
    PriceItem obj = createMock(PriceItem.class);
    PriceItemDao dao = createMock(PriceItemDao.class);
    Object[] mocks = new Object[]{auditReader, obj, dao};

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void beforeMethod() {
        reset(mocks);
    }

    public void testEtlFlags() throws Exception {
        expect(obj.getPriceItemId()).andReturn(entityId);
        replay(mocks);

        PriceItemEtl tst = new PriceItemEtl();
        tst.setPriceItemDao(dao);
        tst.setAuditReaderDao(auditReader);

        assertEquals(tst.getEntityClass(), PriceItem.class);

        assertEquals(tst.getBaseFilename(), "price_item");

        assertEquals(tst.entityId(obj), (Long) entityId);

        assertNull(tst.entityStatusRecord(etlDateStr, null, null, false));

        assertTrue(tst.isEntityEtl());

        verify(mocks);
    }

    public void testCantMakeEtlRecord() throws Exception {
        expect(dao.findById(PriceItem.class, -1L)).andReturn(null);

        replay(mocks);

        PriceItemEtl tst = new PriceItemEtl();
        tst.setPriceItemDao(dao);
        tst.setAuditReaderDao(auditReader);

        assertEquals(tst.entityRecord(etlDateStr, false, -1L).size(), 0);

        verify(mocks);
    }

    public void testIncrementalEtl() throws Exception {
        expect(dao.findById(PriceItem.class, entityId)).andReturn(obj);

        expect(obj.getPriceItemId()).andReturn(entityId);
        expect(obj.getPlatform()).andReturn(platform);
        expect(obj.getCategory()).andReturn(category).times(2);
        expect(obj.getName()).andReturn(name);
        expect(obj.getQuoteServerId()).andReturn(quoteServerId);
        expect(obj.getPrice()).andReturn(price);
        expect(obj.getUnits()).andReturn(units);

        replay(mocks);

        PriceItemEtl tst = new PriceItemEtl();
        tst.setPriceItemDao(dao);
        tst.setAuditReaderDao(auditReader);

        Collection<String> records = tst.entityRecord(etlDateStr, false, entityId);
        assertEquals(records.size(), 1);

        verifyRecord(records.iterator().next());

        verify(mocks);
    }

    public void testBackfillEtl() throws Exception {
        List<PriceItem> list = new ArrayList<PriceItem>();
        list.add(obj);
        expect(dao.findAll(eq(PriceItem.class), (GenericDao.GenericDaoCallback<PriceItem>) anyObject())).andReturn(list);

        expect(obj.getPriceItemId()).andReturn(entityId);
        expect(obj.getPlatform()).andReturn(platform);
        expect(obj.getCategory()).andReturn(category).times(2);
        expect(obj.getName()).andReturn(name);
        expect(obj.getQuoteServerId()).andReturn(quoteServerId);
        expect(obj.getPrice()).andReturn(price);
        expect(obj.getUnits()).andReturn(units);

        replay(mocks);

        PriceItemEtl tst = new PriceItemEtl();
        tst.setPriceItemDao(dao);
        tst.setAuditReaderDao(auditReader);

        Collection<String> records = tst.entityRecordsInRange(entityId, entityId, etlDateStr, false);
        assertEquals(records.size(), 1);
        verifyRecord(records.iterator().next());

        verify(mocks);
    }

    private void verifyRecord(String record) {
	int i = 0;
        String[] parts = record.split(",");
        assertEquals(parts[i++], etlDateStr);
        assertEquals(parts[i++], "F");
        assertEquals(parts[i++], String.valueOf(entityId));
        assertEquals(parts[i++], platform);
        assertEquals(parts[i++], category);
        assertEquals(parts[i++], name);
        assertEquals(parts[i++], quoteServerId);
        assertEquals(parts[i++], price);
        assertEquals(parts[i++], units);
    }

}

