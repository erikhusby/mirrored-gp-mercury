package org.broadinstitute.gpinformatics.athena.boundary.orders;

import edu.mit.broad.bsp.core.datavo.workrequest.items.kit.PostReceiveOption;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderKitTest;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderKit;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderKitDetail;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.workrequest.KitType;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.withdb.ProductOrderDBTestFactory;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Test(groups = TestGroups.DATABASE_FREE)
public class ProductOrderEjbTest {

    public static final String ALIQUOT_ID_1 = "SM-ALQT1";
    public static final String ALIQUOT_ID_2 = "SM-ALQT2";
    public static final String STOCK_ID = "SM-STOCK";

    private UserBean mockUserBean = Mockito.mock(UserBean.class);
    private ProductOrderDao productOrderDaoMock = Mockito.mock(ProductOrderDao.class);
    ProductOrderEjb productOrderEjb = new ProductOrderEjb(productOrderDaoMock, null, null, null, mockUserBean, null,
            null, null
    );


    public void testUpdateKitInfo() throws Exception {

        Mockito.when(mockUserBean.getBspUser()).thenReturn(new BSPUserList.QADudeUser("PM", 2423L));

        ProductOrder productOrder = ProductOrderDBTestFactory.createTestProductOrder(
                ResearchProjectTestFactory.createTestResearchProject(),
                ProductTestFactory.createDummyProduct(Workflow.NONE, Product.SAMPLE_INITIATION_PART_NUMBER));

        Set<ProductOrderKitDetail> originalKitDetailSet = new HashSet<>();
        ProductOrderKitDetail kitDetail = new ProductOrderKitDetail(5L, KitType.DNA_MATRIX, 87L,
                ProductOrderKitTest.materialInfoDto);
        kitDetail.getPostReceiveOptions().addAll(Collections.singleton(PostReceiveOption.PICO_RECEIVED));
        kitDetail.setProductOrderKitDetailId(4243L);

        ProductOrderKitDetail kitDetailToDelete = new ProductOrderKitDetail(4L, KitType.DNA_MATRIX, 187L,
                ProductOrderKitTest.materialInfoDto);
        kitDetailToDelete.getPostReceiveOptions().addAll(Collections.singleton(PostReceiveOption.PICO_RECEIVED));
        kitDetailToDelete.setProductOrderKitDetailId(2243L);
        originalKitDetailSet.add(kitDetail);
        originalKitDetailSet.add(kitDetailToDelete);

        ProductOrderKit orderKit = new ProductOrderKit(33L, 44L);
        orderKit.setKitOrderDetails(originalKitDetailSet);
        productOrder.setProductOrderKit(orderKit);

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
        kitDetailChange2.setProductOrderKitDetailId(4243L);

        kitDetailSet.add(kitDetailChange1);
        kitDetailSet.add(null);
        kitDetailSet.add(kitDetailChange2);

        productOrderEjb.persistProductOrder(ProductOrder.SaveType.UPDATING, productOrder,Collections.singleton("2243"),
                kitDetailSet);

        Assert.assertEquals(productOrder.getProductOrderKit().getKitOrderDetails().size(), 2);
        for(ProductOrderKitDetail kitDetailToTest:productOrder.getProductOrderKit().getKitOrderDetails()) {

            Assert.assertNotEquals(kitDetailToTest.getProductOrderKitDetailId(), 2243L);

            if(kitDetailToTest.getProductOrderKitDetailId() != null &&
               kitDetailToTest.getProductOrderKitDetailId().equals(4243L)) {
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
