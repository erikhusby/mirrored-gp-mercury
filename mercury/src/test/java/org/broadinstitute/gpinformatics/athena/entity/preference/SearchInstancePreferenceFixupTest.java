package org.broadinstitute.gpinformatics.athena.entity.preference;

import org.broadinstitute.gpinformatics.athena.control.dao.preference.PreferenceDao;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchInstance;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Fixup test to modify existing saved SearchInstance preferences
 */
@Test(groups = TestGroups.FIXUP)
public class SearchInstancePreferenceFixupTest extends Arquillian {

    @Inject
    private PreferenceDao preferenceDao;

    @Inject
    private UserBean userBean;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @Test(enabled = false)
    public void modifyResultColumnNamesGPLIM3955() {
        int replaceCount = 0;
        try {
            userBean.loginOSUser();

            PreferenceType[] preferenceTypes = {
                    PreferenceType.GLOBAL_LAB_EVENT_SEARCH_INSTANCES,
                    PreferenceType.GLOBAL_LAB_VESSEL_SEARCH_INSTANCES,
                    PreferenceType.USER_LAB_EVENT_SEARCH_INSTANCES,
                    PreferenceType.USER_LAB_VESSEL_SEARCH_INSTANCES};

            replaceCount += renameSavedResultColumn("Mercury Sample ID", "Root Sample ID", preferenceTypes);

            preferenceDao.persist(new FixupCommentary("GPLIM-3955 Rename " + replaceCount
                    + " Saved Search Column/Term 'Mercury Sample ID' to 'Root Sample ID'"));

            preferenceDao.flush();

        } catch ( Exception e ) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Renames result columns in each SearchInstance in the supplied PreferenceType array
     * @return number of renames made
     * @throws Exception
     */
    private int renameSavedResultColumn( String oldName, String newName, PreferenceType[] preferenceTypes )
            throws Exception {
        int replaceCount = 0;

        for( int i = 0; i < preferenceTypes.length; i++ ) {
            PreferenceType preferenceType = preferenceTypes[i];
            List<Preference> preferences = preferenceDao.getPreferences(preferenceType);
            for( Preference preference : preferences ) {

                if (preference.getData() == null || preference.getData().isEmpty()) {
                    return 0;
                }

                SearchInstanceList searchInstanceList =
                        (SearchInstanceList) preference.getPreferenceDefinition().getDefinitionValue();

                if (searchInstanceList == null || searchInstanceList.getSearchInstances().isEmpty()) {
                    return 0;
                }

                for (SearchInstance searchInstance : searchInstanceList.getSearchInstances()) {
                    List<String> names = searchInstance.getPredefinedViewColumns();
                    for (int j = 0; j < names.size(); j++) {
                        if (names.get(j).equals(oldName)) {
                            System.out.println(names.get(j));
                            names.set(j, newName);
                            replaceCount++;
                        }
                    }
                }

                preference.setData(searchInstanceList.marshal());
            }
        }

        return replaceCount;
    }
}