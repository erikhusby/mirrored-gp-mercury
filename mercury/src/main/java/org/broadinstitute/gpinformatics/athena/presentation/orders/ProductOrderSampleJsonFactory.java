package org.broadinstitute.gpinformatics.athena.presentation.orders;

import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.infrastructure.bsp.LabEventSampleDTO;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.Format;
import java.util.Date;

public class ProductOrderSampleJsonFactory {

    private static final Format dateFormatter = FastDateFormat.getInstance(CoreActionBean.DATE_PATTERN);
    private static final Log log = LogFactory.getLog(ProductOrderSampleJsonFactory.class);

    public JSONObject toJson(ProductOrderSample productOrderSample) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(BspSampleData.SAMPLE_ID, productOrderSample.getSampleKey());
        if (productOrderSample.isInBspFormat()) {
            setupSampleDTOItems(productOrderSample, jsonObject);
        } else {
            setupEmptyItems(productOrderSample, jsonObject);
        }
        return jsonObject;
    }

    private void populateBspValues(JSONObject jsonObject, ProductOrderSample productOrderSample) throws JSONException {
        SampleData sampleData = productOrderSample.getSampleData();
        setCollaboratorSampleId(jsonObject, sampleData.getCollaboratorsSampleName());
        setPatientId(jsonObject, sampleData.getPatientId());
    }

    private void populateEmptyValues(JSONObject jsonObject) throws JSONException {
        setCollaboratorSampleId(jsonObject, "");
        setPatientId(jsonObject, "");
    }

    private void setCollaboratorSampleId(JSONObject jsonObject, String value) throws JSONException {
        jsonObject.put(BspSampleData.COLLABORATOR_SAMPLE_ID, value);
    }

    private void setPatientId(JSONObject jsonObject, String value) throws JSONException {
        jsonObject.put(BspSampleData.PATIENT_ID, value);
    }

    public static void setupSampleDTOItems(ProductOrderSample sample, JSONObject item) throws JSONException {
        SampleData sampleData = sample.getSampleData();

        item.put(BspSampleData.SAMPLE_ID, sample.getProductOrderSampleId());
        item.put(BspSampleData.COLLABORATOR_SAMPLE_ID, sampleData.getCollaboratorsSampleName());
        item.put(BspSampleData.PATIENT_ID, sampleData.getPatientId());
        item.put(BspSampleData.COLLABORATOR_PARTICIPANT_ID, sampleData.getCollaboratorParticipantId());
        item.put(BspSampleData.SAMPLE_TYPE, sampleData.getSampleType());
        // This is here because ServiceAccessUtility doesn't work DATABASE_FREE
        try {
            item.put(BspSampleData.MATERIAL_TYPE, sample.getLatestMaterialType());
        } catch (Exception e) {
            log.error("Could not get gt material type from sample");
        }
        item.put(BspSampleData.VOLUME, sampleData.getVolume());
        item.put(BspSampleData.CONCENTRATION, sampleData.getConcentration());
        item.put(BspSampleData.JSON_RIN_KEY, sampleData.getRawRin());
        item.put(BspSampleData.JSON_RQS_KEY, sampleData.getRqs());
        item.put(BspSampleData.JSON_DV200_KEY, sampleData.getDv200());
        item.put(BspSampleData.PICO_DATE, formatPicoRunDate(sampleData.getPicoRunDate(), "No Pico"));
        item.put(BspSampleData.TOTAL, sampleData.getTotal());
        item.put(BspSampleData.HAS_SAMPLE_KIT_UPLOAD_RACKSCAN_MISMATCH,
                sampleData.getHasSampleKitUploadRackscanMismatch());
        item.put(BspSampleData.COMPLETELY_BILLED, sample.isCompletelyBilled());

        LabEventSampleDTO labEventSampleDTO = sample.getLabEventSampleDTO();

        item.put(BspSampleData.RECEIPT_DATE, sample.getFormattedReceiptDate());
        if (labEventSampleDTO != null) {
            item.put(BspSampleData.PACKAGE_DATE, labEventSampleDTO.getSamplePackagedDate());
        } else {
            item.put(BspSampleData.PACKAGE_DATE, "");
            item.put(BspSampleData.RECEIPT_DATE, "");
        }
    }

    private static String formatPicoRunDate(Date picoRunDate, String defaultReturn) {

        String returnValue = defaultReturn;
        if (picoRunDate != null) {
            returnValue = dateFormatter.format(picoRunDate);
        }

        return returnValue;
    }

    public static void setupEmptyItems(ProductOrderSample sample, JSONObject item) throws JSONException {
        item.put(BspSampleData.SAMPLE_ID, sample.getProductOrderSampleId());
        item.put(BspSampleData.COLLABORATOR_SAMPLE_ID, "");
        // This is here because ServiceAccessUtility doesn't work DATABASE_FREE
        try {
            item.put(BspSampleData.MATERIAL_TYPE, sample.getLatestMaterialType());
        } catch (Exception e) {
            log.error("Could not get gt material type from sample");
        }
        item.put(BspSampleData.PATIENT_ID, "");
        item.put(BspSampleData.COLLABORATOR_PARTICIPANT_ID, "");
        item.put(BspSampleData.SAMPLE_TYPE, "");
        item.put(BspSampleData.VOLUME, "");
        item.put(BspSampleData.CONCENTRATION, "");
        item.put(BspSampleData.JSON_RIN_KEY, "");
        item.put(BspSampleData.JSON_RQS_KEY, "");
        item.put(BspSampleData.PICO_DATE, "");
        item.put(BspSampleData.TOTAL, "");
        item.put(BspSampleData.HAS_SAMPLE_KIT_UPLOAD_RACKSCAN_MISMATCH, "");
        item.put(BspSampleData.PACKAGE_DATE, "");
        item.put(BspSampleData.RECEIPT_DATE, "");
    }
}
