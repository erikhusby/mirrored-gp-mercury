package org.broadinstitute.gpinformatics.infrastructure.search;

import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetricRun;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Functionality required to traverse a set of starting lab vessel entities and produce a set of metric runs
 *     against all ancestor and descendant vessels. </br >
 * Starts with a List of LabVessel objects and returns a List of labMetricRun data values to use for pagination.
 */
public class LabMetricRunTraversalEvaluator extends TraversalEvaluator {

    public LabMetricRunTraversalEvaluator(){ }

    /**
     * Get all LabMetricRun objects related to ancestors and descendants of supplied lab vessels.
     * @param rootEntities A group of starting lab vessels from which to obtain ancestors or descendants
     * @param searchInstance The search instance used for this search
     * @return LabMetricRun objects related to vessel ancestors and descendants
     */
    @Override
    public Set<Object> evaluate(List<?> rootEntities, SearchInstance searchInstance) {

        List<LabVessel> rootVessels = (List<LabVessel>) rootEntities;
        String termName = searchInstance.getSearchValues().get(0).getTermName();

        Set allMetricRuns = new HashSet<LabMetricRun>();

        // Terms which return a single vessel can get metrics using a single criteria for each vessel
        if( LabMetricRunSearchDefinition.MultiRefTerm.RUN_SAMPLE.isNamed(termName)
                || LabMetricRunSearchDefinition.MultiRefTerm.RUN_VESSEL.isNamed(termName) ) {

            for (LabVessel labVessel : rootVessels) {
                Map<LabMetric.MetricType, Set<LabMetric>> metricsByType = labVessel.getMetricsForVesselAndAncestors();
                addMetricsToBaseSet(allMetricRuns, metricsByType);
            }

            for (LabVessel labVessel : rootVessels) {
                Map<LabMetric.MetricType, Set<LabMetric>> metricsByType = labVessel.getMetricsForVesselAndDescendants();
                addMetricsToBaseSet(allMetricRuns, metricsByType);
            }

        } else {
            // For PDO and LCSET, many vessels are returned.  Get a list of all ancestor and descendant vessels
            //   by sharing a single criteria to keep track of which branches not to run a duplicate traversal against
            TransferTraverserCriteria.LabVesselAncestorCriteria ancestorCriteria
                    = new TransferTraverserCriteria.LabVesselAncestorCriteria();

            for( LabVessel labVessel : rootVessels ) {
                labVessel.evaluateCriteria(ancestorCriteria, TransferTraverserCriteria.TraversalDirection.Ancestors);
            }
            Collection<LabVessel> allVessels = ancestorCriteria.getLabVesselAncestors();

            TransferTraverserCriteria.LabVesselDescendantCriteria descendantCriteria
                    = new TransferTraverserCriteria.LabVesselDescendantCriteria();
            for( LabVessel labVessel : rootVessels ) {
                labVessel.evaluateCriteria(descendantCriteria, TransferTraverserCriteria.TraversalDirection.Descendants);
            }
            allVessels.addAll(descendantCriteria.getLabVesselDescendants());
            allVessels.addAll(rootVessels);

            for (LabVessel curVessel : allVessels) {
                for( LabMetric labMetric : curVessel.getMetrics()){
                    if( labMetric.getLabMetricRun() != null ) {
                        allMetricRuns.add(labMetric.getLabMetricRun());
                    }
                }
            }
        }

        return allMetricRuns;
    }

    private void addMetricsToBaseSet(Set<LabMetricRun> allMetricRuns, Map<LabMetric.MetricType, Set<LabMetric>> metricsByType) {
        for( Map.Entry<LabMetric.MetricType, Set<LabMetric>> mapEntry : metricsByType.entrySet() ) {
            for( LabMetric labMetric : mapEntry.getValue() ) {
                // Null metric run for ECO_QPCR LabMetricId 178001, 181958 ?
                if( labMetric.getLabMetricRun() != null ) {
                    allMetricRuns.add(labMetric.getLabMetricRun());
                }
            }
        }
    }

    /**
     * Convert a collection of LabMetricRun objects to ids (Long)
     * @param entities The LabMetricRun objects produced from lab vessels by evaluate()
     * @return List of LabMetricRunId values
     */
    @Override
    public List<Long> buildEntityIdList( Set entities ) {
        List<Long> idList = new ArrayList<>();
        for( LabMetricRun labMetricRun : (Set<LabMetricRun>)entities ) {
            idList.add(labMetricRun.getLabMetricRunId());
        }
        return idList;
    }

}
