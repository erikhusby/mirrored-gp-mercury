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
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class BucketEntryJsonFactory {
    public static final String ENTRY_ID = "ENTRY_ID";
    public static final String LAB_VESSEL = "LAB_VESSEL";
    public static final String SAMPLE_ID = "SAMPLE_ID";
    public static final String MATERIAL_TYPE = "MATERIAL_TYPE";
    public static final String PDO = "PDO";
    public static final String PDO_NAME = "PDO_NAME";
    public static final String PDO_OWNER = "PDO_OWNER";
    public static final String BATCH_NAME = "BATCH_NAME";
    public static final String WORKFLOW = "WORKFLOW";
    public static final String PRODUCT = "PRODUCT";
    public static final String PRODUCT_ADDONS = "PRODUCT_ADDONS";
    public static final String RECEIPT_DATE = "RECEIPT_DATE";
    public static final String CREATED_DATE = "CREATED_DATE";
    public static final String ENTRY_TYPE = "ENTRY_TYPE";
    public static final String REWORK_REASON = "REWORK_REASON";
    public static final String REWORK_COMMENT = "REWORK_COMMENT";
    public static final String REWORK_USER = "REWORK_USER";
    public static final String REWORK_DATE = "REWORK_DATE";
    private static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance("MM/dd/yyyy HH:mm");


    public static JSONObject toJson(BucketEntry bucketEntry, BucketViewActionBean actionBean) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(ENTRY_ID, bucketEntry.getBucketEntryId());
        jsonObject.put(LAB_VESSEL, bucketEntry.getLabVessel().getLabel());
        StringBuilder sampleKeyBuilder = new StringBuilder();
        for (MercurySample mercurySample : bucketEntry.getLabVessel().getMercurySamples()) {
            sampleKeyBuilder.append(mercurySample.getSampleKey());
        }
        jsonObject.put(SAMPLE_ID, sampleKeyBuilder.toString());
        jsonObject.put(MATERIAL_TYPE,
                bucketEntry.getLabVessel().getLatestMaterialType().getDisplayName());
        jsonObject.put(PDO, actionBean.getLink(bucketEntry.getProductOrder().getJiraTicketKey()));
        jsonObject.put(PDO_NAME, bucketEntry.getProductOrder().getTitle());
        jsonObject.put(PDO_OWNER, actionBean.getUserFullName(bucketEntry.getProductOrder().getCreatedBy()));
        if (bucketEntry.getLabBatch() != null) {
            jsonObject.put(BATCH_NAME, actionBean.getLink(bucketEntry.getLabBatch().getJiraTicket().getTicketName()));
        }

        jsonObject.put(WORKFLOW, bucketEntry.getWorkflowNames());
        jsonObject.put(PRODUCT, bucketEntry.getProductOrder().getProduct().getName());
        jsonObject.put(PRODUCT_ADDONS, bucketEntry.getProductOrder().getAddOnList("<br/>"));

        Set<String> dates = new HashSet<>();
        for (MercurySample mercurySample : bucketEntry.getLabVessel().getMercurySamples()) {
            if (mercurySample.getReceivedDate() != null) {
                dates.add(DATE_FORMAT.format(mercurySample.getReceivedDate()));
            }
        }
        jsonObject.put(RECEIPT_DATE, dates);
        jsonObject.put(CREATED_DATE, DATE_FORMAT.format(bucketEntry.getCreatedDate()));
        jsonObject.put(ENTRY_TYPE, bucketEntry.getEntryType().getName());

        if (bucketEntry.getReworkDetail() != null) {
            String reworkReason = bucketEntry.getReworkDetail().getReason().getReason();
            String reworkComment = bucketEntry.getReworkDetail().getComment();
            String reworkUser = actionBean
                    .getUserFullName(bucketEntry.getReworkDetail().getAddToReworkBucketEvent().getEventOperator());
            Date reworkDate = bucketEntry.getReworkDetail().getAddToReworkBucketEvent().getEventDate();
            jsonObject.put(REWORK_REASON, reworkReason);
            jsonObject.put(REWORK_COMMENT, reworkComment);
            jsonObject.put(REWORK_USER, reworkUser);
            if (reworkDate!=null) {
                jsonObject.put(REWORK_DATE, DATE_FORMAT.format(reworkDate));

            }
        }
        return jsonObject;
    }
}
