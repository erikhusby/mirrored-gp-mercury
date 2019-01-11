/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2017 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.athena.presentation.orders;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.bsp.LabEventSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.presentation.SampleLink;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.datatables.DatatablesStateSaver;
import org.codehaus.jackson.annotate.JsonProperty;

import java.text.Format;
import java.util.Arrays;
import java.util.Date;
import java.util.List;


public class ProductOrderSampleBean {
    private static final Format dateFormatter = FastDateFormat.getInstance(CoreActionBean.DATE_PATTERN);
    public static final Log log = LogFactory.getLog(ProductOrderSampleBean.class);
    public static final String RECORDS_TOTAL = "recordsTotal";
    public static final String DATA_FIELD = "data";
    public static final String SAMPLE_DATA_ROW_COUNT = "rowsWithSampleData";
    public static final String SAMPLES_NOT_RECEIVED = "numberSamplesNotReceived";
    public static final String UNIQUE_ROW_IDENTIFIER = "rowId";
    public static final String SAMPLE_ID = "ID";
    public static final String PRODUCT_ORDER_SAMPLE_ID = "PRODUCT_ORDER_SAMPLE_ID";
    public static final String SAMPLE_LINK = "SAMPLE_LINK";
    public static final String AGGREGATION_PARTICLE = "Aggregation Particle";
    public static final String COLLABORATOR_SAMPLE_ID = "Collaborator Sample ID";
    public static final String PARTICIPANT_ID = "Participant ID";
    public static final String COLLABORATOR_PARTICIPANT_ID = "Collaborator Participant ID";
    public static final String MATERIAL_TYPE = "Material Type";
    public static final String RIN = "RIN";
    public static final String DV2000 = "DV2000";
    public static final String RQS = "RQS";
    public static final String SAMPLE_TYPE = "Sample Type";
    public static final String PICO_RUN_DATE = "Last Pico Run Date";
    public static final String VOLUME = "Volume";
    public static final String YIELD_AMOUNT = "Yield Amount";
    public static final String RACKSCAN_MISMATCH = "Rackscan Mismatch";
    public static final String RACKSCAN_MISMATCH_DETAILS = "Rackscan Mismatch Details";
    public static final String CONCENTRATION = "Concentration";
    public static final String BILLED = "Billed";
    public static final String BILLED_DETAILS = "Billed Details";
    public static final String RECEIVED_DATE = "Received Date";
    public static final String SHIPPED_DATE = "Shipped Date";
    public static final String ON_RISK = "On Risk";
    public static final String RISK_STRING = "Risk String";
    public static final String ON_RISK_DETAILS = "On Risk Details";
    public static final String PROCEED_OOS = "Proceed OOS";
    public static final String STATUS = "Status";
    public static final String COMMENT = "Comment";
    public static final String ROW_ID_PREFIX = "sampleId-";
    public static final String POSITION = "#";

    public static final List<String> SLOW_COLUMNS = Arrays
        .asList(ProductOrderSampleBean.MATERIAL_TYPE, ProductOrderSampleBean.RECEIVED_DATE,
            ProductOrderSampleBean.SHIPPED_DATE);

    @JsonProperty(COLLABORATOR_SAMPLE_ID)
    private String collaboratorSampleId = "";
    @JsonProperty(AGGREGATION_PARTICLE)
    private String aggregationParticle = "";
    @JsonProperty(PRODUCT_ORDER_SAMPLE_ID)
    private Long productOrderSampleId;
    @JsonProperty(UNIQUE_ROW_IDENTIFIER)
    private String uniqueRowIdentifier = "";
    @JsonProperty(SAMPLE_ID)
    private String sampleId = "";
    @JsonProperty(SAMPLE_LINK)
    private String sampleLinkString = "";
    @JsonProperty(POSITION)
    private Integer position;
    @JsonProperty(PARTICIPANT_ID)
    private String patientId = "";
    @JsonProperty(COLLABORATOR_PARTICIPANT_ID)
    private String collaboratorParticipantId = "";
    @JsonProperty(MATERIAL_TYPE)
    private String materialType = "";
    @JsonProperty(PROCEED_OOS)
    private String proceedOutOfSpec="";
    @JsonProperty(RIN)
    private String rin = "";
    @JsonProperty(DV2000)
    private Double dv2000;
    @JsonProperty(STATUS)
    private String status = "";
    @JsonProperty(COMMENT)
    private String comment = "";
    @JsonProperty(RISK_STRING)
    private String riskString = "";
    @JsonProperty(RQS)
    private Double rqs;
    @JsonProperty(SAMPLE_TYPE)
    private String sampleType = "";
    @JsonProperty(PICO_RUN_DATE)
    private String picoDate = "";
    @JsonProperty(VOLUME)
    private Double volume;
    @JsonProperty(YIELD_AMOUNT)
    private Double total;
    @JsonProperty(CONCENTRATION)
    private Double concentration;
    @JsonProperty(RACKSCAN_MISMATCH)
    private boolean hasSampleKitUploadRackscanMismatch;
    @JsonProperty(RACKSCAN_MISMATCH_DETAILS)
    private String sampleKitUploadRackscanMismatchDetails;
    @JsonProperty(BILLED)
    private boolean completelyBilled;
    @JsonProperty(BILLED_DETAILS)
    private String completelyBilledDetails;
    @JsonProperty(RECEIVED_DATE)
    private String receiptDate = "";
    @JsonProperty(SHIPPED_DATE)
    private String packageDate="";
    @JsonProperty(ON_RISK)
    private boolean onRisk;
    @JsonProperty(ON_RISK_DETAILS)
    private String onRiskDetails = "";

    private ProductOrderSample sample;
    @JsonProperty("includeSampleData")
    private boolean includeSampleData;
    private boolean initialLoad;
    private DatatablesStateSaver preferenceSaver;
    private SampleLink sampleLink;

    public ProductOrderSampleBean(ProductOrderSample sample, boolean includeSampleData,
                                  boolean initialLoad, DatatablesStateSaver preferenceSaver, SampleLink sampleLink) {
        this.sample = sample;
        this.initialLoad = initialLoad;
        this.preferenceSaver = preferenceSaver;
        this.sampleLink = sampleLink;
        this.includeSampleData = includeSampleData;
        setupSampleDTOItems();
    }


    private void setupSampleDTOItems() {
        productOrderSampleId = sample.getProductOrderSampleId();
        uniqueRowIdentifier = ROW_ID_PREFIX + sample.getProductOrderSampleId();
        if (preferenceSaver.showColumn(SAMPLE_ID)) {
            sampleId = sample.getSampleKey();
            if (sampleLink != null && sampleLink.getHasLink()) {
                sampleLinkString =
                        String.format("<a href='%s' class='external' target='%s' title='%s' data-sample-name='%s'>%s</>",
                                sampleLink.getUrl(),
                                sampleLink.getTarget(), sampleLink.getLabel(), sample.getName(), sample.getName());
            } else {
                sampleLinkString = sample.getName();
            }
        }
        if (preferenceSaver.showColumn(POSITION)) {
            position = sample.getSamplePosition() + 1;
        }
        if (initialLoad) {
            if (preferenceSaver.showColumn(BILLED_DETAILS)) {
                completelyBilled = sample.isCompletelyBilled();
                if(sample.isCompletelyBilled()) {
                    completelyBilledDetails =
                            buildBeenBilledDiv(sample, sample.getSampleKey() + "<BR>\n" +
                                                       StringUtils.join(sample.completelyBilledDetails(), "<br>\n"));
                }
            }
            if (preferenceSaver.showColumn(ON_RISK)) {
                onRisk = sample.isOnRisk();
                riskString = onRisk ? buildRiskDiv(sample) : "";
            }
        }

        if (!includeSampleData) {
            return;
        }

        if (preferenceSaver.showColumn(COMMENT)) {
            comment = sample.getSampleComment();
        }
        if (preferenceSaver.showColumn(PROCEED_OOS)) {
            if (sample.getProceedIfOutOfSpec() != null) {
                proceedOutOfSpec = sample.getProceedIfOutOfSpec().getDisplayName();
            }
        }
        if (preferenceSaver.showColumn(STATUS)) {
            status = sample.getDeliveryStatus().getDisplayName();
        }

        if (preferenceSaver.showColumn(SHIPPED_DATE)) {
            LabEventSampleDTO labEventSampleDTO = sample.getLabEventSampleDTO();
            if (labEventSampleDTO != null) {
                packageDate = labEventSampleDTO.getSamplePackagedDate();
            }
        }


        if (includeSampleData) {
            SampleData sampleData = sample.getSampleData();
            if (preferenceSaver.showColumn(COLLABORATOR_SAMPLE_ID)) {
                collaboratorSampleId = sampleData.getCollaboratorsSampleName();
            }
            if (sample.getProductOrder().getJiraTicketKey()!=null && preferenceSaver.showColumn(AGGREGATION_PARTICLE)) {
                aggregationParticle = sample.getAggregationParticleDisplayValue();
            }
            if (preferenceSaver.showColumn(PARTICIPANT_ID)) {
                patientId = sampleData.getPatientId();
            }
            if (preferenceSaver.showColumn(COLLABORATOR_PARTICIPANT_ID)) {
                collaboratorParticipantId = sampleData.getCollaboratorParticipantId();
            }
            if (preferenceSaver.showColumn(SAMPLE_TYPE)) {
                sampleType = sampleData.getSampleType();
            }
            // This is here because ServiceAccessUtility doesn't work DATABASE_FREE
            if (preferenceSaver.showColumn(MATERIAL_TYPE)) {
                try {
                    materialType = sample.getLatestMaterialType();
                } catch (Exception e) {
                    log.error("Could not get gt material type from sample");
                }
            }
            if (preferenceSaver.showColumn(VOLUME)) {
                volume = sampleData.getVolume();
            }
            if (preferenceSaver.showColumn(YIELD_AMOUNT)) {
                total = sampleData.getTotal();
            }
            if (preferenceSaver.showColumn(CONCENTRATION)) {
                concentration = sampleData.getConcentration();
            }
            if (preferenceSaver.showColumn(RIN)) {
                rin = sampleData.getRawRin();
            }
            if (preferenceSaver.showColumn(RQS)) {
                rqs = sampleData.getRqs();
            }
            if (preferenceSaver.showColumn(DV2000)) {
                dv2000 = sampleData.getDv200();
            }
            if (preferenceSaver.showColumn(PICO_RUN_DATE)) {
                picoDate = formatPicoRunDate(sampleData.getPicoRunDate(), "");
            }
            if (preferenceSaver.showColumn(RACKSCAN_MISMATCH_DETAILS)) {
                hasSampleKitUploadRackscanMismatch = sampleData.getHasSampleKitUploadRackscanMismatch();
                if(sampleData.getHasSampleKitUploadRackscanMismatch()) {
                    sampleKitUploadRackscanMismatchDetails =
                            buildCheckColumnDiv(sample, "Rack Scan Mismatched for",
                                    sample.getBusinessKey() + " has a rack scan mismatch");
                }
            }
            if (preferenceSaver.showColumn(RECEIVED_DATE)) {
                receiptDate = sample.getFormattedReceiptDate();
            }
        }
    }

    private String buildBeenBilledDiv(ProductOrderSample sample, String billedDetailData) {
        String billedDiv = buildCheckColumnDiv(sample, "Billed details for", billedDetailData);
        return billedDiv;
    }

    private String buildRiskDiv(ProductOrderSample sample) {
        String riskDiv = buildCheckColumnDiv(sample, "On Risk Details for", sample.getRiskString());
        return riskDiv;
    }

    private String buildCheckColumnDiv(ProductOrderSample sample, final String titlePrefix, String detailData) {
        return String.format(
                "<div class=\"onRisk\" title=\"" + titlePrefix
                + " %s\" rel=\"popover\" data-trigger=\"hover\" data-placement=\"left\" data-html=\"true\" data-content=\"<div style='text-align: left; white-space: normal; word-break: break-word;'>%s</div>\"><img src=\"/Mercury/images/check.png\">...</div>",
                    sample.getSampleKey(), detailData);
    }

    private static String formatPicoRunDate(Date picoRunDate, String defaultReturn) {
        String returnValue = defaultReturn;
        if (picoRunDate != null) {
            returnValue = dateFormatter.format(picoRunDate);
        }

        return returnValue;
    }

    public String getUniqueRowIdentifier() {
        return uniqueRowIdentifier;
    }

    public void setUniqueRowIdentifier(String uniqueRowIdentifier) {
        this.uniqueRowIdentifier = uniqueRowIdentifier;
    }

    public String getSampleId() {
        return sampleId;
    }

    public void setSampleId(String sampleId) {
        this.sampleId = sampleId;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public String getCollaboratorSampleId() {
        return collaboratorSampleId;
    }

    public void setCollaboratorSampleId(String collaboratorSampleId) {
        this.collaboratorSampleId = collaboratorSampleId;
    }

    public Long getProductOrderSampleId() {
        return productOrderSampleId;
    }

    public String getSampleLinkString() {
        return sampleLinkString;
    }

    public void setSampleLinkString(String sampleLinkString) {
        this.sampleLinkString = sampleLinkString;
    }

    public void setProductOrderSampleId(Long productOrderSampleId) {
        this.productOrderSampleId = productOrderSampleId;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getCollaboratorParticipantId() {
        return collaboratorParticipantId;
    }

    public void setCollaboratorParticipantId(String collaboratorParticipantId) {
        this.collaboratorParticipantId = collaboratorParticipantId;
    }

    public String getMaterialType() {
        return materialType;
    }

    public void setMaterialType(String materialType) {
        this.materialType = materialType;
    }

    public String getRin() {
        return rin;
    }

    public void setRin(String rin) {
        this.rin = rin;
    }

    public Double getDv2000() {
        return dv2000;
    }

    public void setDv2000(Double dv2000) {
        this.dv2000 = dv2000;
    }

    public Double getRqs() {
        return rqs;
    }

    public void setRqs(Double rqs) {
        this.rqs = rqs;
    }

    public String getSampleType() {
        return sampleType;
    }

    public void setSampleType(String sampleType) {
        this.sampleType = sampleType;
    }

    public String getPicoDate() {
        return picoDate;
    }

    public void setPicoDate(String picoDate) {
        this.picoDate = picoDate;
    }

    public Double getVolume() {
        return volume;
    }

    public void setVolume(Double volume) {
        this.volume = volume;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public Double getConcentration() {
        return concentration;
    }

    public void setConcentration(Double concentration) {
        this.concentration = concentration;
    }

    public boolean isHasSampleKitUploadRackscanMismatch() {
        return hasSampleKitUploadRackscanMismatch;
    }

    public void setHasSampleKitUploadRackscanMismatch(boolean hasSampleKitUploadRackscanMismatch) {
        this.hasSampleKitUploadRackscanMismatch = hasSampleKitUploadRackscanMismatch;
    }

    public boolean isCompletelyBilled() {
        return completelyBilled;
    }

    public void setCompletelyBilled(boolean completelyBilled) {
        this.completelyBilled = completelyBilled;
    }

    public String getReceiptDate() {
        return receiptDate;
    }

    public void setReceiptDate(String receiptDate) {
        this.receiptDate = receiptDate;
    }

    public String getPackageDate() {
        return packageDate;
    }

    public void setPackageDate(String packageDate) {
        this.packageDate = packageDate;
    }

    public boolean isOnRisk() {
        return onRisk;
    }

    public void setOnRisk(boolean onRisk) {
        this.onRisk = onRisk;
    }

    public String getOnRiskDetails() {
        return onRiskDetails;
    }

    public void setOnRiskDetails(String onRiskDetails) {
        this.onRiskDetails = onRiskDetails;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public String getProceedOutOfSpec() {
        return proceedOutOfSpec;
    }

    public void setProceedOutOfSpec(String proceedOutOfSpec) {
        this.proceedOutOfSpec = proceedOutOfSpec;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getRiskString() {
        return riskString;
    }

    public void setRiskString(String riskString) {
        this.riskString = riskString;
    }


}
