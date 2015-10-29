package org.broadinstitute.gpinformatics.mercury.boundary.transfervis;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Generates json to allow a javascript library to draw diagrams of transfers
 */
public class TransferVisualizerV2 {

    private static final Log logger = LogFactory.getLog(TransferVisualizerV2.class);

    private static class Traverser implements TransferTraverserCriteria {
        @SuppressWarnings("StringBufferField")
        private final StringBuilder nodesJson = new StringBuilder();
        @SuppressWarnings("StringBufferField")
        private final StringBuilder linksJson = new StringBuilder();
        private final Set<String> renderedLabels = new HashSet<>();
        private final Set<String> renderedEvents = new HashSet<>();

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
            // events
            String eventId = event.getEventLocation() + "|" + event.getEventDate().getTime() + "|" +
                    event.getDisambiguator();
            if (renderedEvents.add(eventId)) {
                logger.info("Rendering event " + event.getLabEventType());
                for (SectionTransfer sectionTransfer : event.getSectionTransfers()) {
                    String sourceId = sectionTransfer.getSourceVesselContainer().getEmbedder().getLabel();
                    String targetId = sectionTransfer.getTargetVesselContainer().getEmbedder().getLabel();
                    renderContainer(sectionTransfer.getTargetVesselContainer(),
                            sectionTransfer.getAncillaryTargetVessel(), labVessel, false);
                    renderLink(eventId, sourceId, targetId);
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
                    renderLink(eventId, cherryPickTransfer.getSourceVesselContainer().getEmbedder().getLabel(),
                            sourceVesselLabel,
                            cherryPickTransfer.getTargetVesselContainer().getEmbedder().getLabel(),
                            targetVesselLabel);
                }
                for (VesselToSectionTransfer vesselToSectionTransfer : event.getVesselToSectionTransfers()) {
                    String sourceLabel = vesselToSectionTransfer.getSourceVessel().getLabel();
                    Set<VesselContainer<?>> containers = vesselToSectionTransfer.getSourceVessel().getContainers();
                    if (!containers.isEmpty()) {
                        sourceLabel = containers.iterator().next().getEmbedder().getLabel();
                    }
                    renderContainer(vesselToSectionTransfer.getTargetVesselContainer(),
                            vesselToSectionTransfer.getAncillaryTargetVessel(), labVessel, false);
                    renderLink(eventId, sourceLabel,
                            vesselToSectionTransfer.getTargetVesselContainer().getEmbedder().getLabel());
                }
            }
        }

        private void renderLink(String eventId, String sourceId, String targetId) {
            linksJson.append("{ \"source\": \"").append(sourceId).append("\", \"target\": \"").append(targetId).
                    append("\" },\n");
        }
        private void renderLink(String eventId, String sourceId, String sourceChild, String targetId, String targetChild) {
            linksJson.append("{ \"source\": \"").append(sourceId).
                    append("\", \"sourceChild\": \"").append(sourceChild).
                    append("\", \"target\": \"").append(targetId).
                    append("\", \"targetChild\": \"").append(targetChild).
                    append("\" },\n");
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
                logger.info("Rendering container " + containerLabel);
                VesselGeometry vesselGeometry = vesselContainer.getEmbedder().getVesselGeometry();
                StringBuilder childBuilder = new StringBuilder();
                int maxColumn = 0;
                int maxRow = 0;
                for (VesselPosition vesselPosition : vesselGeometry.getVesselPositions()) {
                    VesselGeometry.RowColumn rowColumn = vesselGeometry.getRowColumnForVesselPosition(vesselPosition);
                    LabVessel child = vesselContainer.getVesselAtPosition(vesselPosition);
                    if (child != null) {
                        if (childBuilder.length() > 0) {
                            childBuilder.append(",");
                        }
                        childBuilder.append("{ \"name\": \"").append(child.getLabel()).
                                append("\", \"x\": ").append((rowColumn.getColumn() - 1) * 60).
                                append(", \"y\": ").append((rowColumn.getRow() - 1) * 20).
                                append(", \"w\": 60, \"h\": 20 ");
                        if (child.equals(labVessel)) {
                            childBuilder.append(", \"highlight\": 1");
                        }
                        childBuilder.append("}");
                        maxColumn = Math.max(maxColumn, rowColumn.getColumn());
                        maxRow = Math.max(maxRow, rowColumn.getRow());
                    }
                }
                int width = Math.max(120, maxColumn * 60);
                int height = Math.max(20, maxRow * 20);
                nodesJson.append("{ \"id\":\"").append(containerLabel).append("\", \"values\": {\"label\": \"").
                        append(ancillaryLabel).append("\", \"width\": ").append(width).append(", \"height\": ").
                        append(height).append(", \"children\": [");
                nodesJson.append(childBuilder.toString());
                nodesJson.append("] } },\n");

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
            String linksJsonString = linksJson.toString();
            if (linksJsonString.endsWith(",\n")) {
                linksJsonString = linksJsonString.substring(0, linksJsonString.length() - 2);
            }
            return "{ \"nodes\": [\n" + nodesJsonString + "],\n  \"links\": [\n" + linksJsonString + "  ] }";
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
                renderLink(null, sourceContainer.getEmbedder().getLabel(), labVessel.getLabel(),
                        targetContainer.getEmbedder().getLabel(), labVessel.getLabel());
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
