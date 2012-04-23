package org.broadinstitute.pmbridge.infrastructure.gap;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name="product")
public class Product {
    private String name;
    private String displayName;
    private String id;

    public Product() {}

    public Product(String name, String id) {
        this.name = name;
        this.id = id;
    }

    @XmlAttribute(name="id", required=true)
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @XmlAttribute(name="display-name")
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @XmlAttribute(name="name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
