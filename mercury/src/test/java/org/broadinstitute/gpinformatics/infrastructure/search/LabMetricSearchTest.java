package org.broadinstitute.gpinformatics.infrastructure.search;

import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnEntity;
import org.broadinstitute.gpinformatics.infrastructure.columns.ConfigurableListFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.Collections;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

@Test(groups = TestGroups.STANDARD)
public class LabMetricSearchTest extends Arquillian {

    @Inject
    private ConfigurableListFactory configurableListFactory;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @Test
    public void testWorkRequest() {
        String entityName = ColumnEntity.LAB_METRIC.getEntityName();
        ConfigurableSearchDefinition searchDef = SearchDefinitionFactory.getForEntity(entityName);
        SearchInstance searchInstance = new SearchInstance();

        SearchInstance.SearchValue searchValue = searchInstance.addTopLevelTerm("Work Request ID", searchDef);
        searchValue.setOperator(SearchInstance.Operator.EQUALS);
        searchValue.setValues(Collections.singletonList("WR-49038"));

        searchInstance.getPredefinedViewColumns().add("Nearest Sample ID");
        searchInstance.getPredefinedViewColumns().add("Metric Value");

        searchInstance.establishRelationships(searchDef);
        ConfigurableListFactory.FirstPageResults firstPageResults = configurableListFactory.getFirstResultsPage(
                searchInstance, searchDef, null, 0, null, "DSC", entityName);
        firstPageResults.getResultList().getResultRows().size();
    }
}
