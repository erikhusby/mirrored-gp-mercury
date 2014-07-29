package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.athena.entity.products.RiskCriterion;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.broadinstitute.gpinformatics.PatternMatcher.pattern;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

/**
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class SampleLedgerExporterFactoryTest {

    private Product product;
    private ProductOrder productOrder;
    private SampleLedgerExporterFactory factory;

    @BeforeMethod
    public void setUp() throws Exception {
        BSPUserList mockBspUserList = Mockito.mock(BSPUserList.class);
        BspUser bspUser = new BspUser();
        bspUser.setFirstName("Test");
        bspUser.setLastName("Dummy");
        when(mockBspUserList.getById(1L)).thenReturn(bspUser);

        product = new Product("Test Product", null, null, null, null, null, null, null, null, null, null, null, true,
                Workflow.NONE, false, null);

        ResearchProject researchProject = new ResearchProject(1L, "Test Project", "Test", true);

        productOrder =
                new ProductOrder(1L, "SampleLedgerExporterFactoryTest", new ArrayList<ProductOrderSample>(), "QUOTE-1",
                        product, researchProject);
        factory = new SampleLedgerExporterFactory(null, mockBspUserList, null, null, null, null, null, null);
    }

    public void gatherNoSamplesForEmptyProductOrder() {
        List<List<String>> data = factory.gatherSampleRowData(productOrder);
        assertThat(data.size(), equalTo(0));
    }

    public void gatherSampleFromProductOrderWithOneSample() {
        productOrder.addSample(new ProductOrderSample("SM-1234", new BSPSampleDTO()));

        List<List<String>> data = factory.gatherSampleRowData(productOrder);
        assertThat(data.size(), equalTo(1));
        assertThat(data.get(0).get(0), equalTo("SM-1234"));
    }

    public void gatherSamplesFromProductOrderWithTwoSamples() {
        productOrder.addSample(new ProductOrderSample("SM-1234", new BSPSampleDTO()));
        productOrder.addSample(new ProductOrderSample("SM-5678", new BSPSampleDTO()));

        List<List<String>> data = factory.gatherSampleRowData(productOrder);
        assertThat(data.size(), equalTo(2));
        assertThat(data.get(0).get(0), equalTo("SM-1234"));
        assertThat(data.get(1).get(0), equalTo("SM-5678"));
    }

    public void gatherCollaboratorSampleId() {
        productOrder.addSample(new ProductOrderSample("SM-1234", new BSPSampleDTO(Collections.singletonMap(
                BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID, "Sample1"))));

        List<List<String>> data = factory.gatherSampleRowData(productOrder);
        assertThat(data.get(0).get(1), equalTo("Sample1"));
    }

    public void gatherMaterialType() {
        productOrder.addSample(new ProductOrderSample("SM-1234",
                new BSPSampleDTO(Collections.singletonMap(BSPSampleSearchColumn.MATERIAL_TYPE, "Test material"))));

        List<List<String>> data = factory.gatherSampleRowData(productOrder);
        assertThat(data.get(0).get(2), equalTo("Test material"));
    }

    public void gatherRiskWhenNoRisk() {
        productOrder.addSample(new ProductOrderSample("SM-1234", new BSPSampleDTO()));

        List<List<String>> data = factory.gatherSampleRowData(productOrder);
        assertThat(data.get(0).get(3), equalTo(""));
    }

    public void gatherRiskWhenOnRisk() {
        ProductOrderSample sample = new ProductOrderSample("SM-1234", new BSPSampleDTO());
        sample.setManualOnRisk(RiskCriterion.createManual(), "Test risk");
        productOrder.addSample(sample);

        List<List<String>> data = factory.gatherSampleRowData(productOrder);
        assertThat(data.get(0).get(3),
                pattern("At \\d{1,2}:\\d{2}:\\d{2} [AP]M on \\w+ \\d{1,2}, \\d{4}, calculated Manual with comment: Test risk"));
    }

    public void gatherDeliveryStatus() {
        ProductOrderSample sample = new ProductOrderSample("SM-1234", new BSPSampleDTO());
        sample.setDeliveryStatus(ProductOrderSample.DeliveryStatus.DELIVERED);
        productOrder.addSample(sample);

        List<List<String>> data = factory.gatherSampleRowData(productOrder);
        assertThat(data.get(0).get(4), equalTo(ProductOrderSample.DeliveryStatus.DELIVERED.getDisplayName()));
    }

    public void gatherProductName() {
        productOrder.addSample(new ProductOrderSample("SM-1234", new BSPSampleDTO()));

        List<List<String>> data = factory.gatherSampleRowData(productOrder);
        assertThat(data.get(0).get(5), equalTo("Test Product"));
    }

    public void gatherProductOrderKey() {
        productOrder.addSample(new ProductOrderSample("SM-1234", new BSPSampleDTO()));
        productOrder.setJiraTicketKey("PDO-123");

        List<List<String>> data = factory.gatherSampleRowData(productOrder);
        assertThat(data.get(0).get(6), equalTo("PDO-123"));
    }

    public void gatherProductOrderTitle() {
        productOrder.addSample(new ProductOrderSample("SM-1234", new BSPSampleDTO()));

        List<List<String>> data = factory.gatherSampleRowData(productOrder);
        assertThat(data.get(0).get(7), equalTo("SampleLedgerExporterFactoryTest"));
    }

    public void gatherProjectManager() {
        productOrder.addSample(new ProductOrderSample("SM-1234", new BSPSampleDTO()));

        List<List<String>> data = factory.gatherSampleRowData(productOrder);
        assertThat(data.get(0).get(8), equalTo("Test Dummy"));
    }

    @Test(enabled = false)
    public void gatherNumberOfLanes() {
        productOrder.addSample(new ProductOrderSample("SM-1234", new BSPSampleDTO()));
        product.setProductFamily(new ProductFamily(ProductFamily.ProductFamilyName.SEQUENCE_ONLY.getFamilyName()));

        List<List<String>> data = factory.gatherSampleRowData(productOrder);
        assertThat(data.get(0).get(9), equalTo("8"));
    }
}
