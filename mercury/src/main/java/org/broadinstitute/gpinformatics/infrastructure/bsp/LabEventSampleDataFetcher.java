package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Wrapper around {@link org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao} that allows
 * easy access to each {@link org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel} associated with samples.
 */
public class LabEventSampleDataFetcher {

    private static final long serialVersionUID = -1432207534876411738L;

    @Inject
    private LabVesselDao labVesselDao;

    public LabEventSampleDataFetcher() {
    }

    /**
     * For the passed samples, return a map of the {@link org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel} objects associated.
     *
     * @param sampleKeys List of mercury sample keys.
     *
     * @return
     */
    public Map<String, List<LabVessel>> findMapBySampleKeys(Collection<String> sampleKeys) {
        return labVesselDao.findMapBySampleKeys(sampleKeys);
    }
}
