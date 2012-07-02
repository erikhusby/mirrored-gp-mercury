package org.broadinstitute.sequel.integration.entity.billing;


import org.broadinstitute.sequel.entity.billing.PerSampleBillableFactory;
import org.broadinstitute.sequel.entity.billing.Quote;
import org.broadinstitute.sequel.entity.project.BasicProjectPlan;
import org.broadinstitute.sequel.entity.project.JiraTicket;
import org.broadinstitute.sequel.entity.vessel.BSPSampleAuthorityTwoDTube;
import org.broadinstitute.sequel.test.BettaLimsMessageFactory;
import org.broadinstitute.sequel.bettalims.jaxb.PlateTransferEventType;
import org.broadinstitute.sequel.control.dao.person.PersonDAO;
import org.broadinstitute.sequel.control.labevent.LabEventFactory;
import org.broadinstitute.sequel.control.labevent.LabEventHandler;
import org.broadinstitute.sequel.entity.bsp.BSPStartingSample;
import org.broadinstitute.sequel.entity.labevent.LabEventName;
import org.broadinstitute.sequel.entity.project.BasicProject;
import org.broadinstitute.sequel.entity.project.WorkflowDescription;
import org.broadinstitute.sequel.entity.sample.StartingSample;
import org.broadinstitute.sequel.infrastructure.jira.issue.CreateIssueRequest;
import org.broadinstitute.sequel.infrastructure.quote.*;
import org.broadinstitute.sequel.entity.labevent.LabEvent;
import org.broadinstitute.sequel.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.sequel.integration.ContainerTest;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;

import static org.broadinstitute.sequel.TestGroups.EXTERNAL_INTEGRATION;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class PerSampleBillableFactoryTest extends ContainerTest {

    @Inject
    private Biller biller;

    @Inject
    private LabEventHandler labEventHandler;

    /**
     * Add expectations to {@link QuoteService#registerNewWork(org.broadinstitute.sequel.infrastructure.quote.Quote, org.broadinstitute.sequel.infrastructure.quote.PriceItem, double, String, String, String)}
     * to the given mock
     * @param service the mock
     * @param quote
     * @param priceItem
     * @param numWorkUnits
     * @param returnedId
     */
    private void addExpectedQuoteInteraction(QuoteService service,
                                            org.broadinstitute.sequel.infrastructure.quote.Quote quote,
                                            PriceItem priceItem,
                                            double numWorkUnits,
                                            String returnedId,
                                            int numTimesCalled) {
        EasyMock.expect(service.registerNewWork(quote,priceItem,numWorkUnits,null,null,null)).andReturn(returnedId).times(numTimesCalled);
    }

    /**
     * Makes a new {@link BettaLimsMessageFactory#buildRackToPlate(String, String, java.util.List, String) rack to plate event}
     * where the
     * tubes are semi-equally distributed between
     * two different {@link org.broadinstitute.sequel.entity.project.BasicProjectPlan}s. This helps us test that
     * we bill the single {@link LabEvent} back to two different
     * {@link org.broadinstitute.sequel.entity.billing.Quote}s.
     *
     * plan1 and plan2 should have different
     * {@link org.broadinstitute.sequel.entity.billing.Quote}s.
     * @param plan1
     * @param plan2
     * @param totalSamples the total number of samples
     *                     to add to the source rack
     * @return
     */
    private LabEvent buildLabEvent(BasicProjectPlan plan1,
                                   BasicProjectPlan plan2,
                                   int totalSamples) {
        LabEventFactory labEventFactory = new LabEventFactory();
        labEventFactory.setPersonDAO(new PersonDAO());

        Map<String, TwoDBarcodedTube> mapBarcodeToTube = new LinkedHashMap<String, TwoDBarcodedTube>();
        for(int rackPosition = 1; rackPosition <= totalSamples; rackPosition++) {
            BasicProjectPlan projectPlan = null;
            if (rackPosition % 2 == 0) {
                projectPlan = plan1;
            }
            else {
                projectPlan = plan2;
            }
            String barcode = "R" + rackPosition;
            String bspStock = "SM-" + rackPosition;
            BSPSampleAuthorityTwoDTube bspAliquot = new BSPSampleAuthorityTwoDTube(new BSPStartingSample(bspStock + ".aliquot", projectPlan, null));
            mapBarcodeToTube.put(barcode,bspAliquot);

        }
        
        BettaLimsMessageFactory bettaLimsMessageFactory = new BettaLimsMessageFactory();
        String shearPlateBarcode = "ShearPlate";
        PlateTransferEventType shearingTransferEventJaxb = bettaLimsMessageFactory.buildRackToPlate(
                "ShearingTransfer", "KioskRack", new ArrayList<String>(mapBarcodeToTube.keySet()), shearPlateBarcode);
        LabEvent shearingTransferEventEntity = labEventFactory.buildFromBettaLimsRackToPlateDbFree(
                shearingTransferEventJaxb, mapBarcodeToTube, null);

        return shearingTransferEventEntity;
    }

    private BasicProjectPlan createProjectPlan(String projectName,
                                          String projectPlanName,
                                          String quoteAlphanumericId) {
        BasicProjectPlan plan = new BasicProjectPlan(new BasicProject(projectName,new JiraTicket()),
                projectPlanName,
                new WorkflowDescription("ChocolateChipCookies", new PriceItem("Illumina LC","5","Standard LC","3.5",PriceItem.SAMPLE_UNITS,PriceItem.GSP_PLATFORM_NAME), CreateIssueRequest.Fields.Issuetype.Whole_Exome_HybSel));
        Quote quote = new org.broadinstitute.sequel.entity.billing.Quote(quoteAlphanumericId,
                new org.broadinstitute.sequel.infrastructure.quote.Quote(quoteAlphanumericId,new QuoteFunding(new FundingLevel("100",new Funding(Funding.FUNDS_RESERVATION,"NCI")))));
        plan.setQuote(quote);   
        
        return plan;
    }
    

    /**
     * Verifies that a rack -> plate event, which contains
     * {@link StartingSample} from different (@link BasicProjectPlan}s
     * and different {@link org.broadinstitute.sequel.entity.billing.Quote}s
     * will be billed properly.
     * 
     * In particular, this test is supposed to verify that
     * if you have 12 {@link StartingSample}s for one
     * {@link org.broadinstitute.sequel.entity.billing.Quote}
     * and 12 other {@link StartingSample}s from a different
     * {@link org.broadinstitute.sequel.entity.billing.Quote}
     * comingled on a plate and
     * you want to bill for the {@link LabEvent} that uses
     * the plate, what you'll post back to quote server is
     * a single line item for one quote for the first set
     * of 12 samples and another work item in quote server
     * against the other quote for the other batch of 12.
     */
    @Test(groups = {EXTERNAL_INTEGRATION})
    public void test_billing() {
        String expectedBatchId = "QuoteWorkItemBatchId123";
        BasicProjectPlan plan1 = createProjectPlan("Project1","Plan1","DNA33");
        BasicProjectPlan plan2 = createProjectPlan("Project2","Plan2","DNA35");

        int totalSampleCount = 24; // keep it even; we're just splitting across two quotes
        LabEvent labEvent = buildLabEvent(plan1,plan2,totalSampleCount);

        Billable billable = PerSampleBillableFactory.createBillable(labEvent);

        QuoteService service = EasyMock.createMock(QuoteService.class);
        int numSamplesBilledPerQuote = totalSampleCount/2;
        addExpectedQuoteInteraction(service,
                                    plan1.getQuoteDTO(),
                                    plan1.getWorkflowDescription().getPriceItem(),
                                    numSamplesBilledPerQuote,
                                    expectedBatchId,
                                    1);
        addExpectedQuoteInteraction(service,
                plan2.getQuoteDTO(),
                plan2.getWorkflowDescription().getPriceItem(),
                numSamplesBilledPerQuote,
                expectedBatchId,
                1);

        EasyMock.replay(service);
        billable.doBilling(service);
        Assert.assertEquals(labEvent.getQuoteServerBatchId(),expectedBatchId);
        EasyMock.verify(service);

        // now we'll verify that the handler sends a new billing
        // event via CDI to the Biller
        String workBatchId = "workBatch1";
        labEvent.setQuoteServerBatchId(null);
        QuoteService quoteService = createQuoteService(workBatchId,2);
        biller.setQuoteService(quoteService);  // there has to be a better way to do this.
                                                                                // how can we configure injection of particular
                                                                                // classes from within a single unit test?
        labEventHandler.processEvent(labEvent, null);
        EasyMock.verify(service);
        Assert.assertEquals(labEvent.getQuoteServerBatchId(),workBatchId);
    }

    private QuoteService createQuoteService(String workBatchId,int numCalls) {
        QuoteService service = EasyMock.createMock(QuoteService.class);
        EasyMock.expect(service.registerNewWork((org.broadinstitute.sequel.infrastructure.quote.Quote)EasyMock.anyObject(),(PriceItem)EasyMock.anyObject(),EasyMock.anyDouble(),(String)EasyMock.anyObject(),(String)EasyMock.anyObject(),(String)EasyMock.anyObject())).andReturn(workBatchId).times(numCalls);
        EasyMock.replay(service);

        return service;
    }
}
