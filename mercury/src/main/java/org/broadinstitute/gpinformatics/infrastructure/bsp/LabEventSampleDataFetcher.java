package org.broadinstitute.gpinformatics.infrastructure.bsp;

import com.google.common.collect.Multimap;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.util.Collection;

/**
 * Wrapper around {@link LabVesselDao} that allows easy access to each {@link LabVessel} associated with samples.
 */
@RequestScoped
public class LabEventSampleDataFetcher {

    @Inject
    private LabVesselDao labVesselDao;

    public LabEventSampleDataFetcher() {
    }

    /**
     * For the passed samples, return a map of the {@link LabVessel} objects associated.
     *
     * @param samples The list of product order samples.
     *
     * @return A mapping of the sample keys to the found vessels
     */
    public Multimap<String, LabVessel> findMapBySampleKeys(Collection<ProductOrderSample> samples) {
        return labVesselDao.findMapBySampleKeys(samples);
    }
}
