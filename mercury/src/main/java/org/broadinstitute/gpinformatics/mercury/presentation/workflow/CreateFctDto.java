package org.broadinstitute.gpinformatics.mercury.presentation.workflow;

import org.broadinstitute.gpinformatics.mercury.presentation.run.FctDto;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

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
    private String product;
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
                        @Nonnull String eventDate,
                        @Nonnull String product, @Nonnull String startingBatchVessels, @Nonnull String tubeType,
                        @Nonnull BigDecimal loadingConc, String lcsetUrl, @Nonnull String regulatoryDesignation,
                        int numberSamples) {
        this.barcode = barcode;
        this.lcset = lcset;
        this.additionalLcsets = additionalLcsets;
        this.eventDate = eventDate;
        this.product = product;
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

    /** Returns the unique combinations of lcset and tubeType which are just concatenated together. */
    public static Set<String> allLcsetsAndTubeTypes(Collection<CreateFctDto> createFctDtos) {
        Set<String> set = new HashSet<>();
        for (CreateFctDto createFctDto : createFctDtos) {
            set.add(createFctDto.getLcset() + createFctDto.getTubeType());
        }
        return set;
    }

    /** Returns the starting batch vessel barcodes. */
    public static Set<String> allStartingBatchVessels(Collection<CreateFctDto> createFctDtos) {
        Set<String> set = new HashSet<>();
        for (CreateFctDto createFctDto : createFctDtos) {
            set.add(createFctDto.getStartingBatchVessels());
        }
        return set;
    }

    /**
     * Split is used for partial dto allocation. This is unsupported since it should never happen, since
     * this type of dto is only used in the CreateFCT page that can only do complete flowcell fills.
     */
    public CreateFctDto split(int allocatedLanes) {
        throw new RuntimeException("Expected to only do complete flowcell fills.");
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

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

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public BigDecimal getLoadingConc() {
        return loadingConc;
    }

    public void setLoadingConc(BigDecimal loadingConc) {
        this.loadingConc = loadingConc;
    }

    public Integer getNumberLanes() {
        return numberLanes;
    }

    public void setNumberLanes(int numberLanes) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CreateFctDto)) {
            return false;
        }

        CreateFctDto createFctDto = (CreateFctDto) o;

        if (!barcode.equals(createFctDto.barcode)) {
            return false;
        }
        if (!lcset.equals(createFctDto.lcset)) {
            return false;
        }
        if (!eventDate.equals(createFctDto.eventDate)) {
            return false;
        }
        if (!tubeType.equals(createFctDto.tubeType)) {
            return false;
        }
        return product.equals(createFctDto.product);

    }

    @Override
    public int hashCode() {
        int result = barcode.hashCode();
        result = 31 * result + lcset.hashCode();
        result = 31 * result + eventDate.hashCode();
        result = 31 * result + tubeType.hashCode();
        result = 31 * result + product.hashCode();
        return result;
    }
}
