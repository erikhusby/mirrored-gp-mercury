/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2013 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.common.BaseSplitter;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.SystemRouter;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.ControlDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VarioskanParserTest;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.ReworkDetail;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent_;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.SectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.run.FlowcellDesignation;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.sample.Control;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.presentation.MessageReporter;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

//import com.jprofiler.api.agent.Controller;

@Test(groups = TestGroups.FIXUP)
public class LabBatchFixUpTest extends Arquillian {

    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s");

    @Inject
    private LabBatchDao labBatchDao;

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private LabBatchEjb labBatchEjb;

    @Inject
    private UserTransaction userTransaction;

    @Inject
    private ControlDao controlDao;

    @Inject
    private SampleDataFetcher sampleDataFetcher;

    @Inject
    private UserBean userBean;

    @Inject
    private BSPUserList bspUserList;

    @Inject
    private MercurySampleDao mercurySampleDao;

    @Inject
    private SystemRouter systemRouter;

    @Inject
    private LabEventDao labEventDao;

    @Inject
    private JiraService jiraService;

    @Inject
    private UserTransaction utx;

    // Use (RC, "rc"), (PROD, "prod") to push the backfill to RC and production respectively.
    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @Test(enabled = false)
    public void gplim4393removeSamplesFromLcset() throws Exception {
        userBean.loginOSUser();

        LabBatch labBatch = labBatchDao.findByName("LCSET-9978");
        List<String> samplesToRemove = Arrays.asList("SM-9J5HB", "SM-9J5HC");
        removeSamples(samplesToRemove, labBatch);

        labBatchDao.persist(new FixupCommentary("GPLIM-4393: Remove samples from LCSET-9978"));
    }

    @Test(enabled = false)
    public void gplim5572removeSamplesFromLcset() throws Exception {
        removeSamplesFromLcset("RemoveFromLCSET.txt");
    }

    /**
     * Remove samples from LCSET(s)
     * Expected format is
     * <pre>
     * fixup commentary
     * LCSET-1  SM-1    SM-2    SM-3
     * LCSET-2  SM-4    SM-5    SM-6
     * </pre>
     * Assertions will fail if any samples to be removed are not present in the LCSET
     * @param testDataFile
     * @throws IOException
     */
    private void removeSamplesFromLcset(String testDataFile) throws IOException {
        userBean.loginOSUser();

        List<String> fixupLines = IOUtils.readLines(VarioskanParserTest.getTestResource(testDataFile));
        Assert.assertTrue(CollectionUtils.size(fixupLines) >= 2, String.format("The file %s has no content.", testDataFile));
        String fixupReason = fixupLines.get(0).trim();
        Assert.assertTrue(StringUtils.isNotBlank(fixupReason), "A fixup reason is necessary in order to record the fixup.");

        List<String> lcsetData = fixupLines.subList(1, fixupLines.size());
        Assert.assertTrue(CollectionUtils.isNotEmpty(lcsetData), "No data found in file.");
        ImmutableListMultimap<String, String> lcsetToSamples =
            lcsetData.stream().collect(
                ImmutableListMultimap.flatteningToImmutableListMultimap(
                    key -> key.split("\\s+")[0],
                    values -> {
                        List<String> split = Arrays.asList(values.split("\\s+"));
                        return split.subList(1, split.size()).stream();
                    }));

        ArrayListMultimap<String, BucketEntry> bucketEntriesToRemove = ArrayListMultimap.create();
        labBatchDao.findByListIdentifier(new ArrayList<>(lcsetToSamples.keySet()))
            .forEach(labBatch -> {
                String businessKey = labBatch.getBusinessKey();
                labBatch.getBucketEntries().stream()
                    .filter(bucketEntry -> bucketEntry.getLabVessel().getMercurySamples().stream()
                        .anyMatch(mercurySample -> lcsetToSamples.get(businessKey).contains(mercurySample.getSampleKey())))
                    .forEach(bucketEntriesToRemove.get(businessKey)::add);
            });

        // Validate that the fix-up has not already been run
        lcsetToSamples.asMap().keySet().forEach(key -> {
            Assert.assertEquals(lcsetToSamples.get(key).size(), bucketEntriesToRemove.get(key).size());
        });

        bucketEntriesToRemove.asMap().forEach((batchName, bucketEntries) -> {
            List<Long> bucketEntryIds =
                bucketEntries.stream().map(BucketEntry::getBucketEntryId).collect(Collectors.toList());

            String bucketName = bucketEntries.iterator().next().getBucket().getBucketDefinitionName();
            try {
                labBatchEjb
                    .updateLabBatch(batchName, Collections.emptyList(), Collections.emptyList(), bucketEntryIds,
                        bucketName, MessageReporter.UNUSED, Collections.emptyList());
            } catch (ValidationException | IOException e) {
                Assert.fail(e.getMessage(), e);
            }
        });

        labBatchDao.persist(new FixupCommentary(fixupReason));
    }

    private List<LabBatchStartingVessel> removeSamples(List<String> sampleNames, LabBatch labBatch) {
        List<LabBatchStartingVessel> vesselsToRemove = new ArrayList<>();
        List<String> sampleNamesFound = new ArrayList<>();
        for (LabBatchStartingVessel startingVessel : labBatch.getLabBatchStartingVessels()) {
            Set<SampleInstanceV2> sampleInstances = startingVessel.getLabVessel().getSampleInstancesV2();
            for (SampleInstanceV2 sampleInstance : sampleInstances) {
                String sample = sampleInstance.getRootOrEarliestMercurySampleName();
                if (sampleNames.contains(sample)) {
                    System.out.println("Removing " + sample + " from " + labBatch.getBatchName());
                    vesselsToRemove.add(startingVessel);
                    sampleNamesFound.add(sample);
                }
            }
        }
        Assert.assertTrue(sampleNamesFound.containsAll(sampleNames), labBatch.getBatchName() + " does not contain " +
                StringUtils.join(CollectionUtils.subtract(sampleNames, sampleNamesFound), ", "));
        for (LabBatchStartingVessel vesselToRemove : vesselsToRemove) {
            labBatch.getLabBatchStartingVessels().remove(vesselToRemove);
            vesselToRemove.getLabVessel().getLabBatchStartingVessels().remove(vesselToRemove);
        }
        return vesselsToRemove;
    }

    /**
     * Removes samples from Jira. Note: this will not remove rework!!!
     */
    private void removeSamplesFromJira(List<String> sampleNames, LabBatch batch, String fixupReason) {
        try {
            JiraIssue issue = batch.getJiraTicket().getJiraDetails();
            String issueField = (String) issue.getField(LabBatch.TicketFields.GSSR_IDS.getName());
            List<String> samplesInJira =
                Arrays.stream(issueField.split("\n"))
                    .filter(sample -> !sampleNames.contains(sample))
                    .collect(Collectors.toList());

            String joinedSamples = String.join("\n", samplesInJira);

            Collection<CustomField> customFieldList = new LinkedList<>();
            Map<String, CustomFieldDefinition> customFieldMap = issue
                .getCustomFields(LabBatch.TicketFields.GSSR_IDS.getName(),
                    LabBatch.TicketFields.NUMBER_OF_SAMPLES.getName());

            int numSamples = samplesInJira.size();
            customFieldList.add(new CustomField(customFieldMap, LabBatch.TicketFields.GSSR_IDS, joinedSamples));
            customFieldList.add(new CustomField(customFieldMap, LabBatch.TicketFields.NUMBER_OF_SAMPLES, numSamples));
            issue.updateIssue(customFieldList);
            issue.addComment(
                String.format("Removed %d samples because \"%s\". The samples removed were:\n%s", sampleNames.size(), fixupReason,
                    sampleNames.stream().collect(Collectors.joining("\n"))));

        } catch (IOException e) {
            Assert.fail(e.getMessage(), e);
        }
    }

    @Test(enabled = false)
    public void updateNullLabBatchType() {
        final List<LabBatch> nullTypes = labBatchDao.findList(LabBatch.class, LabBatch_.labBatchType, null);
        List<LabBatch> fixedBatches = new ArrayList<>(nullTypes.size());
        for (LabBatch nullType : nullTypes) {
            if (nullType.getLabBatchType() != null) {
                throw new IllegalStateException(
                        "wait, why did the dao return me? " + nullType.getLabBatchId() + " " + nullType
                                .getBatchName() + " " + nullType.getLabBatchType());

            }
            if (nullType.getBatchName().startsWith("BP-")) {
                nullType.setLabBatchType(LabBatch.LabBatchType.BSP);
            } else if (nullType.getBatchName().startsWith("LCSET-")) {
                nullType.setLabBatchType(LabBatch.LabBatchType.WORKFLOW);
            } else if (nullType.getBatchName().startsWith("EX-")) {
                nullType.setLabBatchType(LabBatch.LabBatchType.SAMPLES_IMPORT);
            } else if (nullType.getBatchName().startsWith("SK-")) {
                nullType.setLabBatchType(LabBatch.LabBatchType.SAMPLES_RECEIPT);
            } else {
                throw new IllegalStateException(
                        "I don't know what type of lab batch this is! " + nullType.getLabBatchId() + " " + nullType
                                .getBatchName());
            }
        }
        if (!fixedBatches.isEmpty()) {
            labBatchDao.persistAll(fixedBatches);
        }

    }

    @Test(enabled = false)
    public void updateStartingLabBatches() {
        List<LabBatch> allBatches = labBatchDao.findAll(LabBatch.class);
        List<LabBatch> fixedBatches = new ArrayList<>(allBatches.size());
        for (LabBatch batch : allBatches) {
            Set<LabVessel> oldVessels = batch.getStartingLabVessels();
            for (LabVessel vessel : oldVessels) {
                batch.addLabVessel(vessel);
            }
            fixedBatches.add(batch);
        }
        if (!fixedBatches.isEmpty()) {
            labBatchDao.persistAll(fixedBatches);
        }
    }

    /**
     * LCSET-3792 is supposed to be routed to Mercury, but it was created on July 24th, so change to
     * July 25th.
     */
    @Test(enabled = false)
    public void updateLcset3792ToMercury() {
        LabBatch labBatch = labBatchDao.findByName("LCSET-3792");
        GregorianCalendar gregorianCalendar = new GregorianCalendar(2013, 6, 25, 1, 1, 1);
        labBatch.setCreatedOn(gregorianCalendar.getTime());
        labBatchDao.flush();
    }

    /** Rename Exome Express to Agilent Exome Express. */
    @Test(enabled = false)
    public void updateWorkflowName() {
        List<LabBatch> updates = new ArrayList<>();
        for (LabBatch batch : labBatchDao.findAll(LabBatch.class)) {
            if ("Exome Express".equals(batch.getWorkflowName())) {
                batch.setWorkflow(Workflow.AGILENT_EXOME_EXPRESS);
                updates.add(batch);
            }
        }
        if (!updates.isEmpty()) {
            labBatchDao.persistAll(updates);
        }
    }

    @Test(enabled = false)
    public void fixupGplim2355() {
        List<String> batchNames = new ArrayList<>();
        batchNames.add("EX-41050");
        batchNames.add("EX-41052");
        batchNames.add("EX-41054");
        batchNames.add("EX-41056");
        batchNames.add("EX-41058");
        batchNames.add("EX-41060");
        batchNames.add("EX-41062");
        batchNames.add("EX-41066");
        batchNames.add("EX-41068");
        for (String batchName : batchNames) {
            LabBatch labBatch = labBatchDao.findByName(batchName);
            labBatch.setBatchName(batchName + "x");
        }
        labBatchDao.flush();
    }

    /**
     * Auto-bucketing failed, so samples were added as reworks, but getSampleInstances doesn't handle this.
     */
    @Test(enabled = false)
    public void fixupIpi61025() {
        LabBatch labBatch = labBatchDao.findByName("LCSET-4828");
        for (BucketEntry bucketEntry : labBatch.getBucketEntries()) {
            ReworkDetail reworkDetail = bucketEntry.getReworkDetail();
            if (reworkDetail != null) {
                if (!reworkDetail.getComment().equals("rework from shearing")) {
                    bucketEntry.setReworkDetail(null);
                }
            }
        }
        labBatchDao.flush();
    }

    @Test(enabled = false)
    public void removeSamplesFromLcset() {
        LabBatch lcset = labBatchDao.findByName("LCSET-5332");
        Set<LabBatchStartingVessel> vesselsToRemoveFromBatch = new HashSet<>();
        Set<String> samplesToRemove = new HashSet<>();
        samplesToRemove.add("SM-5R5KW");
        samplesToRemove.add("SM-5U3X7");
        samplesToRemove.add("SM-5U3X9");

        for (LabBatchStartingVessel startingVessel : lcset.getLabBatchStartingVessels()) {
            // Originally run using SampleInstance V1 but changed after its removal
            Set<SampleInstanceV2> sampleInstances = startingVessel.getLabVessel().getSampleInstancesV2();
            for (SampleInstanceV2 sampleInstance : sampleInstances) {
                String sample = sampleInstance.getRootOrEarliestMercurySampleName();
                if (samplesToRemove.contains(sample)) {
                    vesselsToRemoveFromBatch.add(startingVessel);
                }
            }
        }
        int numStartingVessels = lcset.getLabBatchStartingVessels().size();
        for (LabBatchStartingVessel vesselToRemove : vesselsToRemoveFromBatch) {
            lcset.getLabBatchStartingVessels().remove(vesselToRemove);
            vesselToRemove.getLabVessel().getLabBatchStartingVessels().remove(vesselToRemove);
        }
        int numVesselsAtEnd = lcset.getLabBatchStartingVessels().size();
        System.out.println("Removed " + (numStartingVessels - numVesselsAtEnd) + " samples from " + lcset.getBatchName());
        labBatchDao.flush();
    }

    @Test(enabled = false)
    public void fixupGplim2921() {
        LabBatch labBatch = labBatchDao.findByName("LCSET-5704");
        LabVessel labVessel = labVesselDao.findByIdentifier("0156349196");
        ProductOrder productOrder = productOrderDao.findByBusinessKey("PDO-3974");
        BucketEntry bucketEntry = new BucketEntry(labVessel, productOrder, BucketEntry.BucketEntryType.PDO_ENTRY);
        bucketEntry.setStatus(BucketEntry.Status.Archived);
        labBatch.addBucketEntry(bucketEntry);
        labBatchDao.flush();
    }

    @Test(enabled = false)
    public void fixupGplim3547() {
        userBean.loginOSUser();
        try {
            userTransaction.begin();
            String batchName = "LCSET-7158";
            LabBatch labBatch = labBatchDao.findByName(batchName);

            Map<String, String> mapOldSampleIdToNew = new HashMap<>();
            mapOldSampleIdToNew.put("SM-9LIRY", "SM-9MKCY");
            mapOldSampleIdToNew.put("SM-9LIRZ", "SM-9MKCZ");
            mapOldSampleIdToNew.put("SM-9LIS1", "SM-9MKD1");
            mapOldSampleIdToNew.put("SM-9LIS2", "SM-9MKD2");
            mapOldSampleIdToNew.put("SM-9LIS3", "SM-9MKD3");
            Map<String, MercurySample> mapIdToMercurySample = mercurySampleDao.findMapIdToMercurySample(
                    mapOldSampleIdToNew.values());

            int foundCount = 0;
            Set<LabVessel> oldLabVessels = new HashSet<>();
            Set<LabVessel> newLabVessels = new HashSet<>();

            // Change bucket entries
            for (BucketEntry bucketEntry : labBatch.getBucketEntries()) {
                Set<SampleInstanceV2> sampleInstancesV2 = bucketEntry.getLabVessel().getSampleInstancesV2();
                Assert.assertEquals(sampleInstancesV2.size(), 1);
                SampleInstanceV2 sampleInstanceV2 = sampleInstancesV2.iterator().next();
                String newMercurySampleId = mapOldSampleIdToNew.get(sampleInstanceV2.getNearestMercurySampleName());
                if (newMercurySampleId != null) {
                    MercurySample newMercurySample = mapIdToMercurySample.get(newMercurySampleId);
                    oldLabVessels.add(bucketEntry.getLabVessel());
                    Assert.assertEquals(newMercurySample.getLabVessel().size(), 1);
                    LabVessel newLabVessel = newMercurySample.getLabVessel().iterator().next();
                    bucketEntry.setLabVessel(newLabVessel);
                    newLabVessels.add(newLabVessel);
                    foundCount++;
                }
            }
            Assert.assertEquals(foundCount, 5);

            // Change reworks
            for (LabVessel oldLabVessel : oldLabVessels) {
                Assert.assertTrue(labBatch.getReworks().remove(oldLabVessel));
            }
            labBatch.getReworks().addAll(newLabVessels);

            labBatchDao.persist(new FixupCommentary("GPLIM-3547 change bucket entries for " + batchName));
            labBatchDao.flush();
            userTransaction.commit();
        } catch (NotSupportedException | SystemException | RollbackException | HeuristicMixedException |
                HeuristicRollbackException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Generate a file to back populate controls into LCSETs.
     */
    @Test(enabled = false)
    public void fixupGplim3442GenFile() {
//        Controller.startCPURecording(true);
//        Controller.startProbeRecording(Controller.PROBE_NAME_JDBC, true);

        Map<String, List<LabEventType>> mapBucketToEvents = new HashMap<>();
        mapBucketToEvents.put("Pico/Plating Bucket", Collections.singletonList(LabEventType.SHEARING_TRANSFER));
        mapBucketToEvents.put("Shearing Bucket", Collections.singletonList(LabEventType.SHEARING_TRANSFER));
        mapBucketToEvents.put("ICE Bucket", Collections.singletonList(LabEventType.ICE_POOLING_TRANSFER));
        mapBucketToEvents.put("Hybridization Bucket", Collections.singletonList(LabEventType.HYBRIDIZATION));
        mapBucketToEvents.put("Pooling Bucket", Collections.singletonList(LabEventType.POOLING_TRANSFER));

        List<Control> controls = controlDao.findAllActive();
        List<String> controlAliases = new ArrayList<>();
        for (Control control : controls) {
            controlAliases.add(control.getCollaboratorParticipantId());
        }

        List<LabBatch> workflowLabBatches = labBatchDao.findByType(LabBatch.LabBatchType.WORKFLOW);
        List<String> batchNames = new ArrayList<>();
        for (LabBatch workflowLabBatch : workflowLabBatches) {
            batchNames.add(workflowLabBatch.getBatchName());
        }
        labBatchDao.clear();
        Collections.sort(batchNames);
        List<Collection<String>> listListBatchNames = BaseSplitter.split(batchNames, 20);

        for (Collection<String> listBatchName : listListBatchNames) {
            workflowLabBatches = labBatchDao.findByListIdentifier(new ArrayList<>(listBatchName));
            for (LabBatch workflowLabBatch : workflowLabBatches) {
                int bucketEntryCount = workflowLabBatch.getBucketEntries().size();
                Set<LabVessel> labVessels = new HashSet<>();
                for (BucketEntry bucketEntry : workflowLabBatch.getBucketEntries()) {
                    LabVessel bucketEntryLabVessel = bucketEntry.getLabVessel();
                    if (bucketEntry.getBucket() == null) {
                        continue;
                    }
                    String bucketDefinitionName = bucketEntry.getBucket().getBucketDefinitionName();
                    List<LabEventType> labEventTypes = mapBucketToEvents.get(bucketDefinitionName);
                    if (labEventTypes == null) {
                        throw new RuntimeException("No event types for bucket " + bucketDefinitionName);
                    }
                    labVessels.addAll(getEventLabVessels(labEventTypes, bucketEntryLabVessel));
                }
                boolean found = false;
                if (!labVessels.isEmpty()) {
                    List<String> sampleNames = new ArrayList<>();
                    for (LabVessel labVessel : labVessels) {
                        for (SampleInstanceV2 sampleInstanceV2 : labVessel.getSampleInstancesV2()) {
                            sampleNames.add(sampleInstanceV2.getEarliestMercurySampleName());
                        }
                    }
                    Map<String, SampleData> mapSampleNameToData = sampleDataFetcher.fetchSampleData(sampleNames);
                    for (LabVessel labVessel : labVessels) {
                        int foundCount;
                        if (OrmUtil.proxySafeIsInstance(labVessel, BarcodedTube.class)) {
                            foundCount = labVessels.size();
                        } else if (OrmUtil.proxySafeIsInstance(labVessel, TubeFormation.class)) {
                            foundCount = labVessel.getContainerRole().getContainedVessels().size();
                        } else {
                            throw new RuntimeException("Unexpected class " + labVessel.getClass());
                        }
                        System.out.println("For " + workflowLabBatch.getBatchName() +
                                ", found lab vessels count = " + foundCount +
                                ", bucket entries size = " + bucketEntryCount);
                        int difference = foundCount - bucketEntryCount;
                        if (difference < 0 || difference > 4) {
                            System.out.println("Difference out of range: " + difference);
                        }
                        if (OrmUtil.proxySafeIsInstance(labVessel, BarcodedTube.class)) {
                            found = processTube(workflowLabBatch, found, (BarcodedTube) labVessel,
                                    mapSampleNameToData, controlAliases);
                        } else if (OrmUtil.proxySafeIsInstance(labVessel, TubeFormation.class)) {
                            for (LabVessel containedVessel : labVessel.getContainerRole().getContainedVessels()) {
                                found = processTube(workflowLabBatch, found, (BarcodedTube) containedVessel,
                                        mapSampleNameToData, controlAliases);
                            }
                        }
                    }
                }
                if (!found) {
                    System.out.println("Failed to find any controls for " + workflowLabBatch.getBatchName());
                }
            }
            System.out.println("Clear session");
            labBatchDao.clear();
        }
//        Controller.stopProbeRecording(Controller.PROBE_NAME_JDBC);
//        Controller.stopCPURecording();
    }

    private Set<LabVessel> getEventLabVessels(List<LabEventType> labEventTypes, LabVessel bucketEntryLabVessel) {
        Set<LabVessel> labVessels = new HashSet<>();
        for (LabEvent labEvent : bucketEntryLabVessel.getTransfersFrom()) {
            if (labEventTypes.contains(labEvent.getLabEventType())) {
                labVessels.addAll(labEvent.getSourceLabVessels());
            }
        }
        if (labVessels.isEmpty()) {
            Map<LabEvent, Set<LabVessel>> vesselsForLabEventTypes =
                    bucketEntryLabVessel.findVesselsForLabEventTypes(labEventTypes,
                            Arrays.asList(TransferTraverserCriteria.TraversalDirection.Descendants), false);
            for (Set<LabVessel> vessels : vesselsForLabEventTypes.values()) {
                labVessels.addAll(vessels);
            }
        }
        return labVessels;
    }

    private boolean processTube(LabBatch workflowLabBatch, boolean found,
            BarcodedTube labVessel, Map<String, SampleData> mapSampleNameToData, List<String> controlAliases) {
        Set<SampleInstanceV2> sampleInstances = labVessel.getSampleInstancesV2();
        if (sampleInstances.size() == 1) {
            SampleInstanceV2 sampleInstance = sampleInstances.iterator().next();
            SampleData sampleData = mapSampleNameToData.get(sampleInstance.getEarliestMercurySampleName());
            if (controlAliases.contains(sampleData.getCollaboratorParticipantId())) {
                found = true;
                System.out.println("Add " + labVessel.getLabel() + " to " +
                        workflowLabBatch.getBatchName() + " " +
                        sampleInstance.getEarliestMercurySampleName());
            }
        }
        return found;
    }

    /**
     * Read a file to back populate controls into LCSETs.
     */
    @Test(enabled = false)
    public void fixupGplim3442() {
        userBean.loginOSUser();
        InputStream testSpreadSheetInputStream = VarioskanParserTest.getTestResource("BackfillInput.txt");
        BufferedReader reader = new BufferedReader(new InputStreamReader(testSpreadSheetInputStream));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] words = line.split("\\s+");
                if (words[0].startsWith("#")) {
                    continue;
                }
                LabVessel labVessel = labVesselDao.findByIdentifier(words[0]);
                LabBatch labBatch = labBatchDao.findByName(words[1]);
                labBatch.addLabVessel(labVessel);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        labBatchDao.persist(new FixupCommentary("GPLIM-3442 back fill controls into LCSETs"));
        labBatchDao.flush();
    }

    /**
     * Tubes were put in both LCSET-7015 and 7016. 7016 was then cancelled. Need to unlink the tubes from 7016.
     *
     * This test should result in audit changes to Bucket and LabBatch (both no-op changes),
     * deletes in BucketEntry and LabBatchStartingVessel, change to BarcodedTube, and add to FixupCommentary.
     */
    @Test(enabled = false)
    public void gplim3493unlinkLcset7016() throws Exception {
        userBean.loginOSUser();

        final LabBatch undesiredLcset = labBatchDao.findByName("LCSET-7016");
        Assert.assertNotNull(undesiredLcset);

        for (Iterator<LabBatchStartingVessel> lbsvIter = undesiredLcset.getLabBatchStartingVessels().iterator();
             lbsvIter.hasNext(); ) {
            LabBatchStartingVessel labBatchStartingVessel = lbsvIter.next();

            // Removing the undesired bucket entry should leave starting vessel with another bucket entry.
            BucketEntry undesiredBucketEntry = null;
            boolean foundOtherBucketEntry = false;
            for (BucketEntry bucketEntry : labBatchStartingVessel.getLabVessel().getBucketEntries()) {
                if (undesiredLcset.getBatchName().equals(bucketEntry.getLabBatch().getBatchName())) {
                    undesiredBucketEntry = bucketEntry;
                } else {
                    foundOtherBucketEntry = true;
                }
            }
            if (undesiredBucketEntry != null) {
                if (foundOtherBucketEntry) {
                    System.out.println("Removing bucketEntry " + undesiredBucketEntry.getBucketEntryId() +
                                       " and batch starting vessel " + labBatchStartingVessel.getBatchStartingVesselId());

                    // Unlinks the bucket entry from the vessel.
                    labBatchStartingVessel.getLabVessel().getBucketEntries().remove(undesiredBucketEntry);
                    // Unlinks the bucket entry from the batch.
                    undesiredLcset.getBucketEntries().remove(undesiredBucketEntry);
                    // Unlinks the starting vessel from the batch.
                    lbsvIter.remove();
                } else {
                    System.out.println("Keeping vessel's only bucketEntry " + undesiredBucketEntry.getBucketEntryId());
                }
            }
        }

        labBatchDao.persist(new FixupCommentary("GPLIM-3493 unlink all tubes from " + undesiredLcset.getBatchName()));
        labBatchDao.flush();
    }


    @Test(enabled = false)
    public void fixupQual680(){
        createFctsWithoutLcset("AB56073486", IlluminaFlowcell.FlowcellType.HiSeq2500Flowcell, new BigDecimal("15"),
                IlluminaFlowcell.FlowcellType.HiSeq2500Flowcell.getVesselGeometry().getVesselPositions(),
                "QUAL-680 create FCT tickets without LCSET");
    }

    /**
     * If a denature tube contains only positive controls, it doesn't have an LCSET (until control inference is
     * removed), so the Create FCT Ticket page can't be used.  This method is adapted from that ActionBean.
     */
    private void createFctsWithoutLcset(String startingTubeLabel, IlluminaFlowcell.FlowcellType selectedType,
                                        BigDecimal loadingConc, VesselPosition[] lanes, String reason) {
        try {
            userBean.loginOSUser();
            userTransaction.begin();

            LabBatch.LabBatchType batchType = selectedType.getBatchType();
            CreateFields.IssueType issueType = selectedType.getIssueType();

            LabVessel startingTube = labVesselDao.findByIdentifier(startingTubeLabel);
            LabBatch batch = new LabBatch(startingTubeLabel + " FCT ticket",
                    Collections.singletonList(new LabBatch.VesselToLanesInfo(Arrays.asList(lanes), loadingConc,
                            startingTube, null, null, Collections.<FlowcellDesignation>emptyList())),
                    batchType, selectedType);
            batch.setBatchDescription(batch.getBatchName());
            labBatchEjb.createLabBatch(batch, userBean.getLoginUserName(), issueType);

            labBatchDao.persist(new FixupCommentary(reason));
            labBatchDao.flush();
            userTransaction.commit();
            System.out.println("Created " + batch.getBatchName());

        } catch (NotSupportedException | SystemException | HeuristicMixedException | HeuristicRollbackException |
                RollbackException e) {
            throw new RuntimeException(e);
        }
    }

    /** Removes BSP Lab batches BP-71077 and BP-71083 along with their MercurySamples and BarcodedTubes. */
    @Test(enabled = false)
    public void fixupGplim3764() throws Exception {
        userBean.loginOSUser();
        userTransaction.begin();

        List<LabBatch> batches = labBatchDao.findByListIdentifier(Arrays.asList(new String[]{"BP-71077", "BP-71083"}));
        Assert.assertEquals(batches.size(), 2);
        final int vesselCount = 192;

        List<LabBatchStartingVessel> labBatchStartingVessels = labBatchDao.findListByList(LabBatchStartingVessel.class,
                LabBatchStartingVessel_.labBatch, batches);
        Assert.assertEquals(labBatchStartingVessels.size(), vesselCount);

        Set<LabVessel> labVessels = new HashSet<>();
        Set<MercurySample> mercurySamples = new HashSet<>();
        for (LabBatchStartingVessel labBatchStartingVessel : labBatchStartingVessels) {
            labVessels.add(labBatchStartingVessel.getLabVessel());
            mercurySamples.addAll(labBatchStartingVessel.getLabVessel().getMercurySamples());
        }
        Assert.assertEquals(labVessels.size(), vesselCount);
        Assert.assertEquals(mercurySamples.size(), vesselCount);

        for (MercurySample mercurySample : mercurySamples) {
            labBatchDao.remove(mercurySample);
        }
        for (LabVessel vessel : labVessels) {
            // This also removes the labBatchStartingVessel.
            labBatchDao.remove(vessel);
        }
        for (LabBatch labBatch : batches) {
            System.out.println("Deleted batch " + labBatch.getBatchName());
            labBatchDao.remove(labBatch);
        }
        labBatchDao.persist(new FixupCommentary("GPLIM-3764 delete batches, vessels, and samples created by BSP test"));
        labBatchDao.flush();
        userTransaction.commit();
    }

    /**
     * Samples were changed in BSP from Eppendorf 1.5 (no 2D barcode) to Matrix tubes.
     */
    @Test(enabled = false)
    public void fixupSupport1455() throws Exception {
        userBean.loginOSUser();
        userTransaction.begin();
        LabBatch labBatch = labBatchDao.findByName("LCSET-8579");
        Map<String, String> mapOldBarcodeToNew = new HashMap<String, String>(){{
            put("SM-ATXQU", "1113558682");
            put("SM-ATXQV", "1113558673");
            put("SM-ATXQW", "1113558664");
            put("SM-ATXQX", "1113559211");
        }};
        for (LabBatchStartingVessel labBatchStartingVessel : labBatch.getLabBatchStartingVessels()) {
            String newBarcode = mapOldBarcodeToNew.get(labBatchStartingVessel.getLabVessel().getLabel());
            if (newBarcode != null) {
                LabVessel newLabVessel = labVesselDao.findByIdentifier(newBarcode);
                System.out.println("Replacing " + labBatchStartingVessel.getLabVessel().getLabel() + " with " +
                        newLabVessel.getLabel());
                labBatchStartingVessel.setLabVessel(newLabVessel);
            }
        }

        labBatchDao.persist(new FixupCommentary("SUPPORT-1455 replace vessels in batch"));
        labBatchDao.flush();
        userTransaction.commit();
    }
    /**
     * LCSET-8578 & LCSET-8579 were created by an apparent button double click.  Delete LCSET-8578, to
     * avoid problems with LCSET inference in the future.
     */
    @Test(enabled = false)
    public void fixupSupport1456() throws Exception {
        userBean.loginOSUser();
        userTransaction.begin();

        // From database queries, found that there were batch_starting_vessels for this LCSET, but no bucket entries.
        // Ran the test with rollback, and verified with SQL logging that batch_starting_vessels orphans were removed.
        LabBatch labBatch = labBatchDao.findByName("LCSET-8578");
        System.out.println("Deleting " + labBatch.getBatchName());
        labBatchDao.remove(labBatch.getJiraTicket());
        labBatchDao.remove(labBatch);

        labBatchDao.persist(new FixupCommentary("SUPPORT-1456 delete duplicate LCSET"));
        labBatchDao.flush();
        userTransaction.commit();
    }

    /**
     * Samples were changed in BSP from Eppendorf 1.5 (no 2D barcode) to Matrix tubes.  Previously fixed
     * LabBatchStartingVessels, now need to fix BucketEntries too.
     */
    @Test(enabled = false)
    public void fixupSupport1455Part2() throws Exception {
        userBean.loginOSUser();
        userTransaction.begin();
        LabBatch labBatch = labBatchDao.findByName("LCSET-8579");
        Map<String, String> mapOldBarcodeToNew = new HashMap<String, String>(){{
            put("SM-ATXQU", "1113558682");
            put("SM-ATXQV", "1113558673");
            put("SM-ATXQW", "1113558664");
            put("SM-ATXQX", "1113559211");
        }};
        for (BucketEntry bucketEntry : labBatch.getBucketEntries()) {
            String newBarcode = mapOldBarcodeToNew.get(bucketEntry.getLabVessel().getLabel());
            if (newBarcode != null) {
                LabVessel newLabVessel = labVesselDao.findByIdentifier(newBarcode);
                System.out.println("Replacing " + bucketEntry.getLabVessel().getLabel() + " with " +
                        newLabVessel.getLabel());
                bucketEntry.setLabVessel(newLabVessel);
            }
        }

        labBatchDao.persist(new FixupCommentary("SUPPORT-1455 replace vessels in batch"));
        labBatchDao.flush();
        userTransaction.commit();
    }

    @Test(enabled = false)
    public void fixupSupport1592() throws Exception{
        List<String> tubeBarcodes = Arrays.asList(
                "1125638276",
                "1125638278",
                "1125638301",
                "1125638308",
                "1125638324",
                "1125638325",
                "1125638326",
                "1125638328",
                "1125638329",
                "1125638331",
                "1125638332",
                "1125638349",
                "1125638350",
                "1125638351",
                "1125638352",
                "1125638354",
                "1125638356");

        changeBucketEntriesToAliquots(tubeBarcodes, 1191104L, "LCSET-8673", "SUPPORT-1592");
    }

    /**
     * Starting with a set of tubes, looks for ancestors in the given LCSET, then changes bucket entries to the
     * aliquots created by the given event.  This is intended to fix the case where a root / stock tube appears in a
     * Mercury LCSET, but an aliquot is later used in a Squid LCSET, causing a routing error.  Moving the bucket entry
     * from the root to the Mercury aliquot causes the Mercury LCSET to be invisible when routing the Squid tubes.
     * @param childBarcodes list of aliquots that are children of the bucket entries to be changed (could be siblings
     *                      of the Mercury aliquots)
     * @param eventId id of daughter plate transfer that creates Mercury aliquots
     * @param lcset Mercury LCSET
     * @param ticket for FixupCommentary
     */
    private void changeBucketEntriesToAliquots(List<String> childBarcodes, long eventId, String lcset, String ticket)
            throws Exception {
        userBean.loginOSUser();
        userTransaction.begin();
        Assert.assertEquals(systemRouter.routeForVesselBarcodes(childBarcodes), SystemRouter.System.MERCURY);

        LabBatch labBatch = labBatchDao.findByBusinessKey(lcset);
        Map<String, LabVessel> mapSourceToTarget = new HashMap<>();

        // Find ancestor bucket entries for given LCSET
        Set<BucketEntry> bucketEntries = new HashSet<>();
        Map<String, LabVessel> mapBarcodeToVessel = labVesselDao.findByBarcodes(childBarcodes);
        for (Map.Entry<String, LabVessel> stringLabVesselEntry : mapBarcodeToVessel.entrySet()) {
            for (LabVessel labVessel : stringLabVesselEntry.getValue().getAncestorVessels()) {
                for (BucketEntry bucketEntry : labVessel.getBucketEntries()) {
                    if (bucketEntry.getLabBatch().getBatchName().equals(lcset)) {
                        bucketEntries.add(bucketEntry);
                        break;
                    }
                }
            }
        }

        // Find aliquots for given transfer, change bucket entries
        for (BucketEntry bucketEntry : bucketEntries) {
            for (LabVessel container : bucketEntry.getLabVessel().getContainers()) {
                for (SectionTransfer sectionTransfer : container.getContainerRole().getSectionTransfersFrom()) {
                    if (sectionTransfer.getLabEvent().getLabEventId() == eventId) {
                        VesselPosition vesselPosition = container.getContainerRole().getPositionOfVessel(
                                bucketEntry.getLabVessel());
                        LabVessel targetTube = sectionTransfer.getTargetVesselContainer().getVesselAtPosition(
                                vesselPosition);
                        mapSourceToTarget.put(bucketEntry.getLabVessel().getLabel(), targetTube);
                        System.out.println("Changing bucket entry " + bucketEntry.getBucketEntryId() + " from " +
                                bucketEntry.getLabVessel().getLabel() + " to " + targetTube.getLabel());
                        bucketEntry.setLabVessel(targetTube);
                    }
                }
            }
        }
        // Change LabBatchStartingVessels
        for (LabBatchStartingVessel labBatchStartingVessel : labBatch.getLabBatchStartingVessels()) {
            LabVessel targetTube = mapSourceToTarget.get(labBatchStartingVessel.getLabVessel().getLabel());
            if (targetTube != null) {
                System.out.println("Changing lbsv " + labBatchStartingVessel.getBatchStartingVesselId() + " from " +
                        labBatchStartingVessel.getLabVessel().getLabel() + " to " + targetTube.getLabel());
                labBatchStartingVessel.setLabVessel(targetTube);
            }
        }

        labBatchDao.persist(new FixupCommentary(ticket + " change bucket entries to aliquot"));
        labBatchDao.flush();
        labVesselDao.clear();

        Assert.assertEquals(systemRouter.routeForVesselBarcodes(childBarcodes), SystemRouter.System.SQUID);
        userTransaction.commit();
    }

    @Test(enabled = false)
    public void fixupSupport1848() throws Exception{
        List<String> tubeBarcodes = Arrays.asList(
                "1131926708",
                "1131929230",
                "1131929231",
                "1131929232",
                "1131929233",
                "1131929234",
                "1131929235",
                "1131929236",
                "1131929237",
                "1131929246",
                "1131929247",
                "1131929248",
                "1131929249",
                "1131929250",
                "1131929251",
                "1131929252",
                "1131929253",
                "1131929254",
                "1131929255",
                "1131929256",
                "1131929257",
                "1131929258",
                "1131929259",
                "1131929260",
                "1131929261",
                "1131929270",
                "1131929272",
                "1131929273",
                "1131929274",
                "1131929275",
                "1131929276",
                "1131929277",
                "1131929278",
                "1131929279",
                "1131929280",
                "1131929281",
                "1131929282",
                "1131929283",
                "1131929284",
                "1131929285",
                "1131929294",
                "1131929295",
                "1131929296",
                "1131929297",
                "1131929298",
                "1131929299",
                "1131929300",
                "1131929301",
                "1131929302",
                "1131929303",
                "1131929304",
                "1131929305",
                "1131929306",
                "1131929307",
                "1131929308",
                "1131929309",
                "1131929320",
                "1131929321",
                "1131929322",
                "1131929323",
                "1131929324",
                "1131929325");

        changeBucketEntriesToAliquots(tubeBarcodes, 1371144L, "LCSET-9347", "SUPPORT-1848");
    }

    /**
     * The first attempt used the wrong daughter plate event, so change the entries back, based on the log entries.
     */
    @Test(enabled = false)
    public void fixupSupport1848_Try2a() throws Exception {
        userBean.loginOSUser();
        userTransaction.begin();

        List<ImmutableTriple<Long, String, String>> bucketEntries = new ArrayList<>();
        bucketEntries.add(new ImmutableTriple<>(160524L, "0174166000", "1131929255"));
        bucketEntries.add(new ImmutableTriple<>(160518L, "0174166025", "1131929306"));
        bucketEntries.add(new ImmutableTriple<>(160557L, "0174166012", "1131929274"));
        bucketEntries.add(new ImmutableTriple<>(160541L, "0174166035", "1131929258"));
        bucketEntries.add(new ImmutableTriple<>(160517L, "0174166021", "1131929246"));
        bucketEntries.add(new ImmutableTriple<>(160554L, "0174165993", "1131929320"));
        bucketEntries.add(new ImmutableTriple<>(160511L, "0174166028", "1131929301"));
        bucketEntries.add(new ImmutableTriple<>(160549L, "0174166011", "1131929235"));
        bucketEntries.add(new ImmutableTriple<>(160522L, "0174166070", "1131929324"));
        bucketEntries.add(new ImmutableTriple<>(160510L, "0174166004", "1131929248"));
        bucketEntries.add(new ImmutableTriple<>(160551L, "0174166064", "1131929261"));
        bucketEntries.add(new ImmutableTriple<>(160512L, "0174166072", "1131929232"));
        bucketEntries.add(new ImmutableTriple<>(160527L, "0174166071", "1131929284"));
        bucketEntries.add(new ImmutableTriple<>(160505L, "0174166001", "1131929300"));
        bucketEntries.add(new ImmutableTriple<>(160501L, "0174166076", "1131929307"));
        bucketEntries.add(new ImmutableTriple<>(160533L, "0174165988", "1131929251"));
        bucketEntries.add(new ImmutableTriple<>(160536L, "0174166039", "1131929237"));
        bucketEntries.add(new ImmutableTriple<>(160553L, "0174166061", "1131929321"));
        bucketEntries.add(new ImmutableTriple<>(160521L, "0174166026", "1131929236"));
        bucketEntries.add(new ImmutableTriple<>(160559L, "0174166037", "1131929230"));
        bucketEntries.add(new ImmutableTriple<>(160508L, "0174162698", "1131929276"));
        bucketEntries.add(new ImmutableTriple<>(160509L, "0174166049", "1131929297"));
        bucketEntries.add(new ImmutableTriple<>(160555L, "0174166062", "1131929296"));
        bucketEntries.add(new ImmutableTriple<>(160516L, "0174165998", "1131929299"));
        bucketEntries.add(new ImmutableTriple<>(160556L, "0174166036", "1131929259"));
        bucketEntries.add(new ImmutableTriple<>(160506L, "0174162718", "1131929282"));
        bucketEntries.add(new ImmutableTriple<>(160520L, "0174166003", "1131929247"));
        bucketEntries.add(new ImmutableTriple<>(160545L, "0174166016", "1131929305"));
        bucketEntries.add(new ImmutableTriple<>(160513L, "0174166022", "1131929253"));
        bucketEntries.add(new ImmutableTriple<>(160542L, "0174166066", "1131929249"));
        bucketEntries.add(new ImmutableTriple<>(160526L, "0174166074", "1131929275"));
        bucketEntries.add(new ImmutableTriple<>(160560L, "0174166042", "1131929294"));
        bucketEntries.add(new ImmutableTriple<>(160546L, "0174166063", "1131929260"));
        bucketEntries.add(new ImmutableTriple<>(160558L, "0174166017", "1131929250"));
        bucketEntries.add(new ImmutableTriple<>(160562L, "0174165991", "1131929273"));
        bucketEntries.add(new ImmutableTriple<>(160552L, "0174166065", "1131929279"));
        bucketEntries.add(new ImmutableTriple<>(160523L, "0174166052", "1131929302"));
        bucketEntries.add(new ImmutableTriple<>(160519L, "0174166073", "1131929304"));
        bucketEntries.add(new ImmutableTriple<>(160503L, "0174166024", "1131929254"));
        bucketEntries.add(new ImmutableTriple<>(160531L, "0174166027", "1131929323"));
        bucketEntries.add(new ImmutableTriple<>(160547L, "0174166040", "1131929281"));
        bucketEntries.add(new ImmutableTriple<>(160561L, "0174166041", "1131929285"));
        bucketEntries.add(new ImmutableTriple<>(160535L, "0174166015", "1131929322"));
        bucketEntries.add(new ImmutableTriple<>(160530L, "0174166045", "1131929308"));
        bucketEntries.add(new ImmutableTriple<>(160502L, "0174166069", "1131929234"));
        bucketEntries.add(new ImmutableTriple<>(160507L, "0174165999", "1131929277"));
        bucketEntries.add(new ImmutableTriple<>(160515L, "0174166002", "1131926708"));
        bucketEntries.add(new ImmutableTriple<>(160534L, "0174166060", "1131929270"));
        bucketEntries.add(new ImmutableTriple<>(160504L, "0174166075", "1131929283"));
        bucketEntries.add(new ImmutableTriple<>(160539L, "0174166014", "1131929231"));
        bucketEntries.add(new ImmutableTriple<>(160550L, "0174165989", "1131929303"));
        bucketEntries.add(new ImmutableTriple<>(160528L, "0174166050", "1131929298"));
        bucketEntries.add(new ImmutableTriple<>(160543L, "0174165987", "1131929256"));
        bucketEntries.add(new ImmutableTriple<>(160537L, "0174166038", "1131929309"));
        bucketEntries.add(new ImmutableTriple<>(160532L, "0174166059", "1131929295"));
        bucketEntries.add(new ImmutableTriple<>(160548L, "0174166013", "1131929278"));
        bucketEntries.add(new ImmutableTriple<>(160525L, "0174166023", "1131929257"));
        bucketEntries.add(new ImmutableTriple<>(160514L, "0174166051", "1131929233"));
        bucketEntries.add(new ImmutableTriple<>(160529L, "0174166046", "1131929325"));
        bucketEntries.add(new ImmutableTriple<>(160540L, "0174166018", "1131929280"));
        bucketEntries.add(new ImmutableTriple<>(160544L, "0174165992", "1131929272"));
        bucketEntries.add(new ImmutableTriple<>(160538L, "0174165990", "1131929252"));

        for (ImmutableTriple<Long, String, String> bucketEntryTriple : bucketEntries) {
            BucketEntry bucketEntry = labVesselDao.findById(BucketEntry.class, bucketEntryTriple.getLeft());
            Assert.assertEquals(bucketEntry.getLabVessel().getLabel(), bucketEntryTriple.getRight());
            LabVessel labVessel = labVesselDao.findByIdentifier(bucketEntryTriple.getMiddle());
            System.out.println("Change bucket entry " + bucketEntry.getBucketEntryId() + " from " +
                    bucketEntry.getLabVessel().getLabel() + " to " + labVessel.getLabel());
            bucketEntry.setLabVessel(labVessel);
        }

        List<ImmutableTriple<Long, String, String>> lbsvs = new ArrayList<>();
        lbsvs.add(new ImmutableTriple<>(2312483L, "0174166040", "1131929281"));
        lbsvs.add(new ImmutableTriple<>(2312432L, "0174166000", "1131929255"));
        lbsvs.add(new ImmutableTriple<>(2312487L, "0174166018", "1131929280"));
        lbsvs.add(new ImmutableTriple<>(2312497L, "0174166026", "1131929236"));
        lbsvs.add(new ImmutableTriple<>(2312496L, "0174166015", "1131929322"));
        lbsvs.add(new ImmutableTriple<>(2312476L, "0174166013", "1131929278"));
        lbsvs.add(new ImmutableTriple<>(2312436L, "0174166051", "1131929233"));
        lbsvs.add(new ImmutableTriple<>(2312472L, "0174166042", "1131929294"));
        lbsvs.add(new ImmutableTriple<>(2312433L, "0174166038", "1131929309"));
        lbsvs.add(new ImmutableTriple<>(2312456L, "0174166075", "1131929283"));
        lbsvs.add(new ImmutableTriple<>(2312489L, "0174165993", "1131929320"));
        lbsvs.add(new ImmutableTriple<>(2312437L, "0174166062", "1131929296"));
        lbsvs.add(new ImmutableTriple<>(2312480L, "0174165998", "1131929299"));
        lbsvs.add(new ImmutableTriple<>(2312457L, "0174166004", "1131929248"));
        lbsvs.add(new ImmutableTriple<>(2312443L, "0174166023", "1131929257"));
        lbsvs.add(new ImmutableTriple<>(2312474L, "0174166071", "1131929284"));
        lbsvs.add(new ImmutableTriple<>(2312482L, "0174166045", "1131929308"));
        lbsvs.add(new ImmutableTriple<>(2312431L, "0174166052", "1131929302"));
        lbsvs.add(new ImmutableTriple<>(2312423L, "0174166002", "1131926708"));
        lbsvs.add(new ImmutableTriple<>(2312493L, "0174166022", "1131929253"));
        lbsvs.add(new ImmutableTriple<>(2312422L, "0174166011", "1131929235"));
        lbsvs.add(new ImmutableTriple<>(2312479L, "0174166036", "1131929259"));
        lbsvs.add(new ImmutableTriple<>(2312508L, "0174166017", "1131929250"));
        lbsvs.add(new ImmutableTriple<>(2312473L, "0174166041", "1131929285"));
        lbsvs.add(new ImmutableTriple<>(2312460L, "0174166061", "1131929321"));
        lbsvs.add(new ImmutableTriple<>(2312445L, "0174166001", "1131929300"));
        lbsvs.add(new ImmutableTriple<>(2312430L, "0174166035", "1131929258"));
        lbsvs.add(new ImmutableTriple<>(2312427L, "0174166025", "1131929306"));
        lbsvs.add(new ImmutableTriple<>(2312477L, "0174166065", "1131929279"));
        lbsvs.add(new ImmutableTriple<>(2312458L, "0174166076", "1131929307"));
        lbsvs.add(new ImmutableTriple<>(2312506L, "0174166039", "1131929237"));
        lbsvs.add(new ImmutableTriple<>(2312461L, "0174165988", "1131929251"));
        lbsvs.add(new ImmutableTriple<>(2312494L, "0174166027", "1131929323"));
        lbsvs.add(new ImmutableTriple<>(2312424L, "0174166016", "1131929305"));
        lbsvs.add(new ImmutableTriple<>(2312455L, "0174165992", "1131929272"));
        lbsvs.add(new ImmutableTriple<>(2312481L, "0174165991", "1131929273"));
        lbsvs.add(new ImmutableTriple<>(2312448L, "0174166059", "1131929295"));
        lbsvs.add(new ImmutableTriple<>(2312454L, "0174166003", "1131929247"));
        lbsvs.add(new ImmutableTriple<>(2312459L, "0174165990", "1131929252"));
        lbsvs.add(new ImmutableTriple<>(2312468L, "0174166072", "1131929232"));
        lbsvs.add(new ImmutableTriple<>(2312418L, "0174166070", "1131929324"));
        lbsvs.add(new ImmutableTriple<>(2312466L, "0174166074", "1131929275"));
        lbsvs.add(new ImmutableTriple<>(2312426L, "0174162698", "1131929276"));
        lbsvs.add(new ImmutableTriple<>(2312504L, "0174166037", "1131929230"));
        lbsvs.add(new ImmutableTriple<>(2312492L, "0174166063", "1131929260"));
        lbsvs.add(new ImmutableTriple<>(2312446L, "0174166069", "1131929234"));
        lbsvs.add(new ImmutableTriple<>(2312429L, "0174166021", "1131929246"));
        lbsvs.add(new ImmutableTriple<>(2312470L, "0174166014", "1131929231"));
        lbsvs.add(new ImmutableTriple<>(2312444L, "0174162718", "1131929282"));
        lbsvs.add(new ImmutableTriple<>(2312419L, "0174166012", "1131929274"));
        lbsvs.add(new ImmutableTriple<>(2312495L, "0174166064", "1131929261"));
        lbsvs.add(new ImmutableTriple<>(2312416L, "0174166073", "1131929304"));
        lbsvs.add(new ImmutableTriple<>(2312471L, "0174166046", "1131929325"));
        lbsvs.add(new ImmutableTriple<>(2312463L, "0174166050", "1131929298"));
        lbsvs.add(new ImmutableTriple<>(2312500L, "0174165999", "1131929277"));
        lbsvs.add(new ImmutableTriple<>(2312509L, "0174166024", "1131929254"));
        lbsvs.add(new ImmutableTriple<>(2312467L, "0174166028", "1131929301"));
        lbsvs.add(new ImmutableTriple<>(2312486L, "0174166049", "1131929297"));
        lbsvs.add(new ImmutableTriple<>(2312484L, "0174165989", "1131929303"));
        lbsvs.add(new ImmutableTriple<>(2312450L, "0174166060", "1131929270"));
        lbsvs.add(new ImmutableTriple<>(2312425L, "0174166066", "1131929249"));
        lbsvs.add(new ImmutableTriple<>(2312501L, "0174165987", "1131929256"));

        for (ImmutableTriple<Long, String, String> lbsvTriple : lbsvs) {
            LabBatchStartingVessel labBatchStartingVessel = labVesselDao.findById(LabBatchStartingVessel.class, lbsvTriple.getLeft());
            Assert.assertEquals(labBatchStartingVessel.getLabVessel().getLabel(), lbsvTriple.getRight());
            LabVessel labVessel = labVesselDao.findByIdentifier(lbsvTriple.getMiddle());
            System.out.println("Change lbsv " + labBatchStartingVessel.getBatchStartingVesselId() + " from " +
                    labBatchStartingVessel.getLabVessel().getLabel() + " to " + labVessel.getLabel());
            labBatchStartingVessel.setLabVessel(labVessel);
        }
        labBatchDao.persist(new FixupCommentary("SUPPORT-1848 change back bucket entries and lbsvs"));
        labBatchDao.flush();
        userTransaction.commit();
    }

    @Test(enabled = false)
    public void fixupSupport1848_Try2b() throws Exception{
        List<String> tubeBarcodes = Arrays.asList(
                "1131926708",
                "1131929230",
                "1131929231",
                "1131929232",
                "1131929233",
                "1131929234",
                "1131929235",
                "1131929236",
                "1131929237",
                "1131929246",
                "1131929247",
                "1131929248",
                "1131929249",
                "1131929250",
                "1131929251",
                "1131929252",
                "1131929253",
                "1131929254",
                "1131929255",
                "1131929256",
                "1131929257",
                "1131929258",
                "1131929259",
                "1131929260",
                "1131929261",
                "1131929270",
                "1131929272",
                "1131929273",
                "1131929274",
                "1131929275",
                "1131929276",
                "1131929277",
                "1131929278",
                "1131929279",
                "1131929280",
                "1131929281",
                "1131929282",
                "1131929283",
                "1131929284",
                "1131929285",
                "1131929294",
                "1131929295",
                "1131929296",
                "1131929297",
                "1131929298",
                "1131929299",
                "1131929300",
                "1131929301",
                "1131929302",
                "1131929303",
                "1131929304",
                "1131929305",
                "1131929306",
                "1131929307",
                "1131929308",
                "1131929309",
                "1131929320",
                "1131929321",
                "1131929322",
                "1131929323",
                "1131929324",
                "1131929325");

        changeBucketEntriesToAliquots(tubeBarcodes, 1361856L, "LCSET-9347", "SUPPORT-1848");
    }

    @Test(enabled = false)
    public void fixupSupport2239Lcset10114() throws Exception{
        List<String> tubeBarcodes = Arrays.asList(
                "1140130880",
                "1140130879",
                "1140130878",
                "1140130877",
                "1140130876",
                "1140130875",
                "1140130874",
                "1140130873",
                "1140130872",
                "1140130871",
                "1140130883",
                "1140130884",
                "1140130885",
                "1140130886",
                "1140130887",
                "1140130888",
                "1140130889",
                "1140130890",
                "1140130891",
                "1140130892",
                "1140130893",
                "1140130894",
                "1140130906",
                "1140130905",
                "1140130904",
                "1140130903",
                "1140130902",
                "1140130901",
                "1140130900",
                "1140130899",
                "1140130898",
                "1140130897",
                "1140130896",
                "1140130895");

        changeBucketEntriesToAliquots(tubeBarcodes, 1676559L, "LCSET-10114", "SUPPORT-2239");
    }

    @Test(enabled = false)
    public void fixupSupport2239Lcset9978() throws Exception{
        List<String> tubeBarcodes = Arrays.asList(
                "1140130910",
                "1140130911",
                "1140130912",
                "1140130913",
                "1140130914",
                "1140130915",
                "1140130916",
                "1140130917",
                "1140130918",
                "1140130930",
                "1140130929",
                "1140130928",
                "1140130927",
                "1140130926");

        changeBucketEntriesToAliquots(tubeBarcodes, 1651091L, "LCSET-9978", "SUPPORT-2239");
    }

    /**
     * At last step in extraction, the SM-ID was scanned, should have been the 2D barcode.
     */
    @Test(enabled = false)
    public void fixupGplim4196() throws Exception {
        userBean.loginOSUser();
        userTransaction.begin();
        LabBatch labBatch = labBatchDao.findByName("LCSET-9442");
        Map<String, String> mapOldBarcodeToNew = new HashMap<String, String>(){{
            put("SM-AZRN2", "1124988659");
            put("SM-AZRN3", "1124988660");
            put("SM-AZRNE", "1124988672");
            put("SM-AZRNQ", "1124988683");
        }};
        fixBatchEntries(labBatch, mapOldBarcodeToNew);

        labBatchDao.persist(new FixupCommentary("GPLIM-4196 replace vessels in batch"));
        labBatchDao.flush();
        userTransaction.commit();
    }

    private void fixBatchEntries(LabBatch labBatch, Map<String, String> mapOldBarcodeToNew) {
        for (LabBatchStartingVessel labBatchStartingVessel : labBatch.getLabBatchStartingVessels()) {
            String newBarcode = mapOldBarcodeToNew.get(labBatchStartingVessel.getLabVessel().getLabel());
            if (newBarcode != null) {
                LabVessel newLabVessel = labVesselDao.findByIdentifier(newBarcode);
                System.out.println("Changing lbsv " + labBatchStartingVessel.getBatchStartingVesselId() + " from " +
                        labBatchStartingVessel.getLabVessel().getLabel() + " to " + newLabVessel.getLabel());
                labBatchStartingVessel.setLabVessel(newLabVessel);
            }
        }
        for (BucketEntry bucketEntry : labBatch.getBucketEntries()) {
            String newBarcode = mapOldBarcodeToNew.get(bucketEntry.getLabVessel().getLabel());
            if (newBarcode != null) {
                LabVessel newLabVessel = labVesselDao.findByIdentifier(newBarcode);
                System.out.println("Changing bucket entry " + bucketEntry.getBucketEntryId() + " from " +
                        bucketEntry.getLabVessel().getLabel() + " with " + newLabVessel.getLabel());
                bucketEntry.setLabVessel(newLabVessel);
            }
        }
    }

    /** Adds Jira links from LCSET to FCT. */
    @Test(enabled = false)
    public void gplim4491() throws Exception {
        Map<String, String> fctLcsets = new HashMap<String, String>() {{
            put("FCT-33529", "LCSET-10152");
            put("FCT-33530", "LCSET-10152");
            put("FCT-33531", "LCSET-10170");
            put("FCT-33532", "LCSET-10170");
            put("FCT-33533", "LCSET-10153");
            put("FCT-33534", "LCSET-10153");
            put("FCT-33535", "LCSET-10153");
        }};
        userBean.loginOSUser();
        for (Map.Entry<String, String> fctLcset : fctLcsets.entrySet()) {
            String lcsetName = fctLcset.getValue();
            LabBatch fctBatch = labBatchDao.findByName(fctLcset.getKey());
            Assert.assertNotNull(fctBatch, fctLcset.getKey());

            System.out.println("Linking " + lcsetName + " to " + fctBatch.getBatchName());
            labBatchEjb.linkJiraBatchToTicket(lcsetName, fctBatch);
        }
        // There is no database change except for the fixup commentary.
        labBatchDao.persist(new FixupCommentary("GPLIM-4491 add Jira links from LCSET to FCT"));
        labBatchDao.flush();
    }

    @Test(enabled = false)
    public void fixupSupport2414() throws Exception{
        List<String> tubeBarcodes = Arrays.asList(
                "0209156339");

        changeBucketEntriesToAliquots(tubeBarcodes, 1716867L, "LCSET-10218", "SUPPORT-2414");
    }

    @Test(enabled = false)
    public void fixupSupport2461() throws Exception{
        List<String> tubeBarcodes = Arrays.asList(
                "1147584526",
                "1147584542",
                "1147584541");

        changeBucketEntriesToAliquots(tubeBarcodes, 1716867L, "LCSET-10218", "SUPPORT-2461");
    }


    /**
     * Likely that user scanned same rack for two different LCSETs, so one control was associated with 2 LCSETs,
     * and another control with none.
     */
    @Test(enabled = false)
    public void fixupSupport2545() {
        userBean.loginOSUser();
        LabVessel labVessel = labVesselDao.findByIdentifier("1147649282");
        LabVessel labVessel2 = labVesselDao.findByIdentifier("1147649297");
        for (LabBatchStartingVessel labBatchStartingVessel : labVessel.getLabBatchStartingVessels()) {
            if (labBatchStartingVessel.getLabBatch().getBatchName().equals("LCSET-10566")) {
                System.out.println("Changing LabBatchStartingVessel " +
                        labBatchStartingVessel.getBatchStartingVesselId() + " from " + labVessel.getLabel() + " to " +
                        labVessel2.getLabel());
                labBatchStartingVessel.setLabVessel(labVessel2);
            }
        }
        labBatchDao.persist(new FixupCommentary("SUPPORT-2545 change batch membership"));
        labBatchDao.flush();
    }

    /**
     * This test is driven by a file of the following format (line 1 is for the fixup commentary;
     * line 2 is the LCSET that has the routing error;
     * line 3 is the barcode of the tube that is in two LCSETs;
     * line 4 is the barcode of the tube that is in none):
     * SUPPORT-2545
     * LCSET-10646
     * 1147649332
     * 1147649319
     */
    @Test(enabled = false)
    public void fixupSupport2603() {
        try {
            List<String> lines = IOUtils.readLines(VarioskanParserTest.getTestResource("MoveControlDiffLcset.txt"));
            moveControlToDiffLcset(lines.get(0), lines.get(1), lines.get(2), lines.get(3));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * User somehow scanned same rack for two different LCSETs, so one control was associated with 2 LCSETs,
     * and another control with none.
     * @param supportTicketId the ID of the ticket in which the problem was reported
     * @param lcsetRoutingError the ID of the LCSET that has the routing error
     * @param barcodeInTwoLcsets the barcode of the tube that is in two LCSETs
     * @param barcodeInNoLcset the barcode of the tube in the message
     */
    private void moveControlToDiffLcset(String supportTicketId, String lcsetRoutingError, String barcodeInTwoLcsets, String barcodeInNoLcset) {
        userBean.loginOSUser();
        LabVessel labVessel = labVesselDao.findByIdentifier(barcodeInTwoLcsets);
        LabVessel labVessel2 = labVesselDao.findByIdentifier(barcodeInNoLcset);
        for (LabBatchStartingVessel labBatchStartingVessel : labVessel.getLabBatchStartingVessels()) {
            if (labBatchStartingVessel.getLabBatch().getBatchName().equals(lcsetRoutingError)) {
                System.out.println("Changing LabBatchStartingVessel " +
                        labBatchStartingVessel.getBatchStartingVesselId() + " from " + labVessel.getLabel() + " to " +
                        labVessel2.getLabel());
                labBatchStartingVessel.setLabVessel(labVessel2);
            }
        }
        labBatchDao.persist(new FixupCommentary(supportTicketId + " change batch membership"));
        labBatchDao.flush();
    }

    /**
     * Likely that user scanned same rack for two different LCSETs, so one control was associated with 2 LCSETs,
     * and another control with none.
     */
    @Test(enabled = false)
    public void fixupIpi61789() {
        userBean.loginOSUser();
        LabVessel labVessel = labVesselDao.findByIdentifier("1147649272");
        LabVessel labVessel2 = labVesselDao.findByIdentifier("1147649283");
        for (LabBatchStartingVessel labBatchStartingVessel : labVessel.getLabBatchStartingVessels()) {
            if (labBatchStartingVessel.getLabBatch().getBatchName().equals("LCSET-10652")) {
                System.out.println("Changing LabBatchStartingVessel " +
                        labBatchStartingVessel.getBatchStartingVesselId() + " from " + labVessel.getLabel() + " to " +
                        labVessel2.getLabel());
                labBatchStartingVessel.setLabVessel(labVessel2);
            }
        }
        labBatchDao.persist(new FixupCommentary("IPI-61789 change batch membership"));
        labBatchDao.flush();
    }

    /**
     * This test reads its parameters from a file, testdata/ChangeBucketEntries.txt, so it can be used for other similar fixups,
     * without writing a new test.  Example contents of the file are:
     * LCSET-10923
     * GPLIM-4797
     * 0221477796 0221477790
     * 0221477798 0221477792
     */
    @Test(enabled = false)
    public void fixupGplim4797() throws Exception {
        userBean.loginOSUser();
        userTransaction.begin();

        List<String> lines = IOUtils.readLines(VarioskanParserTest.getTestResource("ChangeBucketEntries.txt"));
        String batchName = lines.get(0);
        String fixupTicketId = lines.get(1);
        Map<String, String> mapOldBarcodeToNew = new HashMap<>();
        for (int i = 2; i < lines.size(); i++) {
            String[] fields = WHITESPACE_PATTERN.split(lines.get(i));
            if (fields.length != 2) {
                throw new RuntimeException("Expected two white-space separated fields in " + lines.get(i));
            }
            mapOldBarcodeToNew.put(fields[0], fields[1]);
        }

        LabBatch labBatch = labBatchDao.findByName(batchName);
        for (LabBatchStartingVessel labBatchStartingVessel : labBatch.getLabBatchStartingVessels()) {
            String newBarcode = mapOldBarcodeToNew.get(labBatchStartingVessel.getLabVessel().getLabel());
            if (newBarcode != null) {
                LabVessel newLabVessel = labVesselDao.findByIdentifier(newBarcode);
                System.out.println("Replacing LBSV " + labBatchStartingVessel.getLabVessel().getLabel() + " with " +
                        newLabVessel.getLabel());
                labBatchStartingVessel.setLabVessel(newLabVessel);
            }
        }
        for (BucketEntry bucketEntry : labBatch.getBucketEntries()) {
            String newBarcode = mapOldBarcodeToNew.get(bucketEntry.getLabVessel().getLabel());
            if (newBarcode != null) {
                LabVessel newLabVessel = labVesselDao.findByIdentifier(newBarcode);
                System.out.println("Replacing BE " + bucketEntry.getLabVessel().getLabel() + " with " +
                        newLabVessel.getLabel());
                bucketEntry.setLabVessel(newLabVessel);
            }
        }

        labBatchDao.persist(new FixupCommentary(fixupTicketId + " replace vessels in batch"));
        labBatchDao.flush();
        userTransaction.commit();
    }

    /*
     * This test is used to delete LCSETs that are canceled in JIRA.  It reads its parameters from a file,
     * testdata/DeleteLabBatch.txt, so it can be used for other similar fixups, without writing a new test.
     * Example contents of the file are:
     * PO-9128
     * LCSET-11312
     * LCSET-11553
     * ...
     */
    @Test(enabled = false)
    public void fixupPo9128() throws Exception {
        userBean.loginOSUser();
        userTransaction.begin();

        List<String> lines = IOUtils.readLines(VarioskanParserTest.getTestResource("DeleteLabBatch.txt"));
        // From database queries, found that there were batch_starting_vessels for this LCSET, but no bucket entries.
        // Ran the test with rollback, and verified with SQL logging that batch_starting_vessels orphans were removed.
        for (int i = 1; i < lines.size(); ++i) {
            LabBatch labBatch = labBatchDao.findByName(lines.get(i));
            Set<BucketEntry> badBucketEntries = labBatch.getBucketEntries();
            Iterator<BucketEntry> badBucketEntryIter = badBucketEntries.iterator();
            while (badBucketEntryIter.hasNext()) {
                BucketEntry bucketEntry = badBucketEntryIter.next();
                LabVessel labVessel = bucketEntry.getLabVessel();
                System.out.println("Removing bucket entry " + bucketEntry + " from " + labBatch.getBatchName());
                labVessel.removeBucketEntry(bucketEntry);
            }

            for (LabEvent labEvent : labBatchDao.findListByList(LabEvent.class, LabEvent_.manualOverrideLcSet,
                    Collections.singletonList(labBatch))) {
                System.out.println("Removing manual override of " + labBatch.getBatchName() + " from " +
                                   labEvent.getLabEventType().name() + " (labEventId " + labEvent.getLabEventId() + ")");
                labEvent.setManualOverrideLcSet(null);
            }
            System.out.println("Deleting " + labBatch.getBatchName());
            labBatchDao.remove(labBatch.getJiraTicket());
            labBatch.getReworks().clear();
            labBatchDao.remove(labBatch);
        }
        labBatchDao.persist(new FixupCommentary(lines.get(0) + " delete cancelled LCSET"));
        labBatchDao.flush();
        userTransaction.commit();
    }

    /*
     * This test is used to change the PDO on bucket entries.  It reads its parameters from a file,
     * testdata/ChangeBucketEntryPdo.txt, so it can be used for other similar fixups, without writing a new test.
     * The first line is for the FixupCommentary.  The second line is the lab batch name.  The third and subsequent
     * lines are space separated Lab Vessel label and Product Order.
     * Example contents of the file are:
     * SUPPORT-3543
     * ARRAY-9623
     * CO-25311884A01 PDO-13210
     * CO-25311884A02 PDO-13210
     * CO-25311884A03 PDO-13210
     * ...
     */
    @Test(enabled = false)
    public void fixupSupport3543() throws Exception {
        userBean.loginOSUser();
        userTransaction.begin();

        List<String> lines = IOUtils.readLines(VarioskanParserTest.getTestResource("ChangeBucketEntryPdo.txt"));
        LabBatch labBatch = labBatchDao.findByName(lines.get(1));
        Map<String, ProductOrder> mapKeyToProductOrder = new HashMap<>();
        for (int i = 2; i < lines.size(); ++i) {
            String[] fields = WHITESPACE_PATTERN.split(lines.get(i));
            if (fields.length != 2) {
                throw new RuntimeException("Expected two white-space separated fields in " + lines.get(i));
            }
            for (BucketEntry bucketEntry : labBatch.getBucketEntries()) {
                if (bucketEntry.getLabVessel().getLabel().equals(fields[0])) {
                    ProductOrder productOrder = mapKeyToProductOrder.get(fields[1]);
                    if (productOrder == null) {
                        productOrder = productOrderDao.findByBusinessKey(fields[1]);
                        mapKeyToProductOrder.put(productOrder.getBusinessKey(), productOrder);
                    }
                    System.out.println("Setting " + bucketEntry.getLabVessel().getLabel() + " to " +
                            productOrder.getBusinessKey());
                    bucketEntry.setProductOrder(productOrder);
                }
            }

        }
        labBatchDao.persist(new FixupCommentary(lines.get(0)));
        labBatchDao.flush();
        userTransaction.commit();
    }


    /**
     * This test is used to add missing in-place SampleImport events to the tube formations and rack of tubes exported from BSP <br/>
     * The rack of tubes, tube formation, and in-place event is not getting persisted on BSP sample export and thus, no positions are available for Sampleimport events <br/>
     * The BSP query (change the date range criteria as required): <br/>
     * <pre>
          SELECT 'EX-' || to_char(se.job_id) AS EXPORT_BATCH,
              'SM-' || se.sample_id AS sample_id,
              to_char( se.export_date, 'mm/dd/yyyy hh24:mi:ss' ) as export_date,
              se.exported_by,
              'CO-' || r.receptacle_group_id as rack,
              CHR( ASCII('A') + r.receptacle_row ) || substr( to_char( r.receptacle_column + 101 ), 2) as position,
              r.external_id as tube_label,
              count(se.sample_id) over ( partition by se.job_id ) as tube_count
         FROM bsp_sample_export se
            , bsp_sample s
            , bsp_receptacle r
        where se.destination = 'Mercury'
          and se.export_date > to_date( '01/01/2018', 'MM/DD/YYYY')
          --and r.receptacle_group_id = 25673470
          and s.sample_id = se.sample_id
          and r.receptacle_id = s.receptacle_id
        order by se.job_id, r.receptacle_row, r.receptacle_column
     </pre> <br/>
     * ... Export the results to a text file, include header, tab delimit, no quote delimiters
     */
    @Test(enabled = false)
    public void fixupGPLIM5380() throws Exception {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm");

        userBean.loginOSUser();
        userTransaction.begin();

        // The data file to read (mercury\src\test\resources\testdata\2018_bsp_exports.txt)
        List<String> lines = IOUtils.readLines(VarioskanParserTest.getTestResource("2017_Q1_Q2_bsp.txt"));

        // Tack on a dummy line to handle edge case of last batch in file
        lines.add("EX-EXPORTFAKE\tSM-FAKE\t01/01/1970 00:00\tjsacco\tCO-DUMMY\tA01\tBARCODE\t0");

        // We want a detailed log of all the activity
        String logDirName = System.getProperty("jboss.server.log.dir");
        Writer processLogWriter = new FileWriter(logDirName + File.separator + "gplim5380_2017_Q1_Q2_fixup.log", true);
        ToStringBuilder.setDefaultStyle(ToStringStyle.SHORT_PREFIX_STYLE);
        processLogWriter.write("======== STARTING GPLIM-5380 FIXUP =============\n");
        processLogWriter.write(SimpleDateFormat.getDateTimeInstance().format(new Date()));
        processLogWriter.write( "\n");

        boolean headerLine = true;

        String currentBatchName = "NONE";
        String skipMissingBatch = "NONE";
        String nextBatchName;

        Date exportDate = null;
        String userLogin = null;
        Long userId = null;
        String rackLabel = null;
        VesselPosition position = null;
        String tubeLabel = null;
        int vesselCount = 0;
        LabBatch labBatch = null;
        RackOfTubes rackOfTubes = null;
        Map<VesselPosition, BarcodedTube> mapPositionToTube = null;

        for( String line : lines ) {

            // Ignore header and any obviously short lines
            if( headerLine || line.length() < 12 ) {
                headerLine = false;
                continue;
            }

            String[] tokens = line.split("\t", 8);
            nextBatchName = tokens[0];

            // Don't waste the time with a missing batch
            if( nextBatchName.equals(skipMissingBatch )) {
                continue;
            }

            if( !currentBatchName.equals(nextBatchName )) {

                // Persist data for previous batch
                if( mapPositionToTube != null && !mapPositionToTube.isEmpty() ) {
                    processLogWriter.write("Verifying batch " + currentBatchName + "\n");
                    TubeFormation persistTubeFormation;
                    TubeFormation dummyTubeFormation = new TubeFormation(mapPositionToTube, RackOfTubes.RackType.Matrix96);
                    LabVessel labVessel = labVesselDao.findByIdentifier(dummyTubeFormation.getLabel());
                    if( labVessel != null ) {
                        processLogWriter.write(".. Tube formation exists in mercury\n");
                        persistTubeFormation = OrmUtil.proxySafeCast(labVessel, TubeFormation.class);
                    } else {
                        processLogWriter.write(".. Tube formation DOES NOT exist in mercury\n");
                        // Build it using attached tubes
                        for(Map.Entry<VesselPosition,BarcodedTube> entry : mapPositionToTube.entrySet()) {
                            LabVessel tube = labVesselDao.findByIdentifier(entry.getValue().getLabel());
                            if( tube != null ) {
                                entry.setValue(OrmUtil.proxySafeCast(tube, BarcodedTube.class));
                            }
                        }
                        persistTubeFormation = new TubeFormation(mapPositionToTube, RackOfTubes.RackType.Matrix96);
                    }

                    boolean foundImport = false;
                    for( LabEvent labEvent : persistTubeFormation.getInPlaceLabEvents() ) {
                        if( labEvent.getLabEventType() == LabEventType.SAMPLE_IMPORT ) {
                            processLogWriter.write(".. Found in place SampleImport event for tube formation\n");
                            foundImport = true;
                            break;
                        }
                    }
                    if( !foundImport ) {
                        labVesselDao.persist(persistTubeFormation);

                        LabVessel labVesselRack = labVesselDao.findByIdentifier(rackLabel);
                        if( labVesselRack == null ) {
                            rackOfTubes = new RackOfTubes(rackLabel, RackOfTubes.RackType.Matrix96);
                            labVesselDao.persist(rackOfTubes);
                        } else if( OrmUtil.proxySafeIsInstance(labVesselRack, RackOfTubes.class)) {
                            rackOfTubes = OrmUtil.proxySafeCast(labVesselRack, RackOfTubes.class);
                        } else {
                            rackOfTubes = null;
                        }

                        if( rackOfTubes != null ) {
                            persistTubeFormation.addRackOfTubes(rackOfTubes);
                        }
                        processLogWriter.write(".. Persisting in place SampleImport event for tube formation " + dummyTubeFormation.getLabel() + ", rack " + rackOfTubes.getLabel() + "\n");
                        // TODO: If copying this code, assign rack of tubes to in place event ancillary vessel
                        persistTubeFormation.addInPlaceEvent( new LabEvent(LabEventType.SAMPLE_IMPORT, exportDate, "BSP", 1L, userId,
                            "BSP"));
                        labVesselDao.flush();
                    }
                }

                // build new
                mapPositionToTube = new HashMap<>();
                currentBatchName = nextBatchName;
                labBatch = labBatchDao.findByName(currentBatchName);
                if( labBatch == null ) {
                    processLogWriter.write("Can't find import batch " + currentBatchName + "\n");
                    skipMissingBatch = currentBatchName;
                    continue;
                }
            }

            // All is persisted at new batch - get data for current line and build out tube formation
            exportDate  = dateFormat.parse(tokens[2]);
            // Bump up a second, clashing on disambiguator uniqueness
            exportDate  = new Date(exportDate.getTime() + 1000L );
            userLogin = tokens[3];
            userId = bspUserList.getByUsername(userLogin).getUserId();
            rackLabel = tokens[4];
            if( tokens[5].isEmpty() ) {
                position = null;
            } else {
                position = VesselPosition.getByName(tokens[5]);
            }
            tubeLabel = tokens[6];
            vesselCount = Integer.parseInt(tokens[7]);

            if( labBatch.getLabBatchStartingVessels().size() != vesselCount ) {
                // Mercury does not match BSP?
                processLogWriter.write(labBatch.getBatchName() + " BSP tube count (" + vesselCount + ") does not match mercury batch starting vessel count (" + labBatch.getLabBatchStartingVessels().size() + ")\n");
            }

            // Strange cases from BSP - no barcode, barcode = "null", no position,  from BSP  !tubeLabel.isEmpty() &&
            if( !tubeLabel.isEmpty() && !"null".equals(tubeLabel) && position != null && rackLabel.length() > 9 ) {
                mapPositionToTube.put(position, new BarcodedTube(tubeLabel));
            }

        }

        labBatchDao.persist(new FixupCommentary("GPLIM-5380 Add missing SampleImport events for tube formations"));
        labBatchDao.flush();
        userTransaction.commit();

        processLogWriter.write("======== FINISHED GPLIM-5380 FIXUP =============\n");
        processLogWriter.write(SimpleDateFormat.getDateTimeInstance().format(new Date()));
        processLogWriter.close();
    }


    /*
     * This test removes samples (starting vessels and bucket entries) from an LCSET.
     * To simplify reuse, parameters come from file testdata/RemoveLabBatchSample.txt
     * The file needs a ticket id, an lcset, and comma-delimited list of samples.
     * For example:
     * SUPPORT-1234
     * LCSET-1234
     * SM-ABCD1,SM-ABCD2,SM-ABCD3
     */
    @Test(enabled = false)
    public void removeFromLcset() throws Exception {
        userBean.loginOSUser();
        userTransaction.begin();

        List<String> lines = IOUtils.readLines(VarioskanParserTest.getTestResource("RemoveLabBatchSample.txt"));
        Assert.assertEquals(lines.size(), 3);

        String lcsetName = lines.get(1);
        LabBatch labBatch = labBatchDao.findByName(lcsetName);
        Assert.assertNotNull(labBatch, "Cannot find LCSET: '" + lcsetName + "'");
        List<String> samplesToRemove = Arrays.asList(lines.get(2).split(","));
        removeStartingVesselsAndBucketEntries(samplesToRemove, labBatch);

        labBatchDao.persist(new FixupCommentary(lines.get(0) + " Removed samples from " + lcsetName));
        labBatchDao.flush();
        userTransaction.commit();
    }

    private List<LabBatchStartingVessel> removeStartingVesselsAndBucketEntries(List<String> sampleNames,
            LabBatch labBatch) {
        List<LabBatchStartingVessel> vesselsToRemove = new ArrayList<>();
        Set<BucketEntry> bucketEntriesToRemove = new HashSet<>();
        List<String> sampleNamesFound = new ArrayList<>();
        for (LabBatchStartingVessel startingVessel : labBatch.getLabBatchStartingVessels()) {
            Set<SampleInstanceV2> sampleInstances = startingVessel.getLabVessel().getSampleInstancesV2();
            for (SampleInstanceV2 sampleInstance : sampleInstances) {
                String sample = sampleInstance.getRootOrEarliestMercurySampleName();
                if (sampleNames.contains(sample)) {
                    vesselsToRemove.add(startingVessel);
                    sampleNamesFound.add(sample);
                    for (BucketEntry bucketEntry : sampleInstance.getAllBucketEntries()) {
                        if (bucketEntry.getLabBatch().equals(labBatch)) {
                            bucketEntriesToRemove.add(bucketEntry);
                        }
                    }
                }
            }
        }
        Assert.assertEquals(sampleNamesFound, sampleNames, labBatch.getBatchName() + " does not contain " +
                StringUtils.join(CollectionUtils.subtract(sampleNames, sampleNamesFound), ", "));

        System.out.println("Removing starting vessels and bucket entries for samples " +
                StringUtils.join(sampleNames, ", ") + " from " + labBatch.getBatchName());

        for (LabBatchStartingVessel vesselToRemove : vesselsToRemove) {
            labBatch.getLabBatchStartingVessels().remove(vesselToRemove);
            vesselToRemove.getLabVessel().getLabBatchStartingVessels().remove(vesselToRemove);
        }
        for (BucketEntry bucketEntry : bucketEntriesToRemove) {
            labBatch.getBucketEntries().remove(bucketEntry);
            LabVessel labVessel = bucketEntry.getLabVessel();
            labVessel.removeBucketEntry(bucketEntry);
            ProductOrder productOrder = bucketEntry.getProductOrder();
            productOrder.getBucketEntries().remove(bucketEntry);
            labBatchDao.remove(bucketEntry);
        }
        return vesselsToRemove;
    }

    /**
     * This test is used to find LabEvents that have more than one LabBatch.  The output is used to create input files
     * for {@link #consolidateLabBatches()}.
     */
    @Test(enabled = false)
    public void findAmbiguousEvents() {
        List<LabEvent> labEvents = labEventDao.findByDateAndType(
                new GregorianCalendar(2018, Calendar.JANUARY, 1).getTime(), new Date(), LabEventType.SHEARING_TRANSFER);
        List<Long> eventIds = labEvents.stream().map(LabEvent::getLabEventId).collect(Collectors.toList());

        // Clear the session and refetch each event individually, to avoid running out of memory during event traversal
        labEventDao.clear();
        for (Long eventId : eventIds) {
            LabEvent labEvent = labEventDao.findById(LabEvent.class, eventId);
            Set<LabBatch> computedLcSets = labEvent.getComputedLcSets();
            if (computedLcSets.isEmpty()) {
                System.out.print(labEvent.getLabEventId() + " ");
                Set<String> batchNames = new HashSet<>();
                Set<String> controlBatchNames = new HashSet<>();
                LabVessel labVessel = labEvent.getTargetLabVessels().iterator().next();
                for (SampleInstanceV2 sampleInstanceV2 : labVessel.getSampleInstancesV2()) {
                    List<BucketEntry> bucketEntries = sampleInstanceV2.getAllBucketEntries();
                    if (bucketEntries.isEmpty()) {
                        for (LabBatch labBatch : sampleInstanceV2.getAllWorkflowBatches()) {
                            controlBatchNames.add(labBatch.getBatchName());
                        }
                    } else {
                        for (BucketEntry bucketEntry : bucketEntries) {
                            batchNames.add(bucketEntry.getLabBatch().getBatchName() + " " +
                                    bucketEntry.getLabBatch().getCreatedOn());
                        }
                    }
                }
                System.out.print("control " + StringUtils.join(controlBatchNames, ", ") + " ");
                System.out.println("others " + batchNames.stream().sorted().collect(Collectors.joining(",")));
            }
            labEventDao.clear();
        }
    }

    /**
     * This test is used to consolidate LabBatches.  For example, some ShearingTransfers have multiple LCSETs; Mercury
     * cannot infer original vs rework LCSET in the presence of multiple LCSETs, so this test is used to move samples
     * to a single LCSET.  The samples are not removed from the JIRA ticket, because that would make meaningless any
     * comments that were made on the ticket.  The test adds to each source ticket a comment with the name of the
     * destination ticket.
     * The test reads its input from file mercury/src/test/resources/testdata/UpdateSampleMetadata.txt.
     * The first line is the fixup commentary.  The second line is the destination LabBatch.  The third and
     * subsequent lines are the source LabBatches.
     */
    @Test(enabled = false)
    public void consolidateLabBatches() throws IOException, SystemException, NotSupportedException,
            HeuristicRollbackException, HeuristicMixedException, RollbackException {
        userBean.loginOSUser();
        utx.begin();
        List<String> lines = IOUtils.readLines(VarioskanParserTest.getTestResource("ConsolidateLabBatches.txt"));
        String destBatchName = lines.get(1);
        LabBatch destLabBatch = labBatchDao.findByName(destBatchName);
        Assert.assertNotNull(destLabBatch, "Failed to find " + destBatchName);

        for (String sourceBatchName : lines.subList(2, lines.size())) {
            LabBatch sourceLabBatch = labBatchDao.findByName(sourceBatchName);
            Assert.assertNotNull(sourceLabBatch, "Failed to find " + sourceBatchName);
            System.out.println("Moving entries from " + sourceLabBatch.getBusinessKey() + " to " +
                    destLabBatch.getBusinessKey());
            for (BucketEntry bucketEntry : sourceLabBatch.getBucketEntries()) {
                destLabBatch.addBucketEntry(bucketEntry);
            }
            for (LabBatchStartingVessel labBatchStartingVessel : sourceLabBatch.getLabBatchStartingVessels()) {
                labBatchStartingVessel.setLabBatch(destLabBatch);
            }
        }
        labBatchDao.persist(new FixupCommentary(lines.get(0)));
        labBatchDao.flush();

        for (String sourceBatchName : lines.subList(2, lines.size())) {
            jiraService.addComment(sourceBatchName, "In Mercury database, moved samples to " +
                    destLabBatch.getBusinessKey() + " to get a single LCSET for the first event.");
        }
        utx.commit();
    }


    @Test(enabled = false)
    public void backfillInference() throws SystemException, NotSupportedException, HeuristicRollbackException,
            HeuristicMixedException, RollbackException, IOException {
        userBean.loginOSUser();

        List<String> lines = IOUtils.readLines(VarioskanParserTest.getTestResource("BackfillLabBatchInference.txt"));
        for (String line : lines) {
            if (line.startsWith("#")) {
                continue;
            }
            userTransaction.begin();
            LabBatch labBatch = labBatchDao.findByName(line);
            System.out.println(labBatch.getBatchName());
            for (BucketEntry bucketEntry : labBatch.getBucketEntries()) {
                TransferTraverserCriteria transferTraverserCriteria = new ComputeLabBatchTtc(false);
                bucketEntry.getLabVessel().evaluateCriteria(transferTraverserCriteria,
                        TransferTraverserCriteria.TraversalDirection.Descendants);
            }

            userTransaction.commit();
            labBatchDao.clear();
        }
    }

    public static class ComputeLabBatchTtc extends TransferTraverserCriteria {
        private boolean force;

        public ComputeLabBatchTtc(boolean force) {
            this.force = force;
        }

        @Override
        public TraversalControl evaluateVesselPreOrder(Context context) {
            LabVessel contextVessel = context.getContextVessel();
            VesselContainer<?> contextVesselContainer = context.getContextVesselContainer();
            LabVessel.VesselEvent contextVesselEvent = context.getVesselEvent();

            if(contextVesselEvent != null ) {
                compute(contextVesselEvent.getLabEvent());
            }

            if( contextVessel != null ) {
                for (LabEvent labEvent : contextVessel.getInPlaceLabEvents()) {
                    compute(labEvent);
                }

                for (VesselContainer<?> containerVessel : contextVessel.getVesselContainers()) {
                    // In place events may apply to containers
                    for (LabEvent labEvent : containerVessel.getEmbedder().getInPlaceLabEvents()) {
                        compute(labEvent);
                    }
                }
            }

            // Check for in place events on vessel container (e.g. EndRepair, ABase, APWash)
            if( contextVesselContainer != null ) {
                LabVessel containerVessel = contextVesselContainer.getEmbedder();
                if (containerVessel != null) {
                    for (LabEvent labEvent : containerVessel.getInPlaceLabEvents()) {
                        compute(labEvent);
                    }

                    // Look for what comes in from the side (e.g. IndexedAdapterLigation, BaitAddition)
                    for (LabEvent containerEvent : containerVessel.getTransfersTo()) {
                        compute(containerEvent);
                        for (LabVessel ancestorLabVessel : containerEvent.getSourceLabVessels()) {
                            if( ancestorLabVessel.getContainerRole() != null ){
                                for (LabEvent labEvent : ancestorLabVessel.getContainerRole().getEmbedder().getTransfersTo()) {
                                    compute(labEvent);
                                }
                            }
                        }
                    }
                }
            }

            return TraversalControl.ContinueTraversing;
        }

        @Override
        public void evaluateVesselPostOrder(Context context) {

        }

        private void compute(LabEvent labEvent) {
            if (force || !labEvent.isLabBatchComputed()) {
                labEvent.computeLabBatches();
            }
        }
    }
}
