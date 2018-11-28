package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.Date;


/**
 * dbfree unit test of entity etl.
 *
 * @author epolk
 */

@Test(groups = TestGroups.DATABASE_FREE)
public class ProductEtlDbFreeTest {
    private final String etlDateString = ExtractTransform.formatTimestamp(new Date());
    private static final long ENTITY_ID = 1122334455L;
    private static final String PRODUCT_NAME = "Test Product";
    private static final String PART_NUMBER = "TestNumber-5544";
    private static final Date AVAILABILITY_DATE = new Date(1350000000000L);
    private static final Date DISCONTINUED_DATE = new Date(1380000000000L);
    private static final int EXPECTED_CYCLE_TIME_SECONDS = 5400;
    private static final int GUARANTEED_CYCLE_TIME_SECONDS = 99999;
    private static final int SAMPLES_PER_WEEK = 200;
    private static final boolean IS_TOP_LEVEL_PRODUCT = true;
    private static final String WORKFLOW = Workflow.AGILENT_EXOME_EXPRESS;
    private static final String PRODUCT_FAMILY_NAME = "Test ProductFamily";
    private static final long PRIMARY_PRICE_ITEM_ID = 987654321L;
    private static final String AGGREGATION_DATA_TYPE = "Exome";
    private static final boolean commercialIndicator = true;
    private static final boolean savedInSapIndicator = true;

    private ProductEtl productEtl;

    private final AuditReaderDao auditReader = EasyMock.createMock(AuditReaderDao.class);

    private final Product product = EasyMock.createMock(Product.class);
    private final ProductDao productDao = EasyMock.createMock(ProductDao.class);
    private final ProductFamily family = EasyMock.createMock(ProductFamily.class);
    private final PriceItem primaryPriceItem = EasyMock.createMock(PriceItem.class);
    private final Object[] mocks = new Object[]{auditReader, productDao, product, family, primaryPriceItem};

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void beforeMethod() {
        EasyMock.reset(mocks);

        productEtl = new ProductEtl(productDao);
        productEtl.setAuditReaderDao(auditReader);
    }

    public void testEtlFlags() throws Exception {
        EasyMock.expect(product.getProductId()).andReturn(ENTITY_ID);
        EasyMock.replay(mocks);

        Assert.assertEquals(productEtl.entityClass, Product.class);
        Assert.assertEquals(productEtl.baseFilename, "product");
        Assert.assertEquals(productEtl.entityId(product), (Long) ENTITY_ID);

        EasyMock.verify(mocks);
    }

    public void testCantMakeEtlRecord() throws Exception {
        EasyMock.expect(productDao.findById(Product.class, -1L)).andReturn(null);

        EasyMock.replay(mocks);

        Assert.assertEquals(productEtl.dataRecords(etlDateString, false, -1L).size(), 0);

        EasyMock.verify(mocks);
    }

    public void testIncrementalEtl() throws Exception {
        EasyMock.expect(productDao.findById(Product.class, ENTITY_ID)).andReturn(product);

        EasyMock.expect(product.getProductId()).andReturn(ENTITY_ID);
        EasyMock.expect(product.getProductName()).andReturn(PRODUCT_NAME);
        EasyMock.expect(product.getPartNumber()).andReturn(PART_NUMBER);
        EasyMock.expect(product.getAvailabilityDate()).andReturn(AVAILABILITY_DATE);
        EasyMock.expect(product.getDiscontinuedDate()).andReturn(DISCONTINUED_DATE);
        EasyMock.expect(product.getExpectedCycleTimeSeconds()).andReturn(EXPECTED_CYCLE_TIME_SECONDS);
        EasyMock.expect(product.getGuaranteedCycleTimeSeconds()).andReturn(GUARANTEED_CYCLE_TIME_SECONDS);
        EasyMock.expect(product.getSamplesPerWeek()).andReturn(SAMPLES_PER_WEEK);
        EasyMock.expect(product.isTopLevelProduct()).andReturn(IS_TOP_LEVEL_PRODUCT);
        EasyMock.expect(product.getWorkflowName()).andReturn(WORKFLOW).anyTimes();
        EasyMock.expect(product.getProductFamily()).andReturn(family).anyTimes();
        EasyMock.expect(product.getPrimaryPriceItem()).andReturn(primaryPriceItem).anyTimes();
        EasyMock.expect(product.getAggregationDataType()).andReturn(AGGREGATION_DATA_TYPE).anyTimes();

        EasyMock.expect(primaryPriceItem.getPriceItemId()).andReturn(PRIMARY_PRICE_ITEM_ID);

        EasyMock.expect(family.getName()).andReturn(PRODUCT_FAMILY_NAME);

        EasyMock.expect(product.isExternalOnlyProduct()).andReturn(commercialIndicator);
        EasyMock.expect(product.isSavedInSAP()).andReturn(savedInSapIndicator);

        EasyMock.replay(mocks);

        Collection<String> records = productEtl.dataRecords(etlDateString, false, ENTITY_ID);
        Assert.assertEquals(records.size(), 1);

        verifyRecord(records.iterator().next());

        EasyMock.verify(mocks);
    }

    private void verifyRecord(String record) {
        String[] parts = record.split(",");
        int i = 0;
        Assert.assertEquals(parts[i++], etlDateString);
        Assert.assertEquals(parts[i++], "F");
        Assert.assertEquals(parts[i++], String.valueOf(ENTITY_ID));
        Assert.assertEquals(parts[i++], PRODUCT_NAME);
        Assert.assertEquals(parts[i++], PART_NUMBER);
        Assert.assertEquals(parts[i++], EtlTestUtilities.format(AVAILABILITY_DATE));
        Assert.assertEquals(parts[i++], EtlTestUtilities.format(DISCONTINUED_DATE));
        Assert.assertEquals(parts[i++], String.valueOf(EXPECTED_CYCLE_TIME_SECONDS));
        Assert.assertEquals(parts[i++], String.valueOf(GUARANTEED_CYCLE_TIME_SECONDS));
        Assert.assertEquals(parts[i++], String.valueOf(SAMPLES_PER_WEEK));
        Assert.assertEquals(parts[i++], EtlTestUtilities.format(IS_TOP_LEVEL_PRODUCT));
        Assert.assertEquals(parts[i++], WORKFLOW);
        Assert.assertEquals(parts[i++], PRODUCT_FAMILY_NAME);
        Assert.assertEquals(parts[i++], String.valueOf(PRIMARY_PRICE_ITEM_ID));
        Assert.assertEquals(parts[i++], String.valueOf(AGGREGATION_DATA_TYPE));
        Assert.assertEquals(parts[i++], EtlTestUtilities.format(commercialIndicator));
        Assert.assertEquals(parts[i++], EtlTestUtilities.format(savedInSapIndicator));
        Assert.assertEquals(parts.length, 17);
    }
}
