package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.billing.LedgerEntryDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.Date;

/**
 * Database Free unit test of Ledger Entry entity ETL.
 *
 * @author epolk
 */

@Test(groups = TestGroups.DATABASE_FREE)
public class LedgerEntryEtlDbFreeTest {
    private final String etlDateStr = ExtractTransform.formatTimestamp(new Date());
    private static final long LEDGER_ID = 1122334455;
    private static final long PRODUCT_ORDER_SAMPLE_ID = 223344551;
    private static final String QUOTE_ID = "ABCD9";
    private static final long PRICE_ITEM_ID = 123;
    private static final LedgerEntry.PriceItemType PRICE_ITEM_TYPE = LedgerEntry.PriceItemType.ADD_ON_PRICE_ITEM;
    private static final double LEDGER_QUANTITY = 1;
    private static final long BILLING_SESSION_ID = 456;
    private static final String BILLING_SESSION_MESSAGE = "Quote server returned:\nERROR";
    private static final String FORMATTED_BILLING_SESSION_MESSAGE = BILLING_SESSION_MESSAGE.replace('\n', ' ');
    private static final Date WORK_COMPLETE_DATE = new Date();
    private static final String WORK_ITEM_ID = "2201";
    private static final String SAP_DELIVERY_DOCUMENT_ID = "0200003194";
    private String datafileDir;
    private LedgerEntryEtl ledgerEntryEtl;

    private final AuditReaderDao auditReader = EasyMock.createMock(AuditReaderDao.class);
    private final LedgerEntry ledgerEntry = EasyMock.createMock(LedgerEntry.class);
    private final LedgerEntryDao ledgerEntryDao = EasyMock.createMock(LedgerEntryDao.class);
    private final ProductOrderSample productOrderSample = EasyMock.createMock(ProductOrderSample.class);
    private final PriceItem priceItem = EasyMock.createMock(PriceItem.class);
    private final BillingSession billingSession = EasyMock.createMock(BillingSession.class);
    private final Object[] mocks = new Object[]{auditReader, ledgerEntry, ledgerEntryDao, productOrderSample,
            priceItem, billingSession};

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void beforeMethod() {
        datafileDir = System.getProperty("java.io.tmpdir");
        ExtractTransform.setDatafileDir(datafileDir);
        EtlTestUtilities.deleteEtlFiles(datafileDir);

        ledgerEntryEtl = new LedgerEntryEtl(ledgerEntryDao);
        ledgerEntryEtl.setAuditReaderDao(auditReader);

        EasyMock.reset(mocks);
    }

    @AfterMethod(groups = TestGroups.DATABASE_FREE)
    public void afterMethod() throws Exception {
        EtlTestUtilities.deleteEtlFiles(datafileDir);
    }

    public void testEtlFlags() throws Exception {
        EasyMock.expect(ledgerEntry.getLedgerId()).andReturn(LEDGER_ID);
        EasyMock.replay(mocks);

        Assert.assertEquals(ledgerEntryEtl.entityClass, LedgerEntry.class);
        Assert.assertEquals(ledgerEntryEtl.baseFilename, "ledger_entry");
        Assert.assertEquals(ledgerEntryEtl.entityId(ledgerEntry), (Long) LEDGER_ID);

        EasyMock.verify(mocks);
    }

    public void testCantMakeEtlRecord() throws Exception {
        EasyMock.expect(ledgerEntryDao.findById(LedgerEntry.class, -1L)).andReturn(null);

        EasyMock.replay(mocks);

        Assert.assertEquals(ledgerEntryEtl.dataRecords(etlDateStr, false, -1L).size(), 0);

        EasyMock.verify(mocks);
    }

    public void testMakeRecord() throws Exception {
        EasyMock.expect(ledgerEntryDao.findById(LedgerEntry.class, LEDGER_ID)).andReturn(ledgerEntry);
        EasyMock.expect(ledgerEntry.getLedgerId()).andReturn(LEDGER_ID);
        EasyMock.expect(ledgerEntry.getProductOrderSample()).andReturn(productOrderSample);
        EasyMock.expect(productOrderSample.getProductOrderSampleId()).andReturn(PRODUCT_ORDER_SAMPLE_ID);
        EasyMock.expect(ledgerEntry.getQuoteId()).andReturn(QUOTE_ID);
        EasyMock.expect(ledgerEntry.getPriceItem()).andReturn(priceItem);
        EasyMock.expect(priceItem.getPriceItemId()).andReturn(PRICE_ITEM_ID);
        EasyMock.expect(ledgerEntry.getPriceItemType()).andReturn(PRICE_ITEM_TYPE);
        EasyMock.expect(ledgerEntry.getQuantity()).andReturn(LEDGER_QUANTITY);
        EasyMock.expect(ledgerEntry.getBillingSession()).andReturn(billingSession);
        EasyMock.expect(billingSession.getBillingSessionId()).andReturn(BILLING_SESSION_ID);
        EasyMock.expect(ledgerEntry.getBillingMessage()).andReturn(BILLING_SESSION_MESSAGE);
        EasyMock.expect(ledgerEntry.getWorkCompleteDate()).andReturn(WORK_COMPLETE_DATE);
        EasyMock.expect(ledgerEntry.getWorkItem()).andReturn(WORK_ITEM_ID);
        EasyMock.expect(ledgerEntry.getSapDeliveryDocumentId()).andReturn(SAP_DELIVERY_DOCUMENT_ID);
        EasyMock.replay(mocks);

        Collection<String> records = ledgerEntryEtl.dataRecords(etlDateStr, false, LEDGER_ID);
        Assert.assertEquals(records.size(), 1);

        verifyRecord(records.iterator().next());

        EasyMock.verify(mocks);
    }

    private void verifyRecord(String record) {
        int i = 0;
        String[] parts = record.split(",");
        Assert.assertEquals(parts[i++], etlDateStr);
        Assert.assertEquals(parts[i++], "F");
        Assert.assertEquals(parts[i++], String.valueOf(LEDGER_ID));
        Assert.assertEquals(parts[i++], String.valueOf(PRODUCT_ORDER_SAMPLE_ID));
        Assert.assertEquals(parts[i++], QUOTE_ID);
        Assert.assertEquals(parts[i++], String.valueOf(PRICE_ITEM_ID));
        Assert.assertEquals(parts[i++], PRICE_ITEM_TYPE.toString());
        Assert.assertEquals(parts[i++], String.valueOf(LEDGER_QUANTITY));
        Assert.assertEquals(parts[i++], String.valueOf(BILLING_SESSION_ID));
        Assert.assertEquals(parts[i++], FORMATTED_BILLING_SESSION_MESSAGE);
        Assert.assertEquals(parts[i++], EtlTestUtilities.format(WORK_COMPLETE_DATE));
        Assert.assertEquals(parts[i++], WORK_ITEM_ID);
        Assert.assertEquals(parts[i++], SAP_DELIVERY_DOCUMENT_ID);
        Assert.assertEquals(parts.length, i);
    }
}
