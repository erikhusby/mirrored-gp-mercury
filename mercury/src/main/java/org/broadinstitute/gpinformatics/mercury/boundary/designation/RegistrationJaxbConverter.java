package org.broadinstitute.gpinformatics.mercury.boundary.designation;

import org.broadinstitute.gpinformatics.mercury.boundary.squid.IndexPosition;
import org.broadinstitute.gpinformatics.mercury.boundary.squid.LibraryMolecularIndex;
import org.broadinstitute.gpinformatics.mercury.boundary.squid.RegistrationSample;
import org.broadinstitute.gpinformatics.mercury.boundary.squid.SequelLibrary;
import org.broadinstitute.gpinformatics.mercury.boundary.squid.SequencingPlanDetails;
import org.broadinstitute.gpinformatics.mercury.boundary.squid.SequencingTechnology;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndex;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.Strandedness;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * RegistrationJaxbConverter is a utility class intended to assist in translating entities defined in Mercury to JAXB
 * DTO's used to
 *
 * @author Scott Matthews
 *         Date: 6/21/12
 *         Time: 3:10 PM
 */
public class RegistrationJaxbConverter {


    public static SequelLibrary squidify(TwoDBarcodedTube tubeIn/*, ProjectPlan planIn*/) {

        final SequelLibrary registerLibrary = new SequelLibrary();
        registerLibrary.setLibraryName(tubeIn.getLabCentricName());
        registerLibrary.setReceptacleBarcode(tubeIn.getLabel());

        Set<Strandedness> strandednessesState = new HashSet<Strandedness>();

        for(SampleInstance currSample:tubeIn.getSampleInstances()) {
            final RegistrationSample sampleInstance = new RegistrationSample();
            sampleInstance.setBspContextReference(currSample.getStartingSample().getSampleKey());
            sampleInstance.setTechnology(SequencingTechnology.ILLUMINA);

            for(Reagent sampleReagent:currSample.getReagents()) {
                if(sampleReagent instanceof MolecularIndexReagent) {
                    for(Map.Entry<MolecularIndexingScheme.IndexPosition,MolecularIndex> currScheme:((MolecularIndexReagent) sampleReagent).getMolecularIndexingScheme().getIndexes().entrySet()) {
                        if(!currScheme.getKey().getTechnology().equals(MolecularIndexingScheme.TECHNOLOGY_ILLUMINA)) {
                            throw new RuntimeException("Illumina is the only Scheme technology allowed now for Exome");
                        }
                        LibraryMolecularIndex newIndex = new LibraryMolecularIndex();
                        newIndex.setMolecularBarcode(currScheme.getValue().getSequence());
                        IndexPosition indexPosition = IndexPosition.fromValue(currScheme.getKey().getIndexPosition().name());
                            newIndex.setPositionHint(indexPosition);
                        sampleInstance.getMolecularIndexes().add(newIndex);
                    }
                    break;
                }
            }

            strandednessesState.add(currSample.getMolecularState().getStrand());
            registerLibrary.getSamples().add(sampleInstance);
        }

        if(strandednessesState.size()>1) {
            throw new RuntimeException("The samples found here should either be all single stranded or all "+
                                               "Double stranded.  There should not be a mixture");
        }

        registerLibrary.setSingleStrandInd(Strandedness.SINGLE_STRANDED.equals(
                strandednessesState.iterator().next()));

        SequencingPlanDetails details = new SequencingPlanDetails();

//        details.setNumberOfLanes(planIn.getLaneCoverage());
//        details.setReadLength(planIn.getReadLength());

        registerLibrary.setPlanDetails(details);

        return registerLibrary;
    }

}
