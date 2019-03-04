package org.broadinstitute.gpinformatics.mercury.presentation.run;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.run.FlowcellDesignation;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Represents the UI data table row.
 */
public class DesignationDto implements Cloneable, FctDto {
    private boolean selected = false;
    private FlowcellDesignation.Status status;
    private Date createdOn;
    private IlluminaFlowcell.FlowcellType sequencerModel;
    private FlowcellDesignation.IndexType indexType;
    private FlowcellDesignation.Priority priority;
    private Integer numberLanes;
    private int numberLanesAllocated;
    private Integer readLength;
    private BigDecimal loadingConc;
    private Boolean poolTest;
    private Boolean pairedEndRead;
    private String barcode;
    private String lcsetUrl;
    private String lcset;
    private String chosenLcset = null;
    private List<String> productNames = new ArrayList<>();
    private String startingBatchVessels;
    private String tubeType;
    private String tubeDate;
    private String regulatoryDesignation;
    private boolean groupByRegulatoryDesignation;
    private int numberSamples;

    private Long designationId;
    private int priorityValue = 0;

    public final static String DELIMITER = "<br/>";

    public DesignationDto() {
        createdOn = new Date();
        status = FlowcellDesignation.Status.UNSAVED;
        priority = FlowcellDesignation.Priority.NORMAL;
        groupByRegulatoryDesignation = true;
        numberLanesAllocated = 0;
    }

    public DesignationDto(FlowcellDesignation flowcellDesignation) {
        this();
        if (flowcellDesignation != null) {
            setChosenLcset(flowcellDesignation.getChosenLcset() != null ?
                    flowcellDesignation.getChosenLcset().getBatchName() : null);
            setNumberLanes(flowcellDesignation.getNumberLanes());
            setPoolTest(flowcellDesignation.isPoolTest());
            setIndexType(flowcellDesignation.getIndexType());
            setStatus(flowcellDesignation.getStatus());
            setSequencerModel(flowcellDesignation.getSequencerModel());
            setCreatedOn(flowcellDesignation.getCreatedOn());
            setReadLength(flowcellDesignation.getReadLength());
            setPairedEndRead(flowcellDesignation.isPairedEndRead());
            setLoadingConc(flowcellDesignation.getLoadingConc());
            setDesignationId(flowcellDesignation.getDesignationId());
            setPriority(flowcellDesignation.getPriority());
            setBarcode(flowcellDesignation.getLoadingTube().getLabel());
        }
    }

    /**
     * Updates the tube type and tube date from the first transfer-to lab event for the tube.
     */
    public void setTypeAndDate(LabVessel loadingTube) {
        // Collects transfer events targeting the tube, oldest first.
        List<LabEvent> events = new ArrayList<>(loadingTube.getTransfersTo());
        if (!events.isEmpty()) {
            Collections.sort(events, LabEvent.BY_EVENT_DATE);
            LabEvent preferredEvent = events.get(0);
            tubeType = preferredEvent.getLabEventType().getName().replaceAll("Transfer", "");
            tubeDate = DateUtils.convertDateTimeToString(preferredEvent.getEventDate());
        }
    }

    /**
     * Splits dto into two so that the original gets the allocated lane count and
     * the new one gets the unallocated lane count.
     *
     * @return  a new dto like This but with null entity id and a lane count of the unallocated number of lanes.
     */
    @Override
    public DesignationDto split() {
        try {
            DesignationDto splitDto = (DesignationDto)this.clone();
            splitDto.setDesignationId(null);
            splitDto.setAllocatedLanes(0);
            splitDto.setNumberLanes(numberLanes - numberLanesAllocated);
            setNumberLanes(numberLanesAllocated);
            return splitDto;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    /** Calculates the number of cycles from read length, paired end read, and index type. */
    public int calculateCycles() {
        int numberCycles = readLength != null ? readLength : 0;
        if (pairedEndRead != null && pairedEndRead) {
            numberCycles *= 2;
        }
        if (indexType != null) {
            numberCycles += indexType.getIndexSize();
        }
        return numberCycles;
    }

    @Override
    public String getProduct() {
        return StringUtils.join(productNames, DELIMITER);
    }

    public void setProduct(String delimitedProductNames) {
        productNames = Arrays.asList(delimitedProductNames.split(DELIMITER));
    }

    @Override
    public List<String> getProductNames() {
        return productNames;
    }

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

    public void setSequencerModel(IlluminaFlowcell.FlowcellType sequencerModel) {
        this.sequencerModel = sequencerModel;
    }

    public FlowcellDesignation.IndexType getIndexType() {
        return indexType;
    }

    public void setIndexType(FlowcellDesignation.IndexType indexType) {
        this.indexType = indexType;
    }

    public FlowcellDesignation.Priority getPriority() {
        return priority;
    }

    public void setPriority(FlowcellDesignation.Priority priority) {
        this.priority = priority;
        priorityValue = (priority == FlowcellDesignation.Priority.HIGH ? 1 :
                priority == FlowcellDesignation.Priority.LOW ? -1 : 0);
    }

    @Override
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

    @Override
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

    @Override
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

    @Override
    public String getLcset() {
        return lcset;
    }

    public void setLcset(String lcset) {
        this.lcset = lcset;
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

    public FlowcellDesignation.Status getStatus() {
        return status;
    }

    public void setStatus(FlowcellDesignation.Status status) {
        this.status = status;
    }

    public Long getDesignationId() {
        return designationId;
    }

    public void setDesignationId(Long designationId) {
        this.designationId = designationId;
    }

    public String getChosenLcset() {
        return chosenLcset;
    }

    public void setChosenLcset(String chosenLcset) {
        this.chosenLcset = chosenLcset;
    }

    public Boolean getPairedEndRead() {
        return pairedEndRead;
    }

    public void setPairedEndRead(Boolean pairedEndRead) {
        this.pairedEndRead = pairedEndRead;
    }

    public int getPriorityValue() {
        return priorityValue;
    }

    public boolean getGroupByRegulatoryDesignation() {
        return groupByRegulatoryDesignation;
    }

    public void setGroupByRegulatoryDesignation(boolean groupByRegulatoryDesignation) {
        this.groupByRegulatoryDesignation = groupByRegulatoryDesignation;
    }

    @Override
    public int getAllocatedLanes() {
        return numberLanesAllocated;
    }

    @Override
    public void setAllocatedLanes(int allocatedLanes) {
        numberLanesAllocated = allocatedLanes;
    }

    /**
     * Returns true if designation may be combined with others on a flowcell.
     * @param groupDtos the dtos to test against.
     */
    @Override
    public <DTO_TYPE extends FctDto> boolean isCompatible(Collection<DTO_TYPE> groupDtos) {
        for (FctDto dto : groupDtos) {
            DesignationDto groupDto = (DesignationDto)dto;
            if (!getSequencerModel().equals(groupDto.getSequencerModel()) ||
                    calculateCycles() != groupDto.calculateCycles() ||
                    !getReadLength().equals(groupDto.getReadLength()) ||
                    !getIndexType().equals(groupDto.getIndexType()) ||
                    (getGroupByRegulatoryDesignation() &&
                            groupDto.getGroupByRegulatoryDesignation() &&
                            !getRegulatoryDesignation().equals(groupDto.getRegulatoryDesignation()))) {
                return false;
            }
        }
        return true;
    }

    /** Orders by highest priority, then by highest number of lanes, then by barcode. */
    public static final Comparator<FctDto> BY_ALLOCATION_ORDER = (f1, f2) -> {
        if (f2.getPriorityValue() != f1.getPriorityValue()) {
            return f2.getPriorityValue() - f1.getPriorityValue();
        }
        if (f2.getNumberLanes() != null && f1.getNumberLanes() != null &&
                !f2.getNumberLanes().equals(f1.getNumberLanes())) {
            return f2.getNumberLanes().compareTo(f1.getNumberLanes());
        }
        return f1.getBarcode() != null ? f1.getBarcode().compareTo(f2.getBarcode()) : 0;
    };

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DesignationDto)) {
            return false;
        }

        DesignationDto that = (DesignationDto) o;

        if (getStatus() != that.getStatus()) {
            return false;
        }
        if (getSequencerModel() != that.getSequencerModel()) {
            return false;
        }
        if (getIndexType() != that.getIndexType()) {
            return false;
        }
        if (getPriority() != that.getPriority()) {
            return false;
        }
        if (getPairedEndRead() != null ? !getPairedEndRead().equals(that.getPairedEndRead()) :
                that.getPairedEndRead() != null) {
            return false;
        }
        if (getNumberLanes() != null ? !getNumberLanes().equals(that.getNumberLanes()) :
                that.getNumberLanes() != null) {
            return false;
        }
        if (getReadLength() != null ? !getReadLength().equals(that.getReadLength()) : that.getReadLength() != null) {
            return false;
        }
        if (getLoadingConc() != null ? !getLoadingConc().equals(that.getLoadingConc()) :
                that.getLoadingConc() != null) {
            return false;
        }
        if (getPoolTest() != null ? !getPoolTest().equals(that.getPoolTest()) : that.getPoolTest() != null) {
            return false;
        }
        if (getBarcode() != null ? !getBarcode().equals(that.getBarcode()) : that.getBarcode() != null) {
            return false;
        }
        if (getLcset() != null ? !getLcset().equals(that.getLcset()) : that.getLcset() != null) {
            return false;
        }
        if (getDesignationId() != null ? !getDesignationId().equals(that.getDesignationId()) :
                that.getDesignationId() != null) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = getStatus() != null ? getStatus().hashCode() : 0;
        result = 31 * result + (getSequencerModel() != null ? getSequencerModel().hashCode() : 0);
        result = 31 * result + (getIndexType() != null ? getIndexType().hashCode() : 0);
        result = 31 * result + (getPriority() != null ? getPriority().hashCode() : 0);
        result = 31 * result + (getPairedEndRead() != null ? getPairedEndRead().hashCode() : 0);
        result = 31 * result + (getNumberLanes() != null ? getNumberLanes().hashCode() : 0);
        result = 31 * result + (getReadLength() != null ? getReadLength().hashCode() : 0);
        result = 31 * result + (getLoadingConc() != null ? getLoadingConc().hashCode() : 0);
        result = 31 * result + (getPoolTest() != null ? getPoolTest().hashCode() : 0);
        result = 31 * result + (getBarcode() != null ? getBarcode().hashCode() : 0);
        result = 31 * result + (getLcset() != null ? getLcset().hashCode() : 0);
        result = 31 * result + (getDesignationId() != null ? getDesignationId().hashCode() : 0);
        return result;
    }

}
