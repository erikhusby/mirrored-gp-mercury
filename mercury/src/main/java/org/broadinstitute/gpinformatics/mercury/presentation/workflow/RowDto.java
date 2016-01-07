package org.broadinstitute.gpinformatics.mercury.presentation.workflow;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Date;

/**
 * Represents a table row in Create FCT page.
 */
public class RowDto {
    private String barcode = "";
    private String lcset = "";
    private int numberLanes = 0;
    private BigDecimal loadingConc = new BigDecimal("7.0");
    private Date eventDate = new Date();
    private int readLength = 1;
    private String product = "";

    public RowDto() {
    }

    public RowDto(@Nonnull String barcode, @Nonnull String lcset, @Nonnull Date eventDate, @Nonnull String product) {
        this.barcode = barcode;
        this.lcset = lcset;
        this.eventDate = eventDate;
        this.product = product;
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
        return product.equals(rowDto.product);

    }

    @Override
    public int hashCode() {
        int result = barcode.hashCode();
        result = 31 * result + lcset.hashCode();
        result = 31 * result + eventDate.hashCode();
        result = 31 * result + product.hashCode();
        return result;
    }
}
