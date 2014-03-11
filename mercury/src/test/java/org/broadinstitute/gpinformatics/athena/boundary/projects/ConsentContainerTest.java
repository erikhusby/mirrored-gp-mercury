package org.broadinstitute.gpinformatics.athena.boundary.projects;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.project.Consent;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.withdb.ProductOrderDBTestFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.Date;

public class ConsentContainerTest extends ContainerTest {

    @Inject
    ProductOrderDao pdoDao;

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testConsentRelationship() throws Exception {
        ProductOrder productOrder = ProductOrderDBTestFactory.createProductOrder(pdoDao, "SM-test1");

        Date today = new Date();
        Consent consent = new Consent("test Consent" + today.getTime(),
                Consent.Type.ORSP_NOT_HUMAN_SUBJECTS_RESEARCH, "1322" + today.getTime());
        productOrder.getResearchProject().addConsent(consent);

        pdoDao.flush();

        Assert.assertEquals(productOrder.findAvailableConsents().size(), 1);

        productOrder.addConsent(productOrder.getResearchProject().getConsents().toArray(new Consent[productOrder.getResearchProject().getConsents().size()]));
        pdoDao.flush();
        Assert.assertEquals(productOrder.getConsents().size(), 1);
    }


}
