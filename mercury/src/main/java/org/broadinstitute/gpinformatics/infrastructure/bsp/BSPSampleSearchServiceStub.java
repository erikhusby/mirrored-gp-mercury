package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Stub;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stub for BSP's sample search service.
 * <p/>
 * This class is @ApplicationScoped because it should only be used in integration tests (so threading shouldn't be an
 * issue) and because it more closely emulates the concept of an endpoint to a single remote system.
 */
@Stub
@Alternative
@Priority(0)
@ApplicationScoped
public class BSPSampleSearchServiceStub implements BSPSampleSearchService {


    public static final String SOMATIC_MAT_TYPE = "DNA:DNA Somatic";
    public static final String GENOMIC_MAT_TYPE = "DNA:DNA Genomic";
    public static final String ROOT = "ROOT";

    public static final String SM_1P3XN = "SM-1P3XN";
    public static final String LSID_PREFIX = "broadinstitute.org:bsp.prod.sample:";
    public static final String SM_1P3XN_LSID = LSID_PREFIX + "1P3XN";
    public static final String SM_1P3XN_SPECIES = "Chicken";
    public static final String SM_1P3XN_CONC = "0.293";
    public static final String SM_1P3XN_VOLUME = "1.3";
    public static final String SM_1P3XN_COLL = "Hungarian Goulash";
    public static final String SM_1P3XN_COLLAB_SAMP_ID = "CollaboratorSampleX";
    public static final String SM_1P3XN_DISEASE = "Malignant peripheral nerve sheath tumor";
    public static final String SM_1P3XN_DNA = "1.7717738037109374";
    public static final String SM_1P3XN_MAT_TYPE = "DNA:DNA Genomic";
    public static final String SM_1P3XN_STOCK_SAMP = "SM-1P3XN";
    public static final String SM_1P3XN_ROOT_SAMP = "ROOT";
    public static final String SM_1P3XN_COLLAB_PID = "CHTN_CA1";
    public static final String SM_1P3XN_PATIENT_ID = "PT-2LK3";


    public static final String SM_1P3WY_DISEASE = "Carcinoid Tumor";
    public static final String SM_1P3WY_DNA = "1.7717738037109374";
    public static final String SM_1P3WY_MAT_TYPE = "DNA:DNA Genomic";
    public static final String SM_1P3WY_COLLAB_PID = "CHTN_CA1";
    public static final String SM_1P3WY = "SM-1P3WY";
    public static final String SM_1P3WY_LSID = LSID_PREFIX + "1P3WY";
    public static final String SM_1P3WY_SPECIES = "Chicken";
    public static final String SM_1P3WY_CONC = "0.293";
    public static final String SM_1P3WY_VOLUME = "1.3";
    public static final String SM_1P3WY_COLL = "Hungarian Goulash";
    public static final String SM_1P3WY_COLLAB_SAMP_ID = "CollaboratorSampleX";
    public static final String SM_1P3WY_STOCK_SAMP = "SM-1P3WY";
    public static final String SM_1P3WY_ROOT_SAMP = "ROOT";
    public static final String SM_1P3WY_PATIENT_ID = "PT-2LK3";

    public static final String SM_12CO4 = "SM-12CO4";
    public static final String SM_12CO4_PATIENT_ID = "PT-2LK3";
    public static final String SM_12CO4_ROOT_SAMP = "";
    public static final String SM_12CO4_STOCK_SAMP = "SM-12CO4";
    public static final String SM_12CO4_COLLAB_SAMP_ID = "CollaboratorSampleX";
    public static final String SM_12CO4_COLL = "Hungarian Goulash";
    public static final String SM_12CO4_VOLUME = "1.3";
    public static final String SM_12CO4_CONC = "0.293";
    public static final String SM_12CO4_SPECIES = "Chicken";
    public static final String SM_12CO4_LSID = LSID_PREFIX + "12CO4";
    public static final String SM_12CO4_COLLAB_PID = "CHTN_CA1";
    public static final String SM_12CO4_MAT_TYPE = "DNA:DNA Somatic";
    public static final String SM_12CO4_DNA = "3.765242738037109374";
    public static final String SM_12CO4_DISEASE = "Carcinoid Tumor";
    public static final String SM_12CO4_COLLABORATOR = "Home Simpson";


    public static final String SM_12FO4 = "SM-12FO4";
    public static final String SM_12FO4_PATIENT_ID = "PT-3PS6";
    public static final String SM_12FO4_ROOT_SAMP = "ROOT";
    public static final String SM_12FO4_STOCK_SAMP = "SM-12FO4";
    public static final String SM_12FO4_COLLAB_SAMP_ID = "CollaboratorSampleX";
    public static final String SM_12FO4_COLL = "Hungarian Goulash";
    public static final String SM_12FO4_VOLUME = "1.3";
    public static final String SM_12FO4_CONC = "0.293";
    public static final String SM_12FO4_SPECIES = "Human";
    public static final String SM_12FO4_LSID = LSID_PREFIX + "12FO4";
    public static final String SM_12FO4_COLLAB_PID = "CHTN_SEW";
    public static final String SM_12FO4_MAT_TYPE = "DNA:DNA Somatic";
    public static final String SM_12FO4_DNA = "3.765242738037109374";
    public static final String SM_12FO4_DISEASE = "Carcinoid Tumor";


    public static final String SM_12DW4 = "SM-12DW4";
    public static final String SM_12DW4_PATIENT_ID = "PT-1TS1";
    public static final String SM_12DW4_ROOT_SAMP = "ROOT";
    public static final String SM_12DW4_STOCK_SAMP = "SM-12DW4";
    public static final String SM_12DW4_COLLAB_SAMP_ID = "CollaboratorSampleX";
    public static final String SM_12DW4_COLL = "Hungarian Goulash";
    public static final String SM_12DW4_VOLUME = "1.3";
    public static final String SM_12DW4_CONC = "0.293";
    public static final String SM_12DW4_SPECIES = "Canine";
    public static final String SM_12DW4_LSID = LSID_PREFIX + "12DW4";
    public static final String SM_12DW4_COLLAB_PID = "CHTN_SEW";
    public static final String SM_12DW4_MAT_TYPE = "DNA:DNA Somatic";
    public static final String SM_12DW4_DNA = "3.765242738037109374";
    public static final String SM_12DW4_DISEASE = "Carcinoid Tumor";


    public static final String SM_12MD2 = "SM-12MD2";
    public static final String SM_12MD2_PATIENT_ID = "PT-5PT9";
    public static final String SM_12MD2_ROOT_SAMP = "ROOT";
    public static final String SM_12MD2_STOCK_SAMP = "SM-12MD2";
    public static final String SM_12MD2_COLLAB_SAMP_ID = "CollaboratorSampleX";
    public static final String SM_12MD2_COLL = "Hungarian Goulash";
    public static final String SM_12MD2_VOLUME = "1.3";
    public static final String SM_12MD2_CONC = "0.293";
    public static final String SM_12MD2_SPECIES = "Canine";
    public static final String SM_12MD2_LSID = LSID_PREFIX + "12MD2";
    public static final String SM_12MD2_COLLAB_PID = "CHTN_SEW";
    public static final String SM_12MD2_MAT_TYPE = "DNA:DNA Somatic";
    public static final String SM_12MD2_DNA = "3.765242738037109374";
    public static final String SM_12MD2_DISEASE = "Carcinoid Tumor";


    public static final String SM_12CO4_CONTAINER_ID = "CO-2859994";
    public static final String SM_1P3WY_CONTAINER_ID = "CO-2859994";
    public static final String SM_1P3XN_CONTAINER_ID = "CO-2859994";
    public static final String SM_12FO4_CONTAINER_ID = "CO-2859994";
    public static final String SM_12DW4_CONTAINER_ID = "CO-2859994";
    public static final String SM_12MD2_CONTAINER_ID = "CO-2859994";

    public static final String CANINE_SPECIES = "Canine";

    /**
     * Samples that this service stub knows about and can return. This collection is NOT static to avoid the possibility
     * of tests running in parallel side-effecting each other.
     */
    private final Map<String, Map<BSPSampleSearchColumn, String>> samples = new HashMap<>();

    public BSPSampleSearchServiceStub() {
        addToMap(SM_12CO4, new EnumMap<BSPSampleSearchColumn, String>(BSPSampleSearchColumn.class) {{
            put(BSPSampleSearchColumn.PARTICIPANT_ID, SM_12CO4_PATIENT_ID);
            put(BSPSampleSearchColumn.ROOT_SAMPLE, SM_12CO4_ROOT_SAMP);
            put(BSPSampleSearchColumn.STOCK_SAMPLE, SM_12CO4_STOCK_SAMP);
            put(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID, SM_12CO4_COLLAB_SAMP_ID);
            put(BSPSampleSearchColumn.COLLECTION, SM_12CO4_COLL);
            put(BSPSampleSearchColumn.VOLUME, SM_12CO4_VOLUME);
            put(BSPSampleSearchColumn.CONCENTRATION, SM_12CO4_CONC);
            put(BSPSampleSearchColumn.SPECIES, SM_12CO4_SPECIES);
            put(BSPSampleSearchColumn.LSID, SM_12CO4_LSID);
            put(BSPSampleSearchColumn.COLLABORATOR_PARTICIPANT_ID, SM_12CO4_COLLAB_PID);
            put(BSPSampleSearchColumn.MATERIAL_TYPE, SM_12CO4_MAT_TYPE);
            put(BSPSampleSearchColumn.TOTAL_DNA, SM_12CO4_DNA);
            put(BSPSampleSearchColumn.SAMPLE_TYPE, BspSampleData.TUMOR_IND);
            put(BSPSampleSearchColumn.PRIMARY_DISEASE, SM_12CO4_DISEASE);
            put(BSPSampleSearchColumn.GENDER, BspSampleData.FEMALE_IND);
            put(BSPSampleSearchColumn.STOCK_TYPE, "");
            put(BSPSampleSearchColumn.CONTAINER_ID, SM_12CO4_CONTAINER_ID);
            put(BSPSampleSearchColumn.SAMPLE_ID, SM_12CO4);
        }});

        addToMap(SM_1P3WY, new EnumMap<BSPSampleSearchColumn, String>(BSPSampleSearchColumn.class) {{
            put(BSPSampleSearchColumn.PARTICIPANT_ID, SM_1P3WY_PATIENT_ID);
            put(BSPSampleSearchColumn.ROOT_SAMPLE, SM_1P3WY_ROOT_SAMP);
            put(BSPSampleSearchColumn.STOCK_SAMPLE, SM_1P3WY_STOCK_SAMP);
            put(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID, SM_1P3WY_COLLAB_SAMP_ID);
            put(BSPSampleSearchColumn.COLLECTION, SM_1P3WY_COLL);
            put(BSPSampleSearchColumn.VOLUME, SM_1P3WY_VOLUME);
            put(BSPSampleSearchColumn.CONCENTRATION, SM_1P3WY_CONC);
            put(BSPSampleSearchColumn.SPECIES, SM_1P3WY_SPECIES);
            put(BSPSampleSearchColumn.LSID, SM_1P3WY_LSID);
            put(BSPSampleSearchColumn.COLLABORATOR_PARTICIPANT_ID, SM_1P3WY_COLLAB_PID);
            put(BSPSampleSearchColumn.MATERIAL_TYPE, SM_1P3WY_MAT_TYPE);
            put(BSPSampleSearchColumn.TOTAL_DNA, SM_1P3WY_DNA);
            put(BSPSampleSearchColumn.SAMPLE_TYPE, BspSampleData.NORMAL_IND);
            put(BSPSampleSearchColumn.PRIMARY_DISEASE, SM_1P3WY_DISEASE);
            put(BSPSampleSearchColumn.GENDER, BspSampleData.MALE_IND);
            put(BSPSampleSearchColumn.STOCK_TYPE, BspSampleData.ACTIVE_IND);
            put(BSPSampleSearchColumn.CONTAINER_ID, SM_1P3WY_CONTAINER_ID);
            put(BSPSampleSearchColumn.SAMPLE_ID, SM_1P3WY);
        }});

        addToMap(SM_1P3XN, new EnumMap<BSPSampleSearchColumn, String>(BSPSampleSearchColumn.class) {{
            put(BSPSampleSearchColumn.PARTICIPANT_ID, SM_1P3XN_PATIENT_ID);
            put(BSPSampleSearchColumn.ROOT_SAMPLE, SM_1P3XN_ROOT_SAMP);
            put(BSPSampleSearchColumn.STOCK_SAMPLE, SM_1P3XN_STOCK_SAMP);
            put(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID, SM_1P3XN_COLLAB_SAMP_ID);
            put(BSPSampleSearchColumn.COLLECTION, SM_1P3XN_COLL);
            put(BSPSampleSearchColumn.VOLUME, SM_1P3XN_VOLUME);
            put(BSPSampleSearchColumn.CONCENTRATION, SM_1P3XN_CONC);
            put(BSPSampleSearchColumn.SPECIES, SM_1P3XN_SPECIES);
            put(BSPSampleSearchColumn.LSID, SM_1P3XN_LSID);
            put(BSPSampleSearchColumn.COLLABORATOR_PARTICIPANT_ID, SM_1P3XN_COLLAB_PID);
            put(BSPSampleSearchColumn.MATERIAL_TYPE, SM_1P3XN_MAT_TYPE);
            put(BSPSampleSearchColumn.TOTAL_DNA, SM_1P3XN_DNA);
            put(BSPSampleSearchColumn.SAMPLE_TYPE, BspSampleData.NORMAL_IND);
            put(BSPSampleSearchColumn.PRIMARY_DISEASE, SM_1P3XN_DISEASE);
            put(BSPSampleSearchColumn.GENDER, BspSampleData.MALE_IND);
            put(BSPSampleSearchColumn.STOCK_TYPE, BspSampleData.ACTIVE_IND);
            put(BSPSampleSearchColumn.CONTAINER_ID, SM_1P3XN_CONTAINER_ID);
            put(BSPSampleSearchColumn.SAMPLE_ID, SM_1P3XN);
        }});

        addToMap(SM_12FO4, new EnumMap<BSPSampleSearchColumn, String>(BSPSampleSearchColumn.class) {{
            put(BSPSampleSearchColumn.PARTICIPANT_ID, SM_12FO4_PATIENT_ID);
            put(BSPSampleSearchColumn.ROOT_SAMPLE, SM_12FO4_ROOT_SAMP);
            put(BSPSampleSearchColumn.STOCK_SAMPLE, SM_12FO4_STOCK_SAMP);
            put(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID, SM_12FO4_COLLAB_SAMP_ID);
            put(BSPSampleSearchColumn.COLLECTION, SM_12FO4_COLL);
            put(BSPSampleSearchColumn.VOLUME, SM_12FO4_VOLUME);
            put(BSPSampleSearchColumn.CONCENTRATION, SM_12FO4_CONC);
            put(BSPSampleSearchColumn.SPECIES, SM_12FO4_SPECIES);
            put(BSPSampleSearchColumn.LSID, SM_12FO4_LSID);
            put(BSPSampleSearchColumn.COLLABORATOR_PARTICIPANT_ID, SM_12FO4_COLLAB_PID);
            put(BSPSampleSearchColumn.MATERIAL_TYPE, SM_12FO4_MAT_TYPE);
            put(BSPSampleSearchColumn.TOTAL_DNA, SM_12FO4_DNA);
            put(BSPSampleSearchColumn.SAMPLE_TYPE, BspSampleData.NORMAL_IND);
            put(BSPSampleSearchColumn.PRIMARY_DISEASE, SM_12FO4_DISEASE);
            put(BSPSampleSearchColumn.GENDER, BspSampleData.MALE_IND);
            put(BSPSampleSearchColumn.STOCK_TYPE, BspSampleData.ACTIVE_IND);
            put(BSPSampleSearchColumn.CONTAINER_ID, SM_12FO4_CONTAINER_ID);
            put(BSPSampleSearchColumn.SAMPLE_ID, SM_12FO4);
        }});

        addToMap(SM_12DW4, new EnumMap<BSPSampleSearchColumn, String>(BSPSampleSearchColumn.class) {{
            put(BSPSampleSearchColumn.PARTICIPANT_ID, SM_12DW4_PATIENT_ID);
            put(BSPSampleSearchColumn.ROOT_SAMPLE, SM_12DW4_ROOT_SAMP);
            put(BSPSampleSearchColumn.STOCK_SAMPLE, SM_12DW4_STOCK_SAMP);
            put(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID, SM_12DW4_COLLAB_SAMP_ID);
            put(BSPSampleSearchColumn.COLLECTION, SM_12DW4_COLL);
            put(BSPSampleSearchColumn.VOLUME, SM_12DW4_VOLUME);
            put(BSPSampleSearchColumn.CONCENTRATION, SM_12DW4_CONC);
            put(BSPSampleSearchColumn.SPECIES, SM_12DW4_SPECIES);
            put(BSPSampleSearchColumn.LSID, SM_12DW4_LSID);
            put(BSPSampleSearchColumn.COLLABORATOR_PARTICIPANT_ID, SM_12DW4_COLLAB_PID);
            put(BSPSampleSearchColumn.MATERIAL_TYPE, SM_12DW4_MAT_TYPE);
            put(BSPSampleSearchColumn.TOTAL_DNA, SM_12DW4_DNA);
            put(BSPSampleSearchColumn.SAMPLE_TYPE, BspSampleData.NORMAL_IND);
            put(BSPSampleSearchColumn.PRIMARY_DISEASE, SM_12DW4_DISEASE);
            put(BSPSampleSearchColumn.GENDER, BspSampleData.MALE_IND);
            put(BSPSampleSearchColumn.STOCK_TYPE, BspSampleData.ACTIVE_IND);
            put(BSPSampleSearchColumn.CONTAINER_ID, SM_12DW4_CONTAINER_ID);
            put(BSPSampleSearchColumn.SAMPLE_ID, SM_12DW4);
        }});

        addToMap(SM_12MD2, new EnumMap<BSPSampleSearchColumn, String>(BSPSampleSearchColumn.class) {{
            put(BSPSampleSearchColumn.PARTICIPANT_ID, SM_12MD2_PATIENT_ID);
            put(BSPSampleSearchColumn.ROOT_SAMPLE, SM_12MD2_ROOT_SAMP);
            put(BSPSampleSearchColumn.STOCK_SAMPLE, SM_12MD2_STOCK_SAMP);
            put(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID, SM_12MD2_COLLAB_SAMP_ID);
            put(BSPSampleSearchColumn.COLLECTION, SM_12MD2_COLL);
            put(BSPSampleSearchColumn.VOLUME, SM_12MD2_VOLUME);
            put(BSPSampleSearchColumn.CONCENTRATION, SM_12MD2_CONC);
            put(BSPSampleSearchColumn.SPECIES, SM_12MD2_SPECIES);
            put(BSPSampleSearchColumn.LSID, SM_12MD2_LSID);
            put(BSPSampleSearchColumn.COLLABORATOR_PARTICIPANT_ID, SM_12MD2_COLLAB_PID);
            put(BSPSampleSearchColumn.MATERIAL_TYPE, SM_12MD2_MAT_TYPE);
            put(BSPSampleSearchColumn.TOTAL_DNA, SM_12MD2_DNA);
            put(BSPSampleSearchColumn.SAMPLE_TYPE, BspSampleData.NORMAL_IND);
            put(BSPSampleSearchColumn.PRIMARY_DISEASE, SM_12MD2_DISEASE);
            put(BSPSampleSearchColumn.GENDER, BspSampleData.FEMALE_IND);
            put(BSPSampleSearchColumn.STOCK_TYPE, BspSampleData.ACTIVE_IND);
            put(BSPSampleSearchColumn.CONTAINER_ID, SM_12MD2_CONTAINER_ID);
            put(BSPSampleSearchColumn.SAMPLE_ID, SM_12MD2);
        }});

    }

    /**
     * Configures a sample for this stub to be able to return.
     *
     * @param sampleName the sample name
     * @param attributes a map of attributes to return when the sample name is queried
     *
     * @throws RuntimeException if the sampleName is already configured for this stub
     */
    public void addToMap(String sampleName, Map<BSPSampleSearchColumn, String> attributes) {
        if (samples.containsKey(sampleName)) {
            throw new RuntimeException("The mock BSP service already contains " + sampleName);
        }

        samples.put(sampleName, attributes);
    }

    @Override
    public List<Map<BSPSampleSearchColumn, String>> runSampleSearch(Collection<String> sampleIDs,
                                                                    BSPSampleSearchColumn... resultColumns) {
        List<Map<BSPSampleSearchColumn, String>> sampleAttributes = new ArrayList<>();
        for (String sampleID : sampleIDs) {
            if (samples.containsKey(sampleID)) {
                sampleAttributes.add(samples.get(sampleID));
            }
        }

        return sampleAttributes;
    }

    public Map<String, Map<BSPSampleSearchColumn, String>> getSamples() {
        return samples;
    }
}
