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
 * dbfree unit test of entity etl.
 *
 * @author epolk
 */

@Test(groups = TestGroups.DATABASE_FREE)
public class PriceItemEtlDbFreeTest {
    private String etlDateStr = ExtractTransform.secTimestampFormat.format(new Date());
    private long entityId = 1122334455L;
    private String platform = "Some platform";
    private String category = "Some category";
    private String name = "Some name";
    private String quoteServerId = "QT-9876";
    private String price = "1000.01";
    private String units = "1.5";
    private PriceItemEtl tst;

    private AuditReaderDao auditReader = createMock(AuditReaderDao.class);
    private PriceItem obj = createMock(PriceItem.class);
    private PriceItemDao dao = createMock(PriceItemDao.class);
    private Object[] mocks = new Object[]{auditReader, obj, dao};

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void beforeMethod() {
        reset(mocks);

        tst = new PriceItemEtl();
        tst.setPriceItemDao(dao);
        tst.setAuditReaderDao(auditReader);
    }

    public void testEtlFlags() throws Exception {
        expect(obj.getPriceItemId()).andReturn(entityId);
        replay(mocks);

        assertEquals(tst.entityClass, PriceItem.class);
        assertEquals(tst.baseFilename, "price_item");
        assertEquals(tst.entityId(obj), (Long) entityId);

        verify(mocks);
    }

    public void testCantMakeEtlRecord() throws Exception {
        expect(dao.findById(PriceItem.class, -1L)).andReturn(null);

        replay(mocks);

        assertEquals(tst.dataRecords(etlDateStr, false, -1L).size(), 0);

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

        Collection<String> records = tst.dataRecords(etlDateStr, false, entityId);
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
        assertEquals(parts.length, i);
    }

}

