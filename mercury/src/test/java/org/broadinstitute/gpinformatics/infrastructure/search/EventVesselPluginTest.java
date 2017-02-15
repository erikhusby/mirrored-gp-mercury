package org.broadinstitute.gpinformatics.infrastructure.search;

import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnEntity;
import org.broadinstitute.gpinformatics.infrastructure.columns.ConfigurableList;
import org.broadinstitute.gpinformatics.infrastructure.columns.ConfigurableListFactory;
import org.broadinstitute.gpinformatics.infrastructure.columns.EventVesselSourcePositionPlugin;
import org.broadinstitute.gpinformatics.infrastructure.columns.EventVesselTargetPositionPlugin;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Validate that the event vessel plugin locates and builds container arrays correctly
 */
@Test(groups = TestGroups.STANDARD)
public class EventVesselPluginTest extends Arquillian {

    @Inject
    private LabEventDao labEventDao;

    @Inject
    private ConfigurableListFactory configurableListFactory;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    public void testNestedTablePlugin() {

        LabEvent labEvent = labEventDao.findById(LabEvent.class, 617246L);

        EventVesselSourcePositionPlugin eventVesselSourcePositionPlugin;
        try {
            eventVesselSourcePositionPlugin =
                    EventVesselSourcePositionPlugin.class.newInstance();
        } catch( Exception ex ) {
            throw new RuntimeException("Instantiation failure", ex );
        }

        // Need a search instance set in context
        ConfigurableSearchDefinition configurableSearchDefinition =
                SearchDefinitionFactory.getForEntity(ColumnEntity.LAB_EVENT.getEntityName());
        SearchInstance searchInstance = new SearchInstance();
        SearchInstance.SearchValue searchValue = searchInstance.addTopLevelTerm("LabEventId", configurableSearchDefinition);
        searchValue.setOperator(SearchInstance.Operator.EQUALS);
        searchValue.setValues(Collections.singletonList("617246"));
        searchInstance.getPredefinedViewColumns().add("Source Layout");
        searchInstance.getPredefinedViewColumns().add("Gender");
        searchInstance.establishRelationships(configurableSearchDefinition);
        searchInstance.postLoad();

        SearchContext searchContext = new SearchContext();
        searchContext.setSearchInstance(searchInstance);
        searchContext.setColumnEntityType(ColumnEntity.LAB_EVENT);
        searchContext.setSearchTerm(configurableSearchDefinition.getSearchTerm("Source Layout"));
        searchContext.setSearchValue(searchValue);

        ConfigurableList.ResultList resultList = eventVesselSourcePositionPlugin.getNestedTableData(labEvent, null, searchContext);
        // Matrix96 12 columns + label on left, 8 rows + header label
        // Event ia a PoolingTransfer of all tubes into A-01 position

        // Array conversion for spreadsheet export
        Object[][] arrayOutput = resultList.getAsArray();
        Assert.assertEquals( arrayOutput.length, 10, "Raw data array should have 10 discrete rows" );
        Assert.assertEquals( arrayOutput[0].length, 13, "Raw data array should have 13 discrete columns" );

        // Data
        Assert.assertEquals( resultList.getHeaders().size(), 13 );
        Assert.assertEquals( resultList.getResultRows().size(), 8 );

        Assert.assertEquals( resultList.getHeaders().get(0).getViewHeader(),  "");
        Assert.assertEquals( resultList.getHeaders().get(1).getViewHeader(),  "01");
        Assert.assertEquals( resultList.getHeaders().get(12).getViewHeader(), "12");
        Assert.assertEquals( resultList.getResultRows().get(0).getRenderableCells().get(0), "A");
        Assert.assertEquals( resultList.getResultRows().get(7).getRenderableCells().get(0), "H");
        Assert.assertEquals( resultList.getResultRows().get(0).getRenderableCells().get(2), "Vessel Barcode: 0173524221\nGender: [No Data]");

        EventVesselTargetPositionPlugin eventVesselTargetPositionPlugin;
        try {
            eventVesselTargetPositionPlugin =
                    EventVesselTargetPositionPlugin.class.newInstance();
        } catch( Exception ex ) {
            throw new RuntimeException("Instantiation failure", ex );
        }

        searchInstance.getPredefinedViewColumns().add("Destination Layout");
        searchInstance.establishRelationships(configurableSearchDefinition);
        searchInstance.postLoad();
        searchContext.setSearchTerm(configurableSearchDefinition.getSearchTerm("Destination Layout"));

        // ColumnTabulation not required for vessel position matrix
        resultList = eventVesselTargetPositionPlugin.getNestedTableData(labEvent, null, searchContext);
        // Matrix96 12 columns + label on left, 8 rows + header label
        Assert.assertEquals( resultList.getHeaders().size(), 13 );
        Assert.assertEquals( resultList.getResultRows().size(), 8 );

        Assert.assertEquals( resultList.getHeaders().get(0).getViewHeader(),  "");
        Assert.assertEquals( resultList.getHeaders().get(1).getViewHeader(),  "01");
        Assert.assertEquals( resultList.getHeaders().get(12).getViewHeader(), "12");
        Assert.assertEquals( resultList.getResultRows().get(0).getRenderableCells().get(0), "A");
        Assert.assertEquals( resultList.getResultRows().get(7).getRenderableCells().get(0), "H");
        Assert.assertEquals( resultList.getResultRows().get(0).getRenderableCells().get(1), "Vessel Barcode: 0116404353\nGender: [No Data]");

    }


    @Test
    public void testVesselLayout() {
//        Controller.startCPURecording(true);
        SearchInstance searchInstance = new SearchInstance();
        String entity = ColumnEntity.LAB_VESSEL.getEntityName();
        ConfigurableSearchDefinition configurableSearchDef = SearchDefinitionFactory.getForEntity(entity);

        SearchInstance.SearchValue searchValue = searchInstance.addTopLevelTerm("Barcode", configurableSearchDef);
        searchValue.setOperator(SearchInstance.Operator.EQUALS);
        searchValue.setValues(Collections.singletonList("HG7MCBBXX"));

        searchInstance.getPredefinedViewColumns().add("Layout");
        searchInstance.getPredefinedViewColumns().add("Nearest Sample ID");
        searchInstance.getPredefinedViewColumns().add("Molecular Index");
        searchInstance.getPredefinedViewColumns().add("Collaborator Sample ID");
        searchInstance.establishRelationships(configurableSearchDef);

        ConfigurableListFactory.FirstPageResults firstPageResults = configurableListFactory.getFirstResultsPage(
                searchInstance, configurableSearchDef, null, 0, null, "ASC", entity);
        List<ConfigurableList.ResultRow> resultRows = firstPageResults.getResultList().getResultRows();
        Assert.assertEquals(resultRows.size(), 38);
////        Controller.stopCPURecording();
//        Assert.assertEquals(resultRows.get(0).getRenderableCells().get(0), "CO-15138260");
//        Assert.assertEquals(resultRows.get(1).getRenderableCells().get(0), "CO-18828951");
//        Assert.assertEquals(resultRows.get(1).getRenderableCells().get(1), "000016825709");
    }
}
