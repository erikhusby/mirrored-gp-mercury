package org.broadinstitute.gpinformatics.mercury.control.zims;

import org.broadinstitute.gpinformatics.mercury.control.dao.run.IlluminaSequencingRunDao;
import org.broadinstitute.gpinformatics.mercury.entity.bsp.BSPStartingSample;
import org.broadinstitute.gpinformatics.mercury.entity.project.Project;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.run.RunCartridge;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.sample.StartingSample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StripTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.zims.LibraryBean;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaChamber;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaRun;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Builds ZIMS library beans from entities
 */
public class LibraryBeanFactory {
    @Inject
    private IlluminaSequencingRunDao illuminaSequencingRunDao;

    public ZimsIlluminaRun buildLibraries(String runName) {
        IlluminaSequencingRun illuminaSequencingRun = illuminaSequencingRunDao.findByRunName(runName);
        return buildLibraries(illuminaSequencingRun);
    }

    public ZimsIlluminaRun buildLibraries(IlluminaSequencingRun illuminaSequencingRun) {
        List<LibraryBean> libraries = new ArrayList<LibraryBean>();
        RunCartridge runCartridge = illuminaSequencingRun.getSampleCartridge().iterator().next();
        StripTube stripTube = (StripTube) runCartridge.getTransfersTo().iterator().next().getSourceLabVessels().iterator().next();
//        Set<SampleInstance> sampleInstances = stripTube.getVesselContainer().getSampleInstancesAtPosition(VesselPosition.TUBE1);
        Map<StartingSample,Collection<LabVessel>> singleSampleLibrariesForInstance = stripTube.getVesselContainer().getSingleSampleAncestors(VesselPosition.TUBE1);

        for (Map.Entry<StartingSample, Collection<LabVessel>> entry : singleSampleLibrariesForInstance.entrySet()) {
            Collection<LabVessel> singleSampleLibraries = entry.getValue();
            if (singleSampleLibraries.isEmpty()) {
                throw new RuntimeException("Could not find single sample libraries for " + entry.getKey().getSampleName());
            }
            else if (singleSampleLibraries.size() > 1) {
                throw new RuntimeException("There are " + singleSampleLibraries.size() + " possible single sample libraries for " + entry.getKey().getSampleName());
            }

            LabVessel singleSampleLibrary = singleSampleLibraries.iterator().next();
            String libraryName = singleSampleLibrary.getLabCentricName();
//            Project project = sampleInstances.iterator().next().getSingleProjectPlan().getProject();
            SampleInstance sampleInstance = singleSampleLibrary.getSampleInstances().iterator().next();

            Project project = sampleInstance.getSingleProjectPlan().getProject();
            // todo jmt is this downcast legitimate
            BSPStartingSample bspStartingSample = (BSPStartingSample) sampleInstance.getStartingSample();
            libraries.add(new LibraryBean(
                    libraryName/*String library*/,
                    project.getProjectName()/*String project*/,
                    null/*String initiative*/,
                    null/*Long workRequest*/,
                    null/*MolecularIndexingScheme indexingScheme*/,
                    null/*Boolean hasIndexingRead*/,
                    null/*String expectedInsertSize*/,
                    null/*project.getSequenceAnalysisInstructions(sampleInstance).getAnalysisMode()*//*String analysisType*/,
                    null/*project.getSequenceAnalysisInstructions(sampleInstance).getReferenceSequence().toString()*//*String referenceSequence*/,
                    null/*String referenceSequenceVersion*/,
                    null/*String collaboratorSampleId*/,
                    null/*String collaborator*/,
                    null/*String organism*/,
                    null/*String species*/,
                    null/*String strain*/,
                    bspStartingSample.getBspDTO().getSampleLsid()/*String sampleLSID*/,
                    null/*String tissueType*/,
                    null/*String expectedPlasmid*/,
                    null/*String aligner*/,
                    null/*String rrbsSizeRange*/,
                    null/*String restrictionEnzyme*/,
                    null/*String cellLine*/,
                    null/*String bait*/,
                    null/*String individual*/,
                    0/*Double labMeasuredInsertSize*/,
                    null/*Boolean positiveControl*/,
                    null/*Boolean negativeControl*/,
                    null/*String weirdness*/,
                    0/*Double preCircularizationDnaSize*/,
                    null/*Boolean partOfDevExperiment*/,
                    null/*TZDevExperimentData devExperimentData*/,
                    null/*String gssrBarcode*/,
                    null/*Collection<String> gssrBarcodes*/,
                    null/*String gssrSampleType*/,
                    null/*Short targetLaneCoverage*/,
                    null/*Boolean doAggregation*/,
                    null/*Collection<String> customAmpliconSetNames*/,
                    false/*Boolean fastTrack*/));
        }
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(ZimsIlluminaRun.DATE_FORMAT);
        ZimsIlluminaRun zimsIlluminaRun = new ZimsIlluminaRun(illuminaSequencingRun.getRunName(),
                illuminaSequencingRun.getRunBarcode(), runCartridge.getCartridgeBarcode(),
                illuminaSequencingRun.getMachineName(), null, simpleDateFormat.format(illuminaSequencingRun.getRunDate()),
                (short) 0, (short) 0, (short) 0, (short) 0, (short) 0, false);
        zimsIlluminaRun.addLane(new ZimsIlluminaChamber((short) 1, libraries, "",""));
        return zimsIlluminaRun;
    }
}
