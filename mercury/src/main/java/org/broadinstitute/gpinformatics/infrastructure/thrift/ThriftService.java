package org.broadinstitute.gpinformatics.infrastructure.thrift;

import edu.mit.broad.prodinfo.thrift.lims.FlowcellDesignation;
import edu.mit.broad.prodinfo.thrift.lims.LibraryData;
import edu.mit.broad.prodinfo.thrift.lims.TZamboniRun;

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

    public List<LibraryData> fetchLibraryDetailsByTubeBarcode(List<String> tubeBarcodes, boolean includeWorkRequestDetails);

    boolean doesSquidRecognizeAllLibraries(List<String> barcodes);

    public List<String> fetchMaterialTypesForTubeBarcodes(List<String> tubeBarcodes);

    public FlowcellDesignation findFlowcellDesignationByTaskName(final String taskName);

    public FlowcellDesignation findFlowcellDesignationByFlowcellBarcode(final String flowcellBarcode);

    public FlowcellDesignation findFlowcellDesignationByReagentBlockBarcode(final String flowcellBarcode);

    public String fetchUserIdForBadgeId(String badgeId);

    public Map<String, Boolean> fetchParentRackContentsForPlate(String plateBarcode);

    public double fetchQpcrForTube(String tubeBarcode);

    public double fetchQuantForTube(String tubeBarcode, String quantType);

    public List<LibraryData> fetchLibraryDetailsByLibraryName(List<String> libraryNames);
}
