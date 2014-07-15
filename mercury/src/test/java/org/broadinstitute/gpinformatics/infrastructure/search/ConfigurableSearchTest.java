package org.broadinstitute.gpinformatics.infrastructure.search;

import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.athena.control.dao.preference.PreferenceDao;
import org.broadinstitute.gpinformatics.athena.entity.preference.Preference;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.ArrayList;
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

    @Test
    public void testX() {
        // Create instance
        SearchInstance searchInstance = new SearchInstance();
        SearchDefinitionFactory searchDefinitionFactory = new SearchDefinitionFactory();
        searchInstance.addTopLevelTerm("EventDate", searchDefinitionFactory.getForEntity("LabEvent"));

        // Add columns
        searchInstance.getPredefinedViewColumns().add("LabEventId");

        // Save instance
        Map<String, Preference> preferenceMap = new HashMap<>();
        List<String> searchInstanceNames = new ArrayList<>();
        List<String> newSearchLevels = new ArrayList<>();
        searchInstanceEjb.fetchInstances(preferenceMap, searchInstanceNames, newSearchLevels);
        searchInstanceEjb.persistSearch(true, searchInstance, new MessageCollection(), "GLOBAL", "Test", "Test",
                preferenceMap);
        preferenceDao.flush();
        preferenceDao.clear();

        // Retrieve instance
        searchInstanceEjb.fetchInstances(preferenceMap, searchInstanceNames, newSearchLevels);
//        Assert.assertEquals(preferenceMap.get(), );
        // Search
        // Download
        // Delete instance
    }
}
