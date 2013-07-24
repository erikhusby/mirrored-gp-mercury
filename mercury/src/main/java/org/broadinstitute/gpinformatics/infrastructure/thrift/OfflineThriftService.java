package org.broadinstitute.gpinformatics.infrastructure.thrift;

import edu.mit.broad.prodinfo.thrift.lims.*;
import org.broadinstitute.gpinformatics.infrastructure.Offline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author breilly
 */
@Offline
public class OfflineThriftService implements ThriftService {

    private static int libraryNumber = 100;

    @Override
    public TZamboniRun fetchRun(String runName) {
        return makeRun(runName, 8, 12);
    }

    @Override
    public List<LibraryData> fetchLibraryDetailsByTubeBarcode(List<String> tubeBarcodes, boolean includeWorkRequestDetails) {
        return null;
    }

    @Override
    public boolean doesSquidRecognizeAllLibraries(List<String> barcodes) {
        return false;
    }

    @Override
    public List<String> fetchMaterialTypesForTubeBarcodes(List<String> tubeBarcodes) {
        return null;
    }

    @Override
    public FlowcellDesignation findFlowcellDesignationByTaskName(String taskName) {
        return null;
    }

    @Override
    public FlowcellDesignation findFlowcellDesignationByFlowcellBarcode(String flowcellBarcode) {
        return null;
    }

    @Override
    public FlowcellDesignation findFlowcellDesignationByReagentBlockBarcode(String flowcellBarcode) {
        return null;
    }

    @Override
    public List<String> findImmediatePlateParents(String plateBarcode) {
        return null;
    }

    @Override
    public String fetchUserIdForBadgeId(String badgeId) {
        return null;
    }

    @Override
    public Map<String, Boolean> fetchParentRackContentsForPlate(String plateBarcode) {
        return null;
    }

    @Override
    public double fetchQpcrForTube(String tubeBarcode) {
        return 0;
    }

    @Override
    public double fetchQuantForTube(String tubeBarcode, String quantType) {
        return 0;
    }

    @Override
    public List<LibraryData> fetchLibraryDetailsByLibraryName(List<String> libraryNames) {
        List<LibraryData> libraryDataList = new ArrayList<>();
        for (String libraryName : libraryNames) {
            LibraryData libraryData = new LibraryData();
            libraryData.setLibraryName(libraryName);
            libraryData.setLibraryNameIsSet(true);
            libraryDataList.add(libraryData);
        }

        return libraryDataList;
    }

    @Override
    public List<String> fetchUnfulfilledDesignations() {
        return null;
    }

    @Override
    public List<String> findRelatedDesignationsForAnyTube(List<String> tubeBarcodes) {
        return null;
    }

    @Override
    public List<WellAndSourceTube> fetchSourceTubesForPlate(String plateBarcode) {
        return null;
    }

    @Override
    public List<PlateTransfer> fetchTransfersForPlate(String plateBarcode, short depth) {
        return null;
    }

    @Override
    public List<PoolGroup> fetchPoolGroups(List<String> tubeBarcoces) {
        return null;
    }

    public static TZamboniRun makeRun(String runName, int numLanes, int numLibraries) {
        List<TZamboniLane> lanes = new ArrayList<>();
        for (int i = 1; i <= numLanes; i++) {
            lanes.add(makeLane(i, numLibraries));
        }
        List<TZamboniRead> reads = new ArrayList<>();
        reads.add(new TZamboniRead((short) 1, (short) 10, TZReadType.INDEX));
        reads.add(new TZamboniRead((short) 11, (short) 20, TZReadType.TEMPLATE));
        //TZamboniRun run = new TZamboniRun(runName, "Flowcell-123", lanes, "Sequencer 123", "Test Sequencer", "05/11/2012 17:08", "Run-123", (short) 1, (short) 2,
        //        (short) 3, (short) 4, (short) 5, false, 123, reads, "36T8B", 999.987654);
        TZamboniRun tZRun = new TZamboniRun();
        tZRun.setRunName(runName); tZRun.setFlowcellBarcode("Flowcell-123"); tZRun.setLanes(lanes);
        tZRun.setSequencer("Sequencer 123"); tZRun.setSequencerModel("Test Sequencer"); tZRun.setRunDate("05/11/2012 17:08");
        tZRun.setRunBarcode("Run-123"); tZRun.setFirstCycle((short) 1); tZRun.setFirstCycleReadLength((short) 2);
        tZRun.setLastCycle((short) 3); tZRun.setMolBarcodeCycle((short) 4); tZRun.setMolBarcodeLength((short) 5);
        tZRun.setRunId(123); tZRun.setReads(reads);
        //tZRun.setActualReadStructure("36T8B"); tZRun.setImagedAreaPerLaneMM2(999.986);

        return tZRun;
    }

    private static TZamboniLane makeLane(int laneNumber, int numLibraries) {
        List<TZamboniLibrary> libraries = new ArrayList<>();
        for (int i = 0; i < numLibraries; i++) {
            libraries.add(makeLibrary(Integer.toString(libraryNumber)));
            libraryNumber++;
        }
        return new TZamboniLane((short) laneNumber, libraries, "PESP1+T", "LaneLibrary-" + laneNumber);
    }

    private static TZamboniLibrary makeLibrary(String number) {
        List<String> conditions = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            conditions.add("condition " + number + "-" + i);
        }
        List<String> gssrBarcodes = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            gssrBarcodes.add(number + "." + i);
        }
        TZDevExperimentData experimentData = new TZDevExperimentData("Experiment " + number, conditions);
        return new TZamboniLibrary("Library-" + number, "Project-" + number, "Initiative-" + number, 1L, new MolecularIndexingScheme("IndexingScheme-" + number, new HashMap<IndexPosition, String>()), null, null, number, "Analysis-" + number, (short) 1, "GSSR-" + number, "OrganismDescription-" + number, "LSID-" + number, "Strain-" + number, "Sample-" + number, "Collaborator-" + number, "Tissue-" + number, "Organism-" + number, "Plasmid-" + number, false, false, "Aligner-" + number, "Size Range " + number, "Enzyme-" + number, "Species-" + number, "CellLine-" + number, "Reference-" + number, "RevVer-" + number, "Bait-" + number, true, "GSSR SampleType-" + number, gssrBarcodes, "Individual-" + number, 123.4, false, false, "no weirdness", 123.4, false, experimentData, new ArrayList<String>(), false,null,null);

    }
}
