package org.broadinstitute.sequel.control.quote;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "PriceItem")
public class PriceItem {

    public static final String GSP_PLATFORM_NAME = "DNA Sequencing";

    public static final String SAMPLE_UNITS = "Sample";
    
    @XmlAttribute(name = "platform")
    private String platform;

    @XmlAttribute(name = "categoryName")
    private String categoryName;

    @XmlAttribute(name = "id")
    private String id;

    @XmlAttribute(name = "name")
    private String name;

    @XmlAttribute(name = "price")
    private String price;

    @XmlAttribute(name = "units")
    private String units;

    public PriceItem() {}
    
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
}
