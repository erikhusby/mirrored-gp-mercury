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
        Assert.assertEquals(row.getRenderableCells().get(8), "0173519367");
        Assert.assertEquals(row.getRenderableCells().get(9), "A01");
        Assert.assertEquals(row.getRenderableCells().get(10), "2.38 ng/uL");
        Assert.assertEquals(row.getRenderableCells().get(11), "10/29/2014 13:26:13");

        // 2nd Catch Pico
        Assert.assertEquals(headers.get(12).getViewHeader(), "Newest Bait Catch Pico 121914 "
                + VesselMetricDetailsPlugin.MetricColumn.BARCODE.getDisplayName());
        Assert.assertEquals(row.getRenderableCells().get(12), "0173520489");
        Assert.assertEquals(row.getRenderableCells().get(13), "H10");
        Assert.assertEquals(row.getRenderableCells().get(14), "1.36 ng/uL");
        Assert.assertEquals(row.getRenderableCells().get(15), "12/19/2014 15:31:25");

        // Empty Plating Ribo
        Assert.assertEquals(headers.get(16).getViewHeader(), LabMetric.MetricType.PLATING_RIBO.getDisplayName()
                + " " + VesselMetricDetailsPlugin.MetricColumn.BARCODE.getDisplayName());
        Assert.assertEquals(row.getRenderableCells().get(16), "");
        Assert.assertEquals(row.getRenderableCells().get(17), "");
        Assert.assertEquals(row.getRenderableCells().get(18), "");
        Assert.assertEquals(headers.get(19).getViewHeader(), LabMetric.MetricType.PLATING_RIBO.getDisplayName()
                + " " + VesselMetricDetailsPlugin.MetricColumn.DATE.getDisplayName());
        Assert.assertEquals(row.getRenderableCells().get(19), "");

    }

}
