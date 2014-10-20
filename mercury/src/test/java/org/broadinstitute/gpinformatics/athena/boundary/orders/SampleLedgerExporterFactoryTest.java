package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.RiskCriterion;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.broadinstitute.gpinformatics.PatternMatcher.pattern;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link SampleLedgerExporterFactory} ability to gather all of the data needed to write rows to a billing
 * tracker.
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class SampleLedgerExporterFactoryTest {

    public static final String TEST_PRODUCT_NAME = "Test Product";
    public static final String TEST_PRODUCT_ORDER_TITLE = "SampleLedgerExporterFactoryTest";
    public static final long PRODUCT_ORDER_CREATOR_ID = 1;
    public static final long RESEARCH_PROJECT_CREATOR_ID = 2;

    private ProductOrder productOrder;
    private SampleLedgerExporterFactory factory;
    private BspUser productOrderCreator;

    @BeforeMethod
    public void setUp() throws Exception {
        BSPUserList mockBspUserList = Mockito.mock(BSPUserList.class);
        productOrderCreator = new BspUser();
        productOrderCreator.setFirstName("Test");
        productOrderCreator.setLastName("Dummy");
        when(mockBspUserList.getById(PRODUCT_ORDER_CREATOR_ID)).thenReturn(productOrderCreator);
        when(mockBspUserList.getUserFullName(PRODUCT_ORDER_CREATOR_ID)).thenReturn(productOrderCreator.getFullName());

        Product product = new Product(Product.TOP_LEVEL_PRODUCT);
        product.setProductName(TEST_PRODUCT_NAME);

        ResearchProject researchProject =
                new ResearchProject(RESEARCH_PROJECT_CREATOR_ID, "Test Project", "Test", true,
                                    ResearchProject.RegulatoryDesignation.RESEARCH_ONLY);

        productOrder = new ProductOrder(PRODUCT_ORDER_CREATOR_ID, TEST_PRODUCT_ORDER_TITLE,
                Collections.<ProductOrderSample>emptyList(), "QUOTE-1", product, researchProject);
        factory = new SampleLedgerExporterFactory(null, mockBspUserList, null, null, null, null, null, null);
    }

    public void gatherNoSamplesForEmptyProductOrder() {
        List<SampleLedgerRow> data = factory.gatherSampleRowData(productOrder);
        assertThat(data.size(), equalTo(0));
    }

    public void gatherSampleFromProductOrderWithOneSample() {
        String sampleId = "SM-1234";
        productOrder.addSample(new ProductOrderSample(sampleId, new BspSampleData()));

        List<SampleLedgerRow> data = factory.gatherSampleRowData(productOrder);
        assertThat(data.size(), equalTo(1));
        assertThat(data.get(0).getSampleId(), equalTo(sampleId));
    }

    public void gatherSamplesFromProductOrderWithTwoSamples() {
        String sampleId1 = "SM-1234";
        String sampleId2 = "SM-5678";
        productOrder.addSample(new ProductOrderSample(sampleId1, new BspSampleData()));
        productOrder.addSample(new ProductOrderSample(sampleId2, new BspSampleData()));

        List<SampleLedgerRow> data = factory.gatherSampleRowData(productOrder);
        assertThat(data.size(), equalTo(2));
        assertThat(data.get(0).getSampleId(), equalTo(sampleId1));
        assertThat(data.get(1).getSampleId(), equalTo(sampleId2));
    }

    public void gatherCollaboratorSampleId() {
        String collaboratorSampleId = "Sample1";
        productOrder.addSample(new ProductOrderSample("SM-1234", new BspSampleData(Collections.singletonMap(
                BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID, collaboratorSampleId))));

        List<SampleLedgerRow> data = factory.gatherSampleRowData(productOrder);
        assertThat(data.get(0).getCollaboratorSampleId(), equalTo(collaboratorSampleId));
    }

    public void gatherMaterialType() {
        String materialType = "Test material";
        productOrder.addSample(new ProductOrderSample("SM-1234",
                new BspSampleData(Collections.singletonMap(BSPSampleSearchColumn.MATERIAL_TYPE, materialType))));

        List<SampleLedgerRow> data = factory.gatherSampleRowData(productOrder);
        assertThat(data.get(0).getMaterialType(), equalTo(materialType));
    }

    public void gatherRiskWhenNoRisk() {
        productOrder.addSample(new ProductOrderSample("SM-1234", new BspSampleData()));

        List<SampleLedgerRow> data = factory.gatherSampleRowData(productOrder);
        assertThat(data.get(0).getRiskText(), equalTo(""));
    }

    public void gatherRiskWhenOnRisk() {
        ProductOrderSample sample = new ProductOrderSample("SM-1234", new BspSampleData());
        sample.setManualOnRisk(RiskCriterion.createManual(), "Test risk");
        productOrder.addSample(sample);

        List<SampleLedgerRow> data = factory.gatherSampleRowData(productOrder);
        assertThat(data.get(0).getRiskText(),
                pattern("At \\d{1,2}:\\d{2}:\\d{2} [AP]M on \\w+ \\d{1,2}, \\d{4}, calculated Manual with comment: Test risk"));
    }

    public void gatherDeliveryStatus() {
        ProductOrderSample sample = new ProductOrderSample("SM-1234", new BspSampleData());
        sample.setDeliveryStatus(ProductOrderSample.DeliveryStatus.DELIVERED);
        productOrder.addSample(sample);

        List<SampleLedgerRow> data = factory.gatherSampleRowData(productOrder);
        assertThat(data.get(0).getDeliveryStatus(),
                equalTo(ProductOrderSample.DeliveryStatus.DELIVERED.getDisplayName()));
    }

    public void gatherProductName() {
        productOrder.addSample(new ProductOrderSample("SM-1234", new BspSampleData()));

        List<SampleLedgerRow> data = factory.gatherSampleRowData(productOrder);
        assertThat(data.get(0).getProductName(), equalTo(TEST_PRODUCT_NAME));
    }

    public void gatherProductOrderKey() {
        productOrder.addSample(new ProductOrderSample("SM-1234", new BspSampleData()));
        String jiraTicketKey = "PDO-123";
        productOrder.setJiraTicketKey(jiraTicketKey);

        List<SampleLedgerRow> data = factory.gatherSampleRowData(productOrder);
        assertThat(data.get(0).getProductOrderKey(), equalTo(jiraTicketKey));
    }

    public void gatherProductOrderTitle() {
        productOrder.addSample(new ProductOrderSample("SM-1234", new BspSampleData()));

        List<SampleLedgerRow> data = factory.gatherSampleRowData(productOrder);
        assertThat(data.get(0).getProductOrderTitle(), equalTo(TEST_PRODUCT_ORDER_TITLE));
    }

    public void gatherProjectManager() {
        productOrder.addSample(new ProductOrderSample("SM-1234", new BspSampleData()));

        List<SampleLedgerRow> data = factory.gatherSampleRowData(productOrder);
        assertThat(data.get(0).getProjectManagerName(), equalTo(productOrderCreator.getFullName()));
    }

    public void gatherNumberOfLanes() {
        productOrder.addSample(new ProductOrderSample("SM-1234", new BspSampleData()));
        productOrder.setLaneCount(8);

        List<SampleLedgerRow> data = factory.gatherSampleRowData(productOrder);
        assertThat(data.get(0).getNumberOfLanes(), equalTo(8));
    }

    public void gatherAutoLedgerDate() {
        ProductOrderSample sample = new ProductOrderSample("SM-1234", new BspSampleData());
        productOrder.addSample(sample);
        Date workCompleteDate = new Date(1);
        Date autoLedgerDate = new Date(2);
        sample.addAutoLedgerItem(workCompleteDate, new PriceItem("Quote-1", "Crush", "Test", "Test Price Item"), 1,
                autoLedgerDate);

        List<SampleLedgerRow> data = factory.gatherSampleRowData(productOrder);
        assertThat(data.get(0).getAutoLedgerDate(), equalTo(autoLedgerDate));
    }

    public void gatherWorkCompleteDate() {
        ProductOrderSample sample = new ProductOrderSample("SM-1234", new BspSampleData());
        productOrder.addSample(sample);
        Date workCompleteDate = new Date(1);
        Date autoLedgerDate = new Date(2);
        sample.addAutoLedgerItem(workCompleteDate, new PriceItem("Quote-1", "Crush", "Test", "Test Price Item"), 1,
                autoLedgerDate);

        List<SampleLedgerRow> data = factory.gatherSampleRowData(productOrder);
        assertThat(data.get(0).getWorkCompleteDate(), equalTo(workCompleteDate));
    }
}
