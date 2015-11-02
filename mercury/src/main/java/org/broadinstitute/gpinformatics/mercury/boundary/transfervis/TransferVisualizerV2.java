package org.broadinstitute.gpinformatics.mercury.boundary.transfervis;

import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.CherryPickTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.SectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.VesselToSectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselGeometry;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.text.Format;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Generates json to allow a javascript library to draw diagrams of transfers.
 */
public class TransferVisualizerV2 {

    private static final Log logger = LogFactory.getLog(TransferVisualizerV2.class);
    private static final Format DATE_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss");

    @Inject
    private BSPUserList bspUserList;

    private class Traverser implements TransferTraverserCriteria {
        public static final String REARRAY_LABEL = "rearray";
        public static final int ROW_HEIGHT = 20;
        public static final int PLATE_WIDTH = 480;
        public static final int WELL_WIDTH = 80;
        /** Accumulates JSON for graph nodes. */
        @SuppressWarnings("StringBufferField")
        private final StringBuilder nodesJson = new StringBuilder();
        /** Accumulates JSON for graph edges. */
        @SuppressWarnings("StringBufferField")
        private final StringBuilder edgesJson = new StringBuilder();
        /** Prevents vessels being rendered more than once. */
        private final Set<String> renderedLabels = new HashSet<>();
        /** Prevents events being rendered more than once. */
        private final Set<String> renderedEvents = new HashSet<>();
        /** Prevents multiple labels for a pool. */
        private final Set<String> renderedEdgeLabels = new HashSet<>();
        /** The ID to scroll to when the page is rendered.  If the starting barcode was a tube, this is one of the
         * enclosing racks (the tube may appear in multiple racks, so it can't be used as the start). */
        private String startId;

        @Override
        public TraversalControl evaluateVesselPreOrder(Context context) {
            if (context.getVesselContainer() == null) {
                renderVessel(context.getLabVessel());
            } else {
                renderContainer(context.getVesselContainer(), null, context.getLabVessel(), true);
            }
            if (context.getEvent() != null) {
                renderEvent(context.getEvent(), context.getLabVessel());
            }
            return TraversalControl.ContinueTraversing;
        }

        private void renderEvent(LabEvent event, LabVessel labVessel) {
            // Primary key for events
            String eventId = event.getEventLocation() + "|" + event.getEventDate().getTime() + "|" +
                    event.getDisambiguator();
            String label = buildEventLabel(event);

            if (renderedEvents.add(eventId)) {
                logger.info("Rendering event " + event.getLabEventType());
                for (SectionTransfer sectionTransfer : event.getSectionTransfers()) {
                    String sourceId = sectionTransfer.getSourceVesselContainer().getEmbedder().getLabel();
                    String targetId = sectionTransfer.getTargetVesselContainer().getEmbedder().getLabel();
                    renderContainer(sectionTransfer.getTargetVesselContainer(),
                            sectionTransfer.getAncillaryTargetVessel(), labVessel, false);
                    renderEdge(sourceId, targetId, label);
                }
                for (CherryPickTransfer cherryPickTransfer : event.getCherryPickTransfers()) {
                    LabVessel sourceVessel = cherryPickTransfer.getSourceVesselContainer().getVesselAtPosition(
                            cherryPickTransfer.getSourcePosition());
                    String sourceVesselLabel = sourceVessel == null ? cherryPickTransfer.getSourcePosition().name() :
                            sourceVessel.getLabel();
                    LabVessel targetVessel = cherryPickTransfer.getTargetVesselContainer().getVesselAtPosition(
                            cherryPickTransfer.getTargetPosition());
                    String targetVesselLabel = targetVessel == null ? cherryPickTransfer.getTargetPosition().name() :
                            targetVessel.getLabel();
                    // todo jmt handle plate wells
                    renderContainer(cherryPickTransfer.getSourceVesselContainer(),
                            cherryPickTransfer.getAncillarySourceVessel(), labVessel, false);
                    renderContainer(cherryPickTransfer.getTargetVesselContainer(),
                            cherryPickTransfer.getAncillaryTargetVessel(), labVessel, false);
                    renderEdge(cherryPickTransfer.getSourceVesselContainer().getEmbedder().getLabel(),
                            sourceVesselLabel,
                            cherryPickTransfer.getTargetVesselContainer().getEmbedder().getLabel(),
                            targetVesselLabel, label);
                }
                for (VesselToSectionTransfer vesselToSectionTransfer : event.getVesselToSectionTransfers()) {
                    String sourceLabel = vesselToSectionTransfer.getSourceVessel().getLabel();
                    Set<VesselContainer<?>> containers = vesselToSectionTransfer.getSourceVessel().getContainers();
                    if (!containers.isEmpty()) {
                        sourceLabel = containers.iterator().next().getEmbedder().getLabel();
                    }
                    renderContainer(vesselToSectionTransfer.getTargetVesselContainer(),
                            vesselToSectionTransfer.getAncillaryTargetVessel(), labVessel, false);
                    renderEdge(sourceLabel,
                            vesselToSectionTransfer.getTargetVesselContainer().getEmbedder().getLabel(), label);
                }
            }
        }

        @Nonnull
        private String buildEventLabel(LabEvent event) {
            StringBuilder labelBuilder = new StringBuilder();
            labelBuilder.append(event.getLabEventType().getName()).append(" ").
                    append(event.getEventLocation()).append(" ").
                    append(DATE_FORMAT.format(event.getEventDate()));
            if (bspUserList != null) {
                BspUser bspUser = bspUserList.getById(event.getEventOperator());
                if (bspUser != null) {
                    labelBuilder.append(" ").append(bspUser.getFullName());
                }
            }
            return labelBuilder.toString();
        }

        private void renderEdge(String sourceId, String targetId, String label) {
            edgesJson.append("{ \"source\": \"").append(sourceId).
                    append("\", \"target\": \"").append(targetId).
                    append("\", \"label\": \"").append(label).
                    append("\" },\n");
        }
        private void renderEdge(String sourceId, String sourceChild, String targetId, String targetChild, String label) {
            edgesJson.append("{ \"source\": \"").append(sourceId).
                    append("\", \"sourceChild\": \"").append(sourceChild).
                    append("\", \"target\": \"").append(targetId).
                    append("\", \"targetChild\": \"").append(targetChild);
            if (label.equals(REARRAY_LABEL)) {
                edgesJson.append("\", \"class\": \"graphEdgeDashed");
            }
            if (renderedEdgeLabels.add(sourceId + targetId + label)) {
                edgesJson.append("\", \"label\": \"").append(label);
            }
            edgesJson.append("\" },\n");
        }

        private void renderVessel(LabVessel labVessel) {
            String label = labVessel.getLabel();
            // todo jmt distinguish between stand-alone tube and container
//            if (mapLabelToIndex.get(label) == null) {
//                logger.info("Rendering vessel " + label);
//                mapLabelToIndex.put(label, nodeIndex);
//                nodeIndex++;
                for (VesselContainer<?> vesselContainer : labVessel.getContainers()) {
                    renderContainer(vesselContainer, null, labVessel, true);
                }
//            }
        }

        @Override
        public void evaluateVesselInOrder(Context context) {

        }

        @Override
        public void evaluateVesselPostOrder(Context context) {

        }

        private void renderContainer(VesselContainer<?> vesselContainer, LabVessel ancillaryVessel, LabVessel labVessel,
                boolean followRearrays) {
            String containerLabel = vesselContainer.getEmbedder().getLabel();
            String ancillaryLabel = null;
            if (ancillaryVessel == null) {
                if (OrmUtil.proxySafeIsInstance(vesselContainer.getEmbedder(), TubeFormation.class)) {
                    TubeFormation tubeFormation = OrmUtil.proxySafeCast(vesselContainer.getEmbedder(), TubeFormation.class);
                    if (!tubeFormation.getRacksOfTubes().isEmpty()) {
                        ancillaryLabel = tubeFormation.getRacksOfTubes().iterator().next().getLabel();
                    }
                }
                if (ancillaryLabel == null) {
                    ancillaryLabel = containerLabel;
                }
            } else {
                ancillaryLabel = ancillaryVessel.getLabel();
            }

            if (renderedLabels.add(containerLabel)) {
                logger.debug("Rendering container " + containerLabel);

                // JSON for child vessels (e.g. tubes in a rack)
                VesselGeometry vesselGeometry = vesselContainer.getEmbedder().getVesselGeometry();
                StringBuilder childBuilder = new StringBuilder();
                int maxColumn = 0;
                int maxRow = 0;
                int inPlaceHeight = vesselContainer.getEmbedder().getInPlaceLabEvents().size() * ROW_HEIGHT;
                for (VesselPosition vesselPosition : vesselGeometry.getVesselPositions()) {
                    VesselGeometry.RowColumn rowColumn = vesselGeometry.getRowColumnForVesselPosition(vesselPosition);
                    LabVessel child = vesselContainer.getVesselAtPosition(vesselPosition);
                    if (child != null) {
                        if (childBuilder.length() > 0) {
                            childBuilder.append(",");
                        }
                        childBuilder.append("{ \"name\": \"").append(child.getLabel()).
                                append("\", \"x\": ").append((rowColumn.getColumn() - 1) * WELL_WIDTH).
                                append(", \"y\": ").append((rowColumn.getRow()) * ROW_HEIGHT + inPlaceHeight).
                                append(", \"w\": ").append(WELL_WIDTH).append(", \"h\": ").append(ROW_HEIGHT);
                        if (child.equals(labVessel)) {
                            childBuilder.append(", \"highlight\": 1");
                        }
                        childBuilder.append("}");
                        maxColumn = Math.max(maxColumn, rowColumn.getColumn());
                        maxRow = Math.max(maxRow, rowColumn.getRow() + 1);
                    }
                }
                int width = Math.max(PLATE_WIDTH, maxColumn * WELL_WIDTH);
                int height = Math.max(ROW_HEIGHT, maxRow * ROW_HEIGHT) + inPlaceHeight;

                // JSON for in-place events
                StringBuilder eventBuilder = new StringBuilder();
                int inPlaceEventOffset = ROW_HEIGHT;
                for (LabEvent labEvent : vesselContainer.getEmbedder().getInPlaceLabEvents()) {
                    if (eventBuilder.length() > 0) {
                        eventBuilder.append(",");
                    }
                    eventBuilder.append("{ \"name\": \"").append(buildEventLabel(labEvent)).
                            append("\", \"x\": 0, \"y\": ").append(inPlaceEventOffset).
                            append(", \"w\": ").append(width).append(", \"h\": ").append(ROW_HEIGHT).append("}");
                    inPlaceEventOffset += ROW_HEIGHT;
                }

                // JSON for the parent vessel
                nodesJson.append("{ \"id\":\"").append(containerLabel).append("\", \"values\": {\"label\": \"").
                        append(ancillaryLabel).append("\", \"width\": ").append(width).append(", \"height\": ").
                        append(height).append(", \"children\": [");
                nodesJson.append(eventBuilder.toString());
                if (eventBuilder.length() > 0 && childBuilder.length() > 0) {
                    nodesJson.append(", ");
                }
                nodesJson.append(childBuilder.toString());
                nodesJson.append("] } },\n");
                if (startId == null) {
                    startId = containerLabel;
                }

                // JSON re-arrays
                if (labVessel != null && followRearrays) {
                    List<Rearray> rearrays = new ArrayList<>();
                    if (labVessel.getContainers().size() > 1) {
                        for (VesselContainer<?> otherContainer : labVessel.getContainers()) {
                            if (!renderedLabels.contains(otherContainer.getEmbedder().getLabel())) {
                                rearrays.add(new Rearray(labVessel, vesselContainer, otherContainer));
                            }
                        }
                    }
                    for (Rearray rearray : rearrays) {
                        rearray.render();
                    }
                }
            }
        }

        public String getJson() {
            String nodesJsonString = nodesJson.toString();
            if (nodesJsonString.endsWith(",\n")) {
                nodesJsonString = nodesJsonString.substring(0, nodesJsonString.length() - 2);
            }
            String linksJsonString = edgesJson.toString();
            if (linksJsonString.endsWith(",\n")) {
                linksJsonString = linksJsonString.substring(0, linksJsonString.length() - 2);
            }
            return "{ \"nodes\": [\n" + nodesJsonString + "],\n \"startId\":\"" + startId +
                    "\", \n \"links\": [\n" + linksJsonString + "  ] }";
        }

        private class Rearray {
            private LabVessel labVessel;
            private VesselContainer<?> vesselContainer;
            private VesselContainer<?> otherContainer;

            private Rearray(LabVessel labVessel, VesselContainer<?> vesselContainer, VesselContainer<?> otherContainer) {
                this.labVessel = labVessel;
                this.vesselContainer = vesselContainer;
                this.otherContainer = otherContainer;
            }

            private void render() {
                renderContainer(vesselContainer, null, labVessel, false);
                renderContainer(otherContainer, null, labVessel, false);
                VesselContainer<?> sourceContainer;
                VesselContainer<?> targetContainer;
                if (vesselContainer.getEmbedder().getCreatedOn().before(otherContainer.getEmbedder().getCreatedOn())) {
                    sourceContainer = vesselContainer;
                    targetContainer = otherContainer;
                } else {
                    sourceContainer = otherContainer;
                    targetContainer = vesselContainer;
                }
                renderEdge(sourceContainer.getEmbedder().getLabel(), labVessel.getLabel(),
                        targetContainer.getEmbedder().getLabel(), labVessel.getLabel(), REARRAY_LABEL);
            }
        }
    }

    public String jsonForVessels(List<LabVessel> labVessels,
            List<TransferTraverserCriteria.TraversalDirection> traversalDirections) {
        if (traversalDirections.isEmpty()) {
            throw new IllegalArgumentException("Must supply at least one direction");
        }
        Traverser traverser = new Traverser();
        for (LabVessel labVessel : labVessels) {
            for (TransferTraverserCriteria.TraversalDirection traversalDirection : traversalDirections) {
                VesselContainer<?> containerRole = labVessel.getContainerRole();
                if (containerRole == null) {
                    labVessel.evaluateCriteria(traverser, traversalDirection);
                } else {
                    containerRole.evaluateCriteria(labVessel.getVesselGeometry().getVesselPositions()[0], traverser,
                            traversalDirection, null, 0);
                }
            }
        }
        return traverser.getJson();
    }

}
