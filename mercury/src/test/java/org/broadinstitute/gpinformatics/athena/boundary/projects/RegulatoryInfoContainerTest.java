package org.broadinstitute.gpinformatics.athena.boundary.projects;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.project.RegulatoryInfo;
import org.broadinstitute.gpinformatics.infrastructure.test.StubbyContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.withdb.ProductOrderDBTestFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.util.Date;

@Test(groups = TestGroups.STUBBY)
@Dependent
public class RegulatoryInfoContainerTest extends StubbyContainerTest {

    public RegulatoryInfoContainerTest(){}

    @Inject
    ProductOrderDao pdoDao;

    public void testRegulatoryInfoRelationship() throws Exception {
        Date today = new Date();
        ProductOrder productOrder = ProductOrderDBTestFactory.createProductOrder(pdoDao, "SM-"+today.getTime());

        RegulatoryInfo regulatoryInfo = new RegulatoryInfo("test Consent" + today.getTime(),
                RegulatoryInfo.Type.ORSP_NOT_HUMAN_SUBJECTS_RESEARCH, "1322" + today.getTime());
        productOrder.getResearchProject().getRegulatoryInfos().add(regulatoryInfo);
        regulatoryInfo = new RegulatoryInfo("test irb" + today.getTime(),
                RegulatoryInfo.Type.IRB, "1322" + today.getTime());
        productOrder.getResearchProject().getRegulatoryInfos().add(regulatoryInfo);
        pdoDao.flush();

        Assert.assertEquals(productOrder.findAvailableRegulatoryInfos().size(), 2);

        productOrder.getRegulatoryInfos().addAll(productOrder.getResearchProject().getRegulatoryInfos());
        pdoDao.flush();
        Assert.assertEquals(productOrder.getRegulatoryInfos().size(), 2);
    }


}
