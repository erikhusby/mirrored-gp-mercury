package org.broadinstitute.gpinformatics.infrastructure.thrift;

import edu.mit.broad.prodinfo.thrift.lims.*;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Alternative;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Alternative
@Dependent
public class MockThriftService implements ThriftService {

    public MockThriftService() {
    }

    @Override
    public TZamboniRun fetchRun(String runName) {
        TZamboniRun run = null;

        try {
            run = ThriftFileAccessor.deserializeRun();
        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialize run from local file", e);
        }

        if (runName.equals(run.getRunName())) {
            return run;
        }
        else {
            return null;
        }
    }

    @Override
    public TZamboniRun fetchRunByBarcode(String runBarcode) {
        TZamboniRun run = null;

        try {
            run = ThriftFileAccessor.deserializeRun();
        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialize run from local file", e);
        }

        if (runBarcode.equals(run.getRunBarcode())) {
            return run;
        }
        else {
            return null;
        }
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
        return  null;
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


}