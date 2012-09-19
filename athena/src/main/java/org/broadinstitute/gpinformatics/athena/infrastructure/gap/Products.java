package org.broadinstitute.gpinformatics.athena.infrastructure.gap;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name="products")
public class Products {
    private List<Product> products = new ArrayList<Product>();

    @XmlElement(name="product")
    public List<Product> getProducts() {
        return products;
    }

    public void setProducts(List<Product> products) {
        this.products = products;
    }
}
    
