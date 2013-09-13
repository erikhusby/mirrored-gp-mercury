package org.broadinstitute.gpinformatics.mercury.boundary.graph;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A graph transferred from the TransferVisualizerServer to the TransferVisualizerClient, for rendering.
 */
public class Graph implements Serializable {
    private static final long serialVersionUID = 20100819L;

    /** A message for the user */
    private String message;
    /** Random access to vertices */
    private final Map<String, Vertex> mapIdToVertex = new HashMap<>();
    /** Random access to edges */
    private final Map<String, Edge> mapIdToEdge = new HashMap<>();
    /** Avoid visiting an event twice */
    private final Set<String> visitedEventIds = new HashSet<>();

    public Map<String, Vertex> getMapIdToVertex() {
        return mapIdToVertex;
    }

    public Map<String, Edge> getMapIdToEdge() {
        return mapIdToEdge;
    }

    public Set<String> getVisitedEventIds() {
        return visitedEventIds;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
