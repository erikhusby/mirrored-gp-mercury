package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Uses a DAO that throws exceptions to confirm the
 * exception handling of the pdoSampleBillingStatus web service.
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class PdoSampleBillingExceptionHandlingTest {

    private static final String EXCEPTION_TEXT = "Boom!";

    private ProductOrderSampleDao createPDOSampleDaoThatThrowsAnException() {
        ProductOrderSampleDao pdoSampleDao = EasyMock.createMock(ProductOrderSampleDao.class);
        EasyMock.expect(pdoSampleDao.findByOrderKeyAndSampleNames((String)EasyMock.anyObject(),(Set<String>)EasyMock.anyObject())).andThrow(
                new RuntimeException(EXCEPTION_TEXT)).once();
        return pdoSampleDao;
    }


    @Test(groups = TestGroups.DATABASE_FREE)
    public void testThatAnExceptionThrownInTheWebServiceIsCaughtAndAddedToTheListOfErrors() {
        ProductOrderSampleDao mockDao = createPDOSampleDaoThatThrowsAnException();
        EasyMock.replay(mockDao);
        List<PDOSample> pdoSamplesList = new ArrayList<>();
        PDOSample pdoSample1 = new PDOSample("PDO-872", "SM-47KKU",null, new Date());
        pdoSamplesList.add(pdoSample1);

        PDOSamples pdoSamples = new PDOSamples();
        pdoSamples.setPdoSamples(pdoSamplesList);

        ProductOrderResource pdoResource = new ProductOrderResource();
        pdoResource.setProductOrderSampleDao(mockDao);

        PDOSamples returnedPDOSamples = pdoResource.getPdoSampleStatus(pdoSamples);

        Assert.assertTrue(returnedPDOSamples.getPdoSamples().isEmpty());
        Assert.assertEquals(returnedPDOSamples.getErrors().size(),1);
        Assert.assertEquals(returnedPDOSamples.getErrors().iterator().next(),EXCEPTION_TEXT);

        EasyMock.verify(mockDao);
    }
}
