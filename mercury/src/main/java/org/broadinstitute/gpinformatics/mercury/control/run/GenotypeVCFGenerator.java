package org.broadinstitute.gpinformatics.mercury.control.run;

import htsjdk.samtools.Defaults;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.SAMTextHeaderCodec;
import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import htsjdk.samtools.reference.ReferenceSequence;
import htsjdk.samtools.reference.ReferenceSequenceFile;
import htsjdk.samtools.util.CollectionUtil;
import htsjdk.samtools.util.FormatUtil;
import htsjdk.samtools.util.IOUtil;
import htsjdk.samtools.util.Interval;
import htsjdk.samtools.util.IntervalList;
import htsjdk.samtools.util.ListMap;
import htsjdk.samtools.util.Log;
import htsjdk.samtools.util.StringLineReader;
import htsjdk.samtools.util.StringUtil;
import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.CommonInfo;
import htsjdk.variant.variantcontext.Genotype;
import htsjdk.variant.variantcontext.GenotypeBuilder;
import htsjdk.variant.variantcontext.GenotypesContext;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.variantcontext.VariantContextBuilder;
import htsjdk.variant.variantcontext.VariantContextUtils;
import htsjdk.variant.variantcontext.writer.Options;
import htsjdk.variant.variantcontext.writer.VariantContextWriter;
import htsjdk.variant.variantcontext.writer.VariantContextWriterBuilder;
import htsjdk.variant.vcf.VCFConstants;
import htsjdk.variant.vcf.VCFFileReader;
import htsjdk.variant.vcf.VCFFormatHeaderLine;
import htsjdk.variant.vcf.VCFHeader;
import htsjdk.variant.vcf.VCFHeaderLine;
import htsjdk.variant.vcf.VCFHeaderLineCount;
import htsjdk.variant.vcf.VCFHeaderLineType;
import htsjdk.variant.vcf.VCFHeaderVersion;
import htsjdk.variant.vcf.VCFInfoHeaderLine;
import htsjdk.variant.vcf.VCFStandardHeaderLines;
import htsjdk.variant.vcf.VCFUtils;
import org.broadinstitute.gpinformatics.mercury.entity.run.Fingerprint;
import org.broadinstitute.gpinformatics.mercury.entity.run.FpGenotype;
import org.broadinstitute.gpinformatics.mercury.entity.run.Snp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;


public class GenotypeVCFGenerator {
    private static final Log log = Log.getInstance(GenotypeVCFGenerator.class);
    private static Boolean CREATE_INDEX = Defaults.CREATE_INDEX;



    public static List<SnpGenotype> gapResultsToGenotypes(final Fingerprint gapResults,
                                                          final HaplotypeMap haplotypes) {

        final List<SnpGenotype> genotypes = new ArrayList<>(100);

            for (FpGenotype result : gapResults.getFpGenotypesOrdered()) {
                if (result == null){
                    continue;
                }
                final HaplotypeBlock haplotype = haplotypes.getHaplotype(result.getSnp().getRsId());
//                final HaplotypeBlock haplotype = haplotypes.getHaplotype(result.getGenotype());
                if (haplotype == null) {
                log.warn("GAP returned a genotype for unknown SNP: " + result.getSnp().getRsId());
                    continue;
                }

                final PicardSnp snp = haplotype.getSnp(result.getSnp().getRsId());
                try {
                    genotypes.add(new SnpGenotype(snp, DiploidGenotype.fromBases(result.getGenotype().getBytes())));
                } catch (IllegalArgumentException e) {
                log.warn("Excluding following genotype: " + result);
                }
            }

        return genotypes;
    }


    /** Examines replicates and proxies to ensure that genotype data is consistent */
    public static List<SnpGenotype> cleanupGenotypes(final List<SnpGenotype> genotypes, final HaplotypeMap haplotypes) {

        final ListMap<HaplotypeBlock, SnpGenotype> genotypesByHaplotype = new ListMap<>();
        genotypes.forEach(gt -> genotypesByHaplotype.add(haplotypes.getHaplotype(gt.snp.name), gt));

        final List<SnpGenotype> cleanedGenotypes = new ArrayList<>();
        genotypesByHaplotype.forEach((h, gts) -> {
            final Set<DiploidHaplotype> hs = gts.stream()
                    .map(gt -> h.getDiploidHaplotype(gt.snp, gt.genotype))
                    .collect(Collectors.toSet());

            if (hs.size() > 1) {
                log.warn("Conflicting genotypes for haplotype " + h + ", " + gts);
            } else {
            cleanedGenotypes.addAll(gts);
            }
        });

        return cleanedGenotypes;
    }

    /**
     * Convert the genotypes to VariantContext objects and sort them prior to writing them
     * out to a VCF file.
     */
    public static SortedSet<VariantContext> makeVariantContexts(final List<SnpGenotype> genotypes,
                                                                final HaplotypeMap haplotypes,
                                                                final ReferenceSequenceFile ref,
                                                                Fingerprint enteredFp) {

        // Setup a TreeSet that will sort contexts properly and squish out duplicate genotypes!
        final TreeSet<VariantContext> contexts = new VariantContextSet(
                haplotypes.getHeader().getSequenceDictionary());
        Map<String, ?> attributesMap = new HashMap<>();

        genotypes.forEach(gt -> {
            final byte refAllele = StringUtil.toUpperCase(
                    ref.getSubsequenceAt(gt.snp.getChrom(), gt.snp.getPos(), gt.snp.getPos()).getBases()[0]);

            final Allele allele1 = Allele.create(gt.snp.getAllele1(), gt.snp.getAllele1() == refAllele);
            final Allele allele2 = Allele.create(gt.snp.getAllele2(), gt.snp.getAllele2() == refAllele);
            final List<Allele> alleles = Arrays.asList(allele1, allele2);

            final List<Allele> sampleAlleles;
            if (gt.genotype.isHeterozygous()){
                sampleAlleles = alleles;
            } else if (gt.genotype.getAllele1() == allele1.getBases()[0]) {
                sampleAlleles = Arrays.asList(allele1, allele1);
            } else {
                sampleAlleles = Arrays.asList(allele2, allele2);
            }

            final VariantContextBuilder builder = new VariantContextBuilder(
                    gt.snp.getName(), gt.snp.getChrom(), gt.snp.getPos(), gt.snp.getPos(), alleles);

            builder.log10PError(VariantContext.NO_LOG10_PERROR);
            builder.id(gt.snp.getName());
            builder.genotypes(GenotypeBuilder.create(enteredFp.getMercurySample().getSampleKey(), sampleAlleles));
            builder.passFilters();
            builder.attributes(attributesMap);
            builder.putAttributes(attributesMap);
//            builder.attribute(enteredFp.getMercurySample().getSampleKey(), attributesMap.values());


            contexts.add(builder.make());
        });

        return contexts;
    }

    /**
     * Writes out the VariantContext objects in the order presented to the
     * supplied output file in VCF format.
     */
    public static void writeVcf(final SortedSet<VariantContext> variants,
                                final SAMSequenceDictionary dict,
                                Fingerprint enteredFp){

        log.info("Writing out " + variants.size() + " final genotypes");

        final Set<VCFHeaderLine> lines = new LinkedHashSet<>();
        lines.add(new VCFHeaderLine("reference", "/Volumes/seq_references/Homo_sapiens_assembly19/v1/Homo_sapiens_assembly19.fasta"));
        lines.add(new VCFHeaderLine("source", "Genotypes by Picard v" +  " DownloadGenotypes"));
//        lines.add(new VCFHeaderLine("source", getCommandLine()));
        lines.add(new VCFHeaderLine("fileDate", new Date().toString()));
        lines.add(VCFStandardHeaderLines.getFormatLine(VCFConstants.GENOTYPE_KEY));
        lines.add(new VCFHeaderLine("gender", enteredFp.getGender().getAbbreviation()));

        final VariantContextWriterBuilder builder = new VariantContextWriterBuilder();
//        if (OUTPUT_PATH == null) {
            builder.setOutputFile("/Users/cnolet/Desktop/untitled folder/" + enteredFp.getMercurySample().getSampleKey() + "_" + new Date() + ".vcf");
//        } else {
//            builder.setOutputPath(OUTPUT_PATH);
//        }

        builder.setReferenceDictionary(dict)
                .modifyOption(Options.INDEX_ON_THE_FLY, CREATE_INDEX);


        try (final VariantContextWriter out = builder.build()) {
            final VCFHeader header = new VCFHeader(lines, Collections.singleton(enteredFp.getMercurySample().getSampleKey()));
            header.setSequenceDictionary(dict);
            out.writeHeader(header);
            variants.forEach(out::add);
        }
    }


    /**
     * Merges multiple VariantContexts all for the same locus into a single hybrid.
     *
     * @param variantContexts           list of VCs
     * @return new VariantContext       representing the merge of variantContexts
     */
    public static VariantContext merge(final SortedSet<VariantContext> variantContexts) throws IOException {
        if ( variantContexts == null || variantContexts.isEmpty() )
            return null;

        // establish the baseline info from the first VC
        final VariantContext first = variantContexts.first();
        final String name = first.getSource();

        final Set<String> filters = new HashSet<>();

        int depth = 0;
        double log10PError = CommonInfo.NO_LOG10_PERROR;
        boolean anyVCHadFiltersApplied = false;
        GenotypesContext genotypes = GenotypesContext.create();

        // Go through all the VCs, verify that the loci and ID and other attributes agree.
        final Map<String, Object> firstAttributes = first.getAttributes();
        for (final VariantContext vc : variantContexts ) {
            if ((vc.getStart() != first.getStart()) || (!vc.getContig().equals(first.getContig()))) {
                throw new IOException("Mismatch in loci among input VCFs");
            }
            if (!vc.getID().equals(first.getID())) {
                throw new IOException("Mismatch in ID field among input VCFs");
            }
            if (!vc.getReference().equals(first.getReference())) {
                throw new IOException("Mismatch in REF allele among input VCFs");
            }
            checkThatAllelesMatch(vc, first);

            genotypes.addAll(vc.getGenotypes());

            // We always take the QUAL of the first VC with a non-MISSING qual for the combined value
            if ( log10PError == CommonInfo.NO_LOG10_PERROR )
                log10PError =  vc.getLog10PError();

            filters.addAll(vc.getFilters());
            anyVCHadFiltersApplied |= vc.filtersWereApplied();

            // add attributes
            // special case DP (add it up)
            if (vc.hasAttribute(VCFConstants.DEPTH_KEY))
                depth += vc.getAttributeAsInt(VCFConstants.DEPTH_KEY, 0);

            // Go through all attributes - if any differ (other than AC, AF, or AN) that's an error.  (recalc AC, AF, and AN later)
            for (final Map.Entry<String, Object> p : vc.getAttributes().entrySet()) {
                final String key = p.getKey();
                if ((!key.equals("AC")) && (!key.equals("AF")) && (!key.equals("AN")) && (!key.equals("devX_AB")) && (!key.equals("devY_AB"))) {
                    final Object value = p.getValue();
                    final Object extantValue = firstAttributes.get(key);
                    if (extantValue == null) {
                        // attribute in one VCF but not another.  Die!
                        throw new IOException("Attribute '" + key + "' not found in all VCFs");
                    }
                    else if (!extantValue.equals(value)) {
                        // Attribute disagrees in value between one VCF Die! (if not AC, AF, nor AN, skipped above)
                        throw new IOException("Values for attribute '" + key + "' disagrees among input VCFs");
                    }
                }
            }
        }

        if (depth > 0)
            firstAttributes.put(VCFConstants.DEPTH_KEY, String.valueOf(depth));

        final VariantContextBuilder builder = new VariantContextBuilder().source(name).id(first.getID());
        builder.loc(first.getContig(), first.getStart(), first.getEnd());
        builder.alleles(first.getAlleles());
        builder.genotypes(genotypes);

        builder.attributes(new TreeMap<>(firstAttributes));
        // AC, AF, and AN are recalculated here
        VariantContextUtils.calculateChromosomeCounts(builder, false);

        builder.log10PError(log10PError);
        if ( anyVCHadFiltersApplied ) {
            builder.filters(filters.isEmpty() ? filters : new TreeSet<>(filters));
        }

        return builder.make();
    }

    private static void checkThatAllelesMatch(final VariantContext vc1, final VariantContext vc2) throws IOException {
        if (!vc1.getReference().equals(vc2.getReference())) {
            throw new IOException("Mismatch in REF allele among input VCFs");
        }
        if (vc1.getAlternateAlleles().size() != vc2.getAlternateAlleles().size()) {
            throw new IOException("Mismatch in ALT allele count among input VCFs");
        }
        for (int i = 0; i < vc1.getAlternateAlleles().size(); i++) {
            if (!vc1.getAlternateAllele(i).equals(vc2.getAlternateAllele(i))) {
                throw new IOException("Mismatch in ALT allele among input VCFs for " + vc1.getContig() + "." + vc1.getStart());
            }
        }
    }


    public static class SnpGenotype {
        final PicardSnp snp;
        final DiploidGenotype genotype;

        SnpGenotype(final PicardSnp snp, final DiploidGenotype genotype) {
            this.snp = snp;
            this.genotype = genotype;
        }


        // For comparisons in unit tests.
        @Override
        public boolean equals(Object that) {
            boolean result = false;
            if (that instanceof SnpGenotype) {
                final SnpGenotype thatGenotype = (SnpGenotype)that;
                result = (snp.equals(thatGenotype.snp) && genotype.equals(thatGenotype.genotype));
            }
            return result;
        }
    }


    public static class HaplotypeMap {
        public static final String HET_GENOTYPE_FOR_PHASING = "HetGenotypeForPhasing";
        public static final String SYNTHETIC_PHASESET_PREFIX = "Synthetic";
        public static final String PHASESET_PREFIX = "PhaseSet";

        private final List<HaplotypeBlock> haplotypeBlocks = new ArrayList<>();
        private final Map<PicardSnp, HaplotypeBlock> haplotypesBySnp = new HashMap<>();
        private final Map<String, HaplotypeBlock> haplotypesBySnpName = new HashMap<>();
        private final Map<String, HaplotypeBlock> haplotypesBySnpLocus = new HashMap<>();
        private final Map<String,PicardSnp> snpsByPosition = new HashMap<>();
        private IntervalList intervals;
        private SAMFileHeader header;

        /**
         * Constructs a HaplotypeMap from the provided file.
         */

        private void fromVcf(final File file) {
            try ( final VCFFileReader reader = new VCFFileReader(file, false)) {

                final SAMSequenceDictionary dict = reader.getFileHeader().getSequenceDictionary();

                if (dict == null || dict.getSequences().isEmpty()) {
                    throw new IllegalStateException("Haplotype map VCF file must contain header: " + file.getAbsolutePath());
                }

                initialize(new SAMFileHeader(dict));

                final Map<String, HaplotypeBlock> anchorToHaplotype = new HashMap<>();

                for (final VariantContext vc : reader) {

                    if (vc.getNSamples() > 1) {
                        throw new IllegalStateException("Haplotype map VCF file must contain at most one sample: " + file.getAbsolutePath());
                    }

                    final Genotype gc = vc.getGenotype(0); // may be null
                    final boolean hasGc = gc != null;

                    if (vc.getAlternateAlleles().size() != 1) {
                        throw new IllegalStateException("Haplotype map VCF file must contain exactly one alternate allele per site: " + vc.toString());
                    }

                    if (!vc.isSNP()) {
                        throw new IllegalStateException("Haplotype map VCF file must contain only SNPs: " + vc.toString());
                    }

                    if (!vc.hasAttribute(VCFConstants.ALLELE_FREQUENCY_KEY)) {
                        throw new IllegalStateException("Haplotype map VCF Variants must have an '"+ VCFConstants.ALLELE_FREQUENCY_KEY + "' INFO field: " + vc.toString());
                    }


                    if (hasGc && gc.isPhased() && !gc.hasExtendedAttribute(VCFConstants.PHASE_SET_KEY)) {
                        throw new IllegalStateException("Haplotype map VCF Variants' genotypes that are phased must have a PhaseSet (" + VCFConstants.PHASE_SET_KEY+")" + vc.toString());
                    }

                    if (hasGc && gc.isPhased() && !gc.isHet()) {
                        throw new IllegalStateException("Haplotype map VCF Variants' genotypes that are phased must be HET" + vc.toString());
                    }

                    // Then parse them out
                    final String chrom = vc.getContig();
                    final int pos = vc.getStart();
                    final String name = vc.getID();

                    final byte ref = vc.getReference().getBases()[0];
                    final byte var = vc.getAlternateAllele(0).getBases()[0];

                    final double temp_maf = vc.getAttributeAsDouble(VCFConstants.ALLELE_FREQUENCY_KEY, 0D);
                    final boolean swapped = hasGc && !gc.getAllele(0).equals(vc.getReference());

                    final byte major, minor;
                    final double maf;

                    if (swapped) {
                        major = var;
                        minor = ref;
                        maf = 1 - temp_maf;
                    } else {
                        major = ref;
                        minor = var;
                        maf = temp_maf;
                    }

                    final String anchor = anchorFromVc(vc);

                    // If it's the anchor snp, start the haplotype
                    if (!anchorToHaplotype.containsKey(anchor)) {
                        final HaplotypeBlock newBlock = new HaplotypeBlock(maf);
                        anchorToHaplotype.put(anchor, newBlock);
                    }
                    final HaplotypeBlock block = anchorToHaplotype.get(anchor);
                    block.addSnp(new PicardSnp(name, chrom, pos, major, minor, maf, null));
                }

                // And add them all now that they are all ready.
                fromHaplotypes(anchorToHaplotype.values());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void fromHaplotypes(final Collection<HaplotypeBlock> haplotypes){
            haplotypes.forEach(this::addHaplotype);
        }

        private String anchorFromVc(final VariantContext vc) {
            final Genotype genotype = vc.getGenotype(0);

            if (genotype == null || !genotype.hasExtendedAttribute(VCFConstants.PHASE_SET_KEY)) {
                return SYNTHETIC_PHASESET_PREFIX + "_" + vc.getContig() + "_" + vc.getStart();
            } else {
                return PHASESET_PREFIX + "_" + vc.getContig() + "_" + genotype.getExtendedAttribute(VCFConstants.PHASE_SET_KEY);
            }
        }

        private void fromHaplotypeDatabase(final File file) throws IOException {

            try(BufferedReader in = new BufferedReader(new InputStreamReader(IOUtil.openFileForReading(file)))){
                // Setup a reader and parse the header
                final StringBuilder builder = new StringBuilder(4096);
                String line = null;

                while ((line = in.readLine()) != null) {
                    if (line.startsWith("@")) {
                        builder.append(line).append('\n');
                    } else {
                        break;
                    }
                }

                if (builder.length() == 0) {
                    throw new IllegalStateException("Haplotype map file must contain header: " + file.getAbsolutePath());
                }

                final SAMFileHeader header = new SAMTextHeaderCodec().decode(new StringLineReader(builder.toString()), "BufferedReader");

                initialize(header);
                // Then read in the file
                final FormatUtil format = new FormatUtil();
                final List<HaplotypeMapFileEntry> entries = new ArrayList<>();
                final Map<String, HaplotypeBlock> anchorToHaplotype = new HashMap<>();
                if (line == null) {
                    return;
                }
                do {
                    if (line.trim().isEmpty()) continue; // skip over blank lines
                    if (line.startsWith("#")) continue;      // ignore comments/headers

                    // Make sure we have the right number of fields
                    final String[] fields = line.split("\\t");
                    if (fields.length < 6 || fields.length > 8) {
                        throw new IOException("Invalid haplotype map record contains " +
                                                  fields.length + " fields: " + line);
                    }

                    // Then parse them out
                    final String chrom = fields[0];
                    final int pos = format.parseInt(fields[1]);
                    final String name = fields[2];
                    final byte major = (byte) fields[3].charAt(0);
                    final byte minor = (byte) fields[4].charAt(0);
                    final double maf = format.parseDouble(fields[5]);
                    final String anchor = fields.length > 6 ? fields[6] : null;
                    final String fpPanels = fields.length > 7 ? fields[7] : null;
                    List<String> panels = null;
                    if (fpPanels != null) {
                        panels = new ArrayList<>();
                        panels.addAll(Arrays.asList(fpPanels.split(",")));
                    }

                    // If it's the anchor snp, start the haplotype
                    if (anchor == null || anchor.trim().equals("") || name.equals(anchor)) {
                        final HaplotypeBlock type = new HaplotypeBlock(maf);
                        type.addSnp(new PicardSnp(name, chrom, pos, major, minor, maf, panels));
                        anchorToHaplotype.put(name, type);
                    } else {  // Otherwise save it for later
                        final HaplotypeMapFileEntry entry = makeHaplotypeMapFileEntry(
                                chrom, pos, name, major, minor, maf, anchor, panels);
                        entries.add(entry);
                    }
                }
                while ((line = in.readLine()) != null);

                // Now, go through and add all the anchored snps
                for (final HaplotypeMapFileEntry entry : entries) {
                    final HaplotypeBlock block = anchorToHaplotype.get(entry.anchorSnp);

                    if (block == null) {
                        throw new IOException("No haplotype found for anchor snp " + entry.anchorSnp);
                    }

                    block.addSnp(new PicardSnp(entry.snpName, entry.chromosome, entry.position,
                            entry.majorAllele, entry.minorAllele,
                            entry.minorAlleleFrequency, entry.panels));
                }

                // And add them all
                fromHaplotypes(anchorToHaplotype.values());

            } catch (IOException ioe) {
                throw new IOException("Error parsing haplotype map.", ioe);
            }
        }

        private HaplotypeMapFileEntry makeHaplotypeMapFileEntry(final String chrom, final int pos, final String name,
                                                                final byte major, final byte minor, final double maf,
                                                                final String anchorSnp, final List<String> fingerprintPanels) {
            return new HaplotypeMapFileEntry(chrom, pos, name, major, minor, maf, anchorSnp, fingerprintPanels);
        }

        /** Constructs an empty HaplotypeMap using the provided SAMFileHeader's sequence dictionary. */
        public HaplotypeMap(final SAMFileHeader header) {
            initialize(header);
        }

        public HaplotypeMap(final File file) throws IOException {
            if (VcfUtils.isVariantFile(file)){
                fromVcf(file);
            } else {
                fromHaplotypeDatabase(file);
            }
        }

        /**
         * since this constructor doesn't initialize the HaplotypeMap "properly", it should be used carefully!
         */
        public HaplotypeMap(final Collection<HaplotypeBlock> haplotypeBlocks){
            fromHaplotypes(haplotypeBlocks);
        }

        private void initialize(final SAMFileHeader header){
            this.header = header;
            this.intervals = new IntervalList(header);
        }

        /**
         * Adds a HaplotypeBlock to the map and updates all the relevant caches/indices.
         */
        public void addHaplotype(final HaplotypeBlock haplotypeBlock) {
            this.haplotypeBlocks.add(haplotypeBlock);

            for (final PicardSnp snp : haplotypeBlock.getSnps()) {
                if (haplotypesBySnp.containsKey(snp)) {
                    throw new IllegalStateException("Same snp name cannot be used twice" + snp.toString());
                }

                this.haplotypesBySnp.put(snp, haplotypeBlock);
                this.haplotypesBySnpName.put(snp.getName(), haplotypeBlock);
                this.haplotypesBySnpLocus.put(toKey(snp.getChrom(), snp.getPos()), haplotypeBlock);
                this.snpsByPosition.put(toKey(snp.getChrom(), snp.getPos()), snp);
                this.intervals.add(new Interval(snp.getChrom(), snp.getPos(), snp.getPos(), false, snp.getName()));
            }
        }

        /** Queries a HaplotypeBlock by Snp object. Returns NULL if none found. */
        public HaplotypeBlock getHaplotype(final Snp snp) {
            return this.haplotypesBySnp.get(snp);
        }

        /** Queries a HaplotypeBlock by Snp name. Returns NULL if none found. */
        public HaplotypeBlock getHaplotype(final String snpName) {
            return this.haplotypesBySnpName.get(snpName);
        }

        /** Queries a HaplotypeBlock by Snp chromosome and position. Returns NULL if none found. */
        public HaplotypeBlock getHaplotype(final String chrom, final int pos) {
            return this.haplotypesBySnpLocus.get(toKey(chrom, pos));
        }

        /** Returns an unmodifiable collection of all the haplotype blocks in the map. */
        public List<HaplotypeBlock> getHaplotypes() {
            return Collections.unmodifiableList(this.haplotypeBlocks);
        }

        /** Queries a Snp by chromosome and position. Returns NULL if none found. */
        public PicardSnp getSnp(final String chrom, final int pos) {
            return this.snpsByPosition.get(toKey(chrom, pos));
        }

        /** Returns an unmodifiable collection of all SNPs in all Haplotype blocks. */
        public Set<PicardSnp> getAllSnps() {
            return Collections.unmodifiableSet(haplotypesBySnp.keySet());
        }

        /** Returns an IntervalList with an entry for every SNP in every Haplotype in the map. */
        public IntervalList getIntervalList() {
            this.intervals = this.intervals.sorted(); // TODO: should probably do this elsewhere
            return this.intervals;
        }

        private String toKey(final String chrom, final int pos) {
            return chrom + ":" + pos;
        }

        /**
         * Returns a copy of this haplotype map that excludes haplotypes on the chromosomes provided.
         * @param chroms a set of zero or more chromosome names
         */
        public HaplotypeMap withoutChromosomes(final Set<String> chroms) {
            final HaplotypeMap out = new HaplotypeMap(getHeader());
            for (final HaplotypeBlock block : this.haplotypeBlocks) {
                if (!chroms.contains(block.getFirstSnp().getChrom())) {
                    out.addHaplotype(block);
                }
            }

            return out;
        }

        public void writeAsVcf(final File output, final File refFile) throws FileNotFoundException {
            ReferenceSequenceFile ref = new IndexedFastaSequenceFile(refFile);
            try (VariantContextWriter writer = new VariantContextWriterBuilder()
                    .setOutputFile(output)
                    .setReferenceDictionary(ref.getSequenceDictionary())
                    .build()) {

                final VCFHeader vcfHeader = new VCFHeader(
                        VCFUtils.withUpdatedContigsAsLines(Collections.emptySet(), refFile, header.getSequenceDictionary(), false),
                        Collections.singleton(HET_GENOTYPE_FOR_PHASING));

                VCFUtils.withUpdatedContigsAsLines(Collections.emptySet(), refFile, header.getSequenceDictionary(), false);

                vcfHeader.addMetaDataLine(new VCFHeaderLine(VCFHeaderVersion.VCF4_2.getFormatString(), VCFHeaderVersion.VCF4_2.getVersionString()));
                vcfHeader.addMetaDataLine(new VCFInfoHeaderLine(VCFConstants.ALLELE_FREQUENCY_KEY, VCFHeaderLineCount.A, VCFHeaderLineType.Float, "Allele Frequency, for each ALT allele, in the same order as listed"));
                vcfHeader.addMetaDataLine(new VCFFormatHeaderLine(VCFConstants.GENOTYPE_KEY, 1, VCFHeaderLineType.String, "Genotype"));
                vcfHeader.addMetaDataLine(new VCFFormatHeaderLine(VCFConstants.PHASE_SET_KEY, 1, VCFHeaderLineType.String, "Phase-set identifier for phased genotypes."));
                vcfHeader.addMetaDataLine(new VCFHeaderLine(VCFHeader.SOURCE_KEY,"HaplotypeMap::writeAsVcf"));
                vcfHeader.addMetaDataLine(new VCFHeaderLine("reference","HaplotypeMap::writeAsVcf"));


                //  vcfHeader.addMetaDataLine(new VCFHeaderLine());
                writer.writeHeader(vcfHeader);
                final LinkedList<VariantContext> variants = new LinkedList<>(this.asVcf(ref));
                variants.sort(vcfHeader.getVCFRecordComparator());
                variants.forEach(writer::add);
            }
        }

        public Collection<VariantContext> asVcf(final ReferenceSequenceFile ref) {

            final List<VariantContext> entries = new ArrayList<>();
            final SortedSet<PicardSnp> snps = new TreeSet<>(getAllSnps());
            final Map<PicardSnp, Boolean> allele1MatchesReference = new HashMap<>(snps.size());

            for( final PicardSnp snp : snps) {
                final ReferenceSequence seq = ref.getSubsequenceAt(snp.getChrom(), snp.getPos(), snp.getPos());
                if (seq.getBases()[0] == snp.getAllele1()) {
                    allele1MatchesReference.put(snp, true);
                } else if (seq.getBases()[0] == snp.getAllele2()) {
                    allele1MatchesReference.put(snp, false);
                } else {
                    throw new RuntimeException("One of the two alleles should agree with the reference: " + snp.toString());
                }
            }

            for (final HaplotypeBlock block : this.getHaplotypes()) {
                PicardSnp anchorSnp = null;
                final SortedSet<PicardSnp> blocksSnps = new TreeSet<>(block.getSnps());

                for (final PicardSnp snp : blocksSnps) {

                    if (anchorSnp == null) {
                        anchorSnp = snp;
                    }

                    final String alleleString = snp.getAlleleString();
                    final boolean swap = allele1MatchesReference.get(snp);
                    final String reference = !swap ? alleleString.substring(0, 1) : alleleString.substring(1, 2);
                    final String alternate = swap ? alleleString.substring(0, 1) : alleleString.substring(1, 2);

                    final double maf = !swap ? snp.getMaf() : 1 - snp.getMaf();

                    VariantContextBuilder builder = new VariantContextBuilder()
                            .chr(snp.getChrom())
                            .start(snp.getPos())
                            .stop(snp.getPos())
                            .alleles(reference, alternate)
                            .attribute(VCFConstants.ALLELE_FREQUENCY_KEY, maf)
                            .id(snp.getName());
                    GenotypeBuilder genotypeBuilder = new GenotypeBuilder(HET_GENOTYPE_FOR_PHASING);

                    if (blocksSnps.size() > 1 && swap) {
                        genotypeBuilder.alleles(Arrays.asList(builder.getAlleles().get(1), builder.getAlleles().get(0)));
                    } else {
                        genotypeBuilder.alleles(builder.getAlleles());
                    }

                    if (blocksSnps.size() > 1) {
                        genotypeBuilder.phased(true);
                        genotypeBuilder.attribute(VCFConstants.PHASE_SET_KEY, anchorSnp.getPos());
                    }
                    builder.genotypes(genotypeBuilder.make());

                    entries.add(builder.make());
                }
            }
            return entries;
        }


        /** Writes out a HaplotypeMap file with the contents of this map. */
        public void writeToFile(final File file) throws IOException {
            try {
                final BufferedWriter out = new BufferedWriter(new OutputStreamWriter(IOUtil.openFileForWriting(file)));
                final FormatUtil format = new FormatUtil();

                // Write out the header
                if (this.header != null) {
                    final SAMTextHeaderCodec codec = new SAMTextHeaderCodec();
                    codec.encode(out, this.header);
                }

                // Write the header for the entries.
                out.write("#CHROMOSOME\tPOSITION\tNAME\tMAJOR_ALLELE\tMINOR_ALLELE\tMAF\tANCHOR_SNP\tPANELS");
                out.newLine();

                final List<HaplotypeMapFileEntry> entries = new ArrayList<>();
                for (final HaplotypeBlock block : this.getHaplotypes()) {
                    String anchor = null;
                    final SortedSet<PicardSnp> snps = new TreeSet<>(block.getSnps());

                    for (final PicardSnp snp : snps) {
                        entries.add(new HaplotypeMapFileEntry(snp.getChrom(), snp.getPos(), snp.getName(),
                                snp.getAllele1(), snp.getAllele2(), snp.getMaf(), anchor, snp.getFingerprintPanels()));

                        if (anchor == null) {
                            anchor = snp.getName();
                        }
                    }
                }

                Collections.sort(entries);
                for (final HaplotypeMapFileEntry entry : entries) {
                    out.write(entry.chromosome + "\t");
                    out.write(format.format(entry.position) + "\t");
                    out.write(entry.snpName + "\t");
                    out.write((char)entry.majorAllele + "\t");
                    out.write((char)entry.minorAllele + "\t");
                    out.write(format.format(entry.minorAlleleFrequency) + "\t");
                    if (entry.anchorSnp != null) {
                        out.write(entry.anchorSnp);
                    }
                    out.write("\t");
                    if (entry.getPanels() != null) {
                        out.write(entry.getPanels());
                    }
                    out.newLine();
                }
                out.flush();
                out.close();
            }
            catch (IOException ioe) {
                throw new IOException("Error writing out haplotype map to file: " + file.getAbsolutePath(), ioe);
            }
        }

        public SAMFileHeader getHeader() { return header; }

        /** Class used to represent all the information for a row in a haplotype map file, used in reading and writing. */
        private class HaplotypeMapFileEntry implements Comparable {
            private final String chromosome;
            private final int position;
            private final String snpName;
            private final byte majorAllele;
            private final byte minorAllele;
            private final double minorAlleleFrequency;
            private final String anchorSnp;
            private final List<String> panels;

            public HaplotypeMapFileEntry(final String chrom, final int pos, final String name,
                                         final byte major, final byte minor, final double maf,
                                         final String anchorSnp, final List<String> fingerprintPanels) {
                this.chromosome = chrom;
                this.position = pos;
                this.snpName = name;
                this.majorAllele = major;
                this.minorAllele = minor;
                this.minorAlleleFrequency = maf;
                this.anchorSnp = anchorSnp;

                // Always sort the list of panels so they are in a predictable order
                this.panels = new ArrayList<>();
                if (fingerprintPanels != null) {
                    this.panels.addAll(fingerprintPanels);
                    Collections.sort(this.panels);
                }
            }

            public String getPanels() {
                if (panels == null) return "";
                final StringBuilder sb = new StringBuilder();

                for (final String panel : panels) {
                    if (sb.length() > 0) sb.append(',');
                    sb.append(panel);
                }

                return sb.toString();
            }

            public int compareTo(final Object o) {
                final HaplotypeMapFileEntry that = (HaplotypeMapFileEntry) o;
                int diff = header.getSequenceIndex(this.chromosome) - header.getSequenceIndex(that.chromosome);
                if (diff != 0) return diff;

                diff = this.position - that.position;
                if (diff != 0) return diff;

                diff = this.snpName.compareTo(that.snpName);
                if (diff != 0) return diff;

                diff = this.majorAllele - that.majorAllele;
                if (diff != 0) return diff;

                diff = this.minorAllele - that.minorAllele;
                if (diff != 0) return diff;

                diff = Double.compare(this.minorAlleleFrequency, that.minorAlleleFrequency);
                if (diff != 0) return diff;

                if (this.anchorSnp != null) {
                    if (that.anchorSnp != null) {
                        diff = this.anchorSnp.compareTo(that.anchorSnp);
                    }
                    else {
                        diff = 1;
                    }
                }
                else {
                    if (that.anchorSnp != null) {
                        diff = -1;
                    }
                    else {
                        diff = 0;
                    }

                }
                if (diff != 0) return diff;

                final String p1 = this.getPanels();
                final String p2 = that.getPanels();
                if (p1 != null) {
                    if (p2 != null) {
                        return p1.compareTo(p2);
                    }
                    return 1;
                }
                else if (p2 != null) {
                    return -1;
                }
                else {
                    return 0;
                }
            }
        }
    }

    public static class HaplotypeBlock implements Comparable<HaplotypeBlock>{

        private final double maf;
        private final Map<String, PicardSnp> snpsByName   = new HashMap<>();
        private final double[] haplotypeFrequencies = new double[3];

        private PicardSnp firstSnp;
        private String chrom;
        private int start;
        private int end;

        /** Constructs a haplotype block with the provided minor allele frequency. */
        public HaplotypeBlock(final double maf) {
            this.maf = maf;

            // Set the haplotype frequencies assuming hardy-weinberg
            final double majorAf = (1 - maf);
            this.haplotypeFrequencies[0] = majorAf * majorAf;
            this.haplotypeFrequencies[1] = majorAf * maf * 2;
            this.haplotypeFrequencies[2] = maf * maf;
        }

        /** Gets the set of haplotype frequencies. */
        public double[] getHaplotypeFrequencies() { return this.haplotypeFrequencies; }

        /** Adds a SNP to the haplotype.  Will throw an exception if the SNP is on the wrong chromosome. */
        public void addSnp(final PicardSnp snp) throws IOException {
            if (this.snpsByName.isEmpty()) {
                this.chrom = snp.getChrom();
                this.start = snp.getPos();
                this.end   = snp.getPos();
                this.firstSnp = snp;
            }
            else if (!this.chrom.equals(snp.getChrom())) {
                throw new IOException("Snp chromosome " + snp.getChrom() +
                                          " does not agree with chromosome of existing snp(s): " + this.chrom);
            }
            else {
                if (snp.getPos() < this.start) {
                    this.start = snp.getPos();
                    this.firstSnp = snp;
                }
                if (snp.getPos() > this.end) {
                    this.end = snp.getPos();
                }
            }

            this.snpsByName.put(snp.getName(), snp);
        }

        /** Gets a SNP by name if it belongs to this haplotype. */
        public PicardSnp getSnp(final String name) {
            return this.snpsByName.get(name);
        }

        /** Gets the arbitrarily first SNP in the haplotype. */
        public PicardSnp getFirstSnp() {
            return this.firstSnp;
        }

        /** Returns true if the SNP is contained within the haplotype block, false otherwise. */
        public boolean contains(final PicardSnp snp) {
            // Check is performed on SNP name and position because of the fact that some SNPs
            // have multiple mappings in the genome and we're paranoid!
            final PicardSnp contained = this.snpsByName.get(snp.getName());
            return contained != null && contained.getChrom().equals(snp.getChrom()) &&
                   contained.getPos() == snp.getPos();
        }

        /** Returns the number of SNPs within the haplotype block. */
        public int size() {
            return snpsByName.size();
        }

        /** Returns an unmodifiable, unordered, collection of all SNPs in this haplotype block. */
        public Collection<PicardSnp> getSnps() {
            return Collections.unmodifiableCollection(this.snpsByName.values());
        }

        /**
         * Gets the frequency of the i'th diploid haplotype where haplotypes are ordered accorinding
         * to DiploidHaplotype.
         */
        public double getHaplotypeFrequency(final int i) {
            if (i < 0 || i > 2) throw new IllegalArgumentException("Illegal haplotype index " + i);
            else return this.haplotypeFrequencies[i];
        }

        /** Returns the minor allele frequency of this haplotype. */
        public double getMaf() { return this.maf; }

        /**
         * Gets the expected genotype of the provided SNP given the provided haplotype of this
         * haplotype block.
         */
        public DiploidGenotype getSnpGenotype(final PicardSnp snp, final DiploidHaplotype haplotype) {
            if (!contains(snp)) throw new IllegalArgumentException("Snp is not part of haplotype " + snp);
            return snp.getGenotype(haplotype);
        }

        /**
         * Gets the diploid haplotype for this haplotype block given the provided SNP and SNP
         * genotype.
         */
        public DiploidHaplotype getDiploidHaplotype(final PicardSnp snp, final DiploidGenotype gt) {
            if (!contains(snp)) throw new IllegalArgumentException("Snp is not part of haplotype " + snp);
            return DiploidHaplotype.values()[snp.indexOf(gt)];
        }

        @Override
        public int compareTo(final HaplotypeBlock that) {
            int retval = this.chrom.compareTo(that.chrom);
            if (retval == 0) retval = this.start - that.start;
            if (retval == 0) retval = this.end   - that.end;

            return retval;
        }

        @Override public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            else return this.compareTo((HaplotypeBlock) o) == 0;
        }

        @Override public int hashCode() {
            return this.start;
        }

        @Override public String toString() {
            return this.chrom + "[" + this.start + "-" + this.end + "]";
        }
    }


    public enum DiploidHaplotype {
        AA, Aa, aa
    }


    public static class VariantContextSet extends TreeSet<VariantContext> {
        public VariantContextSet(final SAMSequenceDictionary dict) {
            super((lhs, rhs) -> {
                final int lhsContig = dict.getSequenceIndex(lhs.getContig());
                final int rhsContig = dict.getSequenceIndex(rhs.getContig());

                if (lhsContig < rhsContig) {
                    return -1;
                } else if (rhsContig < lhsContig) {
                    return 1;
                } else {
                    return Integer.compare(lhs.getStart(), rhs.getStart());
                }
            });
        }
    }

    /**
     * A genotype produced by one of the concrete implementations of AbstractAlleleCaller.
     * DO NOT ADD TO OR REORDER THIS ENUM AS THAT WOULD BREAK THE GELI FILE FORMAT.
     */
    public enum DiploidGenotype {
        AA('A','A'),
        AC('A','C'),
        AG('A','G'),
        AT('A','T'),
        CC('C','C'),
        CG('C','G'),
        CT('C','T'),
        GG('G','G'),
        GT('G','T'),
        TT('T','T');

        private static final Map<Integer, DiploidGenotype> genotypes = new HashMap<Integer, DiploidGenotype>();

        static {
            for (final DiploidGenotype genotype : values()) {
                // this relies on the fact that the integer sum of allele1 and allele2 is unique
                if (genotypes.put(genotype.allele1 + genotype.allele2, genotype) != null) {
                    // this check is just for safety, this should never happen
                    try {
                        throw new IOException("sum of allele values are not unique!!!");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        /** Converts a pair of bases into a DiploidGenotype regardless of base order or case */
        public static DiploidGenotype fromBases(final byte[] bases) {
            if (bases.length != 2) {
                throw new IllegalArgumentException("bases must contain 2 and only 2 bases, it actually contained " + bases.length);
            }
            return fromBases(bases[0], bases[1]);
        }

        /** Converts a pair of bases into a DiploidGenotype regardless of base order or case */
        public static DiploidGenotype fromBases(final byte base1, final byte base2) {
            final byte first = StringUtil.toUpperCase(base1);
            final byte second = StringUtil.toUpperCase(base2);
            final DiploidGenotype genotype = genotypes.get(first + second);
            if (genotype == null) {
                throw new IllegalArgumentException("Unknown genotype string [" +
                                                   StringUtil.bytesToString(new byte[] {base1, base2}) +
                                                   "], any pair of ACTG case insensitive is acceptable");
            }
            return genotype;
        }

        /**
         * @return true if this is a valid base, i.e. one of [ACGTacgt]
         */
        public static boolean isValidBase(final byte base) {
            switch(StringUtil.toUpperCase(base)) {
            case 'A':
            case 'C':
            case 'G':
            case 'T':
                return true;
            default:
                return false;
            }
        }

        private final byte allele1;
        private final byte allele2;

        private DiploidGenotype(final char allele1, final char allele2) {
            this.allele1 = (byte)(allele1 & 0xff);
            this.allele2 = (byte)(allele2 & 0xff);
        }

        public byte getAllele1() { return allele1; }
        public byte getAllele2() { return allele2; }
        public boolean isHeterozygous() { return this.allele1 != this.allele2; }
        public boolean isHomomozygous() { return this.allele1 == this.allele2; }
    }


    public static class PicardSnp implements Comparable<PicardSnp> {
        private String name;
        private String chrom;
        private int pos;
        private byte allele1;
        private byte allele2;
        private double maf; // technically the allele frequency of allele2
        private List<String> fingerprintPanels;

        private final DiploidGenotype[] genotypes = new DiploidGenotype[3];

        public PicardSnp(final String name, final String chrom, final int pos, final byte allele1, final byte allele2,
                   final double maf, final List<String> fingerprintPanels) {
            this.name = name;
            this.chrom = chrom;
            this.pos = pos;
            this.allele1 = StringUtil.toUpperCase(allele1);
            this.allele2 = StringUtil.toUpperCase(allele2);
            this.maf = maf;
            this.fingerprintPanels = fingerprintPanels == null ? new ArrayList<String>() : fingerprintPanels;

            // Construct the genotypes for ease of comparison
            this.genotypes[0] = DiploidGenotype.fromBases(allele1, allele1);
            this.genotypes[1] = DiploidGenotype.fromBases(allele1, allele2);
            this.genotypes[2] = DiploidGenotype.fromBases(allele2, allele2);
        }

        /** Returns a new SNP object with the alleles swapped and MAF corrected. */
        public PicardSnp flip() {
            return new PicardSnp(name, chrom, pos, allele2, allele1, 1-maf, fingerprintPanels);
        }

        public String getName() { return name; }
        public String getChrom() { return chrom; }
        public int getPos() { return pos; }
        public byte getAllele1() { return allele1; }
        public byte getAllele2() { return allele2; }

        public List<Allele> getAlleles() {
            return CollectionUtil.makeList(Allele.create(getAllele1()), Allele.create(getAllele2()));
        }

        public double getMaf() { return maf; }
        public List<String> getFingerprintPanels() { return this.fingerprintPanels; }

        public DiploidGenotype getHomozygousAllele1Genotype() { return this.genotypes[0]; }
        public DiploidGenotype getHeterogyzousGenotype() { return this.genotypes[1]; }
        public DiploidGenotype getHomozygousAllele2Genotype() { return this.genotypes[2]; }

        /** Gets the genotype with the given index. */
        DiploidGenotype getGenotype(final DiploidHaplotype haplotype) { return this.genotypes[haplotype.ordinal()]; }

        /** Gets the index of the supplied genotype within the genotypes for this SNP. */
        int indexOf(final DiploidGenotype gt) {
            for (int i=0; i<this.genotypes.length; ++i) {
                if (gt == this.genotypes[i]) return i;
            }

            throw new IllegalArgumentException("Genotype " + gt + " is not valid for this SNP.");
        }

        public String getAlleleString() {
            return StringUtil.bytesToString(new byte[] {allele1, StringUtil.toLowerCase(allele2)});
        }


        public int compareTo(final PicardSnp that) {
            int retval = this.chrom.compareTo(that.chrom);
            if (retval == 0) retval = this.pos - that.pos;
            return retval;
        }

        @Override
        public boolean equals(final Object o) {
            return (this == o) || ((o instanceof Snp) && compareTo((PicardSnp) o) == 0);
        }

        @Override
        public int hashCode() {
            int result = chrom.hashCode();
            result = 31 * result + pos;
            return result;
        }

        @Override
        public String toString() {
            return this.chrom + ":" + this.pos;
        }
    }

    public static class VcfUtils {

        /**
         * Checks if the suffix is one of those that are allowed for the various
         * formats that contain variants (currently vcf and bcf)
         */
        public static boolean isVariantFile(final File file){
            final String name = file.getName();

            return name.endsWith(IOUtil.VCF_FILE_EXTENSION) ||
                   name.endsWith(IOUtil.COMPRESSED_VCF_FILE_EXTENSION) ||
                   name.endsWith(IOUtil.BCF_FILE_EXTENSION);
        }
    }
}
