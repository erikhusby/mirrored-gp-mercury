package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.Date;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.testng.Assert.assertEquals;


/**
 * dbfree unit test of entity etl.
 *
 * @author epolk
 */

@Test(groups = TestGroups.DATABASE_FREE)
public class ProductEtlDbFreeTest {
    private final String etlDateString = ExtractTransform.formatTimestamp(new Date());
    private long entityId = 1122334455L;
    private String productName = "Test Product";
    private String partNumber = "TestNumber-5544";
    private Date availabilityDate = new Date(1350000000000L);
    private Date discontinuedDate = new Date(1380000000000L);
    private int expectedCycleTimeSeconds = 5400;
    private int guaranteedCycleTimeSeconds = 99999;
    private int samplesPerWeek = 200;
    private boolean isTopLevelProduct = true;
    private Workflow workflow = Workflow.EXOME_EXPRESS;
    private String productFamilyName = "Test ProductFamily";
    private long primaryPriceItemId = 987654321L;
    private ProductEtl tst;

    private AuditReaderDao auditReader = createMock(AuditReaderDao.class);

    private Product obj = createMock(Product.class);
    private ProductDao dao = createMock(ProductDao.class);
    private ProductFamily family = createMock(ProductFamily.class);
    private PriceItem primaryPriceItem = createMock(PriceItem.class);
    private Object[] mocks = new Object[]{auditReader, dao, obj, family, primaryPriceItem};

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void beforeMethod() {
        reset(mocks);

        tst = new ProductEtl(dao);
        tst.setAuditReaderDao(auditReader);
    }

    public void testEtlFlags() throws Exception {
        expect(obj.getProductId()).andReturn(entityId);
        replay(mocks);

        assertEquals(tst.entityClass, Product.class);
        assertEquals(tst.baseFilename, "product");
        assertEquals(tst.entityId(obj), (Long) entityId);

        verify(mocks);
    }

    public void testCantMakeEtlRecord() throws Exception {
        expect(dao.findById(Product.class, -1L)).andReturn(null);

        replay(mocks);

        assertEquals(tst.dataRecords(etlDateString, false, -1L).size(), 0);

        verify(mocks);
    }

    public void testIncrementalEtl() throws Exception {
        expect(dao.findById(Product.class, entityId)).andReturn(obj);

        expect(obj.getProductId()).andReturn(entityId);
        expect(obj.getProductName()).andReturn(productName);
        expect(obj.getPartNumber()).andReturn(partNumber);
        expect(obj.getAvailabilityDate()).andReturn(availabilityDate);
        expect(obj.getDiscontinuedDate()).andReturn(discontinuedDate);
        expect(obj.getExpectedCycleTimeSeconds()).andReturn(expectedCycleTimeSeconds);
        expect(obj.getGuaranteedCycleTimeSeconds()).andReturn(guaranteedCycleTimeSeconds);
        expect(obj.getSamplesPerWeek()).andReturn(samplesPerWeek);
        expect(obj.isTopLevelProduct()).andReturn(isTopLevelProduct);
        expect(obj.getWorkflow()).andReturn(workflow);
        expect(obj.getProductFamily()).andReturn(family).times(2);
        expect(obj.getPrimaryPriceItem()).andReturn(primaryPriceItem).times(2);

        expect(primaryPriceItem.getPriceItemId()).andReturn(primaryPriceItemId);

        expect(family.getName()).andReturn(productFamilyName);

        replay(mocks);

        Collection<String> records = tst.dataRecords(etlDateString, false, entityId);
        assertEquals(records.size(), 1);

        verifyRecord(records.iterator().next());

        verify(mocks);
    }

    private void verifyRecord(String record) {
        String[] parts = record.split(",");
        int i = 0;
        assertEquals(parts[i++], etlDateString);
        assertEquals(parts[i++], "F");
        assertEquals(parts[i++], String.valueOf(entityId));
        assertEquals(parts[i++], productName);
        assertEquals(parts[i++], partNumber);
        assertEquals(parts[i++], EtlTestUtilities.format(availabilityDate));
        assertEquals(parts[i++], EtlTestUtilities.format(discontinuedDate));
        assertEquals(parts[i++], String.valueOf(expectedCycleTimeSeconds));
        assertEquals(parts[i++], String.valueOf(guaranteedCycleTimeSeconds));
        assertEquals(parts[i++], String.valueOf(samplesPerWeek));
        assertEquals(parts[i++], EtlTestUtilities.format(isTopLevelProduct));
        assertEquals(parts[i++], workflow.getWorkflowName());
        assertEquals(parts[i++], productFamilyName);
        assertEquals(parts[i++], String.valueOf(primaryPriceItemId));
        assertEquals(parts.length, i);
    }
}
