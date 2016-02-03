/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2016 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.mercury.presentation.workflow;

import org.apache.commons.lang3.time.FastDateFormat;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

public class BucketEntryJsonFactory {
    public static JSONObject toJson(BucketEntry bucketEntry, BucketViewActionBean actionBean) throws JSONException {
        JSONObject jsonObject = new JSONObject();
//        jsonObject.put("ENTRY_ID", bucketEntry.getBucketEntryId());
        jsonObject.put(Metadata.Key.MATERIAL_TYPE.name(), bucketEntry.getLabVessel().getLatestMaterialType().getDisplayName());
        jsonObject.put("WORKFLOW", bucketEntry.getWorkflowNames());
        Set<String> dates = new HashSet<>();
        FastDateFormat fastDateFormat = FastDateFormat.getInstance("MM/dd/yyyy HH:mm");
        for (MercurySample mercurySample : bucketEntry.getLabVessel().getMercurySamples()) {
            if (mercurySample.getReceivedDate()!=null) {
                dates.add(fastDateFormat.format(mercurySample.getReceivedDate()));
            }
        }
        jsonObject.put("RECEIPT_DATE", dates);

        StringBuilder batchBuilder=new StringBuilder();
        for (LabBatch labBatch : bucketEntry.getLabVessel().getNearestWorkflowLabBatches()) {
            batchBuilder.append(actionBean.getLink(labBatch.getBusinessKey())).append(" ");
        }
        jsonObject.put("BATCHES", batchBuilder.toString());
        return jsonObject;
    }
}
