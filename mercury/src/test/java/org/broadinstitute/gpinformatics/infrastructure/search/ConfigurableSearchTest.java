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
        Map<PreferenceType.PreferenceScope, Preference> mapScopeToPreference = new HashMap<>();
        List<String> searchInstanceNames = new ArrayList<>();
        List<String> newSearchLevels = new ArrayList<>();
        searchInstanceEjb.fetchInstances(mapScopeToPreference, searchInstanceNames, newSearchLevels);
        String newSearchName = "Test" +  new SimpleDateFormat("MM/dd/yyyy").format(new Date(System.currentTimeMillis()));
        searchInstanceEjb.persistSearch(true, searchInstance, new MessageCollection(),
                PreferenceType.PreferenceScope.GLOBAL, newSearchName, newSearchName, mapScopeToPreference);
        preferenceDao.flush();
        preferenceDao.clear();

        // Retrieve instance
        mapScopeToPreference.clear();
        searchInstanceNames.clear();
        newSearchLevels.clear();
        searchInstanceEjb.fetchInstances(mapScopeToPreference, searchInstanceNames, newSearchLevels);
        Preference preference = mapScopeToPreference.get(PreferenceType.PreferenceScope.GLOBAL);
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
        searchInstanceEjb.deleteSearch(new MessageCollection(), newSearchName, mapScopeToPreference);
    }
}
