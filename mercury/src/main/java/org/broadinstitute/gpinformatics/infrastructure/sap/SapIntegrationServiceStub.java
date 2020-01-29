package org.broadinstitute.gpinformatics.infrastructure.sap;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.boundary.billing.BillingCredit;
import org.broadinstitute.gpinformatics.athena.boundary.billing.QuoteImportItem;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Stub;
import org.broadinstitute.sap.entity.OrderCalculatedValues;
import org.broadinstitute.sap.entity.OrderValue;
import org.broadinstitute.sap.entity.material.SAPMaterial;
import org.broadinstitute.sap.entity.quote.SapQuote;
import org.broadinstitute.sap.services.SAPIntegrationException;
import org.broadinstitute.sap.services.SapIntegrationClientImpl;

import javax.enterprise.inject.Alternative;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Stub
@Alternative
public class SapIntegrationServiceStub implements SapIntegrationService {

     public SapIntegrationServiceStub(){}

    public static final String TEST_SAP_NUMBER = "Test000001";
    public static final String TEST_CUSTOMER_NUMBER = "CUST_000002";
    public static final String TEST_DELIVERY_DOCUMENT_ID = "DD_0000003";

    @Override
    public String createOrder(ProductOrder placedOrder) throws SAPIntegrationException {
        return TEST_SAP_NUMBER;
    }

    @Override
    public void updateOrder(ProductOrder placedOrder, boolean serviceOptions) throws SAPIntegrationException {
    }

    @Override
    public String billOrder(QuoteImportItem item, BigDecimal quantityOverride, Date workCompleteDate) throws SAPIntegrationException {
        return TEST_DELIVERY_DOCUMENT_ID;
    }

    @Override
    public void publishProductInSAP(Product product) throws SAPIntegrationException {

    }

    @Override
    public void publishProductInSAP(Product product, boolean extendProductsToOtherPlatforms, PublishType publishType) throws SAPIntegrationException {

    }

    @Override
    public Set<SAPMaterial> findProductsInSap() throws SAPIntegrationException {
        final Set<SAPMaterial> sapMaterials = new HashSet<>();
        List<String> initialTest = Arrays.asList("P-CLA-0004,CLIA Somatic Exome,Exome,Sample,Exome Sequencing Analysis : Somatic Exome,CRSP,,yes",
        "P-RNA-0016,Transcriptome Capture v1,RNA,sample,RNA Sequencing Analysis : Transcriptome Capture,Genomics Platform,,",
        "P-SEQ-0018,HiSeq X up to 300 cycles,Sequence Only,lane,Illumina Sequencing Only : HiSeq X up to 300 cycles,Genomics Platform,,",
        "XTNL-AAS-010100,AAS-010100 Germline VCF,Data Analysis,sample,Analysis and Storage : AAS-010100 Germline VCF,Genomics Special Products,yes,",
        "XTNL-AAS-010101,AAS-010101 Somatic MAF,Data Analysis,individual,Analysis and Storage : AAS-010101 Somatic MAF,Genomics Special Products,yes,",
        "XTNL-EXT-010101_1,EXT-010101 DNA Extraction from Blood,Sample Initiation, Qualification & Cell Culture,sample,Extraction and Sample Handling : EXT-010101 Extraction,Genomics Special Products,yes,",
        "XTNL-EXT-010101_2,EXT-010105 DNA Extraction from Saliva,Sample Initiation, Qualification & Cell Culture,sample,Extraction and Sample Handling : EXT-010105 Kit and Saliva Extraction,Genomics Special Products,yes,",
        "XTNL-EXT-010102_1,EXT-010102 DNA Extraction from Buffy Coat,Sample Initiation, Qualification & Cell Culture,sample,Extraction and Sample Handling : EXT-010102 Extraction,Genomics Special Products,yes,",
        "XTNL-EXT-010102_2,EXT-010102 DNA Extraction from Cell Pellet,Sample Initiation, Qualification & Cell Culture,sample,Extraction and Sample Handling : EXT-010102 Extraction,Genomics Special Products,yes,",
        "XTNL-EXT-010102_3,EXT-010102 DNA Extraction from Frozen Tissue,Sample Initiation, Qualification & Cell Culture,sample,Extraction and Sample Handling : EXT-010102 Extraction,Genomics Special Products,yes,",
        "XTNL-EXT-010102_4,EXT-010102 RNA Extraction from PAXGene Blood,Sample Initiation, Qualification & Cell Culture,sample,Extraction and Sample Handling : EXT-010102 Extraction,Genomics Special Products,yes,",
        "XTNL-EXT-010102_6,EXT-010102 RNA Extraction from Frozen Tissue,Sample Initiation, Qualification & Cell Culture,sample,Extraction and Sample Handling : EXT-010102 Extraction,Genomics Special Products,yes,",
        "XTNL-EXT-010103_1,EXT-010103 DNA Extraction from Blood Spots,Sample Initiation, Qualification & Cell Culture,sample,Extraction and Sample Handling : EXT-010103 Extraction,Genomics Special Products,yes,",
        "XTNL-EXT-010103_2,EXT-010103 DNA Extraction from FFPE,Sample Initiation, Qualification & Cell Culture,sample,Extraction and Sample Handling : EXT-010103 Extraction,Genomics Special Products,yes,",
        "P-ESH-0016,PICO Only,Sample Initiation, Qualification & Cell Culture,Each,Materials : Custom Lab Work,Genomics Platform,,",
        "XTNL-EXT-010102_5,EXT-010102 RNA Extraction from Cell Pellet,Sample Initiation, Qualification & Cell Culture,sample,Extraction and Sample Handling : EXT-010102 Extraction,Genomics Special Products,yes,",
        "P-WG-0054,MKD Genotyping Assay v1,Whole Genome Genotyping,Each,Materials : Custom Lab Work,CRSP,,yes",
        "XTNL-EXT-010103_3,EXT-010103 RNA Extraction from FFPE,Sample Initiation, Qualification & Cell Culture,sample,Extraction and Sample Handling : EXT-010103 Extraction,Genomics Special Products,yes,",
        "XTNL-EXT-010103_4,EXT-010103 DNA and RNA Extraction from Cell Pellet,Sample Initiation, Qualification & Cell Culture,sample,Extraction and Sample Handling : EXT-010103 Extraction,Genomics Special Products,yes,",
        "XTNL-EXT-010103_5,EXT-010103 DNA and RNA Extraction from Frozen Tissue,Sample Initiation, Qualification & Cell Culture,sample,Extraction and Sample Handling : EXT-010103 Extraction,Genomics Special Products,yes,",
        "XTNL-EXT-010104_1,EXT-010104 DNA and RNA Extraction from FFPE,Sample Initiation, Qualification & Cell Culture,sample,Extraction and Sample Handling : EXT-010104 Extraction,Genomics Special Products,yes,",
        "P-WG-0053,Infinium GWAS Supplement Array,Whole Genome Genotyping,sample,Whole Genome Arrays (Processing only) : 12 or 24-Sample Array Processing,Genomics Platform,,",
        "P-WG-0055,Infinium Psych Array Processing v2,Whole Genome Genotyping,sample,Whole Genome Arrays (Processing only) : 12 or 24-Sample Array Processing,Genomics Platform,,",
        "P-WG-0056,Infinium Array Orders,Whole Genome Genotyping,Each,Materials : Custom Lab Work,Genomics Platform,,",
        "P-EX-0018,Standard Germline Exome v5,Exome,sample,Exome Sequencing Analysis : Standard Germline Exome,Genomics Platform,,",
        "XTNL-SAM-010701,SAM-010701 Sample Transfer Into Broad Tubes,Sample Initiation, Qualification & Cell Culture,sample,Extraction and Sample Handling : SAM-010701 Sample Transfer into Broad Tubes,Genomics Special Products,yes,",
        "P-SEQ-0021,NextSeq up to 150 cycles,Sequence Only,Flowcell,Illumina Sequencing Only : NextSeq up to 150 cycles,Genomics Platform,,",
        "P-SEQ-0022,NextSeq up to 300 cycles,Sequence Only,Flowcell,Illumina Sequencing Only : NextSeq up to 300 cycles,Genomics Platform,,",
        "P-ESH-0026,RNAQC Only,Sample Initiation, Qualification & Cell Culture,Each,Materials : Custom Lab Work,Genomics Platform,,",
        "P-ESH-0027,Sample Destruction - Physical AND Electronic Termination,Sample Initiation, Qualification & Cell Culture,sample,Extraction and Sample Handling : Stock Tube Return,Genomics Platform,,",
        "P-SEQ-0012,HiSeq 2500 up to 250 cycles,Sequence Only,lane,Illumina Sequencing Only : HiSeq 2500 up to 250 cycles,Genomics Platform,,",
        "P-GTX-0012,GTEX: Tissue External Shipment,Sample Initiation, Qualification & Cell Culture,sample,Extraction and Sample Handling : GTEX: Tissue External Shipment,Genomics Platform,,",
        "P-SEQ-0010,HiSeq 2500 up to 50 cycles,Sequence Only,lane,Illumina Sequencing Only : HiSeq 2500 up to 50 cycles,Genomics Platform,,",
        "P-SEQ-0011,HiSeq 2500 up to 200 cycles,Sequence Only,lane,Illumina Sequencing Only : HiSeq 2500 up to 200 cycles,Genomics Platform,,",
        "P-ALT-0018,10X Genomics,Alternate Library Prep & Development,Each,Materials : Custom Lab Work,Genomics Platform,,",
        "P-VAL-0011,TSCA + Walk-Up Sequencing,Small Design, Validation & Extension,Each,Materials : Custom Lab Work,Genomics Platform,,",
        "P-VAL-0013,Custom Hybrid Selection_Takeda Myeloid Lymphoid Panel v1,Small Design, Validation & Extension,Each,Materials : Custom Lab Work,Genomics Special Products,yes,",
        "P-ANA-0006,Picard Analysis of External Data - Human Whole Genome (35x < x < 65x coverage),Data Analysis,sample,Analysis Only : Picard Analysis of External Data - Human Whole Genome (35x < x < 65x coverage),Genomics Platform,,",
        "P-SEQ-0023,HiSeq X Ten 2x101 Paired Lane,Sequence Only,Each,Materials : Custom Lab Work,Genomics Platform,,",
        "P-VAL-0015,Custom Hybrid Selection_Takeda Myeloid Lymphoid Panel v1 Germline,Small Design, Validation & Extension,Each,Materials : Custom Lab Work,Genomics Platform,yes,",
        "P-ANA-0009,AdHoc Joint Callset,Data Analysis,sample,Analysis Only : AdHoc Joint Callset,Genomics Platform,,",
        "P-WG-0090,Blood Biopsy Extraction and ULP WGS QC v2,Whole Genome Sequencing,sample,Genome Sequencing Analysis : Blood Biopsy Extraction and ULP WGS QC,Genomics Platform,,",
        "XTNL-AAS-010104,AAS-010104 Summary Report,Data Analysis,each,Analysis and Storage : AAS-010104 Summary Report,Genomics Special Products,yes,",
        "P-VAL-0014,Custom Hybrid Selection_Takeda LeSabre v2,Small Design, Validation & Extension,Each,Materials : Custom Lab Work,Genomics Special Products,yes,",
        "P-ESH-0011,Viable Cell Line Retrieval,Sample Initiation, Qualification & Cell Culture,sample,Extraction and Sample Handling : Viable Cell Line Retrieval,Genomics Platform,,",
        "P-ANA-0001,Picard Analysis of External Data - Human Exome,Data Analysis,sample,Analysis Only : Picard Analysis of External Data - Human Exome,Genomics Platform,,",
        "P-ESH-0009,DNA or RNA External Plating into Tubes,Sample Initiation, Qualification & Cell Culture,sample,Extraction and Sample Handling : DNA or RNA External Plating into Tubes,Genomics Platform,,",
        "P-ANA-0005,Picard Analysis of External Data - Human Whole Genome (<35x coverage),Data Analysis,sample,Analysis Only : Picard Analysis of External Data - Human Whole Genome (<35x coverage),Genomics Platform,,",
        "P-RNA-0017,Fast Track Transcriptome Capture - High Coverage (50M pairs),RNA,Each,Materials : Custom Lab Work,Genomics Platform,,",
        "P-WG-0058,Infinium MEGA Array Processing,Whole Genome Genotyping,sample,Whole Genome Arrays (Processing only) : 8-Sample Array Processing,Genomics Platform,,",
        "P-SEQ-0020,NextSeq up to 75 cycles,Sequence Only,Flowcell,Illumina Sequencing Only : NextSeq up to 75 cycles,Genomics Platform,,",
        "P-ESH-0010,DNA External Plating into Plates,Sample Initiation, Qualification & Cell Culture,sample,Extraction and Sample Handling : DNA External Plating into Plates,Genomics Platform,,",
        "P-SEQ-0001,HiSeq 2x25 Paired Lane,Sequence Only,lane,Illumina Sequencing Only : HiSeq 2x25 Paired Lane,Genomics Platform,,",
        "P-ESH-0002,Sample Qualification Only (Human Samples),Sample Initiation, Qualification & Cell Culture,sample,Extraction and Sample Handling : Sample Qualification Only - Human,Genomics Platform,,",
        "P-SEQ-0017,HiSeq 2500 2x250 Paired Lane,Sequence Only,lane,Illumina Sequencing Only : HiSeq 2500 2x250 Paired Lane,Genomics Platform,,",
        "P-SEQ-0003,HiSeq 2x76 Paired Lane,Sequence Only,lane,Illumina Sequencing Only : HiSeq 2x76 Paired lane,Genomics Platform,,",
        "P-SEQ-0005,MiSeq up to 50 cycles,Sequence Only,lane,Illumina Sequencing Only : MiSeq up to 50 cycles,Genomics Platform,,",
        "P-CLA-0003,CLIA Germline Exome,Exome,Sample,Exome Sequencing Analysis : Germline Exome,CRSP,,yes",
        "P-SEQ-0006,MiSeq up to 300 cycles,Sequence Only,lane,Illumina Sequencing Only : MiSeq up to 300 cycles,Genomics Platform,,",
        "P-SEQ-0004,HiSeq 2x101 Paired Lane,Sequence Only,lane,Illumina Sequencing Only : HiSeq 2x101 Paired lane,Genomics Platform,,",
        "P-ESH-0022,Fragment Integrity QC,Sample Initiation, Qualification & Cell Culture,sample,Extraction and Sample Handling : Fragment Integrity QC,Genomics Platform,,",
        "P-VAL-0004,Custom Reagent Orders,Small Design, Validation & Extension,Each,Materials : Custom Lab Work,Genomics Platform,,",
        "P-SEQ-0014,HiSeq 2500 up to 300 cycles,Sequence Only,lane,Illumina Sequencing Only : HiSeq 2500 up to 300 cycles,Genomics Platform,,",
        "P-SEQ-0007,MiSeq up to 500 cycles,Sequence Only,lane,Illumina Sequencing Only : MiSeq up to 500 cycles,Genomics Platform,,",
        "P-RNA-0006,Fast Track Strand Specific RNA Sequencing - High Coverage (50M pairs),RNA,Each,Materials : Custom Lab Work,Genomics Platform,,",
        "P-ESH-0055,DNA and RNA Extraction from Cell Pellet,Sample Initiation, Qualification & Cell Culture,sample,Extraction and Sample Handling : DNA and RNA Extraction from Cell Pellet,Genomics Platform,,",
        "P-ESH-0056,DNA and RNA Extraction from FFPE,Sample Initiation, Qualification & Cell Culture,sample,Extraction and Sample Handling : DNA and RNA Extraction from FFPE,Genomics Platform,,",
        "P-WG-0077,Infinium EPIC Array Processing - Chip Included,Whole Genome Genotyping,sample,Whole Genome Arrays : EPIC Methylation Array,Genomics Platform,,",
        "P-ESH-0058,DNA and RNA Extraction from Stool with Fresh Dissection,Sample Initiation, Qualification & Cell Culture,sample,Extraction and Sample Handling : DNA and RNA Extraction from Stool with Fresh Dissection,Genomics Platform,,",
        "P-ESH-0070,NCI: Sample Initiation and Data Delivery,Sample Initiation, Qualification & Cell Culture,sample,Extraction and Sample Handling : NCI: Sample Initiation and Data Delivery,Genomics Platform,,",
        "P-WG-0081,PCR+ Human WGS - 60x v1,Whole Genome Sequencing,sample,Genome Sequencing Analysis : PCR+ Human WGS - 60x,Genomics Platform,,",
        "P-WG-0082,PCR+ Human WGS - 15x v1,Whole Genome Sequencing,sample,Genome Sequencing Analysis : PCR+ Human WGS - 15x,Genomics Platform,,",
        "P-ESH-0057,DNA and RNA Extraction from Frozen Tissue,Sample Initiation, Qualification & Cell Culture,sample,Extraction and Sample Handling : DNA and RNA Extraction from Frozen Tissue,Genomics Platform,,",
        "P-ESH-0059,DNA and RNA Extraction from Stool with Frozen Dissection,Sample Initiation, Qualification & Cell Culture,sample,Extraction and Sample Handling : DNA and RNA Extraction from Stool with Frozen Dissection,Genomics Platform,,",
        "P-ESH-0060,DNA and RNA Extraction from Stool without Dissection,Sample Initiation, Qualification & Cell Culture,sample,Extraction and Sample Handling : DNA and RNA Extraction from Stool without Dissection,Genomics Platform,,",
        "P-ESH-0062,DNA Extraction from Buffy Coat,Sample Initiation, Qualification & Cell Culture,sample,Extraction and Sample Handling : DNA Extraction from Buffy Coat,Genomics Platform,,",
        "P-ESH-0064,DNA Extraction from Saliva,Sample Initiation, Qualification & Cell Culture,sample,Extraction and Sample Handling : DNA Extraction from Saliva,Genomics Platform,,",
        "P-ESH-0065,DNA HMW Extraction,Sample Initiation, Qualification & Cell Culture,sample,Extraction and Sample Handling : DNA HMW Extraction,Genomics Platform,,",
        "P-ESH-0066,GTEX: DNA Extraction from Frozen Tissue,Sample Initiation, Qualification & Cell Culture,sample,Extraction and Sample Handling : GTEX: DNA Extraction from Frozen Tissue,Genomics Platform,,",
        "P-ESH-0068,GTEX: RNA Extraction from Frozen Tissue,Sample Initiation, Qualification & Cell Culture,sample,Extraction and Sample Handling : GTEX: RNA Extraction from Frozen Tissue,Genomics Platform,,",
        "P-WG-0083,PCR-Free Human WGS - 15x v1,Whole Genome Sequencing,sample,Genome Sequencing Analysis : PCR-Free Human WGS - 15x,Genomics Platform,,",
        "P-WG-0084,PCR-Free Human WGS - 80x v1,Whole Genome Sequencing,sample,Genome Sequencing Analysis : PCR-Free Human WGS - 80x,Genomics Platform,,",
        "XTNL-ALT-010802,ALT-010802 10X Genomics Caliper QC Only,Alternate Library Prep & Development,Each,Materials : Custom Lab Work,Genomics Special Products,yes,",
        "P-ESH-0069,GTEX: RNA Extraction from PAXGene Blood,Sample Initiation, Qualification & Cell Culture,sample,Extraction and Sample Handling : GTEX: RNA Extraction from PAXGene Blood,Genomics Platform,,",
        "XTNL-RNA-010406,RNA-010406 Transcriptome Capture v1,RNA,sample,RNA Sequencing Analysis : RNA-010406 Transcriptome Capture,Genomics Special Products,yes,",
        "P-ANA-0008,External Processing - Short term Genome gtVCF only,Data Analysis,sample,Analysis Only : External Processing - Short term Genome gtVCF only,Genomics Platform,,",
        "XTNL-WES-010268,WES-010237 Express Somatic Human WES (Standard Coverage) v1.1,Exome,sample,Exome Sequencing Analysis : WES-010237 Express Somatic Human WES (Standard Coverage),Genomics Special Products,yes,",
        "XTNL-WES-010269,WES-010238 Express Somatic Human WES (Standard Coverage) v1.1,Exome,sample,Exome Sequencing Analysis : WES-010238 Express Somatic Human WES (Standard Coverage),Genomics Special Products,yes,",
        "XTNL-WES-010270,WES-010239 Express Somatic Human WES (Standard Coverage) v1.1,Exome,sample,Exome Sequencing Analysis : WES-010239 Express Somatic Human WES (Standard Coverage),Genomics Special Products,yes,",
        "XTNL-WES-010228,WES-010228 Standard Germline Exome,Exome,sample,Exome Sequencing Analysis : WES-010228 Standard Germline Exome,Genomics Special Products,yes,",
        "XTNL-WES-010229,WES-010229 Standard Germline Exome,Exome,sample,Exome Sequencing Analysis : WES-010229 Standard Germline Exome,Genomics Special Products,yes,",
        "XTNL-WES-010230,WES-010230 Standard Germline Exome,Exome,sample,Exome Sequencing Analysis : WES-010230 Standard Germline Exome,Genomics Special Products,yes,",
        "P-CLA-0008,CLIA PCR-Free Whole Genome,Whole Genome Sequencing,Sample,Genome Sequencing Analysis : CLIA PCR-Free Whole Genome,CRSP,,yes",
        "P-EX-0039,Express Somatic Human WES (Deep Coverage) v1.1,Exome,sample,Exome Sequencing Analysis : Express Somatic Human WES (Deep Coverage),Genomics Platform,,",
        "P-EX-0040,Express Somatic Human WES (Standard Coverage) v1.1,Exome,sample,Exome Sequencing Analysis : Express Somatic Human WES (Standard Coverage),Genomics Platform,,",
        "XTNL-WES-010271,WES-010240 Express Somatic Human WES (Deep Coverage) v1.1,Exome,sample,Exome Sequencing Analysis : WES-010240 Express Somatic Human WES (Deep Coverage),Genomics Special Products,yes,",
        "XTNL-WES-010272,WES-010241 Express Somatic Human WES (Deep Coverage) v1.1,Exome,sample,Exome Sequencing Analysis : WES-010241 Express Somatic Human WES (Deep Coverage),Genomics Special Products,yes,",
        "XTNL-WES-010273,WES-010242 Express Somatic Human WES (Deep Coverage) v1.1,Exome,sample,Exome Sequencing Analysis : WES-010242 Express Somatic Human WES (Deep Coverage),Genomics Special Products,yes,",
        "XTNL-WES-010274,WES-010243 Somatic Human WES (Standard Coverage) v1.1,Exome,sample,Exome Sequencing Analysis : WES-010243 Somatic Human WES (Standard Coverage),Genomics Special Products,yes,",
        "XTNL-WES-010275,WES-010244 Somatic Human WES (Standard Coverage) v1.1,Exome,sample,Exome Sequencing Analysis : WES-010244 Somatic Human WES (Standard Coverage),Genomics Special Products,yes,",
        "XTNL-WES-010276,WES-010245 Somatic Human WES (Standard Coverage) v1.1,Exome,sample,Exome Sequencing Analysis : WES-010245 Somatic Human WES (Standard Coverage),Genomics Special Products,yes,",
        "XTNL-WES-010277,WES-010246 Somatic Human WES (Deep Coverage) v1.1,Exome,sample,Exome Sequencing Analysis : WES-010246 Somatic Human WES (Deep Coverage),Genomics Special Products,yes,",
        "XTNL-WES-010278,WES-010247 Somatic Human WES (Deep Coverage) v1.1,Exome,sample,Exome Sequencing Analysis : WES-010247 Somatic Human WES (Deep Coverage),Genomics Special Products,yes,",
        "XTNL-WES-010279,WES-010248 Somatic Human WES (Deep Coverage) v1.1,Exome,sample,Exome Sequencing Analysis : WES-010248 Somatic Human WES (Deep Coverage),Genomics Special Products,yes,",
        "P-EX-0041,Somatic Human WES (Deep Coverage) v1.1,Exome,sample,Exome Sequencing Analysis : Somatic Human WES (Deep Coverage),Genomics Platform,,",
        "P-EX-0042,Somatic Human WES (Standard Coverage) v1.1,Exome,sample,Exome Sequencing Analysis : Somatic Human WES (Standard Coverage),Genomics Platform,,",
        "P-EX-0044,Human WES - Normal (150xMTC) v1.1,Exome,sample,Exome Sequencing Analysis : Somatic Human WES (Standard Coverage),Genomics Platform,,",
        "P-EX-0045,Human WES - Tumor (150xMTC) v1.1,Exome,sample,Exome Sequencing Analysis : Somatic Human WES (Deep Coverage),Genomics Platform,,",
        "XTNL-WGS-010338,WGS-010317 Genome, Unaligned, PCR-Free v1,Whole Genome Sequencing,sample,Genome Sequencing Analysis : WGS-010324 Genome, PCR-Free, 30X,Genomics Special Products,yes,",
        "P-EX-0038,Standard Germline Exome v5 Plus GSA Array,Exome,sample,Exome Sequencing Analysis : Standard Germline Exome Plus GSA Array,Genomics Platform,,",
        "XTNL-WGS-010339,WGS-010318 Genome, PCR-Free, 30X v1,Whole Genome Sequencing,sample,Genome Sequencing Analysis : WGS-010318 Genome, PCR-Free, 30X,Genomics Special Products,yes,",
        "XTNL-WGS-010340,WGS-010319 Genome, PCR-Free, 60X v1,Whole Genome Sequencing,sample,Genome Sequencing Analysis : WGS-010319 Genome, PCR-Free, 60X,Genomics Special Products,yes,",
        "XTNL-WGS-010325,WGS-010303 Genome, PCR-Free, 30X v1,Whole Genome Sequencing,sample,Genome Sequencing Analysis : WGS-010303 Genome, PCR-Free,Genomics Special Products,yes,",
        "XTNL-WGS-010341,WGS-010320 Genome, PCR Plus,  30X v1,Whole Genome Sequencing,sample,Genome Sequencing Analysis : WGS-010320 Genome, PCR Plus,  30X,Genomics Special Products,yes,",
        "XTNL-WGS-010342,WGS-010321 Genome, PCR Plus,  60X v1,Whole Genome Sequencing,sample,Genome Sequencing Analysis : WGS-010321 Genome, PCR Plus,  60X,Genomics Special Products,yes,",
        "XTNL-MCB-01901,MCB-01900 16S Sequencing (Stool),Microbial & Viral Analysis,sample,Genome Sequencing Analysis : MCB-010901 16S Sequencing,Genomics Special Products,yes,",
        "P-RNA-0020,Tru-Seq Non-Strand Specific RNA Sequencing (50M pairs) v1,RNA,sample,RNA Sequencing Analysis : Tru-Seq Non-Strand Specific RNA Sequencing - High Coverage (50M pairs),Genomics Platform,,",
        "P-RNA-0022,Tru-Seq Strand Specific Large Insert RNA Sequencing - Deep Coverage (100M pairs) v1,RNA,sample,RNA Sequencing Analysis : Tru-Seq Strand Specific Large Insert RNA Sequencing - Deep Coverage (100M pairs),Genomics Platform,,",
        "XTNL-RNA-010407,RNA-010407 Stranded, Long Insert Transcriptome (100M) v1,RNA,sample,RNA Sequencing Analysis : RNA-010407 Stranded, Long Insert Transcriptome (100M),Genomics Special Products,yes,",
        "XTNL-RNA-010408,RNA-010408 Stranded, Long Insert Transcriptome (100M) v1,RNA,sample,RNA Sequencing Analysis : RNA-010408 Stranded, Long Insert Transcriptome (100M),Genomics Special Products,yes,",
        "XTNL-RNA-010409,RNA-010409 Stranded, Long Insert Transcriptome (100M) v1,RNA,sample,RNA Sequencing Analysis : RNA-010409 Stranded, Long Insert Transcriptome (100M),Genomics Special Products,yes,",
        "P-WG-0087,PCR-Free Human WGS - 30x v1 (High Volume, >10,000 samples),Whole Genome Sequencing,sample,Genome Sequencing Analysis : PCR-Free Human WGS - 30x (High Volume, >10,000 samples),Genomics Platform,,",
        "P-CLA-0005,CLA-0005 Clinical Germline Whole Exome Sequencing,Exome,Sample,Exome Sequencing Analysis : CLA-0005 Clinical Germline Whole Exome Sequencing,CRSP,yes,yes",
        "P-CLA-0006,CLA-0006 Clinical Somatic Whole Exome Sequencing,Exome,Sample,Exome Sequencing Analysis : CLA-0006 Clinical Somatic Whole Exome Sequencing,CRSP,yes,yes",
        "P-SEQ-0030,NovaSeq S2 up to 200 cycles,Sequence Only,Flowcell,Illumina Sequencing Only : NovaSeq S2 up to 200 cycles,Genomics Platform,,",
        "P-SEQ-0027,HiSeq 2x50 Paired Lane,Sequence Only,lane,Illumina Sequencing Only : HiSeq 2x50 Paired Lane,Genomics Platform,,",
        "P-ESH-0052,Ship and Store,Sample Initiation, Qualification & Cell Culture,,Genome Sequencing Analysis : Ship and Store,Genomics Platform,,",
        "XTNL-MCB-01902,MCB-01900 16S Sequencing (Biopsy),Microbial & Viral Analysis,sample,Genome Sequencing Analysis : MCB-010901 16S Sequencing,Genomics Special Products,yes,",
        "XTNL-GEN-011003,GEN-011001 Infinium Global Screening Array Processing,Whole Genome Genotyping,sample,Whole Genome Arrays (Processing only) : GEN-011001 12 or 24-Sample Array Processing,Genomics Special Products,yes,",
        "XTNL-SAM-010702,SAM-010702 Fragment Integrity QC,Sample Initiation, Qualification & Cell Culture,sample,Extraction and Sample Handling : SAM-010702 Fragment Integrity QC,Genomics Special Products,yes,",
        "P-RNA-0019,Tru-Seq Strand Specific Large Insert RNA Sequencing (50M pairs) v1,RNA,sample,RNA Sequencing Analysis : Tru-Seq Strand Specific Large Insert RNA Sequencing - High Coverage (50M pairs),Genomics Platform,,",
        "P-ESH-0053,Stock Tube Return,Sample Initiation, Qualification & Cell Culture,sample,Extraction and Sample Handling : Stock Tube Return,Genomics Platform,,",
        "P-MCV-0009,Standard 16S Sequencing (Stool),Microbial & Viral Analysis,sample,Assembly and Metagenomic Analysis : 16S Sequencing,Genomics Platform,,",
        "P-MCV-0010,Standard 16S Sequencing (Biopsy),Microbial & Viral Analysis,sample,Assembly and Metagenomic Analysis : 16S Sequencing,Genomics Platform,,",
        "P-WG-0059,Infinium MEGA (High Volume >40,000 sample) Array Processing,Whole Genome Genotyping,sample,Whole Genome Arrays (Processing only) : 8-Sample Array Processing (High Volume, >40,000 samples),Genomics Platform,,",
        "P-WG-0068,Infinium EPIC Array Processing,Whole Genome Genotyping,sample,Whole Genome Arrays (Processing only) : 8-Sample Methylation Array Processing,Genomics Platform,,",
        "XTNL-GEN-011004,GEN-011001 Infinium Psych Array Processing,Whole Genome Genotyping,sample,Whole Genome Arrays (Processing only) : GEN-011001 12 or 24-Sample Array Processing,Genomics Special Products,yes,",
        "XTNL-GEN-011005,GEN-011002 Infinium MEGA Array Processing,Whole Genome Genotyping,sample,Whole Genome Arrays (Processing only) : GEN-011002 8-Sample Array Processing,Genomics Special Products,yes,",
        "P-WG-0072,Infinium Psych Array Processing v3,Whole Genome Genotyping,sample,Whole Genome Arrays (Processing only) : 12 or 24-Sample Array Processing,Genomics Platform,,",
        "P-SEQ-0031,NovaSeq S2 up to 300 cycles,Sequence Only,Flowcell,Illumina Sequencing Only : NovaSeq S2 up to 300 cycles,Genomics Platform,,",
        "XTNL-RNA-010402.2,RNA-010402 Stranded, Long Insert Transcriptome v1,RNA,sample,RNA Sequencing Analysis : RNA-010402 Stranded, Long Insert Transcriptome,Genomics Special Products,yes,",
        "P-ESH-0021,Sample Transfer into Broad Tubes,Sample Initiation, Qualification & Cell Culture,sample,Extraction and Sample Handling : Sample Transfer into Broad Tubes,Genomics Platform,,",
        "P-ESH-0061,DNA Extraction from Blood,Sample Initiation, Qualification & Cell Culture,sample,Extraction and Sample Handling : DNA Extraction from Blood,Genomics Platform,,",
        "P-ESH-0063,DNA Extraction from FFPE,Sample Initiation, Qualification & Cell Culture,sample,Extraction and Sample Handling : DNA Extraction from FFPE,Genomics Platform,,",
        "P-ESH-0067,GTEX: DNA and RNA Extraction from PAXGene Tissue,Sample Initiation, Qualification & Cell Culture,Sample,Extraction and Sample Handling : DNA and RNA Extraction from PAXgene Preserved Tissue,Genomics Platform,,",
        "XTNL-ALT-010801,ALT-010801 10X Genomics,Alternate Library Prep & Development,Each,Materials : Custom Lab Work,Genomics Special Products,yes,",
        "P-CLA-0007,Clinical Extraction,Sample Initiation, Qualification & Cell Culture,Sample,Extraction and Sample Handling : Clinical Extraction,CRSP,,yes",
        "P-ANA-0007,External Processing - Long term Genome CRAM and gtVCF,Data Analysis,sample,Analysis Only : External Processing - Long term Genome CRAM and gtVCF,Genomics Platform,,",
        "P-ALT-0019,10X Genomics Caliper QC Only,Alternate Library Prep & Development,Each,Materials : Custom Lab Work,Genomics Platform,,",
        "XTNL-SAM-010703,SAM-010703 Stock Tube Return,Sample Initiation, Qualification & Cell Culture,sample,Extraction and Sample Handling : SAM-010703 Stock Tube Return,Genomics Special Products,yes,",
        "P-VAL-0017,Custom Hybrid Selection_Takeda LeSabre V3,Small Design, Validation & Extension,Each,Materials : Custom Lab Work,Genomics Special Products,yes,",
        "P-WG-0069,PCR-Free Human WGS - 30x v1,Whole Genome Sequencing,sample,Genome Sequencing Analysis : PCR-Free Human WGS - 30x,Genomics Platform,,",
        "P-SEQ-0029,NovaSeq S2 up to 100 cycles,Sequence Only,Flowcell,Illumina Sequencing Only : NovaSeq S2 up to 100 cycles,Genomics Platform,,",
        "P-WG-0073,Infinium Global Screening Array + Multi Disease Processing,Whole Genome Genotyping,sample,Whole Genome Arrays (Processing only) : 12 or 24-Sample Array Processing,Genomics Platform,,",
        "P-WG-0079,PCR-Free Human WGS - 60x v1,Whole Genome Sequencing,sample,Genome Sequencing Analysis : PCR-Free Human WGS - 60x,Genomics Platform,,",
        "P-WG-0080,PCR+ Human WGS - 30x v1,Whole Genome Sequencing,sample,Genome Sequencing Analysis : PCR+ Human WGS - 30x,Genomics Platform,,",
        "XTNL-RNA-010401.2,RNA-010401 Stranded, Long Insert Transcriptome v1,RNA,sample,RNA Sequencing Analysis : RNA-010401 Stranded, Long Insert Transcriptome,Genomics Special Products,yes,",
        "XTNL-RNA-010400.2,RNA-010400 Stranded, Long Insert Transcriptome v1,RNA,sample,RNA Sequencing Analysis : RNA-010400 Stranded, Long Insert Transcriptome,Genomics Special Products,yes,",
        "P-WG-0071,PCR-Free Human WGS - 20x v1,Whole Genome Sequencing,sample,Genome Sequencing Analysis : PCR-Free Human WGS - 20x,Genomics Platform,,",
        "XTNL-WGS-010347,WGS-010347 Genome, PCR-Free, 15X v1,Whole Genome Sequencing,sample,Genome Sequencing Analysis : WGS-010347 Genome, PCR-Free, 15X,Genomics Special Products,yes,",
        "XTNL-WES-010290,WES-010290 Standard Coverage Exome for Liquid Biopsy Normal v2,Exome,sample,Exome Sequencing Analysis : WES-010243 Somatic Human WES (Standard Coverage),Genomics Special Products,yes,",
        "P-WG-0096,ULP WGS QC for non-cfDNA v1,Whole Genome Sequencing,sample,Genome Sequencing Analysis : Blood Biopsy Extraction and ULP WGS QC,Genomics Platform,,",
        "P-MCV-0012,Microbial WGS Mid-output (1.5GB),Microbial & Viral Analysis,Each,Materials : Custom Lab Work,Genomics Platform,,",
        "P-MCV-0014,Microbial WGS High-output (3GB),Microbial & Viral Analysis,Each,Materials : Custom Lab Work,Genomics Platform,,",
        "P-RNA-0024,Tru-Seq Strand Specific Large Insert RNA Sequencing (75M pairs) v1,RNA,sample,RNA Sequencing Analysis : Tru-Seq Strand Specific Large Insert RNA Sequencing - 75M Pairs,Genomics Platform,,",
        "P-EX-0051,Standard Germline Exome v6,Exome,sample,Exome Sequencing Analysis : Standard Germline Exome v6,Genomics Platform,,",
        "XTNL-WES-010291,XTNL-WES-010291 Standard Germline Exome v6,Exome,sample,Exome Sequencing Analysis : WES-010291 Standard Germline Exome v6,Genomics Special Products,,",
        "P-WG-0092,Infinium Omni Express + Exome Array Processing v3,Whole Genome Genotyping,sample,Whole Genome Arrays (Processing only) : 8-Sample Array Processing,Genomics Platform,,",
        "XTNL-EXT-010102_7,EXT-010102 DNA Extraction from Buffy Coat v 2.0,Sample Initiation, Qualification & Cell Culture,sample,Extraction and Sample Handling : EXT-010102 Extraction,Genomics Special Products,,",
        "P-ESH-0073,DNA Extraction from Blood v 2.0,Sample Initiation, Qualification & Cell Culture,sample,Extraction and Sample Handling : DNA Extraction from Blood,Genomics Platform,,",
        "P-ESH-0074,DNA and RNA Extraction from Stool without Dissection v 2.0,Sample Initiation, Qualification & Cell Culture,sample,Extraction and Sample Handling : DNA and RNA Extraction from Stool without Dissection,Genomics Platform,,",
        "P-ESH-0075,DNA and RNA Extraction from Stool with Frozen Dissection v 2.0,Sample Initiation, Qualification & Cell Culture,sample,Extraction and Sample Handling : DNA and RNA Extraction from Stool with Frozen Dissection,Genomics Platform,,",
        "P-ESH-0076,DNA and RNA Extraction from Stool with Fresh Dissection v 2.0,Sample Initiation, Qualification & Cell Culture,sample,Extraction and Sample Handling : DNA and RNA Extraction from Stool with Fresh Dissection,Genomics Platform,,",
        "XTNL-WGS-010348,WGS-010348 Genome, PCR-Free, 80X v1,Whole Genome Sequencing,sample,Genome Sequencing Analysis : WGS-010348 Genome, PCR-Free, 80X,Genomics Special Products,yes,",
        "P-WG-0098,PCR+ Human WGS - 30x UNALIGNED v1,Whole Genome Sequencing,sample,Genome Sequencing Analysis : PCR+ Human WGS - 30x,Genomics Platform,,",
        "XTNL-WGS-010345,WGS-010324 Genome, PCR-Free, 30X v1,Whole Genome Sequencing,sample,Genome Sequencing Analysis : WGS-010324 Genome, PCR-Free, 30X,Genomics Special Products,yes,",
        "XTNL-RNA-010412,RNA-010412 Express Transcriptome Capture,RNA,sample,RNA Sequencing Analysis : RNA-010412 Express Transcriptome Capture,Genomics Special Products,yes,",
        "XTNL-RNA-010411,RNA-010411 Express Strand Specific RNA Sequencing - High Coverage (50M pairs),RNA,sample,RNA Sequencing Analysis : RNA-010411 Express Strand Specific RNA Sequencing - High Coverage (50M pairs),Genomics Special Products,yes,",
        "P-SEQ-0032,NovaSeq S4 up to 300 cycles,Sequence Only,Flowcell,Illumina Sequencing Only : NovaSeq S4 up to 300 cycles,Genomics Platform,,",
        "P-MCV-0011,Malaria Amplicon Sequencing v1,Microbial & Viral Analysis,sample,Assembly and Metagenomic Analysis : Malaria Amplicon Sequencing,Genomics Platform,,",
        "P-ALT-0021,GP Dev - RNA,Alternate Library Prep & Development,Each,Materials : Custom Lab Work,Genomics Platform,,",
        "P-ALT-0022,10X Single Cell Transcriptome,Alternate Library Prep & Development,sample,RNA Sequencing Analysis : 10X Single Cell Transcriptome,Genomics Platform,,",
        "P-ALT-0023,mRNA SMART-Seq2 Single-cell v1,Alternate Library Prep & Development,plate,RNA Sequencing Analysis : mRNA SMART-Seq2 Single-Cell,Genomics Platform,,",
        "XTNL-RNA-010410,RNA-010410 Stranded, Long Insert Transcriptome Unaligned v1,RNA,Each,Materials : Custom Lab Work,Genomics Special Products,yes,",
        "XTNL-WES-010289,WES-010289 Deep Coverage Exome for Cell-Free Liquid Biopsy v2,Exome,sample,Exome Sequencing Analysis : WES-010246 Somatic Human WES (Deep Coverage),Genomics Special Products,yes,",
        "XTNL-RNA-010414,RNA-010414 mRNA SMART-Seq2 Single-Cell v1,RNA,plate,RNA Sequencing Analysis : RNA-010414 mRNA SMART-Seq2 Single-Cell,Genomics Special Products,yes,",
        "XTNL-RNA-010413,RNA-010413 mRNA SMART-Seq2 Cell-Population v1,RNA,plate,RNA Sequencing Analysis : RNA-010413 mRNA SMART-Seq2 Cell-Population,Genomics Special Products,yes,",
        "P-ESH-0077,cfDNA extraction from plasma_DEV,Sample Initiation, Qualification & Cell Culture,Each,Materials : Custom Lab Work,Genomics Platform,,",
        "P-EX-0050,Deep Coverage Exome from non-cfDNA ULP libraries v1,Exome,sample,Exome Sequencing Analysis : Somatic Human WES (Deep Coverage),Genomics Platform,,",
        "P-ALT-0032,Custom Panels_Hybrid Selection Exome,Alternate Library Prep & Development,Each,Materials : Custom Lab Work,Genomics Platform,,",
        "P-ESH-0072,DNA Extraction from Buffy Coat v 2.0,Sample Initiation, Qualification & Cell Culture,sample,Extraction and Sample Handling : DNA Extraction from Buffy Coat,Genomics Platform,,",
        "P-WG-0097,Plasma Combination for ULP Blood Biopsy Extraction,Sample Initiation, Qualification & Cell Culture,Each,Materials : Custom Lab Work,Genomics Platform,,",
        "XTNL-WGS-010349,XTNL-WGS-010349 Plasma Combination for ULP Blood Biopsy Extraction,Sample Initiation, Qualification & Cell Culture,Each,Materials : Custom Lab Work,Genomics Special Products,yes,",
        "P-EX-0048,Deep Coverage Exome for Cell-Free Liquid Biopsy v2,Exome,sample,Exome Sequencing Analysis : Somatic Human WES (Deep Coverage),Genomics Platform,,",
        "P-ALT-0034,mRNA SMART-Seq2 Cell-Population v1,Alternate Library Prep & Development,plate,RNA Sequencing Analysis : mRNA SMART-Seq2 Cell-Population,Genomics Platform,,",
        "XTNL-WGS-010350,WGS-010350 Genome, PCR Plus, 20X v1,Whole Genome Sequencing,sample,Genome Sequencing Analysis : WGS-010322 Genome, PCR-Free, 20X,Genomics Special Products,yes,",
        "P-MCV-0013,Microbial WGS Mid-output Express (1.5GB),Microbial & Viral Analysis,Each,Materials : Custom Lab Work,Genomics Platform,,",
        "P-MCV-0015,Microbial WGS High-output Express (3GB),Microbial & Viral Analysis,Each,Materials : Custom Lab Work,Genomics Platform,,",
        "P-WG-0086,PCR-Free Human WGS - 20x v1 (High Volume, >10,000 samples),Whole Genome Sequencing,sample,Genome Sequencing Analysis : PCR-Free Human WGS - 20x (High Volume, >10,000 samples),Genomics Platform,,",
        "P-WG-0088,PCR-Free Human WGS - 60x v1 (High Volume, >10,000 samples),Whole Genome Sequencing,sample,Genome Sequencing Analysis : PCR-Free Human WGS - 60x (High Volume, >10,000 samples),Genomics Platform,,",
        "XTNL-WGS-010344,WGS-010323 Genome, PCR-Free, 20X v1,Whole Genome Sequencing,sample,Genome Sequencing Analysis : WGS-010323 Genome, PCR-Free, 20X,Genomics Special Products,yes,",
        "P-WG-0089,PCR-Free Human WGS - 80x v1 (High Volume, >10,000 samples),Whole Genome Sequencing,sample,Genome Sequencing Analysis : PCR-Free Human WGS - 80x (High Volume, >10,000 samples),Genomics Platform,,",
        "XTNL-WGS-010343,WGS-010322 Genome, PCR-Free, 20X v1,Whole Genome Sequencing,sample,Genome Sequencing Analysis : WGS-010322 Genome, PCR-Free, 20X,Genomics Special Products,yes,",
        "XTNL-SAM-010704,SAM-010704 Sample Qualification Only - Human,Sample Initiation, Qualification & Cell Culture,sample,Extraction and Sample Handling : SAM-010704 Sample Qualification Only - Human,Genomics Special Products,yes,",
        "P-EX-0049,Standard Coverage Exome for Liquid Biopsy Normal v2,Exome,sample,Exome Sequencing Analysis : Somatic Human WES (Standard Coverage),Genomics Platform,,",
        "P-ESH-0071,GTEx Cutlist Pre-tare,Sample Initiation, Qualification & Cell Culture,Each,Materials : Custom Lab Work,Genomics Platform,,",
        "P-RNA-0023,Tru-Seq Strand Specific Large Insert RNA Sequencing Unaligned v1,RNA,Each,Materials : Custom Lab Work,Genomics Platform,,",
        "P-CLA-0009,CLIA Infinium MEGA Array,Whole Genome Genotyping,Each,Materials : Custom Lab Work,CRSP,,yes",
        "XTNL-WGS-010346,WGS-010346 Blood Biopsy Extraction and ULP WGS QC v2,Whole Genome Sequencing,sample,Genome Sequencing Analysis : WGS-010325 Blood Biopsy Extraction and ULP WGS QC,Genomics Special Products,yes,",
        "XTNL-EXT-010101_3,EXT-010101 DNA Extraction from Blood v 2.0,Sample Initiation, Qualification & Cell Culture,sample,Extraction and Sample Handling : EXT-010101 Extraction,Genomics Special Products,yes,",
        "P-CLA-0010,CLIA Infinium AoU Array,Whole Genome Genotyping,Each,Materials : Custom Lab Work,CRSP,,yes",
        "P-MCV-0016,Microbial WGS Low-output (.25GB),Microbial & Viral Analysis,Each,Materials : Custom Lab Work,Genomics Platform,,");

        Integer testPrice = 1000;
        for (String productDump : initialTest) {


            String[] dividedProductInfo = StringUtils.split(productDump);
            boolean isCommercial = BooleanUtils.toBoolean(dividedProductInfo[7]) || BooleanUtils.toBoolean(dividedProductInfo[8]);
            SapIntegrationClientImpl.SAPCompanyConfiguration companyCode =
                isCommercial ? SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD :
                    SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES;
            SAPMaterial initialMaterial =
                new SAPMaterial(dividedProductInfo[0], companyCode, companyCode.getDefaultWbs(), dividedProductInfo[1],
                    String.valueOf(testPrice), SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA,
                    BigDecimal.ZERO, new Date(), new Date(), null, null,
                    SAPMaterial.MaterialStatus.ENABLED, companyCode.getSalesOrganization());
            testPrice += 10;
            sapMaterials.add(initialMaterial);
        }

        return sapMaterials;
    }

    @Override
    public OrderCalculatedValues calculateOpenOrderValues(int addedSampleCount, SapQuote sapQuote,
                                                          ProductOrder productOrder) throws SAPIntegrationException {
        return new OrderCalculatedValues(BigDecimal.ONE, Collections.<OrderValue>emptySet(),productOrder.getSapOrderNumber(),
                sapQuote.getQuoteHeader().fundsRemaining());
    }

    @Override
    public SapQuote findSapQuote(String sapQuoteId) throws SAPIntegrationException {
        return null;
    }

    @Override
    public String creditDelivery(BillingCredit billingReturn) throws SAPIntegrationException {
        return null;
    }
}
