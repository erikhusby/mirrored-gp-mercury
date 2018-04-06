package org.broadinstitute.gpinformatics.athena.entity.preference;

import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.gpinformatics.athena.control.dao.preference.PreferenceDao;
import org.broadinstitute.gpinformatics.infrastructure.search.LabVesselSearchDefinition;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
     * Growing list of event specific search terms replaced by user configurable result column parameters
     */
    @Test(enabled = false)
    public void modifyResultColumnNamesGPLIM4941() {
        int replaceCount = 0;
        try {
            userBean.loginOSUser();

            PreferenceType[] preferenceTypes = {
                    PreferenceType.GLOBAL_LAB_VESSEL_SEARCH_INSTANCES,
                    PreferenceType.USER_LAB_VESSEL_SEARCH_INSTANCES};

            List<Pair<String,String>> swaps = new ArrayList<>();
            swaps.add( Pair.of( "Imported Sample Tube Barcode", "{\"searchTermName\":\"Event Vessel Barcodes\",\"userColumnName\":\"Imported Sample Tube Barcode\",\"paramValues\":[{\"name\":\"userColumnName\",\"value\":\"Imported Sample Tube Barcode\"},{\"name\":\"eventTypes\",\"value\":\"SAMPLE_IMPORT\"},{\"name\":\"srcOrTarget\",\"value\":\"target\"},{\"name\":\"captureNearest\",\"value\":\"captureNearest\"}]}") );
            swaps.add( Pair.of( "Imported Sample Position", "{\"searchTermName\":\"Event Vessel Positions\",\"userColumnName\":\"Imported Sample Position\",\"paramValues\":[{\"name\":\"userColumnName\",\"value\":\"Imported Sample Position\"},{\"name\":\"eventTypes\",\"value\":\"SAMPLE_IMPORT\"},{\"name\":\"srcOrTarget\",\"value\":\"target\"},{\"name\":\"captureNearest\",\"value\":\"captureNearest\"}]}") );
            swaps.add( Pair.of( "Pond Tube Barcode", "{\"searchTermName\":\"Event Vessel Barcodes\",\"userColumnName\":\"Pond Tube Barcode\",\"paramValues\":[{\"name\":\"userColumnName\",\"value\":\"Pond Tube Barcode\"},{\"name\":\"eventTypes\",\"value\":\"POND_REGISTRATION\"},{\"name\":\"eventTypes\",\"value\":\"PCR_FREE_POND_REGISTRATION\"},{\"name\":\"eventTypes\",\"value\":\"PCR_PLUS_POND_REGISTRATION\"},{\"name\":\"srcOrTarget\",\"value\":\"target\"}]}") );
            swaps.add( Pair.of( "Pond Sample Position", "{\"searchTermName\":\"Event Vessel Positions\",\"userColumnName\":\"Pond Sample Position\",\"paramValues\":[{\"name\":\"userColumnName\",\"value\":\"Pond Sample Position\"},{\"name\":\"eventTypes\",\"value\":\"POND_REGISTRATION\"},{\"name\":\"eventTypes\",\"value\":\"PCR_FREE_POND_REGISTRATION\"},{\"name\":\"eventTypes\",\"value\":\"PCR_PLUS_POND_REGISTRATION\"},{\"name\":\"srcOrTarget\",\"value\":\"target\"}]}") );
            swaps.add( Pair.of( "Norm Pond Tube Barcode", "{\"searchTermName\":\"Event Vessel Barcodes\",\"userColumnName\":\"Norm Pond Tube Barcode\",\"paramValues\":[{\"name\":\"userColumnName\",\"value\":\"Norm Pond Tube Barcode\"},{\"name\":\"eventTypes\",\"value\":\"PCR_PLUS_POND_NORMALIZATION\"},{\"name\":\"srcOrTarget\",\"value\":\"target\"}]}") );
            swaps.add( Pair.of( "Shearing Sample Position", "{\"searchTermName\":\"Event Vessel Positions\",\"userColumnName\":\"Shearing Sample Position\",\"paramValues\":[{\"name\":\"userColumnName\",\"value\":\"Shearing Sample Position\"},{\"name\":\"eventTypes\",\"value\":\"SHEARING_TRANSFER\"},{\"name\":\"srcOrTarget\",\"value\":\"source\"}]}") );
            swaps.add( Pair.of( "Shearing Sample Barcode", "{\"searchTermName\":\"Event Vessel Barcodes\",\"userColumnName\":\"Shearing Sample Barcode\",\"paramValues\":[{\"name\":\"userColumnName\",\"value\":\"Shearing Sample Barcode\"},{\"name\":\"eventTypes\",\"value\":\"SHEARING_TRANSFER\"},{\"name\":\"srcOrTarget\",\"value\":\"source\"}]}") );
            swaps.add( Pair.of( "Catch Sample Position", "{\"searchTermName\":\"Event Vessel Positions\",\"userColumnName\":\"Catch Sample Position\",\"paramValues\":[{\"name\":\"userColumnName\",\"value\":\"Catch Sample Position\"},{\"name\":\"eventTypes\",\"value\":\"ICE_CATCH_ENRICHMENT_CLEANUP\"},{\"name\":\"eventTypes\",\"value\":\"NORMALIZED_CATCH_REGISTRATION\"},{\"name\":\"srcOrTarget\",\"value\":\"source\"}]}") );
            swaps.add( Pair.of( "Catch Tube Barcode", "{\"searchTermName\":\"Event Vessel Barcodes\",\"userColumnName\":\"Catch Tube Barcode\",\"paramValues\":[{\"name\":\"userColumnName\",\"value\":\"Catch Tube Barcode\"},{\"name\":\"eventTypes\",\"value\":\"ICE_CATCH_ENRICHMENT_CLEANUP\"},{\"name\":\"eventTypes\",\"value\":\"NORMALIZED_CATCH_REGISTRATION\"},{\"name\":\"srcOrTarget\",\"value\":\"source\"}]}") );
            swaps.add( Pair.of( "Flowcell Barcode", "{\"searchTermName\":\"Event Vessel Barcodes\",\"userColumnName\":\"Flowcell Barcode\",\"paramValues\":[{\"name\":\"userColumnName\",\"value\":\"Flowcell Barcode\"},{\"name\":\"eventTypes\",\"value\":\"FLOWCELL_TRANSFER\"},{\"name\":\"eventTypes\",\"value\":\"DENATURE_TO_FLOWCELL_TRANSFER\"},{\"name\":\"eventTypes\",\"value\":\"DILUTION_TO_FLOWCELL_TRANSFER\"},{\"name\":\"srcOrTarget\",\"value\":\"source\"}]}") );

            for( Pair<String,String> fromTo : swaps ) {
                replaceCount += renameSavedResultColumn(fromTo.getLeft(), fromTo.getRight(), preferenceTypes);
            }

            preferenceDao.persist(new FixupCommentary("GPLIM-4941 Modified " + replaceCount
                    + " Saved Search Columns to use result column parameters"));

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

    /**
     * GPLIM-4761 Array UDS refactoring
     */
    @Test(enabled=false)
    public void refactorArraySearchesGPLIM4761(){

        int replaceCount = 0;
        try {
            userBean.loginOSUser();

            // Do the global search instance
            List<Preference> preferences = preferenceDao.getPreferences(PreferenceType.GLOBAL_LAB_VESSEL_SEARCH_INSTANCES);
            for (Preference preference : preferences) {
                int nbrFixed = fixGPLIM4761Preference( preference );
                if( nbrFixed > 0 ) {
                    replaceCount += nbrFixed;
                }
            }

            // Do the user search instances
            preferences = preferenceDao.getPreferences(PreferenceType.USER_LAB_VESSEL_SEARCH_INSTANCES);
            for (Preference preference : preferences) {
                int nbrFixed = fixGPLIM4761Preference( preference );
                if( nbrFixed > 0 ) {
                    replaceCount += nbrFixed;
                }
            }

            preferenceDao.persist(new FixupCommentary("GPLIM-4761 Array UDS refactoring, " + replaceCount
                    + " saved searches modified"));

            preferenceDao.flush();

        } catch ( Exception e ) {
            throw new RuntimeException(e);
        }
    }

    private int fixGPLIM4761Preference(Preference preference ) {
        int count = 0;
        SearchInstanceList searchInstanceList;

        Set<String> infiniumTermNames = new HashSet<>();
        infiniumTermNames.add(LabVesselSearchDefinition.MultiRefTerm.INFINIUM_AMP_PLATE.getTermRefName());
        infiniumTermNames.add(LabVesselSearchDefinition.MultiRefTerm.INFINIUM_CHIP.getTermRefName());
        infiniumTermNames.add(LabVesselSearchDefinition.MultiRefTerm.INFINIUM_DNA_PLATE.getTermRefName());

        StringBuilder statusOutput = new StringBuilder();
        try {
            // Unmarshall
            searchInstanceList =
                    (SearchInstanceList) preference.getPreferenceDefinition().getDefinitionValue();

            for( SearchInstance searchInstance : searchInstanceList.getSearchInstances() ) {
                boolean isInfinium = false;

                for( SearchInstance.SearchValue searchValue : searchInstance.getSearchValues() ) {
                    String termName = searchValue.getTermName();
                    if( termName.equals(LabVesselSearchDefinition.MultiRefTerm.INFINIUM_DNA_PLATE.getTermRefName() ) ) {
                        searchInstance.setExcludeInitialEntitiesFromResults(true);
                        searchInstance.setCustomTraversalOptionName("infiniumWells");
                        searchInstance.getTraversalEvaluatorValues().put("descendantOptionEnabled", Boolean.TRUE);
                        isInfinium = true;
                        statusOutput.append( "Modified named search [");
                        statusOutput.append(searchInstance.getName());
                        statusOutput.append("] (DNA plate term)\n");
                    } else if( termName.equals(LabVesselSearchDefinition.MultiRefTerm.INFINIUM_AMP_PLATE.getTermRefName() )
                            || termName.equals(LabVesselSearchDefinition.MultiRefTerm.INFINIUM_CHIP.getTermRefName() ) ) {
                        searchInstance.setExcludeInitialEntitiesFromResults(true);
                        searchInstance.setCustomTraversalOptionName("infiniumWells");
                        searchInstance.getTraversalEvaluatorValues().put("ancestorOptionEnabled", Boolean.TRUE);
                        isInfinium = true;
                        statusOutput.append( "Modified named search [");
                        statusOutput.append(searchInstance.getName());
                        statusOutput.append("] (Amp plate or Chip term)\n");
                    } else if( termName.equals("Infinium PDO")) {
                        isInfinium = true;
                        searchValue.setTermName("PDO");
                        searchValue.setIncludeInResults(Boolean.FALSE);
                        searchInstance.setExcludeInitialEntitiesFromResults(true);
                        searchInstance.setCustomTraversalOptionName("infiniumPlates");
                        searchInstance.getTraversalEvaluatorValues().put("descendantOptionEnabled", Boolean.TRUE);
                        statusOutput.append( "Modified named search [");
                        statusOutput.append(searchInstance.getName());
                        statusOutput.append("] (Infinium PDO term)\n");
                    }
                }

                if( isInfinium ) {
                    count++;
                } else {
                    // Existing ancestor/descendant logic
                    if( searchInstance.getTraversalEvaluatorValues().containsValue(Boolean.TRUE
                    && searchInstance.getCustomTraversalOptionName().equals("none"))) {
                        searchInstance.setCustomTraversalOptionName("tubesEtcTraverser");
                        count++;
                        statusOutput.append( "Modified named search [");
                        statusOutput.append(searchInstance.getName());
                        statusOutput.append("] (converted traversal to custom tube traverser)\n");
                    }
                }
            }

            if( statusOutput.length() > 1 ) {
                statusOutput.insert(0, " had modifications: \n");
                statusOutput.insert(0, preference.getPreferenceType().toString());
                System.out.println(statusOutput.toString());
            }

            if( count > 0 ) {
                preference.setData(searchInstanceList.marshal());
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return count;
    }
}