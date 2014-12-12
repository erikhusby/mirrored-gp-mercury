package org.broadinstitute.gpinformatics.mercury.entity.sample;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.infrastructure.common.AbstractSample;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.rapsheet.RapSheet;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.samples.MercurySampleData;
import org.hibernate.annotations.Index;
import org.hibernate.envers.Audited;

import javax.annotation.Nonnull;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents Mercury's view of a sample.  Sample information is held in another system (initially Athena),
 * this entity just holds a key to that system's representation.
 */
@Entity
@Audited
@Table(schema = "mercury")
public class MercurySample extends AbstractSample {

    public static final String OTHER_METADATA_SOURCE = "OTHER";
    public static final String BSP_METADATA_SOURCE = "BSP";
    public static final String MERCURY_METADATA_SOURCE = "MERCURY";
    public static final String GSSR_METADATA_SOURCE = "GSSR";

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

    @ManyToMany(mappedBy = "mercurySamples", cascade = CascadeType.PERSIST)
    protected Set<LabVessel> labVessel = new HashSet<>();

    /**
     * For JPA
     */
    protected MercurySample() {
    }

    /**
     * Creates a new MercurySample with a specific metadata source in the absence of the actual sample data.
     *
     * @param sampleKey         the name of the sample
     * @param metadataSource    the source of the sample data
     */
    public MercurySample(String sampleKey, MetadataSource metadataSource) {
        this.sampleKey = sampleKey;
        this.metadataSource = metadataSource;
    }

    /**
     * Creates a new MercurySample with the given sample data from BSP.
     *
     * @param sampleKey        the name of the sample
     * @param bspSampleData    the sample data as fetched from BSP
     */
    public MercurySample(String sampleKey, BspSampleData bspSampleData) {
        super(bspSampleData);
        this.sampleKey = sampleKey;
        this.metadataSource = MetadataSource.BSP;
    }

    /**
     * Creates a new MercurySample with the given sample data in Mercury.
     *
     * @param sampleKey    the name of the sample
     * @param metadata     the sample data to associate with the sample
     */
    public MercurySample(String sampleKey, Set<Metadata> metadata) {
        this(sampleKey, MetadataSource.MERCURY);
        addMetadata(metadata);
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

    @Override
    public MetadataSource getMetadataSource() {
        return metadataSource;
    }

    public void addMetadata(Set<Metadata> metadata) {
        if (metadataSource == MetadataSource.MERCURY) {
            this.metadata.addAll(metadata);
            setSampleData(new MercurySampleData(sampleKey, this.metadata));
        } else {
            throw new IllegalStateException(String.format(
                    "MercurySamples with metadata source of %s cannot have Mercury metadata", metadataSource));
        }
    }

    public Set<Metadata> getMetadata() {
        return metadata;
    }

    public Long getMercurySampleId() {
        return mercurySampleId;
    }

    /**
     * Returns the text that the pipeline uses to figure out
     * where to go for more sample metadata.  Do not alter
     * these values without consulting the pipeline team.
     */
    public String getMetadataSourceForPipelineAPI() {
        MercurySample.MetadataSource metadataSource = getMetadataSource();
        String metadataSourceString;

        if (metadataSource == MercurySample.MetadataSource.BSP) {
            metadataSourceString = BSP_METADATA_SOURCE;
        }
        else if (metadataSource == MercurySample.MetadataSource.MERCURY) {
            metadataSourceString = MERCURY_METADATA_SOURCE;
        }
        else {
            if (isInGSSRFormat(getSampleKey())) {
                metadataSourceString = GSSR_METADATA_SOURCE;
            }
            else {
                metadataSourceString = OTHER_METADATA_SOURCE;
            }
        }
        return metadataSourceString;
    }

    private boolean isInGSSRFormat(@Nonnull String sampleId) {
        return sampleId.matches("\\d+\\.\\d+");
    }

    public SampleData makeSampleData() {
        switch (metadataSource) {
        case BSP:
            return new BspSampleData();
        case MERCURY:
            return new MercurySampleData(sampleKey, Collections.<Metadata>emptySet());
        default:
            throw new IllegalStateException("Unknown sample data source: " + metadataSource);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || !(OrmUtil.proxySafeIsInstance(o, MercurySample.class))) {
            return false;
        }

        MercurySample that = OrmUtil.proxySafeCast(o, MercurySample.class);

        return new EqualsBuilder().append(getSampleKey(), that.getSampleKey()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(getSampleKey()).toHashCode();
    }

    public void removeSampleFromVessels(Collection<LabVessel> vesselsForRemoval) {
        for (LabVessel labVesselForRemoval : vesselsForRemoval) {
            labVesselForRemoval.getMercurySamples().remove(this);
            labVessel.remove(labVesselForRemoval);
        }
    }
}
