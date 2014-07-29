package org.broadinstitute.gpinformatics.mercury.boundary.transfervis;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.mercury.boundary.graph.Edge;
import org.broadinstitute.gpinformatics.mercury.boundary.graph.Graph;
import org.broadinstitute.gpinformatics.mercury.boundary.graph.Vertex;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.StaticPlateDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.BarcodedTubeDao;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.CherryPickTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.SectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.VesselToSectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainerEmbedder;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselGeometry;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import javax.ejb.Remote;
import javax.ejb.Stateful;
import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
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

    /**
     * How many additional vessels to render each time the user clicks "More Transfers".  Larger numbers are
     * confusing, because too much new stuff is disorientating, but small numbers require more clicks.
     */
    private int maxNumVesselsPerRequest = 1;

    @Inject
    private StaticPlateDao staticPlateDao;

    @Inject
    private BarcodedTubeDao barcodedTubeDao;

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

    private final Map<IdType, ExpandVertex> mapIdTypeToExpandVertex = new EnumMap<>(IdType.class);

    void initMap() {
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
                BarcodedTube receptacle = barcodedTubeDao.findByBarcode(vertexId);
                transferEntityGrapher.startWithTube(receptacle, graph, alternativeIds);
            }
        });
    }

    @Override
    public Graph forContainer(String containerBarcode, List<AlternativeId> alternativeIds) {
        Graph graph = new Graph();
        LabVessel labVessel = labVesselDao.findByIdentifier(containerBarcode);
        if (labVessel == null) {
            graph.setMessage("No container was found with that barcode");
        } else if (OrmUtil.proxySafeIsInstance(labVessel, VesselContainerEmbedder.class)) {
            startWithContainer(labVessel.getContainerRole(), graph, alternativeIds);
        } else {
            graph.setMessage("The barcode is not attached to a container");
        }
        return graph;
    }

//    private void startWithWellMap(WellMap wellMap, Graph graph, List<AlternativeId> alternativeIds) {
//        Queue<VesselVertex> vesselQueue = new LinkedList<VesselVertex>();
//
//        vesselQueue.add(new ContainerVertex(wellMap));
//        processQueue(vesselQueue, graph, alternativeIds);
//    }

    /**
     * Build a graph, starting with a tube barcode
     *
     * @param tubeBarcode    tube the user is searching on
     * @param alternativeIds which IDs to include in the vertices
     *
     * @return vertices and edges
     */
    @Override
    public Graph forTube(String tubeBarcode, List<AlternativeId> alternativeIds) {
        Graph graph = new Graph();
        BarcodedTube receptacle = barcodedTubeDao.findByBarcode(tubeBarcode);
        startWithTube(receptacle, graph, alternativeIds);
        return graph;
    }

    /**
     * Starting point for rendering receptacles, searched by barcode, GSSR sample, library name
     *
     * @param graph          empty
     * @param receptacle     starting point for filling graph
     * @param alternativeIds IDs to display
     */
    public void startWithTube(BarcodedTube receptacle, Graph graph, List<AlternativeId> alternativeIds) {
        if (receptacle == null) {
            graph.setMessage("No tube was found with that barcode");
        } else {
            Queue<VesselVertex> vesselVertexQueue = new LinkedList<>();

            for (VesselContainer<?> vesselContainer : receptacle.getContainers()) {
                vesselVertexQueue.add(new ContainerVertex(vesselContainer));
            }
            vesselVertexQueue.add(new ReceptacleVesselVertex(receptacle));
            processQueue(vesselVertexQueue, graph, alternativeIds);
        }
    }

    public void startWithContainer(VesselContainer<?> vesselContainer, Graph graph,
                                   List<AlternativeId> alternativeIds) {
        if (vesselContainer == null) {
            graph.setMessage("No container was found with that barcode");
        } else {
            Queue<VesselVertex> vesselVertexQueue = new LinkedList<>();

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
     *
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
     *
     * @param tubeBarcode barcode of the tube the user is searching on
     *
     * @return map from AlternativeId displayName to list of IDs
     */
    @Override
    public Map<String, List<String>> getIdsForTube(String tubeBarcode) {
        BarcodedTube receptacle = barcodedTubeDao.findByBarcode(tubeBarcode);
        return getAlternativeIds(receptacle, receptacle.getSampleInstances(), Arrays.asList(AlternativeId.values()));
    }

    /**
     * Builds a label for an edge, with event name etc.
     *
     * @param labEvent holds details of the event
     *
     * @return HTML
     */
    private String buildEdgeLabel(LabEvent labEvent) {
        StringBuilder label = new StringBuilder();
        label.append("<html>");
        String eventTypeName = labEvent.getLabEventType().getName();
//        if (!ReceptacleTransferEvent.TRANSFER_EVENT.equals(eventTypeName)) {
        label.append(eventTypeName);
//            if (labEvent instanceof PlateTransferEvent) {
//                PlateTransferEvent plateTransferEvent = (PlateTransferEvent) labEvent;
//                label.append("<br/>");
//                label.append(plateTransferEvent.getSourceSectionLayout());
//                label.append(" to ");
//                label.append(plateTransferEvent.getSectionLayout());
//            }
        label.append("<br/>");
//        }
        String labMachineName = labEvent.getEventLocation();
        if (!labMachineName.contains("Unknown")) {
            label.append(labMachineName).append(", ");
        }
        label.append(labEvent.getEventDate());
        label.append("<br/>");
        if (bspUserList != null) {
            BspUser bspUser = bspUserList.getById(labEvent.getEventOperator());
            if (bspUser != null) {
                label.append(bspUser.getFullName());
            }
            label.append("<br/>");
        }
        if (!labEvent.getComputedLcSets().isEmpty()) {
            for (LabBatch labBatch : labEvent.getComputedLcSets()) {
                label.append(labBatch.getBatchName());
                label.append(", ");
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
         *
         * @return true if the vertex is new, false if it was already in the graph
         */
        abstract boolean render(Graph graph, List<AlternativeId> alternativeIds);

        /**
         * Insert the vessel's transfers as edges in the graph.  For each transfer, add to the queue the vessel at
         * the opposite end of the transfer.
         *
         * @param graph             existing edges and vertices
         * @param vesselVertexQueue queue to which to add (new) vessels at opposite end of each transfer (vessel is not
         *                          added if it's already in the graph)
         * @param alternativeIds    IDs the user wants rendered
         *
         * @return number of vessels added to the queue
         */
        public abstract int renderEdges(Graph graph, Queue<VesselVertex> vesselVertexQueue,
                                        List<AlternativeId> alternativeIds);

        public Vertex getVertex() {
            return vertex;
        }

        public void setVertex(Vertex vertex) {
            this.vertex = vertex;
        }
    }


    private int renderReceptacleEdges(LabVessel receptacle, Graph graph, Queue<VesselVertex> vesselVertexQueue,
                                      List<AlternativeId> alternativeIds) {
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
        for (VesselToSectionTransfer vesselToSectionTransfer : receptacle.getVesselToSectionTransfersThisAsSource()) {
            ContainerVertex plateVessel = new ContainerVertex(vesselToSectionTransfer.getTargetVesselContainer());
            if (plateVessel.render(graph, alternativeIds)) {
                vesselVertexQueue.add(plateVessel);
                numVesselsAddedReturn++;
            }
            renderEdge(graph, vesselToSectionTransfer);
        }
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
        if (receptacle.getContainers().size() > 1) {
            Iterator<VesselContainer<?>> iterator = receptacle.getContainers().iterator();
            VesselContainer<?> sourceContainer = iterator.next();
            ContainerVertex sourceContainerVertex = new ContainerVertex(sourceContainer);
            if (sourceContainerVertex.render(graph, alternativeIds)) {
                vesselVertexQueue.add(sourceContainerVertex);
                numVesselsAddedReturn++;
            }
            while (iterator.hasNext()) {
                VesselContainer<?> destinationContainer = iterator.next();
                ContainerVertex destinationContainerVertex = new ContainerVertex(destinationContainer);
                if (destinationContainerVertex.render(graph, alternativeIds)) {
                    vesselVertexQueue.add(destinationContainerVertex);
                    numVesselsAddedReturn++;
                }
                if (sourceContainer.getEmbedder().getCreatedOn().after(sourceContainer.getEmbedder().getCreatedOn())) {
                    renderRearrayEdge(graph, destinationContainer, sourceContainer, receptacle);
                } else {
                    renderRearrayEdge(graph, sourceContainer, destinationContainer, receptacle);
                }
            }
        }
        return numVesselsAddedReturn;
    }

    /**
     * Insert an edge for a receptacle re-array, i.e. a tube moved from one rack to another
     *
     * @param graph                edges and vertices
     * @param sourceContainer      source of re-array
     * @param destinationContainer destination of re-array
     */
    private void renderRearrayEdge(Graph graph, VesselContainer<?> sourceContainer,
                                   VesselContainer<?> destinationContainer, LabVessel containee) {
        // There's no event, so create a fake event ID
        String eventId =
                sourceContainer.getEmbedder().getLabel() + "|" + destinationContainer.getEmbedder().getLabel() +
                "|" + containee.getLabel();
        if (graph.getVisitedEventIds().add(eventId)) {
            Vertex sourceReceptacleVertex = graph.getMapIdToVertex().get(containee.getLabel() + "|" +
                                                                         sourceContainer.getEmbedder().getLabel());
            Vertex destinationReceptacleVertex = graph.getMapIdToVertex().get(containee.getLabel() + "|" +
                                                                              destinationContainer.getEmbedder()
                                                                                      .getLabel());
            graph.getMapIdToEdge().put(eventId, new Edge("Rearray",
                    sourceReceptacleVertex, destinationReceptacleVertex, Edge.LineType.DASHED));
        }
    }

    private class ContainerVertex extends VesselVertex {

        private VesselContainer<?> vesselContainer;

        private ContainerVertex(VesselContainer<?> vesselContainer) {
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
                // todo jmt set these based on the actual layout of tubes
                VesselGeometry vesselGeometry = vesselContainer.getEmbedder().getVesselGeometry();
                int maxRowNumber = vesselGeometry.getRowNames().length;
                int maxColumnNumber = vesselGeometry.getColumnNames().length;

                // todo jmt fix rack type
                rackVertex = new Vertex(label, IdType.CONTAINER_ID_TYPE.toString(),
                        /*vesselContainer.getRackType().getAutomationName() +*/ " : " +
                                                                             vesselContainer.getEmbedder()
                                                                                     .getLabCentricName(),
                        maxRowNumber, maxColumnNumber);
                for (VesselPosition vesselPosition : vesselGeometry.getVesselPositions()) {
                    LabVessel receptacle = vesselContainer.getVesselAtPosition(vesselPosition);
                    String barcode = receptacle == null ? vesselPosition.name() : receptacle.getLabel();
                    Vertex tubeVertex = new Vertex(barcode + "|" + label, IdType.TUBE_IN_RACK_ID_TYPE.toString(),
                            barcode, rackVertex, getAlternativeIds(receptacle, receptacle == null ?
                            Collections.<SampleInstance>emptySet() : receptacle.getSampleInstances(), alternativeIds));
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
            numVesselsAdded +=
                    renderSectionTransfers(graph, vesselVertexQueue, alternativeIds, sectionTransfersThisAsSource,
                            sectionTransfersThisAsTarget);
            numVesselsAdded += renderCherryPickTransfers(graph, vesselVertexQueue, alternativeIds,
                    vesselContainer.getCherryPickTransfersFrom());
            numVesselsAdded += renderCherryPickTransfers(graph, vesselVertexQueue, alternativeIds,
                    vesselContainer.getCherryPickTransfersTo());

            // Render any transfers from the rack tubes
            for (Object o : vesselContainer.getMapPositionToVessel().entrySet()) {
                Map.Entry<VesselPosition, LabVessel> vesselPositionBarcodedTubeEntry =
                        (Map.Entry<VesselPosition, LabVessel>) o;
                numVesselsAdded += renderReceptacleEdges(vesselPositionBarcodedTubeEntry.getValue(),
                        graph, vesselVertexQueue, alternativeIds);
            }
            for (Object o : vesselContainer.getVesselToSectionTransfersTo()) {
                VesselToSectionTransfer vesselToSectionTransfer = (VesselToSectionTransfer) o;
                ReceptacleVesselVertex receptacleVessel = new ReceptacleVesselVertex(
                        OrmUtil.proxySafeCast(vesselToSectionTransfer.getSourceVessel(), BarcodedTube.class));
                if (receptacleVessel.render(graph, alternativeIds)) {
                    vesselVertexQueue.add(receptacleVessel);
                    numVesselsAdded++;
                }
                renderEdge(graph, vesselToSectionTransfer);
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

        private BarcodedTube receptacle;

        private ReceptacleVesselVertex(BarcodedTube receptacle) {
            this.receptacle = receptacle;
        }

        @Override
        boolean render(Graph graph, List<AlternativeId> alternativeIds) {
//            System.out.println("Rendering receptacle " + receptacle.getBarcode());
            boolean newVertex = false;
            Vertex vertex = graph.getMapIdToVertex().get(receptacle.getLabel());
            if (vertex == null) {
                newVertex = true;
                vertex = new Vertex(receptacle.getLabel(), IdType.RECEPTACLE_ID_TYPE.toString(),
                        buildReceptacleLabel(), getAlternativeIds(receptacle, receptacle.getSampleInstances(), alternativeIds));
                graph.getMapIdToVertex().put(vertex.getId(), vertex);
                vertex.setHasMoreEdges(true);
            }
            setVertex(vertex);
            return newVertex;
        }

        @Override
        public int renderEdges(Graph graph, Queue<VesselVertex> vesselVertexQueue, List<AlternativeId> alternativeIds) {
            return renderReceptacleEdges(receptacle, graph, vesselVertexQueue, alternativeIds);
        }

        private String buildReceptacleLabel() {
            String contentTypeName = "";
//            if (receptacle.getCurrentContent() != null /*&& receptacle.getCurrentContent().getTypeId() != null*/) {
//                contentTypeName = receptacle.getCurrentContent().getLibraryTypeName();
//            }
            return "Tube : " + receptacle.getLabel() + "<br/>" + contentTypeName;
        }
    }

    private int renderSectionTransfers(Graph graph, Queue<VesselVertex> vesselVertexQueue,
                                       List<AlternativeId> alternativeIds,
                                       Set<SectionTransfer> sourceSectionTransfers,
                                       Set<SectionTransfer> targetSectionTransfers) {
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

    private int renderCherryPickTransfers(Graph graph, Queue<VesselVertex> vesselVertexQueue,
                                          List<AlternativeId> alternativeIds,
                                          Set<CherryPickTransfer> cherryPickTransfers) {
        int numVesselsAdded = 0;
        for (CherryPickTransfer cherryPickTransfer : cherryPickTransfers) {
            String eventId = cherryPickTransfer.getKey();
            if (graph.getVisitedEventIds().add(eventId)) {
                numVesselsAdded += renderEmbedder(graph, vesselVertexQueue, alternativeIds,
                        cherryPickTransfer.getTargetVesselContainer().getEmbedder());
                numVesselsAdded += renderEmbedder(graph, vesselVertexQueue, alternativeIds,
                        cherryPickTransfer.getSourceVesselContainer().getEmbedder());

                LabVessel receptacle =  cherryPickTransfer.getSourceVesselContainer().getVesselAtPosition(
                        cherryPickTransfer.getSourcePosition());
                String barcode = receptacle == null ? cherryPickTransfer.getSourcePosition().name() :
                        receptacle.getLabel();
                Vertex sourceReceptacleVertex = graph.getMapIdToVertex().get(barcode + "|" +
                        cherryPickTransfer.getSourceVesselContainer().getEmbedder().getLabel());
                LabVessel vesselAtPosition = cherryPickTransfer.getTargetVesselContainer()
                        .getVesselAtPosition(cherryPickTransfer.getTargetPosition());
                Vertex destinationReceptacleVertex = graph.getMapIdToVertex().get(
                        (vesselAtPosition == null ? cherryPickTransfer.getTargetPosition() :
                                vesselAtPosition.getLabel()) + "|" +
                        cherryPickTransfer.getTargetVesselContainer().getEmbedder().getLabel());
                graph.getMapIdToEdge().put(eventId, new Edge(buildEdgeLabel(cherryPickTransfer.getLabEvent()),
                        sourceReceptacleVertex, destinationReceptacleVertex));
            }
        }

        return numVesselsAdded;
    }

    private int renderEmbedder(Graph graph, Queue<VesselVertex> vesselVertexQueue, List<AlternativeId> alternativeIds,
                               LabVessel embedder) {
        int numVesselsAdded = 0;
        switch (embedder.getType()) {
        case STATIC_PLATE:
        case TUBE_FORMATION:
        case STRIP_TUBE:
        case FLOWCELL:
        case MISEQ_REAGENT_KIT:
            ContainerVertex containerVertex = new ContainerVertex(embedder.getContainerRole());
            if (containerVertex.render(graph, alternativeIds)) {
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
     * @param graph           edges and vertices
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
     * @param graph                   edges and vertices
     * @param vesselToSectionTransfer source and destination
     */
    private void renderEdge(Graph graph, VesselToSectionTransfer vesselToSectionTransfer) {
        String eventId = vesselToSectionTransfer.getKey();
        if (graph.getVisitedEventIds().add(eventId)) {
            Vertex sourceVertex = graph.getMapIdToVertex().get(
                    vesselToSectionTransfer.getSourceVessel().getLabel());
            if (sourceVertex == null) {
                throw new RuntimeException("In vesselToSectionTransfer, no source vertex for " +
                                           vesselToSectionTransfer.getSourceVessel().getLabel());
            }
            Vertex destinationVertex = graph.getMapIdToVertex().get(vesselToSectionTransfer.
                    getTargetVesselContainer().getEmbedder().getLabel());
            if (destinationVertex == null) {
                throw new RuntimeException("In vesselToSectionTransfer, no destination vertex for " +
                                           vesselToSectionTransfer.getTargetVesselContainer().getEmbedder().getLabel());
            }
            graph.getMapIdToEdge().put(eventId, new Edge(buildEdgeLabel(vesselToSectionTransfer.getLabEvent()),
                    sourceVertex,
                    destinationVertex));
        }
    }

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
     *
     * @param labVessel
     * @param sampleInstances   sample details
     * @param alternativeIdList list of ID types specified by user
     *
     * @return list of Ids for the given receptacle
     */
    private Map<String, List<String>> getAlternativeIds(
            LabVessel labVessel, Set<SampleInstance> sampleInstances, List<AlternativeId> alternativeIdList) {
        Map<String, List<String>> alternativeIdValues = new HashMap<>();
        for (SampleInstance sampleInstance : sampleInstances) {
            if (alternativeIdList.contains(AlternativeId.SAMPLE_ID)) {
                MercurySample startingSample = sampleInstance.getStartingSample();
                if (startingSample != null) {
                    Vertex.addAlternativeId(alternativeIdValues, AlternativeId.SAMPLE_ID.getDisplayName(),
                            startingSample.getSampleKey());
                }
            }
            if (alternativeIdList.contains(AlternativeId.LCSET)) {
                LabBatch labBatch = sampleInstance.getLabBatch();
                if (labBatch != null) {
                    Vertex.addAlternativeId(alternativeIdValues, AlternativeId.LCSET.getDisplayName(),
                            labBatch.getBatchName());
                }
            }
            if (alternativeIdList.contains(AlternativeId.BUCKET_ENTRY)) {
                for (BucketEntry bucketEntry : labVessel.getBucketEntries()) {
                    if (bucketEntry.getLabBatch() != null) {
                        Vertex.addAlternativeId(alternativeIdValues, AlternativeId.LCSET.getDisplayName(),
                                bucketEntry.getLabBatch().getBatchName());
                    }
                }
            }
        }
        return alternativeIdValues;
    }

    public void setMaxNumVesselsPerRequest(int maxNumVesselsPerRequest) {
        this.maxNumVesselsPerRequest = maxNumVesselsPerRequest;
    }
}
