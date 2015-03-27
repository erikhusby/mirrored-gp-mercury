package org.broadinstitute.gpinformatics.infrastructure.search;

import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnEntity;
import org.broadinstitute.gpinformatics.infrastructure.columns.ConfigurableList;
import org.broadinstitute.gpinformatics.infrastructure.columns.ConfigurableListFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;

//import com.jprofiler.api.agent.Controller;

/**
 * Validate that the traverser finds events correctly
 */
@Test(groups = TestGroups.STANDARD)
public class LabEventTraversalTest extends ContainerTest {

    @Inject
    private ConfigurableListFactory configurableListFactory;


    public void testLcsetDescendantSearch() {
        ConfigurableSearchDefinition configurableSearchDefinition =
                SearchDefinitionFactory.getForEntity(ColumnEntity.LAB_EVENT.getEntityName());

        SearchInstance searchInstance = new SearchInstance();
        // Select descendants
        searchInstance.getTraversalEvaluatorValues().put("descendantOptionEnabled", Boolean.TRUE );
        SearchInstance.SearchValue searchValue = searchInstance.addTopLevelTerm("LCSET", configurableSearchDefinition);
        searchValue.setOperator(SearchInstance.Operator.EQUALS);
        searchValue.setValues(Collections.singletonList("LCSET-5102"));

        searchValue = searchInstance.addTopLevelTerm("EventDate", configurableSearchDefinition);
        searchValue.setOperator(SearchInstance.Operator.BETWEEN);
        ArrayList<String> dateVals = new ArrayList<>();
        dateVals.add("11/20/2013"); dateVals.add("11/21/2013");
        searchValue.setValues(dateVals);

        searchInstance.getPredefinedViewColumns().add("LabEventId");
        searchInstance.getPredefinedViewColumns().add("EventType");

        searchInstance.establishRelationships(configurableSearchDefinition);

        ConfigurableListFactory.FirstPageResults firstPageResults =
                configurableListFactory.getFirstResultsPage(
                        searchInstance, configurableSearchDefinition, null, 1, null, "ASC", "LabEvent" );

        Assert.assertEquals(firstPageResults.getPagination().getIdList().size(), 117);

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

        ConfigurableListFactory.FirstPageResults firstPageResults =
                configurableListFactory.getFirstResultsPage(
                        searchInstance, configurableSearchDefinition, null, 1, null, "ASC", "LabEvent" );

        Assert.assertEquals(firstPageResults.getPagination().getIdList().size(), 46);
        for (ConfigurableList.ResultRow resultRow : firstPageResults.getResultList().getResultRows()) {
            System.out.println(resultRow.getRenderableCells().get(1) + " " + resultRow.getRenderableCells().get(2));
        }


        // assert LCSET for every event, LCSET-6625 for the first 23, then LCSET-6875

//        Controller.stopProbeRecording(Controller.PROBE_NAME_JDBC);
//        Controller.stopCPURecording();
    }

}
