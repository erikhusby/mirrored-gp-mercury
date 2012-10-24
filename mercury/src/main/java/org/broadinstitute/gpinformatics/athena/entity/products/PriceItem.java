package org.broadinstitute.gpinformatics.athena.entity.products;

import clover.org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.envers.Audited;
import org.jetbrains.annotations.NotNull;

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


    /**
     * GP is the only platform referenced by Athena?
     */
    public enum Platform {

        GP("Genomics Platform");

        private final String quoteServerPlatform;

        private Platform(String quoteServerPlatform) {
            this.quoteServerPlatform = quoteServerPlatform;
        }

        public String getQuoteServerPlatform() {
            return quoteServerPlatform;
        }
    }


    /**
     * For my dummy test data I am making Category essentially synonymous with {@link ProductFamily}
     */
    public enum Category {

        GENERAL_PRODUCTS("General Products"),
        EXOME_SEQUENCING_ANALYSIS("Exome Sequencing Analysis"),
        WHOLE_GENOME_SEQUENCING_ANALYSIS("Whole Genome Sequencing Analysis"),
        WHOLE_GENOME_ARRAY_ANALYSIS("Whole Genome Array Analysis"),
        RNA_ANALYSIS("RNA Analysis"),
        ASSEMBLY_ANALYSIS("Assembly Analysis"),
        METAGENOMIC_ANALYSIS("Metagenomic Analysis"),
        EPIGENOMIC_ANALYSIS("Epigenomic Analysis"),
        ILLUMINA_SEQUENCING_ONLY("Illumina Sequencing Only"),
        ALTERNATIVE_TECHNOLOGIES("Alternative Technologies"),
        CUSTOM_PRODUCTS_TARGETED_SEQUENCING("Targeted Sequencing");

        private final String quoteServerCategory;


        Category(String quoteServerCategory) {
            this.quoteServerCategory = quoteServerCategory;
        }


        public String getQuoteServerCategory() {
            return quoteServerCategory;
        }
    }



    public enum Name {

        EXOME_EXPRESS("Exome Express"),
        STANDARD_EXOME_SEQUENCING("Standard Exome Sequencing"),
        DNA_EXTRACTION("DNA Extraction"),
        EXTRA_HISEQ_COVERAGE("Extra HiSeq Coverage"),
        TIME_AND_MATERIALS_LAB("Time and Materials - Laboratory"),
        TIME_AND_MATERIALS_IFX("Time and Materials - Informatics");

        private final String quoteServerName;

        private Name(String quoteServerName) {
            this.quoteServerName = quoteServerName;
        }

        public String getQuoteServerName() {
            return quoteServerName;
        }
    }



    @Id
    @SequenceGenerator(name = "SEQ_PRICE_ITEM", schema = "athena", sequenceName = "SEQ_PRICE_ITEM")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PRICE_ITEM")
    private Long priceItemId;

    private String platform;

    private String category;

    private String name;

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

    public PriceItem(@NotNull String platform, String category, @NotNull String name) {
        this.platform = platform;
        this.category = category;
        this.name = name;
    }


    public PriceItem(@NotNull Platform platform, Category category, @NotNull Name name,
                     @NotNull String quoteServerId) {

        if (platform == null) {
            throw new NullPointerException("Null platform specified!");
        }

        if (name == null) {
            throw new NullPointerException("Null price item name specified!");
        }

        if (quoteServerId == null) {
            throw new NullPointerException("Null quote server price item id specified!");
        }

        // don't currently know how to validate this other than against emptiness...
        if (StringUtils.isBlank(quoteServerId)) {
            throw new IllegalArgumentException("Empty quote server price item id specified!");
        }

        this.platform = platform.getQuoteServerPlatform();
        this.category = category.getQuoteServerCategory();
        this.name = name.getQuoteServerName();
        this.quoteServerId = quoteServerId;
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
