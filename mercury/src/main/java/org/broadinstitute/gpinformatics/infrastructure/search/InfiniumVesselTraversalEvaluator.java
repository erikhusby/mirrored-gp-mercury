package org.broadinstitute.gpinformatics.infrastructure.search;

import org.apache.commons.collections4.MultiValuedMap;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.SectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.PlateWell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * This traversal evaluator is tied to an Infinium search term as part of a custom traversal option.
 * Expands an Infinium array process related vessel (PDO sample tube, DNA plate, amp plate, or Infinium chip) by
 *      traversing events to obtain ancestor DNA plates/DNA plate wells
 */
public class InfiniumVesselTraversalEvaluator extends CustomTraversalEvaluator {

    // All Infinium array search logic is based upon a DNA plate/DNA plate well as the source located by the target of
    //    ancestor/descendant event type
    List<LabEventType> infiniumRootEventTypes = Collections.singletonList(LabEventType.ARRAY_PLATING_DILUTION);

    protected boolean shouldProducePlates = false;
    protected boolean shouldProduceWells = false;

    // Implementations to return only DNA plates or plate wells associated with starting vessel(s)
    public static InfiniumVesselTraversalEvaluator DNA_PLATE_INSTANCE = new InfiniumVesselTraversalEvaluator("Infinium DNA Plates Only", "infiniumPlates");
    public static InfiniumVesselTraversalEvaluator DNA_PLATEWELL_INSTANCE = new InfiniumVesselTraversalEvaluator("Infinium DNA Plate Wells Only", "infiniumWells");

    // Used to determine if a search is related to array processing
    private static Set<String> INFINIUM_TRAVERSER_IDS = new HashSet<>();

    static{
        DNA_PLATE_INSTANCE.shouldProducePlates = true;
        DNA_PLATEWELL_INSTANCE.shouldProduceWells = true;
        INFINIUM_TRAVERSER_IDS.add(DNA_PLATE_INSTANCE.getUiName());
        INFINIUM_TRAVERSER_IDS.add(DNA_PLATEWELL_INSTANCE.getUiName());
    }

    public InfiniumVesselTraversalEvaluator(String label, String uiName){
        super(label, uiName);
    }

    @Override
    public Set<Object> evaluate(List<? extends Object> rootEntities, TransferTraverserCriteria.TraversalDirection traversalDirection, SearchInstance searchInstance) {
        Set<Object> infiniumVessels = new TreeSet<>( new Comparator() {
            @Override
            public int compare(Object first, Object second) {
                return ((LabVessel)first).getLabel().compareTo(((LabVessel)second).getLabel());
            }
        });

        // Get the Infinium vessels by traversing ancestor/descendant events of initial vessel(s)
        TransferTraverserCriteria.VesselForEventTypeCriteria eventTypeCriteria
                = new TransferTraverserCriteria.VesselForEventTypeCriteria(infiniumRootEventTypes, true);

        for( LabVessel startingVessel : (List<LabVessel>) rootEntities ) {

            // If starting vessel is DNA plate or well, add plate and/or well(s) and continue
            Set<LabVessel> dnaVessels = testForStartingOnDnaPlateOrWell(startingVessel);
            if( dnaVessels.size() > 0 ) {
                infiniumVessels.addAll(dnaVessels);
                continue;
            }

            boolean found = false;
            VesselContainer<?> vesselContainer = startingVessel.getContainerRole();
            if( vesselContainer != null ) {
                // Rarely if ever will someone come in with daughter plate, just do the traverse
                startingVessel.getContainerRole().applyCriteriaToAllPositions(eventTypeCriteria, traversalDirection);
            } else {

                // In cases where the PDO tube is plated directly, we don't need to traverse.
                for (LabVessel rack : startingVessel.getContainers()) {
                    for (SectionTransfer sectionTransfer : rack.getContainerRole().getSectionTransfersFrom()) {
                        if (sectionTransfer.getLabEvent().getLabEventType() == LabEventType.ARRAY_PLATING_DILUTION) {
                            VesselContainer<?> dnaPlate = sectionTransfer.getTargetVesselContainer();
                            infiniumVessels.add(dnaPlate.getEmbedder());
                            // Grab all wells in the DNA plate to make sure controls are also included
                            for(VesselPosition position : sectionTransfer.getTargetSection().getWells() ) {
                                LabVessel dnaWell = dnaPlate.getVesselAtPosition(position);
                                if( dnaWell != null ) {
                                    infiniumVessels.add(dnaWell);
                                    // Starting vessel would be daughter plate tube barcode
                                    searchInstance.getEvalContext().getPagination().addExtraIdInfo(dnaWell.getLabel(),
                                            rack.getContainerRole().getVesselAtPosition(position).getLabel());
                                }
                            }
                            found = true;
                        }
                    }
                }
                if (!found) {
                    startingVessel.evaluateCriteria(eventTypeCriteria, TransferTraverserCriteria.TraversalDirection.Descendants);
                }
            }
        }

        // Filter types out of any initial DNA plate vessels found
        for(Iterator iter = infiniumVessels.iterator(); iter.hasNext(); ) {
            LabVessel eventVessel = (LabVessel)iter.next();
            if( eventVessel.getType() == LabVessel.ContainerType.PLATE_WELL && !shouldProduceWells ) {
                iter.remove();
            } else if( eventVessel.getType() == LabVessel.ContainerType.STATIC_PLATE && !shouldProducePlates ) {
                iter.remove();
            }
        }

        for(Map.Entry<LabEvent, Set<LabVessel>> eventEntry : eventTypeCriteria.getVesselsForLabEventType().entrySet()) {
            for( LabVessel eventVessel : eventEntry.getValue() ) {
                if( eventVessel.getType() == LabVessel.ContainerType.PLATE_WELL && shouldProduceWells ) {
                    infiniumVessels.add( eventVessel );
                } else if( eventVessel.getType() == LabVessel.ContainerType.PLATE_WELL && shouldProducePlates ) {
                    infiniumVessels.add( ((PlateWell)eventVessel).getPlate() );
                } else if( eventVessel.getType() == LabVessel.ContainerType.STATIC_PLATE && shouldProducePlates ) {
                    infiniumVessels.add( eventVessel );
                }
            }
        }

        return infiniumVessels;
    }

    @Override
    public List<Object> buildEntityIdList(Set<? extends Object> entities) {
        List<Object> idList = new ArrayList<>();

        // Filter out the containers, otherwise we get the DNA plate container in addition to the wells
        for( LabVessel vessel : (Set<LabVessel>) entities ) {
            idList.add(vessel.getLabel());
        }

        return idList;
    }

    /**
     * All Infinium related columns depend on search being related to Infinium arrays.
     * @param context Used to determine if search has a custom infinium traverser attached
     * @return True if the search term is related to Infinium arrays
     */
    public static boolean isInfiniumSearch( SearchContext context ){
        String customTraversalOptionName = context.getSearchInstance().getCustomTraversalOptionName();
        return INFINIUM_TRAVERSER_IDS.contains(customTraversalOptionName);
    }

    /**
     * Gets the DNA plate barcode for any Infinium vessel (DNA Plate well, amp plate, chip)
     * @param infiniumVessel Any vessel associated with infinium process
     * @param context Search context holding various shared objects
     * @return Always a single plate associated with any downstream vessel/position
     */
    public static String getInfiniumDnaPlateBarcode(LabVessel infiniumVessel, SearchContext context ) {
        String result = null;
        if(!isInfiniumSearch(context)) {
            return result;
        }

        // All Infinium vessels look in ancestors for dna plate barcode
        LabVesselSearchDefinition.VesselsForEventTraverserCriteria infiniumAncestorCriteria = new LabVesselSearchDefinition.VesselsForEventTraverserCriteria(
                Collections.singletonList(LabEventType.ARRAY_PLATING_DILUTION), true, true);
        if( infiniumVessel.getContainerRole() == null ) {
            infiniumVessel.evaluateCriteria(infiniumAncestorCriteria, TransferTraverserCriteria.TraversalDirection.Ancestors);
        } else {
            boolean found = false;
            for (SectionTransfer sectionTransfer : infiniumVessel.getContainerRole().getSectionTransfersTo()) {
                if (sectionTransfer.getLabEvent().getLabEventType() == LabEventType.ARRAY_PLATING_DILUTION) {
                    infiniumAncestorCriteria.getPositions().put(infiniumVessel, VesselPosition.A01);
                    found = true;
                }
            }

            if (!found) {
                infiniumVessel.getContainerRole().applyCriteriaToAllPositions(infiniumAncestorCriteria,
                        TransferTraverserCriteria.TraversalDirection.Ancestors);
            }
        }

        for (Map.Entry<LabVessel, Collection<VesselPosition>> labVesselAndPositions
                : infiniumAncestorCriteria.getPositions().asMap().entrySet()) {
            LabVessel plateVessel = labVesselAndPositions.getKey();
            if( plateVessel.getType() == LabVessel.ContainerType.PLATE_WELL ) {
                result = plateVessel.getContainers().iterator().next().getLabel();
                break;
            } else if( plateVessel.getType() == LabVessel.ContainerType.STATIC_PLATE ) {
                result = plateVessel.getLabel();
            }
        }

        return result;
    }

    public static String getInfiniumAmpPlateBarcode(LabVessel infiniumVessel, SearchContext context) {
        String result = null;
        if(!isInfiniumSearch(context)) {
            return result;
        }

        // Infinium vessels look in ancestors and descendants for amp plate barcode
        LabVesselSearchDefinition.VesselsForEventTraverserCriteria infiniumAncestorCriteria = new LabVesselSearchDefinition.VesselsForEventTraverserCriteria(
                Collections.singletonList(LabEventType.INFINIUM_AMPLIFICATION), true, true);
        if( infiniumVessel.getContainerRole() == null ) {
            infiniumVessel.evaluateCriteria(infiniumAncestorCriteria, TransferTraverserCriteria.TraversalDirection.Descendants);
            // Try ancestors
            if( infiniumAncestorCriteria.getPositions().isEmpty() ) {
                infiniumAncestorCriteria.resetAllTraversed();
                infiniumVessel.evaluateCriteria(infiniumAncestorCriteria, TransferTraverserCriteria.TraversalDirection.Ancestors);
            }
        } else {
            boolean found = false;
            for (SectionTransfer sectionTransfer : infiniumVessel.getContainerRole().getSectionTransfersTo()) {
                if (sectionTransfer.getLabEvent().getLabEventType() == LabEventType.ARRAY_PLATING_DILUTION) {
                    for (SectionTransfer transfer : sectionTransfer.getTargetVesselContainer().getSectionTransfersFrom()) {
                        if (transfer.getLabEvent().getLabEventType() == LabEventType.INFINIUM_AMPLIFICATION) {
                            infiniumAncestorCriteria.getPositions().put(
                                    transfer.getTargetVesselContainer().getEmbedder(), VesselPosition.A01);
                            found = true;
                        }
                    }
                }
            }

            if (!found) {
                infiniumVessel.getContainerRole().applyCriteriaToAllPositions(infiniumAncestorCriteria,
                        TransferTraverserCriteria.TraversalDirection.Descendants);
                // Try ancestors
                if( infiniumAncestorCriteria.getPositions().isEmpty() ) {
                    infiniumAncestorCriteria.resetAllTraversed();
                    infiniumVessel.getContainerRole().applyCriteriaToAllPositions(infiniumAncestorCriteria,
                            TransferTraverserCriteria.TraversalDirection.Ancestors);
                }
            }
        }

        for(Map.Entry<LabVessel, Collection<VesselPosition>> labVesselAndPositions
                : infiniumAncestorCriteria.getPositions().asMap().entrySet()) {
            result = labVesselAndPositions.getKey().getLabel();
            break;
        }

        return result;
    }

    /**
     * Given a LabVessel representing a DNA plate well, get details of the associated Infinium plate and position.  <br/>
     * Only shows the latest chip details if a re-hyb occurs downstream of a plate well
     * @param dnaPlateWell The DNA plate well to get the downstream Infinium chip details for.
     * @param chipEventTypes The downstream event type(s) to capture to get Infinium chip details <br />
     *                       ( INFINIUM_HYBRIDIZATION, but allow for flexibility )
     * @param context SearchContext containing values associated with search instance
     * @return All downstream vessels and associated positions, if initial vessel not a plate well, ignore and return empty Map
     */
    public static MultiValuedMap<LabVessel, VesselPosition> getChipDetailsForDnaWell(
            LabVessel dnaPlateWell, List<LabEventType> chipEventTypes, SearchContext context ) {

        // Every Infinium event/vessel looks to descendant for chip barcode
        LabVesselSearchDefinition.VesselsForEventTraverserCriteria infiniumDescendantCriteria = new LabVesselSearchDefinition.VesselsForEventTraverserCriteria(
                chipEventTypes, false, true);

        // Set flag to present only the newest chip details for a DNA plate well if re-hybed
        infiniumDescendantCriteria.captureLatestEventVesselsOnly();

        // Evaluate non-container only.  If a container, don't evaluate and return empty collection
        if( isInfiniumSearch(context) && dnaPlateWell.getContainerRole() == null ) {
            dnaPlateWell.evaluateCriteria(infiniumDescendantCriteria, TransferTraverserCriteria.TraversalDirection.Descendants);
        }

        return infiniumDescendantCriteria.getPositions();
    }

    /**
     * Given a LabVessel representing a DNA plate, get details of ALL dowbstream Infinium chips (initial and re-hyb)
     * @param dnaPlate The DNA plate to get the downstream Infinium chip details for.
     * @param chipEventTypes The downstream event type(s) to capture to get Infinium chip details <br />
     *                       ( INFINIUM_HYBRIDIZATION, but allow for flexibility )
     * @param context SearchContext containing values associated with search instance
     * @return All downstream Infinium chips and associated positions (positions ignored)
     */
    public static MultiValuedMap<LabVessel, VesselPosition> getChipDetailsForDnaPlate(
            LabVessel dnaPlate, List<LabEventType> chipEventTypes, SearchContext context ) {

        // Every Infinium event/vessel looks to descendant for chip barcode
        LabVesselSearchDefinition.VesselsForEventTraverserCriteria infiniumDescendantCriteria = new LabVesselSearchDefinition.VesselsForEventTraverserCriteria(
                chipEventTypes, false, true);

        // Evaluate container only.  If not a container, don't evaluate and return empty collection
        if( isInfiniumSearch(context) && dnaPlate.getContainerRole() != null ) {
            dnaPlate.getContainerRole().applyCriteriaToAllPositions(infiniumDescendantCriteria, TransferTraverserCriteria.TraversalDirection.Descendants);
        }

        return infiniumDescendantCriteria.getPositions();
    }

    /**
     * Determine if search starting vessel is a DNA plate or well and add all applicable vessels to set
     */
    private Set<LabVessel> testForStartingOnDnaPlateOrWell(LabVessel startingVessel ) {

        if( startingVessel.getType() != LabVessel.ContainerType.PLATE_WELL
                && startingVessel.getType() != LabVessel.ContainerType.STATIC_PLATE ) {
            return Collections.EMPTY_SET;
        }


        if( startingVessel.getType() == LabVessel.ContainerType.STATIC_PLATE ) {
            VesselContainer<?> vesselContainer = startingVessel.getContainerRole();
            for (SectionTransfer sectionTransfer : vesselContainer.getSectionTransfersTo()) {
                if (sectionTransfer.getLabEvent().getLabEventType() == LabEventType.ARRAY_PLATING_DILUTION) {
                    Set<LabVessel> infiniumVessels = new HashSet<>();
                    infiniumVessels.add(vesselContainer.getEmbedder());
                    infiniumVessels.addAll(vesselContainer.getContainedVessels());
                    return infiniumVessels;
                }
            }
        } else {
            VesselContainer<?> vesselContainer = ((PlateWell)startingVessel).getPlate().getContainerRole();
            for (SectionTransfer sectionTransfer : vesselContainer.getSectionTransfersTo()) {
                if (sectionTransfer.getLabEvent().getLabEventType() == LabEventType.ARRAY_PLATING_DILUTION) {
                    Set<LabVessel> infiniumVessels = new HashSet<>();
                    infiniumVessels.add(vesselContainer.getEmbedder());
                    infiniumVessels.add(startingVessel);
                    return infiniumVessels;
                }
            }
        }

        return Collections.EMPTY_SET;
    }
}