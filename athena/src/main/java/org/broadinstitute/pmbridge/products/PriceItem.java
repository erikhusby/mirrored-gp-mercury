package org.broadinstitute.pmbridge.products;

import org.broadinstitute.pmbridge.Namespaces;

import javax.xml.bind.annotation.XmlType;

@XmlType(namespace = Namespaces.PRODUCT_NS)
public class PriceItem {

    private String platform;
    private String categoryName;
    private String id;
    private String name;
    private String price;
    private String units;

    public PriceItem() {
    }

    public PriceItem(String categoryName,
                     String id,
                     String name,
                     String price,
                     String units,
                     String platform) {
        this.categoryName = categoryName;
        this.id = id;
        this.name = name;
        this.price = price;
        this.units = units;
        this.platform = platform;
    }

    public String getPlatform() {
        return platform;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPrice() {
        return price;
    }

    public String getUnits() {
        return units;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PriceItem priceItem = (PriceItem) o;

        if (categoryName != null ? !categoryName.equals(priceItem.categoryName) : priceItem.categoryName != null)
            return false;
        if (id != null ? !id.equals(priceItem.id) : priceItem.id != null) return false;
        if (name != null ? !name.equals(priceItem.name) : priceItem.name != null) return false;
        if (platform != null ? !platform.equals(priceItem.platform) : priceItem.platform != null) return false;
        if (price != null ? !price.equals(priceItem.price) : priceItem.price != null) return false;
        if (units != null ? !units.equals(priceItem.units) : priceItem.units != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = platform != null ? platform.hashCode() : 0;
        result = 31 * result + (categoryName != null ? categoryName.hashCode() : 0);
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (price != null ? price.hashCode() : 0);
        result = 31 * result + (units != null ? units.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "PriceItem{" +
                "platform='" + platform + '\'' +
                ", categoryName='" + categoryName + '\'' +
                ", id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", price='" + price + '\'' +
                ", units='" + units + '\'' +
                '}';
    }
}
