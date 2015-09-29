package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.easymock.EasyMock.*;
import static org.testng.Assert.*;

/**
 * dbfree unit test of entity etl.
 *
 * @author epolk
 */

@Test(groups = TestGroups.DATABASE_FREE)
public class ProductOrderSampleEtlDbFreeTest {
    private final String etlDateString = ExtractTransform.formatTimestamp(new Date());
    private long entityId = 1122334455L;
    private long pdoId = 98789798L;
    private String sampleName = "SM-007";
    private ProductOrderSample.DeliveryStatus deliveryStatus = ProductOrderSample.DeliveryStatus.NOT_STARTED;
    private int position = 42;
    private ProductOrderSampleEtl tst;
    private String participantId = null;
    private String sampleType = null;
    private Date receiptDate = null;
    private String origSampleType = null;

    private AuditReaderDao auditReader = createMock(AuditReaderDao.class);
    private ProductOrderSampleDao dao = createMock(ProductOrderSampleDao.class);
    private ProductOrder pdo = createMock(ProductOrder.class);
    private ProductOrderSample obj = createMock(ProductOrderSample.class);
    private MercurySample mercurySample = createMock(MercurySample.class);
    private Metadata metaParticipant = createMock(Metadata.class);
    private Metadata metaUseless1 = createMock(Metadata.class);
    private Metadata metaSampleType = createMock(Metadata.class);
    private Metadata metaUseless2 = createMock(Metadata.class);
    private Object[] mocks = new Object[]{auditReader, dao, pdo, obj, mercurySample,
            metaParticipant, metaUseless1, metaSampleType, metaUseless2};

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void setUp() {
        reset(mocks);

        tst = new ProductOrderSampleEtl(dao);
        tst.setAuditReaderDao(auditReader);
        participantId = null;
        sampleType = null;
        receiptDate = new Date();
        origSampleType = null;
    }

    public void testEtlFlags() throws Exception {
        expect(obj.getProductOrderSampleId()).andReturn(entityId);
        replay(mocks);

        assertEquals(tst.entityClass, ProductOrderSample.class);
        assertEquals(tst.baseFilename, "product_order_sample");
        assertEquals(tst.entityId(obj), (Long) entityId);

        verify(mocks);
    }

    public void testCantMakeEtlRecord() throws Exception {
        expect(dao.findById(ProductOrderSample.class, -1L)).andReturn(null);

        replay(mocks);

        assertEquals(tst.dataRecords(etlDateString, false, -1L).size(), 0);

        verify(mocks);
    }

    public void testIncrementalEtlNoSample() throws Exception {
        expect(dao.findById(ProductOrderSample.class, entityId)).andReturn(obj);
        expect(obj.getProductOrderSampleId()).andReturn(entityId);
        expect(obj.getProductOrder()).andReturn(pdo).times(2);
        expect(pdo.getProductOrderId()).andReturn(pdoId);
        expect(obj.getName()).andReturn(sampleName);
        expect(obj.getDeliveryStatus()).andReturn(deliveryStatus);
        expect(obj.getSamplePosition()).andReturn(position);
        expect(obj.getReceiptDate()).andReturn(receiptDate);
        expect(obj.getMercurySample()).andReturn(null);

        replay(mocks);

        Collection<String> records = tst.dataRecords(etlDateString, false, entityId);
        assertEquals(records.size(), 1);
        verifyRecord(records.iterator().next());

        verify(mocks);
    }

    public void testIncrementalEtlWithSample() throws Exception {
        expect(dao.findById(ProductOrderSample.class, entityId)).andReturn(obj);
        expect(obj.getProductOrderSampleId()).andReturn(entityId);
        expect(obj.getProductOrder()).andReturn(pdo).times(2);
        expect(pdo.getProductOrderId()).andReturn(pdoId);
        expect(obj.getName()).andReturn(sampleName);
        expect(obj.getDeliveryStatus()).andReturn(deliveryStatus);
        expect(obj.getSamplePosition()).andReturn(position);
        expect(obj.getReceiptDate()).andReturn(receiptDate);
        expect(obj.getMercurySample()).andReturn(mercurySample).times(2);

        participantId = "PARTICIPANT-1";
        sampleType = "Bag-O-Blood";
        receiptDate = new Date();
        // origSampleType deliberately left blank and no Metadata created for it

        expect(metaParticipant.getKey()).andReturn(Metadata.Key.PATIENT_ID).anyTimes();
        expect(metaParticipant.getValue()).andReturn(participantId).anyTimes();
        expect(metaUseless1.getKey()).andReturn(Metadata.Key.BROAD_2D_BARCODE).anyTimes();
        expect(metaUseless1.getValue()).andReturn("metaUseless1").anyTimes();
        expect(metaSampleType.getKey()).andReturn(Metadata.Key.MATERIAL_TYPE).anyTimes();
        expect(metaSampleType.getValue()).andReturn(sampleType).anyTimes();
        expect(metaUseless2.getKey()).andReturn(Metadata.Key.BUICK_COLLECTION_DATE).anyTimes();
        expect(metaUseless2.getValue()).andReturn("metaUseless2").anyTimes();

        Set<Metadata> metadataSet = new HashSet<>();
        metadataSet.add(metaParticipant);
        metadataSet.add(metaUseless1);
        metadataSet.add(metaSampleType);
        metadataSet.add(metaUseless2);

        expect(mercurySample.getMetadata()).andReturn(metadataSet);

        replay(mocks);

        Collection<String> records = tst.dataRecords(etlDateString, false, entityId);
        assertEquals(records.size(), 1);
        verifyRecord(records.iterator().next());

        verify(mocks);
    }

    private void verifyRecord(String record) {
        int i = 0;
        String[] parts = record.split(",");
        assertEquals(parts[i++], etlDateString);
        assertEquals(parts[i++], "F");
        assertEquals(parts[i++], String.valueOf(entityId));
        assertEquals(parts[i++], String.valueOf(pdoId));
        assertEquals(parts[i++], sampleName);
        assertEquals(parts[i++], deliveryStatus.name());
        assertEquals(parts[i++], String.valueOf(position));
        assertEquals(parts[i++], GenericEntityEtl.format(participantId));
        assertEquals(parts[i++], GenericEntityEtl.format(sampleType));
        assertEquals(parts[i++], GenericEntityEtl.format(receiptDate));
        assertEquals(parts[i++], GenericEntityEtl.format(origSampleType));
        assertEquals(parts.length, i);
    }

}

