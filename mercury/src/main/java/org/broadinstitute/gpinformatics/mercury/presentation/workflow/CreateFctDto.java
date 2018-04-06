package org.broadinstitute.gpinformatics.mercury.presentation.workflow;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.mercury.presentation.run.DesignationDto;
import org.broadinstitute.gpinformatics.mercury.presentation.run.FctDto;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Represents a table row in Create FCT page.
 */
public class CreateFctDto implements FctDto, Cloneable {
    private String barcode;
    private String lcset;
    private String additionalLcsets;
    private int numberLanes = 0;
    private BigDecimal loadingConc;
    private String eventDate;
    private int readLength = 1;
    private List<String> productNames = new ArrayList<>();
    private String startingBatchVessels;
    private String tubeType;
    private String lcsetUrl;
    private String regulatoryDesignation;
    private int numberSamples;
    private boolean allocated = false;
    private int allocationOrder = 0;

    public CreateFctDto() {
    }

    public CreateFctDto(@Nonnull String barcode, @Nonnull String lcset, String additionalLcsets,
                        @Nonnull String eventDate, @Nonnull List<String> productNames,
                        @Nonnull String startingBatchVessels, @Nonnull String tubeType,
                        @Nonnull BigDecimal loadingConc, String lcsetUrl, @Nonnull String regulatoryDesignation,
                        int numberSamples) {
        this.barcode = barcode;
        this.lcset = lcset;
        this.additionalLcsets = additionalLcsets;
        this.eventDate = eventDate;
        this.productNames.addAll(productNames);
        this.startingBatchVessels = startingBatchVessels;
        this.tubeType = tubeType;
        this.loadingConc = loadingConc;
        this.lcsetUrl = lcsetUrl;
        this.regulatoryDesignation = regulatoryDesignation;
        this.numberSamples = numberSamples;
    }


    public CreateFctDto(String barcode, String lcset, BigDecimal loadingConc, int numberLanes) {
        this.barcode = barcode;
        this.lcset = lcset;
        this.loadingConc = loadingConc;
        this.numberLanes = numberLanes;
    }

    /**
     * Split is used for partial dto allocation. This is unsupported since it should never happen, since
     * this type of dto is only used in the CreateFCT page that can only do complete flowcell fills.
     */
    @Override
    public CreateFctDto split(int allocatedLanes) {
        throw new RuntimeException("Expected to only do complete flowcell fills.");
    }

    @Override
    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    @Override
    public String getLcset() {
        return lcset;
    }

    public void setLcset(String lcset) {
        this.lcset = lcset;
    }

    public void setAdditionalLcsets(String additionalLcsets) {
        this.additionalLcsets = additionalLcsets;
    }

    public String getAdditionalLcsets() {
        return additionalLcsets;
    }

    public String getEventDate() {
        return eventDate;
    }

    public void setEventDate(String eventDate) {
        this.eventDate = eventDate;
    }

    public int getReadLength() {
        return readLength;
    }

    public void setReadLength(int readLength) {
        this.readLength = readLength;
    }

    @Override
    public String getProduct() {
        return StringUtils.join(productNames, DesignationDto.DELIMITER);
    }

    @Override
    public List<String> getProductNames() {
        return productNames;
    }

    public void setProduct(String delimitedProductNames) {
        productNames = Arrays.asList(StringUtils.trimToEmpty(delimitedProductNames).split(DesignationDto.DELIMITER));
    }

    @Override
    public BigDecimal getLoadingConc() {
        return loadingConc;
    }

    public void setLoadingConc(BigDecimal loadingConc) {
        this.loadingConc = loadingConc;
    }

    @Override
    public Integer getNumberLanes() {
        return numberLanes;
    }

    public void setNumberLanes(Integer numberLanes) {
        this.numberLanes = numberLanes;
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

    public String getLcsetUrl() {
        return lcsetUrl;
    }

    public void setLcsetUrl(String lcsetUrl) {
        this.lcsetUrl = lcsetUrl;
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

    /** Indicates if dto was allocated to a flowcell. */
    @Override
    public boolean isAllocated() {
        return allocated;
    }

    @Override
    public void setAllocated(boolean allocated) {
        this.allocated = allocated;
    }

    /** Indicates the order in which multiple dtos are allocated to a flowcell. */
    @Override
    public int getAllocationOrder() {
        return allocationOrder;
    }

    public static final Comparator BY_BARCODE = new Comparator() {
        @Override
        public int compare(Object o1, Object o2) {
            return ((CreateFctDto)o1).getBarcode().compareTo(((CreateFctDto)o2).getBarcode());
        }
    };

    /**
     * Returns true if designation may be combined with others on a flowcell.
     * @param groupDtos the dtos to test against.
     */
    @Override
    public <DTO_TYPE extends FctDto> boolean isCompatible(Collection<DTO_TYPE> groupDtos) {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CreateFctDto)) {
            return false;
        }

        CreateFctDto that = (CreateFctDto) o;

        if (!getBarcode().equals(that.getBarcode())) {
            return false;
        }
        if (getLcset() != null ? !getLcset().equals(that.getLcset()) : that.getLcset() != null) {
            return false;
        }
        if (getEventDate() != null ? !getEventDate().equals(that.getEventDate()) : that.getEventDate() != null) {
            return false;
        }
        return getTubeType() != null ? getTubeType().equals(that.getTubeType()) : that.getTubeType() == null;

    }

    @Override
    public int hashCode() {
        int result = getBarcode().hashCode();
        result = 31 * result + (getLcset() != null ? getLcset().hashCode() : 0);
        result = 31 * result + (getEventDate() != null ? getEventDate().hashCode() : 0);
        result = 31 * result + (getTubeType() != null ? getTubeType().hashCode() : 0);
        return result;
    }
}
