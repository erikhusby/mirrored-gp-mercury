package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Test(groups = TestGroups.DATABASE_FREE)
public class PDOSamplePairUtilsTest {

    String sample1 = "123.0";
    String sample2 = "SM-123";
    String pdoKey = "PDO-123";
    String pdoKey2 = "PDO-555";

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
        PDOSamplePairs pdoSamplePairsResult = PDOSamplePairUtils
                .buildOutputPDOSamplePairsFromInputAndQueryResults(samplePairs, pdoSamples);
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

        PDOSamplePairs pdoSamplePairsResult = PDOSamplePairUtils
                .buildOutputPDOSamplePairsFromInputAndQueryResults(samplePairs, pdoSamples);
        Assert.assertEquals(pdoSamplePairsResult.getPdoSamplePairs().size(),3);
        Assert.assertEquals(pdoSamplePairsResult.getErrors().size(), 1);
        for (ProductOrderSample pdoSample : pdoSamples) {
            Assert.assertTrue(doesPdoSamplePairContainSample(pdoSamplePairsResult,pdoSample.getName()));
         }
    }

    /**
     * This test covers the "double coverage" scenario where
     * we have the same sample in a PDO more than once.  In that
     * situation, we want to make sure that we return
     * both pdo/sample pairs.
     */
    public void testSameSampleInPdoMoreThanOnce() {
        ProductOrderSample duplicatePDOSample = pdoSamples.iterator().next();
        pdoSamples.add(duplicatePDOSample);
        PDOSamplePairs pdoSamplePairsResult = PDOSamplePairUtils
                .buildOutputPDOSamplePairsFromInputAndQueryResults(samplePairs, pdoSamples);
        Assert.assertEquals(pdoSamplePairsResult.getPdoSamplePairs().size(),3);

        int numOccurrences = 0;
        for (PDOSamplePair pdoSamplePair : pdoSamplePairsResult.getPdoSamplePairs()) {
            if (pdoSamplePair.getSampleName().equals(duplicatePDOSample.getName()) && pdoSamplePair.getPdoKey().equals(duplicatePDOSample.getProductOrder().getBusinessKey())) {
                numOccurrences++;
            }
        }
        Assert.assertEquals(numOccurrences,2,"PDOs that have the same sample multiple times are not accounted for properly.");
    }

    public void testListToMapConversion() {
        samplePairs.addPdoSamplePair(pdoKey2,sample1,null);
        Map<String,Set<String>> pdoToSamples = PDOSamplePairUtils.convertPdoSamplePairsListToMap(samplePairs);
        Assert.assertEquals(pdoToSamples.keySet().size(),2);
        Assert.assertTrue(pdoToSamples.containsKey(pdoKey));
        Assert.assertEquals(pdoToSamples.get(pdoKey).size(),2);
        Assert.assertEquals(pdoToSamples.get(pdoKey2).size(),1);
        Assert.assertTrue(pdoToSamples.get(pdoKey2).contains(sample1));
        Assert.assertTrue(pdoToSamples.get(pdoKey).contains(sample1));
        Assert.assertTrue(pdoToSamples.get(pdoKey).contains(sample2));
    }
}
