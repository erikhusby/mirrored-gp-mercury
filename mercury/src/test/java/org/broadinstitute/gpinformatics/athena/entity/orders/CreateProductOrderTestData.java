package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.broadinstitute.gpinformatics.athena.control.dao.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.UUID;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 10/11/12
 * Time: 10:05 AM
 */
public class CreateProductOrderTestData extends ContainerTest {

    @Inject
    ResearchProjectDao researchProjectDao;

    @Inject
    ProductOrderDao productOrderDao;

    @Inject
    ProductOrderSampleDao productOrderSampleDao;
    ResearchProject researchProject;
    ProductOrder productOrder;

    public static final String TEST_ORDER_TITLE_PREFIX = "TestProductOrder_";


    @Test(enabled = false, groups = TestGroups.EXTERNAL_INTEGRATION)
    public void createTestData() {

        // create a researchProject
        researchProject = new ResearchProject(1L, "ResearchProject_" + UUID.randomUUID(), "synopsis");
        researchProjectDao.persist(researchProject);



    }
}
