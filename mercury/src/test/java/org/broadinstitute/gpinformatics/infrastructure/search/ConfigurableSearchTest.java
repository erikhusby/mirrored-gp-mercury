package org.broadinstitute.gpinformatics.infrastructure.search;

import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.athena.control.dao.preference.PreferenceDao;
import org.broadinstitute.gpinformatics.athena.entity.preference.Preference;
import org.broadinstitute.gpinformatics.athena.entity.preference.PreferenceType;
import org.broadinstitute.gpinformatics.athena.entity.preference.SearchInstanceList;
import org.broadinstitute.gpinformatics.infrastructure.columns.ConfigurableListFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test creating, saving, retrieving and executing a search.
 */
public class ConfigurableSearchTest extends ContainerTest {

    @Inject
    private SearchInstanceEjb searchInstanceEjb;

    @Inject
    private PreferenceDao preferenceDao;

    @Inject
    private ConfigurableListFactory configurableListFactory;

    @Test
    public void testX() {
        // Create instance
        SearchInstance searchInstance = new SearchInstance();
        SearchDefinitionFactory searchDefinitionFactory = new SearchDefinitionFactory();
        String entity = "LabEvent";
        ConfigurableSearchDefinition configurableSearchDef = searchDefinitionFactory.getForEntity(entity);
        SearchInstance.SearchValue searchValue = searchInstance.addTopLevelTerm("EventDate",
                configurableSearchDef);
        searchValue.setOperator(SearchInstance.Operator.BETWEEN);
        searchValue.setValues(Arrays.asList("6/2/2014", "6/3/2014"));

        // Add columns
        searchInstance.getPredefinedViewColumns().add("LabEventId");

        // Save instance
        Map<PreferenceType, Preference> mapTypeToPreference = new HashMap<>();
        Map<String,String> newSearchLevels = new HashMap<>();
        Map<String,String> searchInstanceNames = new HashMap<>();
        try {
            searchInstanceEjb.fetchInstances(10814l, SearchEntityType.LAB_VESSEL, mapTypeToPreference, searchInstanceNames, newSearchLevels);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String newSearchName = "Test" +  new SimpleDateFormat("MM/dd/yyyy").format(new Date(System.currentTimeMillis()));
        searchInstanceEjb.persistSearch(10814l, true, searchInstance, new MessageCollection(),
                PreferenceType.GLOBAL_LAB_VESSEL_SEARCH_INSTANCES, newSearchName, newSearchName, mapTypeToPreference);
        preferenceDao.flush();
        preferenceDao.clear();

        // Retrieve instance
        mapTypeToPreference.clear();
        searchInstanceNames.clear();
        newSearchLevels.clear();
        try {
            searchInstanceEjb.fetchInstances(10814l, SearchEntityType.LAB_VESSEL, mapTypeToPreference, searchInstanceNames, newSearchLevels);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Preference preference = mapTypeToPreference.get(PreferenceType.PreferenceScope.GLOBAL);
        SearchInstance fetchedSearchInstance = null;
        try {
            SearchInstanceList searchInstanceList =
                    (SearchInstanceList) preference.getPreferenceDefinition().getDefinitionValue();
            for (SearchInstance instance : searchInstanceList.getSearchInstances()) {
                if (instance.getName().equals(newSearchName)) {
                    fetchedSearchInstance = instance;
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Assert.assertNotNull(fetchedSearchInstance);

        // Search
        fetchedSearchInstance.establishRelationships(configurableSearchDef);
        fetchedSearchInstance.postLoad();

        ConfigurableListFactory.FirstPageResults firstPageResults = configurableListFactory.getFirstResultsPage(
                fetchedSearchInstance, configurableSearchDef, null, 1, null, "ASC", entity);
        Assert.assertEquals(firstPageResults.getResultList().getResultRows().size(), 100);

        // Delete instance
        searchInstanceEjb.deleteSearch(10814l,new MessageCollection(), PreferenceType.GLOBAL_LAB_VESSEL_SEARCH_INSTANCES, newSearchName, mapTypeToPreference);
    }
}
