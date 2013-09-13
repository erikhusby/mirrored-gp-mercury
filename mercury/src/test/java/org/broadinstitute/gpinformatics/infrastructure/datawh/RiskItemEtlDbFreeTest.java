package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.RiskItemDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.orders.RiskItem;
import org.broadinstitute.gpinformatics.athena.entity.products.RiskCriterion;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.Date;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.verify;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.replay;
import static org.testng.Assert.assertEquals;

/**
 * dbfree unit test of entity etl.
 *
 * @author epolk
 */

@Test(groups = TestGroups.DATABASE_FREE)
public class RiskItemEtlDbFreeTest {
    private final String etlDateString = ExtractTransform.formatTimestamp(new Date());
    private long posId = 2233445511L;
    private String datafileDir;
    private RiskItemEtl tst;

    private AuditReaderDao auditReader = createMock(AuditReaderDao.class);
    private RiskItem obj = createMock(RiskItem.class);
    private RiskItemDao dao = createMock(RiskItemDao.class);
    private ProductOrderSampleDao pdoDao = createMock(ProductOrderSampleDao.class);
    private ProductOrderSample pos = createMock(ProductOrderSample.class);
    private Object[] mocks = new Object[]{auditReader, obj, dao, pdoDao, pos};
    private String riskType = RiskCriterion.RiskCriteriaType.TOTAL_DNA.getLabel();
    private String riskMessage = "At 4:56:41 PM on May 23, 2013, calculated (Total DNA > 5) risk on value 7.67";

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void beforeMethod() {
        datafileDir = System.getProperty("java.io.tmpdir");
        ExtractTransform.setDatafileDir(datafileDir);
        EtlTestUtilities.deleteEtlFiles(datafileDir);

        reset(mocks);

        tst = new RiskItemEtl(dao, pdoDao);
        tst.setAuditReaderDao(auditReader);
    }

    @AfterMethod
    public void afterMethod() throws Exception {
        EtlTestUtilities.deleteEtlFiles(datafileDir);
    }

    public void testEtlFlags() throws Exception {
        long entityId = 1122334455L;
        expect(obj.getRiskItemId()).andReturn(entityId);
        replay(mocks);

        assertEquals(tst.entityClass, RiskItem.class);
        assertEquals(tst.baseFilename, "product_order_sample_risk");
        assertEquals(tst.entityId(obj), (Long) entityId);

        verify(mocks);
    }

    public void testCantMakeEtlRecord() throws Exception {
        expect(dao.findById(ProductOrderSample.class, -1L)).andReturn(null);

        replay(mocks);

        assertEquals(tst.dataRecords(etlDateString, false, -1L).size(), 0);

        verify(mocks);
    }

    public void testMakePosRecord() throws Exception {
        expect(dao.findById(ProductOrderSample.class, posId)).andReturn(pos);
        expect(pos.getProductOrderSampleId()).andReturn(posId);
        expect(pos.isOnRisk()).andReturn(true);
        expect(pos.getRiskTypeString()).andReturn(riskType);
        expect(pos.getRiskString()).andReturn(riskMessage);
        replay(mocks);

        Collection<String> records = tst.dataRecords(etlDateString, false, posId);
        assertEquals(records.size(), 1);

        verifyRecord(records.iterator().next());

        verify(mocks);
    }

    private void verifyRecord(String record) {
        int i = 0;

        // Get record data splitting on commas that are not inside surrounding quotes.
        String[] parts = EtlTestUtilities.splitRecords(record);
        assertEquals(parts[i++], etlDateString);
        assertEquals(parts[i++], "F");
        assertEquals(parts[i++], String.valueOf(posId));
        assertEquals(parts[i++], "T");
        assertEquals(parts[i++], GenericEntityEtl.format(riskType));

        // For strings that could contain commas we must use the GenericEntityEtl.format() method as it will add quotes around the string.
        assertEquals(parts[i++], GenericEntityEtl.format(riskMessage));
        assertEquals(parts.length, i);
    }

}

