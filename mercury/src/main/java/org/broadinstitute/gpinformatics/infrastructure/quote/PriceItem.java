package org.broadinstitute.gpinformatics.infrastructure.quote;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Date;

@XmlRootElement(name = "priceItem")
public class PriceItem {

    public static final String GSP_PLATFORM_NAME = QuotePlatformType.SEQ.getPlatformName();

    public static final String SAMPLE_UNITS = "Sample";

    private String id;
    private String name;
    private String price;
    private String shortName;
    private String unit;
    private Date submittedDate;
    private Date effectiveDate;
    private String platformName;
    private String categoryName;
    private String priceItemStatus;
    private String priceItemGroupName;
    private String description;
    private String wbsAccount;
    private String priceListName;
    private String priceListLink;

    private ReplacementItems replacementItems;

    public PriceItem() {}

    public static PriceItem convertMercuryPriceItem(
            org.broadinstitute.gpinformatics.athena.entity.products.PriceItem priceItem) {

        PriceItem quotePriceItem = new PriceItem();
        quotePriceItem.setName(priceItem.getName());
        quotePriceItem.setCategoryName(priceItem.getCategory());
        quotePriceItem.setPlatformName(priceItem.getPlatform());
        return quotePriceItem;
    }
    public PriceItem(String quoteServerId, String platformName, String categoryName, String name) {
        this.id = quoteServerId;
        this.platformName = platformName;
        this.categoryName = categoryName;
        this.name = name;
    }

    /**
     * This constructor is only used by test code.
     *
     * @param categoryName
     * @param id
     * @param name
     * @param price
     * @param unit
     * @param platformName
     */
    public PriceItem(String categoryName,
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
    @XmlJavaTypeAdapter(PriceItemDateAdapter.class)
    public Date getSubmittedDate() {
        return submittedDate;
    }

    public void setSubmittedDate(Date submittedDate) {
        this.submittedDate = submittedDate;
    }

    @XmlElement(name = "effectiveDate")
    @XmlJavaTypeAdapter(PriceItemDateAdapter.class)
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PriceItem)) return false;

        PriceItem priceItem = (PriceItem) o;

        if (categoryName != null ? !categoryName.equals(priceItem.categoryName) : priceItem.categoryName != null)
            return false;
        if (name != null ? !name.equals(priceItem.name) : priceItem.name != null) return false;
        if (platformName != null ? !platformName.equals(priceItem.platformName) : priceItem.platformName != null)
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
        return "PriceItem{" +
                "platformName='" + platformName + '\'' +
                ", name='" + name + '\'' +
                ", categoryName='" + categoryName + '\'' +
                '}';
    }
}
