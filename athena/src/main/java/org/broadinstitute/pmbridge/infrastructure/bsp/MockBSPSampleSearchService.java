package org.broadinstitute.pmbridge.infrastructure.bsp;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.pmbridge.entity.bsp.BSPCollection;
import org.broadinstitute.pmbridge.entity.bsp.BSPCollectionID;
import org.broadinstitute.pmbridge.entity.person.Person;

import javax.enterprise.inject.Default;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory  based on mockservice in mercury.
 * Date: 6/6/12
 * Time: 10:01 AM
 */
@Default
public class MockBSPSampleSearchService implements BSPSampleSearchService  {

    private static Log logger = LogFactory.getLog(MockBSPSampleSearchService.class);
    private final Map<String,String[]> samples = new HashMap<String,String[]>();

    public MockBSPSampleSearchService() {
        addToMap("SM-12CO4",new String[] {
                "PT-2LK3",  // patient
                "ROOT", // root
                "STOCK", // stock
                "CollaboratorSampleX", // collaborator sample id
                "Hungarian Goulash", // collection
                "1.3",  // volume
                "0.293", // concentration
                "Chicken"  // species
        });
        addToMap("SM-1P3WY",new String[] {
                "PT-2LK3",  // patient
                "ROOT", // root
                "STOCK", // stock
                "CollaboratorSampleX", // collaborator sample id
                "Hungarian Goulash", // collection
                "1.3",  // volume
                "0.293", // concentration
                "Chicken"  // species
        });
        addToMap("SM-1P3XN",new String[] {
                "PT-2LK3",  // patient
                "ROOT", // root
                "STOCK", // stock
                "CollaboratorSampleX", // collaborator sample id
                "Hungarian Goulash", // collection
                "1.3",  // volume
                "0.293", // concentration
                "Chicken"  // species
        });
    }

    @Override
    public List<String[]> runSampleSearch(Collection<String> sampleIDs, BSPSampleSearchColumn... resultColumns) {
        List<String[]> sampleAttributes = new ArrayList<String[]>();
        for (String sampleID : sampleIDs) {
            if (samples.containsKey(sampleID)) {
                sampleAttributes.add(samples.get(sampleID));
            }
        }
        return sampleAttributes;
    }


    @Override
    public Set<BSPCollection> getCohortsByUser(final Person bspUser) {

        if ((bspUser == null ) || (StringUtils.isBlank(bspUser.getUsername()))) {
            throw new IllegalArgumentException("Bsp Username is not valid. Canot retrieve list of cohorts from BSP.");
        }
        // Mocked out
        Set<BSPCollection> cohortSet =  getFakeCollections();
        return cohortSet;
    }


    @Override
    public List<String> runSampleSearchByCohort(final BSPCollection cohort) {

        if ((cohort == null) ) {
            throw new IllegalArgumentException("Cohort param was null. Cannot retrieve list of samples from BSP.");
        }

        List<String> results = new ArrayList<String>();

        // find samples in cohort
        // Faked out here as API is not yet available. Add 4 samples to results.
        List<String> tempIds = new ArrayList<String>();
        tempIds.add("SM-11K1"); tempIds.add("SM-11K2");tempIds.add("SM-11K4");tempIds.add("SM-11K4");

        List<String[]> tempResults = runSampleSearch(tempIds, BSPSampleSearchColumn.SAMPLE_ID );
        for ( String[] sampleResult : tempResults ) {
            results.add(sampleResult[0]);
        }

        return results;
    }


    private void addToMap(String sampleName,String[] attributes) {
        if (samples.containsKey(sampleName)) {
            throw new RuntimeException("The mock BSP service already contains " + sampleName);
        }
        samples.put(sampleName,attributes);
    }

    // Fake up a retrieved collection.
    private Set<BSPCollection> getFakeCollections() {

        HashSet<BSPCollection> fakeCohorts = new HashSet<BSPCollection>();
        fakeCohorts.add( new BSPCollection(new BSPCollectionID("12345"), "AlxCollection1"));
        return fakeCohorts;

    }


}
