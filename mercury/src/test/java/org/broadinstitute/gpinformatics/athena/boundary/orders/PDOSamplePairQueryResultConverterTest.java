package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Test(groups = TestGroups.DATABASE_FREE)
public class PDOSamplePairQueryResultConverterTest {

    String sample1 = "123.0";
    String sample2 = "SM-123";
    String pdoKey = "PDO-123";

    private List<ProductOrderSample> pdoSamples;

    private PDOSamplePairs samplePairs;

    @BeforeMethod
    public void setUp() {
        pdoSamples = new ArrayList<>();
        samplePairs = new PDOSamplePairs();
        samplePairs.addPdoSamplePair(pdoKey,sample1,null);
        samplePairs.addPdoSamplePair(pdoKey,sample2,null);

        ProductOrderSample pdoSample1 = new ProductOrderSample(sample1);
        ProductOrder pdo1 = new ProductOrder();
        pdo1.setJiraTicketKey(pdoKey);
        pdo1.addSample(pdoSample1);

        ProductOrderSample pdoSample2 = new ProductOrderSample(sample2);
        ProductOrder pdo2 = new ProductOrder();
        pdo2.setJiraTicketKey(pdoKey);
        pdo2.addSample(pdoSample2);

        pdoSamples.add(pdoSample1);
        pdoSamples.add(pdoSample2);
    }

    public void testAllSamplesAreFound() {
        PDOSamplePairs pdoSamplePairsResult = PDOSamplePairQueryResultConverter.buildOutputPDOSamplePairsFromInputAndQueryResults(samplePairs,pdoSamples);
        Assert.assertEquals(pdoSamplePairsResult.getPdoSamplePairs().size(),2);
        Assert.assertTrue(pdoSamplePairsResult.getErrors().isEmpty());
        for (ProductOrderSample pdoSample : pdoSamples) {
            Assert.assertTrue(doesPdoSamplePairContainSample(pdoSamplePairsResult, pdoSample.getName()));
        }
    }

    private boolean doesPdoSamplePairContainSample(PDOSamplePairs pdoSamplePairs,String sampleName) {
        boolean foundIt = false;
        for (PDOSamplePair pdoSamplePair : pdoSamplePairs.getPdoSamplePairs()) {
            if (sampleName.equals(pdoSamplePair.getSampleName())) {
                foundIt = true;
            }
        }
        return foundIt;
    }

    public void testSomeSamplesAreNotFound() {
        samplePairs.addPdoSamplePair("PDO-NOTFOUND","SM-NOTTHERE",null);

        PDOSamplePairs pdoSamplePairsResult = PDOSamplePairQueryResultConverter.buildOutputPDOSamplePairsFromInputAndQueryResults(samplePairs,pdoSamples);
        Assert.assertEquals(pdoSamplePairsResult.getPdoSamplePairs().size(),3);
        Assert.assertEquals(pdoSamplePairsResult.getErrors().size(), 1);
        for (ProductOrderSample pdoSample : pdoSamples) {
            Assert.assertTrue(doesPdoSamplePairContainSample(pdoSamplePairsResult,pdoSample.getName()));
         }
    }
}
