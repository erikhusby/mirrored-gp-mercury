package org.broadinstitute.gpinformatics.infrastructure.search;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.SectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This traversal evaluator is tied to an Infinium search term as part of an alternate search definition.
 * Expands an Infinium array process related vessel (PDO sample tube, DNA plate, amp plate, or Infinium chip) by
 *      traversing events to obtain ancestor DNA plates/DNA plate wells
 */
public class  InfiniumPlateSourceEvaluator extends TraversalEvaluator {

    // All Infinium array search logic is based upon a DNA plate/DNA plate well as the source located by the target of
    // ancestor event type
    List<LabEventType> infiniumRootEventTypes = Collections.singletonList(LabEventType.ARRAY_PLATING_DILUTION);

    public InfiniumPlateSourceEvaluator(){}

    @Override
    public Set<Object> evaluate(List<? extends Object> rootEntities, SearchInstance searchInstance) {
        // For PDO search term results, holds the Infinium related vessels (DNA Plates, AMP Plates, and Infinium Chips)
        // For vessel search terms (DNA Plates, AMP Plates, and Infinium Chips), holds the DNA Plate wells
        Set<Object> vessels = new HashSet<>();

        // Search by Infinium PDO does not expand DNA plate wells and includes Amp plates and chips
        if( LabVesselSearchDefinition.MultiRefTerm.INFINIUM_PDO
                .isNamed(searchInstance.getSearchValues().iterator().next().getName()) ) {
            Set<LabVessel> infiniumVessels = getAllInfiniumVessels(rootEntities);
            vessels.addAll(infiniumVessels);
            return vessels;
        } else {
            // Search by Infinium vessel results in DNA plate wells
            Set<LabVessel> infiniumVessels = getAllDnaPlateWells(rootEntities);
            vessels.addAll(infiniumVessels);
            return vessels;
        }
    }

    /**
     * Get Infinium vessels (DNA Plates and AMP Plates) starting from PDO sample tubes
     * @param rootEntities Vessels associated with Infinium PDOs (sample tubes)
     * @return
     */
    private Set<LabVessel> getAllInfiniumVessels(List<? extends Object> rootEntities) {
        Set<LabVessel> infiniumVessels = new HashSet<>();

        // Get the Infinium vessels by traversing descendant events of current PDO sample tubes
        TransferTraverserCriteria.VesselForEventTypeCriteria eventTypeCriteria
                = new TransferTraverserCriteria.VesselForEventTypeCriteria(infiniumRootEventTypes, true);

        for( LabVessel vessel : (List<LabVessel>) rootEntities ) {
            if( vessel.getContainerRole() != null ) {
                vessel.getContainerRole().applyCriteriaToAllPositions(eventTypeCriteria,
                        TransferTraverserCriteria.TraversalDirection.Descendants);
            } else {
                // In most cases, the PDO tube is plated directly, so we don't need to traverse.
                boolean found = false;
                for (LabVessel labVessel : vessel.getContainers()) {
                    for (SectionTransfer sectionTransfer : labVessel.getContainerRole().getSectionTransfersFrom()) {
                        if (sectionTransfer.getLabEvent().getLabEventType() == LabEventType.ARRAY_PLATING_DILUTION) {
                            Set<LabVessel> labVessels = eventTypeCriteria.getVesselsForLabEventType().get(
                                    sectionTransfer.getLabEvent());
                            if (labVessels == null) {
                                labVessels = new HashSet<>();
                                eventTypeCriteria.getVesselsForLabEventType().put(sectionTransfer.getLabEvent(),
                                        labVessels);
                            }
                            labVessels.add(sectionTransfer.getTargetVesselContainer().getEmbedder());
                            found = true;
                            break;
                        }
                    }
                }
                if (!found) {
                    vessel.evaluateCriteria(eventTypeCriteria, TransferTraverserCriteria.TraversalDirection.Descendants);
                }
            }
        }

        for(Map.Entry<LabEvent, Set<LabVessel>> eventEntry : eventTypeCriteria.getVesselsForLabEventType().entrySet()) {
            for( LabVessel eventVessel : eventEntry.getValue() ) {
                if( eventVessel.getType() == LabVessel.ContainerType.PLATE_WELL) {
                    // Use static plate only
                    for(VesselContainer vesselContainer : eventVessel.getVesselContainers() ) {
                        infiniumVessels.add(vesselContainer.getEmbedder());
                    }
                } else if (eventVessel.getContainerRole() == null ||
                        eventVessel.getContainerRole().getContainedVessels().isEmpty()) {
                    infiniumVessels.add(eventVessel);
                } else {
                    infiniumVessels.add( eventVessel.getContainerRole().getEmbedder() );
                }
            }
        }

        return infiniumVessels;
    }

    /**
     * Get Infinium DNA plate wells associated with any Infinium vessel
     * @param rootEntities Downstream Infinium vessels (DNA Plates, AMP Plates, and Infinium Chips)
     * @return DNA plate wells
     */
    private Set<LabVessel> getAllDnaPlateWells(List<? extends Object> rootEntities) {
        Set<LabVessel> infiniumVessels = new HashSet<>();

        // Get the Infinium vessels by traversing ancestor events of current entities
        TransferTraverserCriteria.VesselForEventTypeCriteria eventTypeCriteria
                = new TransferTraverserCriteria.VesselForEventTypeCriteria(infiniumRootEventTypes, true);

        for( LabVessel vessel : (List<LabVessel>) rootEntities ) {
            if( vessel.getContainerRole() != null ) {
                vessel.getContainerRole().applyCriteriaToAllPositions(eventTypeCriteria,
                        TransferTraverserCriteria.TraversalDirection.Ancestors);
            } else {
                vessel.evaluateCriteria(eventTypeCriteria, TransferTraverserCriteria.TraversalDirection.Ancestors);
            }
        }

        for(Map.Entry<LabEvent, Set<LabVessel>> eventEntry : eventTypeCriteria.getVesselsForLabEventType().entrySet()) {
            for( LabVessel eventVessel : eventEntry.getValue() ) {
                // Don't add the container (DNA Plate) to the results
                if (eventVessel.getType() == LabVessel.ContainerType.PLATE_WELL) {
                    infiniumVessels.add(eventVessel);
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
}
