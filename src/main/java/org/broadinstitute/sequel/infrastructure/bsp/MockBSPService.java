package org.broadinstitute.sequel.infrastructure.bsp;

import javax.enterprise.inject.Default;
import java.util.*;

@Default
public class MockBSPService implements BSPSampleSearchService {

    private final Map<String,String[]> samples = new HashMap<String,String[]>();

    public MockBSPService() {
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

    private void addToMap(String sampleName,String[] attributes) {
        if (samples.containsKey(sampleName)) {
            throw new RuntimeException("The mock BSP service already contains " + sampleName);
        }
        samples.put(sampleName,attributes);
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
    public List<String[]> runSampleSearch(Collection<String> sampleIDs, List<BSPSampleSearchColumn> resultColumns) {
        return null;
    }
}
