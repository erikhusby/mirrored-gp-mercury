package org.broadinstitute.gpinformatics.infrastructure.search;

import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnEntity;
import org.broadinstitute.gpinformatics.infrastructure.columns.ConfigurableListFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import java.util.ArrayList;
import java.util.Collections;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

@Test(groups = TestGroups.STANDARD)
public class LabMetricSearchTest extends Arquillian {

    @Inject
    private ConfigurableListFactory configurableListFactory;

    @Inject
    private UserTransaction userTransaction;

    @Inject
    private UserBean userBean;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @Test
    public void testWorkRequest() throws SystemException, NotSupportedException {
        String entityName = ColumnEntity.LAB_METRIC.getEntityName();
        ConfigurableSearchDefinition searchDef = SearchDefinitionFactory.getForEntity(entityName);
        SearchInstance searchInstance = new SearchInstance();

        SearchInstance.SearchValue searchValue = searchInstance.addTopLevelTerm("Work Request ID", searchDef);
        searchValue.setOperator(SearchInstance.Operator.EQUALS);
        searchValue.setValues(Collections.singletonList("WR-49038"));

        searchInstance.getPredefinedViewColumns().add("Nearest Sample ID");
        searchInstance.getPredefinedViewColumns().add("Participant ID");
        searchInstance.getPredefinedViewColumns().add("Collaborator Sample ID");
        searchInstance.getPredefinedViewColumns().add("Collaborator Participant ID");
//        searchInstance.getPredefinedViewColumns().add("Sample Kit WR ID");
//        searchInstance.getPredefinedViewColumns().add("PICO ticket");
        searchInstance.getPredefinedViewColumns().add("Original Material Type");
        searchInstance.getPredefinedViewColumns().add("Material Type");
        searchInstance.getPredefinedViewColumns().add("Tumor / Normal");
        searchInstance.getPredefinedViewColumns().add("Root Sample ID");
//        searchInstance.getPredefinedViewColumns().add("PM");
        searchInstance.getPredefinedViewColumns().add("Volume");
        searchInstance.getPredefinedViewColumns().add("Metric Value");
        searchInstance.getPredefinedViewColumns().add("Metric Total ng");

        searchInstance.establishRelationships(searchDef);
        SearchContext evalContext = new SearchContext();
        evalContext.setUserBean(userBean);
        searchInstance.setEvalContext(evalContext);
        userTransaction.begin();
        ConfigurableListFactory.FirstPageResults firstPageResults = configurableListFactory.getFirstResultsPage(
                searchInstance, searchDef, null, 0, null, "DSC", entityName);
        Assert.assertEquals(firstPageResults.getResultList().getResultRows().size(), 38);
        userTransaction.rollback();
    }

    @Test
    public void testCollection() throws SystemException, NotSupportedException {
        String entityName = ColumnEntity.LAB_METRIC.getEntityName();
        ConfigurableSearchDefinition searchDef = SearchDefinitionFactory.getForEntity(entityName);
        SearchInstance searchInstance = new SearchInstance();

        SearchInstance.SearchValue searchValue = searchInstance.addTopLevelTerm("Group / Collection", searchDef);
        searchValue.setOperator(SearchInstance.Operator.EQUALS);
        searchValue.setValues(Collections.singletonList("Cancer Genome Analysis"));

        ArrayList<SearchInstance.SearchValue> children = new ArrayList<>();
        SearchInstance.SearchValue child = new SearchInstance.SearchValue();
        child.setTermName("Collection");
        child.setOperator(SearchInstance.Operator.EQUALS);
        child.setValues(Collections.singletonList("19255"));
//        child.setValues(Collections.singletonList("26238"));  // 100K samples
        children.add(child);
        searchValue.setChildren(children);

        searchInstance.getPredefinedViewColumns().add("Nearest Sample ID");
        searchInstance.getPredefinedViewColumns().add("Metric Value");

        searchInstance.establishRelationships(searchDef);
        userTransaction.begin();
        ConfigurableListFactory.FirstPageResults firstPageResults = configurableListFactory.getFirstResultsPage(
                searchInstance, searchDef, null, 0, null, "DSC", entityName);
        Assert.assertTrue(firstPageResults.getResultList().getResultRows().size() > 16);
        userTransaction.rollback();
    }
}
