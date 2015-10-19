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

package org.broadinstitute.gpinformatics.mercury.presentation.workflow;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BucketViewJsonFactory {
    private static final Log log = LogFactory.getLog(BucketViewJsonFactory.class);

    public static final String REWORK_REASON = "Rework Reason";
    public static final String REWORK_COMMENT = "Rework Comment";
    public static final String REWORK_USER = "Rework User";
    public static final String REWORK_DATE = "Rework Date";
    public static final String BATCH_NAME = "Batch Name";
    public static final String BUCKET_ENTRY_TYPE = "Bucket Entry Type";
    public static final String PRODUCT = "Product";
    public static final String ADD_ONS = "Add-ons";

    public static JSONArray toJson(Bucket bucket, BSPUserList bspUserList) throws JSONException {
        JSONArray jsonArray = new JSONArray();
        for (BucketEntry bucketEntry : bucket.getBucketEntries()) {
            JSONObject jsonObject = getJson(bucketEntry);
            jsonArray.put(jsonObject);
        }
        for (BucketEntry bucketEntry : bucket.getReworkEntries()) {
            JSONObject jsonObject = getJson(bucketEntry);
            jsonObject.put(REWORK_REASON, bucketEntry.getReworkDetail().getReason().getReason());
            jsonObject.put(REWORK_COMMENT, bucketEntry.getReworkDetail().getComment());
            Long eventOperator = bucketEntry.getReworkDetail().getAddToReworkBucketEvent().getEventOperator();
            jsonObject.put(REWORK_USER, bspUserList.getUserFullName(eventOperator));
            Date eventDate = bucketEntry.getReworkDetail().getAddToReworkBucketEvent().getEventDate();
            jsonObject.put(REWORK_DATE, FastDateFormat.getInstance("MM/dd/yyyy HH:mm:ss").format(eventDate));
            jsonArray.put(jsonObject);

        }
        return jsonArray;
    }

    private static JSONObject getJson(BucketEntry bucketEntry) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(BspSampleData.MATERIAL_TYPE,
                    bucketEntry.getLabVessel().getLatestMaterialType().getDisplayName());
        } catch (Exception e) {
            log.error("Could not get gt material type from sample");
        }
        List<String> batches = new ArrayList<>();
        for (LabBatch labBatch : bucketEntry.getLabVessel().getNearestWorkflowLabBatches()) {
            batches.add(labBatch.getBusinessKey());
        }
        jsonObject.put(BATCH_NAME, StringUtils.join(batches, ' '));
        jsonObject.put(BUCKET_ENTRY_TYPE, bucketEntry.getEntryType().getName());
        jsonObject.put(PRODUCT, bucketEntry.getProductOrder().getProduct().getName());
        jsonObject.put(ADD_ONS, bucketEntry.getProductOrder().getAddOnList());
        return jsonObject;
    }
}
