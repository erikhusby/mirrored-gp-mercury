package org.broadinstitute.gpinformatics.mercury.presentation.transfervis;

import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.layout.mxEdgeLabelLayout;
import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.util.mxConstants;
import com.mxgraph.view.mxGraph;
import org.broadinstitute.gpinformatics.mercury.boundary.graph.Edge;
import org.broadinstitute.gpinformatics.mercury.boundary.graph.Graph;
import org.broadinstitute.gpinformatics.mercury.boundary.graph.Vertex;
import org.broadinstitute.gpinformatics.mercury.boundary.transfervis.TransferEntityGrapher;
import org.broadinstitute.gpinformatics.mercury.boundary.transfervis.TransferVisualizer;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

/*
import javax.jnlp.ClipboardService;
import javax.jnlp.ServiceManager;
import javax.jnlp.UnavailableServiceException;
*/

/**
 * For a given tube or plate, adds edges and vertices to a JGraph component, to show transfer history.
 */
public class TransferVisualizerClient {
    public static final String JNLP_SERVER_ADDRESS = "jnlp.serverAddress";
    public static final String JNLP_RMI_PORT = "jnlp.rmi.port";
    /**
     * Graph from server.
     */
    private Graph graph;
    /**
     * JGraph widget
     */
    private mxGraph mxGraph;
    /**
     * The barcode the user entered, highlit in yellow
     */
    private String highlightBarcode;
    /**
     * The vertex that matches the barcode entered by the user, scroll to it after search
     */
    private mxCell highlightVertex;
    /**
     * Random access to vertices, for edge sources and destinations
     */
    private Map<String, mxCell> mapIdToMxVertex = new HashMap<>();
    /**
     * List of alternative ID types to display in each vertex
     */
    private List<TransferVisualizer.AlternativeId> alternativeDisplayIds = new ArrayList<>();
    private static final int BUTTON_HEIGHT = 20;

    public mxGraph getMxGraph() {
        return mxGraph;
    }

    /**
     * Which entity the user wants to search on
     */
    public enum SearchType {
        SEARCH_TUBE("Tube Barcode"),
        SEARCH_CONTAINER("Container Barcode");
//        SEARCH_GSSR_SAMPLE("GSSR Sample");

        private final String displayName;

        SearchType(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return this.displayName;
        }
    }

    public TransferVisualizerClient(String highlightBarcode,
                                    List<TransferVisualizer.AlternativeId> alternativeDisplayIds) {
        createMxGraph();
        this.highlightBarcode = highlightBarcode;
        this.alternativeDisplayIds = alternativeDisplayIds;
    }

    /**
     * Implemented by vertices that do something when the user clicks on them
     */
    public interface HandlesClicks {
        String handleClick();

        boolean scrollTo();

        boolean deleteCell();
    }

    /**
     * Implemented by vertices that do something when the user right-clicks on them
     */
    public interface HandlesPopups {
        List<String> getPopupList();

        String handlePopup(String name);
    }

    public static class CellValue implements HandlesClicks, HandlesPopups, Serializable {
        private static final String COPY_BARCODE = "Copy barcode";
        private static final String COPY_TUBE_BARCODES = "Copy tube barcodes";

        private Vertex vertex;
        private static final long serialVersionUID = 20101104L;

        public CellValue(Vertex vertex) {
            this.vertex = vertex;
        }

        @Override
        public String handleClick() {
            String message = null;
            TransferVisualizer server = getServer();
            try {
                String tubeBarcode = null;
                // todo jmt fold this into the enum
                if (vertex.getIdType().equals(TransferVisualizer.IdType.TUBE_IN_RACK_ID_TYPE.toString())) {
                    tubeBarcode = vertex.getTitle();
                } else if (vertex.getIdType().equals(TransferVisualizer.IdType.RECEPTACLE_ID_TYPE.toString())) {
                    tubeBarcode = vertex.getId();
                }
                if (tubeBarcode != null) {
                    StringBuilder detailsBuilder = new StringBuilder();
                    Map<String, List<String>> idsForTube = server.getIdsForTube(tubeBarcode);
                    for (Map.Entry<String, List<String>> stringListEntry : idsForTube.entrySet()) {
                        List<String> ids = stringListEntry.getValue();
                        for (String id : ids) {
                            detailsBuilder.append(stringListEntry.getKey());
                            detailsBuilder.append(": ");
                            detailsBuilder.append(id);
                            detailsBuilder.append("\n");
                        }
                    }
                    message = detailsBuilder.toString();
                }
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
            return message;
        }

        @Override
        public boolean scrollTo() {
            return false;
        }

        @Override
        public boolean deleteCell() {
            return false;
        }

        @Override
        public List<String> getPopupList() {
            List<String> popupList = new ArrayList<>();
            popupList.add(CellValue.COPY_BARCODE);
            if (vertex.getChildVertices() != null) {
                popupList.add(CellValue.COPY_TUBE_BARCODES);
            }
            return popupList;
        }

        @Override
        public String handlePopup(String name) {
            StringSelection contents;
            switch (name) {
            case CellValue.COPY_BARCODE:
                contents = new StringSelection(vertex.getTitle());
                break;
            case CellValue.COPY_TUBE_BARCODES:
                StringBuilder tubeBarcodes = new StringBuilder();
                for (Vertex[] rows : vertex.getChildVertices()) {
                    for (Vertex column : rows) {
                        if (column != null) {
                            tubeBarcodes.append(column.getTitle());
                            tubeBarcodes.append(" ");
                        }
                    }
                }
                contents = new StringSelection(tubeBarcodes.toString());
                break;
            default:
                throw new RuntimeException("Unknown popup " + name);
            }

/*
            todo jmt fix reference to javaws.jar on Linux
            try {
                ClipboardService clipboardService = (ClipboardService) ServiceManager.lookup("javax.jnlp.ClipboardService");
                clipboardService.setContents(contents);
            } catch (UnavailableServiceException e) {
*/
            // Assume we're running as a non-JNLP application
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(contents, contents);
/*
            }
*/

            return null;
        }

        @Override
        public String toString() {
            String title;
            if (vertex.getAlternativeIds().isEmpty()) {
                title = vertex.getTitle();
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("<html>");
                stringBuilder.append(vertex.getTitle());
                stringBuilder.append("<br/>");
                for (Map.Entry<String, List<String>> entry : vertex.getAlternativeIds().entrySet()) {
                    for (String id : entry.getValue()) {
                        stringBuilder.append(id);
                        stringBuilder.append("<br/>");
                    }
                }
                stringBuilder.append("</html>");
                title = stringBuilder.toString();
            }
            return title;
        }
    }

    /**
     * Contains a port for attaching edges.  Edges attached directly to child cells don't get laid out correctly.
     */
    public static class ChildCellValue extends CellValue {
        private mxCell port;
        private static final long serialVersionUID = 20101104L;

        public ChildCellValue(mxCell port, Vertex vertex) {
            super(vertex);
            this.port = port;
        }

        public mxCell getPort() {
            return this.port;
        }
    }

    /**
     * A box that the user can click, to see more details about a rack or plate
     */
    public static class MoreDetails implements HandlesClicks {

        private Vertex vertex;

        public MoreDetails(Vertex vertex) {
            this.vertex = vertex;
        }

        @Override
        public String toString() {
            return "More details";
        }

        @Override
        public String handleClick() {
            StringBuilder detailsBuilder = new StringBuilder();
            for (String detail : vertex.getDetails()) {
                detailsBuilder.append(detail);
                detailsBuilder.append("\n");
            }
            return detailsBuilder.toString();
        }

        @Override
        public boolean scrollTo() {
            return false;
        }

        @Override
        public boolean deleteCell() {
            return false;
        }
    }

    /**
     * A box that the user can click to see more transfers
     */
    public class MoreTransfers implements HandlesClicks {

        private Vertex vertex;

        public MoreTransfers(Vertex vertex) {
            this.vertex = vertex;
        }

        @Override
        public String toString() {
            return "More transfers";
        }

        @Override
        public String handleClick() {
            try {
                TransferVisualizerClient.this.graph = getServer().expandVertex(
                        TransferVisualizerClient.this.graph, vertex.getId(), vertex.getIdType(), alternativeDisplayIds);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
            mxGraph.getModel().beginUpdate();
            try {
                renderGraph();
                layout();
            } finally {
                mxGraph.getModel().endUpdate();
            }
            return null;
        }

        @Override
        public boolean scrollTo() {
            return true;
        }

        @Override
        public boolean deleteCell() {
            // remove "more transfers" box
            return true;
        }
    }

    public void renderGraph() {
        for (Vertex vertex : graph.getMapIdToVertex().values()) {
            if (vertex.getParentVertex() == null) {
                renderVertex(vertex);
            }
        }
        for (Edge edge : graph.getMapIdToEdge().values()) {
            renderEdge(edge);
        }
    }

    private void renderEdge(Edge edge) {
        if (!edge.hasBeenRendered()) {
            mxCell sourceReceptacleCell = getReceptacleCell(edge.getSource().getId());
            mxCell destinationReceptacleCell = getReceptacleCell(edge.getDestination().getId());

            sourceReceptacleCell = updateToPort(sourceReceptacleCell);
            destinationReceptacleCell = updateToPort(destinationReceptacleCell);

            String label = edge.getLabel();
            // If the edge has both ends in child cells (e.g. cherry picks)
            if (!sourceReceptacleCell.getParent().equals(mxGraph.getDefaultParent()) && !destinationReceptacleCell
                    .getParent().equals(mxGraph.getDefaultParent())) {
                int sourceParentEdgeCount = sourceReceptacleCell.getParent().getEdgeCount();

                // If we find another edge to the same destination parent, and it already has a label, don't label the
                // current edge, because it would be very cluttered if all edges where labeled
                for (int i = 0; i < sourceParentEdgeCount; i++) {
                    mxCell edgeCell = (mxCell) sourceReceptacleCell.getParent().getEdgeAt(i);
                    if (edgeCell.getTarget().equals(destinationReceptacleCell.getParent())
                        && edgeCell.getValue() != null) {
                        label = null;
                        break;
                    }
                }
            }

            mxGraph.insertEdge(mxGraph.getDefaultParent(), null,
                    label,
                    sourceReceptacleCell,
                    destinationReceptacleCell,
                    edge.getLineType() == Edge.LineType.DASHED ? mxConstants.STYLE_DASHED + "=1" : "");

            edge.markRendered();
        }
    }

    private mxCell updateToPort(mxCell cell) {
        if (cell.getValue() instanceof ChildCellValue) {
            return ((ChildCellValue) cell.getValue()).getPort();
        }

        return cell;
    }

    private mxCell getReceptacleCell(String edgeReceptacleId) {
        mxCell receptacleCell = mapIdToMxVertex.get(edgeReceptacleId);
        if (receptacleCell == null) {
            renderVertex(graph.getMapIdToVertex().get(edgeReceptacleId));
            receptacleCell = mapIdToMxVertex.get(edgeReceptacleId);
        }

        return receptacleCell;
    }

    /**
     * Insert a vertex for a plate, or a rack and tubes
     *
     * @param vertex barcode and transfers
     */
    void renderVertex(Vertex vertex) {
        if (!vertex.hasBeenRendered()) {
            vertex.setRendered(true);
            // the layout algorithm will set x and y later
            // todo jmt highlight GSSR barcodes and library names
            mxCell mxVertex = (mxCell) mxGraph.insertVertex(mxGraph.getDefaultParent(), null,
                    new CellValue(vertex), 0.0, 0.0, 160.0, 40.0,
                    (vertex.getTitle().contains(highlightBarcode) ? "fillColor=#FFFF00;fontColor=#000000;" :
                            "fillColor=#FFFFFF;fontColor=#000000;") +
                    mxConstants.STYLE_VERTICAL_ALIGN + "=" + mxConstants.ALIGN_TOP +
                    mxConstants.STYLE_VERTICAL_LABEL_POSITION + "=" + mxConstants.ALIGN_TOP);
            mxVertex.setConnectable(false);
            mxGraph.updateCellSize(mxVertex);
            if (vertex.getTitle().contains(highlightBarcode)) {
                highlightVertex = mxVertex;
            }

            double rackHeight = mxVertex.getGeometry().getHeight();
            if (vertex.getChildVertices() != null) {
                mxCell[][] childCells = insertChildCells(vertex, mxVertex);
                double maxChildCellHeight = arrangeChildCells(vertex, mxVertex, rackHeight, childCells);
                rackHeight += maxChildCellHeight * (double) childCells.length;
            }

            // If there are self-referential plate events, draw a box for the user to click on, to see a list of these events
            if (vertex.getDetails() != null && !vertex.getDetails().isEmpty()) {
                mxGraph.insertVertex(
                        mxVertex,
                        null,
                        new MoreDetails(vertex),
                        0.0, rackHeight, mxVertex.getGeometry().getWidth(), (double) BUTTON_HEIGHT);
                rackHeight += (double) BUTTON_HEIGHT;
            }

            // If there are more transfers, draw a box for the user to click on, to see the transfers
            if (vertex.getHasMoreEdges()) {
                mxGraph.insertVertex(
                        mxVertex,
                        null,
                        new MoreTransfers(vertex),
                        0.0, rackHeight, mxVertex.getGeometry().getWidth(), (double) BUTTON_HEIGHT);
                rackHeight += (double) BUTTON_HEIGHT;
            }
            mxVertex.getGeometry().setHeight(rackHeight);

            mapIdToMxVertex.put(vertex.getId(), mxVertex);
        }
    }

    /**
     * Insert child cells, and resize, so they fit their titles
     *
     * @param vertex   rack
     * @param mxVertex JGraph vertex
     *
     * @return array of JGraph child cells
     */
    private mxCell[][] insertChildCells(Vertex vertex, mxCell mxVertex) {
        int numChildRows = vertex.getChildVertices().length;
        mxCell[][] childCells = new mxCell[vertex.getChildVertices().length][vertex.getChildVertices()[0].length];
        for (int rowIndex = 0; rowIndex < numChildRows; rowIndex++) {
            Vertex[] row = vertex.getChildVertices()[rowIndex];
            for (int columnIndex = 0; columnIndex < row.length; columnIndex++) {
                Vertex childVertex = row[columnIndex];
                if (childVertex != null) {
                    // The layout algorithm doesn't work when routing an edge between a nested cell (tube in
                    // a rack) and a non-nested cell (free-standing tube), so create ports on the rack, to
                    // which edges can be connected.  We'll set x and y later.
                    mxGeometry geometry = new mxGeometry(0.0, 0.0, 4.0, 4.0);
                    geometry.setRelative(true);
                    mxCell port = new mxCell(null, geometry, "shape=ellipse");
                    port.setVertex(true);
                    mxGraph.addCell(port, mxVertex);

                    // We'll set x and y later
                    mxCell childMxVertex = (mxCell) mxGraph.insertVertex(mxVertex, null,
                            new ChildCellValue(port, childVertex),
                            0.0, 0.0, 0.0, 0.0,
                            childVertex.getTitle().contains(highlightBarcode) ? "fillColor=#FFFF00;fontColor=#000000" :
                                    "fillColor=#FFFFFF;fontColor=#000000");
                    childMxVertex.setConnectable(false);
                    childCells[rowIndex][columnIndex] = childMxVertex;
                    mxGraph.updateCellSize(childMxVertex);
                    if (childVertex.getTitle().contains(highlightBarcode)) {
                        highlightVertex = childMxVertex;
                    }

                    mapIdToMxVertex.put(childVertex.getId(), childMxVertex);
                }
            }
        }
        return childCells;
    }

    /**
     * arrange the resized cells in uniform rows and columns
     *
     * @param vertex           rack
     * @param mxVertex         JGraph vertex
     * @param rackHeaderHeight the height of the rack title bar
     * @param childCells       array of JGraph child vertices
     *
     * @return the height of the tallest child cell
     */
    private double arrangeChildCells(Vertex vertex, mxCell mxVertex, double rackHeaderHeight, mxCell[][] childCells) {
        int numColumns = vertex.getChildVertices()[0].length;
        double maxChildCellHeight = 0.0;
        double maxChildCellWidth = 0.0;
        for (mxCell[] row : childCells) {
            for (mxCell cell : row) {
                if (cell != null) {
                    maxChildCellWidth = Math.max(cell.getGeometry().getWidth(), maxChildCellWidth);
                    maxChildCellHeight = Math.max(cell.getGeometry().getHeight(), maxChildCellHeight);
                }
            }
        }

        double totalCellsHeight = maxChildCellHeight * (double) childCells.length;
        double totalY = rackHeaderHeight + totalCellsHeight;

        if (vertex.getDetails() != null && !vertex.getDetails().isEmpty()) {
            totalY += (double) BUTTON_HEIGHT;
        }

        if (vertex.getHasMoreEdges()) {
            totalY += (double) BUTTON_HEIGHT;
        }

        for (int rowIndex = 0; rowIndex < childCells.length; rowIndex++) {
            for (int columnIndex = 0; columnIndex < childCells[0].length; columnIndex++) {
                mxCell mxCell = childCells[rowIndex][columnIndex];
                if (mxCell != null) {
                    mxGeometry mxGeometry = mxCell.getGeometry();
                    mxGeometry.setHeight(maxChildCellHeight);
                    mxGeometry.setWidth(maxChildCellWidth);
                    mxGeometry.setX(maxChildCellWidth * (double) columnIndex);
                    mxGeometry.setY(rackHeaderHeight + (maxChildCellHeight * (double) rowIndex));

                    // Move the edge attachment port to the left of the cell
                    mxGeometry portGeometry = ((ChildCellValue) mxCell.getValue()).getPort().getGeometry();
                    portGeometry.setX((maxChildCellWidth * (double) columnIndex) / (maxChildCellWidth
                                                                                    * (double) numColumns));
                    double yRatio = (maxChildCellHeight * (double) rowIndex) / totalCellsHeight;
                    portGeometry.setY((yRatio * totalCellsHeight / totalY) + (rackHeaderHeight / totalY));
                }
            }
        }
        mxVertex.getGeometry()
                .setWidth(Math.max(maxChildCellWidth * (double) numColumns, mxVertex.getGeometry().getWidth()));
        return maxChildCellHeight;
    }

    /**
     * Layout the vertices and edges
     */
    public void layout() {
        mxHierarchicalLayout hierarchicalLayout = new mxHierarchicalLayout(mxGraph);
        // todo jmt make these user-adjustable
        hierarchicalLayout.setIntraCellSpacing(80.0);
        hierarchicalLayout.setInterRankCellSpacing(80.0);
        hierarchicalLayout.execute(mxGraph.getDefaultParent());
        mxEdgeLabelLayout edgeLabelLayout = new mxEdgeLabelLayout(mxGraph);
        edgeLabelLayout.execute(mxGraph.getDefaultParent());
    }


    public mxCell getHighlightVertex() {
        return highlightVertex;
    }

    private static TransferVisualizer getServer() {
        try {
            final Hashtable jndiProperties = new Hashtable();
            jndiProperties.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
            final Context context = new InitialContext(jndiProperties);
            // The app name is the application name of the deployed EJBs. This is typically the ear name
            // without the .ear suffix. However, the application name could be overridden in the application.xml of the
            // EJB deployment on the server.
            // Since we haven't deployed the application as a .ear, the app name for us will be an empty string
            final String appName = "";
            // This is the module name of the deployed EJBs on the server. This is typically the jar name of the
            // EJB deployment, without the .jar suffix, but can be overridden via the ejb-jar.xml
            // In this example, we have deployed the EJBs in a jboss-as-ejb-remote-app.jar, so the module name is
            // jboss-as-ejb-remote-app
            // todo jmt change web.xml to make this fixed, or fetch it from BuildInfoBean
            final String moduleName = "Mercury-1.65-SNAPSHOT";
//            final String moduleName = "Mercury-Arquillian";
            // AS7 allows each deployment to have an (optional) distinct name. We haven't specified a distinct name for
            // our EJB deployment, so this is an empty string
            final String distinctName = "";
            // The EJB name which by default is the simple class name of the bean implementation class
            final String beanName = TransferEntityGrapher.class.getSimpleName();
            // the remote view fully qualified class name
            final String viewClassName = TransferVisualizer.class.getName();
            // let's do the lookup (notice the ?stateful string as the last part of the jndi name for stateful bean lookup)
            String name =
                    "ejb:" + appName + "/" + moduleName + "/" + distinctName + "/" + beanName + "!" + viewClassName
                    + "?stateful";
            return (TransferVisualizer) context.lookup(name);
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    public void renderAndLayoutGraph(Graph graph) {
        this.graph = graph;
        mxGraph.getModel().beginUpdate();
        try {
            mxGraph.selectAll();
            mxGraph.removeCells();
        } finally {
            mxGraph.getModel().endUpdate();
        }
        mxGraph.getModel().beginUpdate();
        try {
            renderGraph();
            layout();
        } finally {
            mxGraph.getModel().endUpdate();
        }
    }

    public Graph fetchGraph(String barcode, SearchType searchType,
                            List<TransferVisualizer.AlternativeId> alternativeIds) throws RemoteException {
        TransferVisualizer transferVisualizer = getServer();
        // todo jmt fold this code into the enum
        switch (searchType) {
        case SEARCH_TUBE:
            graph = transferVisualizer.forTube(barcode, alternativeIds);
            break;
        case SEARCH_CONTAINER:
            graph = transferVisualizer.forContainer(barcode, alternativeIds);
            break;
//            case SEARCH_GSSR_SAMPLE:
//                graph = transferVisualizer.forGssrBarcode(barcode, alternativeIds);
//                break;
        default:
            throw new RuntimeException("Unknown searchType " + searchType);
        }
        return graph;
    }

    private void createMxGraph() {
        mxGraph = new mxGraph() {
            /** Enable layout of edges attached to ports */
            @Override
            public boolean isPort(Object cell) {
                mxGeometry geo = getCellGeometry(cell);

                return (geo != null) && geo.isRelative();
            }

            /** Don't allow the user to move nested cells around */
            @Override
            public boolean isCellSelectable(Object cellObject) {
                mxCell cell = (mxCell) cellObject;
                return cell.getParent().equals(this.getDefaultParent());
            }
        };
        // Some labels use <br/>
        mxGraph.setHtmlLabels(true);
        // Don't allow the user to drag edges away from vertices
        mxGraph.setCellsDisconnectable(false);
    }

    /**
     * For testing only.
     */
    public Graph getGraph() {
        return graph;
    }

    /**
     * For testing only.
     */
    public void setGraph(Graph graph) {
        this.graph = graph;
    }
}
