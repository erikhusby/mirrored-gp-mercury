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

    private String id;
    private String idType;
    private String title;
    private List<String> details = new ArrayList<>();
    private Map<String, List<String>> alternativeIds = new HashMap<>();
    private Vertex[][] childVertices;
    private Vertex parentVertex;
    private boolean hasMoreEdges;
    private boolean rendered = false;

    public Vertex(String id, String idType, String title) {
        this.id = id;
        this.idType = idType;
        this.title = title;
    }

    public Vertex(String id, String idType, String title, int numChildRows, int numChildColumns) {
        this.id = id;
        this.idType = idType;
        this.title = title;
        this.childVertices = new Vertex[numChildRows][numChildColumns];
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

    public void setParentVertex(Vertex parentVertex) {
        this.parentVertex = parentVertex;
    }

    @Override
    public String toString() {
        return this.title;
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

    public void setAlternativeIds(Map<String, List<String>> alternativeIds) {
        this.alternativeIds = alternativeIds;
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
