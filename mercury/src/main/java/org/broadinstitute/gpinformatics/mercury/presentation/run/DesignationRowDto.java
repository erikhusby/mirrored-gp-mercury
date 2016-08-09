package org.broadinstitute.gpinformatics.mercury.presentation.run;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.run.DesignationLoadingTube;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Represents the UI data table row.
 */
public class DesignationRowDto {
    private boolean selected = false;
    private DesignationLoadingTube.Status status;
    private Date createdOn;
    private IlluminaFlowcell.FlowcellType sequencerModel;
    private DesignationLoadingTube.IndexType indexType;
    private DesignationLoadingTube.Priority priority;
    private Integer numberCycles;
    private Integer numberLanes;
    private Integer readLength;
    private BigDecimal loadingConc;
    private Boolean poolTest;
    private String barcode;
    private String lcsetUrl;
    private String primaryLcset;
    private List<String> additionalLcsets = new ArrayList<>();
    private List<String> productNames = new ArrayList<>();
    private String startingBatchVessels;
    private String tubeType;
    private String tubeDate;
    private String regulatoryDesignation;
    private int numberSamples;

    private Long designationId;
    private Long tubeEventId;
    private boolean repeatedBarcode;

    private final static String DELIMITER = "<br/>";

    public DesignationRowDto() {
        createdOn = new Date();
        status = DesignationLoadingTube.Status.UNSAVED;
    }

    public DesignationRowDto(LabVessel loadingLabVessel, Collection<LabEvent> labEvents,
                             String lcsetUrl, LabBatch primaryLcset, Collection<String> additionalLcsets,
                             Collection<String> productNames, String startingBatchVessels,
                             String regulatoryDesignation, int numberSamples,
                             IlluminaFlowcell.FlowcellType sequencerModel,
                             DesignationLoadingTube.IndexType indexType,
                             Integer numberCycles, Integer numberLanes, Integer readLength,
                             BigDecimal loadingConc, Boolean poolTest, Date createdOn,
                             DesignationLoadingTube.Status status) {

        this.barcode = loadingLabVessel.getLabel();
        SortedSet<String> tubeDates = new TreeSet<>();
        for (LabEvent labEvent : labEvents) {
            // If a tube with a known lcset has multiple of the same type of event, captures all the event dates
            // but only keeps the latest id.
            tubeDates.add(DateUtils.convertDateTimeToString(labEvent.getEventDate()));
            if (getTubeEventId() == null || labEvent.getLabEventId() > getTubeEventId()) {
                tubeType = labEvent.getLabEventType().getName().replaceAll("Transfer", "");
                setTubeEventId(labEvent.getLabEventId());
            }
        }
        this.tubeDate = StringUtils.join(tubeDates, "<br/>");
        this.primaryLcset = primaryLcset.getBatchName();
        this.lcsetUrl = lcsetUrl;
        this.additionalLcsets.addAll(additionalLcsets);
        this.productNames.addAll(productNames);
        this.startingBatchVessels = startingBatchVessels;

        this.sequencerModel = sequencerModel;
        this.indexType = indexType;
        this.numberCycles = numberCycles;
        this.numberLanes = numberLanes;
        this.readLength = readLength;
        this.loadingConc = loadingConc;
        this.poolTest = poolTest;
        this.priority = DesignationLoadingTube.Priority.NORMAL;
        this.regulatoryDesignation = regulatoryDesignation;
        this.numberSamples = numberSamples;
        this.createdOn = createdOn;
        this.status = status;
    }


    public String getAdditionalLcsetJoin() {
        return StringUtils.join(additionalLcsets, DELIMITER);
    }

    public void setAdditionalLcsetJoin(String delimitedLcsetNames) {
        additionalLcsets = Arrays.asList(delimitedLcsetNames.split(DELIMITER));
    }

    public String getProductNameJoin() {
        return StringUtils.join(productNames, DELIMITER);
    }

    public void setProductNameJoin(String delimitedProductNames) {
        productNames = Arrays.asList(delimitedProductNames.split(DELIMITER));
    }

    /**
     * Orders by barcode, then designation id (nulls last), then by descending created date (older ones last).
     * This affects how duplicates are removed by the action bean.
     */
    public static Comparator<DesignationRowDto> BY_BARCODE_ID_DATE = new Comparator<DesignationRowDto>() {
        @Override
        public int compare(DesignationRowDto o1, DesignationRowDto o2) {
            int barcodeCompare = o1.getBarcode().compareTo(o2.getBarcode());
            int idCompare = o1.getDesignationId() == null ?
                    (o2.getDesignationId() == null ? 0 : -1) :
                    (o2.getDesignationId() == null ? 1 : o1.getDesignationId().compareTo(o2.getDesignationId()));
            return barcodeCompare != 0 ? barcodeCompare : idCompare != 0 ? idCompare :
                    o2.getCreatedOn().compareTo(o1.getCreatedOn());
        }
    };

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public Date getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Date createdOn) {
        this.createdOn = createdOn;
    }

    public IlluminaFlowcell.FlowcellType getSequencerModel() {
        return sequencerModel;
    }

    public void setSequencerModel(
            IlluminaFlowcell.FlowcellType sequencerModel) {
        this.sequencerModel = sequencerModel;
    }

    public DesignationLoadingTube.IndexType getIndexType() {
        return indexType;
    }

    public void setIndexType(
            DesignationLoadingTube.IndexType indexType) {
        this.indexType = indexType;
    }

    public DesignationLoadingTube.Priority getPriority() {
        return priority;
    }

    public void setPriority(DesignationLoadingTube.Priority priority) {
        this.priority = priority;
    }

    public Integer getNumberCycles() {
        return numberCycles;
    }

    public void setNumberCycles(Integer numberCycles) {
        this.numberCycles = numberCycles;
    }

    public Integer getNumberLanes() {
        return numberLanes;
    }

    public void setNumberLanes(Integer numberLanes) {
        this.numberLanes = numberLanes;
    }

    public Integer getReadLength() {
        return readLength;
    }

    public void setReadLength(Integer readLength) {
        this.readLength = readLength;
    }

    public BigDecimal getLoadingConc() {
        return loadingConc;
    }

    public void setLoadingConc(BigDecimal loadingConc) {
        this.loadingConc = loadingConc;
    }

    public Boolean getPoolTest() {
        return poolTest;
    }

    public void setPoolTest(Boolean poolTest) {
        this.poolTest = poolTest;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getLcsetUrl() {
        return lcsetUrl;
    }

    public void setLcsetUrl(String lcsetUrl) {
        this.lcsetUrl = lcsetUrl;
    }

    public String getPrimaryLcset() {
        return primaryLcset;
    }

    public void setPrimaryLcset(String primaryLcset) {
        this.primaryLcset = primaryLcset;
    }

    public List<String> getAdditionalLcsets() {
        return additionalLcsets;
    }

    public void setAdditionalLcsets(List<String> additionalLcsets) {
        this.additionalLcsets = additionalLcsets;
    }

    public List<String> getProductNames() {
        return productNames;
    }

    public void setProductNames(List<String> productNames) {
        this.productNames = productNames;
    }

    public String getStartingBatchVessels() {
        return startingBatchVessels;
    }

    public void setStartingBatchVessels(String startingBatchVessels) {
        this.startingBatchVessels = startingBatchVessels;
    }

    public String getTubeType() {
        return tubeType;
    }

    public void setTubeType(String tubeType) {
        this.tubeType = tubeType;
    }

    public String getTubeDate() {
        return tubeDate;
    }

    public void setTubeDate(String tubeDate) {
        this.tubeDate = tubeDate;
    }

    public String getRegulatoryDesignation() {
        return regulatoryDesignation;
    }

    public void setRegulatoryDesignation(String regulatoryDesignation) {
        this.regulatoryDesignation = regulatoryDesignation;
    }

    public int getNumberSamples() {
        return numberSamples;
    }

    public void setNumberSamples(int numberSamples) {
        this.numberSamples = numberSamples;
    }

    public DesignationLoadingTube.Status getStatus() {
        return status;
    }

    public void setStatus(DesignationLoadingTube.Status status) {
        this.status = status;
    }

    public Long getDesignationId() {
        return designationId;
    }

    public void setDesignationId(Long designationId) {
        this.designationId = designationId;
    }

    public Long getTubeEventId() {
        return tubeEventId;
    }

    public void setTubeEventId(Long tubeEventId) {
        this.tubeEventId = tubeEventId;
    }

    public boolean isRepeatedBarcode() {
        return repeatedBarcode;
    }

    public void setRepeatedBarcode(boolean repeatedBarcode) {
        this.repeatedBarcode = repeatedBarcode;
    }
}
