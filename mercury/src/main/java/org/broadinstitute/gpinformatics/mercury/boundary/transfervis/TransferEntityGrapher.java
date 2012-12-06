package org.broadinstitute.gpinformatics.mercury.boundary.transfervis;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.mercury.boundary.graph.Graph;
import org.broadinstitute.gpinformatics.mercury.boundary.graph.Vertex;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.StaticPlateDAO;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.TwoDBarcodedTubeDAO;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;

import javax.inject.Inject;
import java.rmi.AccessException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * From one of various starting entities (plate, tube etc.), traverse transfers breadth-first, create a graph of
 * entities and transfers, until reach a limit on the amount of work in one invocation.  The user can
 * click "More Transfers" to expand part of the graph.
 */
public class TransferEntityGrapher implements TransferVisualizer {

    private static final int MAX_NUM_VESSELS_PER_REQUEST = 5;

    @Inject
    private StaticPlateDAO staticPlateDAO;

    @Inject
    private TwoDBarcodedTubeDAO twoDBarcodedTubeDAO;

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
        mapIdTypeToExpandVertex.put(IdType.PLATE_ID_TYPE, new TransferEntityGrapher.ExpandVertex() {
            @Override
            public void expandVertex(String vertexId, Graph graph,
                    TransferEntityGrapher transferEntityGrapher, List<AlternativeId> alternativeIds) {
                StaticPlate plate = staticPlateDAO.findByBarcode(vertexId);
                transferEntityGrapher.startWithPlate(plate, graph, alternativeIds);
            }
        });
//        mapIdTypeToExpandVertex.put(IdType.WELL_MAP_ID_TYPE, new TransferEntityGrapher.ExpandVertex() {
//            @Override
//            public void expandVertex(String vertexId, Graph graph,
//                    TransferEntityGrapher transferEntityGrapher, List<AlternativeId> alternativeIds) {
//                WellMap wellMap = entityManager.find(WellMap.class, new Long(vertexId));
//                transferEntityGrapher.startWithWellMap(wellMap, graph, alternativeIds);
//            }
//        });
        mapIdTypeToExpandVertex.put(IdType.TUBE_IN_RACK_ID_TYPE, null); // can't be expanded
        mapIdTypeToExpandVertex.put(IdType.RECEPTACLE_ID_TYPE, new TransferEntityGrapher.ExpandVertex() {
            @Override
            public void expandVertex(String vertexId, Graph graph,
                    TransferEntityGrapher transferEntityGrapher, List<AlternativeId> alternativeIds) {
                TwoDBarcodedTube receptacle = twoDBarcodedTubeDAO.findByBarcode(vertexId);
                transferEntityGrapher.startWithTube(receptacle, graph, alternativeIds);
            }
        });
    }

    /**
     * Start the RMI server
     *
     * @return constructed server
     */
    public static TransferVisualizer init() {
        try {
            TransferVisualizer transferVisualizer = (TransferVisualizer) UnicastRemoteObject.exportObject(new TransferEntityGrapher(), 0);
            Registry registry = LocateRegistry.createRegistry(9345);
            registry.rebind(TransferVisualizer.serviceName, transferVisualizer);
            return transferVisualizer;
        } catch (AccessException e) {
            throw new RuntimeException(e);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * For testing
     *
     * @param args not used
     */
    public static void main(String[] args) {
        TransferEntityGrapher.init();
    }

    /**
     * Build a graph, staring with a plate barcode
     * @param plateBarcode plate the user is searching on
     * @param alternativeIds which IDs to include in the vertices
     * @return vertices and edges
     */
    @Override
    public Graph forPlate(String plateBarcode, List<AlternativeId> alternativeIds) throws RemoteException {
        Graph graph = new Graph();
        try {
            StaticPlate plate = staticPlateDAO.findByBarcode(plateBarcode);
            startWithPlate(plate, graph, alternativeIds);
            return graph;
        } finally {
        }
    }

    private void startWithPlate(StaticPlate plate, Graph graph, List<AlternativeId> alternativeIds) {
        if (plate == null) {
            graph.setMessage("No plate was found with that barcode");
        } else {
//            if (plate.getPlatePhysicalType().getName().equals(PlatePhysicalType.TUBE_RACK)) {
//                graph.setMessage("The barcode is attached to a rack, please enter a plate barcode");
//            } else {
                Queue<Vessel> vesselQueue = new LinkedList<Vessel>();

                vesselQueue.add(new PlateVessel(plate));
                processQueue(vesselQueue, graph, alternativeIds);
//            }
        }
    }

//    private void startWithWellMap(WellMap wellMap, Graph graph, List<AlternativeId> alternativeIds) {
//        Queue<Vessel> vesselQueue = new LinkedList<Vessel>();
//
//        vesselQueue.add(new RackVessel(wellMap));
//        processQueue(vesselQueue, graph, alternativeIds);
//    }

    /**
     * Build a graph, starting with a tube barcode
     * @param tubeBarcode tube the user is searching on
     * @param alternativeIds which IDs to include in the vertices
     * @return vertices and edges
     */
    @Override
    public Graph forTube(String tubeBarcode, List<AlternativeId> alternativeIds) throws RemoteException {
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
            Queue<Vessel> vesselQueue = new LinkedList<Vessel>();

            for (VesselContainer vesselContainer : receptacle.getContainers()) {
                vesselQueue.add(new RackVessel((TubeFormation) vesselContainer.getEmbedder()));
            }
            vesselQueue.add(new ReceptacleVessel(receptacle));
            processQueue(vesselQueue, graph, alternativeIds);
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
        ExpandVertex expandVertex = mapIdTypeToExpandVertex.get(Enum.valueOf(IdType.class, idType));
        expandVertex.expandVertex(vertexId, graph, this, alternativeIds);
        return graph;
    }

    /**
     * Called when the user clicks a tube
     * @param tubeBarcode barcode of the tube the user is searching on
     * @return map from AlternativeId displayName to list of IDs
     * @throws java.rmi.RemoteException
     */
    @Override
    public Map<String, List<String>> getIdsForTube(String tubeBarcode) throws RemoteException {
        try {
            TwoDBarcodedTube receptacle = twoDBarcodedTubeDAO.findByBarcode(tubeBarcode);
            return getAlternativeIds(receptacle, Arrays.asList(AlternativeId.values()));
        } finally {
        }
    }


/*
    private EntityManager createEntityManager() {
        EntityManager entityManager = HibernateUtil.getEntityManagerFactory().createEntityManager();
        entityManager.setFlushMode(FlushModeType.COMMIT);
        return entityManager;
    }
*/

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
//            label.append(eventTypeName);
//            if (stationEvent instanceof PlateTransferEvent) {
//                PlateTransferEvent plateTransferEvent = (PlateTransferEvent) stationEvent;
//                label.append("<br/>");
//                label.append(plateTransferEvent.getSourceSectionLayout());
//                label.append(" to ");
//                label.append(plateTransferEvent.getSectionLayout());
//            }
//            label.append("<br/>");
//        }
        String labMachineName = stationEvent.getEventLocation();
        if (!labMachineName.contains("Unknown")) {
            label.append(labMachineName).append(", ");
        }
        label.append(stationEvent.getEventDate());
        label.append("<br/>");
        BspUser bspUser = bspUserList.getById(stationEvent.getEventOperator());
        if (bspUser != null) {
            label.append(bspUser.getFirstName()).append(" ").append(bspUser.getLastName());
        }
        label.append("</html>");
        return label.toString();
    }

    private abstract static class Vessel {

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
         * @param vesselQueue    queue to which to add (new) vessels at opposite end of each transfer (vessel is not
         *                       added if it's already in the graph)
         * @param alternativeIds IDs the user wants rendered
         * @return number of vessels added to the queue
         */
        public abstract int renderEdges(Graph graph, Queue<Vessel> vesselQueue, List<AlternativeId> alternativeIds);

        public Vertex getVertex() {
            return vertex;
        }

        public void setVertex(Vertex vertex) {
            this.vertex = vertex;
        }
    }


    private class PlateVessel extends Vessel {

        private StaticPlate plate;

        private PlateVessel(StaticPlate plate) {
            this.plate = plate;
        }

        @Override
        boolean render(Graph graph, List<AlternativeId> alternativeIds) {
//            System.out.println("Rendering plate " + plate.getBarcode());
            boolean newVertex = false;
//            Vertex plateVertex = graph.getMapIdToVertex().get(plate.getBarcode());
//            if (plateVertex == null) {
//                newVertex = true;
//                plateVertex = new Vertex(plate.getBarcode(), IdType.PLATE_ID_TYPE.toString(),
//                        new StringBuilder().append(plate.getPlatePhysicalType().getName()).append(" : ").append(plate.getBarcode()).toString());
//                // If there are self-referential plate events, draw a box for the user to click on, to see a list of these events
//                List<PlateEvent> sortedPlateEvents = new ArrayList<PlateEvent>();
//                for (PlateEvent plateEvent : plate.getPlateEvents()) {
//                    if (!(plateEvent instanceof PlateTransferEvent)) {
//                        sortedPlateEvents.add(plateEvent);
//                    }
//                }
//                // Sort the plate events chronologically
//                Collections.sort(sortedPlateEvents, new Comparator<PlateEvent>() {
//                    @Override
//                    public int compare(PlateEvent o1, PlateEvent o2) {
//                        return o1.getStartTime().compareTo(o2.getStartTime());
//                    }
//                });
//                for (PlateEvent plateEvent : sortedPlateEvents) {
//                    plateVertex.getDetails().add(new StringBuilder().append(plateEvent.getEventType().getName()).append(", ").
//                            append(plateEvent.getLabMachine().getName()).append(", ").append(plateEvent.getEndTime()).append(", ").
//                            append(plateEvent.getStaff().getFirstName()).append(" ").append(plateEvent.getStaff().getLastName()).toString());
//                }
//                graph.getMapIdToVertex().put(plateVertex.getId(), plateVertex);
//                plateVertex.setHasMoreEdges(true);
//            }
//            setVertex(plateVertex);
            return newVertex;
        }

        @Override
        public int renderEdges(Graph graph, Queue<Vessel> vesselQueue, List<AlternativeId> alternativeIds) {
            int numVesselsAdded = 0;
//            numVesselsAdded += renderPlateTransfers(graph, vesselQueue, alternativeIds, plate.getSourcePlateTransferEvents(),
//                    plate.getPlateEvents());
//            for (ReceptaclePlateTransferEvent receptaclePlateTransferEvent : plate.getReceptaclePlateTransferEvents()) {
//                ReceptacleVessel receptacleVessel = new ReceptacleVessel((Receptacle) receptaclePlateTransferEvent.getReceptacle());
//                if(receptacleVessel.render(graph, alternativeIds)) {
//                    vesselQueue.add(receptacleVessel);
//                    numVesselsAdded++;
//                }
//                renderEdge(graph, receptaclePlateTransferEvent);
//            }
            return numVesselsAdded;
        }
    }

    private int renderReceptacleEdges(LabVessel receptacle, Graph graph, Queue<Vessel> vesselQueue, List<AlternativeId> alternativeIds) {
        int numVesselsAddedReturn = 0;
//        for (ReceptacleTransferEvent receptacleTransferEvent : receptacle.getReceptacleTransferEventsThisAsSource()) {
//            Receptacle destinationReceptacle = (Receptacle) receptacleTransferEvent.getReceptacle();
//            if(destinationReceptacle.getWellMapEntries().isEmpty()) {
//                ReceptacleVessel receptacleVessel = new ReceptacleVessel(destinationReceptacle);
//                if (receptacleVessel.render(graph, alternativeIds)) {
//                    vesselQueue.add(receptacleVessel);
//                    numVesselsAddedReturn++;
//                }
//            } else {
//                for (WellMapEntry wellMapEntry : destinationReceptacle.getWellMapEntries()) {
//                    RackVessel rackVessel = new RackVessel(wellMapEntry.getWellMap());
//                    if(rackVessel.render(graph, alternativeIds)) {
//                        vesselQueue.add(rackVessel);
//                        numVesselsAddedReturn++;
//                    }
//                }
//            }
//            renderEdge(graph, receptacleTransferEvent);
//        }
//        for (ReceptaclePlateTransferEvent receptaclePlateTransferEvent : receptacle.getReceptaclePlateTransferEvents()) {
//            PlateVessel plateVessel = new PlateVessel(receptaclePlateTransferEvent.getPlate());
//            if(plateVessel.render(graph, alternativeIds)) {
//                vesselQueue.add(plateVessel);
//                numVesselsAddedReturn++;
//            }
//            renderEdge(graph, receptaclePlateTransferEvent);
//        }
//        for (ReceptacleEvent receptacleEvent : receptacle.getReceptacleEvents()) {
//            if (receptacleEvent instanceof ReceptacleTransferEvent) {
//                ReceptacleTransferEvent receptacleTransferEvent = (ReceptacleTransferEvent) receptacleEvent;
//                Receptacle sourceReceptacle = (Receptacle) receptacleTransferEvent.getSourceReceptacle();
//                if(sourceReceptacle.getWellMapEntries().isEmpty()) {
//                    ReceptacleVessel receptacleVessel = new ReceptacleVessel(sourceReceptacle);
//                    if (receptacleVessel.render(graph, alternativeIds)) {
//                        vesselQueue.add(receptacleVessel);
//                        numVesselsAddedReturn++;
//                    }
//                } else {
//                    for (WellMapEntry wellMapEntry : sourceReceptacle.getWellMapEntries()) {
//                        RackVessel rackVessel = new RackVessel(wellMapEntry.getWellMap());
//                        if(rackVessel.render(graph, alternativeIds)) {
//                            vesselQueue.add(rackVessel);
//                            numVesselsAddedReturn++;
//                        }
//                    }
//                }
//                renderEdge(graph, receptacleTransferEvent);
//            }
//        }
//        // if a receptacle appears in two different well maps, that's an implied re-array
//        if(receptacle.getWellMapEntries().size() > 1) {
//            Iterator<WellMapEntry> iterator = receptacle.getWellMapEntries().iterator();
//            WellMapEntry sourceWellMapEntry = iterator.next();
//            RackVessel sourceRackVessel = new RackVessel(sourceWellMapEntry.getWellMap());
//            if (sourceRackVessel.render(graph, alternativeIds)) {
//                vesselQueue.add(sourceRackVessel);
//                numVesselsAddedReturn++;
//            }
//            while(iterator.hasNext()) {
//                WellMapEntry destinationWellMapEntry = iterator.next();
//                RackVessel destinationRackVessel = new RackVessel(destinationWellMapEntry.getWellMap());
//                if (destinationRackVessel.render(graph, alternativeIds)) {
//                    vesselQueue.add(destinationRackVessel);
//                    numVesselsAddedReturn++;
//                }
//                renderRearrayEdge(graph, sourceWellMapEntry, destinationWellMapEntry);
//            }
//        }
        return numVesselsAddedReturn;
    }

    /**
     * Insert an edge for a receptacle re-array, i.e. a tube moved from one rack to another
     * @param graph edges and vertices
     * @param sourceWellMapEntry source of re-array
     * @param destinationWellMapEntry destination of re-array
     */
//    private void renderRearrayEdge(Graph graph, WellMapEntry sourceWellMapEntry, WellMapEntry destinationWellMapEntry) {
//        // There's no event, so create a fake event ID
//        // todo jmt make this more likely to be unique
//        long eventId = sourceWellMapEntry.getId() + destinationWellMapEntry.getId();
//        if(graph.getVisitedEventIds().add(eventId)) {
//            Vertex sourceReceptacleVertex = graph.getMapIdToVertex().get(sourceWellMapEntry.getReceptacle().getBarcode() + "|" +
//                    sourceWellMapEntry.getWellMap().getId());
//            Vertex destinationReceptacleVertex = graph.getMapIdToVertex().get(destinationWellMapEntry.getReceptacle().getBarcode() + "|" +
//                    destinationWellMapEntry.getWellMap().getId());
//            graph.getMapIdToEdge().put(Long.toString(eventId), new Edge("Rearray",
//                    sourceReceptacleVertex, destinationReceptacleVertex, Edge.LineType.DASHED));
//        }
//    }

    private class RackVessel extends Vessel {

        private TubeFormation tubeFormation;

        private RackVessel(TubeFormation tubeFormation) {
            this.tubeFormation = tubeFormation;
        }

        @Override
        boolean render(Graph graph, List<AlternativeId> alternativeIds) {
//            System.out.println("Rendering rack " + wellMap.getPlate().getBarcode());
            boolean newVertex = false;
//            Plate rack = tubeFormation.getPlate();
//            Vertex rackVertex = graph.getMapIdToVertex().get(tubeFormation.getLabel());
//            if (rackVertex == null) {
//                newVertex = true;
//                // Create a child vertex for each tube, and position it correctly within the rack
//                int maxRowNumber = 0;
//                int maxColumnNumber = 0;
//                for (Map.Entry<VesselPosition, TwoDBarcodedTube> vesselPositionTwoDBarcodedTubeEntry :
//                        tubeFormation.getContainerRole().getMapPositionToVessel().entrySet()) {
//                    vesselPositionTwoDBarcodedTubeEntry.getKey();
//                    tubeFormation.getRackType().getVesselGeometry().
//                }
//
//                for (WellMapEntry wellMapEntry : tubeFormation.getWellMapEntries().values()) {
//                    maxRowNumber = Math.max((int) wellMapEntry.getWellDescription().getRowNumber(), maxRowNumber);
//                    maxColumnNumber = Math.max((int) wellMapEntry.getWellDescription().getColumnNumber(), maxColumnNumber);
//                }
//                rackVertex = new Vertex(Long.toString(tubeFormation.getId()), IdType.WELL_MAP_ID_TYPE.toString(),
//                        new StringBuilder().append(rack.getPlatePhysicalType().getName()).append(" : ").append(rack.getBarcode()).toString(),
//                        maxRowNumber, maxColumnNumber);
//                for (WellMapEntry wellMapEntry : tubeFormation.getWellMapEntries().values()) {
//                    Receptacle receptacle = wellMapEntry.getReceptacle();
//                    String barcode = receptacle.getBarcode();
//                    Vertex tubeVertex = new Vertex(new StringBuilder().append(barcode).append("|").append(tubeFormation.getId()).toString(),
//                            IdType.TUBE_IN_RACK_ID_TYPE.toString(), barcode);
//                    tubeVertex.setParentVertex(rackVertex);
//
//                    tubeVertex.setAlternativeIds(getAlternativeIds(receptacle, alternativeIds));
////                    addLibraryTypeToDetails(receptacle, tubeVertex);
//                    rackVertex.getChildVertices()[(int) wellMapEntry.getWellDescription().getRowNumber() - 1][(int) wellMapEntry.getWellDescription().getColumnNumber() - 1] =
//                            tubeVertex;
//                    // map as child|parent and as child, the latter for tube to tube transfers
//                    graph.getMapIdToVertex().put(barcode, tubeVertex);
//                    graph.getMapIdToVertex().put(tubeVertex.getId(), tubeVertex);
//                }
//                graph.getMapIdToVertex().put(rackVertex.getId(), rackVertex);
//                rackVertex.setHasMoreEdges(true);
//            }
//            setVertex(rackVertex);
            return newVertex;
        }

        @Override
        public int renderEdges(Graph graph, Queue<Vessel> vesselQueue, List<AlternativeId> alternativeIds) {
            int numVesselsAdded = 0;
//            Set<PlateTransferEvent> plateTransferEventsThisAsSource = wellMap.getPlateTransferEventsThisAsSource();
//            Set<PlateEvent> plateEvents = wellMap.getPlateEvents();
//            numVesselsAdded += renderPlateTransfers(graph, vesselQueue, alternativeIds, plateTransferEventsThisAsSource, plateEvents);
//
//            // Render any transfers from the rack tubes
//            for (Map.Entry<Long, WellMapEntry> mapEntry : wellMap.getWellMapEntries().entrySet()) {
//                Receptacle receptacle = mapEntry.getValue().getReceptacle();
//                numVesselsAdded += renderReceptacleEdges(receptacle, graph, vesselQueue, alternativeIds);
//            }
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


    private class ReceptacleVessel extends Vessel {

        private TwoDBarcodedTube receptacle;

        private ReceptacleVessel(TwoDBarcodedTube receptacle) {
            this.receptacle = receptacle;
        }

        @Override
        boolean render(Graph graph, List<AlternativeId> alternativeIds) {
//            System.out.println("Rendering receptacle " + receptacle.getBarcode());
            boolean newVertex = false;
//            Vertex vertex = graph.getMapIdToVertex().get(receptacle.getBarcode());
//            if (vertex == null) {
//                newVertex = true;
//                // todo jmt refactor FlowcellLane and StripTubeWell into their own classes?
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
//                    vertex = new Vertex(receptacle.getBarcode(), IdType.RECEPTACLE_ID_TYPE.toString(), buildReceptacleLabel(receptacle));
//                    vertex.setAlternativeIds(getAlternativeIds(receptacle, alternativeIds));
//                    graph.getMapIdToVertex().put(vertex.getId(), vertex);
//                    vertex.setHasMoreEdges(true);
//                }
//            }
//            setVertex(vertex);
            return newVertex;
        }

        @Override
        public int renderEdges(Graph graph, Queue<Vessel> vesselQueue, List<AlternativeId> alternativeIds) {
            int numVesselsAdded = 0;
//            if (receptacle instanceof Flowcell) {
//                Flowcell flowcell = (Flowcell) receptacle;
//                for (FlowcellLane flowcellLane : flowcell.getFlowcellLanes()) {
//                    numVesselsAdded = renderReceptacleEdges(flowcellLane, graph, vesselQueue, alternativeIds);
//                }
//            } else if (receptacle instanceof StripTube) {
//                StripTube stripTube = (StripTube) receptacle;
//                for (StripTubeWell stripTubeWell : stripTube.getWells()) {
//                    numVesselsAdded = renderReceptacleEdges(stripTubeWell, graph, vesselQueue, alternativeIds);
//                }
//            } else {
//                numVesselsAdded = renderReceptacleEdges(receptacle, graph, vesselQueue, alternativeIds);
//            }
            return numVesselsAdded;
        }

//        private String buildReceptacleLabel(Receptacle receptacle) {
//            String contentTypeName = "";
//            if (receptacle.getCurrentContent() != null /*&& receptacle.getCurrentContent().getTypeId() != null*/) {
//                contentTypeName = receptacle.getCurrentContent().getLibraryTypeName();
//            }
//            return new StringBuilder().append("Tube : ").append(receptacle.getBarcode()).append("<br/>").append(contentTypeName).toString();
//        }
    }


//    private int renderPlateTransfers(Graph graph, Queue<Vessel> vesselQueue, List<AlternativeId> alternativeIds,
//            Set<PlateTransferEvent> sourcePlateTransferEvents, Set<PlateEvent> plateEvents) {
//        int numVesselsAdded = 0;
//        for (PlateTransferEvent plateTransferEvent : sourcePlateTransferEvents) {
//            if(plateTransferEvent.getWellMap() == null) {
//                PlateVessel plateVessel = new PlateVessel(plateTransferEvent.getPlate());
//                if(plateVessel.render(graph, alternativeIds)) {
//                    vesselQueue.add(plateVessel);
//                    numVesselsAdded++;
//                }
//            } else {
//                RackVessel rackVessel = new RackVessel(plateTransferEvent.getWellMap());
//                if(rackVessel.render(graph, alternativeIds)) {
//                    vesselQueue.add(rackVessel);
//                    numVesselsAdded++;
//                }
//            }
//            renderEdge(graph, plateTransferEvent);
//        }
//
//        for (PlateEvent plateEvent : plateEvents) {
//            if (plateEvent instanceof PlateTransferEvent) {
//                PlateTransferEvent plateTransferEvent = (PlateTransferEvent) plateEvent;
//                if(plateTransferEvent.getSourceWellMap() == null) {
//                    PlateVessel plateVessel = new PlateVessel(plateTransferEvent.getSourcePlate());
//                    if(plateVessel.render(graph, alternativeIds)) {
//                        vesselQueue.add(plateVessel);
//                        numVesselsAdded++;
//                    }
//                } else {
//                    RackVessel rackVessel = new RackVessel(plateTransferEvent.getSourceWellMap());
//                    if(rackVessel.render(graph, alternativeIds)) {
//                        vesselQueue.add(rackVessel);
//                        numVesselsAdded++;
//                    }
//                }
//                renderEdge(graph, plateTransferEvent);
//            }
//        }
//        return numVesselsAdded;
//    }


    void processQueue(Queue<Vessel> vesselQueue, Graph graph, List<AlternativeId> alternativeIds) {
        int numVesselsProcessed = 0;
        while (!vesselQueue.isEmpty() && numVesselsProcessed < MAX_NUM_VESSELS_PER_REQUEST) {
            Vessel vessel = vesselQueue.remove();
            vessel.render(graph, alternativeIds);
            numVesselsProcessed += vessel.renderEdges(graph, vesselQueue, alternativeIds);
            vessel.getVertex().setHasMoreEdges(false);
        }
    }

    /**
     * Insert an edge for a plate (or rack) to plate (or rack) transfer
     *
     * @param graph              edges and vertices
     * @param plateTransferEvent source and destination
     */
//    private void renderEdge(Graph graph, PlateTransferEvent plateTransferEvent) {
//        String eventId = Long.toString(plateTransferEvent.getId());
//        if (graph.getVisitedEventIds().add(plateTransferEvent.getId())) {
//            String sourceId;
//            if (plateTransferEvent.getSourceWellMap() == null) {
//                sourceId = plateTransferEvent.getSourcePlate().getBarcode();
//            } else {
//                sourceId = Long.toString(plateTransferEvent.getSourceWellMap().getId());
//            }
//            String destinationId;
//            if (plateTransferEvent.getWellMap() == null) {
//                destinationId = plateTransferEvent.getPlate().getBarcode();
//            } else {
//                destinationId = Long.toString(plateTransferEvent.getWellMap().getId());
//            }
//            Vertex sourceVertex = graph.getMapIdToVertex().get(sourceId);
//            if (sourceVertex == null) {
//                throw new RuntimeException("In plateTransferEvent, no source vertex for " + sourceId);
//            }
//            Vertex destinationVertex = graph.getMapIdToVertex().get(destinationId);
//            if (destinationVertex == null) {
//                throw new RuntimeException("In plateTransferEvent, no destination vertex for " + destinationId);
//            }
//            graph.getMapIdToEdge().put(eventId, new Edge(buildEdgeLabel(plateTransferEvent),
//                    sourceVertex,
//                    destinationVertex));
//        }
//    }

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
}
