package org.broadinstitute.gpinformatics.mercury.entity.sample;

import java.util.Arrays;
import java.util.List;

/**
 * Used to help determine whether a particular Lab Vessel is clinical or not.
 */
public enum ContractClient {
    MAYO("Mayo Clinic"),
    KCO("Klarman Cell Observatory"),
    EXTERNAL_CLINICAL_PHARMA("External Pharma Company under NDA"),
    EXTERNAL_NON_CLINICAL_PHARMA("External Pharma Company under NDA non-Clinical");

    // Used to define which clients are Clinical or not.
    public static final List<ContractClient> CLINICAL_CLIENTS = Arrays.asList(MAYO, EXTERNAL_CLINICAL_PHARMA);

    private final String description;

    ContractClient(String description) {
        this.description = description;
    }

    /**
     * Finds a contract client by either the exact name or the exact description.
     *
     * @param searchText    Text to match
     * @return              Matching ContractClient or null.
     */
    public static ContractClient findByNameOrText(String searchText) {
        for (ContractClient client : values()) {
            if (client.name().equals(searchText) || client.description.equals(searchText)) {
                return client;
            }
        }

        return null;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Whether or not a contract client is a clinical client.
     *
     * @return  True if it is clinical, false otherwise
     */
    public boolean isClinical() {
        return CLINICAL_CLIENTS.contains(this);
    }
}
