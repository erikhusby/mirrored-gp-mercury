package org.broadinstitute.gpinformatics.athena.entity.products;

import org.hibernate.envers.Audited;

import javax.persistence.*;

@Entity
@Audited
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"quoteServerId"}))
public class PriceItem {

    @Id
    @SequenceGenerator(name = "SEQ_PRICE_ITEM", sequenceName = "SEQ_PRICE_ITEM")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PRICE_ITEM")
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY)
    private Product product;


    private Long quoteServerId;

    // The @Transient fields below are "owned" by the quote server, what we hold in this class are just cached copies

    @Transient
    private String platform;

    @Transient
    private String categoryName;

    @Transient
    private String name;

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

    public Long getQuoteServerId() {
        return quoteServerId;
    }

    public void setQuoteServerId(Long quoteServerId) {
        this.quoteServerId = quoteServerId;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PriceItem)) return false;

        PriceItem priceItem = (PriceItem) o;

        if (!quoteServerId.equals(priceItem.quoteServerId)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return quoteServerId.hashCode();
    }
}
