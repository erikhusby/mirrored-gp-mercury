package org.broadinstitute.gpinformatics.infrastructure.search;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnEntity;
import org.broadinstitute.gpinformatics.infrastructure.columns.ConfigurableList;
import org.broadinstitute.gpinformatics.infrastructure.columns.ConfigurableListFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

//import com.jprofiler.api.agent.Controller;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Validate that the traverser finds events correctly
 */
@Test(groups = TestGroups.STANDARD)
public class LabEventTraversalTest extends Arquillian {

    @Inject
    private ConfigurableListFactory configurableListFactory;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    public void testLcsetDescendantSearch() {
        ConfigurableSearchDefinition configurableSearchDefinition =
                SearchDefinitionFactory.getForEntity(ColumnEntity.LAB_EVENT.getEntityName());

        SearchInstance searchInstance = new SearchInstance();
        // Select descendants
        searchInstance.getTraversalEvaluatorValues().put("descendantOptionEnabled", Boolean.TRUE );
        SearchInstance.SearchValue searchValue = searchInstance.addTopLevelTerm("LCSET", configurableSearchDefinition);
        searchValue.setOperator(SearchInstance.Operator.EQUALS);
        searchValue.setValues(Collections.singletonList("LCSET-5102"));

        searchInstance.getPredefinedViewColumns().add("LabEventId");
        searchInstance.getPredefinedViewColumns().add("EventType");

        searchInstance.establishRelationships(configurableSearchDefinition);

        // Side effect of traversal evaluator
        Assert.assertFalse(searchInstance.getIsDbSortable(), "Traversal evaluator results are not sortable in DB");

        ConfigurableListFactory.FirstPageResults firstPageResults =
                configurableListFactory.getFirstResultsPage(
                        searchInstance, configurableSearchDefinition, null, 1, null, "ASC", "LabEvent" );

        Assert.assertEquals(firstPageResults.getPagination().getIdList().size(), 364);

    }

    /**
     *  The three tests here exercise the lab event by vessels alternate search definition
     * */
    public void testEventByVesselAlternateSearch() {
        ConfigurableSearchDefinition configurableSearchDefinition =
                SearchDefinitionFactory.getForEntity(ColumnEntity.LAB_EVENT.getEntityName());

        // In place events and transfers to/from a sample tube
        SearchInstance searchInstance = new SearchInstance();
        SearchInstance.SearchValue searchValue = searchInstance.addTopLevelTerm("Event Vessel Barcode", configurableSearchDefinition);
        searchValue.setOperator(SearchInstance.Operator.EQUALS);
        searchValue.setValues(Collections.singletonList("1090469488"));

        searchInstance.getPredefinedViewColumns().add("LabEventId");
        searchInstance.getPredefinedViewColumns().add("EventType");

        searchInstance.establishRelationships(configurableSearchDefinition);

        // Side effect of traversal evaluator
        Assert.assertFalse(searchInstance.getIsDbSortable(), "Traversal evaluator results are not sortable in DB");

        ConfigurableListFactory.FirstPageResults firstPageResults =
                configurableListFactory.getFirstResultsPage(
                        searchInstance, configurableSearchDefinition, null, 1, null, "ASC", "LabEvent" );

        Assert.assertEquals(firstPageResults.getPagination().getIdList().size(), 7);

        // In place events on a sample tube
        searchInstance = new SearchInstance();
        searchValue = searchInstance.addTopLevelTerm("In-Place Vessel Barcode", configurableSearchDefinition);
        searchValue.setOperator(SearchInstance.Operator.EQUALS);
        searchValue.setValues(Collections.singletonList("1090469488"));

        searchInstance.getPredefinedViewColumns().add("LabEventId");
        searchInstance.getPredefinedViewColumns().add("EventType");

        searchInstance.establishRelationships(configurableSearchDefinition);

        firstPageResults = configurableListFactory.getFirstResultsPage(
                        searchInstance, configurableSearchDefinition, null, 1, null, "ASC", "LabEvent" );

        Assert.assertEquals(firstPageResults.getPagination().getIdList().size(), 2);

        // Select descendants of in place vessels
        searchInstance.getTraversalEvaluatorValues().put("descendantOptionEnabled", Boolean.TRUE );
        searchInstance.establishRelationships(configurableSearchDefinition);

        firstPageResults = configurableListFactory.getFirstResultsPage(
                searchInstance, configurableSearchDefinition, null, 1, null, "ASC", "LabEvent" );

        Assert.assertEquals(firstPageResults.getPagination().getIdList().size(), 61);
    }

    public void testLcsetReworkDescendantSearch() {
//        Controller.startCPURecording(true);
//        Controller.startProbeRecording(Controller.PROBE_NAME_JDBC, true);

        ConfigurableSearchDefinition configurableSearchDefinition =
                SearchDefinitionFactory.getForEntity(ColumnEntity.LAB_EVENT.getEntityName());

        SearchInstance searchInstance = new SearchInstance();
        // Select descendants
        searchInstance.getTraversalEvaluatorValues().put("descendantOptionEnabled", Boolean.TRUE );
        SearchInstance.SearchValue searchValue = searchInstance.addTopLevelTerm("LCSET", configurableSearchDefinition);
        searchValue.setOperator(SearchInstance.Operator.EQUALS);
        searchValue.setValues(Collections.singletonList("LCSET-6875"));

        searchInstance.getPredefinedViewColumns().add("LabEventId");
        searchInstance.getPredefinedViewColumns().add("EventType");

        searchInstance.establishRelationships(configurableSearchDefinition);

        ConfigurableListFactory.FirstPageResults firstPageResults = configurableListFactory.getFirstResultsPage(
                searchInstance, configurableSearchDefinition, null, 0, null, "ASC", "LabEvent" );

        Assert.assertEquals(firstPageResults.getPagination().getIdList().size(), 46);
        List<ImmutablePair<String, String>> listEventLcset = Arrays.asList(
                new ImmutablePair<>("NormalizedCatchRegistration", "LCSET-6625"),
                new ImmutablePair<>("CatchPico", "LCSET-6625"),
                new ImmutablePair<>("CatchPico", "LCSET-6625"),
                new ImmutablePair<>("PoolingTransfer", "LCSET-6625"),
                new ImmutablePair<>("EcoTransfer", "LCSET-6625"),
                new ImmutablePair<>("NormalizationTransfer", "LCSET-6625"),
                new ImmutablePair<>("DenatureTransfer", "LCSET-6625"),
                new ImmutablePair<>("MiseqReagentKitLoading", "LCSET-6625"),
                new ImmutablePair<>("ReagentKitToFlowcellTransfer", "LCSET-6625"),
                new ImmutablePair<>("DenatureTransfer", "LCSET-6625"),
                new ImmutablePair<>("DenatureToDilutionTransfer", "LCSET-6625"),
                new ImmutablePair<>("DilutionToFlowcellTransfer", "LCSET-6625"),
                new ImmutablePair<>("DilutionToFlowcellTransfer", "LCSET-6625"),
                new ImmutablePair<>("DilutionToFlowcellTransfer", "LCSET-6625"),
                new ImmutablePair<>("DilutionToFlowcellTransfer", "LCSET-6625"),
                new ImmutablePair<>("DilutionToFlowcellTransfer", "LCSET-6625"),
                new ImmutablePair<>("DilutionToFlowcellTransfer", "LCSET-6625"),
                new ImmutablePair<>("DilutionToFlowcellTransfer", "LCSET-6625"),
                new ImmutablePair<>("DilutionToFlowcellTransfer", "LCSET-6625"),
                new ImmutablePair<>("DilutionToFlowcellTransfer", "LCSET-6625"),
                new ImmutablePair<>("DilutionToFlowcellTransfer", "LCSET-6625"),
                new ImmutablePair<>("DilutionToFlowcellTransfer", "LCSET-6625"),
                new ImmutablePair<>("DilutionToFlowcellTransfer", "LCSET-6625"),
                new ImmutablePair<>("PoolingBucket", "LCSET-6875"),
                new ImmutablePair<>("PoolingBucket", "LCSET-6875"),
                new ImmutablePair<>("PoolingBucket", "LCSET-6875"),
                new ImmutablePair<>("PoolingBucket", "LCSET-6875"),
                new ImmutablePair<>("PoolingBucket", "LCSET-6875"),
                new ImmutablePair<>("PoolingBucket", "LCSET-6875"),
                new ImmutablePair<>("PoolingBucket", "LCSET-6875"),
                new ImmutablePair<>("PoolingBucket", "LCSET-6875"),
                new ImmutablePair<>("PoolingBucket", "LCSET-6875"),
                new ImmutablePair<>("PoolingBucket", "LCSET-6875"),
                new ImmutablePair<>("PoolingBucket", "LCSET-6875"),
                new ImmutablePair<>("PoolingBucket", "LCSET-6875"),
                new ImmutablePair<>("PoolingBucket", "LCSET-6875"),
                new ImmutablePair<>("PoolingTransfer", "LCSET-6875"),
                new ImmutablePair<>("EcoTransfer", "LCSET-6677 LCSET-6875"),
                new ImmutablePair<>("EcoTransfer", "LCSET-6677 LCSET-6875"),
                new ImmutablePair<>("NormalizationTransfer", "LCSET-6875"),
                new ImmutablePair<>("DenatureTransfer", "LCSET-6677 LCSET-6875"),
                new ImmutablePair<>("MiseqReagentKitLoading", "LCSET-6875"),
                new ImmutablePair<>("ReagentKitToFlowcellTransfer", "LCSET-6875"),
                new ImmutablePair<>("DenatureTransfer", "LCSET-6677 LCSET-6875"),
                new ImmutablePair<>("StripTubeBTransfer", "LCSET-6712 LCSET-6875"),
                new ImmutablePair<>("FlowcellTransfer", "LCSET-6875")
        );
        int i = 0;
        for (ConfigurableList.ResultRow resultRow : firstPageResults.getResultList().getResultRows()) {
            Assert.assertEquals(resultRow.getRenderableCells().get(1), listEventLcset.get(i).getLeft(),
                    "Wrong event type in row " + i);
            Assert.assertEquals(resultRow.getRenderableCells().get(2), listEventLcset.get(i).getRight(),
                    "Wrong LCSET in row " + i);
            i++;
        }

//        Controller.stopProbeRecording(Controller.PROBE_NAME_JDBC);
//        Controller.stopCPURecording();
    }

}
