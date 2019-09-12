package org.broadinstitute.gpinformatics.mercury.presentation.hsa;

import net.sourceforge.stripes.action.Before;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.Validate;
import net.sourceforge.stripes.validation.ValidateNestedProperties;
import net.sourceforge.stripes.validation.ValidationMethod;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.deployment.DragenConfig;
import org.broadinstitute.gpinformatics.infrastructure.search.LabVesselSearchDefinition;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;
import org.broadinstitute.gpinformatics.mercury.control.dao.hsa.AlignmentStateDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.IlluminaSequencingRunDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.IlluminaFlowcellDao;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.AlignmentTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.AlignmentAggregationState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.AlignmentState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.FiniteStateMachine;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.State;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatchStartingVessel;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@UrlBinding(AlignmentActionBean.ACTION_BEAN_URL)
public class AlignmentActionBean extends CoreActionBean {
    private static final Log logger = LogFactory.getLog(AlignmentActionBean.class);

    public static final String ACTION_BEAN_URL = "/hsa/workflows/alignment.action";
    public static final String CREATE_ALIGNMENT = CoreActionBean.CREATE + "Alignment Task";

    private static final String ALIGNMENT_CREATE_PAGE = "/hsa/workflows/alignment/create.jsp";

    private static final String CREATE_ALIGNMENT_ACTION = "createAlignment";
    private static final String SEARCH_ACTION = "search";

    @ValidateNestedProperties({
            @Validate(field = "stateName", label = "State Name", required = true, maxlength = 255, on = {CREATE_ACTION}),
    })
    private AlignmentState editAlignmentState;

    @Validate(field = "sampleIds", label = "Sample IDs", required = true, on = {SEARCH_ACTION})
    private String sampleIds;

    @Inject
    private AlignmentStateDao alignmentStateDao;

    @Inject
    private IlluminaSequencingRunDao illuminaSequencingRunDao;

    @Inject
    private MercurySampleDao mercurySampleDao;

    @Inject
    private DragenConfig dragenConfig;

    private String alignmentStateName;

    private String q;

    private Map<String, MercurySample> mapIdToMercurySample;

    private List<SampleRunData> sampleRunData;

    private ReferenceGenome referenceGenome;

    private List<String> selectedRuns;
    private Map<String, List<SampleRunData>> sampleRunByKey;

    /**
     * Initialize the product with the passed in key for display in the form
     */
    @Before(stages = LifecycleStage.BindingAndValidation)
    public void init() {
        alignmentStateName = getContext().getRequest().getParameter("alignmentStateName");
        if (!StringUtils.isBlank(alignmentStateName)) {
            editAlignmentState = alignmentStateDao.findByIdentifier(alignmentStateName);
        } else {
            editAlignmentState = new AlignmentState();
        }
        // TODO Think about filters at a later state
    }

    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        setSubmitString(CREATE_ALIGNMENT);
        return new ForwardResolution(ALIGNMENT_CREATE_PAGE);
    }

    @ValidationMethod(on = {SEARCH_ACTION})
    public void validateSearch() {
        String[] split = sampleIds.split("\\s+");
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

        sampleRunData = new ArrayList<>();

        Map<String, List<IlluminaSequencingRun>> mapFlowcellToRuns = new HashMap<>();
        for (MercurySample sample: mapIdToMercurySample.values()) {
            Set<LabVessel> labVessels = sample.getLabVessel();
            for (LabVessel labVessel: labVessels) {
                LabVesselSearchDefinition.VesselsForEventTraverserCriteria eval
                        = new LabVesselSearchDefinition.VesselsForEventTraverserCriteria(LabVesselSearchDefinition.FLOWCELL_LAB_EVENT_TYPES);
                labVessel.evaluateCriteria(eval, TransferTraverserCriteria.TraversalDirection.Descendants);

                Map<LabVessel, Collection<VesselPosition>> labVesselCollectionMap = eval.getPositions().asMap();

                for(Map.Entry<LabVessel, Collection<VesselPosition>> labVesselAndPositions: labVesselCollectionMap.entrySet()) {
                    IlluminaFlowcell flowcell =
                            OrmUtil.proxySafeCast(labVesselAndPositions.getKey(), IlluminaFlowcell.class);
                    String flowcellBarcode = flowcell.getLabel();

                    List<IlluminaSequencingRun> runs = null;
                    if (mapFlowcellToRuns.containsKey(flowcellBarcode)) {
                        runs = mapFlowcellToRuns.get(flowcellBarcode);
                    } else {
                        runs = illuminaSequencingRunDao.findByFlowcellBarcode(flowcellBarcode);
                    }

                    for (IlluminaSequencingRun run: runs) {
                        SampleRunData sampleRunDto = new SampleRunData(sample.getSampleKey(),
                                new ArrayList<>(labVesselAndPositions.getValue()), run.getRunName(), flowcellBarcode, run.getRunDate());
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
            sampleRunByKey = sampleRunData.stream()
                    .filter(s -> selectedRuns.contains(s.getRunName()))
                    .collect(Collectors.groupingBy(SampleRunData::getSampleKey));
        }
    }

    @HandlesEvent(CREATE_ACTION)
    public Resolution create() {
        FiniteStateMachine finiteStateMachine = new FiniteStateMachine();
        String fileDateTime = DateUtils.getFileDateTime(new Date());
        finiteStateMachine.setStateMachineName("ManualAlignmentMachine_" + fileDateTime);
        AlignmentState alignmentState = new AlignmentState();

        Map<String, MercurySample> mapKeyToSample =
                mercurySampleDao.findMapIdToMercurySample(sampleRunByKey.keySet());
        for (Map.Entry<String, List<SampleRunData>> entry: sampleRunByKey.entrySet()) {
            for (SampleRunData sampleRunData: entry.getValue()) {
                IlluminaSequencingRun run = illuminaSequencingRunDao.findByRunName(sampleRunData.getRunName());

                MercurySample mercurySample = mapKeyToSample.get(entry.getKey());
                File referenceFile = new File(referenceGenome.getPath());
                File fastQList = null;
                File outputDir = null;
                File intermediateResults = new File("/staging/out");
                AlignmentTask alignmentTask = new AlignmentTask(referenceFile, fastQList, mercurySample.getSampleKey(),
                        outputDir, intermediateResults, mercurySample.getSampleKey(), mercurySample.getSampleKey(),
                        new File(referenceGenome.getContamFile()));
                alignmentTask.setTaskName("Alignment_" + mercurySample.getSampleKey() + " ");
                alignmentState.addTask(alignmentTask);
            }
        }
        return new ForwardResolution(ALIGNMENT_CREATE_PAGE);
    }

    public AlignmentState getEditAlignmentState() {
        return editAlignmentState;
    }

    public void setEditAlignmentState(
            AlignmentState editAlignmentState) {
        this.editAlignmentState = editAlignmentState;
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

    public void setReferenceGenome(
            ReferenceGenome referenceGenome) {
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

    // TODO JW Move somewhere better
    public enum ReferenceGenome {
        HG19("hg19", "/staging/reference/hg19/v1/",
                "/seq/reference«s/Homo_sapiens_assembly19/v1/Homo_sapiens_assembly19.haplotype_database.txt",
                "/seq/references/Homo_sapiens_assembly19/v1/Homo_sapiens_assembly19.fasta",
                "/opt/edico/config/sample_cross_contamination_resource_hg19.vcf"),
        HG38("hg38", "/staging/reference/hg38/v1/",
                "/seq/references/Homo_sapiens_assembly38/v0/Homo_sapiens_assembly38.haplotype_database.txt",
                "/seq/references/Homo_sapiens_assembly38/v0/Homo_sapiens_assembly38.fasta",
                "/opt/edico/config/sample_cross_contamination_resource_hg38.vcf");

        private final String name;
        private final String path;
        private final String haplotypeDatabase;
        private final String fasta;
        private final String contamFile;

        private final static Map<String, ReferenceGenome> MAP_PATH_TO_REF = new HashMap<>();

        ReferenceGenome(String name, String path, String haplotypeDatabase, String fasta, String contamFile) {
            this.name = name;
            this.path = path;
            this.haplotypeDatabase = haplotypeDatabase;
            this.fasta = fasta;
            this.contamFile = contamFile;
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
