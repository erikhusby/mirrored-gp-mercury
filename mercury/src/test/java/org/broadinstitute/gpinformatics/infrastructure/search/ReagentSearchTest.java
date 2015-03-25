package org.broadinstitute.gpinformatics.infrastructure.search;

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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Validate the Reagent search functionality
 */
@Test(groups = TestGroups.STANDARD)
public class ReagentSearchTest extends Arquillian {

    @Inject
    private ConfigurableListFactory configurableListFactory;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    /**
     * Validate rejection of a search with any other term(s) in addition to one declared as exclusive
     */
    @Test( expectedExceptions = {IllegalArgumentException.class},expectedExceptionsMessageRegExp = ".*exclusive.*")
    public void testLcsetTermIsExclusive() {
        ConfigurableSearchDefinition configurableSearchDefinition =
                SearchDefinitionFactory.getForEntity(ColumnEntity.REAGENT.getEntityName());

        SearchInstance searchInstance = new SearchInstance();
        SearchInstance.SearchValue searchValue = searchInstance.addTopLevelTerm("LCSET Events", configurableSearchDefinition);
        searchValue.setOperator(SearchInstance.Operator.EQUALS);
        searchValue.setValues(Collections.singletonList("LCSET-6049"));

        searchValue = searchInstance.addTopLevelTerm("Lot Number", configurableSearchDefinition);
        searchValue.setOperator(SearchInstance.Operator.EQUALS);
        searchValue.setValues(Collections.singletonList("Here To Fail"));

        searchInstance.getPredefinedViewColumns().add("Reagent Name");

        searchInstance.establishRelationships(configurableSearchDefinition);

        ConfigurableListFactory.FirstPageResults firstPageResults =
                configurableListFactory.getFirstResultsPage(
                        searchInstance, configurableSearchDefinition, null, 1, null, "ASC", "LabEvent" );

        Assert.assertEquals(firstPageResults.getPagination().getIdList().size(), 117);

    }

    /**
     * Validates traversal of descendant events for the reagents in an LCSET
     */
    public void testLcsetReagentSearch() {
        ConfigurableSearchDefinition configurableSearchDefinition =
                SearchDefinitionFactory.getForEntity(ColumnEntity.REAGENT.getEntityName());

        SearchInstance searchInstance = new SearchInstance();
        SearchInstance.SearchValue searchValue = searchInstance.addTopLevelTerm("LCSET Events", configurableSearchDefinition);
        searchValue.setOperator(SearchInstance.Operator.EQUALS);
        // Choose a simple and fast rework LCSET
        searchValue.setValues(Collections.singletonList("LCSET-6049"));

        searchInstance.getPredefinedViewColumns().add("Reagent Name");
        searchInstance.getPredefinedViewColumns().add("Lot Number");
        searchInstance.getPredefinedViewColumns().add("Expiration Date");

        searchInstance.establishRelationships(configurableSearchDefinition);

        ConfigurableListFactory.FirstPageResults firstPageResults =
                configurableListFactory.getFirstResultsPage(
                        searchInstance, configurableSearchDefinition, null, 1, null, "ASC", "Reagent" );

        ConfigurableList.ResultList resultList = firstPageResults.getResultList();
        Assert.assertEquals(resultList.getHeaders().size(), 3);

        Set<String> reagentNames = new HashSet<>();
        reagentNames.add("10MMTRISH8");
        reagentNames.add("AMPUREXP");
        reagentNames.add("ETOH70");

        String error = "Expected reagent not found in search";
        Assert.assertTrue(reagentNames.contains(resultList.getResultRows().get(0).getRenderableCells().get(0)), error);
        Assert.assertTrue(reagentNames.contains(resultList.getResultRows().get(1).getRenderableCells().get(0)), error);
        Assert.assertTrue(reagentNames.contains(resultList.getResultRows().get(2).getRenderableCells().get(0)), error);
    }

    /**
     * Validates simple reagent search
     */
    public void testReagentSearch() {
        ConfigurableSearchDefinition configurableSearchDefinition =
                SearchDefinitionFactory.getForEntity(ColumnEntity.REAGENT.getEntityName());

        String lotNumber = "14C19A0006";
        SearchInstance searchInstance = new SearchInstance();
        SearchInstance.SearchValue searchValue = searchInstance.addTopLevelTerm("Lot Number", configurableSearchDefinition);
        searchValue.setOperator(SearchInstance.Operator.EQUALS);
        searchValue.setValues(Collections.singletonList(lotNumber));

        searchInstance.getPredefinedViewColumns().add("Reagent Name");
        searchInstance.getPredefinedViewColumns().add("Lot Number");
        searchInstance.getPredefinedViewColumns().add("Expiration Date");

        searchInstance.establishRelationships(configurableSearchDefinition);

        ConfigurableListFactory.FirstPageResults firstPageResults =
                configurableListFactory.getFirstResultsPage(
                        searchInstance, configurableSearchDefinition, null, 1, null, "ASC", "Reagent" );

        ConfigurableList.ResultList resultList = firstPageResults.getResultList();
        Assert.assertEquals( resultList.getResultRows().size(), 1 );
        Assert.assertEquals( resultList.getResultRows().get(0).getRenderableCells().get(0), "AMPUREXP" );
        Assert.assertEquals( resultList.getResultRows().get(0).getRenderableCells().get(1), lotNumber );
        // Null expiration date might be part of a future fix-up - ignore
        //Assert.assertEquals( resultList.getResultRows().get(0).getRenderableCells().get(0), "" );
    }

}
