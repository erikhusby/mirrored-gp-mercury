package org.broadinstitute.gpinformatics.mercury.presentation.sample;

import clover.org.apache.commons.lang3.ArrayUtils;
import htsjdk.samtools.Defaults;
import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.reference.ReferenceSequenceFile;
import htsjdk.samtools.reference.ReferenceSequenceFileFactory;
import htsjdk.samtools.util.CloseableIterator;
import htsjdk.samtools.util.IOUtil;
import htsjdk.samtools.util.SequenceUtil;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.variantcontext.writer.Options;
import htsjdk.variant.variantcontext.writer.VariantContextWriter;
import htsjdk.variant.variantcontext.writer.VariantContextWriterBuilder;
import htsjdk.variant.vcf.VCFFileReader;
import htsjdk.variant.vcf.VCFHeader;
import htsjdk.variant.vcf.VCFHeaderLine;
import htsjdk.variant.vcf.VCFUtils;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.StreamingResolution;
import net.sourceforge.stripes.action.UrlBinding;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.broadinstitute.gpinformatics.infrastructure.spreadsheet.SpreadsheetCreator;
import org.broadinstitute.gpinformatics.infrastructure.spreadsheet.StreamCreatedSpreadsheetUtil;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.SnpListDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.run.FingerprintEjb;
import org.broadinstitute.gpinformatics.mercury.control.run.GenotypeVCFGenerator;
import org.broadinstitute.gpinformatics.mercury.entity.run.Fingerprint;
import org.broadinstitute.gpinformatics.mercury.entity.run.FpGenotype;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.jetbrains.annotations.NotNull;


import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

@UrlBinding(value = FingerprintGenotypeActionBean.ACTIONBEAN_URL_BININDING)
public class FingerprintGenotypeActionBean extends CoreActionBean {

    public static final String ACTIONBEAN_URL_BININDING = "/sample/fingerprint_genotype.action";
    public static final String VIEW_PAGE = "/sample/fingerprint_genotype.jsp";
    private static Boolean CREATE_INDEX = Defaults.CREATE_INDEX;

    @Inject
    private MercurySampleDao mercurySampleDao;

    @Inject
    private FingerprintEjb fingerprintEJB;

    @Inject
    private SnpListDao snpListDao;


    private static String sampleId;
    private boolean showLayout = false;
    private Map<String, MercurySample> mapSmidToMercurySample = new HashMap<>();
    private List<Fingerprint> fingerprints = new ArrayList<>();
    private List<Fingerprint> enteredFps = new ArrayList<>();

    // These items will be removed from the merged header (DSDEGP-1087)
    private final Set<String> sampleSpecificHeaders = new HashSet<String>(Arrays.asList(
            "Biotin(Bgnd)", "Biotin(High)", "expectedGender", "fingerprintGender", "sampleAlias",
            "DNP(Bgnd)", "DNP(High)", "Extension(A)", "Extension(C)", "Extension(G)", "Extension(T)",
            "Hyb(High)", "Hyb(Low)", "Hyb(Medium)", "NP(A)", "NP(C)", "NP(G)", "NP(T)",
            "NSB(Bgnd)Blue", "NSB(Bgnd)Green", "NSB(Bgnd)Purple", "NSB(Bgnd)Red", "Restore",
            "String(MM)", "String(PM)", "TargetRemoval", "p95Green", "p95Red", "imagingDate",
            "fileDate", "autocallDate", "autocallGender", "chipWellBarcode", "analysisVersionNumber", "scannerName"));



    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        return new ForwardResolution(VIEW_PAGE);
    }

    /**
     * Validate search input and returns page with sample details and genotype
     */
    @HandlesEvent("search")
    public Resolution search() {
        int smidLimit = 200;

        try{
            if (sampleId == null) {
                addGlobalValidationError("You must input a valid search term.");
            } else {
                List<String> sampleIds = getSmIds();
                if (StringUtils.isNotBlank(sampleId)
                        && mercurySampleDao.findBySampleKeys(sampleIds).size() == 0) {
                    addGlobalValidationError("There were no matching Sample Ids for " + "'" + sampleId + "'.");
                } else {
                    if (StringUtils.isNotBlank(sampleId)) {
                        mapSmidToMercurySample =
                                mercurySampleDao.findMapIdToMercurySample(sampleIds);
                    }
                    if (mapSmidToMercurySample.values().isEmpty()) {
                        throw new RuntimeException("No samples found.");
                    } else {
                        fingerprints = fingerprintEJB.findFingerprints(mapSmidToMercurySample);
                        enteredFps = fingerprints.stream()
                                .filter(fp -> sampleIds.contains(fp.getMercurySample().getSampleKey()))
                                .sorted(new Fingerprint.OrderFpPtidRootSamp()).collect(Collectors.toList());
                    }
                    if (enteredFps.size() > smidLimit) {
                        addGlobalValidationError("Over " + smidLimit + " SM-IDs have been selected");
//                    } else if (enteredFps.size() < 2) {
//                        addGlobalValidationError("At least " + 2 + " SM-IDs must be selected");
                    } else {
                        showLayout = true;
                    }
                }
            }
        } catch (Exception e) {
            addGlobalValidationError(e.getMessage());
            return new ForwardResolution(VIEW_PAGE);
        }

        return new ForwardResolution(VIEW_PAGE);
    }

    @NotNull
    private static List<String> getSmIds() {
        return Arrays.asList(sampleId.split("\\s+"));
    }

    @HandlesEvent("downloadExcel")
    public Resolution downloadExcel() throws IOException {

        List<String> sampleIds = getSmIds();
        mapSmidToMercurySample =
                mercurySampleDao.findMapIdToMercurySample(sampleIds);
        fingerprints = fingerprintEJB.findFingerprints(mapSmidToMercurySample);
        enteredFps = fingerprints.stream()
                .filter(fp -> sampleIds.contains(fp.getMercurySample().getSampleKey()))
                .sorted(new Fingerprint.OrderFpPtidRootSamp()).collect(Collectors.toList());

    Workbook workbook = makeSpreadsheet(enteredFps);

            String filename = "";
                if (sampleId != null) {
                filename = sampleId.substring(0, 8) + "_" + formatDate(new Date()) + "_FP_REPORT" + ".xlsx";
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
                workbook.write(out);
            StreamingResolution stream = new StreamingResolution(StreamCreatedSpreadsheetUtil.XLS_MIME_TYPE,
                    new ByteArrayInputStream(out.toByteArray()));
                stream.setFilename(filename);


            return stream;
    }

    @HandlesEvent("downloadVCF")
    public Resolution downloadVCF() throws IOException {

        List<GenotypeVCFGenerator.SnpGenotype> genotypes;
        List<GenotypeVCFGenerator.SnpGenotype> cleanGts;
        SortedSet<VariantContext> variantContexts = null;
        List<String> sampleIds = getSmIds();

        mapSmidToMercurySample =
                mercurySampleDao.findMapIdToMercurySample(sampleIds);
        fingerprints = fingerprintEJB.findFingerprints(mapSmidToMercurySample);
        enteredFps = fingerprints.stream()
                .filter(fp -> sampleIds.contains(fp.getMercurySample().getSampleKey()))
                .sorted(new Fingerprint.OrderFpPtidRootSamp()).collect(Collectors.toList());

        GenotypeVCFGenerator.HaplotypeMap haplotypes = new GenotypeVCFGenerator.HaplotypeMap(new File("/Volumes/seq_references/Homo_sapiens_assembly19/v1/Homo_sapiens_assembly19.haplotype_database.txt"));

        for (Fingerprint enteredFp : enteredFps) {
            genotypes = GenotypeVCFGenerator.gapResultsToGenotypes(enteredFp, haplotypes);
            try (ReferenceSequenceFile rsf = ReferenceSequenceFileFactory.getReferenceSequenceFile(
                    new File("/Volumes/seq_references/Homo_sapiens_assembly19/v1/Homo_sapiens_assembly19.fasta"))) {
                SequenceUtil.assertSequenceDictionariesEqual(rsf.getSequenceDictionary(),
                        haplotypes.getHeader().getSequenceDictionary());

                cleanGts = GenotypeVCFGenerator.cleanupGenotypes(genotypes, haplotypes);
                variantContexts = GenotypeVCFGenerator.makeVariantContexts(cleanGts, haplotypes, rsf, enteredFp);
                GenotypeVCFGenerator.writeVcf(variantContexts, rsf.getSequenceDictionary(), enteredFp);
            } catch (IOException e) {
                throw new IOException("There was an error handling the reference sequence file", e);
            }
        }

//
//        File out = new File("/Users/cnolet/Desktop/untitled folder/single vcf");
//        final List<File> UNROLLED_INPUT = IOUtil.unrollFiles(Arrays.asList(out.listFiles()), IOUtil.VCF_EXTENSIONS);for (final File f: UNROLLED_INPUT) IOUtil.assertFileIsReadable(f);
//
//        final SAMSequenceDictionary sequenceDictionary = VCFFileReader.getSequenceDictionary(UNROLLED_INPUT.get(0));
//
//        final List<String> sampleList = new ArrayList<String>();
//        final Collection<CloseableIterator<VariantContext>> iteratorCollection = new ArrayList<>(UNROLLED_INPUT.size());
//        final Collection<VCFHeader> headers = new HashSet<VCFHeader>(UNROLLED_INPUT.size());
//
//        Set<String> sampleNames = new HashSet<>();
//
//        for (final File file : UNROLLED_INPUT) {
//            final VCFFileReader fileReader = new VCFFileReader(file, false);
//            final VCFHeader fileHeader = fileReader.getFileHeader();
//
//            for (final String sampleName : fileHeader.getSampleNamesInOrder()) {
//                if (!sampleNames.add(sampleName)) {
//                    throw new IllegalArgumentException("Input file " + file.getAbsolutePath() + " contains a sample entry (" + sampleName + ") that appears in another input file.");
//                }
//                sampleList.add(sampleName);
//            }
//
//            headers.add(fileHeader);
//            iteratorCollection.add(fileReader.iterator());
//        }
//
//        if (CREATE_INDEX && sequenceDictionary == null) {
//            throw new IOException("A sequence dictionary must be available (either through the input file or by setting it explicitly) when creating indexed output.");
//        }
//
//        final VariantContextWriterBuilder builder = new VariantContextWriterBuilder()
//                .setOutputFile("/Users/cnolet/Desktop/untitled folder/" + "combined_" + getSampleId() + new Date() + ".vcf")
//                .setReferenceDictionary(sequenceDictionary);
//        if (CREATE_INDEX) {
//            builder.setOption(Options.INDEX_ON_THE_FLY);
//        }
//        final VariantContextWriter writer = builder.build();
//
//        Set<VCFHeaderLine> headerLines = VCFUtils.smartMergeHeaders(headers, false);
//        headerLines.removeIf(line -> sampleSpecificHeaders.contains(line.getKey()));
//        writer.writeHeader(new VCFHeader(headerLines, sampleList));
//
//        int closedIteratorCount = 0;
//        while (closedIteratorCount == 0) {
////            List<VariantContext> variantContexts = new ArrayList<>();
//            for (final CloseableIterator<VariantContext> iterator: iteratorCollection) {
//                if (iterator.hasNext()) {
//                    variantContexts.add(iterator.next());
//                } else {
//                    closedIteratorCount++;
//                }
//            }
//            if (closedIteratorCount == 0) {
////                progressLogger.record(variantContexts.get(0).getContig(), variantContexts.get(0).getStart());
//                writer.add(GenotypeVCFGenerator.merge(variantContexts));
//            }
//        }
//        if (closedIteratorCount != iteratorCollection.size()) {
//            throw new IOException("Mismatch in number of variants among input VCFs");
//        }
//        writer.close();



        return new ForwardResolution(VIEW_PAGE);
    }

    private Workbook makeSpreadsheet(List<Fingerprint> fingerprints) {
        Map<String, Object[][]> sheets = new HashMap<>();

        int numFingerprintCells = fingerprints.size() + 1;
        int rowIndex = 0;

        String[][] fingerprintCells = new String[numFingerprintCells][];

        List<String> fluiRsIds = new ArrayList<>();

        for (Fingerprint fingerprint : fingerprints) {
            for (FpGenotype geno : fingerprint.getFpGenotypesOrdered()) {
                if (geno != null) {
                    String rsId = geno.getSnp().getRsId();
                    if (!fluiRsIds.contains(rsId))
                    fluiRsIds.add(rsId);
                }
            }
        }

        fingerprintCells[rowIndex] =
                new String[]{"Participant Id", "Root Sample", "Fingerprint Aliquot", "Date", "Platform", "Pass/Fail", "Genome Build"};

        String[] rsIdArray;
        rsIdArray = fluiRsIds.toArray(new String[0]);

        fingerprintCells[rowIndex] = ArrayUtils.addAll(fingerprintCells[rowIndex], rsIdArray);

        rowIndex++;

        for (Fingerprint fingerprint : fingerprints) {
            String[] snps = new String[fluiRsIds.size()];
            for (FpGenotype geno : fingerprint.getFpGenotypesOrdered()) {
                if (geno != null) {
                    String snp = geno.getGenotype();
                    String rsId = geno.getSnp().getRsId();
                    if (fluiRsIds.contains(rsId)) {
                        int indexOf = fluiRsIds.indexOf(rsId);
                        snps[indexOf] = snp;
                    }
                }
            }

            fingerprintCells[rowIndex] =
                    new String[]{fingerprint.getMercurySample().getSampleData().getPatientId(),
                            fingerprint.getMercurySample().getSampleData().getRootSample(),
                            fingerprint.getMercurySample().getSampleKey(),
                            formatDate(fingerprint.getDateGenerated()), fingerprint.getPlatform().name(),
                            fingerprint.getDisposition().name(), fingerprint.getGenomeBuild().name()};

            fingerprintCells[rowIndex] = ArrayUtils.addAll(fingerprintCells[rowIndex], snps);
            ++rowIndex;
        }

        String[] sheetNames = {"Fingerprints"};
        sheets.put(sheetNames[0], fingerprintCells);

        return SpreadsheetCreator.createSpreadsheet(sheets, SpreadsheetCreator.Type.XLSX);
    }

    public String formatDate(Date date) {
        return DateUtils.getDate(date);
    }

    public List<Fingerprint> getFingerprints() {
        return fingerprints;
    }

    public String getSampleId() {
        return sampleId;
    }

    public void setSampleId(String sampleId) {
        this.sampleId = sampleId.toUpperCase();
    }

    public boolean isShowLayout() {
        return showLayout;
    }

    public void setShowLayout(boolean showLayout) {
        this.showLayout = showLayout;
    }

    public List<Fingerprint> getEnteredFps() {
        return enteredFps;
    }
}
