package org.broadinstitute.sequel.control.zims;

import org.broadinstitute.sequel.control.dao.run.IlluminaSequencingRunDao;
import org.broadinstitute.sequel.entity.project.Project;
import org.broadinstitute.sequel.entity.run.IlluminaSequencingRun;
import org.broadinstitute.sequel.entity.run.RunCartridge;
import org.broadinstitute.sequel.entity.sample.SampleInstance;
import org.broadinstitute.sequel.entity.vessel.StripTube;
import org.broadinstitute.sequel.entity.vessel.VesselPosition;
import org.broadinstitute.sequel.entity.zims.LibraryBean;
import org.broadinstitute.sequel.entity.zims.ZimsIlluminaRun;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Builds ZIMS library beans from entities
 */
public class LibraryBeanFactory {
    @Inject
    private IlluminaSequencingRunDao illuminaSequencingRunDao;

    public ZimsIlluminaRun buildLibraries(String runName) {
        List<LibraryBean> libraries = new ArrayList<LibraryBean>();
        IlluminaSequencingRun illuminaSequencingRun = illuminaSequencingRunDao.findByRunName(runName);
        RunCartridge runCartridge = illuminaSequencingRun.getSampleCartridge().iterator().next();
        StripTube stripTube = (StripTube) runCartridge.getTransfersTo().iterator().next().getSourceLabVessels().iterator().next();
        Set<SampleInstance> sampleInstances = stripTube.getVesselContainer().getSampleInstancesAtPosition(VesselPosition.TUBE1);
/*
        if(sampleInstances.size() != 96) {
            throw new RuntimeException("Wrong number of sample instances: " + sampleInstances.size());
        }
*/
        Project project = sampleInstances.iterator().next().getSingleProjectPlan().getProject();

//        libraries.add(new LibraryBean(
//                stripTubeWell.getLabel()/*String library*/,
//                project.getProjectName()/*String project*/,
//                null/*String initiative*/,
//                null/*Long workRequest*/,
//                null/*MolecularIndexingScheme indexingScheme*/,
//                null/*Boolean hasIndexingRead*/,
//                null/*String expectedInsertSize*/,
//                project.getSequenceAnalysisInstructions().getAnalysisMode()/*String analysisType*/,
//                project.getSequenceAnalysisInstructions().getReferenceSequence().toString()/*String referenceSequence*/,
//                null/*String referenceSequenceVersion*/,
//                null/*String collaboratorSampleId*/,
//                null/*String collaborator*/,
//                null/*String organism*/,
//                null/*String species*/,
//                null/*String strain*/,
//                null/*String sampleLSID*/,
//                null/*String tissueType*/,
//                null/*String expectedPlasmid*/,
//                null/*String aligner*/,
//                null/*String rrbsSizeRange*/,
//                null/*String restrictionEnzyme*/,
//                null/*String cellLine*/,
//                null/*String bait*/,
//                null/*String individual*/,
//                null/*Double labMeasuredInsertSize*/,
//                null/*Boolean positiveControl*/,
//                null/*Boolean negativeControl*/,
//                null/*String weirdness*/,
//                null/*Double preCircularizationDnaSize*/,
//                null/*Boolean partOfDevExperiment*/,
//                null/*TZDevExperimentData devExperimentData*/,
//                null/*String gssrBarcode*/,
//                null/*Collection<String> gssrBarcodes*/,
//                null/*String gssrSampleType*/,
//                null/*Short targetLaneCoverage*/));

        // todo jmt fix this
        return null;
    }
}
