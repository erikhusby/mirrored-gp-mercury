package org.broadinstitute.gpinformatics.mercury.boundary.transfervis;


import org.broadinstitute.gpinformatics.mercury.boundary.graph.Graph;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

/**
 * RMI server for TransferVisualizer
 */
public interface TransferVisualizer {
    String serviceName = "TransferVisualizer";

    enum AlternativeId {
        SAMPLE_ID("Sample ID"),
        LCSET("LCSET"),
        BUCKET_ENTRY("Bucket Entry");

        private String displayName;

        AlternativeId(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
    
    Graph forTube(String tubeBarcode, List<AlternativeId> alternativeIds) throws RemoteException;

    Graph forContainer(String containerBarcode, List<AlternativeId> alternativeIds) throws RemoteException;

    Graph expandVertex(Graph graph, String vertexId, String idType, List<AlternativeId> alternativeIds) throws RemoteException;

    Map<String, List<String>> getIdsForTube(String tubeBarcode) throws RemoteException;

    enum IdType {
        PLATE_ID_TYPE,
        TUBE_IN_RACK_ID_TYPE,
        RECEPTACLE_ID_TYPE,
        CONTAINER_ID_TYPE
    }
}
