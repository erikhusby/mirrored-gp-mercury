package org.broadinstitute.gpinformatics.infrastructure.search;

import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnEntity;
import org.broadinstitute.gpinformatics.infrastructure.columns.ConfigurableList;
import org.broadinstitute.gpinformatics.infrastructure.columns.ConfigurableListFactory;
import org.broadinstitute.gpinformatics.infrastructure.columns.VesselMetricDetailsPlugin;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
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
 * Validate that the vessel metric detail plugin locates ancestor and descendant metrics and and builds columns correctly
 */
@Test(groups = TestGroups.STANDARD)
public class VesselMetricDetailsPluginTest extends Arquillian {

    @Inject
    private ConfigurableListFactory configurableListFactory;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    /**
     * Vessel metrics details traverses ancestors and descendants locating all metrics of a specific type
     */
    public void testVesselMetricDetailsPlugin() {

        String linkBaseUrl = "http://dont/go/here";

        // Need a search instance set in context
        ConfigurableSearchDefinition configurableSearchDefinition =
                SearchDefinitionFactory.getForEntity(ColumnEntity.LAB_VESSEL.getEntityName());
        SearchInstance searchInstance = new SearchInstance();
        SearchInstance.SearchValue searchValue = searchInstance.addTopLevelTerm("Barcode", configurableSearchDefinition);
        searchValue.setOperator(SearchInstance.Operator.EQUALS);
        searchValue.setValues(Collections.singletonList("0160423723"));
        searchValue.setIncludeInResults(Boolean.FALSE);
        // Ancestor
        searchInstance.getPredefinedViewColumns().add(LabMetric.MetricType.INITIAL_PICO.getDisplayName());
        // Current
        searchInstance.getPredefinedViewColumns().add(LabMetric.MetricType.POND_PICO.getDisplayName());
        // Descendant
        searchInstance.getPredefinedViewColumns().add(LabMetric.MetricType.CATCH_PICO.getDisplayName());
        // Nothing
        searchInstance.getPredefinedViewColumns().add(LabMetric.MetricType.PLATING_RIBO.getDisplayName());
        searchInstance.establishRelationships(configurableSearchDefinition);

        // Force web output (drill down links vs. value lists)
        SearchContext searchContext = new SearchContext();
        searchContext.setBaseSearchURL( new StringBuffer(linkBaseUrl) );
        searchContext.setResultCellTargetPlatform(SearchContext.ResultCellTargetPlatform.WEB);

        searchInstance.setEvalContext(searchContext);

        searchInstance.postLoad();

        ConfigurableListFactory.FirstPageResults firstPageResults = configurableListFactory.getFirstResultsPage(
                searchInstance, configurableSearchDefinition, null, 1, null, "ASC", ColumnEntity.LAB_VESSEL.getEntityName());

        List<ConfigurableList.ResultRow> rows = firstPageResults.getResultList().getResultRows();
        List<ConfigurableList.Header> headers = firstPageResults.getResultList().getHeaders();

        Assert.assertEquals(rows.size(), 1);
        Assert.assertEquals(headers.size(), 20);

        ConfigurableList.ResultRow row = rows.get(0);
        // Initial Pico
        Assert.assertEquals(headers.get(0).getViewHeader(), "10-22-14_LCSET-6330_Initial Pico "
                + VesselMetricDetailsPlugin.MetricColumn.BARCODE.getDisplayName());
        Assert.assertEquals(row.getRenderableCells().get(0), "0175362245");
        Assert.assertEquals(row.getRenderableCells().get(1), "E05");
        Assert.assertEquals(row.getRenderableCells().get(2), "11.18 ng/uL");
        Assert.assertEquals(row.getRenderableCells().get(3), "10/22/2014 11:41:51");

        // Pond Pico
        Assert.assertEquals(headers.get(4).getViewHeader(), "10-24-14_LCSET-6330_Pond Pico "
                + VesselMetricDetailsPlugin.MetricColumn.BARCODE.getDisplayName());
        Assert.assertEquals(row.getRenderableCells().get(4), "0160423723");
        Assert.assertEquals(row.getRenderableCells().get(5), "E05");
        Assert.assertEquals(row.getRenderableCells().get(6), "78.17 ng/uL");
        Assert.assertEquals(row.getRenderableCells().get(7), "10/24/2014 16:00:31");

        // 1st Catch Pico
        Assert.assertEquals(headers.get(8).getViewHeader(), "10-29-14_LCSET-6330_Buick Val_Catch "
                + VesselMetricDetailsPlugin.MetricColumn.BARCODE.getDisplayName());

        // Catch Pico spans 8 tubes, see that the column (actually a web link) has them all
        String multiValLink = row.getRenderableCells().get(8);
        String[] firstCatchBarcodes = {"0173519385","0173519387","0173519377","0173519344","0173519367","0173519391","0173519410","0173519390"};
        for( String barcode : firstCatchBarcodes ) {
            Assert.assertTrue( multiValLink.contains(barcode), "Drill down link should include barcode " + barcode );
        }
        Assert.assertTrue( multiValLink.contains("Metric Run ID"), "Drill down link should include term: Metric Run ID");
        Assert.assertTrue( multiValLink.contains(linkBaseUrl), "Drill down link should include server URL");

        // 9 thru 11 are placeholders for multiple positions/values/date

        // 2nd Catch Pico
        Assert.assertEquals(headers.get(12).getViewHeader(), "Newest Bait Catch Pico 121914 "
                + VesselMetricDetailsPlugin.MetricColumn.BARCODE.getDisplayName());
        // 12 thru 15 are multiple barcodes/positions/values/date

        // Empty Plating Ribo
        Assert.assertEquals(headers.get(16).getViewHeader(), LabMetric.MetricType.PLATING_RIBO.getDisplayName()
                + " " + VesselMetricDetailsPlugin.MetricColumn.BARCODE.getDisplayName());
        Assert.assertNull(row.getRenderableCells().get(16));
        Assert.assertNull(row.getRenderableCells().get(17));
        Assert.assertNull(row.getRenderableCells().get(18));
        Assert.assertEquals(headers.get(19).getViewHeader(), LabMetric.MetricType.PLATING_RIBO.getDisplayName()
                + " " + VesselMetricDetailsPlugin.MetricColumn.DATE.getDisplayName());
        Assert.assertNull(row.getRenderableCells().get(19));

    }

}
