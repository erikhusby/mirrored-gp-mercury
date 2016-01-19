package org.broadinstitute.gpinformatics.mercury.presentation.workflow;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a table row in Create FCT page.
 */
public class RowDto {
    private String barcode;
    private String lcset;
    private int numberLanes = 0;
    private BigDecimal loadingConc;
    private String eventDate;
    private int readLength = 1;
    private String product;
    private String startingBatchVessel;
    private String tubeType;
    private String lcsetUrl;

    public RowDto() {
    }

    public RowDto(@Nonnull String barcode, @Nonnull String lcset, @Nonnull String eventDate, @Nonnull String product,
                  @Nonnull String startingBatchVessel, @Nonnull String tubeType, @Nonnull BigDecimal loadingConc,
                  String lcsetUrl) {
        this.barcode = barcode;
        this.lcset = lcset;
        this.eventDate = eventDate;
        this.product = product;
        this.startingBatchVessel = startingBatchVessel;
        this.tubeType = tubeType;
        this.loadingConc = loadingConc;
        this.lcsetUrl = lcsetUrl;
    }

    public RowDto(@Nonnull String barcode, @Nonnull String lcset, @Nonnull String eventDate, @Nonnull String product,
                  @Nonnull String startingBatchVessel, @Nonnull String tubeType, @Nonnull BigDecimal loadingConc,
                  int numberLanes) {
        this(barcode, lcset, eventDate, product, startingBatchVessel, tubeType, loadingConc, null);
        setNumberLanes(numberLanes);
    }

    /** Returns the unique combinations of lcset and tubeType which are just concatenated together. */
    public static Set<String> allLcsetsAndTubeTypes(Collection<RowDto> rowDtos) {
        Set<String> set = new HashSet<>();
        for (RowDto rowDto : rowDtos) {
            set.add(rowDto.getLcset() + rowDto.getTubeType());
        }
        return set;
    }

    /** Returns the starting batch vessel barcodes. */
    public static Set<String> allStartingBatchVessels(Collection<RowDto> rowDtos) {
        Set<String> set = new HashSet<>();
        for (RowDto rowDto : rowDtos) {
            set.add(rowDto.getStartingBatchVessel());
        }
        return set;
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

    public int getNumberLanes() {
        return numberLanes;
    }

    public void setNumberLanes(int numberLanes) {
        this.numberLanes = numberLanes;
    }

    public String getStartingBatchVessel() {
        return startingBatchVessel;
    }

    public void setStartingBatchVessel(String startingBatchVessel) {
        this.startingBatchVessel = startingBatchVessel;
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

    public static final Comparator BY_BARCODE = new Comparator() {
        @Override
        public int compare(Object o1, Object o2) {
            return ((RowDto)o1).getBarcode().compareTo(((RowDto)o2).getBarcode());
        }
    };

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RowDto)) {
            return false;
        }

        RowDto rowDto = (RowDto) o;

        if (!barcode.equals(rowDto.barcode)) {
            return false;
        }
        if (!lcset.equals(rowDto.lcset)) {
            return false;
        }
        if (!eventDate.equals(rowDto.eventDate)) {
            return false;
        }
        if (!tubeType.equals(rowDto.tubeType)) {
            return false;
        }
        return product.equals(rowDto.product);

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
