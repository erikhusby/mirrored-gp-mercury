package org.broadinstitute.gpinformatics.mercury.boundary.graph;

import java.io.Serializable;

/**
 * Represents an edge in a graph, for use as DTO for graphing clients.  Intended to be abstract enough to be used
 * for more than Transfer Visualization.
 */
public class Edge implements Serializable {
    private static final long serialVersionUID = 20101015L;

    public static enum LineType {
        SOLID,
        DASHED
    }

    private String label;
    private Vertex source;
    private Vertex destination;
    private boolean rendered = false;
    private LineType lineType = LineType.SOLID;

    public Edge(String label, Vertex source, Vertex destination) {
        this.label = label;
        this.source = source;
        this.destination = destination;
    }

    public Edge(String label, Vertex source, Vertex destination, LineType lineType) {
        this.label = label;
        this.source = source;
        this.destination = destination;
        this.lineType = lineType;
    }

    public String getLabel() {
        return label;
    }

    public Vertex getSource() {
        return source;
    }

    public Vertex getDestination() {
        return destination;
    }

    public boolean hasBeenRendered() {
        return rendered;
    }

    public void setRendered(boolean rendered) {
        this.rendered = rendered;
    }

    public LineType getLineType() {
        return lineType;
    }
}
