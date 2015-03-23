package org.broadinstitute.gpinformatics.infrastructure.quote;

import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;

import javax.annotation.Nonnull;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.text.SimpleDateFormat;
import java.util.Date;

@XmlRootElement(name = "Item")
public class QuoteItem {

    private String quoteId;
    private String priceItemId;
    private String name;
    private String quantity;
    private String price;
    private String unit;
    private ReplacementItems replacementItems;

    public QuoteItem() {}

    public QuoteItem(String quoteId, String priceItemId, String name, String quantity, String price, String unit,
                     String platformName, String categoryName) {
        this.quoteId = quoteId;
        this.priceItemId = priceItemId;
        this.name = name;
        this.quantity = quantity;
        this.price = price;
        this.unit = unit;
    }

    @XmlElement(name = "quoteId")
    public String getQuoteId() {
        return quoteId;
    }

    public void setQuoteId(String quoteId) {
        this.quoteId = quoteId;
    }

    @XmlElement(name = "priceItemId")
    public String getPriceItemId() {
        return priceItemId;
    }

    public void setPriceItemId(String priceItemId) {
        this.priceItemId = priceItemId;
    }

    @XmlElement(name = "name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @XmlElement(name = "quantity")
    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    @XmlElement(name = "price")
    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    @XmlElement(name = "unit")
    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    @XmlElement(name = "replacementItems")
    public ReplacementItems getReplacementItems() {
        return replacementItems;
    }

    public void setReplacementItems(ReplacementItems replacementItems) {
        this.replacementItems = replacementItems;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QuoteItem)) return false;

        QuoteItem quoteItem = (QuoteItem) o;

        return !(quoteId != null ? !quoteId.equals(quoteItem.quoteId) : quoteItem.quoteId != null) &&
               !(name != null ? !name.equals(quoteItem.name) : quoteItem.name != null);

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (quoteId != null ? quoteId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "QuotePriceItem{" + "quoteId='" + quoteId + '\'' +
                ", name='" + name + '\'' + '\'' + '}';
    }

    /**
     * It would be nice if quote server dates were formatted consistently so we didn't need multiple date conversion
     * classes.
     */
    public static class DateAdapter extends XmlAdapter<String, Date> {

        private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");

        @Override
        public String marshal(Date v) throws Exception {
            return dateFormat.format(v);
        }

        @Override
        public Date unmarshal(String v) throws Exception {
            return dateFormat.parse(v);
        }

    }
}
