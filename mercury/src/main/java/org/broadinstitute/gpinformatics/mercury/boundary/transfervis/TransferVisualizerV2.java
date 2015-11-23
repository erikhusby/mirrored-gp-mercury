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
import org.broadinstitute.gpinformatics.mercury.entity.labevent.VesselToVesselTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselGeometry;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Writer;
import java.text.Format;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Generates JSON to allow a javascript library to draw diagrams of transfers.  The JSON has the following structure:
 * <ul>
 * <li>array of nodes, each has:
 *      <ul>
 *      <li>id - unique, the label from the LabVessel (the hash for a TubeFormation)</li>
 *      <li>label - more readable, usually the ancillary vessel (Rack) if a TubeFormation</li>
 *      <li>width, height - including children</li>
 *      <li>array of children, each has:
 *          <ul>
 *          <li>label - for tubes</li>
 *          <li>name - for in-place events</li>
 *          <li>x, y - relative to the parent</li>
 *          <li>w, h - usually small, for a tube, but can be wide for an in-place event</li>
 *          <li>highlight - 1 if the child is a direct ancestor or descendant of the searched tube</li>
 *          </ul>
 *      </li>
 *      </ul>
 * </li>
 * <li>startId</li>
 * <li>array of links, each has
 *      <ul>
 *      <li>label - who, what, where, when from LabEvent</li>
 *      <li>source - rack or plate id</li>
 *      <li>sourceChild - tube label</li>
 *      <li>target - rack or plate id</li>
 *      <li>targetChild - tube label</li>
 *      <li>class - to make re-arrays dashed</li>
 *      </ul>
 * </li>
 * </ul>
 */
public class TransferVisualizerV2 {

    private static final Log logger = LogFactory.getLog(TransferVisualizerV2.class);
    private static final Format DATE_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss");

    public enum AlternativeIds {
        SAMPLE_ID("Sample Id"),
        LCSET("LCSET");

        private String displayName;

        AlternativeIds(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    @Inject
    private BSPUserList bspUserList;

    private class Traverser extends TransferTraverserCriteria {
        public static final String REARRAY_LABEL = "rearray";
        public static final int ROW_HEIGHT = 16;
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
        /** Prevents multiple edge labels for a pool. */
        private final Set<String> renderedEdgeLabels = new HashSet<>();
        /** The ID to scroll to when the page is rendered.  If the starting barcode was a tube, this is one of the
         * enclosing racks (the tube may appear in multiple racks, so it can't be used as the start). */
        private String startId;
        /** The IDs to add to each rect. */
        private List<AlternativeIds> alternativeIds;
        /** Used to determine size of text elements. */
        private FontMetrics fontMetrics = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).getGraphics().
                getFontMetrics(new Font("SansSerif", Font.PLAIN, 12));

        Traverser(Writer writer, List<AlternativeIds> alternativeIds) throws JSONException {
            this.writer = writer;
            this.alternativeIds = alternativeIds;
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

        /**
         * Render a transfer event.
         */
        private void renderEvent(LabEvent event, LabVessel labVessel) throws JSONException {
            // Primary key for events
            String eventId = event.getEventLocation() + "|" + event.getEventDate().getTime() + "|" +
                    event.getDisambiguator();
            String label = buildEventLabel(event);

            if (renderedEvents.add(eventId)) {
                logger.debug("Rendering event " + event.getLabEventType());
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
                for (VesselToVesselTransfer vesselToVesselTransfer : event.getVesselToVesselTransfers()) {
                    LabVessel sourceVessel = vesselToVesselTransfer.getSourceVessel();
                    renderVessel(sourceVessel);
                    LabVessel targetVessel = vesselToVesselTransfer.getTargetVessel();
                    renderVessel(targetVessel);

                    renderEdge(sourceVessel.getLabel(), targetVessel.getLabel(), label);
                    // Target might be in a rack in another message
                    for (VesselContainer<?> otherContainer : targetVessel.getContainers()) {
                        renderEdge(targetVessel.getLabel(), null, otherContainer.getEmbedder().getLabel(),
                                targetVessel.getLabel(), REARRAY_LABEL);
                    }
                }
            }
        }

        /**
         * Construct a label for an event (transfer or in-place).
         */
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

        /**
         * Render an edge between two containers (a section transfer) or two tubes.
         */
        private void renderEdge(String sourceId, String targetId, String label) throws JSONException {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("source", sourceId).
                    put("target", targetId).
                    put("label", label);
            edgesJson.put(jsonObject);
        }

        /**
         * Render an edge between tubes in racks (a cherry pick or a re-array).
         */
        private void renderEdge(String sourceId, @Nullable String sourceChild, String targetId, String targetChild,
                String label)
                throws JSONException {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("source", sourceId);
            if (sourceChild != null) {
                jsonObject.put("sourceChild", sourceChild);
            }
            jsonObject.put("target", targetId).
                    put("targetChild", targetChild);
            if (label.equals(REARRAY_LABEL)) {
                jsonObject.put("class", "graphEdgeDashed");
            }
            if (renderedEdgeLabels.add(sourceId + targetId + label)) {
                jsonObject.put("label", label);
            }
            edgesJson.put(jsonObject);
        }

        /**
         * Render a stand-alone tube.
         */
        private void renderVessel(LabVessel labVessel) throws JSONException {
            if (renderedLabels.add(labVessel.getLabel())) {
                // todo jmt add alternative IDs
                jsonWriter.object().key("id").value(labVessel.getLabel()).
                        key("label").value(labVessel.getLabel()).
                        key("width").value(WELL_WIDTH).
                        key("height").value(ROW_HEIGHT).
                        key("children").
                        array().endArray().endObject();
            }
            for (VesselContainer<?> vesselContainer : labVessel.getContainers()) {
                renderContainer(vesselContainer, null, labVessel, true);
            }
        }

        @Override
        public void evaluateVesselPostOrder(Context context) {

        }

        /**
         * Render a container, e.g. a rack of tubes.
         */
        @SuppressWarnings("ImplicitNumericConversion")
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
                int maxColumnWidth = WELL_WIDTH;
                int maxRow = 0;
                int maxRowHeight = ROW_HEIGHT;
                int inPlaceHeight = vesselContainer.getEmbedder().getInPlaceLabEvents().size() * ROW_HEIGHT;
                Map<String, List<String>> mapBarcodeToAlternativeIds = new HashMap<>();
                for (VesselPosition vesselPosition : vesselGeometry.getVesselPositions()) {
                    VesselGeometry.RowColumn rowColumn = vesselGeometry.getRowColumnForVesselPosition(vesselPosition);
                    LabVessel child = vesselContainer.getVesselAtPosition(vesselPosition);
                    if (child != null) {
                        // todo jmt popup if more than one sample?
                        maxColumn = Math.max(maxColumn, rowColumn.getColumn());
                        maxRow = Math.max(maxRow, rowColumn.getRow() + 1);
                        for (AlternativeIds alternativeId : alternativeIds) {
                            switch (alternativeId) {
                                case SAMPLE_ID:
                                    Set<SampleInstanceV2> sampleInstances = child.getSampleInstancesV2();
                                    if (sampleInstances.size() == 1) {
                                        SampleInstanceV2 sampleInstance = sampleInstances.iterator().next();
                                        String indexName = sampleInstance.getMolecularIndexingScheme() == null ? "" :
                                                " " + sampleInstance.getMolecularIndexingScheme().getName();
                                        String ids = sampleInstance.getNearestMercurySampleName() + indexName;

                                        List<String> idsList = mapBarcodeToAlternativeIds.get(child.getLabel());
                                        if (idsList == null) {
                                            idsList = new ArrayList<>();
                                            mapBarcodeToAlternativeIds.put(child.getLabel(), idsList);
                                        }
                                        idsList.add(ids);

                                        int width = fontMetrics.stringWidth(ids);
                                        maxColumnWidth = Math.max(width, maxColumnWidth);
                                        maxRowHeight = Math.max(ROW_HEIGHT * (idsList.size() + 1), maxRowHeight);
                                    }
                                    break;
                                case LCSET:
                                    Set<LabBatch> labBatches = new HashSet<>();
                                    for (SampleInstanceV2 sampleInstance : child.getSampleInstancesV2()) {
                                        LabBatch singleBatch = sampleInstance.getSingleBatch();
                                        if (singleBatch != null) {
                                            labBatches.add(singleBatch);
                                        }
                                    }
                                    if (!labBatches.isEmpty()) {
                                        StringBuilder idsBuilder = new StringBuilder();
                                        for (LabBatch labBatch : labBatches) {
                                            if (idsBuilder.length() > 0) {
                                                idsBuilder.append(", ");
                                            }
                                            idsBuilder.append(labBatch.getBatchName());
                                        }

                                        // todo jmt reduce copy / paste
                                        List<String> idsList = mapBarcodeToAlternativeIds.get(child.getLabel());
                                        if (idsList == null) {
                                            idsList = new ArrayList<>();
                                            mapBarcodeToAlternativeIds.put(child.getLabel(), idsList);
                                        }
                                        String ids = idsBuilder.toString();
                                        idsList.add(ids);

                                        int width = fontMetrics.stringWidth(ids);
                                        maxColumnWidth = Math.max(width, maxColumnWidth);
                                        maxRowHeight = Math.max(ROW_HEIGHT * (idsList.size() + 1), maxRowHeight);
                                    }
                                    break;
                            }
                        }
                    }
                }
                int width = Math.max(PLATE_WIDTH, maxColumn * maxColumnWidth);
                int height = Math.max(ROW_HEIGHT, maxRow * maxRowHeight) + inPlaceHeight;

                // JSON for the parent vessel
                jsonWriter.object().key("id").value(containerLabel).
                        key("label").value(ancillaryLabel).
                        key("width").value(width).
                        key("height").value(height).
                        key("children").array();

                // JSON for in-place events
                List<LabEvent> inPlaceLabEvents = new ArrayList<>(vesselContainer.getEmbedder().getInPlaceLabEvents());
                Collections.sort(inPlaceLabEvents, LabEvent.BY_EVENT_DATE);
                int inPlaceEventOffset = ROW_HEIGHT;
                for (LabEvent labEvent : inPlaceLabEvents) {
                    jsonWriter.object().
                            key("name").value(buildEventLabel(labEvent)).
                            key("x").value(0L).key("y").value(inPlaceEventOffset).
                            key("w").value(width).key("h").value(ROW_HEIGHT).
                            endObject();
                    inPlaceEventOffset += ROW_HEIGHT;
                }

                // JSON for children
                for (VesselPosition vesselPosition : vesselGeometry.getVesselPositions()) {
                    VesselGeometry.RowColumn rowColumn = vesselGeometry.getRowColumnForVesselPosition(vesselPosition);
                    LabVessel child = vesselContainer.getVesselAtPosition(vesselPosition);
                    if (child != null) {
                        jsonWriter.object().
                                key("label").value(child.getLabel()).
                                key("x").value((rowColumn.getColumn() - 1) * maxColumnWidth).
                                key("y").value((rowColumn.getRow()) * maxRowHeight + inPlaceHeight).
                                key("w").value(maxColumnWidth).
                                key("h").value(maxRowHeight).
                                key("altIds").array();
                        if (!mapBarcodeToAlternativeIds.isEmpty()) {
                            List<String> ids = mapBarcodeToAlternativeIds.get(child.getLabel());
                            if (ids != null) {
                                for (String id : ids) {
                                    jsonWriter.object().key("altId").value(id).endObject();
                                }
                            }
                        }
                        jsonWriter.endArray();
                        if (child.equals(labVessel)) {
                            jsonWriter.key("highlight").value(1L);
                        }
                        jsonWriter.endObject();
                    }
                }
                jsonWriter.endArray().endObject();
                if (startId == null) {
                    startId = containerLabel;
                }

                // JSON for re-arrays
                if (labVessel != null && followRearrays) {
                    if (labVessel.getContainers().size() > 1) {
                        for (VesselContainer<?> otherContainer : labVessel.getContainers()) {
                            renderReArray(vesselContainer, otherContainer, labVessel);
                        }
                    }
                }
            }
        }

        /**
         * Render a (dashed) edge for a tube moving to a different formation.
         */
        private void renderReArray(VesselContainer<?> vesselContainer, VesselContainer<?> otherContainer,
                LabVessel labVessel) throws JSONException {
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

        /**
         * Finish the streamed nodes, then write the edges.
         */
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
    }

    /**
     * For the given vessels and directions, writes JSON for nodes and edges to the given writer.
     */
    public void jsonForVessels(List<LabVessel> labVessels,
            List<TransferTraverserCriteria.TraversalDirection> traversalDirections,
            Writer writer,
            List<AlternativeIds> alternativeIds) {
        if (traversalDirections.isEmpty()) {
            throw new IllegalArgumentException("Must supply at least one direction");
        }
        try {
            Traverser traverser = new Traverser(writer, alternativeIds);
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
