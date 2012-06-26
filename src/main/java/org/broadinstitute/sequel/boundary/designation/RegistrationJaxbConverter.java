package org.broadinstitute.sequel.boundary.designation;

import org.broadinstitute.sequel.boundary.squid.IndexPosition;
import org.broadinstitute.sequel.boundary.squid.LibraryMolecularIndex;
import org.broadinstitute.sequel.boundary.squid.RegistrationSample;
import org.broadinstitute.sequel.boundary.squid.SequelLibrary;
import org.broadinstitute.sequel.boundary.squid.SequencingTechnology;
import org.broadinstitute.sequel.entity.reagent.MolecularIndex;
import org.broadinstitute.sequel.entity.reagent.MolecularIndexReagent;
import org.broadinstitute.sequel.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.sequel.entity.reagent.Reagent;
import org.broadinstitute.sequel.entity.sample.SampleInstance;
import org.broadinstitute.sequel.entity.vessel.MolecularState;
import org.broadinstitute.sequel.entity.vessel.TwoDBarcodedTube;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * RegistrationJaxbConverter is a utility class intended to assist in translating entities defined in SequeL to JAXB
 * DTO's used to
 *
 * @author Scott Matthews
 *         Date: 6/21/12
 *         Time: 3:10 PM
 */
public class RegistrationJaxbConverter {


    public static SequelLibrary squidify(TwoDBarcodedTube tubeIn) {

        final SequelLibrary registerLibrary = new SequelLibrary();
        registerLibrary.setLibraryName(tubeIn.getLabCentricName());

        Set<MolecularState.STRANDEDNESS> strandednessesState = new HashSet<MolecularState.STRANDEDNESS>();

        for(SampleInstance currSample:tubeIn.getSampleInstances()) {
            final RegistrationSample sampleInstance = new RegistrationSample();
            sampleInstance.setBspContextReference(currSample.getStartingSample().getSampleName());
            sampleInstance.setTechnology(SequencingTechnology.ILLUMINA);

            for(Reagent sampleReagent:currSample.getReagents()) {
                if(sampleReagent instanceof MolecularIndexReagent) {
                    for(Map.Entry<MolecularIndexingScheme.PositionHint,MolecularIndex> currScheme:((MolecularIndexReagent) sampleReagent).getMolecularIndexingScheme().getIndexes().entrySet()) {
                        LibraryMolecularIndex newIndex = new LibraryMolecularIndex();
                        newIndex.setMolecularBarcode(currScheme.getValue().getSequence());
                        if(currScheme.getKey() instanceof MolecularIndexingScheme.IlluminaPositionHint) {
                            switch ((MolecularIndexingScheme.IlluminaPositionHint) currScheme.getKey()) {
                                case P5:
                                    newIndex.setPositionHint(IndexPosition.ILLUMINA_P_5);
                                    break;
                                case P7:
                                    newIndex.setPositionHint(IndexPosition.ILLUMINA_P_7);
                                    break;
                                case IS1:
                                    newIndex.setPositionHint(IndexPosition.ILLUMINA_IS_1);
                                    break;
                                case IS2:
                                    newIndex.setPositionHint(IndexPosition.ILLUMINA_IS_2);
                                    break;
                                case IS3:
                                    newIndex.setPositionHint(IndexPosition.ILLUMINA_IS_3);
                                    break;
                                case IS4:
                                    newIndex.setPositionHint(IndexPosition.ILLUMINA_IS_4);
                                    break;
                                case IS5:
                                    newIndex.setPositionHint(IndexPosition.ILLUMINA_IS_5);
                                    break;
                                case IS6:
                                    newIndex.setPositionHint(IndexPosition.ILLUMINA_IS_6);
                                    break;
                            }
                        } else {
                            throw new RuntimeException("Illumina is the only Scheme technology allowed now for Exome");
                        }

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

        registerLibrary.setSingleStrandInd(MolecularState.STRANDEDNESS.SINGLE_STRANDED.equals(
                strandednessesState.iterator().next()));

        return registerLibrary;
    }

}
