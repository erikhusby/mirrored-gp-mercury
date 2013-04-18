package org.broadinstitute.gpinformatics.athena.entity.products;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.envers.Audited;

import javax.annotation.Nonnull;
import javax.persistence.*;
import java.io.Serializable;
import java.util.Collection;


/**
 * Core entity for PriceItems.
 *
 * @author mcovarr
 */
@Entity
@Audited
@Table(schema = "athena",
        name = "PRICE_ITEM",
        uniqueConstraints = {
        @UniqueConstraint(columnNames = {"platform", "category", "name"})
})
public class PriceItem implements Serializable, Comparable<PriceItem> {

    // Constants currently used by real price items in the quote server.
    public static final String PLATFORM_GENOMICS = "Genomics Platform";
    public static final String CATEGORY_EXOME_SEQUENCING_ANALYSIS = "Exome Sequencing Analysis";
    public static final String NAME_EXOME_EXPRESS = "Exome Express";
    public static final String NAME_STANDARD_WHOLE_EXOME = "Standard Whole Exome";

    @Id
    @SequenceGenerator(name = "SEQ_PRICE_ITEM", schema = "athena", sequenceName = "SEQ_PRICE_ITEM")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PRICE_ITEM")
    private Long priceItemId;

    @Column(nullable = false)
    private String platform;

    // There are null categories.
    private String category;

    @Column(nullable = false)
    private String name;

    // We are currently recording this and it certainly exists on the quote server, but it's possible we might
    // stop recording it at some point since our having a copy of it doesn't seem all that useful at the moment.
    private String quoteServerId;

    // The @Transient fields below are "owned" by the quote server, what we hold in this class are just cached copies.
    @Transient
    private String price;

    @Transient
    private String units;

    /**
     * Package visible constructor for JPA.
     */
    public PriceItem() {}

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
     * Quote server holds price data, we would set this into the entity as a transient property.
     */
    public void setPrice(String price) {
        this.price = price;
    }

    public String getUnits() {
        return units;
    }

    /**
     * Quote server holds units data, we would set this into the entity as a transient property.
     */
    public void setUnits(String units) {
        this.units = units;
    }

    public String getQuoteServerId() {
        return quoteServerId;
    }

    @Override
    public int compareTo(PriceItem that) {
        CompareToBuilder builder = new CompareToBuilder();
        builder.append(platform, that.getPlatform());
        builder.append(category, that.getCategory());
        builder.append(name, that.getName());

        return builder.build();
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

        return new EqualsBuilder().append(category, priceItem.getCategory())
                .append(name, priceItem.getName())
                .append(platform, priceItem.getPlatform()).build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(category).append(name).append(platform).hashCode();
    }

    public String getDisplayName() {
        if (category != null) {
            return category + " : " + name;
        }
        return name;
    }

    public static String[] getPriceItemKeys(Collection<PriceItem> priceItems) {
        String[] keys = new String[priceItems.size()];
        int i = 0;
        for (PriceItem priceItem : priceItems) {
            keys[i++] = makeConcatenatedKey(priceItem.getPlatform(), priceItem.getCategory(), priceItem.getName());
        }

        return keys;
    }

    public static String makeConcatenatedKey(String platform, String category, String name) {
        return platform + '|' + category + '|' + name;
    }
}
