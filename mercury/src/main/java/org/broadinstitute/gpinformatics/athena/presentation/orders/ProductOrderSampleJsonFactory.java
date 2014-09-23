package org.broadinstitute.gpinformatics.athena.presentation.orders;

import org.apache.commons.lang3.time.FastDateFormat;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.LabEventSampleDTO;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.Format;
import java.util.Date;

public class ProductOrderSampleJsonFactory {

    private static final Format dateFormatter = FastDateFormat.getInstance(CoreActionBean.DATE_PATTERN);

    public JSONObject toJson(ProductOrderSample productOrderSample) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(BSPSampleDTO.SAMPLE_ID, productOrderSample.getSampleKey());
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
        jsonObject.put(BSPSampleDTO.COLLABORATOR_SAMPLE_ID, value);
    }

    private void setPatientId(JSONObject jsonObject, String value) throws JSONException {
        jsonObject.put(BSPSampleDTO.PATIENT_ID, value);
    }

    public static void setupSampleDTOItems(ProductOrderSample sample, JSONObject item) throws JSONException {
        SampleData sampleData = sample.getSampleData();

        item.put(BSPSampleDTO.SAMPLE_ID, sample.getProductOrderSampleId());
        item.put(BSPSampleDTO.COLLABORATOR_SAMPLE_ID, sampleData.getCollaboratorsSampleName());
        item.put(BSPSampleDTO.PATIENT_ID, sampleData.getPatientId());
        item.put(BSPSampleDTO.COLLABORATOR_PARTICIPANT_ID, sampleData.getCollaboratorParticipantId());
        item.put(BSPSampleDTO.VOLUME, sampleData.getVolume());
        item.put(BSPSampleDTO.CONCENTRATION, sampleData.getConcentration());
        item.put(BSPSampleDTO.JSON_RIN_KEY, sampleData.getRawRin());
        item.put(BSPSampleDTO.JSON_RQS_KEY, sampleData.getRqs());
        item.put(BSPSampleDTO.PICO_DATE, formatPicoRunDate(sampleData.getPicoRunDate(), "No Pico"));
        item.put(BSPSampleDTO.TOTAL, sampleData.getTotal());
        item.put(BSPSampleDTO.HAS_SAMPLE_KIT_UPLOAD_RACKSCAN_MISMATCH,
                sampleData.getHasSampleKitUploadRackscanMismatch());
        item.put(BSPSampleDTO.COMPLETELY_BILLED, sample.isCompletelyBilled());

        LabEventSampleDTO labEventSampleDTO = sample.getLabEventSampleDTO();

        if (labEventSampleDTO != null) {
            item.put(BSPSampleDTO.PACKAGE_DATE, labEventSampleDTO.getSamplePackagedDate());
            item.put(BSPSampleDTO.RECEIPT_DATE, labEventSampleDTO.getSampleReceiptDate());
        } else {
            item.put(BSPSampleDTO.PACKAGE_DATE, "");
            item.put(BSPSampleDTO.RECEIPT_DATE, "");
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
        item.put(BSPSampleDTO.SAMPLE_ID, sample.getProductOrderSampleId());
        item.put(BSPSampleDTO.COLLABORATOR_SAMPLE_ID, "");
        item.put(BSPSampleDTO.PATIENT_ID, "");
        item.put(BSPSampleDTO.COLLABORATOR_PARTICIPANT_ID, "");
        item.put(BSPSampleDTO.VOLUME, "");
        item.put(BSPSampleDTO.CONCENTRATION, "");
        item.put(BSPSampleDTO.JSON_RIN_KEY, "");
        item.put(BSPSampleDTO.JSON_RQS_KEY, "");
        item.put(BSPSampleDTO.PICO_DATE, "");
        item.put(BSPSampleDTO.TOTAL, "");
        item.put(BSPSampleDTO.HAS_SAMPLE_KIT_UPLOAD_RACKSCAN_MISMATCH, "");
        item.put(BSPSampleDTO.PACKAGE_DATE, "");
        item.put(BSPSampleDTO.RECEIPT_DATE, "");
    }
}
