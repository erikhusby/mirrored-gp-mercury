package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.bsp.GetSampleDetails;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.BarcodedTubeDao;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;

import javax.annotation.Nonnull;
import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.validation.constraints.Null;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Stateful
@RequestScoped
public class VesselEjb {

    @Inject
    private BarcodedTubeDao barcodedTubeDao;

    @Inject
    private MercurySampleDao mercurySampleDao;

    /**
     * Registers {@code BarcodedTube}s for all specified {@param tubeBarcodes} as well as
     * registering any required {@code MercurySample}s.  No new {@code MercurySample}s will be
     * created for sample names that already have {@code MercurySample} instances, but both
     * preexisting and newly created {@code MercurySample}s will be associated with the created
     * {@code BarcodedTube}s.  The sample names are gotten from the
     * {@code GetSampleDetails.SampleInfo} values of the {@param sampleInfoMap}.
     * @param tubeType either BarcodedTube.BarcodedTubeType.name or null (defaults to Matrix tube).
     */
    public void registerSamplesAndTubes(@Nonnull Collection<String> tubeBarcodes,
                                        @Null String tubeType,
                                        @Nonnull Map<String, GetSampleDetails.SampleInfo> sampleInfoMap) {

        // Determine which barcodes are already known to Mercury.
        List<BarcodedTube> previouslyRegisteredTubes = barcodedTubeDao.findListByBarcodes(tubeBarcodes);
        Set<String> previouslyRegisteredTubeBarcodes = new HashSet<>();
        for (BarcodedTube tube : previouslyRegisteredTubes) {
            previouslyRegisteredTubeBarcodes.add(tube.getLabel());
        }

        // The Set of tube barcodes that are known to BSP but not Mercury.
        Set<String> tubeBarcodesToRegister =
                Sets.difference(new HashSet<>(tubeBarcodes), previouslyRegisteredTubeBarcodes);

        // The Set of all sample names for the tubes to be registered.  This is needed
        // to associate MercurySamples to LabVessels irrespective of whether the MercurySamples
        // were created by this code.
        Set<String> sampleNamesToAssociate = new HashSet<>();
        for (String tubeBarcode : tubeBarcodesToRegister) {
            sampleNamesToAssociate.add(sampleInfoMap.get(tubeBarcode).getSampleId());
        }

        // The Set of MercurySample names that need to be registered.  These are all the sample
        // names for the tubes to be registered minus the sample names already known to Mercury.
        Set<String> previouslyRegisteredSampleNames = new HashSet<>();
        for (MercurySample mercurySample : mercurySampleDao.findBySampleKeys(sampleNamesToAssociate)) {
            previouslyRegisteredSampleNames.add(mercurySample.getSampleKey());
        }

        Set<String> sampleNamesToRegister =
                Sets.difference(sampleNamesToAssociate, previouslyRegisteredSampleNames);

        for (String sampleName : sampleNamesToRegister) {
            mercurySampleDao.persist(new MercurySample(sampleName, MercurySample.MetadataSource.BSP));
        }
        // Explicit flush required as Mercury runs in FlushModeType.COMMIT and we want to see the results of any
        // persists done in the loop above reflected in the query below.
        mercurySampleDao.flush();

        // Map all MercurySamples to be associated by sample name.
        Map<String, MercurySample> sampleNameToMercurySample = new HashMap<>();
        for (MercurySample mercurySample : mercurySampleDao.findBySampleKeys(sampleNamesToAssociate)) {
            sampleNameToMercurySample.put(mercurySample.getSampleKey(), mercurySample);
        }

        // Create BarcodedTubes for all tube barcodes to be registered and associate them with
        // the appropriate MercurySamples.
        BarcodedTube.BarcodedTubeType barcodedTubeType = null;
        if (StringUtils.isNotBlank(tubeType)) {
            barcodedTubeType = BarcodedTube.BarcodedTubeType.getByAutomationName(tubeType);
            if (barcodedTubeType == null) {
                throw new RuntimeException("No support for tube type '" + tubeType + "'.");
            }
        }
        for (String tubeBarcode : tubeBarcodesToRegister) {
            BarcodedTube tube = barcodedTubeType != null ?
                    new BarcodedTube(tubeBarcode, barcodedTubeType) : new BarcodedTube(tubeBarcode);
            String sampleId = sampleInfoMap.get(tube.getLabel()).getSampleId();
            tube.addSample(sampleNameToMercurySample.get(sampleId));
            mercurySampleDao.persist(tube);
        }

        mercurySampleDao.flush();
    }
}
