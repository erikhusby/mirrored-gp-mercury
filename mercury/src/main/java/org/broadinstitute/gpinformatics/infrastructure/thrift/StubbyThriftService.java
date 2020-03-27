package org.broadinstitute.gpinformatics.infrastructure.thrift;

import edu.mit.broad.prodinfo.thrift.lims.ConcentrationAndVolume;
import edu.mit.broad.prodinfo.thrift.lims.FlowcellDesignation;
import edu.mit.broad.prodinfo.thrift.lims.LibraryData;
import edu.mit.broad.prodinfo.thrift.lims.PlateTransfer;
import edu.mit.broad.prodinfo.thrift.lims.PoolGroup;
import edu.mit.broad.prodinfo.thrift.lims.TZamboniRun;
import edu.mit.broad.prodinfo.thrift.lims.WellAndSourceTube;
import org.broadinstitute.gpinformatics.infrastructure.test.withdb.RunTimeAlternatives;

import javax.annotation.Priority;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;
import java.util.List;
import java.util.Map;
@Alternative
@Priority(1)
@Dependent
public class StubbyThriftService implements ThriftService {
    @Inject
    private OfflineThriftService defaultService;

    public StubbyThriftService() {
    }

    private ThriftService getService() {
        ThriftService service = RunTimeAlternatives.getThreadLocalAlternative(ThriftService.class);
        return (service == null) ? defaultService : service;
    }

    @Override
    public TZamboniRun fetchRun(String runName) {
        return getService().fetchRun(runName);
    }

    @Override
    public TZamboniRun fetchRunByBarcode(String runBarcode) {
        return getService().fetchRunByBarcode(runBarcode);
    }

    @Override
    public List<LibraryData> fetchLibraryDetailsByTubeBarcode(List<String> tubeBarcodes, boolean includeWorkRequestDetails) {
        return getService().fetchLibraryDetailsByTubeBarcode(tubeBarcodes, includeWorkRequestDetails);
    }

    @Override
    public boolean doesSquidRecognizeAllLibraries(List<String> barcodes) {
        return getService().doesSquidRecognizeAllLibraries(barcodes);
    }

    @Override
    public List<String> fetchMaterialTypesForTubeBarcodes(List<String> tubeBarcodes) {
        return getService().fetchMaterialTypesForTubeBarcodes(tubeBarcodes);
    }

    @Override
    public FlowcellDesignation findFlowcellDesignationByTaskName(String taskName) {
        return getService().findFlowcellDesignationByTaskName(taskName);
    }

    @Override
    public FlowcellDesignation findFlowcellDesignationByFlowcellBarcode(String flowcellBarcode) {
        return getService().findFlowcellDesignationByFlowcellBarcode(flowcellBarcode);
    }

    @Override
    public FlowcellDesignation findFlowcellDesignationByReagentBlockBarcode(String flowcellBarcode) {
        return getService().findFlowcellDesignationByReagentBlockBarcode(flowcellBarcode);
    }

    @Override
    public List<String> findImmediatePlateParents(String plateBarcode) {
        return getService().findImmediatePlateParents(plateBarcode);
    }

    @Override
    public String fetchUserIdForBadgeId(String badgeId) {
        return getService().fetchUserIdForBadgeId(badgeId);
    }

    @Override
    public Map<String, Boolean> fetchParentRackContentsForPlate(String plateBarcode) {
        return getService().fetchParentRackContentsForPlate(plateBarcode);
    }

    @Override
    public double fetchQpcrForTube(String tubeBarcode) {
        return getService().fetchQpcrForTube(tubeBarcode);
    }

    @Override
    public double fetchQpcrForTubeAndType(String tubeBarcode, String qpcrType) {
        return getService().fetchQpcrForTubeAndType(tubeBarcode, qpcrType);
    }

    @Override
    public double fetchQuantForTube(String tubeBarcode, String quantType) {
        return getService().fetchQuantForTube(tubeBarcode, quantType);
    }

    @Override
    public List<LibraryData> fetchLibraryDetailsByLibraryName(List<String> libraryNames) {
        return getService().fetchLibraryDetailsByLibraryName(libraryNames);
    }

    @Override
    public List<String> fetchUnfulfilledDesignations() {
        return getService().fetchUnfulfilledDesignations();
    }

    @Override
    public List<String> findRelatedDesignationsForAnyTube(List<String> tubeBarcodes) {
        return getService().findRelatedDesignationsForAnyTube(tubeBarcodes);
    }

    @Override
    public List<WellAndSourceTube> fetchSourceTubesForPlate(String plateBarcode) {
        return getService().fetchSourceTubesForPlate(plateBarcode);
    }

    @Override
    public List<PlateTransfer> fetchTransfersForPlate(String plateBarcode, short depth) {
        return getService().fetchTransfersForPlate(plateBarcode, depth);
    }

    @Override
    public List<PoolGroup> fetchPoolGroups(List<String> tubeBarcoces) {
        return getService().fetchPoolGroups(tubeBarcoces);
    }

    @Override
    public Map<String, ConcentrationAndVolume> fetchConcentrationAndVolumeForTubeBarcodes(List<String> tubeBarcodes) {
        return getService().fetchConcentrationAndVolumeForTubeBarcodes(tubeBarcodes);
    }
}