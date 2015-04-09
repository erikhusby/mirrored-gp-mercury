package org.broadinstitute.gpinformatics.mercury.entity.sample;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.presentation.Displayable;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.infrastructure.common.AbstractSample;
import org.broadinstitute.gpinformatics.mercury.boundary.manifest.ManifestSessionEjb;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.rapsheet.RapSheet;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.samples.MercurySampleData;
import org.hibernate.annotations.BatchSize;
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
import java.util.Collection;
import java.util.Collections;
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

    public static final String OTHER_METADATA_SOURCE = "OTHER";
    public static final String BSP_METADATA_SOURCE = "BSP";
    public static final String MERCURY_METADATA_SOURCE = "MERCURY";
    public static final String GSSR_METADATA_SOURCE = "GSSR";

    /**
     * Checks if the sample has successfully gone through the accessioning process.  If any vessel (for everything but
     * slides there should only be one vessel associated) has been accessioned, this will return true.
     *
     * Except for Slides, samples should have one and only one lab vessel associated with it.  Instead of
     * implementing
     * <code>
     *     iterator().next()
     * </code>
     * for getting the one item, I will leave the loop since it is a bit cleaner and safer.  When there is a concrete
     * solution for dealing with Slides for Accessioning, and there is a slide entity type, we can alter this logic to
     * check for number of vessels and compare it to vessel type.
     *
     */
    public boolean hasSampleBeenAccessioned() {

        boolean result = false;
        for (LabVessel labVessel : getLabVessel()) {
            if(!labVessel.canBeUsedForAccessioning()) {
                result = true;
                break;
            }
        }

        return result;
    }

    /**
     * Helper method to determine target vessel and Sample viability.  Extracts the logic of finding the lab vessel
     * to make this method available for re-use.
     * <p/>

     *
     * @param targetLabVessel   The label of the lab vessel that  should be associated with the given mercury sample
     * @return the referenced lab vessel if it is both found and eligible
     */
    public AccessioningCheckResult canSampleBeAccessionedWithTargetVessel(LabVessel targetLabVessel) {

        AccessioningCheckResult canAccession = AccessioningCheckResult.TUBE_NOT_ASSOCIATED;

        if(!targetLabVessel.canBeUsedForAccessioning()) {
            canAccession = AccessioningCheckResult.TUBE_ACCESSIONED_PREVIOUSLY;
        }   else {
            // Since upload happens just after Initial Tare, there should not be any other transfers.  For that reason,
            // searching through the MercurySamples on the vessels instead of getSampleInstancesV2 should be sufficient.
            for (MercurySample mercurySample : targetLabVessel.getMercurySamples()) {
                if (mercurySample.equals(this)) {
                    canAccession = AccessioningCheckResult.CAN_BE_ACCESSIONED;
                }
            }
        }
        return canAccession;

    }

    public enum AccessioningCheckResult {
        CAN_BE_ACCESSIONED, TUBE_NOT_ASSOCIATED, TUBE_ACCESSIONED_PREVIOUSLY
    }

    /**
     * Checks if the sample is eligible to be used for Clinical work.  The only criteria here would be that the
     * sample originated in Mercury.
     */
    public boolean canSampleBeUsedForClinical() {
        return getMetadataSource() == MetadataSource.MERCURY;
    }

    /** Determines from which system Mercury gets metadata, e.g. collaborator sample ID */
    public enum MetadataSource implements Displayable {
        BSP("BSP"),
        MERCURY("Mercury");
        private final String value;

        MetadataSource(String value) {
            this.value = value;
        }

        @Override
        public String getDisplayName() {
            return value;
        }
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
    @BatchSize(size = 100)
    private Set<ProductOrderSample> productOrderSamples = new HashSet<>();

    @Enumerated(EnumType.STRING)
    private MetadataSource metadataSource;

    @ManyToMany
    private Set<Metadata> metadata = new HashSet<>();

    // TODO: jms Shouldn't this be plural?
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

    /**
     * For fix-ups only.
     */
    void setMetadataSource(MetadataSource metadataSource) {
        this.metadataSource = metadataSource;
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

    public Set<LabVessel> getLabVessel() {
        return labVessel;
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

    public void addLabVessel(LabVessel vesselToAdd) {
        getLabVessel().add(vesselToAdd);
        vesselToAdd.addSample(this);
    }
}
