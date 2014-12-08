package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.apache.commons.io.output.NullOutputStream;
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
import org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderActionBean;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
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
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.endsWith;
import static org.mockito.Matchers.eq;
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
        when(mockBspUserList.getById(user.getUserId())).thenReturn(user);
        when(mockBspUserList.getUserFullName(user.getUserId())).thenReturn(user.getFullName());

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
        SampleData sampleData = Mockito.mock(SampleData.class);
        when(sampleData.getCollaboratorsSampleName()).thenReturn("Sample1");
        when(sampleData.getMaterialType()).thenReturn("Test Type");
        when(sampleData.getSampleType()).thenReturn("Sample Type");
        ProductOrderSample productOrderSample = new ProductOrderSample("SM-1234", sampleData);
        productOrderSample.setManualOnRisk(RiskCriterion.createManual(), "Test risk");
        productOrderSample.setDeliveryStatus(ProductOrderSample.DeliveryStatus.DELIVERED);

        /*
         * Create a second product order sample.
         */
        SampleData sampleData2 = Mockito.mock(SampleData.class);
        when(sampleData2.getCollaboratorsSampleName()).thenReturn("Sample2");
        ProductOrderSample productOrderSample2 = new ProductOrderSample("SM-5678", sampleData2);

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
        SampleLedgerSpreadSheetWriter mockWriter = Mockito.spy(new SampleLedgerSpreadSheetWriter());

        WorkCompleteMessageDao mockWorkCompleteMessageDao = Mockito.mock(WorkCompleteMessageDao.class);
        AppConfig appConfig = new AppConfig(Deployment.DEV);
        SampleLedgerExporterFactory factory = new SampleLedgerExporterFactory(null, mockBspUserList,
                new PriceListCache(new ArrayList<QuotePriceItem>()), mockWorkCompleteMessageDao,
                Mockito.mock(SampleDataFetcher.class), appConfig,
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
                .writeCell(eq(BillingTrackerHeader.getHistoricalPriceItemNameHeader(historicalPriceItem)),
                        any(CellStyle.class));
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
        dataOrder.verify(mockWriter).writeCell(productOrderSample.getName());
        dataOrder.verify(mockWriter).writeCell(productOrderSample.getSampleData().getCollaboratorsSampleName());
        dataOrder.verify(mockWriter).writeCell(productOrderSample.getSampleData().getMaterialType());
        dataOrder.verify(mockWriter).writeCell(
                eq(productOrderSample.getRiskItems().iterator().next().getInformation()),
                any(CellStyle.class));
        dataOrder.verify(mockWriter).writeCell(productOrderSample.getDeliveryStatus().getDisplayName());
        dataOrder.verify(mockWriter).writeCell(product.getName());
        String pdoKey = productOrder.getJiraTicketKey();
        dataOrder.verify(mockWriter).writeCellLink(pdoKey,
                ProductOrderActionBean.getProductOrderLink(productOrder, appConfig));
        dataOrder.verify(mockWriter).writeCell(productOrder.getName());
        dataOrder.verify(mockWriter).writeCell(user.getFullName());
        dataOrder.verify(mockWriter).writeCell(productOrder.getLaneCount());
        dataOrder.verify(mockWriter).writeCell(eq(productOrderSample.getLatestAutoLedgerTimestamp()),
                any(CellStyle.class));
        dataOrder.verify(mockWriter).writeCell(eq(productOrderSample.getWorkCompleteDate()), any(CellStyle.class));
        // TODO: test aggregation metrics
        dataOrder.verify(mockWriter).writeCell((BigInteger) null);
        dataOrder.verify(mockWriter).writeCell((BigInteger) null);
        dataOrder.verify(mockWriter).writeCell((BigInteger) null);
        dataOrder.verify(mockWriter).writeCell(any(Double.class), any(CellStyle.class));
        dataOrder.verify(mockWriter).writeCell(any(Double.class), any(CellStyle.class));
        dataOrder.verify(mockWriter).writeCell(eq(productOrderSample.getSampleData().getSampleType()));
        // Tableau link
        dataOrder.verify(mockWriter).writeCellLink(anyString(), anyString());
        dataOrder.verify(mockWriter).writeCell(productOrder.getQuoteId());
        dataOrder.verify(mockWriter).writeCell(1);
        dataOrder.verify(mockWriter).writeHistoricalBilledAmount(1.0);
        dataOrder.verify(mockWriter).writeCell(0.0);
        dataOrder.verify(mockWriter).writeCell(eq(2.0), any(CellStyle.class));

        /*
         * Verify that the samples are all written out in the correct order.
         */
        InOrder sampleOrder = inOrder(mockWriter);
        sampleOrder.verify(mockWriter).writeCell(productOrderSample.getName());
        sampleOrder.verify(mockWriter).writeCell(productOrderSample.getSampleData().getCollaboratorsSampleName());
        sampleOrder.verify(mockWriter).writeCell(productOrderSample2.getName());
        sampleOrder.verify(mockWriter).writeCell(productOrderSample2.getSampleData().getCollaboratorsSampleName());

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
