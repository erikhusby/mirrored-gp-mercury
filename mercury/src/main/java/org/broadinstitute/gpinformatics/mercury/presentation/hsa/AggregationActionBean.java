package org.broadinstitute.gpinformatics.mercury.presentation.hsa;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.Validate;
import net.sourceforge.stripes.validation.ValidationMethod;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.deployment.DragenConfig;
import org.broadinstitute.gpinformatics.infrastructure.search.LabVesselSearchDefinition;
import org.broadinstitute.gpinformatics.mercury.control.dao.hsa.StateMachineDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.IlluminaSequencingRunDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.hsa.engine.CramFileNameBuilder;
import org.broadinstitute.gpinformatics.mercury.control.hsa.engine.FiniteStateMachineFactory;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.FiniteStateMachine;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

@UrlBinding(AggregationActionBean.ACTION_BEAN_URL)
public class AggregationActionBean extends CoreActionBean {
    private static final Log logger = LogFactory.getLog(AggregationActionBean.class);

    public static final String ACTION_BEAN_URL = "/hsa/workflows/aggregation.action";
    public static final String CREATE_ALIGNMENT = CoreActionBean.CREATE + "Aggregation Task";

    private static final String ALIGNMENT_CREATE_PAGE = "/hsa/workflows/aggregation/create.jsp";

    private static final String CREATE_ALIGNMENT_ACTION = "createAlignment";
    private static final String SEARCH_ACTION = "search";

    @Validate(field = "sampleIds", label = "Sample ID", required = true, on = {SEARCH_ACTION})
    private String sampleIds;

    @Inject
    private IlluminaSequencingRunDao illuminaSequencingRunDao;

    @Inject
    private MercurySampleDao mercurySampleDao;

    @Inject
    private StateMachineDao stateMachineDao;

    @Inject
    private FiniteStateMachineFactory finiteStateMachineFactory;

    @Inject
    private DragenConfig dragenConfig;

    private String alignmentStateName;

    private String q;

    private List<SampleRunData> sampleRunData;

    private ReferenceGenome referenceGenome;

    private List<String> selectedRuns;

    private boolean createFingerprint;

    private boolean redoDemultiplex;

    private List<String> sampleIdList;

    private Collection<MercurySample> samples;

    private boolean crosscheckFingerprint;

    private String[] configFiles;

    private String selectedConfig;

    private CramFileNameBuilder.CramFilenameFormat cramFilenameFormat;
    private boolean allowMultipleAggregationTasks;

    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        setSubmitString(CREATE_ALIGNMENT);
        return new ForwardResolution(ALIGNMENT_CREATE_PAGE);
    }

    @ValidationMethod(on = {SEARCH_ACTION})
    public void validateSearch() {
        if (StringUtils.isBlank(sampleIds)) {
            addValidationError("sampleIds", "At least one sample is required.");
        } else {
            sampleIdList = Arrays.asList(sampleIds.split("\\s+"));
            Map<String, MercurySample> mapIdToMercurySample = mercurySampleDao.findMapIdToMercurySample(sampleIdList);
            for (String sampleKey: sampleIdList) {
                if (mapIdToMercurySample.get(sampleKey) == null) {
                    addValidationError("sampleIds", "Failed to find Sample: " + sampleKey);
                }
            }

            samples = mapIdToMercurySample.values();
        }
    }

    @HandlesEvent(SEARCH_ACTION)
    public Resolution search() {

        sampleRunData = new ArrayList<>();

        Map<String, List<IlluminaSequencingRun>> mapFlowcellToRuns = new HashMap<>();
        for (MercurySample sample: samples) {
            Set<LabVessel> labVessels = sample.getLabVessel();
            for (LabVessel labVessel : labVessels) {
                LabVesselSearchDefinition.VesselsForEventTraverserCriteria eval
                        = new LabVesselSearchDefinition.VesselsForEventTraverserCriteria(
                        LabVesselSearchDefinition.FLOWCELL_LAB_EVENT_TYPES);
                labVessel.evaluateCriteria(eval, TransferTraverserCriteria.TraversalDirection.Descendants);

                Map<LabVessel, Collection<VesselPosition>> labVesselCollectionMap = eval.getPositions().asMap();

                for (Map.Entry<LabVessel, Collection<VesselPosition>> labVesselAndPositions : labVesselCollectionMap
                        .entrySet()) {
                    IlluminaFlowcell flowcell =
                            OrmUtil.proxySafeCast(labVesselAndPositions.getKey(), IlluminaFlowcell.class);
                    String flowcellBarcode = flowcell.getLabel();

                    List<IlluminaSequencingRun> runs = null;
                    if (mapFlowcellToRuns.containsKey(flowcellBarcode)) {
                        runs = mapFlowcellToRuns.get(flowcellBarcode);
                    } else {
                        runs = illuminaSequencingRunDao.findByFlowcellBarcode(flowcellBarcode);
                        mapFlowcellToRuns.put(flowcellBarcode, runs);
                    }

                    for (IlluminaSequencingRun run : runs) {
                        SampleRunData sampleRunDto = new SampleRunData(sample.getSampleKey(),
                                new ArrayList<>(labVesselAndPositions.getValue()), run.getRunName(), flowcellBarcode,
                                run.getRunDate());
                        sampleRunData.add(sampleRunDto);
                    }
                }
            }
        }

        setSubmitString(CREATE_ALIGNMENT);
        return new ForwardResolution(ALIGNMENT_CREATE_PAGE);
    }

    @ValidationMethod(on = {CREATE_ACTION})
    public void createValidation() {
        if (selectedRuns == null || selectedRuns.isEmpty()) {
            addGlobalValidationError("Must check at least one run to continue.");
        } else {
            // TODO For sample pairs disallow if different seq type
            sampleRunData = sampleRunData.stream()
                    .filter(s -> selectedRuns.contains(s.getRunName()))
                    .collect(Collectors.toList());
        }
    }

    @HandlesEvent(CREATE_ACTION)
    public Resolution create() {
        MessageCollection messageCollection = new MessageCollection();

        List<FiniteStateMachine> finiteStateMachines = null;
        try {
            Map<String, List<SampleRunData>> sampleToLanes =
                    sampleRunData.stream().collect(groupingBy(SampleRunData::getSampleKey));
            boolean useConfig = !selectedConfig.equals("None");
            String configFilePath = null;
            if (useConfig) {
                configFilePath = dragenConfig.getConfigFilePath() + selectedConfig;
            }
            finiteStateMachines = finiteStateMachineFactory.createAggegations(
                    sampleToLanes, redoDemultiplex, crosscheckFingerprint, referenceGenome,
                    useConfig, configFilePath, cramFilenameFormat, allowMultipleAggregationTasks, messageCollection);
            addMessage("Created aggregation workflow.");
        } catch (Exception e) {
            logger.error("Error creating aggregation workflow", e);
            messageCollection.addError("Error creating aggregation workflow: " + e.getMessage());
        }

        if (messageCollection.hasErrors()) {
            addMessages(messageCollection);
        } else if (finiteStateMachines == null) {
            addMessage("Failed to create finite state machine");
        } else {
            stateMachineDao.persistAll(finiteStateMachines);
            stateMachineDao.flush();
        }
        return new ForwardResolution(ALIGNMENT_CREATE_PAGE);
    }

    public String getAlignmentStateName() {
        return alignmentStateName;
    }

    public void setAlignmentStateName(String alignmentStateName) {
        this.alignmentStateName = alignmentStateName;
    }

    public String getSampleIds() {
        return sampleIds;
    }

    public void setSampleIds(String sampleIds) {
        this.sampleIds = sampleIds;
    }

    public List<SampleRunData> getSampleRunData() {
        return sampleRunData;
    }

    public void setSampleRunData(
            List<SampleRunData> sampleRunData) {
        this.sampleRunData = sampleRunData;
    }

    public ReferenceGenome getReferenceGenome() {
        return referenceGenome;
    }

    public void setReferenceGenome(ReferenceGenome referenceGenome) {
        this.referenceGenome = referenceGenome;
    }

    public String getQ() {
        return q;
    }

    public void setQ(String q) {
        this.q = q;
    }

    public List<String> getSelectedRuns() {
        return selectedRuns;
    }

    public void setSelectedRuns(List<String> selectedRuns) {
        this.selectedRuns = selectedRuns;
    }

    public boolean isCreateFingerprint() {
        return createFingerprint;
    }

    public void setCreateFingerprint(boolean createFingerprint) {
        this.createFingerprint = createFingerprint;
    }

    public boolean isRedoDemultiplex() {
        return redoDemultiplex;
    }

    public void setRedoDemultiplex(boolean redoDemultiplex) {
        this.redoDemultiplex = redoDemultiplex;
    }

    public boolean getCrosscheckFingerprint() {
        return crosscheckFingerprint;
    }

    public void setCrosscheckFingerprint(boolean crosscheckFingerprint) {
        this.crosscheckFingerprint = crosscheckFingerprint;
    }

    public String[] getConfigFiles() {
        if (configFiles == null) {
            File configDir = new File(dragenConfig.getConfigFilePath());
            configFiles = configDir.list();
        }
        return configFiles;
    }

    public void setConfigFiles(String[] configFiles) {
        this.configFiles = configFiles;
    }

    public String getSelectedConfig() {
        return selectedConfig;
    }

    public void setSelectedConfig(String selectedConfig) {
        this.selectedConfig = selectedConfig;
    }

    public CramFileNameBuilder.CramFilenameFormat getCramFilenameFormat() {
        return cramFilenameFormat;
    }

    public void setCramFilenameFormat(
            CramFileNameBuilder.CramFilenameFormat cramFilenameFormat) {
        this.cramFilenameFormat = cramFilenameFormat;
    }

    public boolean getAllowMultipleAggregationTasks() {
        return allowMultipleAggregationTasks;
    }

    public void setAllowMultipleAggregationTasks(boolean allowMultipleAggregationTasks) {
        this.allowMultipleAggregationTasks = allowMultipleAggregationTasks;
    }

    // TODO JW Move somewhere better
    public enum ReferenceGenome {
        HG38("hg38", "/seq/dragen/references/hg38/current/",
                "/seq/references/Homo_sapiens_assembly38/v0/Homo_sapiens_assembly38.haplotype_database.txt",
                "/seq/references/Homo_sapiens_assembly38/v0/Homo_sapiens_assembly38.fasta",
                "/opt/edico/config/sample_cross_contamination_resource_hg38.vcf",
                "/seq/dragen/references/hg38/current/wgs_coverage_regions.hg38.interval_list.bed"),
        HG19("hg19", "/seq/dragen/references/hg19/current/",
                "/seq/references/Homo_sapiens_assembly19/v1/Homo_sapiens_assembly19.haplotype_database.txt",
                "/seq/references/Homo_sapiens_assembly19/v1/Homo_sapiens_assembly19.fasta",
                "/seq/dragen/references/hg19/current/sample_cross_contamination_resource_hg19_JIM_EDIT.vcf",
                "/seq/dragen/references/hg19/v1/wgs_coverage_regions.hg19.interval_list.bed"),
        GRCH38DH("GRCh38dh", "/seq/dragen/references/GRCh38dh/current/",
                null, null, "/seq/dragen/references/GRCh38dh/current/SNP_NCBI_GRCh38.vcf",
                "/seq/dragen/references/GRCh38dh/current/wgs_coverage_regions.hg38_minus_N.interval_list.bed",
                "/seq/dragen/references/GRCh38dh/current/acmg59_allofus_19dec2019.GRC38.wGenes.bed",
                "/seq/dragen/references/GRCh38dh/current/PGx_singleSite_GRCh38_ActualTableToSendToFDA_21jan2020.bed"),
        HS37D5("hs37d5", "/seq/dragen/references/hs37d5/current/",
                null, null, "/seq/dragen/references/hs37d5/current/SNP_NCBI_hs37d5.vcf",
                "/seq/dragen/references/hs37d5/current/wgs_coverage_regions.hs37d5_minus_N.interval_list.bed",
                "/seq/dragen/references/hs37d5/current/acmg59_allofus_19dec2019.bed",
                "/seq/dragen/references/hs37d5/current/PGx_singleSite_GRCh37_ActualTableToSendToFDA_21jan2020.annotated.bed");

        private final String name;
        private final String path;
        private final String haplotypeDatabase;
        private final String fasta;
        private final String contamFile;
        private final String coverageBedFile;
        private final String coverageBed2File;
        private final String coverageBed3File;

        private final static Map<String, ReferenceGenome> MAP_PATH_TO_REF = new HashMap<>();

        ReferenceGenome(String name, String path, String haplotypeDatabase, String fasta, String contamFile, String coverageBedFile) {
            this(name, path, haplotypeDatabase, fasta, contamFile, coverageBedFile, null, null);
        }

        ReferenceGenome(String name, String path, String haplotypeDatabase, String fasta, String contamFile, String coverageBedFile,
                        String coverageBed2File, String coverageBed3File) {
            this.name = name;
            this.path = path;
            this.haplotypeDatabase = haplotypeDatabase;
            this.fasta = fasta;
            this.contamFile = contamFile;
            this.coverageBedFile = coverageBedFile;
            this.coverageBed2File = coverageBed2File;
            this.coverageBed3File = coverageBed3File;
        }

        static {
            for (ReferenceGenome referenceGenome: ReferenceGenome.values()) {
                MAP_PATH_TO_REF.put(referenceGenome.getPath(), referenceGenome);
            }
        }

        public static ReferenceGenome getByDragenPath(String path) {
            ReferenceGenome referenceGenome = MAP_PATH_TO_REF.get(path);
            if (referenceGenome == null) {
                path += "/";
                return MAP_PATH_TO_REF.get(path);
            }
            return null;
        }

        public String getName() {
            return name;
        }

        public String getPath() {
            return path;
        }

        public String getHaplotypeDatabase() {
            return haplotypeDatabase;
        }

        public String getFasta() {
            return fasta;
        }

        public String getContamFile() {
            return contamFile;
        }

        public String getCoverageBedFile() {
            return coverageBedFile;
        }

        public String getCoverageBed2File() {
            return coverageBed2File;
        }

        public String getCoverageBed3File() {
            return coverageBed3File;
        }
    }

    public static class SampleRunData {
        private String sampleKey;
        private List<VesselPosition> lanes;
        private String runName;
        private String flowcell;
        private Date runDate;

        public SampleRunData() {
        }

        public SampleRunData(String sampleKey, List<VesselPosition> lanes, String runName, String flowcell, Date runDate) {
            this.sampleKey = sampleKey;
            this.lanes = lanes;
            this.runName = runName;
            this.flowcell = flowcell;
            this.runDate = runDate;
        }

        public String getSampleKey() {
            return sampleKey;
        }

        public void setSampleKey(String sampleKey) {
            this.sampleKey = sampleKey;
        }

        public List<VesselPosition> getLanes() {
            return lanes;
        }

        public void setLanes(List<VesselPosition> lanes) {
            this.lanes = lanes;
        }

        public String getLanesString() {
            return getLanes().stream()
                    .filter(Objects::nonNull)
                    .map(e -> e.name().replace("LANE", ""))
                    .sorted()
                    .collect(Collectors.joining(","));
        }

        public void setLanesString(String lanesString) {
            this.lanes = Arrays.stream(lanesString.split(","))
                    .map(l -> VesselPosition.getByName("LANE" + l))
                    .collect(Collectors.toList());
        }

        public String getRunName() {
            return runName;
        }

        public void setRunName(String runName) {
            this.runName = runName;
        }

        public String getFlowcell() {
            return flowcell;
        }

        public void setFlowcell(String flowcell) {
            this.flowcell = flowcell;
        }

        public Date getRunDate() {
            return runDate;
        }

        public void setRunDate(Date runDate) {
            this.runDate = runDate;
        }
    }
}
