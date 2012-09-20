package org.broadinstitute.gpinformatics.infrastructure.gap;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="product")
public class Product {
    private String name;

    public Product() {}

    public Product(String name) {
        this.name = name;
    }

    @XmlAttribute(name="name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
