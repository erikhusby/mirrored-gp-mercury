package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Stub;

import javax.enterprise.inject.Alternative;
import java.util.*;

@Stub
@Alternative
public class BSPSampleSearchServiceStub implements BSPSampleSearchService {

    public static final String SM_1P3XN = "SM-1P3XN";
    public static final String SM_1P3XN_LSID = "broadinstitute.org:bsp.prod.sample:1P3XN";
    public static final String SM_1P3XN_SPECIES = "Chicken";
    public static final String SM_1P3XN_CONC = "0.293";
    public static final String SM_1P3XN_VOLUME = "1.3";
    public static final String SM_1P3XN_COLL = "Hungarian Goulash";
    public static final String SM_1P3XN_COLLAB_SAMP_ID = "CollaboratorSampleX";
    public static final String SM_1P3XN_FP =
            "AACTCCCCGGAAAGCTACAAAACG--AATTAGAGTTAATTCTCCAATTGTCTAG--GGACAGGGGGTTCTAAACCCAA--GTCTCCCGCTAGTTTTGGAGAGAGCCGGAGCCCTTTCCAGAGTTCTCTAGTTGGCTGGAGTTCCAAAACTTTCCAATTCTTTGTCGCCGGTTTTACCCCCGGAGAGCTCCCT";
    public static final String SM_1P3XN_DISEASE = "Malignant peripheral nerve sheath tumor";
    public static final String SM_1P3XN_DNA = "1.7717738037109374";
    public static final String SM_1P3XN_MAT_TYPE = "DNA:DNA Genomic";
    public static final String SM_1P3XN_STOCK_SAMP = "SM-1P3XN";
    public static final String SM_1P3XN_ROOT_SAMP = "ROOT";
    public static final String SM_1P3XN_COLLAB_PID = "CHTN_CA1";
    public static final String SM_1P3XN_PATIENT_ID = "PT-2LK3";


    public static final String SM_1P3WY_FP =
            "AACTCCCCGGAAAGCTACAAAACG--AATTAGAGTTAATTCTCCAATTGTCTAG--GGACAGGGGGTTCTAAACCCAA--GTCTCCCGCTAGTTTTGGAGAGAGCCGGAGCCCTTTCCAGAGTTCTCTAGTTGGCTGGAGTTCCAAAACTTTCCAATTCTTTGTCGCCGGTTTTACCCCCGGAGAGCTCCCT";
    public static final String SM_1P3WY_DISEASE = "Carcinoid Tumor";
    public static final String SM_1P3WY_DNA = "1.7717738037109374";
    public static final String SM_1P3WY_MAT_TYPE = "DNA:DNA Genomic";
    public static final String SM_1P3WY_COLLAB_PID = "CHTN_CA1";
    public static final String SM_1P3WY = "SM-1P3WY";
    public static final String SM_1P3WY_LSID = "broadinstitute.org:bsp.prod.sample:1P3WY";
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
    public static final String SM_12CO4_LSID = "broadinstitute.org:bsp.prod.sample:12CO4";
    public static final String SM_12CO4_COLLAB_PID = "CHTN_CA1";
    public static final String SM_12CO4_MAT_TYPE = "DNA:DNA Somatic";
    public static final String SM_12CO4_DNA = "3.765242738037109374";
    public static final String SM_12CO4_FP =
            "AACTCCCCGGAAAGCTACAAAACG--AATTAGAGTTAATTCTCCAATTGTCTAG--GGACAGGGGGTTCTAAACCCAA--GTCTCCCGCTAGTTTTGGAGAGAGCCGGAGCCCTTTCCAGAGTTCTCTAGTTGGCTGGAGTTCCAAAACTTTCCAATTCTTTGTCGCCGGTTTTACCCCCGGAGAGCTCCCT";
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
    public static final String SM_12FO4_LSID = "broadinstitute.org:bsp.prod.sample:12FO4";
    public static final String SM_12FO4_COLLAB_PID = "CHTN_SEW";
    public static final String SM_12FO4_MAT_TYPE = "DNA:DNA Somatic";
    public static final String SM_12FO4_DNA = "3.765242738037109374";
    public static final String SM_12FO4_FP = "AACTCCCCGGAAAGCTACAAAACG--AATTAGAGTTAATTCTCCAATTGTCTAG--GGACAGGGGGTTCTAAACCCAA--GTCTCCCGCTAGTTTTGGAGAGAGCCGGAGCCCTTTCCAGAGTTCTCTAGTTGGCTGGAGTTCCAAAACTTTCCAATTCTTTGTCGCCGGTTTTACCCCCGGAGAGCTCCCT";
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
    public static final String SM_12DW4_LSID = "broadinstitute.org:bsp.prod.sample:12DW4";
    public static final String SM_12DW4_COLLAB_PID = "CHTN_SEW";
    public static final String SM_12DW4_MAT_TYPE = "DNA:DNA Somatic";
    public static final String SM_12DW4_DNA = "3.765242738037109374";
    public static final String SM_12DW4_FP = "AACTCCCCGGAAAGCTACAAAACG--AATTAGAGTTAATTCTCCAATTGTCTAG--GGACAGGGGGTTCTAAACCCAA--GTCTCCCGCTAGTTTTGGAGAGAGCCGGAGCCCTTTCCAGAGTTCTCTAGTTGGCTGGAGTTCCAAAACTTTCCAATTCTTTGTCGCCGGTTTTACCCCCGGAGAGCTCCCT";
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
    public static final String SM_12MD2_LSID = "broadinstitute.org:bsp.prod.sample:12MD2";
    public static final String SM_12MD2_COLLAB_PID = "CHTN_SEW";
    public static final String SM_12MD2_MAT_TYPE = "DNA:DNA Somatic";
    public static final String SM_12MD2_DNA = "3.765242738037109374";
    public static final String SM_12MD2_FP = "AACTCCCCGGAAAGCTACAAAACG--AATTAGAGTTAATTCTCCAATTGTCTAG--GGACAGGGGGTTCTAAACCCAA--GTCTCCCGCTAGTTTTGGAGAGAGCCGGAGCCCTTTCCAGAGTTCTCTAGTTGGCTGGAGTTCCAAAACTTTCCAATTCTTTGTCGCCGGTTTTACCCCCGGAGAGCTCCCT";
    public static final String SM_12MD2_DISEASE = "Carcinoid Tumor";


    public static final String SM_SGM_Test_Somatic_1 = "SM-SGM_Test_Somatic1";
    public static final String SM_SGM_Test_Somatic_1_PATIENT_ID = "PT-1TS1";
    public static final String SM_SGM_Test_Somatic_1_ROOT_SAMP = "ROOT";
    public static final String SM_SGM_Test_Somatic_1_STOCK_SAMP = "SM-SGM_Test_Somatic2";
    public static final String SM_SGM_Test_Somatic_1_COLLAB_SAMP_ID = "CollaboratorSampleX";
    public static final String SM_SGM_Test_Somatic_1_COLL = "Hungarian Goulash";
    public static final String SM_SGM_Test_Somatic_1_VOLUME = "1.3";
    public static final String SM_SGM_Test_Somatic_1_CONC = "0.293";
    public static final String SM_SGM_Test_Somatic_1_SPECIES = "Canine";
    public static final String SM_SGM_Test_Somatic_1_LSID = "broadinstitute.org:bsp.prod.sample:SGM_Test_Somatic";
    public static final String SM_SGM_Test_Somatic_1_COLLAB_PID = "CHTN_SEW";
    public static final String SM_SGM_Test_Somatic_1_MAT_TYPE = "DNA:DNA Somatic";
    public static final String SM_SGM_Test_Somatic_1_DNA = "3.765242738037109374";
    public static final String SM_SGM_Test_Somatic_1_FP = "AACTCCCCGGAAAGCTACAAAACG--AATTAGAGTTAATTCTCCAATTGTCTAG--GGACAGGGGGTTCTAAACCCAA--GTCTCCCGCTAGTTTTGGAGAGAGCCGGAGCCCTTTCCAGAGTTCTCTAGTTGGCTGGAGTTCCAAAACTTTCCAATTCTTTGTCGCCGGTTTTACCCCCGGAGAGCTCCCT";
    public static final String SM_SGM_Test_Somatic_1_DISEASE = "Carcinoid Tumor";

    public static final String SM_SGM_Test_Somatic_2 = "SM-SGM_Test_Somatic1";
    public static final String SM_SGM_Test_Somatic_2_PATIENT_ID = "PT-1TS1";
    public static final String SM_SGM_Test_Somatic_2_ROOT_SAMP = "ROOT";
    public static final String SM_SGM_Test_Somatic_2_STOCK_SAMP = "SM-SGM_Test_Somatic2";
    public static final String SM_SGM_Test_Somatic_2_COLLAB_SAMP_ID = "CollaboratorSampleX";
    public static final String SM_SGM_Test_Somatic_2_COLL = "Hungarian Goulash";
    public static final String SM_SGM_Test_Somatic_2_VOLUME = "1.3";
    public static final String SM_SGM_Test_Somatic_2_CONC = "0.293";
    public static final String SM_SGM_Test_Somatic_2_SPECIES = "Canine";
    public static final String SM_SGM_Test_Somatic_2_LSID = "broadinstitute.org:bsp.prod.sample:SGM_Test_Somatic2";
    public static final String SM_SGM_Test_Somatic_2_COLLAB_PID = "CHTN_SEW";
    public static final String SM_SGM_Test_Somatic_2_MAT_TYPE = "DNA:DNA Somatic";
    public static final String SM_SGM_Test_Somatic_2_DNA = "3.765242738037109374";
    public static final String SM_SGM_Test_Somatic_2_FP = "AACTCCCCGGAAAGCTACAAAACG--AATTAGAGTTAATTCTCCAATTGTCTAG--GGACAGGGGGTTCTAAACCCAA--GTCTCCCGCTAGTTTTGGAGAGAGCCGGAGCCCTTTCCAGAGTTCTCTAGTTGGCTGGAGTTCCAAAACTTTCCAATTCTTTGTCGCCGGTTTTACCCCCGGAGAGCTCCCT";
    public static final String SM_SGM_Test_Somatic_2_DISEASE = "Carcinoid Tumor";

    public static final String SM_SGM_Test_Genomic_1 = "SM-SGM_Test_Genomic1";
    public static final String SM_SGM_Test_Genomic_1_PATIENT_ID = "PT-1TS1";
    public static final String SM_SGM_Test_Genomic_1_ROOT_SAMP = "ROOT";
    public static final String SM_SGM_Test_Genomic_1_STOCK_SAMP = "SM-SGM_Test_Genomic2";
    public static final String SM_SGM_Test_Genomic_1_COLLAB_SAMP_ID = "CollaboratorSampleX";
    public static final String SM_SGM_Test_Genomic_1_COLL = "Hungarian Goulash";
    public static final String SM_SGM_Test_Genomic_1_VOLUME = "1.3";
    public static final String SM_SGM_Test_Genomic_1_CONC = "0.293";
    public static final String SM_SGM_Test_Genomic_1_SPECIES = "Canine";
    public static final String SM_SGM_Test_Genomic_1_LSID = "broadinstitute.org:bsp.prod.sample:SGM_Test_Genomic";
    public static final String SM_SGM_Test_Genomic_1_COLLAB_PID = "CHTN_SEW";
    public static final String SM_SGM_Test_Genomic_1_MAT_TYPE = "DNA:DNA Genomic";
    public static final String SM_SGM_Test_Genomic_1_DNA = "3.765242738037109374";
    public static final String SM_SGM_Test_Genomic_1_FP = "AACTCCCCGGAAAGCTACAAAACG--AATTAGAGTTAATTCTCCAATTGTCTAG--GGACAGGGGGTTCTAAACCCAA--GTCTCCCGCTAGTTTTGGAGAGAGCCGGAGCCCTTTCCAGAGTTCTCTAGTTGGCTGGAGTTCCAAAACTTTCCAATTCTTTGTCGCCGGTTTTACCCCCGGAGAGCTCCCT";
    public static final String SM_SGM_Test_Genomic_1_DISEASE = "Carcinoid Tumor";

    public static final String SM_SGM_Test_Genomic_2 = "SM-SGM_Test_Genomic1";
    public static final String SM_SGM_Test_Genomic_2_PATIENT_ID = "PT-1TS1";
    public static final String SM_SGM_Test_Genomic_2_ROOT_SAMP = "ROOT";
    public static final String SM_SGM_Test_Genomic_2_STOCK_SAMP = "SM-SGM_Test_Genomic2";
    public static final String SM_SGM_Test_Genomic_2_COLLAB_SAMP_ID = "CollaboratorSampleX";
    public static final String SM_SGM_Test_Genomic_2_COLL = "Hungarian Goulash";
    public static final String SM_SGM_Test_Genomic_2_VOLUME = "1.3";
    public static final String SM_SGM_Test_Genomic_2_CONC = "0.293";
    public static final String SM_SGM_Test_Genomic_2_SPECIES = "Canine";
    public static final String SM_SGM_Test_Genomic_2_LSID = "broadinstitute.org:bsp.prod.sample:SGM_Test_Genomic2";
    public static final String SM_SGM_Test_Genomic_2_COLLAB_PID = "CHTN_SEW";
    public static final String SM_SGM_Test_Genomic_2_MAT_TYPE = "DNA:DNA Genomic";
    public static final String SM_SGM_Test_Genomic_2_DNA = "3.765242738037109374";
    public static final String SM_SGM_Test_Genomic_2_FP = "AACTCCCCGGAAAGCTACAAAACG--AATTAGAGTTAATTCTCCAATTGTCTAG--GGACAGGGGGTTCTAAACCCAA--GTCTCCCGCTAGTTTTGGAGAGAGCCGGAGCCCTTTCCAGAGTTCTCTAGTTGGCTGGAGTTCCAAAACTTTCCAATTCTTTGTCGCCGGTTTTACCCCCGGAGAGCTCCCT";
    public static final String SM_SGM_Test_Genomic_2_DISEASE = "Carcinoid Tumor";


    public static final String SM_12CO4_CONTAINER_ID = "CO-2859994";
    public static final String SM_1P3WY_CONTAINER_ID = "CO-2859994";
    public static final String SM_1P3XN_CONTAINER_ID = "CO-2859994";
    public static final String SM_12FO4_CONTAINER_ID = "CO-2859994";
    public static final String SM_12DW4_CONTAINER_ID = "CO-2859994";
    public static final String SM_12MD2_CONTAINER_ID = "CO-2859994";
    public static final String SM_SGM_Test_Genomic_1_CONTAINER_ID = "CO-2859994";
    public static final String SM_SGM_Test_Genomic_2_CONTAINER_ID = "CO-2859994";
    public static final String SM_SGM_Test_Somatic_1_CONTAINER_ID = "CO-2859994";
    public static final String SM_SGM_Test_Somatic_2_CONTAINER_ID = "CO-2859994";

    private final Map<String, Map<BSPSampleSearchColumn, String>> samples = new HashMap<String, Map<BSPSampleSearchColumn, String>>();

    public BSPSampleSearchServiceStub() {
        addToMap( SM_12CO4, new HashMap<BSPSampleSearchColumn, String>() {{
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
            put(BSPSampleSearchColumn.SAMPLE_TYPE, BSPSampleDTO.TUMOR_IND);
            put(BSPSampleSearchColumn.PRIMARY_DISEASE, SM_12CO4_DISEASE);
            put(BSPSampleSearchColumn.GENDER, BSPSampleDTO.FEMALE_IND);
            put(BSPSampleSearchColumn.STOCK_TYPE, "");
            put(BSPSampleSearchColumn.FINGERPRINT, SM_12CO4_FP);
            put(BSPSampleSearchColumn.CONTAINER_ID, SM_12CO4_CONTAINER_ID);
            put(BSPSampleSearchColumn.SAMPLE_ID, SM_12CO4);
        }} );

        addToMap( SM_1P3WY, new HashMap<BSPSampleSearchColumn, String>() {{
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
            put(BSPSampleSearchColumn.SAMPLE_TYPE, BSPSampleDTO.NORMAL_IND);
            put(BSPSampleSearchColumn.PRIMARY_DISEASE, SM_1P3WY_DISEASE);
            put(BSPSampleSearchColumn.GENDER, BSPSampleDTO.MALE_IND);
            put(BSPSampleSearchColumn.STOCK_TYPE, BSPSampleDTO.ACTIVE_IND);
            put(BSPSampleSearchColumn.FINGERPRINT, SM_1P3WY_FP);
            put(BSPSampleSearchColumn.CONTAINER_ID, SM_1P3WY_CONTAINER_ID);
            put(BSPSampleSearchColumn.SAMPLE_ID, SM_1P3WY);
        }} );

        addToMap( SM_1P3XN, new HashMap<BSPSampleSearchColumn, String>() {{
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
            put(BSPSampleSearchColumn.SAMPLE_TYPE, BSPSampleDTO.NORMAL_IND);
            put(BSPSampleSearchColumn.PRIMARY_DISEASE, SM_1P3XN_DISEASE);
            put(BSPSampleSearchColumn.GENDER, BSPSampleDTO.MALE_IND);
            put(BSPSampleSearchColumn.STOCK_TYPE, BSPSampleDTO.ACTIVE_IND);
            put(BSPSampleSearchColumn.FINGERPRINT, SM_1P3XN_FP);
            put(BSPSampleSearchColumn.CONTAINER_ID, SM_1P3XN_CONTAINER_ID);
            put(BSPSampleSearchColumn.SAMPLE_ID, SM_1P3XN);
        }} );

        addToMap( SM_12FO4, new HashMap<BSPSampleSearchColumn, String>() {{
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
            put(BSPSampleSearchColumn.SAMPLE_TYPE, BSPSampleDTO.NORMAL_IND);
            put(BSPSampleSearchColumn.PRIMARY_DISEASE, SM_12FO4_DISEASE);
            put(BSPSampleSearchColumn.GENDER, BSPSampleDTO.MALE_IND);
            put(BSPSampleSearchColumn.STOCK_TYPE, BSPSampleDTO.ACTIVE_IND);
            put(BSPSampleSearchColumn.FINGERPRINT, SM_12FO4_FP);
            put(BSPSampleSearchColumn.CONTAINER_ID, SM_12FO4_CONTAINER_ID);
            put(BSPSampleSearchColumn.SAMPLE_ID, SM_12FO4);
        }} );

        addToMap( SM_12DW4, new HashMap<BSPSampleSearchColumn, String>() {{
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
            put(BSPSampleSearchColumn.SAMPLE_TYPE, BSPSampleDTO.NORMAL_IND);
            put(BSPSampleSearchColumn.PRIMARY_DISEASE, SM_12DW4_DISEASE);
            put(BSPSampleSearchColumn.GENDER, BSPSampleDTO.MALE_IND);
            put(BSPSampleSearchColumn.STOCK_TYPE, BSPSampleDTO.ACTIVE_IND);
            put(BSPSampleSearchColumn.FINGERPRINT, SM_12DW4_FP);
            put(BSPSampleSearchColumn.CONTAINER_ID, SM_12DW4_CONTAINER_ID);
            put(BSPSampleSearchColumn.SAMPLE_ID, SM_12DW4);
        }} );

        addToMap( SM_12MD2, new HashMap<BSPSampleSearchColumn, String>() {{
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
            put(BSPSampleSearchColumn.SAMPLE_TYPE, BSPSampleDTO.NORMAL_IND);
            put(BSPSampleSearchColumn.PRIMARY_DISEASE, SM_12MD2_DISEASE);
            put(BSPSampleSearchColumn.GENDER, BSPSampleDTO.FEMALE_IND);
            put(BSPSampleSearchColumn.STOCK_TYPE, BSPSampleDTO.ACTIVE_IND);
            put(BSPSampleSearchColumn.FINGERPRINT, SM_12MD2_FP);
            put(BSPSampleSearchColumn.CONTAINER_ID, SM_12MD2_CONTAINER_ID);
            put(BSPSampleSearchColumn.SAMPLE_ID, SM_12MD2);
        }} );

        addToMap( SM_SGM_Test_Genomic_2, new HashMap<BSPSampleSearchColumn, String>() {{
            put(BSPSampleSearchColumn.PARTICIPANT_ID, SM_SGM_Test_Genomic_2_PATIENT_ID);
            put(BSPSampleSearchColumn.ROOT_SAMPLE, SM_SGM_Test_Genomic_2_ROOT_SAMP);
            put(BSPSampleSearchColumn.STOCK_SAMPLE, SM_SGM_Test_Genomic_2_STOCK_SAMP);
            put(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID, SM_SGM_Test_Genomic_2_COLLAB_SAMP_ID);
            put(BSPSampleSearchColumn.COLLECTION, SM_SGM_Test_Genomic_2_COLL);
            put(BSPSampleSearchColumn.VOLUME, SM_SGM_Test_Genomic_2_VOLUME);
            put(BSPSampleSearchColumn.CONCENTRATION, SM_SGM_Test_Genomic_2_CONC);
            put(BSPSampleSearchColumn.SPECIES, SM_SGM_Test_Genomic_2_SPECIES);
            put(BSPSampleSearchColumn.LSID, SM_SGM_Test_Genomic_2_LSID);
            put(BSPSampleSearchColumn.COLLABORATOR_PARTICIPANT_ID, SM_SGM_Test_Genomic_2_COLLAB_PID);
            put(BSPSampleSearchColumn.MATERIAL_TYPE, SM_SGM_Test_Genomic_2_MAT_TYPE);
            put(BSPSampleSearchColumn.TOTAL_DNA, SM_SGM_Test_Genomic_2_DNA);
            put(BSPSampleSearchColumn.SAMPLE_TYPE, BSPSampleDTO.NORMAL_IND);
            put(BSPSampleSearchColumn.PRIMARY_DISEASE, SM_SGM_Test_Genomic_2_DISEASE);
            put(BSPSampleSearchColumn.GENDER, BSPSampleDTO.FEMALE_IND);
            put(BSPSampleSearchColumn.STOCK_TYPE, BSPSampleDTO.ACTIVE_IND);
            put(BSPSampleSearchColumn.FINGERPRINT, SM_SGM_Test_Genomic_2_FP);
            put(BSPSampleSearchColumn.CONTAINER_ID, SM_SGM_Test_Genomic_2_CONTAINER_ID);
            put(BSPSampleSearchColumn.SAMPLE_ID, SM_SGM_Test_Genomic_2);
        }} );

        addToMap( SM_SGM_Test_Genomic_1, new HashMap<BSPSampleSearchColumn, String>() {{
            put(BSPSampleSearchColumn.PARTICIPANT_ID, SM_SGM_Test_Genomic_1_PATIENT_ID);
            put(BSPSampleSearchColumn.ROOT_SAMPLE, SM_SGM_Test_Genomic_1_ROOT_SAMP);
            put(BSPSampleSearchColumn.STOCK_SAMPLE, SM_SGM_Test_Genomic_1_STOCK_SAMP);
            put(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID, SM_SGM_Test_Genomic_1_COLLAB_SAMP_ID);
            put(BSPSampleSearchColumn.COLLECTION, SM_SGM_Test_Genomic_1_COLL);
            put(BSPSampleSearchColumn.VOLUME, SM_SGM_Test_Genomic_1_VOLUME);
            put(BSPSampleSearchColumn.CONCENTRATION, SM_SGM_Test_Genomic_1_CONC);
            put(BSPSampleSearchColumn.SPECIES, SM_SGM_Test_Genomic_1_SPECIES);
            put(BSPSampleSearchColumn.LSID, SM_SGM_Test_Genomic_1_LSID);
            put(BSPSampleSearchColumn.COLLABORATOR_PARTICIPANT_ID, SM_SGM_Test_Genomic_1_COLLAB_PID);
            put(BSPSampleSearchColumn.MATERIAL_TYPE, SM_SGM_Test_Genomic_1_MAT_TYPE);
            put(BSPSampleSearchColumn.TOTAL_DNA, SM_SGM_Test_Genomic_1_DNA);
            put(BSPSampleSearchColumn.SAMPLE_TYPE, BSPSampleDTO.NORMAL_IND);
            put(BSPSampleSearchColumn.PRIMARY_DISEASE, SM_SGM_Test_Genomic_1_DISEASE);
            put(BSPSampleSearchColumn.GENDER, BSPSampleDTO.FEMALE_IND);
            put(BSPSampleSearchColumn.STOCK_TYPE, BSPSampleDTO.ACTIVE_IND);
            put(BSPSampleSearchColumn.FINGERPRINT, SM_SGM_Test_Genomic_1_FP);
            put(BSPSampleSearchColumn.CONTAINER_ID, SM_SGM_Test_Genomic_1_CONTAINER_ID);
            put(BSPSampleSearchColumn.SAMPLE_ID, SM_SGM_Test_Genomic_1);
        }} );

        addToMap( SM_SGM_Test_Somatic_2, new HashMap<BSPSampleSearchColumn, String>() {{
            put(BSPSampleSearchColumn.PARTICIPANT_ID, SM_SGM_Test_Somatic_2_PATIENT_ID);
            put(BSPSampleSearchColumn.ROOT_SAMPLE, SM_SGM_Test_Somatic_2_ROOT_SAMP);
            put(BSPSampleSearchColumn.STOCK_SAMPLE, SM_SGM_Test_Somatic_2_STOCK_SAMP);
            put(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID, SM_SGM_Test_Somatic_2_COLLAB_SAMP_ID);
            put(BSPSampleSearchColumn.COLLECTION, SM_SGM_Test_Somatic_2_COLL);
            put(BSPSampleSearchColumn.VOLUME, SM_SGM_Test_Somatic_2_VOLUME);
            put(BSPSampleSearchColumn.CONCENTRATION, SM_SGM_Test_Somatic_2_CONC);
            put(BSPSampleSearchColumn.SPECIES, SM_SGM_Test_Somatic_2_SPECIES);
            put(BSPSampleSearchColumn.LSID, SM_SGM_Test_Somatic_2_LSID);
            put(BSPSampleSearchColumn.COLLABORATOR_PARTICIPANT_ID, SM_SGM_Test_Somatic_2_COLLAB_PID);
            put(BSPSampleSearchColumn.MATERIAL_TYPE, SM_SGM_Test_Somatic_2_MAT_TYPE);
            put(BSPSampleSearchColumn.TOTAL_DNA, SM_SGM_Test_Somatic_2_DNA);
            put(BSPSampleSearchColumn.SAMPLE_TYPE, BSPSampleDTO.NORMAL_IND);
            put(BSPSampleSearchColumn.PRIMARY_DISEASE, SM_SGM_Test_Somatic_2_DISEASE);
            put(BSPSampleSearchColumn.GENDER, BSPSampleDTO.FEMALE_IND);
            put(BSPSampleSearchColumn.STOCK_TYPE, BSPSampleDTO.ACTIVE_IND);
            put(BSPSampleSearchColumn.FINGERPRINT, SM_SGM_Test_Somatic_2_FP);
            put(BSPSampleSearchColumn.CONTAINER_ID, SM_SGM_Test_Somatic_2_CONTAINER_ID);
            put(BSPSampleSearchColumn.SAMPLE_ID, SM_SGM_Test_Somatic_2);
        }} );

        addToMap( SM_SGM_Test_Somatic_1, new HashMap<BSPSampleSearchColumn, String>() {{
            put(BSPSampleSearchColumn.PARTICIPANT_ID, SM_SGM_Test_Somatic_1_PATIENT_ID);
            put(BSPSampleSearchColumn.ROOT_SAMPLE, SM_SGM_Test_Somatic_1_ROOT_SAMP);
            put(BSPSampleSearchColumn.STOCK_SAMPLE, SM_SGM_Test_Somatic_1_STOCK_SAMP);
            put(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID, SM_SGM_Test_Somatic_1_COLLAB_SAMP_ID);
            put(BSPSampleSearchColumn.COLLECTION, SM_SGM_Test_Somatic_1_COLL);
            put(BSPSampleSearchColumn.VOLUME, SM_SGM_Test_Somatic_1_VOLUME);
            put(BSPSampleSearchColumn.CONCENTRATION, SM_SGM_Test_Somatic_1_CONC);
            put(BSPSampleSearchColumn.SPECIES, SM_SGM_Test_Somatic_1_SPECIES);
            put(BSPSampleSearchColumn.LSID, SM_SGM_Test_Somatic_1_LSID);
            put(BSPSampleSearchColumn.COLLABORATOR_PARTICIPANT_ID, SM_SGM_Test_Somatic_1_COLLAB_PID);
            put(BSPSampleSearchColumn.MATERIAL_TYPE, SM_SGM_Test_Somatic_1_MAT_TYPE);
            put(BSPSampleSearchColumn.TOTAL_DNA, SM_SGM_Test_Somatic_1_DNA);
            put(BSPSampleSearchColumn.SAMPLE_TYPE, BSPSampleDTO.NORMAL_IND);
            put(BSPSampleSearchColumn.PRIMARY_DISEASE, SM_SGM_Test_Somatic_1_DISEASE);
            put(BSPSampleSearchColumn.GENDER, BSPSampleDTO.FEMALE_IND);
            put(BSPSampleSearchColumn.STOCK_TYPE, BSPSampleDTO.ACTIVE_IND);
            put(BSPSampleSearchColumn.FINGERPRINT, SM_SGM_Test_Somatic_1_FP);
            put(BSPSampleSearchColumn.CONTAINER_ID, SM_SGM_Test_Somatic_1_CONTAINER_ID);
            put(BSPSampleSearchColumn.SAMPLE_ID, SM_SGM_Test_Somatic_1);
        }} );

    }

    private void addToMap(String sampleName, Map<BSPSampleSearchColumn, String> attributes) {
        if (samples.containsKey(sampleName)) {
            throw new RuntimeException("The mock BSP service already contains " + sampleName);
        }

        samples.put(sampleName,attributes);
    }

    @Override
    public List<Map<BSPSampleSearchColumn, String>> runSampleSearch(Collection<String> sampleIDs, BSPSampleSearchColumn... resultColumns) {
        List<Map<BSPSampleSearchColumn, String>> sampleAttributes = new ArrayList<Map<BSPSampleSearchColumn, String>>();
        for (String sampleID : sampleIDs) {
            if (samples.containsKey(sampleID)) {
                sampleAttributes.add(samples.get(sampleID));
            }
        }

        return sampleAttributes;
    }
}
