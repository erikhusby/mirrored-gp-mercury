package org.broadinstitute.gpinformatics.mercury.entity.sample;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VarioskanParserTest;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVesselFixupTest;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Test(groups = TestGroups.FIXUP)
public class MercurySampleFixupTest extends Arquillian {

    @Inject
    private MercurySampleDao mercurySampleDao;

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private LabEventDao labEventDao;

    @Inject
    private UserBean userBean;

    @Inject
    private BSPUserList bspUserList;

    private static final String RECEIVED_DATE_UPDATE_FORMAT = "MM/dd/yyyy";

    @Deployment
    public static WebArchive buildMercuryWar() {

        /*
         * If the need comes to utilize this fixup in production, change the buildMercuryWar parameters accordingly
         */
        return DeploymentBuilder.buildMercuryWar(
                org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV, "DEV");
    }

    @Test(groups = TestGroups.FIXUP, enabled = false)
    public void gplim4381DeleteOrphanedSamples() throws Exception {
        List<String> sampleKeys = Arrays.asList(
                "SM-CNSAY", "SM-CNSB2", "SM-CNSBA", "SM-CNSB8", "SM-CNSB7", "SM-CNSB5", "SM-CNSAX",
                "SM-CNSBB", "SM-CNSAR", "SM-CNSAU", "SM-CNSBC", "SM-CNSB9", "SM-CNSAZ", "SM-CNSB3",
                "SM-CNSB1", "SM-CNSAV", "SM-CNSAW", "SM-CNSAT", "SM-CNSAS"
        );
        List<MercurySample> mercurySamples = mercurySampleDao.findBySampleKeys(sampleKeys);
        userBean.loginOSUser();
        for (MercurySample mercurySample : mercurySamples) {
            Set<LabVessel> labVessels = mercurySample.getLabVessel();
            for (LabVessel labVessel : labVessels) {
                labVessel.getMercurySamples().remove(mercurySample); // todo jmt
            }
            mercurySampleDao.remove(mercurySample);
        }
        mercurySampleDao.persist(new FixupCommentary(
                "GPLIM-4381: Delete BSP Samples from Mercury which were not created in BSP due to an exception."));
    }

    @Test(groups = TestGroups.FIXUP, enabled = false)
    public void gplim4631DeleteOrphanedSamples() throws Exception {
        List<String> sampleKeys = Arrays.asList("SM-D3J5K","SM-D3J59","SM-D3J5N","SM-D3J3I","SM-D3J3D");
        List<MercurySample> mercurySamples = mercurySampleDao.findBySampleKeys(sampleKeys);
        userBean.loginOSUser();
        for (MercurySample mercurySample : mercurySamples) {
            Set<LabVessel> labVessels = mercurySample.getLabVessel();
            for (LabVessel labVessel : labVessels) {
                labVessel.getMercurySamples().remove(mercurySample); // todo jmt
            }
            mercurySampleDao.remove(mercurySample);
        }
        mercurySampleDao.persist(new FixupCommentary(
                "GPLIM-4631: Delete BSP Samples from Mercury which were not created in BSP due to an exception."));
    }

    @Test(groups = TestGroups.FIXUP, enabled = false)
    public void gplim4692DeleteOrphanedSamples() throws Exception {
        List<String> sampleKeys = Arrays.asList("SM-DNXJW","SM-DNXJX","SM-DNXJY","SM-DNXJZ","SM-DNXK3","SM-DNXK4",
                "SM-DNXK5","SM-DNXK6","SM-DNXK7","SM-DNXK8","SM-DNXK9","SM-DNXKC","SM-DNXKD","SM-DNXKE","SM-DNXKF",
                "SM-DNXKI","SM-DNXKJ","SM-DNXKK","SM-DNXKL","SM-DNXKM","SM-DNXKN","SM-DNXKS","SM-DNXKT","SM-DNXKU",
                "SM-DNXKV","SM-DNXKX","SM-DNXKY");
        removeOrphanedSamplesHelper(sampleKeys,
                "GPLIM-4692: Delete BSP Samples from Mercury which were not created in BSP due to an exception.");
    }

    @Test(groups = TestGroups.FIXUP, enabled = false)
    public void gplim4692DeleteMoreOrphanedSamples() throws Exception {
        List<String> sampleKeys = Arrays.asList("SM-DNXK2","SM-DNXK1","SM-DNXKH","SM-DNXKG","SM-DNXKA","SM-DNXKO",
                "SM-DNXKB","SM-DNXKR","SM-DNXKQ","SM-DNXKP","SM-DNXKW");
        removeOrphanedSamplesHelper(sampleKeys,
                "GPLIM-4692: Delete more BSP Samples from Mercury which were not created in BSP due to an exception and looks funny with sample key name.");
    }

    private void removeOrphanedSamplesHelper(List<String> sampleKeys, String fixupReason) {
        List<MercurySample> mercurySamples = mercurySampleDao.findBySampleKeys(sampleKeys);
        userBean.loginOSUser();
        for (MercurySample mercurySample : mercurySamples) {
            Set<LabVessel> labVessels = mercurySample.getLabVessel();
            for (LabVessel labVessel : labVessels) {
                labVessel.getMercurySamples().remove(mercurySample); // todo jmt
            }
            mercurySampleDao.remove(mercurySample);
        }
        mercurySampleDao.persist(new FixupCommentary(
                fixupReason));
    }

    /**
     * This test reads its parameters from a file, mercury/src/test/resources/testdata/DeleteOrphanSamples.txt, so it
     * can be used for other similar fixups, without writing a new test.  Example contents of the file are:
     * GPLIM-5053 delete samples that were rolled back in BSP
     * SM-D3J61
     * SM-D3J62
     */
    @Test(groups = TestGroups.FIXUP, enabled = false)
    public void gplim5053DeleteOrphanedSamples() throws Exception {
        List<String> lines = IOUtils.readLines(VarioskanParserTest.getTestResource("DeleteOrphanSamples.txt"));
        String fixupReason = lines.get(0);
        removeOrphanedSamplesHelper(lines.subList(1, lines.size()), fixupReason);
    }

    /**
     * This fixup will remove duplicate samples from the system and reset their existing labVessel relationships to
     * converge to the one remaining sample of which they are a duplicate.
     *
     *
     * IMPORTANT!!!!!   Before Running this, change the mercurySample ManytoMany relationship on LabVessel to be a
     * List!!!!
     *
     *
     *
     * @throws Exception
     */
    @Test(groups = TestGroups.FIXUP, enabled = false)
    public void gplim3005ReassignDuplicateSamples() throws Exception {

        List<MercurySample> duplicateSamples = mercurySampleDao.findDuplicateSamples();

        Multimap<String, MercurySample> duplicateSampleMap = ArrayListMultimap.create();
        Multimap<String, LabVessel> sampleToVesselMap = ArrayListMultimap.create();

        for (MercurySample duplicateSample : duplicateSamples) {
            duplicateSampleMap.put(duplicateSample.getSampleKey(), duplicateSample);
            for (LabVessel labVessel : duplicateSample.labVessel) {
                sampleToVesselMap.put(duplicateSample.getSampleKey(), labVessel);
            }
        }

        for (String sampleName : duplicateSampleMap.keys()) {
            Long sampleIdToKeep = null;
            for (MercurySample duplicateSample : duplicateSampleMap.get(sampleName)) {
                if(sampleIdToKeep == null) {
                    sampleIdToKeep = duplicateSample.getMercurySampleId();
                    for (LabVessel labVessel : sampleToVesselMap.get(sampleName)) {
                        if(!labVessel.getMercurySamples().contains(duplicateSample)) {
                            labVessel.addSample(duplicateSample);
                        }
                    }
                } else {
                    duplicateSample.removeSampleFromVessels(sampleToVesselMap.get(sampleName));
                    mercurySampleDao.remove(duplicateSample);
                }
            }
        }
    }

    private static class TubeTareData {
        private String tubeBarcode;
        private BigDecimal tareWeight;
        private String machineName;

        private TubeTareData(String tubeBarcode, BigDecimal tareWeight, String machineName) {
            this.tubeBarcode = tubeBarcode;
            this.tareWeight = tareWeight;
            this.machineName = machineName;
        }

        public String getTubeBarcode() {
            return tubeBarcode;
        }

        public BigDecimal getTareWeight() {
            return tareWeight;
        }

        public String getMachineName() {
            return machineName;
        }
    }

    @Test(groups = TestGroups.FIXUP, enabled = false)
    public void gplim3485ImportInitialTareData() {
        userBean.loginOSUser();

        Collection<TubeTareData> tubeTareData = new ArrayList<>();
        tubeTareData.add(new TubeTareData("1109293898", new BigDecimal("0.6295"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109293887", new BigDecimal("0.6306"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109293938", new BigDecimal("0.6305"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109293941", new BigDecimal("0.6291"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109293937", new BigDecimal("0.6287999999999999"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109293896", new BigDecimal("0.6254"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109293881", new BigDecimal("0.6287999999999999"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109293900", new BigDecimal("0.6291"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109293908", new BigDecimal("0.6254"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109293921", new BigDecimal("0.6287999999999999"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109293950", new BigDecimal("0.6277999999999999"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109293920", new BigDecimal("0.6287999999999999"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109293918", new BigDecimal("0.6275"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109293910", new BigDecimal("0.6281"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109293923", new BigDecimal("0.6285"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109293922", new BigDecimal("0.6277"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109293934", new BigDecimal("0.6272000000000001"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109293958", new BigDecimal("0.6255"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109293961", new BigDecimal("0.6279"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109293946", new BigDecimal("0.6285"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109293913", new BigDecimal("0.628"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109293942", new BigDecimal("0.6346"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109293939", new BigDecimal("0.6247"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109293912", new BigDecimal("0.6292000000000001"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109293885", new BigDecimal("0.6325"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109293916", new BigDecimal("0.6291"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109293907", new BigDecimal("0.6343"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109293897", new BigDecimal("0.632"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109293972", new BigDecimal("0.6297"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109293971", new BigDecimal("0.6321"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109293957", new BigDecimal("0.6302000000000001"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109293955", new BigDecimal("0.6304"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109293933", new BigDecimal("0.6343"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109293932", new BigDecimal("0.6296"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109293894", new BigDecimal("0.629"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109293909", new BigDecimal("0.6265"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109293919", new BigDecimal("0.6277999999999999"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109293895", new BigDecimal("0.6277"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109293914", new BigDecimal("0.6259"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109293888", new BigDecimal("0.6281"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109293884", new BigDecimal("0.6287999999999999"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109293906", new BigDecimal("0.6331"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109293959", new BigDecimal("0.6289"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109293945", new BigDecimal("0.6265"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109293935", new BigDecimal("0.6347999999999999"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109293956", new BigDecimal("0.6293"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109115788", new BigDecimal("0.6283"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109293917", new BigDecimal("0.6274"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109293911", new BigDecimal("0.6345"), "BRUCE"));
        tubeTareData.add(new TubeTareData("0175403357", new BigDecimal("0.6307"), "BRUCE"));
        tubeTareData.add(new TubeTareData("0175404432", new BigDecimal("0.6325"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109293892", new BigDecimal("0.6259"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109293891", new BigDecimal("0.6269"), "BRUCE"));
        tubeTareData.add(new TubeTareData("0175579010", new BigDecimal("0.6249"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109293901", new BigDecimal("0.6287999999999999"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109293936", new BigDecimal("0.6274"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109108160", new BigDecimal("0.6283"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109115770", new BigDecimal("0.6304"), "BRUCE"));
        tubeTareData.add(new TubeTareData("0175579015", new BigDecimal("0.6263"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109293944", new BigDecimal("0.629"), "BRUCE"));
        tubeTareData.add(new TubeTareData("0175578720", new BigDecimal("0.6287"), "BRUCE"));
        tubeTareData.add(new TubeTareData("0175403364", new BigDecimal("0.627"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109293890", new BigDecimal("0.6289"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109293948", new BigDecimal("0.63"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109293924", new BigDecimal("0.627"), "BRUCE"));
        tubeTareData.add(new TubeTareData("0175578534", new BigDecimal("0.6277999999999999"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109293960", new BigDecimal("0.638"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109115723", new BigDecimal("0.6335"), "BRUCE"));
        tubeTareData.add(new TubeTareData("0175579022", new BigDecimal("0.6286"), "BRUCE"));
        tubeTareData.add(new TubeTareData("0175578729", new BigDecimal("0.6249"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109293903", new BigDecimal("0.63"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109293889", new BigDecimal("0.6292000000000001"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109108133", new BigDecimal("0.6261"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109293904", new BigDecimal("0.6292000000000001"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109293963", new BigDecimal("0.6293"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109293905", new BigDecimal("0.6306"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109293927", new BigDecimal("0.6297"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109108159", new BigDecimal("0.6341"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109108143", new BigDecimal("0.6301"), "BRUCE"));
        tubeTareData.add(new TubeTareData("1109293940", new BigDecimal("0.6307"), "BRUCE"));

        for (TubeTareData data : tubeTareData) {
            String barcode = data.getTubeBarcode();
            BigDecimal weight = data.getTareWeight();

            LabVessel tube = labVesselDao.findByIdentifier(barcode);
            if (tube == null) {
                throw new RuntimeException("Failed to find tube with barcode: " + barcode);
            }

            tube.setReceptacleWeight(weight);
            LabEvent tareEvent = new LabEvent(LabEventType.INITIAL_TARE, new Date(), data.getMachineName(), 0L,
                    userBean.getBspUser().getUserId(), "MercurySampleFixupTest.gplim3485ImportInitialTareData()");
            tareEvent.setInPlaceLabVessel(tube);
            labEventDao.persist(tareEvent);
            System.out.println(String.format("Set weight of tube %s to %f", barcode, weight.doubleValue()));
        }

        labEventDao.persist(new FixupCommentary("GPLIM-3485 importing initial tare data for samples shipped from BSP"));
        System.out.println("Updated weight for " + tubeTareData.size() + " tubes.");

    }

    /**
     * For this to run successfully, a VM argument (-Dsamples.received=[full file path]) needs to be added to the
     * JBoss Server when run
     */
    @Test(groups = TestGroups.FIXUP, enabled = false)
    public void backfillReceiptForSamples_GPLIM3487() {
        Map<String, MercurySample> nonReceivedSamplesByKey = mercurySampleDao.findNonReceivedCrspSamples();

        List<SampleReceiptFixup> sampleReceiptFixupList = getSamplesToFixup(nonReceivedSamplesByKey);

        long counter = 0;
        Date lastDate = null;
        Collections.sort(sampleReceiptFixupList, SampleReceiptFixup.BY_DATE);
        List<String> updatedSamples = new ArrayList<>();
        for(SampleReceiptFixup currentFixup : sampleReceiptFixupList) {
            if (lastDate == null || !lastDate.equals(currentFixup.getReceiptDate())) {
                counter = 1;
            } else {
                counter++;
            }
            if(currentFixup.getReceivedSample().getLabVessel().size() != 1) {
                Assert.fail("Unable to add Receipt date for sample that is associated to more than one vessel: " +
                            currentFixup.getReceivedSample().getSampleKey());
            } else {
                currentFixup.getReceivedSample().getLabVessel().iterator()
                        .next().setReceiptEvent(bspUserList.getByUsername(currentFixup.getReceiptUserName()),
                        currentFixup.getReceiptDate(), counter, LabEvent.UI_EVENT_LOCATION);
                updatedSamples.add(currentFixup.getReceivedSample().getSampleKey());
            }
            lastDate = currentFixup.getReceiptDate();
        }

        mercurySampleDao.persist(new FixupCommentary(String.format("GPLIM-3487: Added receipt dates for %d samples",
                updatedSamples.size())));
        mercurySampleDao.flush();
    }

    private List<SampleReceiptFixup> getSamplesToFixup(Map<String, MercurySample> samplesByKey) {
        String property = System.getProperty("samples.received");
        if (property == null) {
            Assert.fail("The filename for the sample receipt dates is not found");
        }
        File samplesToUpdate = new File(property);

        List<SampleReceiptFixup> foundSamples = new ArrayList<>();

        List<String> errors = null;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(samplesToUpdate));
            errors = new ArrayList<>();
            String line = null;

            SimpleDateFormat receiptDateFormatter = new SimpleDateFormat(RECEIVED_DATE_UPDATE_FORMAT);

            while((line = reader.readLine()) != null) {
                String[] lineInfo = line.split(",");
                if (!samplesByKey.containsKey(lineInfo[0])) {
                    errors.add(lineInfo[0] + " either does not need a receipt date added or is not found");
                } else {
                    try {
                        foundSamples.add(new SampleReceiptFixup(lineInfo[2], receiptDateFormatter.parse(lineInfo[1])
                                ,samplesByKey.get(lineInfo[0])));
                    } catch (ParseException e) {
                        errors.add(String.format("The date format for %s does not match the necessary format to update "
                                                 + "[%s]:  %s",lineInfo[0], RECEIVED_DATE_UPDATE_FORMAT, lineInfo[1]));
                    }
                }
            }
        } catch (IOException e) {
            Assert.fail("Unable to read form the provided file " + property);
        }
        if(CollectionUtils.isNotEmpty(errors)) {
            Assert.fail(StringUtils.join(errors,"\n"));
        }

        return foundSamples;
    }

    private static class SampleReceiptFixup implements Comparable<SampleReceiptFixup>{
        private String receiptUserName;
        private Date receiptDate;
        private MercurySample receivedSample;

        public static Comparator<SampleReceiptFixup> BY_DATE = new Comparator<SampleReceiptFixup>() {
            @Override
            public int compare(SampleReceiptFixup fixup, SampleReceiptFixup otherFixup) {
                return fixup.getReceiptDate().compareTo(otherFixup.getReceiptDate());
            }
        };

        public SampleReceiptFixup(String receiptUserName, Date receiptDate, MercurySample receivedSampleKey) {
            this.receiptUserName = receiptUserName;
            this.receiptDate = receiptDate;
            this.receivedSample = receivedSampleKey;
        }

        public String getReceiptUserName() {
            return receiptUserName;
        }

        public Date getReceiptDate() {
            return receiptDate;
        }

        public MercurySample getReceivedSample() {
            return receivedSample;
        }

        @Override
        public int compareTo(SampleReceiptFixup that) {
            CompareToBuilder builder = new CompareToBuilder();
            builder.append(getReceiptDate(), that.getReceiptDate());
            return builder.build();
        }
    }

    /**
     * This test reads its parameters from a file, mercury/src/test/resources/testdata/ChangeSampleMetadataSource.txt,
     * so it can be used for other similar fixups, without writing a new test.  Example contents of the file are:
     * CRSP-556 change metadata source
     * SM-G811M MERCURY
     * SM-9T6OH BSP
     */
    @Test(enabled = false)
    public void fixupCrsp556() throws IOException {
        userBean.loginOSUser();

        List<String> lines = IOUtils.readLines(VarioskanParserTest.getTestResource("ChangeSampleMetadataSource.txt"));
        for (int i = 1; i < lines.size(); i++) {
            String[] fields = LabVesselFixupTest.WHITESPACE_PATTERN.split(lines.get(i));
            if (fields.length != 2) {
                throw new RuntimeException("Expected two white-space separated fields in " + lines.get(i));
            }
            MercurySample mercurySample = mercurySampleDao.findBySampleKey(fields[0]);
            Assert.assertNotNull(mercurySample, fields[0] + " not found");
            MercurySample.MetadataSource metadataSource = MercurySample.MetadataSource.valueOf(fields[1]);
            Assert.assertNotNull(metadataSource, fields[1] + " not found");
            System.out.println("Changing " + mercurySample.getSampleKey() + " to " + metadataSource);
            mercurySample.setMetadataSource(metadataSource);
        }

        labVesselDao.persist(new FixupCommentary(lines.get(0)));
        labVesselDao.flush();
    }

    /**
     * This test reads its parameters from a file, mercury/src/test/resources/testdata/AlterSampleName.txt,
     * so it can be used for other similar fixups, without writing a new test.  Example contents of the file are:
     * SUPPORT-3871 change name of incorrectly accessioned sample and vessel
     * SM-G811M A1119993
     * SM-9T6OH A9920002
     */
    @Test(enabled = false)
    public void fixupSupport3871ChangeSampleName() throws Exception {
        userBean.loginOSUser();

        List<String> sampleUpdateLines = IOUtils.readLines(VarioskanParserTest.getTestResource("AlterSampleName.txt"));

        for(int i = 1; i < sampleUpdateLines.size(); i++) {
            String[] fields = LabVesselFixupTest.WHITESPACE_PATTERN.split(sampleUpdateLines.get(i));
            if(fields.length != 2) {
                throw new RuntimeException("Expected two white-space separated fields in " + sampleUpdateLines.get(i));
            }

            MercurySample sample = mercurySampleDao.findBySampleKey(fields[0]);

            Assert.assertNotNull(sample, fields[0] + " not found");
            final String replacementSampleKey = fields[0] + "_bad_sample";
            sample.getMetadata().add(new Metadata(Metadata.Key.BROAD_SAMPLE_ID, replacementSampleKey));
            sample.getMetadata().add(new Metadata(Metadata.Key.BROAD_2D_BARCODE, fields[1]+"_bad_vessel"));
            System.out.println("Changing " + sample.getSampleKey() + " to " + replacementSampleKey);
            sample.setSampleKey(replacementSampleKey);
        }

        mercurySampleDao.persist(new FixupCommentary(sampleUpdateLines.get(0)));
        mercurySampleDao.flush();
    }

    /**
     * This test reads its parameters from a file, mercury/src/test/resources/testdata/AddSampleToVessel.txt,
     * so it can be used for other similar fixups, without writing a new test.  It is used to add BSP samples to
     * vessels that are the result of messages.  Example contents of the file are (first line is the fixup commentary,
     * subsequent lines are whitespace separated vessel barcode and sample ID):
     * SUPPORT-3907 reflect BSP daughter transfer
     * SM-H5GZC SM-H5GZC
     * SM-H5GZI SM-H5GZI
     */
    @Test(enabled = false)
    public void fixupSupport3907() throws IOException {
        userBean.loginOSUser();

        List<String> lines = IOUtils.readLines(VarioskanParserTest.getTestResource("AddSampleToVessel.txt"));
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            // Allow commenting out of lines, to recover from errors
            if (line.startsWith("#")) {
                continue;
            }
            String[] fields = LabVesselFixupTest.WHITESPACE_PATTERN.split(line);
            if (fields.length != 2) {
                throw new RuntimeException("Expected two white-space separated fields in " + line);
            }
            String barcode = fields[0];
            LabVessel labVessel = labVesselDao.findByIdentifier(barcode);
            Assert.assertNotNull(labVessel, barcode + " not found");
            String sampleKey = fields[1];
            MercurySample mercurySample = mercurySampleDao.findBySampleKey(sampleKey);
            if (mercurySample == null) {
                mercurySample = new MercurySample(sampleKey, MercurySample.MetadataSource.BSP);
            }
            if (!mercurySample.getLabVessel().isEmpty()) {
                throw new RuntimeException("Sample " + sampleKey + " is already associated with vessel " +
                        mercurySample.getLabVessel().iterator().next().getLabel());
            }
            System.out.println("Adding " + mercurySample.getSampleKey() + " to " + labVessel.getLabel());
            labVessel.addSample(mercurySample);
            // Limit the size of each transaction, to avoid overloading FixUpEtl
            if (i % 100 == 0) {
                labVesselDao.persist(new FixupCommentary(lines.get(0)));
                labVesselDao.flush();
            }
        }

        labVesselDao.persist(new FixupCommentary(lines.get(0)));
        labVesselDao.flush();
    }

    /**
     * This test reads its parameters from a file, mercury/src/test/resources/testdata/UpdateSampleToRoot.txt,
     * so it can be used for other similar fixups, without writing a new test.  This is used to set the
     * isRoot indicator on the given Mercury Sample.  Example contents of the file are (first line is the fixup commentary,
     * subsequent lines are sample ID):
     * SUPPORT-4707 mark sample as root
     * SM-H5GZC
     * SM-H5GZI
     */
    @Test(enabled = false)
    public void fixupSuppor4707() throws IOException {
        userBean.loginOSUser();

        List<String> lines = IOUtils.readLines(VarioskanParserTest.getTestResource("UpdateSampleToRoot.txt"));
        for (int i = 1; i < lines.size(); i++) {
            String[] fields = LabVesselFixupTest.WHITESPACE_PATTERN.split(lines.get(i));
            if (fields.length != 1) {
                throw new RuntimeException("Expected one white-space separated fields in " + lines.get(i));
            }
            String sampleKey = fields[0];
            MercurySample mercurySample = mercurySampleDao.findBySampleKey(sampleKey);
            if (mercurySample == null) {
                throw new RuntimeException("Failed to find Mercury Sample " + sampleKey);
            }
            System.out.println("Setting " + sampleKey + " is root to true.");
            mercurySample.setRoot(true);
        }

        labVesselDao.persist(new FixupCommentary(lines.get(0)));
        labVesselDao.flush();
    }

    /**
     * This test reads its parameters from a file, mercury/src/test/resources/testdata/ReplaceSampleInVessel.txt,
     * so it can be used for other similar fixups, without writing a new test.  It is used to replace samples in
     * specified vessels.  Example contents of the file are (first line is the fixup commentary,
     * subsequent lines are whitespace separated vessel barcode, old sample ID, new sample ID):
     * SUPPORT-4271 reflect changes to array plates
     * CO-26671753A01 SM-H5GZC SM-HK74N
     * CO-26671756A01 SM-H5GZI SM-HK74M
     */
    @Test(enabled = false)
    public void fixupSupport4271() throws IOException {
        userBean.loginOSUser();

        List<String> lines = IOUtils.readLines(VarioskanParserTest.getTestResource("ReplaceSampleInVessel.txt"));
        for (int i = 1; i < lines.size(); i++) {
            String[] fields = LabVesselFixupTest.WHITESPACE_PATTERN.split(lines.get(i));
            if (fields.length != 3) {
                throw new RuntimeException("Expected three white-space separated fields in " + lines.get(i));
            }
            String barcode = fields[0];
            LabVessel labVessel = labVesselDao.findByIdentifier(barcode);
            Assert.assertNotNull(labVessel, barcode + " not found");

            Map<String, MercurySample> mapIdToMercurySample = mercurySampleDao.findMapIdToMercurySample(
                    Arrays.asList(fields[1], fields[2]));
            MercurySample oldSample = mapIdToMercurySample.get(fields[1]);
            Assert.assertNotNull(oldSample);
            MercurySample newSample = mapIdToMercurySample.get(fields[2]);
            Assert.assertNotNull(newSample);
            labVessel.getMercurySamples().remove(oldSample);
            System.out.println("Adding " + newSample.getSampleKey() + " to " + labVessel.getLabel());
            labVessel.addSample(newSample);
        }

        labVesselDao.persist(new FixupCommentary(lines.get(0)));
        labVesselDao.flush();
    }
}
