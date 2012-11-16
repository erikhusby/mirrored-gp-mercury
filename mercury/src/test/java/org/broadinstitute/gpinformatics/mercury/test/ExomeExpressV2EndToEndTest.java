package org.broadinstitute.gpinformatics.mercury.test;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientServiceStub;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.boundary.bucket.BucketBean;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.person.Person;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDefVersion;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowBucketDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Test Exome Express in Mercury
 */
public class ExomeExpressV2EndToEndTest {
    @Test
    public void test() {
        // Define baits (CATs later)
        // Associate baits with vessels
        // Define molecular indexes
        // Associate molecular indexes with vessels
        // Define Product
        // Define Research Project
        // Define IRB
        // Define Personnel
        // Define Cohort/Collection
        // {Create Quote/Setup Funding}
        // Create Product Order (key lab personnel and PDMs are notified)
        ProductOrder productOrder1 = AthenaClientServiceStub.createDummyProductOrder();

        // Define travel group (can be independent of product order)
        // {Create kit and Ship kit}
        // - Upload metadata through BSP portal
        // {Receive samples} (There are some hooks into product order so the receipt team can direct samples)
        // 3 stars align notification
        // BSP notifies Mercury of existence of samples in plastic
        // Buckets for Extraction and BSP Pico?
        // Add samples to bucket
        // Drain bucket into batch
        // Create JIRA ticket configured in bucket (There is a ticket for an LCSet but none for the extraction/pico work)
        // BSP registers batch in Mercury
        // BSP Manual messaging for extractions, various batches
        BettaLimsMessageFactory bettaLimsMessageFactory = new BettaLimsMessageFactory();
        LabEventFactory labEventFactory = new LabEventFactory();
        List<String> shearingTubeBarcodes = Arrays.asList("SH1", "SH2", "SH3");
        PlateTransferEventType plateTransferEventType = bettaLimsMessageFactory.buildPlateToRack("PlatingToShearingTubes",
                "NormPlate", "CovarisRack", shearingTubeBarcodes);
        Map<String, TwoDBarcodedTube> mapBarcodeToTargetTubes = new HashMap<String, TwoDBarcodedTube>();
        LabEvent labEvent = labEventFactory.buildFromBettaLimsPlateToRackDbFree(plateTransferEventType,
                new StaticPlate("NormPlate", StaticPlate.PlateType.Eppendorf96), mapBarcodeToTargetTubes);
        // Bucket for Shearing - enters from workflow?
        BucketBean bucketBean = new BucketBean();
        WorkflowLoader workflowLoader = new WorkflowLoader();
        WorkflowConfig workflowConfig = workflowLoader.load();
        ProductWorkflowDef productWorkflowDef = workflowConfig.getWorkflowByName(productOrder1.getProduct().getWorkflowName());
        ProductWorkflowDefVersion productWorkflowDefVersion = productWorkflowDef.getEffectiveVersion();
        // todo get from message event name to bucket def
        Bucket shearingBucket = new Bucket(new WorkflowBucketDef("Shearing"));
        // todo plates vs tubes?
        final Person testActor = new Person ();
        bucketBean.add(productOrder1.getBusinessKey(), new LinkedList<LabVessel>(labEvent.getTargetLabVessels()), shearingBucket, testActor );
        // - Deck calls web service to verify source barcodes?
        // - Deck calls web service to validate next action against workflow and batch
        // Decks (BSP and Sequencing) send messages to Mercury, first message auto-drains bucket

        bucketBean.start(testActor,
                         new LinkedList<LabVessel> ()/* TODO SGM Need to define Vessels from test message*/,
                         shearingBucket,"");

        // Various messages advance workflow (test JMS vs JAX-RS)
        // Non ExEx messages handled by BettaLIMS
        // Automation uploads QC
        // Operator views recently handled plasticware
        // Operator visits check point page, chooses re-entry point for rework (type 1)
        // Operator makes note and adds attachment
        // - View batch RAP sheet, add items
        // - View sample RAP sheet, including notes
        // - View Product Order Status (Detail?), including rework and notes
        // - Mark plastic dead
        // Lookup plastic
        // - Search (future)
        // - PdM abandons sample (PMs notified)
        // Add library to queue for sequencing
        // Register library to MiSeq
        // Create pool group
        // Pooling calculator
        // Strip Tube B
        // Create Flowcell JIRA
        // MiSeq reagent block transfer message
        // Register run
        // Analysis calls ZIMS
        // BASS?
        // View PDO details screen, including analysis progress, aggregations and sequencing metrics
        // PDM marks sample in PDO complete, PM notified
        // Billing
        // Submissions
        // Reporting
    }
}
