package org.broadinstitute.gpinformatics.athena.boundary.orders;

import com.google.common.io.NullOutputStream;
import org.apache.poi.ss.usermodel.CellStyle;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.boundary.billing.BillingTrackerHeader;
import org.broadinstitute.gpinformatics.athena.control.dao.work.WorkCompleteMessageDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.athena.entity.products.RiskCriterion;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AppConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuotePriceItem;
import org.broadinstitute.gpinformatics.infrastructure.tableau.TableauConfig;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.matches;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class SampleLedgerExporterTest {

    public void testWriteToStream() throws IOException {
        // Configure a product family that will both output number of lanes (a seq-only product behavior) and display % coverage at 20x (an exome product behavior)
        ProductFamily mockProductFamily = Mockito.mock(ProductFamily.class);
        when(mockProductFamily.isSupportsNumberOfLanes()).thenReturn(true);
        when(mockProductFamily.getName()).thenReturn(ProductFamily.ProductFamilyName.EXOME.getFamilyName());

        Product product =
                new Product("Test Product", mockProductFamily, null, "P-0001", null, null, null, null, null, null, null,
                        null, true, Workflow.NONE, false, null);
        product.setPrimaryPriceItem(new PriceItem("Quote-1", "Crush", "Test", "Test Quote"));

        ResearchProject researchProject = new ResearchProject(1L, "Test Project", "Test", true);

        HashMap<BSPSampleSearchColumn, String> bspData = new HashMap<>();
        bspData.put(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID, "Sample1");
        bspData.put(BSPSampleSearchColumn.MATERIAL_TYPE, "Test Type");
        BSPSampleDTO bspSampleDTO = new BSPSampleDTO(bspData);
        ProductOrderSample productOrderSample = new ProductOrderSample("SM-1234", bspSampleDTO);
        productOrderSample.setManualOnRisk(RiskCriterion.createManual(), "Test risk");
        productOrderSample.setDeliveryStatus(ProductOrderSample.DeliveryStatus.DELIVERED);

        ProductOrder productOrder = new ProductOrder(1L, "SampleLedgerExporterFactoryTest",
                Collections.singletonList(productOrderSample), "QUOTE-1", product,
                researchProject);
        productOrder.setJiraTicketKey("PDO-123");
        productOrder.setLaneCount(8);

        SampleLedgerSpreadSheetWriter mockWriter = Mockito.mock(SampleLedgerSpreadSheetWriter.class);

        BSPUserList mockBspUserList = Mockito.mock(BSPUserList.class);
        BspUser user = new BspUser();
        user.setUserId(1L);
        user.setFirstName("Test");
        user.setLastName("Dummy");
        when(mockBspUserList.getById(1)).thenReturn(user);

        SampleLedgerExporterFactory factory = new SampleLedgerExporterFactory(null, mockBspUserList,
                new PriceListCache(new ArrayList<QuotePriceItem>()), Mockito.mock(WorkCompleteMessageDao.class),
                Mockito.mock(BSPSampleDataFetcher.class), new AppConfig(Deployment.DEV),
                new TableauConfig(Deployment.DEV), mockWriter);
        SampleLedgerExporter exporter = factory.makeExporter(Collections.singletonList(productOrder));

        exporter.writeToStream(new NullOutputStream());

        for (BillingTrackerHeader header : BillingTrackerHeader.values()) {
            verify(mockWriter).writeHeaderCell(header.getText(), header == BillingTrackerHeader.TABLEAU_LINK);
        }

        InOrder inOrder = inOrder(mockWriter);
        inOrder.verify(mockWriter).writeCell("SM-1234");
        inOrder.verify(mockWriter).writeCell("Sample1");
        inOrder.verify(mockWriter).writeCell("Test Type");
        inOrder.verify(mockWriter).writeCell(
                matches("At \\d{1,2}:\\d{2}:\\d{2} [AP]M on \\w+ \\d{1,2}, \\d{4}, calculated Manual with comment: Test risk"),
                any(CellStyle.class));
        inOrder.verify(mockWriter).writeCell(ProductOrderSample.DeliveryStatus.DELIVERED.getDisplayName());
        inOrder.verify(mockWriter).writeCell("Test Product");
        inOrder.verify(mockWriter).writeCellLink("PDO-123",
                "http://localhost:8080/Mercury//orders/order.action?view=&productOrder=PDO-123");
        inOrder.verify(mockWriter).writeCell("SampleLedgerExporterFactoryTest");
        inOrder.verify(mockWriter).writeCell("Test Dummy");
        inOrder.verify(mockWriter).writeCell(8);
    }
}
