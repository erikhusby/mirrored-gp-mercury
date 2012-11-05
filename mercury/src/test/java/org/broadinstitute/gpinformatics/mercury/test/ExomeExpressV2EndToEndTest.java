package org.broadinstitute.gpinformatics.mercury.test;

import org.testng.annotations.Test;

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
        // Define travel group (can be independent of product order)
        // {Create kit and Ship kit}
        // Upload metadata through BSP portal
        // {Receive samples} (There are some hooks into product order so the receipt team can direct samples)
        // 3 stars align notification
        // BSP notifies Mercury of existence of samples in plastic
        // Buckets for Extraction and BSP Pico?
        // Add samples to bucket
        // Drain bucket into batch
        // Create JIRA ticket configured in bucket (There is a ticket for an LCSet but none for the extraction/pico work)
        // BSP registers batch in Mercury
        // BSP Manual messaging for extractions, various batches
        // Bucket for Shearing
        // Deck calls web service to verify source barcodes?
        // Deck calls web service to validate next action against workflow and batch
        // Decks (BSP and Sequencing) send messages to Mercury, first message auto-drains bucket
        // Various messages advance workflow (test JMS vs JAX-RS)
        // Non ExEx messages handled by BettaLIMS
        // Automation uploads QC
        // Operator views recently handled plasticware
        // Operator visits check point page, chooses re-entry point for rework (type 1)
        // Operator makes note and adds attachment
        // View batch RAP sheet, add items
        // View sample RAP sheet, including notes
        // View Product Order Status (Detail?), including rework and notes
        // Mark plastic dead
        // Lookup plastic
        // Search (future)
        // PdM abandons sample (PMs notified)
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
