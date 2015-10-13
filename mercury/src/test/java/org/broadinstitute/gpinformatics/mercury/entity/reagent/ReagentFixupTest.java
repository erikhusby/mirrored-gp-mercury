package org.broadinstitute.gpinformatics.mercury.entity.reagent;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.GenericReagentDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.ReagentDesignDao;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent_;
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
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Fixups for reagents.
 */
@Test(groups = TestGroups.FIXUP)
public class ReagentFixupTest extends Arquillian {

    @Inject
    private ReagentDesignDao reagentDesignDao;

    @Inject
    private GenericReagentDao genericReagentDao;

    @Inject
    private UserBean userBean;

    @Inject
    private LabEventDao labEventDao;

    @Inject
    private UserTransaction utx;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @Test(enabled = false)
    public void fixupGplim2824() {
        // requires setting updatable = true on ReagentDesign.designName (On Product, bait is referred to by name,
        // not PK).
        ReagentDesign reagentDesign = reagentDesignDao.findByBusinessKey("Buick_v6.0_2014");
        reagentDesign.setDesignName("Buick_v6_0_2014");
        reagentDesignDao.flush();
    }

    @Test(enabled = false)
    public void gplim3136fixupLot() {
        userBean.loginOSUser();
        Map<Long, String> map = new HashMap<Long, String> () {{
            put(631019L, "000017279828");
            put(631020L, "WO0000109974644");
        }};
        for (Long id : map.keySet()) {
            Reagent reagent = reagentDesignDao.findById(Reagent.class, id);
            if (reagent == null) {
                throw new RuntimeException("cannot find " + id);
            }
            System.out.println("Reagent lot " + reagent.getLot());
            reagent.setLot(map.get(id));
            System.out.println("   updated to " + reagent.getLot());
        }
        reagentDesignDao.flush();
    }


    @Test(enabled = false)
    public void gplim3132fixupLotAndDate() throws Exception {
        userBean.loginOSUser();

        // Lot 14D24A0015 is the wrong and should be 14D24A0013 with an expiration date of 2/17/15.
        // Existing lot 14D24A0013 should be set to the same expiration date.
        String correctLot = "14D24A0013";
        Date correctDate = (new SimpleDateFormat("MM/dd/yyyy")).parse("02/17/2015");
        List<Reagent> reagents = reagentDesignDao.findListByList(Reagent.class, Reagent_.lot,
                Arrays.asList(new String[]{"14D24A0015", correctLot}));
        int count = 0;
        for (Reagent reagent : reagents) {
            if (reagent.getName().equals("ET2")) {
                ++count;
                System.out.println("Reagent lot " + reagent.getLot());
                if (!correctLot.equals(reagent.getLot())) {
                    reagent.setLot(correctLot);
                }
                reagent.setExpiration(correctDate);
                System.out.println("   updated to " + reagent.getLot() + " expiring " + reagent.getExpiration());
            }
        }
        Assert.assertEquals(count, 2);
        reagentDesignDao.flush();
    }

    @Test(enabled = false)
    public void gplim3158expirationDates() throws Exception {
        userBean.loginOSUser();
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy");
        Map<String, String> map = new HashMap<String, String>() {{
            put("WO0000109974644", "07-09-2015");
            put("000017279828",    "01-07-2015");
            put("14D16A0004", "09-20-2015");
            put("13l02a0005", "12-02-2014");
            put("13L19A0017", "12-19-2014");
            put("14D02A0012", "07-01-2015");
            put("14A14A0006", "01-14-2015");
            put("14F24A0022", "06-30-2015");
            put("9935456",    "05-13-2015");
            put("13L02A0005", "12-02-2014");
            put("14G11A0023", "08-08-2015");
            put("12J10A0010", "06-30-2016");
            put("14G09A0020", "07-31-2015");
            put("14A07A0015", "09-30-2015");
            put("14B14A0011", "02-14-2015");
            put("14a14a0006", "01-14-2015");
            put("14I03A0019", "07-09-2015");
            put("13l19a0017", "12-19-2014");
            put("14D24A0015", "02-14-2015");
        }};
        for (String lot : map.keySet()) {
            List<Reagent> reagents = reagentDesignDao.findListByList(Reagent.class, Reagent_.lot,
                    Collections.singleton(lot));
            Assert.assertTrue(CollectionUtils.isNotEmpty(reagents), "cannot find " + lot);
            for (Reagent reagent : reagents) {
                System.out.println("Updating reagent " + reagent.getName() + " lot " + reagent.getLot());
                Date date = sdf.parse(map.get(reagent.getLot()));
                reagent.setExpiration(date);
                System.out.println("   now expires " + sdf.format(reagent.getExpiration()));
            }
            reagentDesignDao.flush();
        }
    }

    @Test(enabled = false)
    public void gplim3370addExpirationDates() throws Exception {
        Multimap<String, Reagent> barcodeReagentMap = HashMultimap.create();

        // Date formats to try when parsing the expiration dates found in bettalims messages.
        // Expiration dates are truncated to midnight, so these formats ignore the time part.
        final SimpleDateFormat[] sdfs = new SimpleDateFormat[]{
                new SimpleDateFormat("yyyy-M-d"),
                new SimpleDateFormat("M-d-y"),
                new SimpleDateFormat("M/d/y"),
                new SimpleDateFormat("y-d-M"),
                new SimpleDateFormat("d-M-y"),
        };

        // These define the expected date range for reagents.  Dates outside of this range
        // are assumed to be incorrectly parsed and are rejected.  All correctly parsed
        // expiration dates are within this interval.
        final Date firstDate = sdfs[0].parse("2014-01-01");
        final Date lastDate = sdfs[0].parse("2018-01-01");

        // Map of <reagent name & lot> --> <expiration>
        final Map<String, Date> reagentMap = new HashMap<>();
        final String delimiter = "__";

        userBean.loginOSUser ();

        // Reads a pre-computed file of <reagent ...> elements extracted from bettalims messages.
        //
        // This file can be recreated from node1000 (or other server farm machine):
        // $ cd /seq/lims/mercury/prod/bettalims/inbox
        // $ grep -l 'expiration=' 201[45]*/* | while read i
        // do echo $i >> ~/gplim3370_expirationMsgFilenames
        // cat $i | tr '\n' ' ' | tr '<' '\n' | grep 'reagent' | grep 'expiration' | sed -e 's!/>!!' >> ~/gplim3370_tmp
        // done
        // $ sort -u ~/gplim3370_tmp > ~/gplim3370_expirationRecords
        //
        // Then scp the file to the location specified in the next line:
        File file = new File("/huge/3370/gplim3370_expirationRecords");
        Assert.assertTrue(file.exists());

        // Parses each line and extracts barcode, kitType, and expiration, which may appear in any order in one line.
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;
        while ((line = reader.readLine()) != null) {
            if (StringUtils.isBlank(line)) {
                continue;
            }
            int barcodeIdx = line.indexOf("barcode=");
            Assert.assertTrue(barcodeIdx > -1, line);
            String barcode = line.substring(barcodeIdx).split("\"")[1].trim();
            barcodeReagentMap.put(barcode, null);

            int kitIdx = line.indexOf("kitType=");
            Assert.assertTrue(kitIdx > -1, line);
            String kitType = line.substring(kitIdx).split("\"")[1].trim();

            int expIdx = line.indexOf("expiration=");
            Assert.assertTrue(expIdx > -1, line);
            String expiration = line.substring(expIdx).split("\"")[1].trim();

            // Fixes some obvious flaws found in the date.
            if (expiration.startsWith("12015-05-25")) {
                expiration = "2015-05-25";
            } else if (expiration.startsWith("302015-09-30")) {
                expiration = "2015-09-30";
            } else if (expiration.startsWith("42015-08-26")) {
                expiration = "2015-08-26";
            }

            // Makes expiration be more uniformly parseable.
            String expirationNoT = expiration.replace('T', ' ');

            // Attempts to parse using the various data formats until a valid date is obtained.
            // Some dates can be parsed month-day and also day-month.  That ambiguity cannot be
            // resolved here. Month-day will be used unless the day-month format is required.
            Date date = null;
            outerLoop:
            for (SimpleDateFormat sdf : sdfs) {
                for (String expirationPart : expirationNoT.split(" ")) {
                    try {
                        date = sdf.parse(expirationPart);
                        if (date.after(firstDate) && date.before(lastDate)) {
                            break outerLoop;
                        } else {
                            date = null;
                        }
                    } catch (Exception e) {
                        // Just falls through with date = null
                    }
                }
            }
            Assert.assertNotNull(date, "Cannot parse date: '" + expiration +"'");
            reagentMap.put(barcode + delimiter + kitType, date);
        }

        // To make only one transaction, does the dao lookup outside the update iteration, and persists a collection
        // of new entities.
        for (Reagent reagent : reagentDesignDao.findListByList(Reagent.class, Reagent_.lot, barcodeReagentMap.keys())) {
            barcodeReagentMap.put(reagent.getLot(), reagent);
        }
        List<Object> entitiesToPersist = new ArrayList<>();

        // Iterates on the collected barcode, kitType, and date records.  The reagent is added to Mercury
        // if the barcode & kitType combination is not found.  Reagents already in Mercury but without an
        // expiration date get updates with the expiration date.
        List<Reagent> reagentsToAdd = new ArrayList<>();
        for (Map.Entry<String, Date> entry : reagentMap.entrySet()) {
            String barcode = entry.getKey().split(delimiter)[0];
            String kitType = entry.getKey().split(delimiter)[1];
            Date date = entry.getValue();
            Collection<Reagent> reagents = barcodeReagentMap.get(barcode);
            boolean found = false;
            // Iterates on the reagents all having the given barcode, looking for the kit type.
            if (CollectionUtils.isNotEmpty(reagents)) {
                for (Reagent reagent : reagents) {
                    if (reagent != null && reagent.getName().equals(kitType)) {
                        found = true;
                        if (reagent.getExpiration() == null) {
                            // Reagent has no date.
                            System.out.println("Updating reagent " + reagent.getName() + " lot " + reagent.getLot() +
                                               " expiration from null to " + sdfs[0].format(date));
                            reagent.setExpiration(date);
                        } else if (date.compareTo(sdfs[0].parse(sdfs[0].format(reagent.getExpiration()))) != 0) {
                            // Reagent has a date but it differs from the one in the bettalims message.
                            // Leaves the existing one in place and informs us about it.
                            System.out.println("Existing reagent " + reagent.getName() + " lot " + reagent.getLot() +
                                               " expiration is " + sdfs[0].format(reagent.getExpiration()) +
                                               " but message date is " + sdfs[0].format(date));
                        }
                    }
                }
            }
            if (!found) {
                System.out.println("Adding reagent " + kitType + " lot " + barcode + " expiration " +
                                   sdfs[0].format(date));
                entitiesToPersist.add(new GenericReagent(kitType, barcode, date));
            }
        }
        entitiesToPersist.add(new FixupCommentary("GPLIM-3370 add missing reagents and expiration dates"));
        reagentDesignDao.persistAll(entitiesToPersist);
        reagentDesignDao.flush();
    }

    @Test(enabled = false)
    public void fixupQual676(){
        /*
        Used this query to find duplicate reagents, and the events that they are associated with:
        SELECT
            reagent.reagent_id,
            reagent.lot,
            reagent.reagent_name,
            le.lab_event_id,
            le.lab_event_type
        FROM
            reagent
            INNER JOIN lab_event_reagents ler
                ON   ler.reagents = reagent.reagent_id
            INNER JOIN lab_event le
                ON   le.lab_event_id = ler.lab_event
        WHERE
            (reagent.reagent_name, reagent.lot) IN (SELECT
                                                        reagent_name,
                                                        lot
                                                    FROM
                                                        reagent
                                                    GROUP BY
                                                        reagent_name,
                                                        lot
                                                    HAVING
                                                        COUNT(*) > 1)
        ORDER BY
            reagent.reagent_name,
            le.lab_event_id;

        931959	10046231	Cleavage Reagent Master Mix	854895	DILUTION_TO_FLOWCELL_TRANSFER
        931970	10046231	Cleavage Reagent Master Mix	854896	DILUTION_TO_FLOWCELL_TRANSFER
        631024	14D24A0013	ET2	688467	ICE_1ST_CAPTURE
        631027	14D24A0013	ET2	688625	ICE_2ND_CAPTURE
        931966	10029679	Fast Amplification Premix	854895	DILUTION_TO_FLOWCELL_TRANSFER
        931978	10029679	Fast Amplification Premix	854896	DILUTION_TO_FLOWCELL_TRANSFER
        931958	10029663	Fast Amplifixation Mix	854895	DILUTION_TO_FLOWCELL_TRANSFER
        931985	10029663	Fast Amplifixation Mix	854896	DILUTION_TO_FLOWCELL_TRANSFER
        931957	10029690	Fast Denaturation Reagent	854895	DILUTION_TO_FLOWCELL_TRANSFER
        931979	10029690	Fast Denaturation Reagent	854896	DILUTION_TO_FLOWCELL_TRANSFER
        931962	10032298	Fast Linearization Mix 1	854895	DILUTION_TO_FLOWCELL_TRANSFER
        931974	10032298	Fast Linearization Mix 1	854896	DILUTION_TO_FLOWCELL_TRANSFER
        931956	10037762	Fast Linearization Mix 2	854895	DILUTION_TO_FLOWCELL_TRANSFER
        931976	10037762	Fast Linearization Mix 2	854896	DILUTION_TO_FLOWCELL_TRANSFER
        931954	10037735	Fast Resynthesis Mix	854895	DILUTION_TO_FLOWCELL_TRANSFER
        931981	10037735	Fast Resynthesis Mix	854896	DILUTION_TO_FLOWCELL_TRANSFER
        931963	10052129	Incorporation Master Mix	854895	DILUTION_TO_FLOWCELL_TRANSFER
        931983	10052129	Incorporation Master Mix	854896	DILUTION_TO_FLOWCELL_TRANSFER
        931967	10029525	Primer Mix Index i7	854895	DILUTION_TO_FLOWCELL_TRANSFER
        931972	10029525	Primer Mix Index i7	854896	DILUTION_TO_FLOWCELL_TRANSFER
        931960	10037740	Primer Mix Read 1	854895	DILUTION_TO_FLOWCELL_TRANSFER
        931971	10037740	Primer Mix Read 1	854896	DILUTION_TO_FLOWCELL_TRANSFER
        931965	10035675	Primer Mix Read 2	854895	DILUTION_TO_FLOWCELL_TRANSFER
        931984	10035675	Primer Mix Read 2	854896	DILUTION_TO_FLOWCELL_TRANSFER
        931968	10048313	Scan Reagent	854895	DILUTION_TO_FLOWCELL_TRANSFER
        931977	10048313	Scan Reagent	854896	DILUTION_TO_FLOWCELL_TRANSFER
        931969	15C11A0046	TruSeq Rapid PE Cluster Kit	854895	DILUTION_TO_FLOWCELL_TRANSFER
        931980	15C11A0046	TruSeq Rapid PE Cluster Kit	854896	DILUTION_TO_FLOWCELL_TRANSFER
        931955	15C11A0042	TruSeq Rapid SBS Kit	854895	DILUTION_TO_FLOWCELL_TRANSFER
        931973	15C11A0042	TruSeq Rapid SBS Kit	854896	DILUTION_TO_FLOWCELL_TRANSFER
        931961	10046204	Universal Sequencing Buffer	854895	DILUTION_TO_FLOWCELL_TRANSFER
        931964	10046204	Universal Sequencing Buffer	854895	DILUTION_TO_FLOWCELL_TRANSFER
        931975	10046204	Universal Sequencing Buffer	854896	DILUTION_TO_FLOWCELL_TRANSFER
        931982	10046204	Universal Sequencing Buffer	854896	DILUTION_TO_FLOWCELL_TRANSFER

        For each pair, fetch the second event and both reagents.  Remove the second reagent from the second event,
          and add the first reagent.  Delete the second reagent.
         */
        try {
            userBean.loginOSUser();
            utx.begin();

            int[][] ids = {
                    {931959, /* 10046231Cleavage Reagent Master Mix 854895DILUTION_TO_FLOWCELL_TRANSFER*/
                    931970, /*10046231Cleavage Reagent Master Mix */854896/*DILUTION_TO_FLOWCELL_TRANSFER*/},
                    // The following pair aren't directly related, but are preparation for adding a unique constraint.
                    {631024,/* 14D24A0013 ET2 688467ICE_1ST_CAPTURE*/
                    631027, /*14D24A0013 ET2 */688625/*ICE_2ND_CAPTURE*/},
                    {931966, /*10029679Fast Amplification Premix 854895DILUTION_TO_FLOWCELL_TRANSFER*/
                    931978, /*10029679Fast Amplification Premix */854896/*DILUTION_TO_FLOWCELL_TRANSFER*/},
                    {931958, /*10029663Fast Amplifixation Mix 854895DILUTION_TO_FLOWCELL_TRANSFER*/
                    931985, /*10029663Fast Amplifixation Mix */854896/*DILUTION_TO_FLOWCELL_TRANSFER*/},
                    {931957, /*10029690Fast Denaturation Reagent 854895DILUTION_TO_FLOWCELL_TRANSFER*/
                    931979, /*10029690Fast Denaturation Reagent */854896/*DILUTION_TO_FLOWCELL_TRANSFER*/},
                    {931962, /*10032298Fast Linearization Mix 1 854895DILUTION_TO_FLOWCELL_TRANSFER*/
                    931974, /*10032298Fast Linearization Mix 1 */854896/*DILUTION_TO_FLOWCELL_TRANSFER*/},
                    {931956, /*10037762Fast Linearization Mix 2 854895DILUTION_TO_FLOWCELL_TRANSFER*/
                    931976, /*10037762Fast Linearization Mix 2 */854896/*DILUTION_TO_FLOWCELL_TRANSFER*/},
                    {931954, /*10037735Fast Resynthesis Mix 854895DILUTION_TO_FLOWCELL_TRANSFER*/
                    931981, /*10037735Fast Resynthesis Mix */854896/*DILUTION_TO_FLOWCELL_TRANSFER*/},
                    {931963, /*10052129Incorporation Master Mix 854895DILUTION_TO_FLOWCELL_TRANSFER*/
                    931983, /*10052129Incorporation Master Mix */854896/*DILUTION_TO_FLOWCELL_TRANSFER*/},
                    {931967, /*10029525Primer Mix Index i7 854895DILUTION_TO_FLOWCELL_TRANSFER*/
                    931972, /*10029525Primer Mix Index i7 */854896/*DILUTION_TO_FLOWCELL_TRANSFER*/},
                    {931960, /*10037740Primer Mix Read 1 854895DILUTION_TO_FLOWCELL_TRANSFER*/
                    931971, /*10037740Primer Mix Read 1 */854896/*DILUTION_TO_FLOWCELL_TRANSFER*/},
                    {931965, /*10035675Primer Mix Read 2 854895DILUTION_TO_FLOWCELL_TRANSFER*/
                    931984, /*10035675Primer Mix Read 2 */854896/*DILUTION_TO_FLOWCELL_TRANSFER*/},
                    {931968, /*10048313Scan Reagent 854895DILUTION_TO_FLOWCELL_TRANSFER*/
                    931977, /*10048313Scan Reagent */854896/*DILUTION_TO_FLOWCELL_TRANSFER*/},
                    {931969, /*15C11A0046 TruSeq Rapid PE Cluster Kit 854895DILUTION_TO_FLOWCELL_TRANSFER*/
                    931980, /*15C11A0046 TruSeq Rapid PE Cluster Kit */854896/*DILUTION_TO_FLOWCELL_TRANSFER*/},
                    {931955, /*15C11A0042 TruSeq Rapid SBS Kit 854895DILUTION_TO_FLOWCELL_TRANSFER*/
                    931973, /*15C11A0042 TruSeq Rapid SBS Kit */854896/*DILUTION_TO_FLOWCELL_TRANSFER*/},
                    {931961, /*10046204Universal Sequencing Buffer 854895DILUTION_TO_FLOWCELL_TRANSFER*/
                    931964, /*10046204Universal Sequencing Buffer */854895/*DILUTION_TO_FLOWCELL_TRANSFER*/},
                    {931975, /*10046204Universal Sequencing Buffer 854896DILUTION_TO_FLOWCELL_TRANSFER*/
                    931982, /*10046204Universal Sequencing Buffer */854896/*DILUTION_TO_FLOWCELL_TRANSFER*/},
                    // Added this pair after the first run of this fixup, because there were still duplicates
                    {931961,/*	10046204	Universal Sequencing Buffer	854895	DILUTION_TO_FLOWCELL_TRANSFER*/
                    931975,	/*10046204	Universal Sequencing Buffer	*/854896/*	DILUTION_TO_FLOWCELL_TRANSFER*/},
            };

            for (int[] triple : ids) {
                GenericReagent correctReagent = genericReagentDao.findById(GenericReagent.class, (long) triple[0]);
                GenericReagent duplicateReagent = genericReagentDao.findById(GenericReagent.class, (long) triple[1]);
                LabEvent labEvent = labEventDao.findById(LabEvent.class, (long) triple[2]);
                System.out.println("For event " + labEvent.getLabEventId() + ", merge " + duplicateReagent.getName());
                labEvent.getReagents().remove(duplicateReagent);
                labEvent.getReagents().add(correctReagent);
                genericReagentDao.remove(duplicateReagent);
            }
            genericReagentDao.persist(new FixupCommentary("QUAL-676 reagent merge"));
            genericReagentDao.flush();
            utx.commit();
        } catch (NotSupportedException | SystemException | HeuristicRollbackException | RollbackException |
                HeuristicMixedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test(enabled = false)
    public void fixupSupport565(){
        userBean.loginOSUser();
        GenericReagent genericReagent = genericReagentDao.findByReagentNameAndLot("HS buffer", "91Q33120101670146301");
        System.out.print("Updating " + genericReagent.getLot());
        genericReagent.setLot("RG-8252");
        System.out.println(" to " + genericReagent.getLot());
        genericReagentDao.persist(new FixupCommentary("SUPPORT-565 reagent fixup"));
        genericReagentDao.flush();
    }

    @Test(enabled = false)
    public void fixupSupport565Try2(){
        try {
            userBean.loginOSUser();
            utx.begin();
            GenericReagent correctReagent = genericReagentDao.findById(GenericReagent.class, 628953L);
            GenericReagent duplicateReagent = genericReagentDao.findById(GenericReagent.class, 929980L);
            List<LabEvent> labEvents = labEventDao.findListByList(LabEvent.class, LabEvent_.labEventId, Arrays.asList(
                    846510L,
                    846511L,
                    846512L,
                    846486L,
                    846487L,
                    846488L,
                    846500L,
                    846501L,
                    846502L,
                    846522L,
                    846523L,
                    846524L,
                    846619L,
                    846620L,
                    846621L,
                    846723L,
                    846724L,
                    846725L,
                    846830L,
                    846831L,
                    846832L,
                    846838L,
                    846839L,
                    846840L,
                    846845L,
                    846846L,
                    846847L,
                    846857L,
                    846858L,
                    846859L));
            for (LabEvent labEvent : labEvents) {
                labEvent.getReagents().remove(duplicateReagent);
                labEvent.getReagents().add(correctReagent);
            }
            genericReagentDao.remove(duplicateReagent);
            genericReagentDao.persist(new FixupCommentary("SUPPORT-565 reagent merge"));
            genericReagentDao.flush();
            utx.commit();
        } catch (NotSupportedException | SystemException | HeuristicRollbackException | RollbackException |
                HeuristicMixedException e) {
            throw new RuntimeException(e);
        }
    }
    @Test(enabled = false)
    public void fixupSupport660() {
        // Used SQL to verify that RG-8552 is used only by the 9 events in question, so it's safe to change it.
        userBean.loginOSUser();
        GenericReagent genericReagentRg150 = genericReagentDao.findByReagentNameAndLot("HS buffer", "RG-150");
        Assert.assertNull(genericReagentRg150);
        GenericReagent genericReagentRg8552 = genericReagentDao.findByReagentNameAndLot("HS buffer", "RG-8552");
        genericReagentRg8552.setLot("RG-150");
        genericReagentDao.persist(new FixupCommentary("SUPPORT-660 change lot"));
        genericReagentDao.flush();
    }

    @Test(enabled = false)
    public void fixupGplim3538ReagentBarcode() {
        userBean.loginOSUser();
        // Change lab_event 872452 reagents from reagentId 813365 (EWS reagent having lot 10D06A0004)
        // to reagentId 929964 (EWS reagent having lot BATCH-001).
        Reagent reagentEws10d06a0004 = genericReagentDao.findById(GenericReagent.class, 813365L);
        Assert.assertEquals(reagentEws10d06a0004.getName(), "EWS");
        Assert.assertEquals(reagentEws10d06a0004.getLot(), "10D06A0004");

        LabEvent labEvent = labEventDao.findById(LabEvent.class, 872452L);
        Assert.assertTrue(labEvent.getReagents().remove(reagentEws10d06a0004));

        Reagent reagentEwsBatch001 = genericReagentDao.findById(GenericReagent.class, 929964L);
        Assert.assertEquals(reagentEwsBatch001.getName(), "EWS");
        Assert.assertEquals(reagentEwsBatch001.getLot(), "BATCH-001");

        labEvent.addReagent(reagentEwsBatch001);
        Assert.assertEquals(labEvent.getReagents().size(), 3);

        // Change lot from 'rgt4828222' to '10029298' (Mercury reagent id 933959).
        // This reagent is used in only one lab event, so it's safe to update it.
        Reagent reagentRgt = genericReagentDao.findById(Reagent.class, 933959L);
        Assert.assertEquals(reagentRgt.getLot(), "rgt4828222");
        reagentRgt.setLot("10029298");
        genericReagentDao.persist(new FixupCommentary("GPLIM-3538 change lot due to reagent script failure"));
        genericReagentDao.flush();
    }

    @Test(enabled = false)
    public void fixupGplim3588() {
        userBean.loginOSUser();
        // Change reagent on labEventId 897423
        // from "Rapid Capture Kit Box 2 (HP3, EE1)" lot 15C11A0054 expiration 10/22/2015 (reagentId 934964)
        // to same type reagent but with lot 15C27A0012 and expiration 11/25/15 (reagentId 937963).
        Reagent undesired = genericReagentDao.findByReagentNameLotExpiration("Rapid Capture Kit Box 2 (HP3, EE1)",
                "15C11A0054", new GregorianCalendar(2015, Calendar.OCTOBER, 22).getTime());
        Assert.assertNotNull(undesired);

        Reagent desired = genericReagentDao.findByReagentNameLotExpiration(undesired.getName(),
                "15C27A0012", new GregorianCalendar(2015, Calendar.NOVEMBER, 25).getTime());
        Assert.assertNotNull(desired);

        LabEvent labEvent = labEventDao.findById(LabEvent.class, 897423L);
        Assert.assertTrue(labEvent.getReagents().remove(undesired));
        labEvent.addReagent(desired);

        genericReagentDao.persist(new FixupCommentary("GPLIM-3588 change reagent used on Ice Capture 2 event."));
        genericReagentDao.flush();
    }

    @Test(enabled = false)
    public void fixupGplim3712() {
        userBean.loginOSUser();
        Reagent undesired = genericReagentDao.findByReagentNameLotExpiration("HS buffer",
                "91Q33120101689868301", null);
        Assert.assertNotNull(undesired);

        Reagent desired = genericReagentDao.findByReagentNameLotExpiration(undesired.getName(),
                "RG-8252", null);
        Assert.assertNotNull(desired);

        LabEvent labEvent = labEventDao.findById(LabEvent.class, 998742L);
        System.out.println("Replacing reagent " + undesired.getReagentId() + " with " + desired.getReagentId() +
                " for event " + labEvent.getLabEventId());
        Assert.assertTrue(labEvent.getReagents().remove(undesired));
        labEvent.addReagent(desired);

        labEvent = labEventDao.findById(LabEvent.class, 998747L);
        System.out.println("Replacing reagent " + undesired.getReagentId() + " with " + desired.getReagentId() +
                           " for event " + labEvent.getLabEventId());
        Assert.assertTrue(labEvent.getReagents().remove(undesired));
        labEvent.addReagent(desired);

        genericReagentDao.persist(new FixupCommentary("GPLIM-3712 change reagent used on PicoBufferAddition event."));
        genericReagentDao.flush();
    }

    @Test(enabled = false)
    public void fixupGplim3743() {
        userBean.loginOSUser();

        // Replaces Buffer ATE reagent on event 1020154 with a new reagent having lot 151022297, exp 17-APR-2016
        LabEvent labEvent = genericReagentDao.findById(LabEvent.class, 1020154L);
        Assert.assertNotNull(labEvent);

        String kitType = "Buffer ATE";
        Reagent undesired = null;
        for (Reagent reagent : labEvent.getReagents()) {
            if (reagent.getName().equals(kitType)) {
                Assert.assertNull(undesired);
                undesired = reagent;
            }
        }
        Assert.assertNotNull(undesired);

        String barcode = "151022297";
        Date expiration = new GregorianCalendar(2016, Calendar.APRIL, 17).getTime();
        Assert.assertNull(genericReagentDao.findByReagentNameLotExpiration(kitType, barcode, expiration));
        Reagent desired = new GenericReagent(kitType, barcode, expiration);
        System.out.println("Created reagent " + kitType + " lot " + barcode + " expiration " + expiration);
        Assert.assertTrue(labEvent.getReagents().remove(undesired));
        labEvent.getReagents().add(desired);
        System.out.println("Reagent " + undesired.getReagentId() + " removed and " + desired.getReagentId() +
                           " added to event " + labEvent.getLabEventId());

        genericReagentDao.persist(new FixupCommentary("GPLIM-3743 fixup incorrect Buffer ATE reagent"));
        genericReagentDao.flush();
    }

    @Test(enabled = false)
    public void fixupGplim3787() throws Exception {
        userBean.loginOSUser();
        // Puts a new reagent on two Ice1stBaitPick lab events. The reagent was missing from the bettalims msg.
        String lot = "20008218";
        String type = "Rapid Capture Kit Box 4 (Bait)";
        String expiration = "09/2016";
        List<Long> labEventIds = Arrays.asList(new Long[]{1048673L, 1049279L});

        Reagent reagent = new GenericReagent(type, lot, (new SimpleDateFormat("mm/yyyy")).parse(expiration));
        List<LabEvent> labEvents = labEventDao.findListByList(LabEvent.class, LabEvent_.labEventId, labEventIds);
        Assert.assertEquals(labEvents.size(), labEventIds.size());
        for (LabEvent labEvent : labEvents) {
            System.out.println("Adding reagent to event " + labEvent.getLabEventId());
            labEvent.addReagent(reagent);
        }
        genericReagentDao.persist(new FixupCommentary("GPLIM-3787 add missing reagent."));
        genericReagentDao.flush();
    }

}
