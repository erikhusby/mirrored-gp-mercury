package org.broadinstitute.gpinformatics.athena.entity.preference;

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
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.inject.Inject;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;
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

    @Test(enabled = true)
    public void modifyResultColumnNamesGPLIM6178() {
        int replaceCount = 0;
        try {
            userBean.loginOSUser();

            PreferenceType[] preferenceTypes = {
                    PreferenceType.GLOBAL_LAB_EVENT_SEARCH_INSTANCES,
                    PreferenceType.GLOBAL_LAB_METRIC_SEARCH_INSTANCES,
                    PreferenceType.GLOBAL_REAGENT_SEARCH_INSTANCES,
                    PreferenceType.USER_LAB_EVENT_SEARCH_INSTANCES,
                    PreferenceType.USER_LAB_METRIC_SEARCH_INSTANCES,
                    PreferenceType.USER_REAGENT_SEARCH_INSTANCES
            };

            replaceCount += renameSavedResultColumn("LCSET", "Lab Batch", preferenceTypes);

            preferenceDao.persist(new FixupCommentary("GPLIM-6178 Rename " + replaceCount
                    + " Event and Metric Saved Search Column/Term 'LCSET' to 'Batch Name'"));

            preferenceDao.flush();

        } catch ( Exception e ) {
            throw new RuntimeException(e);
        }
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
                        searchInstance.setCustomTraversalOptionConfig("infiniumWells");
                        searchInstance.getTraversalEvaluatorValues().put("descendantOptionEnabled", Boolean.TRUE);
                        isInfinium = true;
                        statusOutput.append( "Modified named search [");
                        statusOutput.append(searchInstance.getName());
                        statusOutput.append("] (DNA plate term)\n");
                    } else if( termName.equals(LabVesselSearchDefinition.MultiRefTerm.INFINIUM_AMP_PLATE.getTermRefName() )
                            || termName.equals(LabVesselSearchDefinition.MultiRefTerm.INFINIUM_CHIP.getTermRefName() ) ) {
                        searchInstance.setExcludeInitialEntitiesFromResults(true);
                        searchInstance.setCustomTraversalOptionConfig("infiniumWells");
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
                        searchInstance.setCustomTraversalOptionConfig("infiniumPlates");
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
                        searchInstance.setCustomTraversalOptionConfig("tubesEtcTraverser");
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

    /**
     * GPLIM-5579 JSON structure of user configurable column params was changed.  Modify any saved searches containing columns:
     * Vessel Drill Downs
     * Event Vessel Barcodes
     * Event Vessel Positions
     * Event Vessel Date
     *
     */
    @Test(enabled=false)
    public void refactorColumnParamsGPLIM5579(){

        int replaceCount = 0;
        try {
            userBean.loginOSUser();

            // Do the global search instance
            List<Preference> preferences = preferenceDao.getPreferences(PreferenceType.GLOBAL_LAB_VESSEL_SEARCH_INSTANCES);
            for (Preference preference : preferences) {
                int nbrFixed = fixGPLIM5579Preference( preference );
                if( nbrFixed > 0 ) {
                    replaceCount += nbrFixed;
                }
            }

            // Do the user search instances
            preferences = preferenceDao.getPreferences(PreferenceType.USER_LAB_VESSEL_SEARCH_INSTANCES);
            for (Preference preference : preferences) {
                int nbrFixed = fixGPLIM5579Preference( preference );
                if( nbrFixed > 0 ) {
                    replaceCount += nbrFixed;
                }
            }

            preferenceDao.persist(new FixupCommentary("GPLIM-5579 UDS result column parameter refactoring, " + replaceCount
                                                      + " saved searches modified"));
            preferenceDao.flush();

        } catch ( Exception e ) {
            throw new RuntimeException(e);
        }
    }

    private int fixGPLIM5579Preference( Preference preference ){
        int count = 0;

        String preferenceData = preference.getData();
        //  Quick bailout test
        if( !preferenceData.contains("paramValues")){
            return 0;
        }

        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();

        try {
            // Unmarshall of existing will fail miserably - manipulate at XML level
            DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(preferenceData)));
            NodeList nodeList = document.getElementsByTagName("predefinedViewColumns");
            for( int i = 0; i < nodeList.getLength(); i++ ) {
                Node node = nodeList.item(i);
                String columnText = node.getTextContent();

                if( columnText.startsWith("{") ) {
                    count++;
                    StringBuilder stringBuilder = new StringBuilder(columnText);

                    int start = stringBuilder.indexOf("\"searchTermName\"");
                    int end = start + 16;
                    stringBuilder.delete(start, end);
                    stringBuilder.insert(start, "\"paramType\":\"SEARCH_TERM\",\"entityName\":\"LabVessel\",\"elementName\"");

                    start = stringBuilder.indexOf("\"userColumnName\"",0);
                    end = stringBuilder.indexOf(",", start );
                    stringBuilder.delete(start, end + 1);

                    node.setTextContent(stringBuilder.toString());
                }
            }

            if( count > 0 ) {
                StringWriter writer = new StringWriter();
                Transformer transformer = TransformerFactory.newInstance().newTransformer();
                transformer.setOutputProperty("omit-xml-declaration", "yes");
                DOMSource source = new DOMSource(document);
                StreamResult result = new StreamResult(writer);
                transformer.transform(source, result);
                preference.setData(writer.getBuffer().toString());
                System.out.println(preference.getPreferenceType().toString() + " had " + count + " modifications");
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return count;
    }
}