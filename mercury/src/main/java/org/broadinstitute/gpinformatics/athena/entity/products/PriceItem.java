package org.broadinstitute.gpinformatics.athena.entity.products;

import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.io.Serializable;


/**
 * Core entity for PriceItems.
 *
 * @author mcovarr
 */
@Entity
@Audited
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"platform", "categoryName", "name"})
})
public class PriceItem implements Serializable {

    @Id
    @SequenceGenerator(name = "SEQ_PRICE_ITEM", sequenceName = "SEQ_PRICE_ITEM")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PRICE_ITEM")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Product product;

    private String platform;

    private String categoryName;

    private String name;

    private String quoteServicePriceItemId;


    // The @Transient fields below are "owned" by the quote server, what we hold in this class are just cached copies
    @Transient
    private String price;

    @Transient
    private String units;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getUnits() {
        return units;
    }

    public void setUnits(String units) {
        this.units = units;
    }

    public String getQuoteServicePriceItemId() {
        return quoteServicePriceItemId;
    }

    public void setQuoteServicePriceItemId(String quoteServicePriceItemId) {
        this.quoteServicePriceItemId = quoteServicePriceItemId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PriceItem)) return false;

        PriceItem priceItem = (PriceItem) o;

        if (!categoryName.equals(priceItem.categoryName)) return false;
        if (!name.equals(priceItem.name)) return false;
        if (!platform.equals(priceItem.platform)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = platform.hashCode();
        result = 31 * result + categoryName.hashCode();
        result = 31 * result + name.hashCode();
        return result;
    }

}
