package org.broadinstitute.gpinformatics.athena.control.dao.orders;

import org.broadinstitute.gpinformatics.athena.control.dao.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 10/10/12
 * Time: 4:03 PM
 */
@Test(enabled = false)
public class BillableItemDaoTest  extends ContainerTest {

    @Inject
    ProductOrderSampleDao productOrderSampleDao;
    @Inject
    ResearchProjectDao researchProjectDao;
    @Inject
    ProductOrderDao productOrderDao;

    public static final String TEST_ORDER_TITLE_PREFIX = "TestBillableItem_";

    //TODO hmc need to complete this test
    public void testFindByProductOrderSample() throws Exception {

        // Find a research project in the DB
        ResearchProject firstResearchProjectFound = null;
        List<ResearchProject> projectsList = researchProjectDao.findAllResearchProjects();
        if ((projectsList != null) && (projectsList.get(0) != null)) {
            firstResearchProjectFound = projectsList.get(0);
        } else {
            Assert.fail("Could not find any research Projects in the Database. Need to create an ResearchProject in the DB.");
        }

        // Try to create a Product Order and persist it.
        String testProductOrderTitle = TEST_ORDER_TITLE_PREFIX + UUID.randomUUID();
        //TODO hmc When there are products in the DB can then persist the Product wit the order.
        List<ProductOrderSample> sampleList = new ArrayList<ProductOrderSample>();
        ProductOrder newProductOrder = new ProductOrder(1L, testProductOrderTitle, sampleList, "quoteId", null, firstResearchProjectFound );
        sampleList.add(new ProductOrderSample("MS-1111", newProductOrder));
        sampleList.add(new ProductOrderSample("MS-1112", newProductOrder));
        productOrderDao.persist(newProductOrder);
        productOrderDao.flush();
        productOrderDao.clear();

    }

    //TODO hmc need to complete this test
    public void testFindByProductOrder() throws Exception {

    }
}
