/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2015 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.mercury.entity.bucket;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;

import java.util.Collections;
import java.util.Date;

public class BucketTestFactory {
    public static Bucket getBucket(ProductOrder productOrder, String barcode, String bucketName,
                                   BucketEntry.BucketEntryType entryType) {
        Bucket bucket = new Bucket(bucketName);
        BarcodedTube vessel = new BarcodedTube(barcode);
        BucketEntry bucketEntry = bucket.addEntry(productOrder, vessel, BucketEntry.BucketEntryType.PDO_ENTRY,
                Workflow.AGILENT_EXOME_EXPRESS);
        if (entryType == BucketEntry.BucketEntryType.REWORK_ENTRY) {
            bucket.removeEntry(bucketEntry);
            bucketEntry.setEntryType(entryType);
            LabEvent reworkEvent =
                    new LabEvent(LabEventType.PICO_BUFFER_ADDITION, new Date(), "here", 0l,
                            ProductOrderTestFactory.TEST_CREATOR, "foo");

            ReworkDetail reworkDetail =
                    new ReworkDetail(new ReworkReason("foo"), ReworkLevel.ONE_SAMPLE_HOLD_REST_BATCH,
                            LabEventType.PICO_REWORK, "hola", reworkEvent);
            bucketEntry.setReworkDetail(reworkDetail);
            bucket.addReworkEntry(bucketEntry);
        }
        bucketEntry.setLabBatch(new LabBatch("batchName", Collections.<LabVessel>singleton(vessel),
                LabBatch.LabBatchType.WORKFLOW));

        return bucket;
    }
}
