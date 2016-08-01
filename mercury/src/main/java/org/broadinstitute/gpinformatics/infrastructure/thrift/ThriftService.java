package org.broadinstitute.gpinformatics.infrastructure.thrift;

import edu.mit.broad.prodinfo.thrift.lims.*;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Thrift client wrapper that handles all of the communication and error
 * handling details associated with using a thrift endpoint. No bare thrift
 * exceptions should escape from this layer.
 *
 * This helps us to test thrift consumers without having to connect to (slow)
 * deployed services.
 */
public interface ThriftService extends Serializable {

    public TZamboniRun fetchRun(String runName);

    public TZamboniRun fetchRunByBarcode(String runBarcode);

    public List<LibraryData> fetchLibraryDetailsByTubeBarcode(List<String> tubeBarcodes, boolean includeWorkRequestDetails);

    boolean doesSquidRecognizeAllLibraries(List<String> barcodes);

    public List<String> fetchMaterialTypesForTubeBarcodes(List<String> tubeBarcodes);

    public FlowcellDesignation findFlowcellDesignationByTaskName(final String taskName);

    public FlowcellDesignation findFlowcellDesignationByFlowcellBarcode(final String flowcellBarcode);

    public FlowcellDesignation findFlowcellDesignationByReagentBlockBarcode(final String flowcellBarcode);

    public List<String> findImmediatePlateParents(String plateBarcode);

    public String fetchUserIdForBadgeId(String badgeId);

    public Map<String, Boolean> fetchParentRackContentsForPlate(String plateBarcode);

    public double fetchQpcrForTube(String tubeBarcode);

    public double fetchQpcrForTubeAndType(String tubeBarcode, String qpcrType);

    public double fetchQuantForTube(String tubeBarcode, String quantType);

    public List<LibraryData> fetchLibraryDetailsByLibraryName(List<String> libraryNames);

    public List<String> fetchUnfulfilledDesignations();

    public List<String> findRelatedDesignationsForAnyTube(List<String> tubeBarcodes);

    public List<WellAndSourceTube> fetchSourceTubesForPlate(String plateBarcode);

    public List<PlateTransfer> fetchTransfersForPlate(String plateBarcode, short depth);

    public List<PoolGroup> fetchPoolGroups(List<String> tubeBarcoces);

    public Map<String,ConcentrationAndVolume> fetchConcentrationAndVolumeForTubeBarcodes(List<String> tubeBarcodes);
}
