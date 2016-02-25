package org.broadinstitute.gpinformatics.infrastructure.search;

import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;

import java.util.*;

/**
 * Functionality required to traverse ancestor and descendant vessels of a set of starting lab metric entities.
 * Starts with a List of LabMetric objects and returns a date sorted List
 *    of labMetricID (Long) values to use for pagination.
 * Directionality implemented in AncestorTraversalEvaluator and DescendantTraversalEvaluator nested classes
 */
public abstract class LabMetricTraversalEvaluator extends TraversalEvaluator {

    protected TransferTraverserCriteria.TraversalDirection traversalDirection;

    public LabMetricTraversalEvaluator(){ }

    /**
     * Traverse ancestor and/or descendants of supplied lab metrics list (or neither if no options selected).
     * @param rootEntities A group of starting lab metrics from which to obtain ancestors or descendants
     * @param searchInstance The search instance used for this search (has user selected tube metrics only term?)
     * @return
     */
    @Override
    public Set<Object> evaluate(List<?> rootEntities, SearchInstance searchInstance) {

        boolean tubesOnly = false;
        for( SearchInstance.SearchValue searchValue : searchInstance.getSearchValues() ) {
            if( searchValue.getName().equals("Only Show Metrics for Tubes")) {
                tubesOnly = true;
                break;
            }
        }

        List<LabMetric> rootMetrics = (List<LabMetric>) rootEntities;

        Set allMetrics = new TreeSet<LabMetric>();

        // Metrics are based upon vessels, need all vessels from core entity list
        Set<LabVessel> allVessels = new HashSet<>();
        for( LabMetric labMetric : rootMetrics ) {
            allVessels.add(labMetric.getLabVessel());
        }

        if( traversalDirection == TransferTraverserCriteria.TraversalDirection.Ancestors ){
            for( LabVessel labVessel : allVessels ) {
                Map<LabMetric.MetricType, Set<LabMetric>> metricsByType = labVessel.getMetricsForVesselAndAncestors();
                addMetricsToBaseSet( allMetrics, metricsByType, tubesOnly);
            }
        } else {
            for( LabVessel labVessel : allVessels ) {
                Map<LabMetric.MetricType, Set<LabMetric>> metricsByType = labVessel.getMetricsForVesselAndDescendants();
                addMetricsToBaseSet( allMetrics, metricsByType, tubesOnly);
            }
        }

        return allMetrics;
    }

    private void addMetricsToBaseSet(Set<LabMetric> allMetrics, Map<LabMetric.MetricType, Set<LabMetric>> metricsByType, boolean tubesOnly) {
        for( Map.Entry<LabMetric.MetricType, Set<LabMetric>> mapEntry : metricsByType.entrySet() ) {
            for( LabMetric labMetric : mapEntry.getValue() ) {
                if( tubesOnly && !OrmUtil.proxySafeIsInstance( labMetric.getLabVessel(), BarcodedTube.class) )  {
                    continue;
                }
                allMetrics.add(labMetric);
            }
        }
    }

    /**
     * Convert a collection of LabMetrics objects to ids (Long)
     * @param entities
     * @return
     */
    @Override
    public List<Long> buildEntityIdList( Set entities ) {
        List<Long> idList = new ArrayList<>();
        for( LabMetric labMetric : (Set<LabMetric>)entities ) {
            idList.add(labMetric.getLabMetricId());
        }
        return idList;
    }

    /**
     * Implementation of the Lab Metric ancestor traversal evaluator
     */
    public static class AncestorTraversalEvaluator extends LabMetricTraversalEvaluator {
        // ID = "ancestorOptionEnabled"
        public AncestorTraversalEvaluator() {
            setHelpNote("Metrics from vessel transfers to primary metric vessel (ancestors)");
            setLabel("Traverse Ancestors");
            traversalDirection = TransferTraverserCriteria.TraversalDirection.Ancestors;
        }
    }

    /**
     * Implementation of the Lab Metric descendant traversal evaluator
     */
    public static class DescendantTraversalEvaluator extends LabMetricTraversalEvaluator {
        // ID = "descendantOptionEnabled"
        public DescendantTraversalEvaluator() {
            setHelpNote("Metrics from vessel transfers from primary metric vessel (descendants)");
            setLabel("Traverse Descendants");
            traversalDirection = TransferTraverserCriteria.TraversalDirection.Descendants;
        }
    }

}
