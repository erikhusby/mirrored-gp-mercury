package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.meanbean.lang.EquivalentFactory;
import org.meanbean.test.*;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

/**
 * @author mccrory
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class ProductOrderTest {

    private static final Long TEST_CREATOR = 1111L;

    private static final String PDO_JIRA_KEY = "PDO-1";
    private ProductOrder productOrder;

    @BeforeMethod
    public void setUp() throws Exception {
        productOrder = AthenaClientServiceStub.createDummyProductOrder(PDO_JIRA_KEY);
    }

    @AfterMethod
    public void tearDown() throws Exception {
    }

    @Test
    public void testBeaniness() {

        BeanTester tester = new BeanTester();
        // Current;y ProductOrder is equivalent based only on RP and title.
        Configuration configuration = new ConfigurationBuilder()
                .ignoreProperty("samples")
                .ignoreProperty("title")
                .ignoreProperty("jiraTicketKey")
                .ignoreProperty("orderStatus")
                .ignoreProperty("count")
                .ignoreProperty("modifiedBy")
                .ignoreProperty("createdBy")
                .ignoreProperty("createdDate")
                .ignoreProperty("modifiedDate")
                .ignoreProperty("placedDate")
                .ignoreProperty("quoteId")
                .ignoreProperty("product")
                .ignoreProperty("researchProject")
                .ignoreProperty("comments")
                .ignoreProperty("sampleList").build();
        tester.testBean(ProductOrder.class, configuration);

        class ProductOrderFactory implements EquivalentFactory<ProductOrder> {
            public final long ID = new Random().nextInt(Integer.MAX_VALUE);
            public String title = "title";

            @Override public ProductOrder create() {

                Product product = new Product("Exome Express", null, "Exome Express", "P-EX-0002", new Date(), null,
                        1814400, 1814400, 184, null, null, null, true, "Exome Express", false);
                ResearchProject researchProject = new ResearchProject(ID, title, "RP title", ResearchProject.IRB_NOT_ENGAGED);

                return new ProductOrder(ID, "PO title", sixBspSamplesNoDupes, "quoteId", product,researchProject );
            }
        }

        // Currently ProductOrder is equivalent based only on RP and title.
        new EqualsMethodTester().testEqualsMethod(new ProductOrderFactory(), configuration);

        new HashCodeMethodTester().testHashCodeMethod(new ProductOrderFactory());

    }

    @Test
    public void testOrder() throws Exception {
        Assert.assertEquals(productOrder.fetchJiraIssueType(), CreateFields.IssueType.PRODUCT_ORDER);
        Assert.assertEquals(productOrder.fetchJiraProject(), CreateFields.ProjectType.Product_Ordering);
        Assert.assertNotNull(productOrder.getJiraTicketKey());
        Assert.assertEquals(productOrder.getJiraTicketKey(), PDO_JIRA_KEY);
    }

    private final List<ProductOrderSample> sixBspSamplesNoDupes =
            createDBFreeSampleList("SM-2ACGC","SM-2ABDD","SM-2ACKV","SM-2AB1B","SM-2ACJC","SM-2AD5D");

    private final List<ProductOrderSample> fourBspSamplesWithDupes =
            createDBFreeSampleList("SM-2ACGC","SM-2ABDD","SM-2ACGC","SM-2AB1B","SM-2ACJC","SM-2ACGC");

    private final List<ProductOrderSample> sixMixedSampleProducts =
            createDBFreeSampleList("SM-2ACGC","SM2ABDD","SM2ACKV","SM-2AB1B","SM-2ACJC","SM-2AD5D");

    private final List<ProductOrderSample> nonBspSampleProducts =
            createDBFreeSampleList("SSM-2ACGC1","SM--2ABDDD","SM-2AB","SM-2AB1B","SM-2ACJCACB","SM-SM-SM");

    @Test
    public void testGetUniqueSampleCount() throws Exception {
        productOrder = new ProductOrder(TEST_CREATOR, "title", sixBspSamplesNoDupes, "quote", null, null);
        Assert.assertEquals(productOrder.getUniqueSampleCount(), 6);

        productOrder = new ProductOrder(TEST_CREATOR, "title", fourBspSamplesWithDupes, "quote", null, null);
        Assert.assertEquals(productOrder.getUniqueSampleCount(), 4);
    }

    @Test
    public void testGetTotalSampleCount() throws Exception {
        productOrder = new ProductOrder(TEST_CREATOR, "title", sixBspSamplesNoDupes, "quote", null, null);
        Assert.assertEquals(productOrder.getTotalSampleCount (), 6);

        productOrder = new ProductOrder(TEST_CREATOR, "title", fourBspSamplesWithDupes, "quote", null, null);
        Assert.assertEquals(productOrder.getTotalSampleCount (), 6);
    }

    @Test
    public void testGetDuplicateCount() throws Exception {
        productOrder = new ProductOrder(TEST_CREATOR, "title", fourBspSamplesWithDupes, "quote", null, null);
        Assert.assertEquals(productOrder.getDuplicateCount (), 2);
    }

    @Test
    public void testAreAllSampleBSPFormat() throws Exception {
        productOrder = new ProductOrder(TEST_CREATOR, "title", fourBspSamplesWithDupes, "quote", null, null);
        Assert.assertTrue(productOrder.areAllSampleBSPFormat());

        productOrder = new ProductOrder(TEST_CREATOR, "title", sixBspSamplesNoDupes, "quote", null, null);
        Assert.assertTrue(productOrder.areAllSampleBSPFormat());

        productOrder = new ProductOrder(TEST_CREATOR, "title", nonBspSampleProducts, "quote", null, null);
        Assert.assertFalse(productOrder.areAllSampleBSPFormat());

        productOrder = new ProductOrder(TEST_CREATOR, "title", sixMixedSampleProducts, "quote", null, null);
        Assert.assertFalse ( productOrder.areAllSampleBSPFormat () );
    }

    public static List<ProductOrderSample> createSampleList(String... sampleList) {
        return ProductOrderSampleTest.createSampleList(sampleList, new HashSet<LedgerEntry>(), false);
    }

    public static List<ProductOrderSample> createDBFreeSampleList(String... sampleList) {
        return ProductOrderSampleTest.createSampleList(sampleList, new HashSet<LedgerEntry>(), true);
    }

}
