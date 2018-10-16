package org.broadinstitute.gpinformatics.mercury.entity.sample;

import java.util.Arrays;
import java.util.List;

public enum ContractClient {
    MAYO("Mayo Clinic"),
    KCO("KCO"),
    EXTERNAL_CLINICAL_PHARMA("External Pharma Company under NDA"),
    EXTERNAL_NON_CLINICAL_PHARMA("External Pharma Company under NDA non-Clinical");

    public static final List<ContractClient> CLINICAL_CLIENTS = Arrays.asList(MAYO, EXTERNAL_CLINICAL_PHARMA);

    private final String description;

    ContractClient(String description) {
        this.description = description;
    }

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

    public boolean isClinical() {
        return CLINICAL_CLIENTS.contains(this);
    }
}
