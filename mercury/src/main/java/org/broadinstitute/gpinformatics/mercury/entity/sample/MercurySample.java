package org.broadinstitute.gpinformatics.mercury.entity.sample;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.common.AbstractSample;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.rapsheet.RapSheet;
import org.hibernate.annotations.Index;
import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents Mercury's view of a sample.  Sample information is held in another system (initially Athena),
 * this entity just holds a key to that system's representation.
 */
@Entity
@Audited
@Table(schema = "mercury")
public class MercurySample extends AbstractSample {

    /** Determines from which system Mercury gets metadata, e.g. collaborator sample ID */
    public enum MetadataSource {
        BSP,
        MERCURY
    }

    @Id
    @SequenceGenerator(name = "SEQ_MERCURY_SAMPLE", schema = "mercury", sequenceName = "SEQ_MERCURY_SAMPLE")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_MERCURY_SAMPLE")
    @SuppressWarnings("UnusedDeclaration")
    private Long mercurySampleId;

    @Index(name = "ix_ms_sample_key")
    private String sampleKey;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE, CascadeType.PERSIST})
    private RapSheet rapSheet;

    @OneToMany(mappedBy = "mercurySample", fetch = FetchType.LAZY,  cascade = CascadeType.PERSIST)
    private Set<ProductOrderSample> productOrderSamples = new HashSet<>();

    @Enumerated(EnumType.STRING)
    private MetadataSource metadataSource;

    @ManyToMany
    private Set<Metadata> metadata = new HashSet<>();

    /**
     * For JPA
     */
    protected MercurySample() {
    }

    public MercurySample(String sampleKey, MetadataSource metadataSource) {
        this.sampleKey = sampleKey;
        this.metadataSource = metadataSource;
    }

    public MercurySample(String sampleKey, BSPSampleDTO bspSampleDTO) {
        super(bspSampleDTO);
        this.sampleKey = sampleKey;
        this.metadataSource = MetadataSource.BSP;
    }

    public RapSheet getRapSheet() {
        if (rapSheet == null) {
            rapSheet = new RapSheet(this);
        }
        return rapSheet;
    }

    public void setRapSheet(RapSheet rapSheet) {
        this.rapSheet = rapSheet;
    }

    @Override
    public String getSampleKey() {
        return sampleKey;
    }

    public Set<ProductOrderSample> getProductOrderSamples() {
        return productOrderSamples;
    }

    public void addProductOrderSample(ProductOrderSample productOrderSample) {
        productOrderSamples.add(productOrderSample);
        productOrderSample.setMercurySample(this);
    }

    public MetadataSource getMetadataSource() {
        return metadataSource;
    }

    public void addMetaData(Metadata metadata) {
        this.metadata.add(metadata);
    }

    public Set<Metadata> getMetadata() {
        return metadata;
    }




    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof MercurySample)) {
            return false;
        }

        MercurySample that = (MercurySample) o;

        return new EqualsBuilder().append(getSampleKey(), that.getSampleKey()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(getSampleKey()).toHashCode();
    }

}
