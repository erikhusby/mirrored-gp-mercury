package org.broadinstitute.gpinformatics.infrastructure.sap;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.boundary.billing.QuoteImportItem;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Stub;
import org.broadinstitute.gpinformatics.infrastructure.quote.FundingLevel;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quote;
import org.broadinstitute.sap.entity.Condition;
import org.broadinstitute.sap.entity.DeliveryCondition;
import org.broadinstitute.sap.entity.OrderCalculatedValues;
import org.broadinstitute.sap.entity.OrderValue;
import org.broadinstitute.sap.entity.SAPMaterial;
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
    public String createOrderWithQuote(ProductOrder placedOrder) throws SAPIntegrationException {
        throw new SAPIntegrationException("SAP Quotes nto available at this time");
    }

    @Override
    public void updateOrder(ProductOrder placedOrder, boolean closingOrder) throws SAPIntegrationException {
    }

    @Override
    public void updateOrderWithQuote(ProductOrder placedOrder, boolean closingOrder) throws SAPIntegrationException {
        throw new SAPIntegrationException("SAP Quotes nto available at this time");
    }

    @Override
    public String findCustomer(SapIntegrationClientImpl.SAPCompanyConfiguration companyCode, FundingLevel fundingLevel) throws SAPIntegrationException {
        return TEST_CUSTOMER_NUMBER;
    }

    @Override
    public String billOrder(QuoteImportItem item, BigDecimal quantityOverride, Date workCompleteDate) throws SAPIntegrationException {
        return TEST_DELIVERY_DOCUMENT_ID;
    }

    @Override
    public void publishProductInSAP(Product product) throws SAPIntegrationException {
        
    }

    @Override
    public Set<SAPMaterial> findProductsInSap() throws SAPIntegrationException {
        final Set<SAPMaterial> sapMaterials = new HashSet<>();
        List<String> initialTest = Arrays.asList("P-ALT-0018,10X Genomics,Alternate Library Prep & Development,Custom Lab Work,Genomics Platform,Materials,0",
                "P-ALT-0019,10X Genomics Caliper QC Only,Alternate Library Prep & Development,Custom Lab Work,Genomics Platform,Materials,0",
                "P-ANA-0001,Picard Analysis of External Data - Human Exome,Data Analysis,Picard Analysis of External Data - Human Exome,Genomics Platform,Analysis Only,0",
                "P-ANA-0005,Picard Analysis of External Data - Human Whole Genome (<35x coverage),Data Analysis,Picard Analysis of External Data - Human Whole Genome (<35x coverage),Genomics Platform,Analysis Only,0",
                "P-ANA-0006,Picard Analysis of External Data - Human Whole Genome (35x < x < 65x coverage),Data Analysis,Picard Analysis of External Data - Human Whole Genome (35x < x < 65x coverage),Genomics Platform,Analysis Only,0",
                "P-ANA-00067,Data Processing on-prem/non-Google Cloud,Data Analysis,Data Processing on-prem/non-Google Cloud,Genomics Platform,Genome Sequencing Analysis,0",
                "P-ANA-00068,Data Delivery via Aspera,Data Analysis,Data Delivery via Aspera,Genomics Platform,Genome Sequencing Analysis,0",
                "P-EX-0012,Express Somatic Human WES (Deep Coverage) v1,Exome,Express Somatic Human WES (Deep Coverage),Genomics Platform,Exome Sequencing Analysis,0",
                "P-EX-0013,Express Somatic Human WES (Standard Coverage) v1,Exome,Express Somatic Human WES (Standard Coverage),Genomics Platform,Exome Sequencing Analysis,0",
                "P-EX-0018,Standard Germline Exome v5,Exome,Standard Germline Exome,Genomics Platform,Exome Sequencing Analysis,0",
                "P-EX-0021,Standard Germline Exome v5 Plus GWAS Supplement Array,Exome,Standard Germline Exome Plus GWAS Supplement Array,Genomics Platform,Exome Sequencing Analysis,0",
                "P-EX-0022,Mendelian Rare Disease Exome v2,Exome,Mendelian Rare Disease Exome,Genomics Platform,Exome Sequencing Analysis,0",
                "P-EX-0023,Express FFPE Somatic Human WES (Deep Coverage) v1,Exome,Express FFPE Somatic Human WES (Deep Coverage),Genomics Platform,Exome Sequencing Analysis,0",
                "P-EX-0027,FFPE Somatic Human WES (Deep Coverage) v1,Exome,FFPE Somatic Human WES (Deep Coverage),Genomics Platform,Exome Sequencing Analysis,0",
                "P-EX-0028,Somatic Human WES (Deep Coverage) v1,Exome,Somatic Human WES (Deep Coverage),Genomics Platform,Exome Sequencing Analysis,0",
                "P-EX-0029,Somatic Human WES (Standard Coverage) v1,Exome,Somatic Human WES (Standard Coverage),Genomics Platform,Exome Sequencing Analysis,0",
                "P-EX-0032,Deep Coverage Exome for Cell-Free Liquid Biopsy,Exome,Express Somatic Human WES (Deep Coverage),Genomics Platform,Exome Sequencing Analysis,0",
                "P-EX-0033,Standard Coverage Exome for Liquid Biopsy Normal,Exome,Express Somatic Human WES (Standard Coverage),Genomics Platform,Exome Sequencing Analysis,0",
                "P-MCV-0009,Standard 16S Sequencing (Stool),Microbial & Viral Analysis,16S Sequencing,Genomics Platform,Assembly and Metagenomic Analysis,0",
                "P-MCV-0010,Standard 16S Sequencing (Biopsy),Microbial & Viral Analysis,16S Sequencing,Genomics Platform,Assembly and Metagenomic Analysis,0",
                "P-RNA-0006,Fast Track Strand Specific RNA Sequencing - High Coverage (50M pairs),RNA,Custom Lab Work,Genomics Platform,Materials,0",
                "P-RNA-0016,Transcriptome Capture v1,RNA,Transcriptome Capture,Genomics Platform,RNA Sequencing Analysis,0",
                "P-RNA-0017,Fast Track Transcriptome Capture - High Coverage (50M pairs),RNA,Custom Lab Work,Genomics Platform,Materials,0",
                "P-RNA-0019,Tru-Seq Strand Specific Large Insert RNA Sequencing (50M pairs) v1.1,RNA,Tru-Seq Strand Specific Large Insert RNA Sequencing - High Coverage (50M pairs),Genomics Platform,RNA Sequencing Analysis,0",
                "P-RNA-0020,Tru-Seq Non-Strand Specific RNA Sequencing (50M pairs) v1.1,RNA,Tru-Seq Non-Strand Specific RNA Sequencing - High Coverage (50M pairs),Genomics Platform,RNA Sequencing Analysis,0",
                "P-RNA-0021,Tru-Seq Strand Specific Large Insert RNA Sequencing (40M pairs) v1.1,RNA,Tru-Seq Strand Specific Large Insert RNA Sequencing - Med Coverage (40M pairs),Genomics Platform,RNA Sequencing Analysis,0",
                "P-ESH-0002,Sample Qualification Only (Human Samples),Sample Initiation, Qualification & Cell Culture,Sample Qualification Only - Human,Genomics Platform,Extraction and Sample Handling,0",
                "P-ESH-0009,DNA or RNA External Plating into Tubes,Sample Initiation, Qualification & Cell Culture,DNA or RNA External Plating into Tubes,Genomics Platform,Extraction and Sample Handling,0",
                "P-ESH-0010,DNA External Plating into Plates,Sample Initiation, Qualification & Cell Culture,DNA External Plating into Plates,Genomics Platform,Extraction and Sample Handling,0",
                "P-ESH-0011,Viable Cell Line Retrieval,Sample Initiation, Qualification & Cell Culture,Viable Cell Line Retrieval,Genomics Platform,Extraction and Sample Handling,0",
                "P-ESH-0016,PICO Only,Sample Initiation, Qualification & Cell Culture,Custom Lab Work,Genomics Platform,Materials,0",
                "P-ESH-0021,Sample Transfer into Broad Tubes,Sample Initiation, Qualification & Cell Culture,Sample Transfer into Broad Tubes,Genomics Platform,Extraction and Sample Handling,0",
                "P-ESH-0022,Fragment Integrity QC,Sample Initiation, Qualification & Cell Culture,Fragment Integrity QC,Genomics Platform,Extraction and Sample Handling,0",
                "P-ESH-0026,RNAQC Only,Sample Initiation, Qualification & Cell Culture,Custom Lab Work,Genomics Platform,Materials,0",
                "P-ESH-0027,Sample Destruction - Physical AND Electronic Termination,Sample Initiation, Qualification & Cell Culture,Stock Tube Return,Genomics Platform,Extraction and Sample Handling,0",
                "P-ESH-0028,DNA Extraction from Blood,Sample Initiation, Qualification & Cell Culture,DNA Extraction from Whole Blood up to 1 mL,Genomics Platform,Extraction and Sample Handling,0",
                "P-ESH-0029,DNA Extraction from Saliva,Sample Initiation, Qualification & Cell Culture,DNA or RNA Extract from Fresh Frozen Tissue, Cell Pellet, Stool, Saliva,Genomics Platform,Extraction and Sample Handling,0",
                "P-ESH-0030,DNA Extraction from Buffy Coat,Sample Initiation, Qualification & Cell Culture,DNA Extract from Buffy coat,Genomics Platform,Extraction and Sample Handling,0",
                "P-ESH-0031,DNA Extraction from Cell Pellet,Sample Initiation, Qualification & Cell Culture,DNA or RNA Extract from Fresh Frozen Tissue, Cell Pellet, Stool, Saliva,Genomics Platform,Extraction and Sample Handling,0",
                "P-ESH-0032,DNA Extraction from Frozen Tissue,Sample Initiation, Qualification & Cell Culture,DNA or RNA Extract from Fresh Frozen Tissue, Cell Pellet, Stool, Saliva,Genomics Platform,Extraction and Sample Handling,0",
                "P-ESH-0033,DNA Extraction from Stool,Sample Initiation, Qualification & Cell Culture,DNA or RNA Extract from Fresh Frozen Tissue, Cell Pellet, Stool, Saliva,Genomics Platform,Extraction and Sample Handling,0",
                "P-ESH-0034,RNA Extraction from PAXGene Blood,Sample Initiation, Qualification & Cell Culture,RNA Extraction from PAXgene Preserved Blood,Genomics Platform,Extraction and Sample Handling,0",
                "P-ESH-0035,RNA Extraction from PAXGene Tissue,Sample Initiation, Qualification & Cell Culture,DNA and RNA Extraction from PAXgene Preserved Tissue,Genomics Platform,Extraction and Sample Handling,0",
                "P-ESH-0036,RNA Extraction from Cell Pellet,Sample Initiation, Qualification & Cell Culture,RNA Extraction from Cell Pellets,Genomics Platform,Extraction and Sample Handling,0",
                "P-ESH-0037,RNA Extraction from Frozen Tissue,Sample Initiation, Qualification & Cell Culture,DNA or RNA Extract from Fresh Frozen Tissue, Cell Pellet, Stool, Saliva,Genomics Platform,Extraction and Sample Handling,0",
                "P-ESH-0038,DNA Extraction From Blood Spots,Sample Initiation, Qualification & Cell Culture,DNA or RNA Extract from Fresh Frozen Tissue, Cell Pellet, Stool, Saliva,Genomics Platform,Extraction and Sample Handling,0",
                "P-ESH-0039,DNA Extraction from FFPE,Sample Initiation, Qualification & Cell Culture,DNA Extract from FFPE or slides,Genomics Platform,Extraction and Sample Handling,0",
                "P-ESH-0040,DNA and RNA Extraction from Cell Pellet,Sample Initiation, Qualification & Cell Culture,DNA and RNA Extract from Fresh Frozen Tissue or stool (ALLPREP),Genomics Platform,Extraction and Sample Handling,0",
                "P-ESH-0041,DNA and RNA Extraction from Frozen Tissue,Sample Initiation, Qualification & Cell Culture,DNA and RNA Extract from Fresh Frozen Tissue or stool (ALLPREP),Genomics Platform,Extraction and Sample Handling,0",
                "P-ESH-0042,DNA and RNA Extraction from FFPE,Sample Initiation, Qualification & Cell Culture,DNA and RNA Extract from Fresh Frozen Tissue or stool (ALLPREP),Genomics Platform,Extraction and Sample Handling,0",
                "P-ESH-0043,DNA and RNA Extraction from Stool,Sample Initiation, Qualification & Cell Culture,DNA and RNA Extract from Fresh Frozen Tissue or stool (ALLPREP),Genomics Platform,Extraction and Sample Handling,0",
                "P-ESH-0044,Pre-Processing (Dissection) of Stool for Extraction,Sample Initiation, Qualification & Cell Culture,Custom Lab Work,Genomics Platform,Materials,0",
                "P-ESH-0045,Stool Extraction from approved  tubes (DNA & RNA),Sample Initiation, Qualification & Cell Culture,Custom Lab Work,Genomics Platform,Materials,0",
                "P-ESH-0046,Stool Extraction from ethanol (DNA & RNA),Sample Initiation, Qualification & Cell Culture,Custom Lab Work,Genomics Platform,Materials,0",
                "P-ESH-0047,Stool Extraction requiring extra dissection,Sample Initiation, Qualification & Cell Culture,Custom Lab Work,Genomics Platform,Materials,0",
                "P-ESH-0052,Ship and Store,Sample Initiation, Qualification & Cell Culture,Ship and Store,Genomics Platform,Genome Sequencing Analysis,0",
                "P-ESH-0053,Stock Tube Return,Sample Initiation, Qualification & Cell Culture,Stock Tube Return,Genomics Platform,Extraction and Sample Handling,0",
                "P-ESH-0054,Cell-Free DNA Extraction,Sample Initiation, Qualification & Cell Culture,Custom Lab Work,Genomics Platform,Materials,0",
                "P-GTX-0004,GTEx EBV Transformation,Sample Initiation, Qualification & Cell Culture,Materials,Genomics Platform,Materials,0",
                "P-GTX-0005,GTEx Fribroblast Initiation,Sample Initiation, Qualification & Cell Culture,Materials,Genomics Platform,Materials,0",
                "P-GTX-0006,GTEx DNA Extraction from Cell Pellet,Sample Initiation, Qualification & Cell Culture,Materials,Genomics Platform,Materials,0",
                "P-GTX-0007,GTEx DNA and RNA Extraction from PAXgene Preserved Tissue,Sample Initiation, Qualification & Cell Culture,DNA and RNA Extraction from PAXgene Preserved Tissue,Genomics Platform,Extraction and Sample Handling,0",
                "P-GTX-0008,GTEx DNA and RNA Extraction from Fresh Frozen Tissue,Sample Initiation, Qualification & Cell Culture,DNA and RNA Extracton from Fresh Frozen Tissue,Genomics Platform,Extraction and Sample Handling,0",
                "P-GTX-0009,GTEx RNA Extraction from PAXgene Preserved Blood,Sample Initiation, Qualification & Cell Culture,RNA Extraction from PAXgene Preserved Blood,Genomics Platform,Extraction and Sample Handling,0",
                "P-GTX-0010,GTEx RNA Extraction from Cell Pellets,Sample Initiation, Qualification & Cell Culture,RNA Extraction from Cell Pellets,Genomics Platform,Extraction and Sample Handling,0",
                "P-GTX-0011,GTEx DNA Extraction from blood,Sample Initiation, Qualification & Cell Culture,DNA or RNA Extract from Fresh Frozen Tissue, Cell Pellet, Stool, Saliva,Genomics Platform,Extraction and Sample Handling,0",
                "P-GTX-0012,GTEx Tissue External Shipment,Sample Initiation, Qualification & Cell Culture,Custom Lab Work,Genomics Platform,Materials,0",
                "P-SEQ-0001,HiSeq 2x25 Paired Lane,Sequence Only,HiSeq 2x25 Paired Lane,Genomics Platform,Illumina Sequencing Only,0",
                "P-SEQ-0002,HiSeq 44 Single Lane,Sequence Only,HiSeq 44 Single lane,Genomics Platform,Illumina Sequencing Only,0",
                "P-SEQ-0003,HiSeq 2x76 Paired Lane,Sequence Only,HiSeq 2x76 Paired lane,Genomics Platform,Illumina Sequencing Only,0",
                "P-SEQ-0004,HiSeq 2x101 Paired Lane,Sequence Only,HiSeq 2x101 Paired lane,Genomics Platform,Illumina Sequencing Only,0",
                "P-SEQ-0005,MiSeq up to 50 cycles,Sequence Only,MiSeq up to 50 cycles,Genomics Platform,Illumina Sequencing Only,0",
                "P-SEQ-0006,MiSeq up to 300 cycles,Sequence Only,MiSeq up to 300 cycles,Genomics Platform,Illumina Sequencing Only,0",
                "P-SEQ-0007,MiSeq up to 500 cycles,Sequence Only,MiSeq up to 500 cycles,Genomics Platform,Illumina Sequencing Only,0",
                "P-SEQ-0010,HiSeq 2500 up to 50 cycles,Sequence Only,HiSeq 2500 up to 50 cycles,Genomics Platform,Illumina Sequencing Only,0",
                "P-SEQ-0011,HiSeq 2500 up to 200 cycles,Sequence Only,HiSeq 2500 up to 200 cycles,Genomics Platform,Illumina Sequencing Only,0",
                "P-SEQ-0012,HiSeq 2500 up to 250 cycles,Sequence Only,HiSeq 2500 up to 250 cycles,Genomics Platform,Illumina Sequencing Only,0",
                "P-SEQ-0014,HiSeq 2500 up to 300 cycles,Sequence Only,HiSeq 2500 up to 300 cycles,Genomics Platform,Illumina Sequencing Only,0",
                "P-SEQ-0017,HiSeq 2500 2x250 Paired Lane,Sequence Only,HiSeq 2500 2x250 Paired Lane,Genomics Platform,Illumina Sequencing Only,0",
                "P-SEQ-0018,HiSeq X Ten 2x151 Paired Lane,Sequence Only,Custom Lab Work,Genomics Platform,Materials,0",
                "P-SEQ-0020,NextSeq up to 75 cycles,Sequence Only,NextSeq up to 75 cycles,Genomics Platform,Illumina Sequencing Only,0",
                "P-SEQ-0021,NextSeq up to 150 cycles,Sequence Only,NextSeq up to 150 cycles,Genomics Platform,Illumina Sequencing Only,0",
                "P-SEQ-0022,NextSeq up to 300 cycles,Sequence Only,NextSeq up to 300 cycles,Genomics Platform,Illumina Sequencing Only,0",
                "P-SEQ-0023,HiSeq X Ten 2x101 Paired Lane,Sequence Only,Custom Lab Materials,Genomics Platform,Materials,0",
                "P-SEQ-0024,HiSeq 4000 up to 50 cycles,Sequence Only,HiSeq 4000 up to 50 cycles,Genomics Platform,Illumina Sequencing Only,0",
                "P-SEQ-0025,HiSeq 4000 up to 300 cycles,Sequence Only,HiSeq 4000 up to 300 cycles,Genomics Platform,Illumina Sequencing Only,0",
                "P-SEQ-0027,HiSeq 2x50 Paired Lane,Sequence Only,HiSeq 2x50 Paired Lane,Genomics Platform,Illumina Sequencing Only,0",
                "P-SEQ-0028,HiSeq 4000 up to 150 cycles,Sequence Only,HiSeq 4000 up to 150 cycles,Genomics Platform,Illumina Sequencing Only,0",
                "P-VAL-0004,Custom Reagent Orders,Small Design, Validation & Extension,Custom Lab Materials,Genomics Platform,Materials,0",
                "P-VAL-0006,Custom Genotyping Design Validation and Orders,Small Design, Validation & Extension,Materials,Genomics Platform,Materials,0",
                "P-VAL-0011,TSCA + Walk-Up Sequencing,Small Design, Validation & Extension,Custom Lab Work,Genomics Platform,Materials,0",
                "P-VAL-0012,Custom Validation_Development Work,Small Design, Validation & Extension,Custom Lab Materials,Genomics Platform,Materials,0",
                "P-WG-0023,Infinium Omni Express + Exome Array Processing,Whole Genome Genotyping,8-Sample Array Processing,Genomics Platform,Whole Genome Arrays (Processing only),0",
                "P-WG-0053,Infinium GWAS Supplement Array,Whole Genome Genotyping,GWAS Supplement Array,Genomics Platform,Whole Genome Arrays,0",
                "P-WG-0055,Infinium Psych Array Processing v2,Whole Genome Genotyping,12 or 24-Sample Array Processing,Genomics Platform,Whole Genome Arrays (Processing only),0",
                "P-WG-0056,Infinium Array Orders,Whole Genome Genotyping,Custom Lab Work,Genomics Platform,Materials,0",
                "P-WG-0058,Infinium MEGA Array Processing,Whole Genome Genotyping,8-Sample Array Processing,Genomics Platform,Whole Genome Arrays (Processing only),0",
                "P-WG-0059,Infinium MEGA (High Volume >40,000 sample) Array Processing,Whole Genome Genotyping,8-Sample Array Processing (High Volume, >40,000 samples),Genomics Platform,Whole Genome Arrays (Processing only),0",
                "P-WG-0066,Infinium Global Screening Array Processing,Whole Genome Genotyping,12 or 24-Sample Array Processing,Genomics Platform,Whole Genome Arrays (Processing only),0",
                "P-WG-0068,Infinium EPIC Array Processing,Whole Genome Genotyping,8-Sample Methylation Array Processing,Genomics Platform,Whole Genome Arrays (Processing only),0",
                "P-WG-0070,Infinium Omni Express + Exome Array Processing v2,Whole Genome Genotyping,8-Sample Array Processing,Genomics Platform,Whole Genome Arrays (Processing only),0",
                "P-WG-0072,Infinium Psych Array Processing v3,Whole Genome Genotyping,12 or 24-Sample Array Processing,Genomics Platform,Whole Genome Arrays (Processing only),0",
                "P-WG-0046,PCR-Free Human WGS (Light Coverage) v1,Whole Genome Sequencing,PCR-Free Human WGS (Light Coverage),Genomics Platform,Genome Sequencing Analysis,0",
                "P-WG-0047,PCR-Free Human WGS (Standard Coverage) v1,Whole Genome Sequencing,PCR-Free Human WGS (Standard Coverage),Genomics Platform,Genome Sequencing Analysis,0",
                "P-WG-0048,PCR-Free Human WGS (Deep Coverage) v1,Whole Genome Sequencing,PCR-Free Human WGS (Deep Coverage),Genomics Platform,Genome Sequencing Analysis,0",
                "P-WG-0049,PCR+ Human WGS (Standard Coverage) v1,Whole Genome Sequencing,PCR+ Human WGS (Standard Coverage),Genomics Platform,Genome Sequencing Analysis,0",
                "P-WG-0050,PCR+ Human WGS (Deep Coverage) v1,Whole Genome Sequencing,PCR+ Human WGS (Deep Coverage),Genomics Platform,Genome Sequencing Analysis,0",
                "P-WG-0067,PCR+ Human WGS (Ultra-Low Coverage) v1,Whole Genome Sequencing,Custom Lab Work,Genomics Platform,Materials,0",
                "P-WG-0069,PCR-Free Human WGS (Standard Coverage) v1.1,Whole Genome Sequencing,PCR-Free Human WGS (Standard Coverage),Genomics Platform,Genome Sequencing Analysis,0",
                "P-WG-0071,PCR-Free Human WGS (Light Coverage) v1.1,Whole Genome Sequencing,PCR-Free Human WGS (Light Coverage),Genomics Platform,Genome Sequencing Analysis,0",
                "XTNL-AAS-010100,AAS-010100 Germline VCF,Data Analysis,AAS-010100 Germline VCF,Genomics Special Products,Analysis and Storage,1",
                "XTNL-AAS-010101,AAS-010101 Somatic VCF and MAF,Data Analysis,AAS-010101 Somatic MAF,Genomics Special Products,Analysis and Storage,1",
                "XTNL-AAS-010102,AAS-010102 Storage Exome or RNA,Data Analysis,AAS-010102 Storage Exome or RNA,Genomics Special Products,Analysis and Storage,1",
                "XTNL-AAS-010103,AAS-010103 Storage Genome,Data Analysis,AAS-010103 Storage Genome,Genomics Special Products,Analysis and Storage,1",
                "XTNL-AAS-010104,AAS-010104 Summary Report,Data Analysis,AAS-010104 Summary Report,Genomics Special Products,Analysis and Storage,1",
                "P-CLA-0003,CLIA Germline Exome,Exome,Germline Exome,CRSP,Exome Sequencing Analysis,1",
                "P-CLA-0004,CLIA Somatic Exome,Exome,Somatic Exome,CRSP,Exome Sequencing Analysis,1",
                "P-EX-0011,Clinical Custom Hybrid Selection - Buick,Exome,Materials,CRSP,Materials,1",
                "XTCP-WES-0002,CP Human WES (80/20),Exome,CP Human WES (80/20),Genomics CP,Exome Sequencing Analysis,1",
                "XTCP-WES-0003,CP Human WES (85/50),Exome,CP Human WES (85/50),Genomics CP,Exome Sequencing Analysis,1",
                "XTCP-WES-0004,NCP Human WES - Tumor (150xMTC),Exome,NCP Human WES - Tumor (150xMTC),Genomics CP,Exome Sequencing Analysis,1",
                "XTCP-WES-0005,NCP Human WES - Normal (150xMTC),Exome,NCP Human WES - Normal (150xMTC),Genomics CP,Exome Sequencing Analysis,1",
                "XTNL-CAP-020100,CAP-020100 Germline Exome (<24 Samples/Batch),Exome,CAP-020100 Germline Exome (<24 Samples/Batch),CRSP,Exome Sequencing Analysis,1",
                "XTNL-CAP-020103,CAP-020103 Parent w/Trio Exome,Exome,CAP-020103 Parent w/Trio Exome,CRSP,Exome Sequencing Analysis,1",
                "XTNL-CAP-020201,CAP-020201 Somatic Exome,Exome,CAP-020201 Somatic Exome,CRSP,Exome Sequencing Analysis,1",
                "XTNL-WES-010201,WES-010201 Standard Germline Exome,Exome,WES-010201 Standard Germline Exome,Genomics Special Products,Exome Sequencing Analysis,1",
                "XTNL-WES-010202,WES-010202 Standard Germline Exome,Exome,WES-010202 Standard Germline Exome,Genomics Special Products,Exome Sequencing Analysis,1",
                "XTNL-WES-010203,WES-010203 Standard Germline Exome,Exome,WES-010203 Standard Germline Exome,Genomics Special Products,Exome Sequencing Analysis,1",
                "XTNL-WES-010204,WES-010204 Express Somatic Human WES (Standard Coverage),Exome,WES-010204 Express Somatic Human WES (Standard Coverage),Genomics Special Products,Exome Sequencing Analysis,1",
                "XTNL-WES-010205,WES-010205 Express Somatic Human WES (Standard Coverage),Exome,WES-010205 Express Somatic Human WES (Standard Coverage),Genomics Special Products,Exome Sequencing Analysis,1",
                "XTNL-WES-010206,WES-010206 Express Somatic Human WES (Standard Coverage),Exome,WES-010206 Express Somatic Human WES (Standard Coverage),Genomics Special Products,Exome Sequencing Analysis,1",
                "XTNL-WES-010207,WES-010207 Express Somatic Human WES (Deep Coverage),Exome,WES-010207 Express Somatic Human WES (Deep Coverage),Genomics Special Products,Exome Sequencing Analysis,1",
                "XTNL-WES-010208,WES-010208 Express Somatic Human WES (Deep Coverage),Exome,WES-010208 Express Somatic Human WES (Deep Coverage),Genomics Special Products,Exome Sequencing Analysis,1",
                "XTNL-WES-010209,WES-010209 Express Somatic Human WES (Deep Coverage),Exome,WES-010209 Express Somatic Human WES (Deep Coverage),Genomics Special Products,Exome Sequencing Analysis,1",
                "XTNL-WES-010210,WES-010210 Standard Germline Exome Plus GWAS Supplement Array,Exome,WES-010210 Standard Germline Exome Plus GWAS Supplement Array,Genomics Special Products,Exome Sequencing Analysis,1",
                "XTNL-WES-010211,WES-010211 Standard Germline Exome Plus GWAS Supplement Array,Exome,WES-010211 Standard Germline Exome Plus GWAS Supplement Array,Genomics Special Products,Exome Sequencing Analysis,1",
                "XTNL-WES-010212,WES-010212 Standard Germline Exome Plus GWAS Supplement Array,Exome,WES-010212 Standard Germline Exome Plus GWAS Supplement Array,Genomics Special Products,Exome Sequencing Analysis,1",
                "XTNL-WES-010213,WES-010213 Mendelian Rare Disease Exome,Exome,WES-010213 Mendelian Rare Disease Exome,Genomics Special Products,Exome Sequencing Analysis,1",
                "XTNL-WES-010214,WES-010214 Mendelian Rare Disease Exome,Exome,WES-010214 Mendelian Rare Disease Exome,Genomics Special Products,Exome Sequencing Analysis,1",
                "XTNL-WES-010215,WES-010215 Mendelian Rare Disease Exome,Exome,WES-010215 Mendelian Rare Disease Exome,Genomics Special Products,Exome Sequencing Analysis,1",
                "XTNL-WES-010216,WES-010216 Somatic Human WES (Deep Coverage),Exome,WES-010216 Somatic Human WES (Deep Coverage),Genomics Special Products,Exome Sequencing Analysis,1",
                "XTNL-WES-010217,WES-010217 Somatic Human WES (Deep Coverage),Exome,WES-010217 Somatic Human WES (Deep Coverage),Genomics Special Products,Exome Sequencing Analysis,1",
                "XTNL-WES-010218,WES-010218 Somatic Human WES (Deep Coverage),Exome,WES-010218 Somatic Human WES (Deep Coverage),Genomics Special Products,Exome Sequencing Analysis,1",
                "XTNL-WES-010219,WES-010219 Somatic Human WES (Standard Coverage),Exome,WES-010219 Somatic Human WES (Standard Coverage),Genomics Special Products,Exome Sequencing Analysis,1",
                "XTNL-WES-010220,WES-010220 Somatic Human WES (Standard Coverage),Exome,WES-010220 Somatic Human WES (Standard Coverage),Genomics Special Products,Exome Sequencing Analysis,1",
                "XTNL-WES-010221,WES-010221 Somatic Human WES (Standard Coverage),Exome,WES-010221 Somatic Human WES (Standard Coverage),Genomics Special Products,Exome Sequencing Analysis,1",
                "XTNL-WES-010222,WES-010222 Express FFPE Somatic Human WES (Deep Coverage),Exome,WES-010222 Express FFPE Somatic Human WES (Deep Coverage),Genomics Special Products,Exome Sequencing Analysis,1",
                "XTNL-WES-010223,WES-010223 Express FFPE Somatic Human WES (Deep Coverage),Exome,WES-010223 Express FFPE Somatic Human WES (Deep Coverage),Genomics Special Products,Exome Sequencing Analysis,1",
                "XTNL-WES-010224,WES-010224 Express FFPE Somatic Human WES (Deep Coverage),Exome,WES-010224 Express FFPE Somatic Human WES (Deep Coverage),Genomics Special Products,Exome Sequencing Analysis,1",
                "XTNL-WES-010225,WES-010225 FFPE Somatic Human WES (Deep Coverage),Exome,WES-010225 FFPE Somatic Human WES (Deep Coverage),Genomics Special Products,Exome Sequencing Analysis,1",
                "XTNL-WES-010226,WES-010226 FFPE Somatic Human WES (Deep Coverage),Exome,WES-010226 FFPE Somatic Human WES (Deep Coverage),Genomics Special Products,Exome Sequencing Analysis,1",
                "XTNL-WES-010227,WES-010227 FFPE Somatic Human WES (Deep Coverage),Exome,WES-010227 FFPE Somatic Human WES (Deep Coverage),Genomics Special Products,Exome Sequencing Analysis,1",
                "XTNL-MCB-01901,MCB-01900 16S Sequencing (Stool),Microbial & Viral Analysis,MCB-010901 16S Sequencing,Genomics Special Products,Genome Sequencing Analysis,1",
                "XTNL-MCB-01902,MCB-01900 16S Sequencing (Biopsy),Microbial & Viral Analysis,MCB-010901 16S Sequencing,Genomics Special Products,Genome Sequencing Analysis,1",
                "XTCP-RNA-0005,CP Transcriptome Capture, 50M,RNA,CP Transcriptome Capture, Full, 50M,Genomics CP,RNA Sequencing Analysis,1",
                "XTCP-RNA-0006,CP Transcriptome Capture, Full, 50M,RNA,CP Transcriptome Capture, 50M,Genomics CP,RNA Sequencing Analysis,1",
                "XTCP-RNA-0007,CP Transcriptome Capture, 150M,RNA,CP Transcriptome Capture, 150M,Genomics CP,RNA Sequencing Analysis,1",
                "XTCP-RNA-0011,CP Stranded, Long Insert, Full, 50M v1.1,RNA,CP Stranded, Long Insert, Full, 50M,Genomics CP,RNA Sequencing Analysis,1",
                "XTCP-RNA-0012,CP Stranded, Long Insert RNA, 150M v1.1,RNA,CP Stranded, Long Insert RNA, 150M,Genomics CP,RNA Sequencing Analysis,1",
                "XTCP-RNA-0013,CP Stranded, Long Insert, 50M v1.1,RNA,CP Stranded, Long Insert, 50M,Genomics CP,RNA Sequencing Analysis,1",
                "XTNL-RNA-010400.2,RNA-010400 Stranded, Long Insert Transcriptome v1.1,RNA,RNA-010400 Stranded, Long Insert Transcriptome,Genomics Special Products,RNA Sequencing Analysis,1",
                "XTNL-RNA-010401.2,RNA-010401 Stranded, Long Insert Transcriptome v1.1,RNA,RNA-010401 Stranded, Long Insert Transcriptome,Genomics Special Products,RNA Sequencing Analysis,1",
                "XTNL-RNA-010402.2,RNA-010402 Stranded, Long Insert Transcriptome v1.1,RNA,RNA-010402 Stranded, Long Insert Transcriptome,Genomics Special Products,RNA Sequencing Analysis,1",
                "XTNL-RNA-010403,RNA-010403 Transcriptome Capture,RNA,RNA-010403 Transcriptome Capture,Genomics Special Products,RNA Sequencing Analysis,1",
                "XTNL-RNA-010404,RNA-010404 Transcriptome Capture,RNA,RNA-010404 Transcriptome Capture,Genomics Special Products,RNA Sequencing Analysis,1",
                "XTNL-RNA-010405,RNA-010405 Transcriptome Capture,RNA,RNA-010405 Transcriptome Capture,Genomics Special Products,RNA Sequencing Analysis,1",
                "XTCP-ESH-0001,CP Fragment Integrity QC,Sample Initiation, Qualification & Cell Culture,CP Fragment Integrity QC,Genomics CP,Extraction and Sample Handling,1",
                "XTCP-EXT-0001,CP Extract AllPrep or RNA from FFPE,Sample Initiation, Qualification & Cell Culture,CP Extract DNA From Blood,Genomics CP,Extraction and Sample Handling,1",
                "XTCP-EXT-0002,CP Extract DNA From Blood,Sample Initiation, Qualification & Cell Culture,CP Extract DNA or RNA From Tissue,Genomics CP,Extraction and Sample Handling,1",
                "XTCP-EXT-0003,CP Extract DNA from FFPE,Sample Initiation, Qualification & Cell Culture,CP Extract DNA from FFPE,Genomics CP,Extraction and Sample Handling,1",
                "XTCP-EXT-0004,CP Extract DNA or RNA From Tissue,Sample Initiation, Qualification & Cell Culture,CP Extract AllPrep or RNA from FFPE,Genomics CP,Extraction and Sample Handling,1",
                "XTNL-EXT-010101_1,EXT-010101 DNA Extraction from Blood,Sample Initiation, Qualification & Cell Culture,EXT-010101 Extraction,Genomics Special Products,Extraction and Sample Handling,1",
                "XTNL-EXT-010101_2,EXT-010101 DNA Extraction from Saliva,Sample Initiation, Qualification & Cell Culture,EXT-010101 Extraction,Genomics Special Products,Extraction and Sample Handling,1",
                "XTNL-EXT-010102_1,EXT-010102 DNA Extraction from Buffy Coat,Sample Initiation, Qualification & Cell Culture,EXT-010102 Extraction,Genomics Special Products,Extraction and Sample Handling,1",
                "XTNL-EXT-010102_2,EXT-010102 DNA Extraction from Cell Pellet,Sample Initiation, Qualification & Cell Culture,EXT-010102 Extraction,Genomics Special Products,Extraction and Sample Handling,1",
                "XTNL-EXT-010102_3,EXT-010102 DNA Extraction from Frozen Tissue,Sample Initiation, Qualification & Cell Culture,EXT-010102 Extraction,Genomics Special Products,Extraction and Sample Handling,1",
                "XTNL-EXT-010102_4,EXT-010102 RNA Extraction from PAXGene Blood,Sample Initiation, Qualification & Cell Culture,EXT-010102 Extraction,Genomics Special Products,Extraction and Sample Handling,1",
                "XTNL-EXT-010102_5,EXT-010102 RNA Extraction from Cell Pellet,Sample Initiation, Qualification & Cell Culture,EXT-010102 Extraction,Genomics Special Products,Extraction and Sample Handling,1",
                "XTNL-EXT-010102_6,EXT-010102 RNA Extraction from Frozen Tissue,Sample Initiation, Qualification & Cell Culture,EXT-010102 Extraction,Genomics Special Products,Extraction and Sample Handling,1",
                "XTNL-EXT-010103_1,EXT-010103 DNA Extraction from Blood Spots,Sample Initiation, Qualification & Cell Culture,EXT-010103 Extraction,Genomics Special Products,Extraction and Sample Handling,1",
                "XTNL-EXT-010103_2,EXT-010103 DNA Extraction from FFPE,Sample Initiation, Qualification & Cell Culture,EXT-010103 Extraction,Genomics Special Products,Extraction and Sample Handling,1",
                "XTNL-EXT-010103_3,EXT-010103 RNA Extraction from FFPE,Sample Initiation, Qualification & Cell Culture,EXT-010103 Extraction,Genomics Special Products,Extraction and Sample Handling,1",
                "XTNL-EXT-010103_4,EXT-010103 DNA and RNA Extraction from Cell Pellet,Sample Initiation, Qualification & Cell Culture,EXT-010103 Extraction,Genomics Special Products,Extraction and Sample Handling,1",
                "XTNL-EXT-010103_5,EXT-010103 DNA and RNA Extraction from Frozen Tissue,Sample Initiation, Qualification & Cell Culture,EXT-010103 Extraction,Genomics Special Products,Extraction and Sample Handling,1",
                "XTNL-EXT-010104_1,EXT-010104 DNA and RNA Extraction from FFPE,Sample Initiation, Qualification & Cell Culture,EXT-010104 Extraction,Genomics Special Products,Extraction and Sample Handling,1",
                "XTNL-SAM-010701,SAM-010701 Sample Transfer Into Broad Tubes,Sample Initiation, Qualification & Cell Culture,SAM-010701 Sample Transfer into Broad Tubes,Genomics Special Products,Extraction and Sample Handling,1",
                "XTNL-SAM-010702,SAM-010702 Fragment Integrity QC,Sample Initiation, Qualification & Cell Culture,SAM-010702 Fragment Integrity QC,Genomics Special Products,Extraction and Sample Handling,1",
                "XTNL-SAM-010703,SAM-010703 Stock Tube Return,Sample Initiation, Qualification & Cell Culture,SAM-010703 Stock Tube Return,Genomics Special Products,Extraction and Sample Handling,1",
                "P-VAL-0013,Custom Hybrid Selection_Takeda Myeloid Lymphoid Panel v1,Small Design, Validation & Extension,Custom Lab Work,Genomics Special Products,Materials,1",
                "P-VAL-0014,Custom Hybrid Selection_Takeda LeSabre,Small Design, Validation & Extension,Custom Lab Work,Genomics Special Products,Materials,1",
                "P-VAL-0015,Custom Hybrid Selection_Takeda Myeloid Lymphoid Panel v1 Germline,Small Design, Validation & Extension,Materials,Genomics Special Products,Materials,1",
                "P-VAL-0016,CLIA eMerge Custom Panel v2,Small Design, Validation & Extension,eMerge Custom Panel,CRSP,Custom Sequencing Analysis,1",
                "P-VAL-0017,Custom Hybrid Selection_Takeda LeSabre V3,Small Design, Validation & Extension,Custom Lab Work,Genomics Special Products,Materials,1",
                "P-WG-0054,MKD Genotyping Assay v1,Whole Genome Genotyping,Custom Lab Work,CRSP,Materials,1",
                "XTNL-GEN-011003,GEN-011001 Infinium Global Screening Array Processing,Whole Genome Genotyping,GEN-011001 12 or 24-Sample Array Processing,Genomics Special Products,Whole Genome Arrays (Processing only),1",
                "XTNL-GEN-011004,GEN-011001 Infinium Psych Array Processing,Whole Genome Genotyping,GEN-011001 12 or 24-Sample Array Processing,Genomics Special Products,Whole Genome Arrays (Processing only),1",
                "XTNL-GEN-011005,GEN-011002 Infinium MEGA Array Processing,Whole Genome Genotyping,GEN-011002 8-Sample Array Processing,Genomics Special Products,Whole Genome Arrays (Processing only),1",
                "XTCP-WGS-0001,CP Genome, High Coverage 30X,Whole Genome Sequencing,CP Genome, High Coverage, BAM-Only 30X,Genomics CP,Genome Sequencing Analysis,1",
                "XTCP-WGS-0002,CP Genome, High Coverage 80X,Whole Genome Sequencing,CP Genome, High Coverage, BAM-Only 80X,Genomics CP,Genome Sequencing Analysis,1",
                "XTCP-WGS-0003,CP Genome, Low Coverage 15X,Whole Genome Sequencing,CP Genome, Low Coverage 15X,Genomics CP,Genome Sequencing Analysis,1",
                "XTCP-WGS-0004,CP Genome, High Coverage 60X,Whole Genome Sequencing,CP Genome, High Coverage, BAM-Only 60X,Genomics CP,Genome Sequencing Analysis,1",
                "XTCP-WGS-0005,CP Genome (PCR Free), High Coverage 30X,Whole Genome Sequencing,CP Genome, High Coverage, BAM-Only 30X,Genomics CP,Genome Sequencing Analysis,1",
                "XTCP-WGS-0006,CP Genome (PCR Free), High Coverage 60X,Whole Genome Sequencing,CP Genome, High Coverage, BAM-Only 60X,Genomics CP,Genome Sequencing Analysis,1",
                "XTCP-WGS-0007,CP Genome (PCR Free), High Coverage 80X,Whole Genome Sequencing,CP Genome, High Coverage, BAM-Only 80X,Genomics CP,Genome Sequencing Analysis,1",
                "XTCP-WGS-0008,CP Genome (PCR Free), Low Coverage 15X,Whole Genome Sequencing,CP Genome, Low Coverage 15X,Genomics CP,Genome Sequencing Analysis,1",
                "XTNL-WGS-010300,WGS-010300 Genome, PCR-Free,Whole Genome Sequencing,WGS-010300 Genome, PCR-Free,Genomics Special Products,Genome Sequencing Analysis,1",
                "XTNL-WGS-010301,WGS-010301 Genome, PCR-Free,Whole Genome Sequencing,WGS-010301 Genome, PCR-Free,Genomics Special Products,Genome Sequencing Analysis,1",
                "XTNL-WGS-010302,WGS-010302 Genome, PCR-Free,Whole Genome Sequencing,WGS-010302 Genome, PCR-Free,Genomics Special Products,Genome Sequencing Analysis,1",
                "XTNL-WGS-010303,WGS-010303 Genome, PCR-Free,Whole Genome Sequencing,WGS-010303 Genome, PCR-Free,Genomics Special Products,Genome Sequencing Analysis,1",
                "XTNL-WGS-010304,WGS-010304 Genome, PCR-Free, 60X,Whole Genome Sequencing,WGS-010304 Genome, PCR-Free, 60X,Genomics Special Products,Genome Sequencing Analysis,1",
                "XTNL-WGS-010306,WGS-010306 Genome, PCR Plus, 60X,Whole Genome Sequencing,WGS-010306 Genome, PCR Plus, 60X,Genomics Special Products,Genome Sequencing Analysis,1",
                "XTNL-WGS-010307,WGS-010307 Genome, PCR Plus,Whole Genome Sequencing,WGS-010307 Genome, PCR Plus,Genomics Special Products,Genome Sequencing Analysis,1",
                "XTNL-WGS-010308,WGS-010308 Genome, PCR Plus,Whole Genome Sequencing,WGS-010308 Genome, PCR Plus,Genomics Special Products,Genome Sequencing Analysis,1",
                "XTNL-WGS-010309,WGS-010309 Genome, PCR Plus,Whole Genome Sequencing,WGS-010309 Genome, PCR Plus,Genomics Special Products,Genome Sequencing Analysis,1",
                "XTNL-WGS-010310,WGS-010310 Genome, PCR Plus,Whole Genome Sequencing,WGS-010310 Genome, PCR Plus,Genomics Special Products,Genome Sequencing Analysis,1",
                "XTNL-WGS-010311,WGS-010311 Genome, PCR-Free, 60X,Whole Genome Sequencing,WGS-010311 Genome, PCR-Free, 60X,Genomics Special Products,Genome Sequencing Analysis,1",
                "XTNL-WGS-010312,WGS-010312 Genome, PCR-Free, 60X,Whole Genome Sequencing,WGS-010312 Genome, PCR-Free, 60X,Genomics Special Products,Genome Sequencing Analysis,1",
                "XTNL-WGS-010313,WGS-010313 Genome, PCR-Free, 60X,Whole Genome Sequencing,WGS-010313 Genome, PCR-Free, 60X,Genomics Special Products,Genome Sequencing Analysis,1",
                "XTNL-WGS-010314,WGS-010314 Genome, PCR Plus, 60X,Whole Genome Sequencing,WGS-010314 Genome, PCR Plus, 60X,Genomics Special Products,Genome Sequencing Analysis,1",
                "XTNL-WGS-010315,WGS-010315 Genome, PCR Plus, 60X,Whole Genome Sequencing,WGS-010315 Genome, PCR Plus, 60X,Genomics Special Products,Genome Sequencing Analysis,1",
                "XTNL-WGS-010316,WGS-010316 Genome, PCR Plus, 60X,Whole Genome Sequencing,WGS-010316 Genome, PCR Plus, 60X,Genomics Special Products,Genome Sequencing Analysis,1",
                "XTNL-WGS-010317,WGS-010317 Genome, Unaligned, PCR-Free,Whole Genome Sequencing,WGS-010303 Genome, PCR-Free,Genomics Special Products,Genome Sequencing Analysis,1");

        Integer testPrice = new Integer(1000);
        for (String productDump : initialTest) {
            String[] dividedProductInfo = StringUtils.split(productDump);
            SAPMaterial initialMaterial = new SAPMaterial(dividedProductInfo[0],testPrice.toString(), Collections.<Condition, BigDecimal>emptyMap(), Collections.<DeliveryCondition, BigDecimal>emptyMap());
            testPrice += 10;
            sapMaterials.add(initialMaterial);
        }

        return sapMaterials;
    }

    @Override
    public OrderCalculatedValues calculateOpenOrderValues(int addedSampleCount, String quoteId,
                                                          ProductOrder productOrder) throws SAPIntegrationException {
        return new OrderCalculatedValues(BigDecimal.ONE, Collections.<OrderValue>emptySet());
    }

    @Override
    public Quote findSapQuote(String sapQuoteId) throws SAPIntegrationException {
        return null;
    }
}
