package org.broadinstitute.gpinformatics.mercury.boundary.transfervis;

import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.CherryPickTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.SectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.VesselToSectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.VesselToVesselTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.DesignedReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.PlateWell;
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
import javax.enterprise.context.Dependent;
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
 *          <li>array of altIds, each has:</li>
 *              <ul>
 *              <li>altId - an alternative ID, e.g. SM- or LCSET-</li>
 *              </ul>
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
 * <li>array of highlights, each has
 *      <ul>
 *      <li>barcode - barcode of vessel to highlight</li>
 *      </ul>
 * </li>
 * </ul>
 */
@Dependent
public class TransferVisualizerV2 {

    private static final Log logger = LogFactory.getLog(TransferVisualizerV2.class);
    private static final Format DATE_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss");

    public enum AlternativeIds {
        SAMPLE_ID("Sample Id"),
        BUCKET_LCSETS("Bucket Entry LCSET"),
        INFERRED_LCSET("Inferred LCSET"),
        ALL_LCSETS("All LCSETs");

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
        public static final int WELL_WIDTH = 80;

        /** Stream to browser. */
        private Writer writer;
        /** The stream to which to write JSON. */
        private JSONWriter jsonWriter;
        /** Appended to JSON if an exception occurs. */
        private String error;
        /** Accumulates JSON for graph edges (nodes are streamed).*/
        private JSONArray edgesJson = new JSONArray();
        /** Prevents vessels being rendered more than once. */
        private final Set<String> renderedLabels = new HashSet<>();
        /** Prevents events being rendered more than once. */
        private final Set<String> renderedEvents = new HashSet<>();
        /** Prevents multiple edge labels for a pool. */
        private final Set<String> renderedEdgeLabels = new HashSet<>();
        /** Ancestor and descendant barcodes to be highlighted. */
        private JSONArray highlightBarcodeJson = new JSONArray();
        /** The ID to scroll to when the page is rendered.  If the starting barcode was a tube, this is one of the
         * enclosing racks (the tube may appear in multiple racks, so it can't be used as the start). */
        private String startId;
        /** The IDs to add to each rect. */
        private List<AlternativeIds> alternativeIds;
        /** Whether to include LCSET in event labels */
        private boolean lcsetInEvent;
        /** Used to determine size of text elements. */
        private FontMetrics fontMetrics = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).getGraphics().
                getFontMetrics(new Font("SansSerif", Font.PLAIN, 12));

        Traverser(Writer writer, List<AlternativeIds> alternativeIds) throws JSONException {
            this.writer = writer;
            this.alternativeIds = alternativeIds;
            if (alternativeIds.contains(AlternativeIds.INFERRED_LCSET) ||
                    alternativeIds.contains(AlternativeIds.BUCKET_LCSETS) ||
                    alternativeIds.contains(AlternativeIds.ALL_LCSETS)) {
                lcsetInEvent = true;
            }
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
                if (context.getContextVessel() != null && context.getContextVessel().getLabel() != null) {
                    highlightBarcode(context.getContextVessel().getLabel());
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
                    String sourceVesselLabel = sourceVessel != null && sourceVessel.getType() == LabVessel.ContainerType.TUBE ?
                            sourceVessel.getLabel() :
                            cherryPickTransfer.getSourcePosition().name();
                    LabVessel targetVessel = cherryPickTransfer.getTargetVesselContainer().getVesselAtPosition(
                            cherryPickTransfer.getTargetPosition());
                    String targetVesselLabel = targetVessel != null && targetVessel.getType() == LabVessel.ContainerType.TUBE ?
                            targetVessel.getLabel() :
                            cherryPickTransfer.getTargetPosition().name();

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
                    Set<VesselContainer<?>> containers = vesselToSectionTransfer.getSourceVessel().getVesselContainers();
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
                    for (VesselContainer<?> otherContainer : targetVessel.getVesselContainers()) {
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
            if (lcsetInEvent) {
                for (LabBatch labBatch : event.getComputedLcSets()) {
                    labelBuilder.append(" ").append(labBatch.getBatchName());
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
            for (VesselContainer<?> vesselContainer : labVessel.getVesselContainers()) {
                renderContainer(vesselContainer, null, labVessel, true);
            }
        }

        private void highlightBarcode(String barcode) throws JSONException {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("barcode", barcode);
            highlightBarcodeJson.put(jsonObject);
        }

        @Override
        public void evaluateVesselPostOrder(Context context) {

        }

        public void setError(String error) {
            this.error = error;
        }

        private class Dimensions {
            private int maxColumn;
            private int maxColumnWidth;
            private int maxRow;
            private int maxRowHeight = ROW_HEIGHT;
            private int inPlaceHeight;
            private int plateWidth;

            public int getMaxColumn() {
                return maxColumn;
            }

            public void setMaxColumn(int maxColumn) {
                this.maxColumn = maxColumn;
            }

            public int getMaxColumnWidth() {
                return maxColumnWidth;
            }

            public void setMaxColumnWidth(int maxColumnWidth) {
                this.maxColumnWidth = maxColumnWidth;
            }

            public int getMaxRow() {
                return maxRow;
            }

            public void setMaxRow(int maxRow) {
                this.maxRow = maxRow;
            }

            public int getMaxRowHeight() {
                return maxRowHeight;
            }

            public void setMaxRowHeight(int maxRowHeight) {
                this.maxRowHeight = maxRowHeight;
            }

            public int getInPlaceHeight() {
                return inPlaceHeight;
            }

            public void setInPlaceHeight(int inPlaceHeight) {
                this.inPlaceHeight = inPlaceHeight;
            }

            public int getPlateWidth() {
                return plateWidth;
            }

            public void setPlateWidth(int plateWidth) {
                this.plateWidth = plateWidth;
            }

            public int getWidth() {
                return Math.max(plateWidth, maxColumn * maxColumnWidth);
            }

            public int getHeight() {
                return Math.max(ROW_HEIGHT, maxRow * maxRowHeight) + inPlaceHeight;
            }
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

                Dimensions dimensions = new Dimensions();
                // Width of plate, based on label and in-place events
                dimensions.setPlateWidth(fontMetrics.stringWidth(containerLabel));
                dimensions.setInPlaceHeight(vesselContainer.getEmbedder().getInPlaceLabEvents().size() * ROW_HEIGHT);
                for (LabEvent labEvent : vesselContainer.getEmbedder().getInPlaceLabEvents()) {
                    dimensions.setPlateWidth(Math.max(dimensions.getPlateWidth(),
                            fontMetrics.stringWidth(buildEventLabel(labEvent))));
                }

                // Sizes for child vessels (e.g. tubes in a rack)
                VesselGeometry vesselGeometry = vesselContainer.getEmbedder().getVesselGeometry();
                Map<String, List<String>> mapBarcodeToAlternativeIds = new HashMap<>();
                for (VesselPosition vesselPosition : vesselGeometry.getVesselPositions()) {
                    VesselGeometry.RowColumn rowColumn = vesselGeometry.getRowColumnForVesselPosition(vesselPosition);
                    LabVessel child = vesselContainer.getVesselAtPosition(vesselPosition);
                    // todo jmt popup if more than one sample?
                    dimensionsForChild(dimensions, mapBarcodeToAlternativeIds, child, vesselPosition,
                            rowColumn.getColumn(), rowColumn.getRow());
                }

                // JSON for the parent vessel
                List<LabEvent> inPlaceLabEvents = new ArrayList<>(vesselContainer.getEmbedder().getInPlaceLabEvents());
                jsonForParent(containerLabel, inPlaceLabEvents, dimensions, ancillaryLabel);

                // JSON for children
                for (VesselPosition vesselPosition : vesselGeometry.getVesselPositions()) {
                    VesselGeometry.RowColumn rowColumn = vesselGeometry.getRowColumnForVesselPosition(vesselPosition);
                    LabVessel child = vesselContainer.getVesselAtPosition(vesselPosition);
                    jsonForChild(child, vesselPosition, dimensions, mapBarcodeToAlternativeIds, rowColumn.getColumn(),
                            rowColumn.getRow());
                }
                jsonWriter.endArray().endObject();
                if (startId == null) {
                    startId = containerLabel;
                }

                // JSON for re-arrays
                if (labVessel != null && followRearrays) {
                    if (labVessel.getVesselContainers().size() > 1) {
                        for (VesselContainer<?> otherContainer : labVessel.getVesselContainers()) {
                            renderReArray(vesselContainer, otherContainer, labVessel);
                        }
                    }
                }
            }
        }

        /**
         * Calculates the dimensions of a child, including alternative IDs.
         */
        private void dimensionsForChild(Dimensions dimensions, Map<String, List<String>> mapBarcodeToAlternativeIds,
                LabVessel child, VesselPosition vesselPosition, int columnNumber, int rowNumber) {
            dimensions.setMaxColumn(Math.max(dimensions.getMaxColumn(), columnNumber));
            dimensions.setMaxRow(Math.max(dimensions.getMaxRow(), rowNumber + 1));
            if (child == null) {
                int width = fontMetrics.stringWidth(vesselPosition.name());
                dimensions.setMaxColumnWidth(Math.max(width, dimensions.getMaxColumnWidth()));
                return;
            } else {
                int width = fontMetrics.stringWidth(OrmUtil.proxySafeIsInstance(child, PlateWell.class) ?
                        vesselPosition.name() : child.getLabel());
                dimensions.setMaxColumnWidth(Math.max(width, dimensions.getMaxColumnWidth()));
            }
            for (AlternativeIds alternativeId : alternativeIds) {
                switch (alternativeId) {
                    case SAMPLE_ID:
                        Set<SampleInstanceV2> sampleInstances = child.getSampleInstancesV2();
                        if (sampleInstances.size() == 1) {
                            SampleInstanceV2 sampleInstance = sampleInstances.iterator().next();
                            String ids = "";
                            if (sampleInstance.isReagentOnly()) {
                                for (Reagent reagent : sampleInstance.getReagents()) {
                                    if (OrmUtil.proxySafeIsInstance(reagent, MolecularIndexReagent.class)) {
                                        MolecularIndexReagent molecularIndexReagent = OrmUtil.proxySafeCast(reagent,
                                                MolecularIndexReagent.class);
                                        ids += molecularIndexReagent.getMolecularIndexingScheme().getName();
                                    } else if (OrmUtil.proxySafeIsInstance(reagent, DesignedReagent.class)) {
                                        DesignedReagent designedReagent = OrmUtil.proxySafeCast(reagent,
                                                DesignedReagent.class);
                                        ids += designedReagent.getReagentDesign().getName();
                                    }
                                }
                            } else {
                                String indexName = sampleInstance.getMolecularIndexingScheme() == null ? "" :
                                        " " + sampleInstance.getMolecularIndexingScheme().getName();
                                ids = sampleInstance.getNearestMercurySampleName() + indexName;
                            }
                            dimensionsForAltId(dimensions, mapBarcodeToAlternativeIds, child, ids);
                        }
                        break;
                    case INFERRED_LCSET: {
                        Set<LabBatch> labBatches = new HashSet<>();
                        for (SampleInstanceV2 sampleInstance : child.getSampleInstancesV2()) {
                            LabBatch singleBatch = sampleInstance.getSingleBatch();
                            if (singleBatch != null) {
                                labBatches.add(singleBatch);
                            }
                        }
                        dimensionForBatches(dimensions, mapBarcodeToAlternativeIds, child, labBatches);
                        break;
                    }
                    case BUCKET_LCSETS: {
                        Set<LabBatch> labBatches = new HashSet<>();
                        for (BucketEntry bucketEntry : child.getBucketEntries()) {
                            if (bucketEntry.getLabBatch() != null) {
                                labBatches.add(bucketEntry.getLabBatch());
                            }
                        }
                        dimensionForBatches(dimensions, mapBarcodeToAlternativeIds, child, labBatches);
                        break;
                    }
                    case ALL_LCSETS: {
                        Set<LabBatch> labBatches = new HashSet<>();
                        for (SampleInstanceV2 sampleInstance : child.getSampleInstancesV2()) {
                            labBatches.addAll(sampleInstance.getAllWorkflowBatches());
                        }
                        dimensionForBatches(dimensions, mapBarcodeToAlternativeIds, child, labBatches);
                        break;
                    }
                }
            }
        }

        private void dimensionForBatches(Dimensions dimensions, Map<String, List<String>> mapBarcodeToAlternativeIds, LabVessel child, Set<LabBatch> labBatches) {
            if (!labBatches.isEmpty()) {
                StringBuilder idsBuilder = new StringBuilder();
                for (LabBatch labBatch : labBatches) {
                    if (idsBuilder.length() > 0) {
                        idsBuilder.append(", ");
                    }
                    idsBuilder.append(labBatch.getBatchName());
                }
                dimensionsForAltId(dimensions, mapBarcodeToAlternativeIds, child, idsBuilder.toString());
            }
        }

        /**
         * Calculates the size of an alternative ID.
         */
        private void dimensionsForAltId(Dimensions dimensions, Map<String, List<String>> mapBarcodeToAlternativeIds,
                LabVessel child, String ids) {
            List<String> idsList = mapBarcodeToAlternativeIds.get(child.getLabel());
            if (idsList == null) {
                idsList = new ArrayList<>();
                mapBarcodeToAlternativeIds.put(child.getLabel(), idsList);
            }
            idsList.add(ids);

            int width = fontMetrics.stringWidth(ids);
            dimensions.setMaxColumnWidth(Math.max(width, dimensions.getMaxColumnWidth()));
            dimensions.setMaxRowHeight(Math.max(ROW_HEIGHT * (idsList.size() + 1), dimensions.getMaxRowHeight()));
        }

        /**
         * Writes JSON for a parent, e.g. a plate, a rack, a flowcell or a strip tube.
         * @throws JSONException
         */
        private void jsonForParent(String containerLabel, List<LabEvent> inPlaceLabEvents, Dimensions dimensions,
                String ancillaryLabel) throws JSONException {
            // JSON for the parent vessel
            jsonWriter.object().key("id").value(containerLabel).
                    key("label").value(ancillaryLabel).
                    key("width").value(dimensions.getWidth()).
                    key("height").value(dimensions.getHeight()).
                    key("children").array();

            // JSON for in-place events
            Collections.sort(inPlaceLabEvents, LabEvent.BY_EVENT_DATE);
            int inPlaceEventOffset = ROW_HEIGHT;
            for (LabEvent labEvent : inPlaceLabEvents) {
                jsonWriter.object().
                        key("name").value(buildEventLabel(labEvent)).
                        key("x").value(0L).key("y").value(inPlaceEventOffset).
                        key("w").value(dimensions.getWidth()).key("h").value(ROW_HEIGHT).
                        endObject();
                inPlaceEventOffset += ROW_HEIGHT;
            }
        }

        /**
         * Writes JSON for a child, e.g. a tube, a flowcell lane or a strip tube well.
         * @throws JSONException
         */
        private void jsonForChild(LabVessel child, VesselPosition vesselPosition, Dimensions dimensions,
                Map<String, List<String>> mapBarcodeToAlternativeIds, int columnNumber,
                int rowNumber) throws JSONException {
            String label = (child == null || OrmUtil.proxySafeIsInstance(child, PlateWell.class) ?
                    vesselPosition.name() : child.getLabel());
            jsonWriter.object().
                    key("label").value(label).
                    key("x").value((columnNumber - 1) * dimensions.getMaxColumnWidth()).
                    key("y").value((rowNumber) * dimensions.getMaxRowHeight() + dimensions.getInPlaceHeight()).
                    key("w").value(dimensions.getMaxColumnWidth()).
                    key("h").value(dimensions.getMaxRowHeight()).
                    key("altIds").array();
            if (!mapBarcodeToAlternativeIds.isEmpty()) {
                List<String> ids = mapBarcodeToAlternativeIds.get(child == null ? label : child.getLabel());
                if (ids != null) {
                    for (String id : ids) {
                        jsonWriter.object().key("altId").value(id).endObject();
                    }
                }
            }
            jsonWriter.endArray();
            jsonWriter.endObject();
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
                jsonWriter.endArray()
                        .key("startId").value(startId)
                        .key("links").value(edgesJson)
                        .key("highlights").value(highlightBarcodeJson)
                        .key("error").value(error)
                        .endObject();
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
        Traverser traverser = null;
        try {
            traverser = new Traverser(writer, alternativeIds);
            for (LabVessel labVessel : labVessels) {
                for (TransferTraverserCriteria.TraversalDirection traversalDirection : traversalDirections) {
                    VesselContainer<?> containerRole = labVessel.getContainerRole();
                    if (containerRole == null) {
                        labVessel.evaluateCriteria(traverser, traversalDirection);
                    } else {
                        for (VesselPosition vesselPosition : labVessel.getVesselGeometry().getVesselPositions()) {
                            containerRole.evaluateCriteria(vesselPosition, traverser, traversalDirection, 0);
                        }
                    }
                    traverser.resetAllTraversed();
                }
            }
        } catch (Exception e) {
            logger.error("Transfer visualizer error: " + e.getMessage(), e );
            if (traverser == null) {
                throw new RuntimeException(e);
            }
            traverser.setError(e.toString());
        } finally {
            if (traverser != null) {
                traverser.completeJson();
            }
        }
    }

}
