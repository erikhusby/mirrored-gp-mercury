package org.broadinstitute.gpinformatics.mercury.boundary.transfervis;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.mercury.boundary.graph.Edge;
import org.broadinstitute.gpinformatics.mercury.boundary.graph.Graph;
import org.broadinstitute.gpinformatics.mercury.boundary.graph.Vertex;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.StaticPlateDAO;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.TwoDBarcodedTubeDAO;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.CherryPickTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.SectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.PlateWell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainerEmbedder;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselGeometry;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import javax.ejb.Remote;
import javax.ejb.Stateful;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * From one of various starting entities (plate, tube etc.), traverse transfers breadth-first, create a graph of
 * entities and transfers, until reach a limit on the amount of work in one invocation.  The user can
 * click "More Transfers" to expand part of the graph.
 */
@Stateful
@Remote(TransferVisualizer.class)
public class TransferEntityGrapher implements TransferVisualizer {

    private int maxNumVesselsPerRequest = 1000;

    @Inject
    private StaticPlateDAO staticPlateDAO;

    @Inject
    private TwoDBarcodedTubeDAO twoDBarcodedTubeDAO;

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private BSPUserList bspUserList;

    /**
     * Handles the "More Transfers" button
     */
    private interface ExpandVertex {
        void expandVertex(String vertexId, Graph graph,
                TransferEntityGrapher transferEntityGrapher, List<AlternativeId> alternativeIds);
    }

    private final Map<IdType, ExpandVertex> mapIdTypeToExpandVertex = new HashMap<IdType, ExpandVertex>();

    void initMap() {
        mapIdTypeToExpandVertex.put(IdType.PLATE_ID_TYPE, new ExpandVertex() {
            @Override
            public void expandVertex(String vertexId, Graph graph,
                    TransferEntityGrapher transferEntityGrapher, List<AlternativeId> alternativeIds) {
                StaticPlate plate = staticPlateDAO.findByBarcode(vertexId);
                transferEntityGrapher.startWithPlate(plate, graph, alternativeIds);
            }
        });
        mapIdTypeToExpandVertex.put(IdType.CONTAINER_ID_TYPE, new ExpandVertex() {
            @Override
            public void expandVertex(String vertexId, Graph graph,
                    TransferEntityGrapher transferEntityGrapher, List<AlternativeId> alternativeIds) {
                LabVessel labVessel = labVesselDao.findByIdentifier(vertexId);
                transferEntityGrapher.startWithContainer(labVessel.getContainerRole(), graph, alternativeIds);
            }
        });
        mapIdTypeToExpandVertex.put(IdType.TUBE_IN_RACK_ID_TYPE, null); // can't be expanded
        mapIdTypeToExpandVertex.put(IdType.RECEPTACLE_ID_TYPE, new ExpandVertex() {
            @Override
            public void expandVertex(String vertexId, Graph graph,
                    TransferEntityGrapher transferEntityGrapher, List<AlternativeId> alternativeIds) {
                TwoDBarcodedTube receptacle = twoDBarcodedTubeDAO.findByBarcode(vertexId);
                transferEntityGrapher.startWithTube(receptacle, graph, alternativeIds);
            }
        });
    }

    /**
     * Build a graph, staring with a plate barcode
     * @param plateBarcode plate the user is searching on
     * @param alternativeIds which IDs to include in the vertices
     * @return vertices and edges
     */
    @Override
    public Graph forPlate(String plateBarcode, List<AlternativeId> alternativeIds) {
        Graph graph = new Graph();
        StaticPlate plate = staticPlateDAO.findByBarcode(plateBarcode);
        startWithPlate(plate, graph, alternativeIds);
        return graph;
    }

    @Override
    public Graph forContainer(String containerBarcode, List<AlternativeId> alternativeIds) {
        Graph graph = new Graph();
        LabVessel labVessel = labVesselDao.findByIdentifier(containerBarcode);
        if(labVessel == null) {
            graph.setMessage("No container was found with that barcode");
        } else if(OrmUtil.proxySafeIsInstance(labVessel, VesselContainerEmbedder.class)) {
            startWithContainer(labVessel.getContainerRole(), graph, alternativeIds);
        } else {
            graph.setMessage("The barcode is not attached to a container");
        }
        return graph;
    }

    private void startWithPlate(StaticPlate plate, Graph graph, List<AlternativeId> alternativeIds) {
        if (plate == null) {
            graph.setMessage("No plate was found with that barcode");
        } else {
//            if (plate.getPlatePhysicalType().getName().equals(PlatePhysicalType.TUBE_RACK)) {
//                graph.setMessage("The barcode is attached to a rack, please enter a plate barcode");
//            } else {
                Queue<VesselVertex> vesselVertexQueue = new LinkedList<VesselVertex>();

                vesselVertexQueue.add(new PlateVesselVertex(plate));
                processQueue(vesselVertexQueue, graph, alternativeIds);
//            }
        }
    }

//    private void startWithWellMap(WellMap wellMap, Graph graph, List<AlternativeId> alternativeIds) {
//        Queue<VesselVertex> vesselQueue = new LinkedList<VesselVertex>();
//
//        vesselQueue.add(new ContainerVertex(wellMap));
//        processQueue(vesselQueue, graph, alternativeIds);
//    }

    /**
     * Build a graph, starting with a tube barcode
     * @param tubeBarcode tube the user is searching on
     * @param alternativeIds which IDs to include in the vertices
     * @return vertices and edges
     */
    @Override
    public Graph forTube(String tubeBarcode, List<AlternativeId> alternativeIds) {
        Graph graph = new Graph();
        TwoDBarcodedTube receptacle = twoDBarcodedTubeDAO.findByBarcode(tubeBarcode);
        startWithTube(receptacle, graph, alternativeIds);
        return graph;
    }

    /**
     * Starting point for rendering receptacles, searched by barcode, GSSR sample, library name
     * @param graph empty
     * @param receptacle starting point for filling graph
     * @param alternativeIds IDs to display
     */
    public void startWithTube(TwoDBarcodedTube receptacle, Graph graph, List<AlternativeId> alternativeIds) {
        if (receptacle == null) {
            graph.setMessage("No tube was found with that barcode");
        } else {
            Queue<VesselVertex> vesselVertexQueue = new LinkedList<VesselVertex>();

            for (VesselContainer vesselContainer : receptacle.getContainers()) {
                vesselVertexQueue.add(new ContainerVertex(vesselContainer));
            }
            vesselVertexQueue.add(new ReceptacleVesselVertex(receptacle));
            processQueue(vesselVertexQueue, graph, alternativeIds);
        }
    }

    public void startWithContainer(VesselContainer vesselContainer, Graph graph, List<AlternativeId> alternativeIds) {
        if (vesselContainer == null) {
            graph.setMessage("No container was found with that barcode");
        } else {
            Queue<VesselVertex> vesselVertexQueue = new LinkedList<VesselVertex>();

            vesselVertexQueue.add(new ContainerVertex(vesselContainer));
            processQueue(vesselVertexQueue, graph, alternativeIds);
        }
    }

    /**
     * Build a graph, starting with a GSSR sample barcode
     * @param gssrBarcode GSSR sample the user is searching on
     * @param alternativeIds which IDs to include in the vertices
     * @return vertices and edges
     */
/*
    @Override
    public Graph forGssrBarcode(String gssrBarcode, List<AlternativeId> alternativeIds) throws RemoteException {
        Graph graph = new Graph();
        EntityManager entityManager = createEntityManager();
        try {
            LcSample lcSample = LcSample.findByBarcode(gssrBarcode, entityManager);
            if(lcSample == null) {
                graph.setMessage("No GSSR Sample was found with that barcode");
            } else {
                // todo jmt go through NGLD?
                String receptacleBarcode = lcSample.getSampleReceptacle().getFactoryBarcode();
                Receptacle receptacle = Receptacle.findByBarcode(entityManager, receptacleBarcode);
                startWithTube(receptacle, graph, alternativeIds);
            }
            return graph;
        } finally {
            entityManager.close();
        }
    }
*/

    /**
     * Build a graph, starting with a library name
     * @param libraryName name of library the user is searching on
     * @param alternativeIds which IDs to include in the vertices
     * @return vertices and edges
     */
/*
    @Override
    public Graph forLibraryName(String libraryName, List<AlternativeId> alternativeIds) throws RemoteException {
        Graph graph = new Graph();
        EntityManager entityManager = createEntityManager();
        try {
            AbstractSeqContent seqContent = AbstractSeqContent.findSingleContentByName(entityManager, libraryName);
            if(seqContent == null) {
                graph.setMessage("No library was found with that name");
            } else {
                Receptacle receptacle = Receptacle.findByBarcode(entityManager, seqContent.getReceptacleBarcode());
                startWithTube(receptacle, graph, alternativeIds);
            }
            return graph;
        } finally {
            entityManager.close();
        }
    }
*/

    /**
     * Called when the user clicks the "More Transfers" button
     *
     * @param graph          graph holding vertex to expand
     * @param vertexId       id of vertex to expand
     * @param idType         plate, PLATE_ID_TYPE, WELL_MAP_ID_TYPE etc.
     * @param alternativeIds which IDs to include in the vertices
     * @return vertices and edges
     */
    @Override
    public Graph expandVertex(Graph graph, String vertexId, String idType, List<AlternativeId> alternativeIds) {
        initMap();
        ExpandVertex expandVertex = mapIdTypeToExpandVertex.get(Enum.valueOf(IdType.class, idType));
        expandVertex.expandVertex(vertexId, graph, this, alternativeIds);
        return graph;
    }

    /**
     * Called when the user clicks a tube
     * @param tubeBarcode barcode of the tube the user is searching on
     * @return map from AlternativeId displayName to list of IDs
     */
    @Override
    public Map<String, List<String>> getIdsForTube(String tubeBarcode) {
        try {
            TwoDBarcodedTube receptacle = twoDBarcodedTubeDAO.findByBarcode(tubeBarcode);
            return getAlternativeIds(receptacle, Arrays.asList(AlternativeId.values()));
        } finally {
        }
    }

    /**
     * Builds a label for an edge, with event name etc.
     *
     * @param stationEvent holds details of the event
     * @return HTML
     */
    private String buildEdgeLabel(LabEvent stationEvent) {
        StringBuilder label = new StringBuilder();
        label.append("<html>");
        String eventTypeName = stationEvent.getLabEventType().getName();
//        if (!ReceptacleTransferEvent.TRANSFER_EVENT.equals(eventTypeName)) {
            label.append(eventTypeName);
//            if (stationEvent instanceof PlateTransferEvent) {
//                PlateTransferEvent plateTransferEvent = (PlateTransferEvent) stationEvent;
//                label.append("<br/>");
//                label.append(plateTransferEvent.getSourceSectionLayout());
//                label.append(" to ");
//                label.append(plateTransferEvent.getSectionLayout());
//            }
            label.append("<br/>");
//        }
        String labMachineName = stationEvent.getEventLocation();
        if (!labMachineName.contains("Unknown")) {
            label.append(labMachineName).append(", ");
        }
        label.append(stationEvent.getEventDate());
        label.append("<br/>");
        if (bspUserList != null) {
            BspUser bspUser = bspUserList.getById(stationEvent.getEventOperator());
            if (bspUser != null) {
                label.append(bspUser.getFullName());
            }
        }
        label.append("</html>");
        return label.toString();
    }

    private abstract static class VesselVertex {

        private Vertex vertex;

        /**
         * Insert the vessel as a vertex in the graph, unless it has already been rendered
         *
         * @param graph          existing edges and vertices
         * @param alternativeIds IDs the user wants rendered
         * @return true if the vertex is new, false if it was already in the graph
         */
        abstract boolean render(Graph graph, List<AlternativeId> alternativeIds);

        /**
         * Insert the vessel's transfers as edges in the graph.  For each transfer, add to the queue the vessel at
         * the opposite end of the transfer.
         *
         * @param graph          existing edges and vertices
         * @param vesselVertexQueue    queue to which to add (new) vessels at opposite end of each transfer (vessel is not
         *                       added if it's already in the graph)
         * @param alternativeIds IDs the user wants rendered
         * @return number of vessels added to the queue
         */
        public abstract int renderEdges(Graph graph, Queue<VesselVertex> vesselVertexQueue, List<AlternativeId> alternativeIds);

        public Vertex getVertex() {
            return vertex;
        }

        public void setVertex(Vertex vertex) {
            this.vertex = vertex;
        }
    }


    private class PlateVesselVertex extends VesselVertex {

        private StaticPlate plate;

        private PlateVesselVertex(StaticPlate plate) {
            this.plate = plate;
        }

        @Override
        boolean render(Graph graph, List<AlternativeId> alternativeIds) {
//            System.out.println("Rendering plate " + plate.getBarcode());
            boolean newVertex = false;
            Vertex plateVertex = graph.getMapIdToVertex().get(plate.getLabel());
            if (plateVertex == null) {
                newVertex = true;
                plateVertex = new Vertex(plate.getLabel(), IdType.PLATE_ID_TYPE.toString(),
                        new StringBuilder().append(plate.getPlateType().getDisplayName()).append(" : ").append(plate.getLabel()).toString());
                // If there are self-referential plate events, draw a box for the user to click on, to see a list of these events
                List<LabEvent> sortedPlateEvents = new ArrayList<LabEvent>();
                for (LabEvent plateEvent : plate.getInPlaceEvents()) {
                    sortedPlateEvents.add(plateEvent);
                }
                // Sort the plate events chronologically
                Collections.sort(sortedPlateEvents, new Comparator<LabEvent>() {
                    @Override
                    public int compare(LabEvent o1, LabEvent o2) {
                        return o1.getEventDate().compareTo(o2.getEventDate());
                    }
                });
                for (LabEvent plateEvent : sortedPlateEvents) {
                    BspUser bspUser;
                    if(bspUserList == null) {
                        bspUser = new BspUser();
                    } else {
                        bspUser = bspUserList.getById(plateEvent.getEventOperator());
                    }
                    plateVertex.getDetails().add(plateEvent.getLabEventType().getName() + ", " +
                            plateEvent.getEventLocation() + ", " + plateEvent.getEventDate() + ", " +
                            (bspUser == null ? "" : bspUser.getFullName()));
                }
                graph.getMapIdToVertex().put(plateVertex.getId(), plateVertex);
                plateVertex.setHasMoreEdges(true);
            }
            setVertex(plateVertex);
            return newVertex;
        }

        @Override
        public int renderEdges(Graph graph, Queue<VesselVertex> vesselVertexQueue, List<AlternativeId> alternativeIds) {
            int numVesselsAdded = 0;
            VesselContainer<PlateWell> containerRole = plate.getContainerRole();
            numVesselsAdded += renderSectionTransfers(graph, vesselVertexQueue, alternativeIds, containerRole.getSectionTransfersFrom(),
                    containerRole.getSectionTransfersTo());
            containerRole.getCherryPickTransfersFrom();
//            for (ReceptaclePlateTransferEvent receptaclePlateTransferEvent : plate.getReceptaclePlateTransferEvents()) {
//                ReceptacleVesselVertex receptacleVessel = new ReceptacleVesselVertex((Receptacle) receptaclePlateTransferEvent.getReceptacle());
//                if(receptacleVessel.render(graph, alternativeIds)) {
//                    vesselVertexQueue.add(receptacleVessel);
//                    numVesselsAdded++;
//                }
//                renderEdge(graph, receptaclePlateTransferEvent);
//            }
            return numVesselsAdded;
        }
    }

    private int renderReceptacleEdges(LabVessel receptacle, Graph graph, Queue<VesselVertex> vesselVertexQueue, List<AlternativeId> alternativeIds) {
        int numVesselsAddedReturn = 0;
//        for (ReceptacleTransferEvent receptacleTransferEvent : receptacle.getReceptacleTransferEventsThisAsSource()) {
//            Receptacle destinationReceptacle = (Receptacle) receptacleTransferEvent.getReceptacle();
//            if(destinationReceptacle.getWellMapEntries().isEmpty()) {
//                ReceptacleVesselVertex receptacleVessel = new ReceptacleVesselVertex(destinationReceptacle);
//                if (receptacleVessel.render(graph, alternativeIds)) {
//                    vesselVertexQueue.add(receptacleVessel);
//                    numVesselsAddedReturn++;
//                }
//            } else {
//                for (WellMapEntry wellMapEntry : destinationReceptacle.getWellMapEntries()) {
//                    ContainerVertex rackVessel = new ContainerVertex(wellMapEntry.getWellMap());
//                    if(rackVessel.render(graph, alternativeIds)) {
//                        vesselVertexQueue.add(rackVessel);
//                        numVesselsAddedReturn++;
//                    }
//                }
//            }
//            renderEdge(graph, receptacleTransferEvent);
//        }
//        for (ReceptaclePlateTransferEvent receptaclePlateTransferEvent : receptacle.getReceptaclePlateTransferEvents()) {
//            PlateVesselVertex plateVessel = new PlateVesselVertex(receptaclePlateTransferEvent.getPlate());
//            if(plateVessel.render(graph, alternativeIds)) {
//                vesselVertexQueue.add(plateVessel);
//                numVesselsAddedReturn++;
//            }
//            renderEdge(graph, receptaclePlateTransferEvent);
//        }
//        for (ReceptacleEvent receptacleEvent : receptacle.getReceptacleEvents()) {
//            if (receptacleEvent instanceof ReceptacleTransferEvent) {
//                ReceptacleTransferEvent receptacleTransferEvent = (ReceptacleTransferEvent) receptacleEvent;
//                Receptacle sourceReceptacle = (Receptacle) receptacleTransferEvent.getSourceReceptacle();
//                if(sourceReceptacle.getWellMapEntries().isEmpty()) {
//                    ReceptacleVesselVertex receptacleVessel = new ReceptacleVesselVertex(sourceReceptacle);
//                    if (receptacleVessel.render(graph, alternativeIds)) {
//                        vesselVertexQueue.add(receptacleVessel);
//                        numVesselsAddedReturn++;
//                    }
//                } else {
//                    for (WellMapEntry wellMapEntry : sourceReceptacle.getWellMapEntries()) {
//                        ContainerVertex rackVessel = new ContainerVertex(wellMapEntry.getWellMap());
//                        if(rackVessel.render(graph, alternativeIds)) {
//                            vesselVertexQueue.add(rackVessel);
//                            numVesselsAddedReturn++;
//                        }
//                    }
//                }
//                renderEdge(graph, receptacleTransferEvent);
//            }
//        }
        // if a receptacle appears in two different well maps, that's an implied re-array
        if(receptacle.getContainers().size() > 1) {
            Iterator<VesselContainer<?>> iterator = receptacle.getContainers().iterator();
            VesselContainer<?> sourceContainer = iterator.next();
            ContainerVertex sourceContainerVertex = new ContainerVertex(sourceContainer);
            if (sourceContainerVertex.render(graph, alternativeIds)) {
                vesselVertexQueue.add(sourceContainerVertex);
                numVesselsAddedReturn++;
            }
            while(iterator.hasNext()) {
                VesselContainer<?> destinationContainer = iterator.next();
                ContainerVertex destinationContainerVertex = new ContainerVertex(destinationContainer);
                if (destinationContainerVertex.render(graph, alternativeIds)) {
                    vesselVertexQueue.add(destinationContainerVertex);
                    numVesselsAddedReturn++;
                }
                renderRearrayEdge(graph, sourceContainer, destinationContainer, receptacle);
            }
        }
        return numVesselsAddedReturn;
    }

    /**
     * Insert an edge for a receptacle re-array, i.e. a tube moved from one rack to another
     * @param graph edges and vertices
     * @param sourceContainer source of re-array
     * @param destinationContainer destination of re-array
     */
    private void renderRearrayEdge(Graph graph, VesselContainer<?> sourceContainer, VesselContainer<?> destinationContainer, LabVessel containee) {
        // There's no event, so create a fake event ID
        String eventId = sourceContainer.getEmbedder().getLabel() + "|" + destinationContainer.getEmbedder().getLabel() +
                "|" + containee.getLabel();
        if(graph.getVisitedEventIds().add(eventId)) {
            Vertex sourceReceptacleVertex = graph.getMapIdToVertex().get(containee.getLabel() + "|" +
                    sourceContainer.getEmbedder().getLabel());
            Vertex destinationReceptacleVertex = graph.getMapIdToVertex().get(containee.getLabel() + "|" +
                    destinationContainer.getEmbedder().getLabel());
            graph.getMapIdToEdge().put(eventId, new Edge("Rearray",
                    sourceReceptacleVertex, destinationReceptacleVertex, Edge.LineType.DASHED));
        }
    }

    private class ContainerVertex extends VesselVertex {

        private VesselContainer vesselContainer;

        private ContainerVertex(VesselContainer vesselContainer) {
            this.vesselContainer = vesselContainer;
        }

        @Override
        boolean render(Graph graph, List<AlternativeId> alternativeIds) {
//            System.out.println("Rendering rack " + wellMap.getPlate().getBarcode());
            boolean newVertex = false;
//            Plate rack = tubeFormation.getPlate();
            String label = vesselContainer.getEmbedder().getLabel();
            Vertex rackVertex = graph.getMapIdToVertex().get(label);
            if (rackVertex == null) {
                newVertex = true;
                // Create a child vertex for each tube, and position it correctly within the rack
                int maxRowNumber = 0;
                int maxColumnNumber = 0;
                // todo jmt set these based on the actual layout of tubes
                VesselGeometry vesselGeometry = vesselContainer.getEmbedder().getVesselGeometry();
                maxRowNumber = vesselGeometry.getRowNames().length;
                maxColumnNumber = vesselGeometry.getColumnNames().length;

                // todo jmt fix rack type
                rackVertex = new Vertex(label, IdType.CONTAINER_ID_TYPE.toString(),
                        /*vesselContainer.getRackType().getDisplayName() +*/ " : " +
                                vesselContainer.getEmbedder().getLabCentricName(),
                        maxRowNumber, maxColumnNumber);
                for (VesselPosition vesselPosition : vesselGeometry.getVesselPositions()) {
                    LabVessel receptacle = vesselContainer.getVesselAtPosition(vesselPosition);
                    String barcode = receptacle == null ? vesselPosition.name() : receptacle.getLabel();
                    Vertex tubeVertex = new Vertex(barcode + "|" + label, IdType.TUBE_IN_RACK_ID_TYPE.toString(), barcode);
                    tubeVertex.setParentVertex(rackVertex);

                    tubeVertex.setAlternativeIds(getAlternativeIds(receptacle, alternativeIds));
//                    addLibraryTypeToDetails(receptacle, tubeVertex);
                    // need way to get from geometry to VesselPositions and vice versa
                    VesselGeometry.RowColumn rowColumn = vesselGeometry.getRowColumnForVesselPosition(vesselPosition);
                    rackVertex.getChildVertices()[rowColumn.getRow() - 1][rowColumn.getColumn() - 1] = tubeVertex;
                    // map as child|parent and as child, the latter for tube to tube transfers
                    graph.getMapIdToVertex().put(barcode, tubeVertex);
                    graph.getMapIdToVertex().put(tubeVertex.getId(), tubeVertex);
                }
                graph.getMapIdToVertex().put(rackVertex.getId(), rackVertex);
                rackVertex.setHasMoreEdges(true);
            }
            setVertex(rackVertex);
            return newVertex;
        }

        @Override
        public int renderEdges(Graph graph, Queue<VesselVertex> vesselVertexQueue, List<AlternativeId> alternativeIds) {
            int numVesselsAdded = 0;
            Set<SectionTransfer> sectionTransfersThisAsSource = vesselContainer.getSectionTransfersFrom();
            Set<SectionTransfer> sectionTransfersThisAsTarget = vesselContainer.getSectionTransfersTo();
            numVesselsAdded += renderSectionTransfers(graph, vesselVertexQueue, alternativeIds, sectionTransfersThisAsSource,
                    sectionTransfersThisAsTarget);
            numVesselsAdded += renderCherryPickTransfers(graph, vesselVertexQueue, alternativeIds, vesselContainer.getCherryPickTransfersFrom());
            numVesselsAdded += renderCherryPickTransfers(graph, vesselVertexQueue, alternativeIds, vesselContainer.getCherryPickTransfersTo());

            // Render any transfers from the rack tubes
            for (Object o : vesselContainer.getMapPositionToVessel().entrySet()) {
                Map.Entry<VesselPosition, LabVessel> vesselPositionTwoDBarcodedTubeEntry = (Map.Entry<VesselPosition, LabVessel>) o;
                numVesselsAdded += renderReceptacleEdges(vesselPositionTwoDBarcodedTubeEntry.getValue(),
                        graph, vesselVertexQueue, alternativeIds);
            }
            return numVesselsAdded;
        }
    }


//    private void addLibraryTypeToDetails(Receptacle receptacle, Vertex tubeVertex) {
//        if (receptacle.getContainedContents() != null) {
//            for (ISeqContent iSeqContent : receptacle.getContainedContents()) {
//                tubeVertex.getDetails().add("Library Type: " + iSeqContent.getLibraryTypeName());
//            }
//        }
//    }


    private class ReceptacleVesselVertex extends VesselVertex {

        private TwoDBarcodedTube receptacle;

        private ReceptacleVesselVertex(TwoDBarcodedTube receptacle) {
            this.receptacle = receptacle;
        }

        @Override
        boolean render(Graph graph, List<AlternativeId> alternativeIds) {
//            System.out.println("Rendering receptacle " + receptacle.getBarcode());
            boolean newVertex = false;
            Vertex vertex = graph.getMapIdToVertex().get(receptacle.getLabel());
            if (vertex == null) {
                newVertex = true;
                // todo jmt refactor FlowcellLane and StripTubeWell into their own classes?
//                if (receptacle instanceof FlowcellLane) {
//                    // render all lanes, even if they're not immediately referenced, because they may be referenced later by "more transfers"
//                    Flowcell flowcell = ((FlowcellLane) receptacle).getFlowcell();
//                    Vertex flowcellVertex = graph.getMapIdToVertex().get(flowcell.getBarcode());
//                    if (flowcellVertex == null) {
//                        flowcellVertex = new Vertex(flowcell.getBarcode(), IdType.RECEPTACLE_ID_TYPE.toString(),
//                                new StringBuilder().append("Flowcell : ").append(flowcell.getBarcode()).toString(),
//                                flowcell.getFlowcellLanes().size(), 1);
//                        graph.getMapIdToVertex().put(flowcellVertex.getId(), flowcellVertex);
//                        for (FlowcellLane flowcellLane : flowcell.getFlowcellLanes()) {
//                            vertex = new Vertex(flowcellLane.getBarcode(), IdType.RECEPTACLE_ID_TYPE.toString(), flowcellLane.getBarcode());
//                            flowcellVertex.getChildVertices()[(int) flowcellLane.getFlowcellLaneDescr().getUiSortOrder().shortValue() - 1][0] = vertex;
//                            vertex.setParentVertex(flowcellVertex);
//                            vertex.setAlternativeIds(getAlternativeIds(receptacle, alternativeIds));
//                            addLibraryTypeToDetails(receptacle, vertex);
//                            graph.getMapIdToVertex().put(vertex.getId(), vertex);
//                        }
//                        flowcellVertex.setHasMoreEdges(true);
//                    }
//                } else if (receptacle instanceof StripTubeWell) {
//                    // render all wells, even if they're not immediately referenced, because they may be referenced later by "more transfers"
//                    StripTube stripTube = ((StripTubeWell) receptacle).getStripTube();
//                    Vertex stripTubeVertex = graph.getMapIdToVertex().get(stripTube.getBarcode());
//                    if (stripTubeVertex == null) {
//                        stripTubeVertex = new Vertex(stripTube.getBarcode(), IdType.RECEPTACLE_ID_TYPE.toString(),
//                                new StringBuilder().append("Strip Tube : ").append(stripTube.getBarcode()).toString(),
//                                stripTube.getWells().size(), 1);
//                        graph.getMapIdToVertex().put(stripTubeVertex.getId(), stripTubeVertex);
//                        for (StripTubeWell stripTubeWell : stripTube.getWells()) {
//                            vertex = new Vertex(stripTubeWell.getBarcode(), IdType.RECEPTACLE_ID_TYPE.toString(), stripTubeWell.getBarcode());
//                            stripTubeVertex.getChildVertices()[(int) stripTubeWell.getFlowcellLaneDescr().getUiSortOrder().shortValue() - 1][0] = vertex;
//                            vertex.setParentVertex(stripTubeVertex);
//                            vertex.setAlternativeIds(getAlternativeIds(receptacle, alternativeIds));
//                            addLibraryTypeToDetails(receptacle, vertex);
//                            graph.getMapIdToVertex().put(vertex.getId(), vertex);
//                        }
//                        stripTubeVertex.setHasMoreEdges(true);
//                    }
//                } else {
                    vertex = new Vertex(receptacle.getLabel(), IdType.RECEPTACLE_ID_TYPE.toString(), buildReceptacleLabel(receptacle));
                    vertex.setAlternativeIds(getAlternativeIds(receptacle, alternativeIds));
                    graph.getMapIdToVertex().put(vertex.getId(), vertex);
                    vertex.setHasMoreEdges(true);
//                }
            }
            setVertex(vertex);
            return newVertex;
        }

        @Override
        public int renderEdges(Graph graph, Queue<VesselVertex> vesselVertexQueue, List<AlternativeId> alternativeIds) {
            int numVesselsAdded = 0;
//            if (receptacle instanceof Flowcell) {
//                Flowcell flowcell = (Flowcell) receptacle;
//                for (FlowcellLane flowcellLane : flowcell.getFlowcellLanes()) {
//                    numVesselsAdded = renderReceptacleEdges(flowcellLane, graph, vesselVertexQueue, alternativeIds);
//                }
//            } else if (receptacle instanceof StripTube) {
//                StripTube stripTube = (StripTube) receptacle;
//                for (StripTubeWell stripTubeWell : stripTube.getWells()) {
//                    numVesselsAdded = renderReceptacleEdges(stripTubeWell, graph, vesselVertexQueue, alternativeIds);
//                }
//            } else {
                numVesselsAdded = renderReceptacleEdges(receptacle, graph, vesselVertexQueue, alternativeIds);
//            }
            return numVesselsAdded;
        }

        private String buildReceptacleLabel(LabVessel receptacle) {
            String contentTypeName = "";
//            if (receptacle.getCurrentContent() != null /*&& receptacle.getCurrentContent().getTypeId() != null*/) {
//                contentTypeName = receptacle.getCurrentContent().getLibraryTypeName();
//            }
            return "Tube : " + receptacle.getLabel() + "<br/>" + contentTypeName;
        }
    }


    private int renderSectionTransfers(Graph graph, Queue<VesselVertex> vesselVertexQueue, List<AlternativeId> alternativeIds,
            Set<SectionTransfer> sourceSectionTransfers, Set<SectionTransfer> targetSectionTransfers) {
        int numVesselsAdded = 0;
        for (SectionTransfer sourceSectionTransfer : sourceSectionTransfers) {
            numVesselsAdded += renderEmbedder(graph, vesselVertexQueue, alternativeIds,
                    sourceSectionTransfer.getTargetVesselContainer().getEmbedder());
            renderEdge(graph, sourceSectionTransfer);
        }

        for (SectionTransfer targetSectionTransfer : targetSectionTransfers) {
            numVesselsAdded += renderEmbedder(graph, vesselVertexQueue, alternativeIds,
                    targetSectionTransfer.getSourceVesselContainer().getEmbedder());
            renderEdge(graph, targetSectionTransfer);
        }
        return numVesselsAdded;
    }

    private int renderCherryPickTransfers(Graph graph, Queue<VesselVertex> vesselVertexQueue, List<AlternativeId> alternativeIds,
            Set<CherryPickTransfer> cherryPickTransfers) {
        int numVesselsAdded = 0;
        for (CherryPickTransfer cherryPickTransfer : cherryPickTransfers) {
            String eventId = cherryPickTransfer.getKey();
            if(graph.getVisitedEventIds().add(eventId)) {
                numVesselsAdded += renderEmbedder(graph, vesselVertexQueue, alternativeIds,
                        cherryPickTransfer.getTargetVesselContainer().getEmbedder());
                numVesselsAdded += renderEmbedder(graph, vesselVertexQueue, alternativeIds,
                        cherryPickTransfer.getSourceVesselContainer().getEmbedder());

                Vertex sourceReceptacleVertex = graph.getMapIdToVertex().get(
                        cherryPickTransfer.getSourceVesselContainer().getVesselAtPosition(cherryPickTransfer.getSourcePosition()).getLabel() + "|" +
                        cherryPickTransfer.getSourceVesselContainer().getEmbedder().getLabel());
                LabVessel vesselAtPosition = cherryPickTransfer.getTargetVesselContainer().getVesselAtPosition(cherryPickTransfer.getTargetPosition());
                Vertex destinationReceptacleVertex = graph.getMapIdToVertex().get(
                        (vesselAtPosition == null ? cherryPickTransfer.getTargetPosition() : vesselAtPosition.getLabel()) + "|" +
                        cherryPickTransfer.getTargetVesselContainer().getEmbedder().getLabel());
                graph.getMapIdToEdge().put(eventId, new Edge(buildEdgeLabel(cherryPickTransfer.getLabEvent()),
                        sourceReceptacleVertex, destinationReceptacleVertex));
            }
        }

        return numVesselsAdded;
    }

    private int renderEmbedder(Graph graph, Queue<VesselVertex> vesselVertexQueue, List<AlternativeId> alternativeIds, LabVessel embedder) {
        int numVesselsAdded = 0;
        switch (embedder.getType()) {
            case STATIC_PLATE:
                PlateVesselVertex plateVessel = new PlateVesselVertex(OrmUtil.proxySafeCast(
                        embedder, StaticPlate.class));
                if(plateVessel.render(graph, alternativeIds)) {
                    vesselVertexQueue.add(plateVessel);
                    numVesselsAdded++;
                }
                break;
            case TUBE_FORMATION:
            case STRIP_TUBE:
            case FLOWCELL:
                ContainerVertex containerVertex = new ContainerVertex(embedder.getContainerRole());
                if(containerVertex.render(graph, alternativeIds)) {
                    vesselVertexQueue.add(containerVertex);
                    numVesselsAdded++;
                }
                break;
            default:
                throw new RuntimeException("Unexpected type " + embedder.getType());
        }
        return numVesselsAdded;
    }


    void processQueue(Queue<VesselVertex> vesselVertexQueue, Graph graph, List<AlternativeId> alternativeIds) {
        int numVesselsProcessed = 0;
        while (!vesselVertexQueue.isEmpty() && numVesselsProcessed < maxNumVesselsPerRequest) {
            VesselVertex vesselVertex = vesselVertexQueue.remove();
            vesselVertex.render(graph, alternativeIds);
            numVesselsProcessed += vesselVertex.renderEdges(graph, vesselVertexQueue, alternativeIds);
            vesselVertex.getVertex().setHasMoreEdges(false);
        }
    }

    /**
     * Insert an edge for a plate (or rack) to plate (or rack) transfer
     *
     * @param graph              edges and vertices
     * @param sectionTransfer source and destination
     */
    private void renderEdge(Graph graph, SectionTransfer sectionTransfer) {
        String eventId = sectionTransfer.getKey();
        if (graph.getVisitedEventIds().add(eventId)) {
            String sourceId = sectionTransfer.getSourceVesselContainer().getEmbedder().getLabel();
            String destinationId = sectionTransfer.getTargetVesselContainer().getEmbedder().getLabel();
            Vertex sourceVertex = graph.getMapIdToVertex().get(sourceId);
            if (sourceVertex == null) {
                throw new RuntimeException("In plateTransferEvent, no source vertex for " + sourceId);
            }
            Vertex destinationVertex = graph.getMapIdToVertex().get(destinationId);
            if (destinationVertex == null) {
                throw new RuntimeException("In plateTransferEvent, no destination vertex for " + destinationId);
            }
            graph.getMapIdToEdge().put(eventId, new Edge(buildEdgeLabel(sectionTransfer.getLabEvent()),
                    sourceVertex,
                    destinationVertex));
        }
    }

    /**
     * Insert an edge for a transfer from a receptacle to a plate section
     *
     * @param graph                        edges and vertices
     * @param receptaclePlateTransferEvent source and destination
     */
//    private void renderEdge(Graph graph, ReceptaclePlateTransferEvent receptaclePlateTransferEvent) {
//        String eventId = Long.toString(receptaclePlateTransferEvent.getId());
//        if (graph.getVisitedEventIds().add(receptaclePlateTransferEvent.getId())) {
//            Vertex sourceVertex = graph.getMapIdToVertex().get(receptaclePlateTransferEvent.getReceptacle().getBarcode());
//            if (sourceVertex == null) {
//                throw new RuntimeException("In receptaclePlateTransferEvent, no source vertex for " + receptaclePlateTransferEvent.getReceptacle().getBarcode());
//            }
//            Vertex destinationVertex = graph.getMapIdToVertex().get(receptaclePlateTransferEvent.getPlate().getBarcode());
//            if (destinationVertex == null) {
//                throw new RuntimeException("In receptaclePlateTransferEvent, no destination vertex for " + receptaclePlateTransferEvent.getPlate().getBarcode());
//            }
//            graph.getMapIdToEdge().put(eventId, new Edge(buildEdgeLabel(receptaclePlateTransferEvent),
//                    sourceVertex,
//                    destinationVertex));
//        }
//    }

    /**
     * Insert an edge for a receptacle to receptacle transfer
     *
     * @param graph                   edges and vertices
     * @param receptacleTransferEvent source and destination
     */
//    private void renderEdge(Graph graph, ReceptacleTransferEvent receptacleTransferEvent) {
//        if (graph.getVisitedEventIds().add(receptacleTransferEvent.getId())) {
//            Vertex sourceReceptacleVertex = graph.getMapIdToVertex().get(receptacleTransferEvent.getSourceReceptacle().getBarcode());
//            if (sourceReceptacleVertex == null) {
//                throw new RuntimeException("In receptacleTransferEvent, No source vertex for " + receptacleTransferEvent.getSourceReceptacle().getBarcode());
//            }
//            Vertex destinationReceptacleVertex = graph.getMapIdToVertex().get(receptacleTransferEvent.getReceptacle().getBarcode());
//            if (destinationReceptacleVertex == null) {
//                throw new RuntimeException("In receptacleTransferEvent, No destination vertex for " + receptacleTransferEvent.getReceptacle().getBarcode());
//            }
//            graph.getMapIdToEdge().put(Long.toString(receptacleTransferEvent.getId()), new Edge(buildEdgeLabel(receptacleTransferEvent),
//                    sourceReceptacleVertex, destinationReceptacleVertex));
//        }
//    }

    /**
     * The user can select one more ID types to be rendered into each vertex
     *
     * @param receptacle        tube for which to list IDs
     * @param alternativeIdList list of ID types specified by user
     * @return list of Ids for the given receptacle
     */
    private Map<String, List<String>> getAlternativeIds(
            LabVessel receptacle, List<AlternativeId> alternativeIdList) {
        Map<String, List<String>> alternativeIdValues = new HashMap<String, List<String>>();
//        if (alternativeIdList.contains(AlternativeId.LIBRARY_NAME) || alternativeIdList.contains(AlternativeId.GSSR_SAMPLE)) {
//            if (receptacle.getContainedContents() != null) {
//                for (ISeqContent iSeqContent : receptacle.getContainedContents()) {
//                    if (alternativeIdList.contains(AlternativeId.LIBRARY_NAME) && iSeqContent.getName() != null) {
//                        Vertex.addAlternativeId(alternativeIdValues, AlternativeId.LIBRARY_NAME.getDisplayName(), iSeqContent.getName());
//                    }
//                    if (alternativeIdList.contains(AlternativeId.GSSR_SAMPLE) && iSeqContent.getContentDescriptions() != null) {
//                        for (ISeqContentDescr iSeqContentDescr : iSeqContent.getContentDescriptions()) {
//                            String sampleBarcode = iSeqContentDescr.getLcSample() != null ? iSeqContentDescr.getLcSample().getBarcode() : "";
//                            String indexName = iSeqContentDescr.getIndexingScheme() != null ?
//                                    iSeqContentDescr.getIndexingScheme().getName() : "";
//                            Vertex.addAlternativeId(alternativeIdValues, AlternativeId.GSSR_SAMPLE.getDisplayName(),
//                                    sampleBarcode + ":" + indexName);
//                        }
//                    }
//                }
//            }
//        }
        return alternativeIdValues;
    }

    public void setMaxNumVesselsPerRequest(int maxNumVesselsPerRequest) {
        this.maxNumVesselsPerRequest = maxNumVesselsPerRequest;
    }
}
