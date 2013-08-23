package org.broadinstitute.gpinformatics.mercury.boundary.graph;

import java.io.Serializable;

/**
 * Represents an edge in a graph, for use as DTO for graphing clients.  Intended to be abstract enough to be used
 * for more than Transfer Visualization.
 */
public class Edge implements Serializable {
    private static final long serialVersionUID = 20101015L;

    public enum LineType {
        SOLID,
        DASHED
    }

    private final String label;
    private final Vertex source;
    private final Vertex destination;
    private final LineType lineType;

    private boolean rendered;

    public Edge(String label, Vertex source, Vertex destination) {
        this(label, source, destination, LineType.SOLID);
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

    public void markRendered() {
        rendered = true;
    }

    public LineType getLineType() {
        return lineType;
    }
}
