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
    private boolean allocated = false;
    private int allocationOrder = 0;

    public final static String DELIMITER = "<br/>";

    public DesignationDto() {
        createdOn = new Date();
        status = FlowcellDesignation.Status.UNSAVED;
        priority = FlowcellDesignation.Priority.NORMAL;
        indexType = FlowcellDesignation.IndexType.DUAL;
        groupByRegulatoryDesignation = true;
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
     * Splits dto into two so that the orignal gets the allocated lane count and
     * the new one gets the unallocated lane count.
     *
     * @param allocatedLanes the new number of lanes on This.
     * @return  a new dto like This but with null entity id and a lane count of the unallocated number of lanes.
     */
    @Override
    public DesignationDto split(int allocatedLanes) {
        try {
            DesignationDto splitDto = (DesignationDto)this.clone();
            splitDto.setDesignationId(null);
            splitDto.setAllocated(false);
            splitDto.setNumberLanes(this.getNumberLanes() - allocatedLanes);
            setNumberLanes(allocatedLanes);
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
        allocationOrder = (priority == FlowcellDesignation.Priority.HIGH ? 1 :
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

    @Override
    public boolean isAllocated() {
        return allocated;
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

    @Override
    public void setAllocated(boolean allocated) {
        this.allocated = allocated;
    }

    @Override
    public int getAllocationOrder() {
        return allocationOrder;
    }

    public boolean getGroupByRegulatoryDesignation() {
        return groupByRegulatoryDesignation;
    }

    public void setGroupByRegulatoryDesignation(boolean groupByRegulatoryDesignation) {
        this.groupByRegulatoryDesignation = groupByRegulatoryDesignation;
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

    public static final Comparator<FctDto> BY_ALLOCATION_ORDER = new Comparator<FctDto>() {
        @Override
        public int compare(FctDto f1, FctDto f2) {
            if (f1 instanceof DesignationDto && f2 instanceof DesignationDto) {
                DesignationDto o1 = (DesignationDto) f1;
                DesignationDto o2 = (DesignationDto) f2;
                // Puts highest allocationOrder first. If it's a tie, puts highest numberLanes first.
                if (o2.getAllocationOrder() != o1.getAllocationOrder()) {
                    return o2.getAllocationOrder() - o1.getAllocationOrder();
                } else {
                    return (o2.getNumberLanes() != null & o1.getNumberLanes() != null) ?
                            o2.getNumberLanes().compareTo(o1.getNumberLanes()) : 0;
                }
            }
            return 0;
        }
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
