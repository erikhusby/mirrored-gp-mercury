package org.broadinstitute.gpinformatics.athena.entity.products;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.envers.Audited;

import javax.annotation.Nonnull;
import javax.persistence.*;
import java.io.Serializable;


/**
 * Core entity for PriceItems.
 *
 * @author mcovarr
 */
@Entity
@Audited
@Table(schema = "athena",
        uniqueConstraints = {
        @UniqueConstraint(columnNames = {"platform", "category", "name"})
})
public class PriceItem implements Serializable {

    // constants currently used by real price items in the quote server
    public static final String PLATFORM_GENOMICS = "Genomics Platform";
    public static final String CATEGORY_EXOME_SEQUENCING_ANALYSIS = "Exome Sequencing Analysis";
    public static final String NAME_EXOME_EXPRESS = "Exome Express";

    @Id
    @SequenceGenerator(name = "SEQ_PRICE_ITEM", schema = "athena", sequenceName = "SEQ_PRICE_ITEM")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PRICE_ITEM")
    private Long priceItemId;

    @Column(nullable = false)
    private String platform;

    // there are null categories
    private String category;

    @Column(nullable = false)
    private String name;

    // we are currently recording this and it certainly exists on the quote server, but it's possible we might
    // stop recording it at some point since our having a copy of it doesn't seem all that useful at the moment
    private String quoteServerId;


    // The @Transient fields below are "owned" by the quote server, what we hold in this class are just cached copies
    @Transient
    private String price;

    @Transient
    private String units;


    /**
     * Package visible constructor for JPA
     */
    PriceItem() {}

    public PriceItem(@Nonnull String quoteServerId, @Nonnull String platform, String category, @Nonnull String name) {
        this.quoteServerId = quoteServerId;
        this.platform = platform;
        this.category = category;
        this.name = name;
    }


    public Long getPriceItemId() {
        return priceItemId;
    }

    public String getPlatform() {
        return platform;
    }

    public String getCategory() {
        return category;
    }

    public String getName() {
        return name;
    }

    public String getPrice() {
        return price;
    }

    /**
     * Quote server holds price data, we would set this into the entity as a transient property
     *
     * @param price
     */
    public void setPrice(String price) {
        this.price = price;
    }

    public String getUnits() {
        return units;
    }


    /**
     * Quote server holds units data, we would set this into the entity as a transient property
     *
     * @param units
     */
    public void setUnits(String units) {
        this.units = units;
    }

    public String getQuoteServerId() {
        return quoteServerId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PriceItem)) {
            return false;
        }

        PriceItem priceItem = (PriceItem) o;

        return new EqualsBuilder().append(category, priceItem.category)
                .append(name, priceItem.name)
                .append(platform, priceItem.platform).build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(platform).append(category).append(name).hashCode();
    }

}
