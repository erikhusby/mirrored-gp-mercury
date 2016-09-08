package org.broadinstitute.gpinformatics.mercury.presentation.run;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.run.FlowcellDesignation;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

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
    private Integer numberCycles;
    private Integer numberLanes;
    private Integer readLength;
    private BigDecimal loadingConc;
    private Boolean poolTest;
    private String barcode;
    private String lcsetUrl;
    private String lcset;
    private List<String> productNames = new ArrayList<>();
    private String startingBatchVessels;
    private String tubeType;
    private String tubeDate;
    private String regulatoryDesignation;
    private int numberSamples;

    private Long designationId;
    private Long tubeEventId;
    private boolean allocated = false;
    private int allocationOrder = 0;

    private final static String DELIMITER = "<br/>";

    public DesignationDto() {
        createdOn = new Date();
        status = FlowcellDesignation.Status.UNSAVED;
    }

    public DesignationDto(LabVessel loadingLabVessel, Collection<LabEvent> labEvents,
                          String lcsetUrl, LabBatch lcset, Collection<String> productNames,
                          String startingBatchVessels, String regulatoryDesignation, int numberSamples,
                          IlluminaFlowcell.FlowcellType sequencerModel,
                          FlowcellDesignation.IndexType indexType,
                          Integer numberCycles, Integer numberLanes, Integer readLength,
                          BigDecimal loadingConc, Boolean poolTest, Date createdOn,
                          FlowcellDesignation.Status status) {

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
        this.lcset = lcset.getBatchName();
        this.lcsetUrl = lcsetUrl;
        this.productNames.addAll(productNames);
        this.startingBatchVessels = startingBatchVessels;

        this.sequencerModel = sequencerModel;
        this.indexType = indexType;
        this.numberCycles = numberCycles;
        this.numberLanes = numberLanes;
        this.readLength = readLength;
        this.loadingConc = loadingConc;
        this.poolTest = poolTest;
        this.priority = FlowcellDesignation.Priority.NORMAL;
        this.regulatoryDesignation = regulatoryDesignation;
        this.numberSamples = numberSamples;
        this.createdOn = createdOn;
        this.status = status;
    }

    /**
     * Splits dto into two so that the orignal gets the allocated lane count and
     * the new one gets the unallocated lane count.
     *
     * @param allocatedLanes the new number of lanes on This.
     * @return  a new dto like This but with null entity id and a lane count of the unallocated number of lanes.
     */
    public DesignationDto split(int allocatedLanes) {
        try {
            DesignationDto splitDto = (DesignationDto)this.clone();
            splitDto.setDesignationId(null);
            splitDto.setNumberLanes(this.getNumberLanes() - allocatedLanes);
            setNumberLanes(allocatedLanes);
            return splitDto;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }


    /** Defines how designations may be combined on a flowcell. */
    public String fctGrouping() {
        return "FctGrouping{" + getSequencerModel() + ", " + getNumberCycles() + " cycles, " +
               getReadLength() + " readLength, " + getIndexType() + " index, " + getRegulatoryDesignation() + "}";
    }

    public String getProductNameJoin() {
        return StringUtils.join(productNames, DELIMITER);
    }

    public void setProductNameJoin(String delimitedProductNames) {
        productNames = Arrays.asList(delimitedProductNames.split(DELIMITER));
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

    public String getLcset() {
        return lcset;
    }

    public void setLcset(String lcset) {
        this.lcset = lcset;
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

    public Long getTubeEventId() {
        return tubeEventId;
    }

    public void setTubeEventId(Long tubeEventId) {
        this.tubeEventId = tubeEventId;
    }

    public boolean isAllocated() {
        return allocated;
    }

    @Override
    public void setAllocated(boolean allocated) {
        this.allocated = allocated;
    }

    @Override
    public int getAllocationOrder() {
        return allocationOrder;
    }

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
        if (getNumberCycles() != null ? !getNumberCycles().equals(that.getNumberCycles()) :
                that.getNumberCycles() != null) {
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
        return getTubeEventId() != null ? getTubeEventId().equals(that.getTubeEventId()) :
                that.getTubeEventId() == null;
    }

    @Override
    public int hashCode() {
        int result = getStatus() != null ? getStatus().hashCode() : 0;
        result = 31 * result + (getSequencerModel() != null ? getSequencerModel().hashCode() : 0);
        result = 31 * result + (getIndexType() != null ? getIndexType().hashCode() : 0);
        result = 31 * result + (getPriority() != null ? getPriority().hashCode() : 0);
        result = 31 * result + (getNumberCycles() != null ? getNumberCycles().hashCode() : 0);
        result = 31 * result + (getNumberLanes() != null ? getNumberLanes().hashCode() : 0);
        result = 31 * result + (getReadLength() != null ? getReadLength().hashCode() : 0);
        result = 31 * result + (getLoadingConc() != null ? getLoadingConc().hashCode() : 0);
        result = 31 * result + (getPoolTest() != null ? getPoolTest().hashCode() : 0);
        result = 31 * result + (getBarcode() != null ? getBarcode().hashCode() : 0);
        result = 31 * result + (getLcset() != null ? getLcset().hashCode() : 0);
        result = 31 * result + (getDesignationId() != null ? getDesignationId().hashCode() : 0);
        result = 31 * result + (getTubeEventId() != null ? getTubeEventId().hashCode() : 0);
        return result;
    }

}
