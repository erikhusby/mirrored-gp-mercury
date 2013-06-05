package org.broadinstitute.gpinformatics.infrastructure.common;

import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUtil;

import javax.annotation.Nonnull;
import javax.persistence.Transient;
import java.util.regex.Pattern;

/**
 * This abstraction describes a sample in both project management and LIMS in Mercury. Put code in here that will be
 * useful for both areas in the application.
 */
public abstract class AbstractSample {

    // FIXME: replace with real sample name pattern when CRSP jira is configured.
    public static final Pattern CRSP_SAMPLE_NAME_PATTERN = Pattern.compile("PDO-[A-Z1-9]{4,6}");

    @Transient
    private BSPSampleDTO bspSampleDTO = new BSPSampleDTO();

    @Transient
    private boolean hasBspDTOBeenInitialized;

    public AbstractSample() {
    }

    public AbstractSample(@Nonnull BSPSampleDTO bspSampleDTO) {
        setBspSampleDTO(bspSampleDTO);
    }

    public abstract String getSampleKey();

    public boolean needsBspMetaData() {
        return isInBspFormat() && !hasBspDTOBeenInitialized;
    }

    /**
     * @return true if sample is a loaded BSP sample but BSP didn't have any data for it.
     */
    public boolean bspMetaDataMissing() {
        return isInBspFormat() && hasBspDTOBeenInitialized && !bspSampleDTO.hasData();
    }

    public BSPSampleDTO getBspSampleDTO() {
        if (!hasBspDTOBeenInitialized) {
            if (isInBspFormat()) {
                BSPSampleDataFetcher bspSampleDataFetcher = ServiceAccessUtility.getBean(BSPSampleDataFetcher.class);
                bspSampleDTO = bspSampleDataFetcher.fetchSingleSampleFromBSP(getSampleKey());

                // If there is no DTO, create one with no data populated.
                if (bspSampleDTO == null) {
                    bspSampleDTO = new BSPSampleDTO();
                }
            }

            hasBspDTOBeenInitialized = true;
        }

        return bspSampleDTO;
    }

    public void setBspSampleDTO(@Nonnull BSPSampleDTO bspSampleDTO) {
        //noinspection ConstantConditions
        if (bspSampleDTO == null) {
            throw new NullPointerException("BSP Sample DTO cannot be null");
        }

        this.bspSampleDTO = bspSampleDTO;
        hasBspDTOBeenInitialized = true;
    }

    public boolean isInBspFormat() {
        return BSPUtil.isInBspFormat(getSampleKey());
    }

    public boolean isInCrspFormat() {
        return CRSP_SAMPLE_NAME_PATTERN.matcher(getSampleKey()).matches();
    }

    public String getBspSampleName() {
        // skip the SM- part of the name.
        if ((getSampleKey().length() > 3) && isInBspFormat()) {
            return getSampleKey().substring(3);
        }
        return getSampleKey();
    }
}
