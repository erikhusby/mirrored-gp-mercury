package org.broadinstitute.gpinformatics.infrastructure.thrift;

import edu.mit.broad.prodinfo.thrift.lims.ConcentrationAndVolume;
import edu.mit.broad.prodinfo.thrift.lims.FlowcellDesignation;
import edu.mit.broad.prodinfo.thrift.lims.IndexPosition;
import edu.mit.broad.prodinfo.thrift.lims.LibraryData;
import edu.mit.broad.prodinfo.thrift.lims.MolecularIndexingScheme;
import edu.mit.broad.prodinfo.thrift.lims.PlateTransfer;
import edu.mit.broad.prodinfo.thrift.lims.PoolGroup;
import edu.mit.broad.prodinfo.thrift.lims.TZDevExperimentData;
import edu.mit.broad.prodinfo.thrift.lims.TZReadType;
import edu.mit.broad.prodinfo.thrift.lims.TZamboniLane;
import edu.mit.broad.prodinfo.thrift.lims.TZamboniLibrary;
import edu.mit.broad.prodinfo.thrift.lims.TZamboniRead;
import edu.mit.broad.prodinfo.thrift.lims.TZamboniRun;
import edu.mit.broad.prodinfo.thrift.lims.WellAndSourceTube;
import org.broadinstitute.gpinformatics.infrastructure.Offline;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Alternative;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author breilly
 */
@Offline
@Alternative
@Dependent
public class OfflineThriftService implements ThriftService {

    public OfflineThriftService(){}

    private static int libraryNumber = 100;

    @Override
    public TZamboniRun fetchRun(String runName) {
        return makeRun(runName, 8, 12);
    }

    @Override
    public TZamboniRun fetchRunByBarcode(String runBarcode) {
        TZamboniRun run = makeRun("Run" + System.currentTimeMillis(), 8, 12);
        run.setRunBarcode(runBarcode);
        return run;
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
    public double fetchQpcrForTubeAndType(String tubeBarcode, String qpcrType) {
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

    @Override
    public Map<String, ConcentrationAndVolume> fetchConcentrationAndVolumeForTubeBarcodes(List<String> tubeBarcodes) {
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
        TZamboniLane zamboniLane = new TZamboniLane();
        zamboniLane.setLaneNumber((short) laneNumber);
        zamboniLane.setLibraries(libraries);
        zamboniLane.setPrimer("PESP1+T");
        zamboniLane.setSequencedLibraryName("LaneLibrary-" + laneNumber);
        return zamboniLane;
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
        TZamboniLibrary zamboniLibrary =
                new TZamboniLibrary();
        zamboniLibrary.setLibrary("Library-" + number);        zamboniLibrary.setProject("Project-" + number);
        zamboniLibrary.setInitiative("Initiative-" + number);        zamboniLibrary.setWorkRequestId(1L);
        zamboniLibrary.setMolecularIndexes(new MolecularIndexingScheme("IndexingScheme-" + number, new HashMap<IndexPosition, String>()));
        zamboniLibrary.setExpectedInsertSize(number);        zamboniLibrary.setAnalysisType("Analysis-" + number);
        zamboniLibrary.setTargetLaneCoverage((short) 1);     zamboniLibrary.setGssrBarcode("GSSR-" + number);
        zamboniLibrary.setSampleOrganismDescription("OrganismDescription-" + number) ;
        zamboniLibrary.setLsid("LSID-" + number) ;        zamboniLibrary.setStrain("Strain-" + number) ;
        zamboniLibrary.setSampleAlias("Sample-" + number);         zamboniLibrary.setSampleCollaborator("Collaborator-" + number);
        zamboniLibrary.setTissueType("Tissue-" + number); zamboniLibrary.setOrganism("Organism-" + number);
        zamboniLibrary.setExpectedPlasmid("Plasmid-" + number);
        zamboniLibrary.setAggregate(false); zamboniLibrary.setCalibrateQualities(false);
        zamboniLibrary.setAligner("Aligner-" + number); zamboniLibrary.setRrbsSizeRange("Size Range " + number);
        zamboniLibrary.setRestrictionEnzyme("Enzyme-" + number); zamboniLibrary.setSpecies("Species-" + number);
        zamboniLibrary.setCellLine("CellLine-" + number); zamboniLibrary.setReferenceSequence("Reference-" + number);
        zamboniLibrary.setReferenceSequenceVersion("RevVer-" + number);
        zamboniLibrary.setBaitSetName("Bait-" + number); zamboniLibrary.setHasIndexingRead(true);
        zamboniLibrary.setGssrSampleType("GSSR SampleType-" + number);
        zamboniLibrary.setGssrBarcodes(gssrBarcodes); zamboniLibrary.setIndividual("Individual-" + number);
        zamboniLibrary.setLabMeasuredInsertSize(123.4);
        zamboniLibrary.setPositiveControl(false); zamboniLibrary.setNegativeControl(false);
        zamboniLibrary.setDevExperimentData(experimentData);
        zamboniLibrary.setCustomAmpliconSetNames(new ArrayList<String>());
        zamboniLibrary.setFastTrack(false);
        return zamboniLibrary;

    }
}
