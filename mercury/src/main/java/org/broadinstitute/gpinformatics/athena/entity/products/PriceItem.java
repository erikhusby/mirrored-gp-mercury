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
        @UniqueConstraint(columnNames = {"platform", "categoryName", "priceItemName"})
})
public class PriceItem implements Serializable {


    /**
     * GP is the only platform referenced by Athena?
     */
    public enum Platform {

        GP("GP");

        private String quoteServerPlatform;

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

        private String quoteServerCategory;


        Category(String quoteServerCategory) {
            this.quoteServerCategory = quoteServerCategory;
        }


        public String getQuoteServerCategory() {
            return quoteServerCategory;
        }
    }



    public enum PriceItemName {

        EXOME_EXPRESS("Exome Express"),
        STANDARD_EXOME_SEQUENCING("Standard Exome Sequencing"),
        TISSUE_DNA_EXTRACTION("Tissue DNA Extraction"),
        BLOOD_DNA_EXTRACTION("Blood DNA Extraction"),
        EXTRA_HISEQ_COVERAGE("Extra HiSeq Coverage");

        private String quoteServerName;

        private PriceItemName(String quoteServerName) {
            this.quoteServerName = quoteServerName;
        }

        public String getQuoteServerName() {
            return quoteServerName;
        }
    }



    @Id
    @SequenceGenerator(name = "SEQ_PRICE_ITEM", sequenceName = "SEQ_PRICE_ITEM")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PRICE_ITEM")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Product product;

    private String platform;

    private String categoryName;

    private String priceItemName;

    private String quoteServicePriceItemId;


    // The @Transient fields below are "owned" by the quote server, what we hold in this class are just cached copies
    @Transient
    private String price;

    @Transient
    private String units;


    /**
     * Package visibile constructor for JPA
     */
    PriceItem() {}


    public PriceItem(Product product, Platform platform, Category categoryName, PriceItemName priceItemName, String quoteServicePriceItemId) {
        this.product = product;
        this.platform = platform.getQuoteServerPlatform();
        this.categoryName = categoryName.getQuoteServerCategory();
        this.priceItemName = priceItemName.getQuoteServerName();
        this.quoteServicePriceItemId = quoteServicePriceItemId;
    }


    public Long getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public String getPlatform() {
        return platform;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public String getPriceItemName() {
        return priceItemName;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PriceItem)) return false;

        PriceItem priceItem = (PriceItem) o;

        if (!categoryName.equals(priceItem.categoryName)) return false;
        if (!priceItemName.equals(priceItem.priceItemName)) return false;
        if (!platform.equals(priceItem.platform)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = platform.hashCode();
        result = 31 * result + categoryName.hashCode();
        result = 31 * result + priceItemName.hashCode();
        return result;
    }

}
