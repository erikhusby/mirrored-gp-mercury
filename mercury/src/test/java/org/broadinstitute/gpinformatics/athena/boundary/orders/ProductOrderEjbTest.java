package org.broadinstitute.gpinformatics.athena.boundary.orders;

import edu.mit.broad.bsp.core.datavo.workrequest.items.kit.PostReceiveOption;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderKitTest;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderKit;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderKitDetail;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchService;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceList;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quote;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteNotFoundException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuotePriceItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServerException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServiceImpl;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quotes;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.workrequest.KitType;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.withdb.ProductOrderDBTestFactory;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Test(groups = TestGroups.DATABASE_FREE)
public class ProductOrderEjbTest {

    public static final String ALIQUOT_ID_1 = "SM-ALQT1";
    public static final String ALIQUOT_ID_2 = "SM-ALQT2";
    public static final String STOCK_ID = "SM-STOCK";

    private UserBean mockUserBean = Mockito.mock(UserBean.class);
    private ProductOrderDao productOrderDaoMock = Mockito.mock(ProductOrderDao.class);
    ProductOrderEjb productOrderEjb = new ProductOrderEjb(productOrderDaoMock, null, null, null, mockUserBean, null, null,
            new BSPSampleDataFetcher(new BSPSampleSearchService() {
                @Override
                public List<Map<BSPSampleSearchColumn, String>> runSampleSearch(final Collection<String> sampleIDs,
                                                                                BSPSampleSearchColumn... resultColumns) {
                    // For this test case, both aliquots map to the same sample.
                    return new ArrayList<Map<BSPSampleSearchColumn, String>>() {{
                        add(new EnumMap<BSPSampleSearchColumn, String>(BSPSampleSearchColumn.class) {{
                            put(BSPSampleSearchColumn.SAMPLE_ID, sampleIDs.iterator().next());
                            put(BSPSampleSearchColumn.STOCK_SAMPLE, STOCK_ID);
                            put(BSPSampleSearchColumn.SAMPLE_ID, sampleIDs.iterator().next());
                        }});
                    }};
                }
            }), null);

    public void testMapAliquotIdToSampleOne() throws Exception {
        ProductOrder order = ProductOrderDBTestFactory.createTestProductOrder(STOCK_ID);

        // Test case where sample has not yet been mapped to an aliquot.
        List<ProductOrderSample> samples = order.getSamples();
        Assert.assertTrue(samples.size() == 1);
        Assert.assertTrue(samples.get(0).getAliquotId() == null);

        // Now map it.
        ProductOrderSample sample = productOrderEjb.mapAliquotIdToSample(order, ALIQUOT_ID_1);
        Assert.assertNotNull(sample);
        Assert.assertEquals(sample.getAliquotId(), ALIQUOT_ID_1);

        // Test case where sample has already been mapped, should return same sample again.
        sample = productOrderEjb.mapAliquotIdToSample(order, ALIQUOT_ID_1);
        Assert.assertNotNull(sample);
        Assert.assertEquals(sample.getAliquotId(), ALIQUOT_ID_1);

        // Try to map another aliquot, should get an exception.
        try {
            sample = productOrderEjb.mapAliquotIdToSample(order, ALIQUOT_ID_2);
            Assert.fail("Exception should be thrown");
        } catch (Exception e) {
            // Error is expected.
        }
    }

    public void testMapAliquotToSampleTwo() throws Exception {
        // Test case where there are multiple samples, where each one maps to a different aliquot.

        ProductOrder order = ProductOrderDBTestFactory.createTestProductOrder(STOCK_ID, STOCK_ID);
        ProductOrderSample sample = productOrderEjb.mapAliquotIdToSample(order, ALIQUOT_ID_1);
        ProductOrderSample sample2 = productOrderEjb.mapAliquotIdToSample(order, ALIQUOT_ID_2);
        Assert.assertEquals(sample.getAliquotId(), ALIQUOT_ID_1);
        Assert.assertEquals(sample2.getAliquotId(), ALIQUOT_ID_2);
    }

    public void testUpdateKitInfo() throws Exception {

        Mockito.when(mockUserBean.getBspUser()).thenReturn(new BSPUserList.QADudeUser("PM", 2423L));

        ProductOrder productOrder = ProductOrderDBTestFactory.createTestProductOrder(
                ResearchProjectTestFactory.createTestResearchProject(),
                ProductTestFactory.createDummyProduct(Workflow.NONE, Product.SAMPLE_INITIATION_PART_NUMBER));

        Set<ProductOrderKitDetail> originalKitDetailSet = new HashSet<>();
        ProductOrderKitDetail kitDetail = new ProductOrderKitDetail(5L, KitType.DNA_MATRIX, 87L,
                ProductOrderKitTest.materialInfoDto);
        kitDetail.getPostReceiveOptions().addAll(Collections.singleton(PostReceiveOption.PICO_RECEIVED));
        kitDetail.setProductOrderKitDetaild(4243L);

        ProductOrderKitDetail kitDetailToDelete = new ProductOrderKitDetail(4L, KitType.DNA_MATRIX, 187L,
                ProductOrderKitTest.materialInfoDto);
        kitDetailToDelete.getPostReceiveOptions().addAll(Collections.singleton(PostReceiveOption.PICO_RECEIVED));
        kitDetailToDelete.setProductOrderKitDetaild(2243L);
        originalKitDetailSet.add(kitDetail);
        originalKitDetailSet.add(kitDetailToDelete);



        ProductOrderKit orderKit = new ProductOrderKit(33L, 44L);
        orderKit.setKitOrderDetails(originalKitDetailSet);

        Set<ProductOrderKitDetail> kitDetailSet = new HashSet<>();

        ProductOrderKitDetail kitDetailChange1 = new ProductOrderKitDetail(6L, KitType.DNA_MATRIX, 88L,
                ProductOrderKitTest.materialInfoDto);
        kitDetailChange1.getPostReceiveOptions().addAll(Collections.singleton(PostReceiveOption.FLUIDIGM_FINGERPRINTING));
        kitDetailChange1.getPostReceiveOptions().addAll(Collections.singleton(PostReceiveOption.BIOANALYZER));
//        kitDetail.setProductOrderKitDetaild(3243L);

        ProductOrderKitDetail kitDetailChange2 = new ProductOrderKitDetail(7L, KitType.DNA_MATRIX, 89L,
                ProductOrderKitTest.materialInfoDto);
        kitDetailChange2.getPostReceiveOptions().addAll(Collections.singleton(PostReceiveOption.FLUIDIGM_FINGERPRINTING));
        kitDetailChange2.getPostReceiveOptions().addAll(Collections.singleton(PostReceiveOption.BIOANALYZER));
        kitDetailChange2.setProductOrderKitDetaild(4243L);

        kitDetailSet.add(kitDetailChange1);
        kitDetailSet.add(null);
        kitDetailSet.add(kitDetailChange2);

        productOrderEjb.persistProductOrder(ProductOrder.SaveType.UPDATING, productOrder,Collections.singleton("2243"),
                kitDetailSet);

        Assert.assertEquals(productOrder.getProductOrderKit().getKitOrderDetails().size(), 2);
        for(ProductOrderKitDetail kitDetailToTest:productOrder.getProductOrderKit().getKitOrderDetails()) {

            Assert.assertNotEquals(kitDetailToTest.getProductOrderKitDetaild(), 2243L);

            if(kitDetailToTest.getProductOrderKitDetaild() != null &&
               kitDetailToTest.getProductOrderKitDetaild().equals(4243L)) {
                Assert.assertEquals((Long)kitDetailToTest.getOrganismId(), (Long)89L);
                Assert.assertEquals((Long)kitDetailToTest.getNumberOfSamples(), (Long)7L);
            } else {
                Assert.assertEquals((Long)kitDetailToTest.getOrganismId(), (Long)88L);
                Assert.assertEquals((Long)kitDetailToTest.getNumberOfSamples(), (Long)6L);

            }

            Assert.assertEquals(kitDetailToTest.getPostReceiveOptions().size(), 2);

        }

    }

    public void testCreatePDOUpdateFieldForQuote() {
        ProductOrder pdo = new ProductOrder();
        pdo.setQuoteId("DOUGH");
        ProductOrderEjb pdoEjb = new ProductOrderEjb();
        PDOUpdateField updateField = PDOUpdateField.createPDOUpdateFieldForQuote(pdo);

        Assert.assertEquals(updateField.getNewValue(),pdo.getQuoteId());

        pdo.setQuoteId(null);

        updateField = PDOUpdateField.createPDOUpdateFieldForQuote(pdo);
        Assert.assertEquals(updateField.getNewValue(),ProductOrder.QUOTE_TEXT_USED_IN_JIRA_WHEN_QUOTE_FIELD_IS_EMPTY);
    }

    public void testValidateQuote() throws Exception {
        ProductOrder pdo = new ProductOrder();
        pdo.setQuoteId("CASH");
        ProductOrderEjb pdoEjb = new ProductOrderEjb();
        pdoEjb.validateQuote(pdo, new QuoteServiceStub());
    }
}
