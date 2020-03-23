package org.broadinstitute.gpinformatics.mercury.presentation.hsa;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.ValidationMethod;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.mercury.control.dao.hsa.AggregationStateDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.hsa.AlignmentStateDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.AligntmentTaskBase;
import org.broadinstitute.gpinformatics.mercury.control.hsa.engine.FiniteStateMachineFactory;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.AggregationState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.AlignmentState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.FiniteStateMachine;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Task;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@UrlBinding(FingerprintWorkflowActionBean.ACTION_BEAN_URL)
public class FingerprintWorkflowActionBean extends CoreActionBean {
    private static final Log log = LogFactory.getLog(FingerprintWorkflowActionBean.class);

    public static final String ACTION_BEAN_URL = "/hsa/workflows/fingerprint.action";
    public static final String CREATE_ALIGNMENT = CoreActionBean.CREATE + "Alignment Task";

    private static final String FP_CREATE_PAGE = "/hsa/workflows/fingerprint/create.jsp";

    private static final String CREATE_ALIGNMENT_ACTION = "createFingerprint";
    private static final String SEARCH_ACTION = "search";

    private String sampleIds;

    private Map<String, MercurySample> mapIdToMercurySample;

    @Inject
    private MercurySampleDao mercurySampleDao;

    @Inject
    private AlignmentStateDao alignmentStateDao;

    @Inject
    private AggregationStateDao aggregationStateDao;

    @Inject
    private FiniteStateMachineFactory finiteStateMachineFactory;

    private List<AlignmentDirectoryDto> alignmentDirectoryDtos;

    private List<String> selectedDirectories;
    private boolean searchAggregations;

    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        setSubmitString(CREATE_ALIGNMENT);
        return new ForwardResolution(FP_CREATE_PAGE);
    }

    @ValidationMethod(on = {SEARCH_ACTION})
    public void validateSearch() {
        if (StringUtils.isBlank(sampleIds)) {
            addValidationError("sampleIds", "Sample ID is required.");
            return;
        }
        String[] split = sampleIds.split("\\s+");
        if (split.length == 0) {
            addValidationError("sampleIds", "Please include at least one sample");
            return;
        }
        mapIdToMercurySample =
                mercurySampleDao.findMapIdToMercurySample(new HashSet<>(Arrays.asList(split)));

        for (String sampleId: split) {
            if (mapIdToMercurySample.get(sampleId) == null) {
                addValidationError("sampleIds", "Failed to find Sample: " + sampleId);
            }
        }
    }

    @HandlesEvent(SEARCH_ACTION)
    public Resolution search() {
        List<AligntmentTaskBase> tasks = null;
        if (searchAggregations) {
            List<AggregationState> alignments =
                    aggregationStateDao.findCompletedAggregationsForSample(mapIdToMercurySample.values());
            tasks = alignments.stream().map(state -> state.getTasksOfType(AligntmentTaskBase.class))
                    .flatMap(Set::stream)
                    .collect(Collectors.toList());
        } else {
            List<AlignmentState> alignments =
                    alignmentStateDao.findCompletedAlignmentsForSample(mapIdToMercurySample.values());
            tasks = alignments.stream().map(state -> state.getTasksOfType(AligntmentTaskBase.class))
                    .flatMap(Set::stream)
                    .collect(Collectors.toList());
        }

        alignmentDirectoryDtos = new ArrayList<>();
        for (Task t: tasks) {
            if (!OrmUtil.proxySafeIsInstance(t, AligntmentTaskBase.class)) {
                continue;
            }
            AligntmentTaskBase alignmentTask = OrmUtil.proxySafeCast(t, AligntmentTaskBase.class);
            if (!alignmentTask.isEnableVariantCaller()) {
                continue;
            }
            String fastQSampleId = alignmentTask.getFastQSampleId();
            String filePrefix = alignmentTask.getOutputFilePrefix();
            if (mapIdToMercurySample.containsKey(fastQSampleId)) {
                MercurySample sample = mapIdToMercurySample.get(fastQSampleId);
                File outputDir = alignmentTask.getOutputDir();
                String vcSampleName = alignmentTask.getVcSampleName();
                File referenceSeq = alignmentTask.getReference();
                AggregationActionBean.ReferenceGenome referenceGenome =
                        AggregationActionBean.ReferenceGenome.getByDragenPath(referenceSeq.getPath());
                if (referenceGenome == null) {
                    log.error("Failed to find reference genome for " + referenceSeq.getPath());
                    continue;
                }
                if (validateBamAndVcfExist(outputDir, filePrefix)) {
                    AlignmentDirectoryDto dto = new AlignmentDirectoryDto();
                    Pair<File, File> bamVcf = grabBamAndVcfFiles(outputDir, filePrefix);
                    dto.setSampleKey(sample.getSampleKey());
                    dto.setBamFile(bamVcf.getLeft().getPath());
                    dto.setVcfFile(bamVcf.getRight().getPath());
                    dto.setFasta(referenceGenome.getFasta());
                    dto.setHaplotypeDatabase(referenceGenome.getHaplotypeDatabase());
                    dto.setOutputDirectory(outputDir.getPath());
                    alignmentDirectoryDtos.add(dto);
                }
            }
        }

        if (alignmentDirectoryDtos.isEmpty()) {
            addGlobalValidationError("Failed to find any alignments.");
        }

        return new ForwardResolution(FP_CREATE_PAGE);
    }


    @ValidationMethod(on = {CREATE_ACTION})
    public void validateCreate() {
        if (selectedDirectories == null || selectedDirectories.isEmpty()) {
            addGlobalValidationError("Please select at least one directory.");
        }
    }

    @HandlesEvent(CREATE_ACTION)
    public Resolution create() {
        List<AlignmentDirectoryDto> selectedAlignments = alignmentDirectoryDtos.stream()
                .filter(dto -> selectedDirectories.contains(dto.getOutputDirectory()))
                .collect(Collectors.toList());
        List<FiniteStateMachine> fpStateMachines =
                finiteStateMachineFactory.createFingerprintTasks(selectedAlignments);
        addMessage(String.format("Created %d state machines", fpStateMachines.size()));
        return new ForwardResolution(FP_CREATE_PAGE);
    }

    private boolean validateBamAndVcfExist(File outputDir, String filePrefix) {
        Pair<File, File> pair = grabBamAndVcfFiles(outputDir, filePrefix);
        return pair.getLeft().exists() && pair.getRight().exists();
    }

    private Pair<File, File> grabBamAndVcfFiles(File outputDir, String filePrefix) {
        File bam = new File(outputDir, filePrefix + ".bam");
        if (!bam.exists()) {
            bam = new File(outputDir, filePrefix + ".cram");
        }
        File vcf = new File(outputDir, filePrefix + ".vcf.gz");
        return Pair.of(bam, vcf);
    }

    public String getSampleIds() {
        return sampleIds;
    }

    public void setSampleIds(String sampleIds) {
        this.sampleIds = sampleIds;
    }

    public List<String> getSelectedDirectories() {
        return selectedDirectories;
    }

    public void setSelectedDirectories(List<String> selectedDirectories) {
        this.selectedDirectories = selectedDirectories;
    }

    public List<AlignmentDirectoryDto> getAlignmentDirectoryDtos() {
        return alignmentDirectoryDtos;
    }

    public void setAlignmentDirectoryDtos(
            List<AlignmentDirectoryDto> alignmentDirectoryDtos) {
        this.alignmentDirectoryDtos = alignmentDirectoryDtos;
    }

    public boolean getSearchAggregations() {
        return searchAggregations;
    }

    public void setSearchAggregations(boolean searchAggregations) {
        this.searchAggregations = searchAggregations;
    }

    public static class AlignmentDirectoryDto {

        private String bamFile;
        private String vcfFile;
        private String haplotypeDatabase;
        private String outputDirectory;
        private String sampleKey;
        private String fasta;

        public AlignmentDirectoryDto() {
        }

        public void setBamFile(String bamFile) {
            this.bamFile = bamFile;
        }

        public String getBamFile() {
            return bamFile;
        }

        public void setVcfFile(String vcfFile) {
            this.vcfFile = vcfFile;
        }

        public String getVcfFile() {
            return vcfFile;
        }

        public void setHaplotypeDatabase(String haplotypeDatabase) {
            this.haplotypeDatabase = haplotypeDatabase;
        }

        public String getHaplotypeDatabase() {
            return haplotypeDatabase;
        }

        public void setOutputDirectory(String outputDirectory) {
            this.outputDirectory = outputDirectory;
        }

        public String getOutputDirectory() {
            return outputDirectory;
        }

        public void setSampleKey(String sampleKey) {
            this.sampleKey = sampleKey;
        }

        public String getSampleKey() {
            return sampleKey;
        }

        public void setFasta(String fasta) {
            this.fasta = fasta;
        }

        public String getFasta() {
            return fasta;
        }
    }
}
