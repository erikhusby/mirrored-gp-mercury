package org.broadinstitute.gpinformatics.mercury.boundary.graph;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a vertex in a graph, for use as DTO for graphing clients.  Intended to be abstract enough to be used
 * for more than Transfer Visualization.
 */
public class Vertex implements Serializable {
    private static final long serialVersionUID = 20110104L;

    private final String id;
    private final String idType;
    private final String title;
    private final List<String> details = new ArrayList<>();
    private Map<String, List<String>> alternativeIds = new HashMap<>();
    private final Vertex[][] childVertices;
    private final Vertex parentVertex;

    private boolean hasMoreEdges;
    private boolean rendered;

    public Vertex(String id, String idType, String title, Vertex parentVertex, Map<String, List<String>> alternativeIds,
                  Vertex[][] childVertices) {
        this.id = id;
        this.idType = idType;
        this.title = title;
        this.parentVertex = parentVertex;
        this.alternativeIds = alternativeIds;
        this.childVertices = childVertices;
    }

    public Vertex(String id, String idType, String title, Map<String, List<String>> alternativeIds) {
        this(id, idType, title, null, alternativeIds, null);
    }

    public Vertex(String id, String idType, String title, Vertex parentVertex,
                  Map<String, List<String>> alternativeIds) {
        this(id, idType, title, parentVertex, alternativeIds, null);
    }

    public Vertex(String id, String idType, String title, int numChildRows, int numChildColumns) {
        this(id, idType, title, null, new HashMap<String, List<String>>(), new Vertex[numChildRows][numChildColumns]);
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public List<String> getDetails() {
        return details;
    }

    public Vertex[][] getChildVertices() {
        return childVertices;
    }

    public Vertex getParentVertex() {
        return parentVertex;
    }

    @Override
    public String toString() {
        return title;
    }

    public boolean getHasMoreEdges() {
        return hasMoreEdges;
    }

    public void setHasMoreEdges(boolean hasMoreEdges) {
        this.hasMoreEdges = hasMoreEdges;
    }

    public boolean hasBeenRendered() {
        return rendered;
    }

    public void setRendered(boolean rendered) {
        this.rendered = rendered;
    }

    public String getIdType() {
        return idType;
    }

    public Map<String, List<String>> getAlternativeIds() {
        return alternativeIds;
    }

    public static void addAlternativeId(Map<String, List<String>> alternativeIds, String type, String id) {
        List<String> idList = alternativeIds.get(type);
        if(idList == null) {
            idList = new ArrayList<>();
            alternativeIds.put(type, idList);
        }
        idList.add(id);
    }
}
