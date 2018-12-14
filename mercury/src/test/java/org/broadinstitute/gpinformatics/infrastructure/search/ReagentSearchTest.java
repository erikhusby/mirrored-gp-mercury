package org.broadinstitute.gpinformatics.infrastructure.search;

import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnEntity;
import org.broadinstitute.gpinformatics.infrastructure.columns.ConfigurableList;
import org.broadinstitute.gpinformatics.infrastructure.columns.ConfigurableListFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.Collections;

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
    // Bug testng 6.10 - expected exception isn't caught!:
    // @Test(expectedExceptions = {InformaticsServiceException.class}, expectedExceptionsMessageRegExp = ".* exclusive .*" )
    public void testLcsetTermIsExclusive() {
        ConfigurableSearchDefinition configurableSearchDefinition =
                SearchDefinitionFactory.getForEntity(ColumnEntity.REAGENT.getEntityName());

        SearchInstance searchInstance = new SearchInstance();
        SearchInstance.SearchValue searchValue = searchInstance.addTopLevelTerm("LCSET", configurableSearchDefinition);
        searchValue.setOperator(SearchInstance.Operator.EQUALS);
        searchValue.setValues(Collections.singletonList("LCSET-6049"));

        searchValue = searchInstance.addTopLevelTerm("Lot Number", configurableSearchDefinition);
        searchValue.setOperator(SearchInstance.Operator.EQUALS);
        searchValue.setValues(Collections.singletonList("Here To Fail"));

        searchInstance.getPredefinedViewColumns().add("Reagent Name");

        searchInstance.establishRelationships(configurableSearchDefinition);

        try {
            ConfigurableListFactory.FirstPageResults firstPageResults =
                    configurableListFactory.getFirstResultsPage(
                            searchInstance, configurableSearchDefinition, null, 1, null, "ASC", "LabEvent" );
        } catch ( InformaticsServiceException ex ) {
            // Do this manually because testng 6.10 doesn't ignore expectedExceptionsMessageRegExp
            Assert.assertTrue(ex.getMessage().contains( " exclusive "), "Expected an exception refererencing exclusive search term mis-use.");
            return;
        }

        Assert.fail( "Expected exclusive search term exception is not thrown.");
    }

    /**
     * Validates traversal of descendant events for the reagents in an LCSET
     */
    public void testLcsetReagentSearch() {
        ConfigurableSearchDefinition configurableSearchDefinition =
                SearchDefinitionFactory.getForEntity(ColumnEntity.REAGENT.getEntityName());

        SearchInstance searchInstance = new SearchInstance();
        SearchInstance.SearchValue searchValue = searchInstance.addTopLevelTerm("LCSET", configurableSearchDefinition);
        searchValue.setOperator(SearchInstance.Operator.EQUALS);
        // Choose a simple and fast rework LCSET
        searchValue.setValues(Collections.singletonList("LCSET-6049"));

        searchInstance.getPredefinedViewColumns().add("Reagent Name");
        searchInstance.getPredefinedViewColumns().add("Lot Number");
        searchInstance.getPredefinedViewColumns().add("Expiration Date");

        searchInstance.establishRelationships(configurableSearchDefinition);

        // Validate side effect of using an alternate search definition
        Assert.assertFalse(searchInstance.getIsDbSortable(),
                "DB sorting is disabled for results from terms with alternate search definitions.");

        ConfigurableListFactory.FirstPageResults firstPageResults =
                configurableListFactory.getFirstResultsPage(
                        searchInstance, configurableSearchDefinition, null, 0, null, "DSC", "Reagent");

        ConfigurableList.ResultList resultList = firstPageResults.getResultList();
        Assert.assertEquals(resultList.getHeaders().size(), 3);

        // Sort is first column descending
        Assert.assertEquals(resultList.getResultRows().get(0).getRenderableCells().get(0), "ETOH70");
        Assert.assertEquals(resultList.getResultRows().get(1).getRenderableCells().get(0), "AMPUREXP");
        Assert.assertEquals(resultList.getResultRows().get(2).getRenderableCells().get(0), "10MMTRISH8");

        // Re-sort on reagent lot ascending
        configurableListFactory.getFirstResultsPage(
                searchInstance, configurableSearchDefinition, null, 1, null, "ASC", "Reagent");
        resultList = firstPageResults.getResultList();

        // Verify sort is second column ascending
        Assert.assertEquals(resultList.getResultRows().get(0).getRenderableCells().get(1), "13G16A0008");
        Assert.assertEquals(resultList.getResultRows().get(1).getRenderableCells().get(1), "14C19A0006");
        Assert.assertEquals(resultList.getResultRows().get(2).getRenderableCells().get(1), "14F04A0001");

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
