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
    public static final String SM_12CO4_CONTAINER_ID = "CO-2859994";
    public static final String SM_1P3WY_CONTAINER_ID = "CO-2859994";
    public static final String SM_1P3XN_CONTAINER_ID = "CO-2859994";
    public static final String SM_12FO4_CONTAINER_ID = "CO-2859994";
    public static final String SM_12DW4_CONTAINER_ID = "CO-2859994";
    public static final String SM_12MD2_CONTAINER_ID = "CO-2859994";


    public static final String ALIQUOT_ID_1 = "SM-ALIQUOT1";
    public static final String ALIQUOT_ID_2 = "SM-ALIQUOT2";
    public static final String STOCK_ID = "SM-STOCK";

    private final Map<String,String[]> samples = new HashMap<String,String[]>();

    public BSPSampleSearchServiceStub() {
        addToMap( SM_12CO4,new String[] {
                SM_12CO4_PATIENT_ID ,  // patient
                SM_12CO4_ROOT_SAMP , // root
                SM_12CO4_STOCK_SAMP , // stock
                SM_12CO4_COLLAB_SAMP_ID , // collaborator sample id
                SM_12CO4_COLL , // collection
                SM_12CO4_VOLUME ,  // volume
                SM_12CO4_CONC , // concentration
                SM_12CO4_SPECIES ,  // species
                SM_12CO4_LSID , // sampleLsid
                SM_12CO4_COLLAB_PID ,//  COLLABORATOR_PARTICIPANT_ID
                SM_12CO4_MAT_TYPE ,//  MATERIAL_TYPE
                SM_12CO4_DNA ,//  TOTAL_DNA
                BSPSampleDTO.TUMOR_IND,  //  SAMPLE_TYPE
                SM_12CO4_DISEASE ,//  PRIMARY_DISEASE
                BSPSampleDTO.FEMALE_IND,//  GENDER
                "",//  STOCK_TYPE
                SM_12CO4_FP,//  FINGERPRINT
                SM_12CO4_CONTAINER_ID, //Container ID,
                SM_12CO4, // Sample ID
                SM_12CO4_COLLABORATOR

        });
        addToMap( SM_1P3WY,new String[] {
                SM_1P3WY_PATIENT_ID ,  // patient
                SM_1P3WY_ROOT_SAMP , // root
                SM_1P3WY_STOCK_SAMP , // stock
                SM_1P3WY_COLLAB_SAMP_ID , // collaborator sample id
                SM_1P3WY_COLL , // collection
                SM_1P3WY_VOLUME ,  // volume
                SM_1P3WY_CONC , // concentration
                SM_1P3WY_SPECIES ,  // species
                SM_1P3WY_LSID , // sampleLsid
                SM_1P3WY_COLLAB_PID ,//  COLLABORATOR_PARTICIPANT_ID
                SM_1P3WY_MAT_TYPE ,//  MATERIAL_TYPE
                SM_1P3WY_DNA ,//  TOTAL_DNA
                BSPSampleDTO.NORMAL_IND,  //  SAMPLE_TYPE
                SM_1P3WY_DISEASE ,//  PRIMARY_DISEASE
                BSPSampleDTO.MALE_IND,//  GENDER
                BSPSampleDTO.ACTIVE_IND,//  STOCK_TYPE
                SM_1P3WY_FP,//  FINGERPRINT
                SM_1P3WY_CONTAINER_ID, //CONTAINER ID
                SM_1P3WY // Sample ID

        });
        addToMap( SM_1P3XN,new String[] {
                SM_1P3XN_PATIENT_ID ,  // patient
                SM_1P3XN_ROOT_SAMP , // root
                SM_1P3XN_STOCK_SAMP , // stock
                SM_1P3XN_COLLAB_SAMP_ID , // collaborator sample id
                SM_1P3XN_COLL , // collection
                SM_1P3XN_VOLUME ,  // volume
                SM_1P3XN_CONC , // concentration
                SM_1P3XN_SPECIES ,  // species
                SM_1P3XN_LSID , // sampleLsid

                SM_1P3XN_COLLAB_PID ,//  COLLABORATOR_PARTICIPANT_ID
                SM_1P3XN_MAT_TYPE ,//  MATERIAL_TYPE
                SM_1P3XN_DNA ,//  TOTAL_DNA
                BSPSampleDTO.NORMAL_IND,  //  SAMPLE_TYPE
                SM_1P3XN_DISEASE ,//  PRIMARY_DISEASE
                BSPSampleDTO.MALE_IND,//  GENDER
                BSPSampleDTO.ACTIVE_IND,//  STOCK_TYPE
                SM_1P3XN_FP,//  FINGERPRINT
                SM_1P3XN_CONTAINER_ID, //CONTAINER ID
                SM_1P3XN // Sample ID
        });
        addToMap( SM_12FO4,new String[] {
                  SM_12FO4_PATIENT_ID ,  // patient
                  SM_12FO4_ROOT_SAMP , // root
                  SM_12FO4_STOCK_SAMP , // stock
                  SM_12FO4_COLLAB_SAMP_ID , // collaborator sample id
                  SM_12FO4_COLL , // collection
                  SM_12FO4_VOLUME ,  // volume
                  SM_12FO4_CONC , // concentration
                  SM_12FO4_SPECIES ,  // species
                  SM_12FO4_LSID , // sampleLsid
                  SM_12FO4_COLLAB_PID ,//  COLLABORATOR_PARTICIPANT_ID
                  SM_12FO4_MAT_TYPE ,//  MATERIAL_TYPE
                  SM_12FO4_DNA ,//  TOTAL_DNA
                  BSPSampleDTO.NORMAL_IND,  //  SAMPLE_TYPE
                  SM_12FO4_DISEASE ,//  PRIMARY_DISEASE
                  BSPSampleDTO.MALE_IND,//  GENDER
                  BSPSampleDTO.ACTIVE_IND,//  STOCK_TYPE
                  SM_12FO4_FP,//  FINGERPRINT
                  SM_12FO4_CONTAINER_ID, //CONTAINER ID
                  SM_12FO4 // Sample ID

        });
        addToMap( SM_12DW4,new String[] {
                  SM_12DW4_PATIENT_ID ,  // patient
                  SM_12DW4_ROOT_SAMP , // root
                  SM_12DW4_STOCK_SAMP , // stock
                  SM_12DW4_COLLAB_SAMP_ID , // collaborator sample id
                  SM_12DW4_COLL , // collection
                  SM_12DW4_VOLUME ,  // volume
                  SM_12DW4_CONC , // concentration
                  SM_12DW4_SPECIES ,  // species
                  SM_12DW4_LSID , // sampleLsid
                  SM_12DW4_COLLAB_PID ,//  COLLABORATOR_PARTICIPANT_ID
                  SM_12DW4_MAT_TYPE ,//  MATERIAL_TYPE
                  SM_12DW4_DNA ,//  TOTAL_DNA
                  BSPSampleDTO.NORMAL_IND,  //  SAMPLE_TYPE
                  SM_12DW4_DISEASE ,//  PRIMARY_DISEASE
                  BSPSampleDTO.MALE_IND,//  GENDER
                  BSPSampleDTO.ACTIVE_IND,//  STOCK_TYPE
                  SM_12DW4_FP,//  FINGERPRINT
                  SM_12DW4_CONTAINER_ID, //CONTAINER ID
                  SM_12DW4 // Sample ID
        });
        addToMap( SM_12MD2,new String[] {
                  SM_12MD2_PATIENT_ID ,  // patient
                   "" , // root
                  SM_12MD2_STOCK_SAMP , // stock
                  SM_12MD2_COLLAB_SAMP_ID , // collaborator sample id
                  SM_12MD2_COLL , // collection
                  SM_12MD2_VOLUME ,  // volume
                  SM_12MD2_CONC , // concentration
                  SM_12MD2_SPECIES ,  // species
                  SM_12MD2_LSID , // sampleLsid
                  SM_12MD2_COLLAB_PID ,//  COLLABORATOR_PARTICIPANT_ID
                  SM_12MD2_MAT_TYPE ,//  MATERIAL_TYPE
                  SM_12MD2_DNA ,//  TOTAL_DNA
                  BSPSampleDTO.NORMAL_IND,  //  SAMPLE_TYPE
                  SM_12MD2_DISEASE ,//  PRIMARY_DISEASE
                  BSPSampleDTO.FEMALE_IND,//  GENDER
                  BSPSampleDTO.ACTIVE_IND,//  STOCK_TYPE
                  SM_12MD2_FP,//  FINGERPRINT
                  SM_12MD2_CONTAINER_ID, //CONTAINER ID
                  SM_12MD2 // Sample ID
        });

        addToMap(ALIQUOT_ID_1, new String[] {
                STOCK_ID
        });

        addToMap(ALIQUOT_ID_2, new String[] {
                STOCK_ID
        });
    }

    private void addToMap(String sampleName, String[] attributes) {
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
}
