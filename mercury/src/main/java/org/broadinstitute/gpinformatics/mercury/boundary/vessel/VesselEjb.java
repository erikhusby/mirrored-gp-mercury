package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import com.google.common.collect.Sets;
import org.broadinstitute.gpinformatics.infrastructure.bsp.GetSampleDetails;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.BarcodedTubeDao;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;

import javax.annotation.Nonnull;
import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
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
     * Registers {@code BarcodedTube}s for all specified {@param matrixBarcodes} as well as
     * registering any required {@code MercurySample}s.  No new {@code MercurySample}s will be
     * created for sample barcodes that already have {@code MercurySample} instances, but both
     * preexisting and newly created {@code MercurySample}s will be associated with the created
     * {@code BarcodedTube}s.  The sample barcodes are specified within the
     * {@code GetSampleDetails.SampleInfo} values of the {@param sampleInfoMap}.
     */
    public void registerSamplesAndTubes(@Nonnull Collection<String> matrixBarcodes,
                                        @Nonnull Map<String, GetSampleDetails.SampleInfo> sampleInfoMap) {

        // Determine which Matrix barcodes are already known to Mercury.
        List<BarcodedTube> previouslyRegisteredTubes = barcodedTubeDao.findListByBarcodes(matrixBarcodes);
        Set<String> previouslyRegisteredTubeBarcodes = new HashSet<>();
        for (BarcodedTube tube : previouslyRegisteredTubes) {
            previouslyRegisteredTubeBarcodes.add(tube.getLabel());
        }

        // The Set of Matrix barcodes that are known to BSP but not Mercury.
        Set<String> matrixBarcodesToRegister =
                Sets.difference(new HashSet<>(matrixBarcodes), previouslyRegisteredTubeBarcodes);

        // The Set of all sample barcodes for the tubes to be registered.  This is needed
        // to associate MercurySamples to LabVessels irrespective of whether the MercurySamples
        // were created by this code.
        Set<String> sampleBarcodesToAssociate = new HashSet<>();
        for (String matrixBarcode : matrixBarcodesToRegister) {
            sampleBarcodesToAssociate.add(sampleInfoMap.get(matrixBarcode).getSampleId());
        }

        // The Set of MercurySample barcodes that need to be registered.  This is all the sample
        // barcodes for the tubes to be registered minus the sample barcodes already known to Mercury.
        Set<String> previouslyRegisteredSampleBarcodes = new HashSet<>();
        for (MercurySample mercurySample : mercurySampleDao.findBySampleKeys(sampleBarcodesToAssociate)) {
            previouslyRegisteredSampleBarcodes.add(mercurySample.getSampleKey());
        }

        Set<String> sampleBarcodesToRegister =
                Sets.difference(sampleBarcodesToAssociate, previouslyRegisteredSampleBarcodes);

        for (String sampleBarcode : sampleBarcodesToRegister) {
            mercurySampleDao.persist(new MercurySample(sampleBarcode));
        }
        // Explicit flush required as Mercury runs in FlushModeType.COMMIT and we want to see the results of any
        // persists done in the loop above reflected in the query below.
        mercurySampleDao.flush();

        // Map all MercurySamples to be associated by sample barcode.
        Map<String, MercurySample> sampleBarcodeToMercurySample = new HashMap<>();
        for (MercurySample mercurySample : mercurySampleDao.findBySampleKeys(sampleBarcodesToAssociate)) {
            sampleBarcodeToMercurySample.put(mercurySample.getSampleKey(), mercurySample);
        }

        // Create BarcodedTubes for all matrix barcodes to be registered and associate them with
        // the appropriate MercurySamples.
        for (String matrixBarcode : matrixBarcodesToRegister) {
            BarcodedTube tube = new BarcodedTube(matrixBarcode);
            String sampleId = sampleInfoMap.get(tube.getLabel()).getSampleId();
            tube.addSample(sampleBarcodeToMercurySample.get(sampleId));
            mercurySampleDao.persist(tube);
        }

        mercurySampleDao.flush();
    }
}
