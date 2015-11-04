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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.io.IOException;
import java.io.Writer;
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

    private class Traverser extends TransferTraverserCriteria {
        public static final String REARRAY_LABEL = "rearray";
        public static final int ROW_HEIGHT = 20;
        public static final int PLATE_WIDTH = 480;
        public static final int WELL_WIDTH = 80;

        /** Stream to browser. */
        private Writer writer;
        /** The stream to which to write JSON. */
        private JSONWriter jsonWriter;
        /** Accumulates JSON for graph edges (nodes are streamed).*/
        private JSONArray edgesJson = new JSONArray();
        /** Prevents vessels being rendered more than once. */
        private final Set<String> renderedLabels = new HashSet<>();
        /** Prevents events being rendered more than once. */
        private final Set<String> renderedEvents = new HashSet<>();
        /** Prevents multiple labels for a pool. */
        private final Set<String> renderedEdgeLabels = new HashSet<>();
        /** The ID to scroll to when the page is rendered.  If the starting barcode was a tube, this is one of the
         * enclosing racks (the tube may appear in multiple racks, so it can't be used as the start). */
        private String startId;

        Traverser(Writer writer) throws JSONException {
            this.writer = writer;
            jsonWriter = new JSONWriter(writer);
            jsonWriter.object().key("nodes").array();
        }

        @Override
        public TraversalControl evaluateVesselPreOrder(Context context) {
            try {
                if (context.getContextVesselContainer() == null) {
                    renderVessel(context.getContextVessel());
                } else {
                    renderContainer(context.getContextVesselContainer(), null, context.getContextVessel(), true);
                }
                if (context.getVesselEvent() != null && context.getVesselEvent().getLabEvent() != null) {
                    renderEvent(context.getVesselEvent().getLabEvent(), context.getContextVessel());
                }
                return TraversalControl.ContinueTraversing;
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }

        private void renderEvent(LabEvent event, LabVessel labVessel) throws JSONException {
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

        private void renderEdge(String sourceId, String targetId, String label) throws JSONException {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("source", sourceId).
                    put("target", targetId).
                    put("label", label);
            edgesJson.put(jsonObject);
        }
        private void renderEdge(String sourceId, String sourceChild, String targetId, String targetChild, String label)
                throws JSONException {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("source", sourceId).
                    put("sourceChild", sourceChild).
                    put("target", targetId).
                    put("targetChild", targetChild);
            if (label.equals(REARRAY_LABEL)) {
                jsonObject.put("class", "graphEdgeDashed");
            }
            if (renderedEdgeLabels.add(sourceId + targetId + label)) {
                jsonObject.put("label", label);
            }
            edgesJson.put(jsonObject);
        }

        private void renderVessel(LabVessel labVessel) throws JSONException {
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
        public void evaluateVesselPostOrder(Context context) {

        }

        private void renderContainer(VesselContainer<?> vesselContainer, LabVessel ancillaryVessel, LabVessel labVessel,
                boolean followRearrays) throws JSONException {
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
                int maxColumn = 0;
                int maxRow = 0;
                int inPlaceHeight = vesselContainer.getEmbedder().getInPlaceLabEvents().size() * ROW_HEIGHT;
                for (VesselPosition vesselPosition : vesselGeometry.getVesselPositions()) {
                    VesselGeometry.RowColumn rowColumn = vesselGeometry.getRowColumnForVesselPosition(vesselPosition);
                    LabVessel child = vesselContainer.getVesselAtPosition(vesselPosition);
                    if (child != null) {
                        maxColumn = Math.max(maxColumn, rowColumn.getColumn());
                        maxRow = Math.max(maxRow, rowColumn.getRow() + 1);
                    }
                }
                int width = Math.max(PLATE_WIDTH, maxColumn * WELL_WIDTH);
                int height = Math.max(ROW_HEIGHT, maxRow * ROW_HEIGHT) + inPlaceHeight;

                // JSON for the parent vessel
                jsonWriter.object().key("id").value(containerLabel).key("values").object().
                        key("label").value(ancillaryLabel).
                        key("width").value(width).
                        key("height").value(height).
                        key("children").array();

                // JSON for in-place events
                int inPlaceEventOffset = ROW_HEIGHT;
                for (LabEvent labEvent : vesselContainer.getEmbedder().getInPlaceLabEvents()) {
                    jsonWriter.object().
                            key("name").value(buildEventLabel(labEvent)).
                            key("x").value(0L).key("y").value(inPlaceEventOffset).
                            key("w").value(width).key("h").value(ROW_HEIGHT).
                            endObject();
                    inPlaceEventOffset += ROW_HEIGHT;
                }

                for (VesselPosition vesselPosition : vesselGeometry.getVesselPositions()) {
                    VesselGeometry.RowColumn rowColumn = vesselGeometry.getRowColumnForVesselPosition(vesselPosition);
                    LabVessel child = vesselContainer.getVesselAtPosition(vesselPosition);
                    if (child != null) {
                        jsonWriter.object().
                                key("name").value(child.getLabel()).
                                key("x").value((rowColumn.getColumn() - 1) * WELL_WIDTH).
                                key("y").value((rowColumn.getRow()) * ROW_HEIGHT + inPlaceHeight).
                                key("w").value(WELL_WIDTH).key("h").value(ROW_HEIGHT);
                        if (child.equals(labVessel)) {
                            jsonWriter.key("highlight").value(1L);
                        }
                        jsonWriter.endObject();
                    }
                }
                jsonWriter.endArray().endObject().endObject();
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

        public void completeJson() {
            try {
                jsonWriter.endArray().key("startId").value(startId).key("links");
                edgesJson.write(writer);
                writer.write(" }");
                writer.flush();
            } catch (JSONException | IOException e) {
                throw new RuntimeException(e);
            }
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

            private void render() throws JSONException {
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

    public void jsonForVessels(List<LabVessel> labVessels,
            List<TransferTraverserCriteria.TraversalDirection> traversalDirections,
            Writer writer) {
        if (traversalDirections.isEmpty()) {
            throw new IllegalArgumentException("Must supply at least one direction");
        }
        try {
            Traverser traverser = new Traverser(writer);
            for (LabVessel labVessel : labVessels) {
                for (TransferTraverserCriteria.TraversalDirection traversalDirection : traversalDirections) {
                    VesselContainer<?> containerRole = labVessel.getContainerRole();
                    if (containerRole == null) {
                        labVessel.evaluateCriteria(traverser, traversalDirection);
                    } else {
                        containerRole.evaluateCriteria(labVessel.getVesselGeometry().getVesselPositions()[0], traverser,
                                traversalDirection, 0);
                    }
                    traverser.resetAllTraversed();
                }
            }
            traverser.completeJson();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

}
