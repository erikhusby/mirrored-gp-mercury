package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.billing.LedgerEntryDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.mockito.Mockito;
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
    private static final Long productId = 3383l;
    private String datafileDir;
    private LedgerEntryEtl ledgerEntryEtl;

    private final AuditReaderDao auditReader = Mockito.mock(AuditReaderDao.class);
    private final LedgerEntry ledgerEntry = Mockito.mock(LedgerEntry.class);

    private final LedgerEntryDao ledgerEntryDao = Mockito.mock(LedgerEntryDao.class);
    private final ProductOrderSample productOrderSample = Mockito.mock(ProductOrderSample.class);

    private final PriceItem priceItem = Mockito.mock(PriceItem.class);
    private final BillingSession billingSession = Mockito.mock(BillingSession.class);
    private final Object[] mocks = new Object[]{auditReader, ledgerEntry, ledgerEntryDao, productOrderSample,
            priceItem, billingSession};

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void beforeMethod() {
        datafileDir = System.getProperty("java.io.tmpdir");
        ExtractTransform.setDatafileDir(datafileDir);
        EtlTestUtilities.deleteEtlFiles(datafileDir);

        ledgerEntryEtl = new LedgerEntryEtl(ledgerEntryDao);
        ledgerEntryEtl.setAuditReaderDao(auditReader);

        Mockito.reset(mocks);
    }

    @AfterMethod(groups = TestGroups.DATABASE_FREE)
    public void afterMethod() throws Exception {
        EtlTestUtilities.deleteEtlFiles(datafileDir);
    }

    public void testEtlFlags() throws Exception {
        Mockito.when(ledgerEntry.getLedgerId()).thenReturn(LEDGER_ID);

        Assert.assertEquals(ledgerEntryEtl.entityClass, LedgerEntry.class);
        Assert.assertEquals(ledgerEntryEtl.baseFilename, "ledger_entry");
        Assert.assertEquals(ledgerEntryEtl.entityId(ledgerEntry), (Long) LEDGER_ID);
    }

    public void testCantMakeEtlRecord() throws Exception {
        Mockito.when(ledgerEntryDao.findById(Mockito.any(), Mockito.eq(-1L))).thenReturn(null);

        Assert.assertEquals(ledgerEntryEtl.dataRecords(etlDateStr, false, -1L).size(), 0);

    }

    public void testMakeRecord() throws Exception {
        Mockito.when(productOrderSample.getProductOrderSampleId()).thenReturn(PRODUCT_ORDER_SAMPLE_ID);
        Mockito.when(priceItem.getPriceItemId()).thenReturn(PRICE_ITEM_ID);
        Mockito.when(ledgerEntryDao.findById(Mockito.anyObject(), Mockito.eq(LEDGER_ID))).thenReturn(ledgerEntry);
        Mockito.when(billingSession.getBillingSessionId()).thenReturn(BILLING_SESSION_ID);
        Mockito.when(ledgerEntry.getLedgerId()).thenReturn(LEDGER_ID);
        Mockito.when(ledgerEntry.getProductOrderSample()).thenReturn(productOrderSample);
        Mockito.when(ledgerEntry.getQuoteId()).thenReturn(QUOTE_ID);
        Mockito.when(ledgerEntry.getPriceItem()).thenReturn(priceItem);
        Mockito.when(ledgerEntry.getPriceItemType()).thenReturn(PRICE_ITEM_TYPE);
        Mockito.when(ledgerEntry.getQuantity()).thenReturn(LEDGER_QUANTITY);
        Mockito.when(ledgerEntry.getBillingSession()).thenReturn(billingSession);
        Mockito.when(ledgerEntry.getBillingMessage()).thenReturn(BILLING_SESSION_MESSAGE);
        Mockito.when(ledgerEntry.getWorkCompleteDate()).thenReturn(WORK_COMPLETE_DATE);
        Mockito.when(ledgerEntry.getWorkItem()).thenReturn(WORK_ITEM_ID);
        Mockito.when(ledgerEntry.getSapDeliveryDocumentId()).thenReturn(SAP_DELIVERY_DOCUMENT_ID);
        Product testProduct = Mockito.mock(Product.class);
        Mockito.when(testProduct.getProductId()).thenReturn(productId);
        Mockito.when(ledgerEntry.getProduct()).thenReturn(testProduct);

        Collection<String> records = ledgerEntryEtl.dataRecords(etlDateStr, false, LEDGER_ID);
        Assert.assertEquals(records.size(), 1);

        verifyRecord(records.iterator().next());

        Mockito.verify(productOrderSample).getProductOrderSampleId();
        Mockito.verify(priceItem).getPriceItemId();
        Mockito.verify(ledgerEntryDao).findById(Mockito.anyObject(), Mockito.anyLong());
        Mockito.verify(billingSession).getBillingSessionId();
        Mockito.verify(ledgerEntry).getLedgerId();
        Mockito.verify(ledgerEntry).getProductOrderSample();
        Mockito.verify(ledgerEntry).getQuoteId();
        Mockito.verify(ledgerEntry).getPriceItem();
        Mockito.verify(ledgerEntry).getPriceItemType();
        Mockito.verify(ledgerEntry).getQuantity();
        Mockito.verify(ledgerEntry).getBillingSession();
        Mockito.verify(ledgerEntry).getBillingMessage();
        Mockito.verify(ledgerEntry).getWorkCompleteDate();
        Mockito.verify(ledgerEntry).getWorkItem();
        Mockito.verify(ledgerEntry).getSapDeliveryDocumentId();
        Mockito.verify(ledgerEntry).getProduct();
        Mockito.verify(testProduct).getProductId();
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
        Assert.assertEquals(parts[i++], String.valueOf(productId));
        Assert.assertEquals(parts.length, i);
    }
}
