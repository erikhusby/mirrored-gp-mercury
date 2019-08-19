package org.broadinstitute.gpinformatics.infrastructure.common;

import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUtil;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.infrastructure.bsp.LabEventSampleDTO;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;

import javax.annotation.Nonnull;
import javax.persistence.Transient;

/**
 * This abstraction describes a sample in both project management and LIMS in Mercury. Put code in here that will be
 * useful for both areas in the application.
 */
public abstract class AbstractSample {

    @Transient
    private LabEventSampleDTO labEventSampleDTO = new LabEventSampleDTO();

    // TODO: replace BSP specific sample data support with a generic API that can support other platforms.
    @Transient
    private SampleData sampleData = new BspSampleData();

    @Transient
    private boolean hasBspSampleDataBeenInitialized;

    public AbstractSample() {
    }

    public AbstractSample(@Nonnull SampleData sampleData) {
        setSampleData(sampleData);
    }

    public abstract MercurySample.MetadataSource getMetadataSource();

    /**
     * @return the unique key for this sample, can be user visible
     */
    public abstract String getSampleKey();

    /**
     * @return true if sample may have BSP data that hasn't been fetched yet
     */
    public boolean needsBspMetaData() {
        return isInBspFormat() && !hasBspSampleDataBeenInitialized;
    }

    public boolean isHasBspSampleDataBeenInitialized() {
        return hasBspSampleDataBeenInitialized;
    }

    /**
     * @return true if sample is a loaded BSP sample but BSP didn't have any data for it.
     */
    public boolean bspMetaDataMissing() {
        return isInBspFormat() && hasBspSampleDataBeenInitialized && !sampleData.hasData();
    }

    /**
     * @return the BSP sample data for this sample
     */
    @Nonnull
    public SampleData getSampleData() {
        return getSampleData(false);
    }

    public SampleData getSampleData(boolean refetch) {
        if (!hasBspSampleDataBeenInitialized || refetch) {
            SampleDataFetcher sampleDataFetcher = ServiceAccessUtility.getBean(SampleDataFetcher.class);
            SampleData sampleDataTemp = sampleDataFetcher.fetchSampleData(getSampleKey());

            // If there is no DTO, create one with no data populated.
            if (sampleDataTemp == null) {
                sampleDataTemp = makeSampleData();
            }
            setSampleData(sampleDataTemp);
        }

        return sampleData;
    }

    protected abstract SampleData makeSampleData();

    /**
     * Set the BSP sample data manually. This is used when loading the sample data for a group of samples at once.
     *
     * @param sampleData the data to set
     */
    public void setSampleData(@Nonnull SampleData sampleData) {
        //noinspection ConstantConditions
        if (sampleData == null) {
            throw new NullPointerException("SampleData cannot be null");
        }

        this.sampleData = sampleData;
        hasBspSampleDataBeenInitialized = true;
    }

    /**
     * @return true if the sample is a BSP sample
     */
    public boolean isInBspFormat() {
        return BSPUtil.isInBspFormat(getSampleKey());
    }

    /**
     * @return the 'BSP sample name' which is the barcode without the SM or SP prefix
     */
    public String getBspSampleName() {
        // Skip the SM- part of the name.
        if ((getSampleKey().length() > 3) && isInBspFormat()) {
            return getSampleKey().substring(3);
        }
        return getSampleKey();
    }

    /**
     * Provides a method of getting lab events associated with the sample.
     *
     * @return {@link LabEventSampleDTO} object tied to the sample
     */
    public LabEventSampleDTO getLabEventSampleDTO() {
        return labEventSampleDTO;
    }

    /**
     * Set the sample lab event data manually. This is used when loading the sample data for a group of samples at once.
     *
     * @param labEventSampleDTO This object has the ability to get lab events for a specific mercury sample
     */
    public void setLabEventSampleDTO(@Nonnull LabEventSampleDTO labEventSampleDTO) {
        this.labEventSampleDTO = labEventSampleDTO;
    }

}
