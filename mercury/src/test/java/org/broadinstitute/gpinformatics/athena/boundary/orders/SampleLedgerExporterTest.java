package org.broadinstitute.gpinformatics.athena.boundary.orders;

import com.google.common.io.NullOutputStream;
import org.apache.poi.ss.usermodel.CellStyle;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.boundary.billing.BillingTrackerHeader;
import org.broadinstitute.gpinformatics.athena.control.dao.work.WorkCompleteMessageDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.athena.entity.products.RiskCriterion;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AppConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuotePriceItem;
import org.broadinstitute.gpinformatics.infrastructure.tableau.TableauConfig;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.integration.RestServiceContainerTest;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.matches;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link SampleLedgerExporter}. Currently just a characterization test to allow for some new feature
 * development and help with refactoring the code to be more easily testable.
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class SampleLedgerExporterTest {

    public void testWriteToStream() throws IOException {

        final String deployedMercuryPort= System.getProperty(RestServiceContainerTest.JBOSS_HTTPS_PORT_SYSTEM_PROPERTY,
                String.valueOf(RestServiceContainerTest.DEFAULT_FORWARD_PORT));

        /*
         * Simulate getting a user from BSP.
         */
        BSPUserList mockBspUserList = Mockito.mock(BSPUserList.class);
        BspUser user = new BspUser();
        user.setUserId(1L);
        user.setFirstName("Test");
        user.setLastName("Dummy");
        when(mockBspUserList.getById(1)).thenReturn(user);
        when(mockBspUserList.getUserFullName(1L)).thenReturn(user.getFullName());

        /*
         * Configure a product family that will both output number of lanes (a seq-only product behavior) and display
         * % coverage at 20x (an exome product behavior).
         */
        ProductFamily mockProductFamily = Mockito.mock(ProductFamily.class);
        when(mockProductFamily.isSupportsNumberOfLanes()).thenReturn(true);
        when(mockProductFamily.getName()).thenReturn(ProductFamily.ProductFamilyName.EXOME.getFamilyName());

        /*
         * Create product, with a known primary price item, and a research project.
         */
        Product product =
                new Product("Test Product", mockProductFamily, null, "P-0001", null, null, null, null, null, null, null,
                        null, true, Workflow.NONE, false, null);
        PriceItem priceItem = new PriceItem("Quote-1", "Crush", "Test", "Test Price Item");
        product.setPrimaryPriceItem(priceItem);

        ResearchProject researchProject = new ResearchProject(1L, "Test Project", "Test", true,
                                                              ResearchProject.RegulatoryDesignation.RESEARCH_ONLY);

        /*
         * Create a product order sample with a few bits of BSP data, manual risk, and a delivery status of "Delivered".
         */
        HashMap<BSPSampleSearchColumn, String> bspData = new HashMap<>();
        bspData.put(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID, "Sample1");
        bspData.put(BSPSampleSearchColumn.MATERIAL_TYPE, "Test Type");
        SampleData sampleData = new BspSampleData(bspData);
        ProductOrderSample productOrderSample = new ProductOrderSample("SM-1234", sampleData);
        productOrderSample.setManualOnRisk(RiskCriterion.createManual(), "Test risk");
        productOrderSample.setDeliveryStatus(ProductOrderSample.DeliveryStatus.DELIVERED);

        /*
         * Create a second product order sample.
         */
        Map<BSPSampleSearchColumn, String> bspData2 = new HashMap<>();
        bspData2.put(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID, "Sample2");
        SampleData sampleDTO2 = new BspSampleData(bspData2);
        ProductOrderSample productOrderSample2 = new ProductOrderSample("SM-5678", sampleDTO2);

        /*
         * Create a product order with a JIRA ticket (submitted) and requested lane count (generally for seq-only PDOs).
         */
        ProductOrder productOrder = new ProductOrder(1L, "SampleLedgerExporterFactoryTest",
                Arrays.asList(productOrderSample, productOrderSample2), "Quote-1", product,
                researchProject);
        productOrder.setJiraTicketKey("PDO-123");
        productOrder.setLaneCount(8);

        /*
         * Add some billing ledger info for the product's primary price item and another price item that could have been
         * associated with the product at some point in the past but no longer is.
         */
        PriceItem historicalPriceItem = new PriceItem("Quote-2", "Crush", "Test", "Test Historical Price Item");
        productOrderSample.addLedgerItem(new Date(1L), historicalPriceItem, 1);
        LedgerEntry historicalLedgerEntry = productOrderSample.getLedgerItems().iterator().next();
        new BillingSession(1L, Collections.singleton(historicalLedgerEntry));
        historicalLedgerEntry.setBillingMessage(BillingSession.SUCCESS);
        productOrderSample.addAutoLedgerItem(new Date(1L), priceItem, 2, new Date(2L));

        /*
         * Create the SampleLedgerExporter unit under test, including a mock spreadsheet writer in order to verify the
         * correct data is written by the exporter.
         */
        SampleLedgerSpreadSheetWriter mockWriter = Mockito.mock(SampleLedgerSpreadSheetWriter.class);

        WorkCompleteMessageDao mockWorkCompleteMessageDao = Mockito.mock(WorkCompleteMessageDao.class);
        SampleLedgerExporterFactory factory = new SampleLedgerExporterFactory(null, mockBspUserList,
                new PriceListCache(new ArrayList<QuotePriceItem>()), mockWorkCompleteMessageDao,
                Mockito.mock(SampleDataFetcher.class), new AppConfig(Deployment.DEV),
                new TableauConfig(Deployment.DEV), mockWriter);
        SampleLedgerExporter exporter = factory.makeExporter(Collections.singletonList(productOrder));

        /*
         * Exercise the unit under test!
         */
        exporter.writeToStream(new NullOutputStream());

        /*
         * Verify that the headers are all written out in the correct order.
         */
        InOrder headerOrder = inOrder(mockWriter);
        for (BillingTrackerHeader header : BillingTrackerHeader.values()) {
            headerOrder.verify(mockWriter)
                    .writeHeaderCell(header.getText(), header == BillingTrackerHeader.TABLEAU_LINK);
        }
        headerOrder.verify(mockWriter)
                .writeCell(eq(BillingTrackerHeader.getHistoricalPriceItemNameHeader(historicalPriceItem)), any(CellStyle.class));
        headerOrder.verify(mockWriter)
                .writeCell(eq(BillingTrackerHeader.getPriceItemNameHeader(priceItem)), any(CellStyle.class));
        headerOrder.verify(mockWriter).writeCell(eq(BillingTrackerHeader.getPriceItemPartNumberHeader(product)),
                any(CellStyle.class));
        headerOrder.verify(mockWriter).writeCell(eq("Comments"), any(CellStyle.class));

        /*
         * Verify that the data cells are all written out in the correct order, which corresponds to the order of the
         * headers checked above.
         */
        InOrder dataOrder = inOrder(mockWriter);
        dataOrder.verify(mockWriter).writeCell("SM-1234");
        dataOrder.verify(mockWriter).writeCell("Sample1");
        dataOrder.verify(mockWriter).writeCell("Test Type");
        dataOrder.verify(mockWriter).writeCell(
                matches("At \\d{1,2}:\\d{2}:\\d{2} [AP]M on \\w+ \\d{1,2}, \\d{4}, calculated Manual with comment: Test risk"),
                any(CellStyle.class));
        dataOrder.verify(mockWriter).writeCell(ProductOrderSample.DeliveryStatus.DELIVERED.getDisplayName());
        dataOrder.verify(mockWriter).writeCell("Test Product");
        dataOrder.verify(mockWriter).writeCellLink("PDO-123",
                "https://localhost:"+deployedMercuryPort+"/Mercury//orders/order.action?view=&productOrder=PDO-123");
        dataOrder.verify(mockWriter).writeCell("SampleLedgerExporterFactoryTest");
        dataOrder.verify(mockWriter).writeCell("Test Dummy");
        dataOrder.verify(mockWriter).writeCell(8);
        dataOrder.verify(mockWriter).writeCell(eq(new Date(2L)), any(CellStyle.class));
        dataOrder.verify(mockWriter).writeCell(eq(new Date(1L)), any(CellStyle.class));
        dataOrder.verify(mockWriter, times(4)).writeCell(""); // TODO: test aggregation metrics
        dataOrder.verify(mockWriter).writeCellLink(anyString(), anyString());
        dataOrder.verify(mockWriter).writeCell("Quote-1");
        dataOrder.verify(mockWriter).writeCell(1);
        dataOrder.verify(mockWriter).writeHistoricalBilledAmount(1.0);
        dataOrder.verify(mockWriter).writeCell(0.0);
        dataOrder.verify(mockWriter).writeCell(eq(2.0), any(CellStyle.class));

        /*
         * Verify that the samples are all written out in the correct order.
         */
        InOrder sampleOrder = inOrder(mockWriter);
        sampleOrder.verify(mockWriter).writeCell("SM-1234");
        sampleOrder.verify(mockWriter).writeCell("Sample1");
        sampleOrder.verify(mockWriter).writeCell("SM-5678");
        sampleOrder.verify(mockWriter).writeCell("Sample2");

        /*
         * Verify that the right number of rows were written.
         */
        verify(mockWriter, times(3)).nextRow();

        /*
         * Verify that the WorkCompleteMessageDao isn't called excessively (default for verify() is times(1)).
         */
        verify(mockWorkCompleteMessageDao).findByPDO("PDO-123");
    }
}
