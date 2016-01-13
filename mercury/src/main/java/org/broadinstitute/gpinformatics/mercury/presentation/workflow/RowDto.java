package org.broadinstitute.gpinformatics.mercury.presentation.workflow;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
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
    private Date eventDate;
    private int readLength = 1;
    private String product;
    private String startingBatchVessel;
    private String eventType;

    public RowDto(@Nonnull String barcode, @Nonnull String lcset, @Nonnull Date eventDate, @Nonnull String product,
                  @Nonnull String startingBatchVessel, @Nonnull String eventType, @Nonnull BigDecimal loadingConc) {
        this.barcode = barcode;
        this.lcset = lcset;
        this.eventDate = eventDate;
        this.product = product;
        this.startingBatchVessel = startingBatchVessel;
        this.eventType = eventType;
        this.loadingConc = loadingConc;
    }

    public RowDto(@Nonnull String barcode, @Nonnull String lcset, @Nonnull Date eventDate, @Nonnull String product,
                  @Nonnull String startingBatchVessel, @Nonnull String eventType, @Nonnull BigDecimal loadingConc,
                  int numberLanes) {
        this.barcode = barcode;
        this.lcset = lcset;
        this.eventDate = eventDate;
        this.product = product;
        this.startingBatchVessel = startingBatchVessel;
        this.eventType = eventType;
        this.loadingConc = loadingConc;
        this.numberLanes = numberLanes;
    }

    /** Returns the unique combinations of lcset and eventType which are just concatenated together. */
    public static Set<String> allLcsetsAndEventTypes(Collection<RowDto> rowDtos) {
        Set<String> set = new HashSet<>();
        for (RowDto rowDto : rowDtos) {
            set.add(rowDto.getLcset() + rowDto.getEventType());
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

    public Date getEventDate() {
        return eventDate;
    }

    public void setEventDate(Date eventDate) {
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

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
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
        if (!eventType.equals(rowDto.eventType)) {
            return false;
        }
        return product.equals(rowDto.product);

    }

    @Override
    public int hashCode() {
        int result = barcode.hashCode();
        result = 31 * result + lcset.hashCode();
        result = 31 * result + eventDate.hashCode();
        result = 31 * result + eventType.hashCode();
        result = 31 * result + product.hashCode();
        return result;
    }
}
