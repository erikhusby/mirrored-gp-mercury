package org.broadinstitute.gpinformatics.infrastructure.search;

import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnEntity;
import org.broadinstitute.gpinformatics.infrastructure.columns.ConfigurableListFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;

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
        // Select descendants  (Assuming it to be second option in TraversalEvaluator
        searchInstance.getTraversalEvaluatorValues().put(configurableSearchDefinition.getTraversalEvaluator().getTraversalOptions().get(1).getId(), Boolean.TRUE );
        SearchInstance.SearchValue searchValue = searchInstance.addTopLevelTerm("LCSET", configurableSearchDefinition);
        searchValue.setOperator(SearchInstance.Operator.EQUALS);
        searchValue.setValues(Collections.singletonList("LCSET-5102"));

        searchValue = searchInstance.addTopLevelTerm("EventDate", configurableSearchDefinition);
        searchValue.setOperator(SearchInstance.Operator.BETWEEN);
        ArrayList<String> dateVals = new ArrayList<>();
        dateVals.add("11/20/2013"); dateVals.add("11/21/2013");
        searchValue.setValues(dateVals);

        searchInstance.getPredefinedViewColumns().add("LabEventId");

        searchInstance.establishRelationships(configurableSearchDefinition);

        ConfigurableListFactory.FirstPageResults firstPageResults =
                configurableListFactory.getFirstResultsPage(
                        searchInstance, configurableSearchDefinition, null, 1, null, "ASC", "LabEvent" );

        Assert.assertEquals(firstPageResults.getPagination().getIdList().size(), 117);

    }

}
