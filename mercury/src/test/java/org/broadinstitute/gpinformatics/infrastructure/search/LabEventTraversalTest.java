package org.broadinstitute.gpinformatics.infrastructure.search;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnEntity;
import org.broadinstitute.gpinformatics.infrastructure.columns.ConfigurableList;
import org.broadinstitute.gpinformatics.infrastructure.columns.ConfigurableListFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
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
        searchValue = searchInstance.addTopLevelTerm("Event Vessel Barcode", configurableSearchDefinition);
        searchValue.setOperator(SearchInstance.Operator.EQUALS);
        searchValue.setValues(Collections.singletonList("1090469488"));

        searchInstance.getPredefinedViewColumns().add("LabEventId");
        searchInstance.getPredefinedViewColumns().add("EventType");

        searchInstance.establishRelationships(configurableSearchDefinition);

        firstPageResults = configurableListFactory.getFirstResultsPage(
                        searchInstance, configurableSearchDefinition, null, 1, null, "ASC", "LabEvent" );

        Assert.assertEquals(firstPageResults.getPagination().getIdList().size(), 7);

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

        // Search term and result values, LCSET order in result column is non-deterministic
        List<String> val6875 = Collections.singletonList("LCSET-6875");
        List<String> val6625 = Collections.singletonList("LCSET-6625");
        List<String> val6677_6875 = Arrays.asList( "LCSET-6677", "LCSET-6875");
        List<String> val6712_6875 = Arrays.asList( "LCSET-6712", "LCSET-6875");
        List<String> val6340_6625 = Arrays.asList( "LCSET-6340", "LCSET-6625");

        ConfigurableSearchDefinition configurableSearchDefinition =
                SearchDefinitionFactory.getForEntity(ColumnEntity.LAB_EVENT.getEntityName());

        SearchInstance searchInstance = new SearchInstance();
        // Select descendants
        searchInstance.getTraversalEvaluatorValues().put("descendantOptionEnabled", Boolean.TRUE );
        SearchInstance.SearchValue searchValue = searchInstance.addTopLevelTerm("LCSET", configurableSearchDefinition);
        searchValue.setOperator(SearchInstance.Operator.EQUALS);
        searchValue.setValues(val6875);

        searchInstance.getPredefinedViewColumns().add("LabEventId");
        searchInstance.getPredefinedViewColumns().add("EventType");

        searchInstance.establishRelationships(configurableSearchDefinition);

        ConfigurableListFactory.FirstPageResults firstPageResults = configurableListFactory.getFirstResultsPage(
                searchInstance, configurableSearchDefinition, null, 0, null, "ASC", "LabEvent" );

        Assert.assertEquals(firstPageResults.getPagination().getIdList().size(), 46);
        List<ImmutablePair<String, List<String>>> listEventLcset = Arrays.asList(
                new ImmutablePair<>("NormalizedCatchRegistration", val6625),
                new ImmutablePair<>("CatchPico", val6625),
                new ImmutablePair<>("CatchPico", val6625),
                new ImmutablePair<>("PoolingTransfer", val6625),
                new ImmutablePair<>("EcoTransfer", val6340_6625),
                new ImmutablePair<>("NormalizationTransfer", val6340_6625),
                new ImmutablePair<>("DenatureTransfer", val6340_6625),
                new ImmutablePair<>("MiseqReagentKitLoading", val6340_6625),
                new ImmutablePair<>("ReagentKitToFlowcellTransfer", val6340_6625),
                new ImmutablePair<>("DenatureTransfer", val6340_6625),
                new ImmutablePair<>("DenatureToDilutionTransfer", val6340_6625),
                new ImmutablePair<>("DilutionToFlowcellTransfer", val6340_6625),
                new ImmutablePair<>("DilutionToFlowcellTransfer", val6340_6625),
                new ImmutablePair<>("DilutionToFlowcellTransfer", val6340_6625),
                new ImmutablePair<>("DilutionToFlowcellTransfer", val6340_6625),
                new ImmutablePair<>("DilutionToFlowcellTransfer", val6340_6625),
                new ImmutablePair<>("DilutionToFlowcellTransfer", val6340_6625),
                new ImmutablePair<>("DilutionToFlowcellTransfer", val6340_6625),
                new ImmutablePair<>("DilutionToFlowcellTransfer", val6340_6625),
                new ImmutablePair<>("DilutionToFlowcellTransfer", val6340_6625),
                new ImmutablePair<>("DilutionToFlowcellTransfer", val6340_6625),
                new ImmutablePair<>("DilutionToFlowcellTransfer", val6340_6625),
                new ImmutablePair<>("DilutionToFlowcellTransfer", val6340_6625),
                new ImmutablePair<>("PoolingBucket", val6875),
                new ImmutablePair<>("PoolingBucket", val6875),
                new ImmutablePair<>("PoolingBucket", val6875),
                new ImmutablePair<>("PoolingBucket", val6875),
                new ImmutablePair<>("PoolingBucket", val6875),
                new ImmutablePair<>("PoolingBucket", val6875),
                new ImmutablePair<>("PoolingBucket", val6875),
                new ImmutablePair<>("PoolingBucket", val6875),
                new ImmutablePair<>("PoolingBucket", val6875),
                new ImmutablePair<>("PoolingBucket", val6875),
                new ImmutablePair<>("PoolingBucket", val6875),
                new ImmutablePair<>("PoolingBucket", val6875),
                new ImmutablePair<>("PoolingBucket", val6875),
                new ImmutablePair<>("PoolingTransfer", val6875),
                new ImmutablePair<>("EcoTransfer", val6677_6875),
                new ImmutablePair<>("EcoTransfer", val6677_6875),
                new ImmutablePair<>("NormalizationTransfer", val6875),
                new ImmutablePair<>("DenatureTransfer", val6677_6875),
                new ImmutablePair<>("MiseqReagentKitLoading", val6875),
                new ImmutablePair<>("ReagentKitToFlowcellTransfer", val6875),
                new ImmutablePair<>("DenatureTransfer", val6677_6875),
                new ImmutablePair<>("StripTubeBTransfer", val6712_6875),
                new ImmutablePair<>("FlowcellTransfer", val6875)
        );
        int i = 0;
        for (ConfigurableList.ResultRow resultRow : firstPageResults.getResultList().getResultRows()) {
            Assert.assertEquals(resultRow.getRenderableCells().get(1), listEventLcset.get(i).getLeft(),
                    "Wrong event type in row " + i);
            String[] results = resultRow.getRenderableCells().get(2).split(" ");
            Assert.assertEquals(results.length, listEventLcset.get(i).getRight().size(),
                    "Wrong number of LCSETs in row " + i);
            for( String result : results ) {
                Assert.assertTrue(listEventLcset.get(i).getRight().contains(result),
                        "Wrong LCSET in row " + i);
            }
            i++;
        }

//        Controller.stopProbeRecording(Controller.PROBE_NAME_JDBC);
//        Controller.stopCPURecording();
    }

}
