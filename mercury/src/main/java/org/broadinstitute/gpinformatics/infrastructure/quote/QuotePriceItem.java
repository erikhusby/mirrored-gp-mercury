package org.broadinstitute.gpinformatics.infrastructure.quote;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.infrastructure.LongDateAdapter;

import javax.annotation.Nonnull;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;

@XmlRootElement(name = "priceItem")
public class QuotePriceItem {

    private String id;
    private String name;
    private String price;
    private String shortName;
    private String unit;
    private Date submittedDate;
    private Date effectiveDate;
    private Date expirationDate;
    private String platformName;
    private String categoryName;
    private String priceItemStatus;
    private String priceItemGroupName;
    private String description;
    private String wbsAccount;
    private String priceListName;
    private String priceListLink;

    private ReplacementItems replacementItems;

    public QuotePriceItem() {}

    public static QuotePriceItem convertMercuryPriceItem(@Nonnull PriceItem priceItem) {
        QuotePriceItem quotePriceItem = new QuotePriceItem();
        quotePriceItem.setName(priceItem.getName());
        quotePriceItem.setCategoryName(priceItem.getCategory());
        quotePriceItem.setPlatformName(priceItem.getPlatform());
        quotePriceItem.setPrice(priceItem.getPrice());
        return quotePriceItem;
    }

    public QuotePriceItem(String quoteServerId, String platformName, String categoryName, String name) {
        this.id = quoteServerId;
        this.platformName = platformName;
        this.categoryName = categoryName;
        this.name = name;
    }

    /**
     * This constructor is only used by test code.
     *
     * @param categoryName The category
     * @param id The price item id
     * @param name The name of the price item
     * @param price The cost of this item
     * @param unit The units
     * @param platformName The platform for this item
     */
    public QuotePriceItem(String categoryName,
                          String id,
                          String name,
                          String price,
                          String unit,
                          String platformName) {
        this.categoryName = categoryName;
        this.id = id;
        this.name = name;
        this.price = price;
        this.unit = unit;
        this.platformName = platformName;
    }


    @XmlElement(name = "id")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @XmlElement(name = "name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @XmlElement(name = "price")
    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    @XmlElement(name = "shortName")
    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    @XmlElement(name = "unit")
    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    @XmlElement(name = "submittedDate")
    @XmlJavaTypeAdapter(LongDateAdapter.class)
    public Date getSubmittedDate() {
        return submittedDate;
    }

    public void setSubmittedDate(Date submittedDate) {
        this.submittedDate = submittedDate;
    }

    @XmlElement(name = "effectiveDate")
    @XmlJavaTypeAdapter(LongDateAdapter.class)
    public Date getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(Date effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    @XmlElement(name = "platformName")
    public String getPlatformName() {
        return platformName;
    }

    public void setPlatformName(String platformName) {
        this.platformName = platformName;
    }

    @XmlElement(name = "categoryName")
    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    @XmlElement(name = "priceItemStatus")
    public String getPriceItemStatus() {
        return priceItemStatus;
    }

    public void setPriceItemStatus(String priceItemStatus) {
        this.priceItemStatus = priceItemStatus;
    }

    @XmlElement(name = "priceItemGroupName")
    public String getPriceItemGroupName() {
        return priceItemGroupName;
    }

    public void setPriceItemGroupName(String priceItemGroupName) {
        this.priceItemGroupName = priceItemGroupName;
    }

    @XmlElement(name = "description")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @XmlElement(name = "wbsAccount")
    public String getWbsAccount() {
        return wbsAccount;
    }

    public void setWbsAccount(String wbsAccount) {
        this.wbsAccount = wbsAccount;
    }

    @XmlElement(name = "priceListName")
    public String getPriceListName() {
        return priceListName;
    }

    public void setPriceListName(String priceListName) {
        this.priceListName = priceListName;
    }

    @XmlElement(name = "priceListLink")
    public String getPriceListLink() {
        return priceListLink;
    }

    public void setPriceListLink(String priceListLink) {
        this.priceListLink = priceListLink;
    }


    @XmlElement(name = "replacementItems")
    public ReplacementItems getReplacementItems() {
        return replacementItems;
    }

    public void setReplacementItems(ReplacementItems replacementItems) {
        this.replacementItems = replacementItems;
    }

    @XmlElement(name = "expirationDate")
    @XmlJavaTypeAdapter(LongDateAdapter.class)
    public Date getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QuotePriceItem)) return false;

        QuotePriceItem quotePriceItem = (QuotePriceItem) o;

        if (categoryName != null ? !categoryName.equals(quotePriceItem.categoryName) : quotePriceItem.categoryName != null)
            return false;
        if (name != null ? !name.equals(quotePriceItem.name) : quotePriceItem.name != null) return false;
        if (platformName != null ? !platformName.equals(quotePriceItem.platformName) : quotePriceItem.platformName != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (platformName != null ? platformName.hashCode() : 0);
        result = 31 * result + (categoryName != null ? categoryName.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "QuotePriceItem{" +
                "platformName='" + platformName + '\'' +
                ", name='" + name + '\'' +
                ", categoryName='" + categoryName + '\'' +
                '}';
    }

    public boolean isMercuryPriceItemEqual(PriceItem priceItem) {
        return platformName.equals(priceItem.getPlatform()) &&
               categoryName.equals(priceItem.getCategory()) &&
               name.equals(priceItem.getName());
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

    public static Comparator<QuotePriceItem> BY_PLATFORM_THEN_CATEGORY_THEN_NAME = new Comparator<QuotePriceItem>() {
        @Override
        public int compare(QuotePriceItem o1, QuotePriceItem o2) {

            CompareToBuilder builder = new CompareToBuilder();
            builder.append(o1.getPlatformName(), o2.getPlatformName());
            builder.append(o1.getCategoryName(), o2.getCategoryName());
            builder.append(o1.getName(), o2.getName());

            return builder.build();
        }
    };

    public boolean sameAsQuoteItem(QuoteItem quoteItem) {
        return quoteItem.getCategoryName().equals(getCategoryName()) &&
               quoteItem.getName().equals(getName()) &&
               quoteItem.getPlatform().equals(getPlatformName());
    }
}
