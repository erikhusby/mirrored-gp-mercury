package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Test(groups = TestGroups.DATABASE_FREE)
public class PDOSamplesTest {

    String sample1 = "123.0";
    String sample2 = "SM-1234";
    String pdoKey = "PDO-123";
    String pdoKey2 = "PDO-555";

    private List<ProductOrderSample> pdoSamplesList;

    private PDOSamples pdoSamples;

    private Product riskyProduct;

    @BeforeMethod
    public void setUp() {
        pdoSamplesList = new ArrayList<>();
        pdoSamples = new PDOSamples();
        Date receiptDate = new Date();
        pdoSamples.addPdoSample(pdoKey, sample1, null, null, receiptDate);
        pdoSamples.addPdoSample(pdoKey, sample2, null, null, receiptDate);

        ProductOrderSample pdoSample1 = new ProductOrderSample(sample1, new BspSampleData());
        Product dummyProduct = ProductTestFactory.createDummyProduct(Workflow.AGILENT_EXOME_EXPRESS, "partNumber");
        riskyProduct = ProductTestFactory.createDummyProduct(Workflow.AGILENT_EXOME_EXPRESS, "partNumber", true, false);
        ProductOrder pdo1 = new ProductOrder(ResearchProjectTestFactory.TEST_CREATOR, "containerTest Product Order Test1",
                Arrays.asList(pdoSample1),
                        "newQuote", dummyProduct,
                new ResearchProject(ResearchProjectTestFactory.TEST_CREATOR, null, "test research project", true,
                                    ResearchProject.RegulatoryDesignation.RESEARCH_ONLY));
        pdo1.setJiraTicketKey(pdoKey);
        pdoSample1.calculateRisk();

        pdo1.addSample(pdoSample1);

        ProductOrderSample pdoSample2 = new ProductOrderSample(sample2, new BspSampleData());
        ProductOrder pdo2 = new ProductOrder(ResearchProjectTestFactory.TEST_CREATOR, "containerTest Product Order Test2",
                        Arrays.asList(pdoSample2),
                                "newQuote", riskyProduct,
                        new ResearchProject(ResearchProjectTestFactory.TEST_CREATOR, null, "test research project", true,
                                            ResearchProject.RegulatoryDesignation.RESEARCH_ONLY));
        pdo2.setJiraTicketKey(pdoKey);
        pdoSample2.calculateRisk();
        pdo2.addSample(pdoSample2);

        pdoSamplesList.add(pdoSample1);
        pdoSamplesList.add(pdoSample2);
    }

    public void testAllSamplesAreFound() {
        PDOSamples pdoSamplesResult = pdoSamples.buildOutputPDOSamplePairsFromInputAndQueryResults(pdoSamplesList);
        Assert.assertEquals(pdoSamplesResult.getPdoSamples().size(),2);
        Assert.assertTrue(pdoSamplesResult.getErrors().isEmpty());
        for (ProductOrderSample pdoSample : pdoSamplesList) {
            Assert.assertTrue(doesPdoSamplePairContainSample(pdoSamplesResult, pdoSample.getName()));
        }
    }

    public void testFindAtRiskPDOSamplesDaoFree() {
        PDOSamples atRiskPDOSamples = pdoSamples.buildOutputPDOSamplePairsFromInputAndQueryResults(pdoSamplesList);
        Assert.assertTrue(!atRiskPDOSamples.getPdoSamples().isEmpty());
        Assert.assertEquals(atRiskPDOSamples.getAtRiskPdoSamples().size(), 1);
        Assert.assertEquals(atRiskPDOSamples.getAtRiskPdoSamples().iterator().next().getRiskCategories().size(),1,"The risk categorized samples field in LCSET tickets is going to be filled in improperly.");
        String riskCategory = atRiskPDOSamples.getAtRiskPdoSamples().iterator().next().getRiskCategories().iterator().next();
        Assert.assertEquals(riskCategory,riskyProduct.getRiskCriteria().iterator().next().getCalculationString(),"The risk categorized samples field in LCSET tickets is not going to show the right value.");
    }

    private boolean doesPdoSamplePairContainSample(PDOSamples pdoSamples,String sampleName) {
        boolean foundIt = false;
        for (PDOSample pdoSample : pdoSamples.getPdoSamples()) {
            if (sampleName.equals(pdoSample.getSampleName())) {
                foundIt = true;
            }
        }
        return foundIt;
    }

    public void testSomeSamplesAreNotFound() {
        pdoSamples.addPdoSample("PDO-NOTFOUND", "SM-NOTTHERE", null, null, new Date());

        PDOSamples pdoSamplesResult = pdoSamples.buildOutputPDOSamplePairsFromInputAndQueryResults(pdoSamplesList);
        Assert.assertEquals(pdoSamplesResult.getPdoSamples().size(),3);
        Assert.assertEquals(pdoSamplesResult.getErrors().size(), 1);
        for (ProductOrderSample pdoSample : pdoSamplesList) {
            Assert.assertTrue(doesPdoSamplePairContainSample(pdoSamplesResult,pdoSample.getName()));
         }
    }

    /**
     * This test covers the "double coverage" scenario where
     * we have the same sample in a PDO more than once.  In that
     * situation, we want to make sure that we return
     * both pdo/sample pairs.
     */
    public void testSameSampleInPdoMoreThanOnce() {
        ProductOrderSample duplicatePDOSample = pdoSamplesList.iterator().next();
        pdoSamplesList.add(duplicatePDOSample);
        PDOSamples pdoSamplesResult = pdoSamples.buildOutputPDOSamplePairsFromInputAndQueryResults(pdoSamplesList);
        Assert.assertEquals(pdoSamplesResult.getPdoSamples().size(),3);

        int numOccurrences = 0;
        for (PDOSample pdoSample : pdoSamplesResult.getPdoSamples()) {
            if (pdoSample.getSampleName().equals(duplicatePDOSample.getName()) && pdoSample.getPdoKey().equals(duplicatePDOSample.getProductOrder().getBusinessKey())) {
                numOccurrences++;
            }
        }
        Assert.assertEquals(numOccurrences,2,"PDOs that have the same sample multiple times are not accounted for properly.");
    }

    public void testListToMapConversion() {
        pdoSamples.addPdoSample(pdoKey2, sample1, null, null, new Date());
        Map<String,Set<String>> pdoToSamples = pdoSamples.convertPdoSamplePairsListToMap();
        Assert.assertEquals(pdoToSamples.keySet().size(),2);
        Assert.assertTrue(pdoToSamples.containsKey(pdoKey));
        Assert.assertEquals(pdoToSamples.get(pdoKey).size(),2);
        Assert.assertEquals(pdoToSamples.get(pdoKey2).size(),1);
        Assert.assertTrue(pdoToSamples.get(pdoKey2).contains(sample1));
        Assert.assertTrue(pdoToSamples.get(pdoKey).contains(sample1));
        Assert.assertTrue(pdoToSamples.get(pdoKey).contains(sample2));
    }
}
