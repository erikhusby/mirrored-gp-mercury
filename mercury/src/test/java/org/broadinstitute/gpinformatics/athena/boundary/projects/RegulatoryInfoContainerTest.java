package org.broadinstitute.gpinformatics.athena.boundary.projects;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.project.RegulatoryInfo;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.withdb.ProductOrderDBTestFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.Date;

@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class RegulatoryInfoContainerTest extends ContainerTest {

    @Inject
    ProductOrderDao pdoDao;

    public void testRegulatoryInfoRelationship() throws Exception {
        ProductOrder productOrder = ProductOrderDBTestFactory.createProductOrder(pdoDao, "SM-test1");

        Date today = new Date();
        RegulatoryInfo regulatoryInfo = new RegulatoryInfo("test Consent" + today.getTime(),
                RegulatoryInfo.Type.ORSP_NOT_HUMAN_SUBJECTS_RESEARCH, "1322" + today.getTime());
        productOrder.getResearchProject().addRegulatoryInfo(regulatoryInfo);

        pdoDao.flush();

        Assert.assertEquals(productOrder.findAvailableRegulatoryInfos().size(), 1);

        productOrder.addRegulatoryInfo(productOrder.getResearchProject().getRegulatoryInfos()
                .toArray(new RegulatoryInfo[productOrder.getResearchProject().getRegulatoryInfos().size()]));
        pdoDao.flush();
        Assert.assertEquals(productOrder.getRegulatoryInfos().size(), 1);
    }


}
