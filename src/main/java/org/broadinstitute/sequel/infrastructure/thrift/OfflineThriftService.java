package org.broadinstitute.sequel.infrastructure.thrift;

import edu.mit.broad.prodinfo.thrift.lims.*;
import org.apache.thrift.TException;
import org.broadinstitute.sequel.infrastructure.Offline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author breilly
 */
@Offline
public class OfflineThriftService implements ThriftService {

    private static int libraryNumber = 100;

    @Override
    public TZamboniRun fetchRun(String runName) throws TZIMSException, TException {
        return makeRun(runName, 8, 12);
    }

    @Override
    public boolean doesSquidRecognizeAllLibraries(List<String> barcodes) {
        return false;
    }

    @Override
    public FlowcellDesignation findFlowcellDesignationByTaskName(String taskName) throws TException, TZIMSException {
        return null;
    }

    @Override
    public FlowcellDesignation findFlowcellDesignationByFlowcellBarcode(String flowcellBarcode) throws TException, TZIMSException {
        return null;
    }

    public static TZamboniRun makeRun(String runName, int numLanes, int numLibraries) {
        List<TZamboniLane> lanes = new ArrayList<TZamboniLane>();
        for (int i = 1; i <= numLanes; i++) {
            lanes.add(makeLane(i, numLibraries));
        }
        List<TZamboniRead> reads = new ArrayList<TZamboniRead>();
        reads.add(new TZamboniRead((short) 1, (short) 10, TZReadType.INDEX));
        reads.add(new TZamboniRead((short) 11, (short) 20, TZReadType.TEMPLATE));
        TZamboniRun run = new TZamboniRun(runName, "Flowcell-123", lanes, "Sequencer 123", "Test Sequencer", "05/11/2012 17:08", "Run-123", (short) 1, (short) 2, (short) 3, (short) 4, (short) 5, false, 123, reads);
        return run;
    }

    private static TZamboniLane makeLane(int laneNumber, int numLibraries) {
        List<TZamboniLibrary> libraries = new ArrayList<TZamboniLibrary>();
        for (int i = 0; i < numLibraries; i++) {
            libraries.add(makeLibrary(Integer.toString(libraryNumber)));
            libraryNumber++;
        }
        return new TZamboniLane((short) laneNumber, libraries, "PESP1+T");
    }

    private static TZamboniLibrary makeLibrary(String number) {
        List<String> conditions = new ArrayList<String>();
        for (int i = 0; i < 3; i++) {
            conditions.add("condition " + number + "-" + i);
        }
        List<String> gssrBarcodes = new ArrayList<String>();
        for (int i = 0; i < 3; i++) {
            gssrBarcodes.add(number + "." + i);
        }
        TZDevExperimentData experimentData = new TZDevExperimentData("Experiment " + number, conditions);
        return new TZamboniLibrary("Library-" + number, "Project-" + number, "Initiative-" + number, 1L, new MolecularIndexingScheme("IndexingScheme-" + number, new HashMap<IndexPosition, String>()), null, null, number, "Analysis-" + number, (short) 1, "GSSR-" + number, "OrganismDescription-" + number, "LSID-" + number, "Strain-" + number, "Sample-" + number, "Collaborator-" + number, "Tissue-" + number, "Organism-" + number, "Plasmid-" + number, false, false, "Aligner-" + number, "Size Range " + number, "Enzyme-" + number, "Species-" + number, "CellLine-" + number, "Reference-" + number, "RevVer-" + number, "Bait-" + number, true, "GSSR SampleType-" + number, gssrBarcodes, "Individual-" + number, 123.4, false, false, "no weirdness", 123.4, false, experimentData, new ArrayList<String>());

    }
}
