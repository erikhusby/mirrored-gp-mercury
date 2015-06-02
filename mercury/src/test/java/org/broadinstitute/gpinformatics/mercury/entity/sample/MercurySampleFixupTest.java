package org.broadinstitute.gpinformatics.mercury.entity.sample;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

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
    private UserTransaction utx;

    private static final String RECEIVED_DATE_UPDATE_FORMAT = "MM/dd/yyyy";

    @Deployment
    public static WebArchive buildMercuryWar() {

        /*
         * If the need comes to utilize this fixup in production, change the buildMercuryWar parameters accordingly
         */
        return DeploymentBuilder.buildMercuryWar(
                org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.PROD, "PROD");
    }

    @BeforeMethod(groups = TestGroups.FIXUP)
    public void setUp() throws Exception {
        if (userBean == null) {
            return;
        }
        userBean.loginOSUser();
        utx.begin();
    }

    @AfterMethod(groups = TestGroups.FIXUP)
    public void tearDown() throws Exception {

        if (userBean == null) {
            return;
        }
        userBean.logout();
        utx.commit();
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

    @Test(groups = TestGroups.FIXUP, enabled = false)
    public void backfillReceiptForSamples_GPLIM3487() {
        Map<String, MercurySample> nonReceivedSamplesByKey = mercurySampleDao.findNonReceivedCrspSamples();

        List<SampleReceiptFixup> sampleReceiptFixupList = getSamplesToFixup(nonReceivedSamplesByKey);

        int counter = 0;
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
                        .next().setReceiptEvent(userBean.getBspUserByUsername(currentFixup.getReceiptUserName()),
                        currentFixup.getReceiptDate(), counter++);
                updatedSamples.add(currentFixup.getReceivedSample().getSampleKey());
            }
            lastDate = currentFixup.getReceiptDate();
        }

        mercurySampleDao.persist(new FixupCommentary(String.format("Added receipt dates for %d samples: %s",
                updatedSamples.size(), StringUtils.join(updatedSamples, "\n"))));
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
}
