package org.broadinstitute.gpinformatics.mercury.entity.reagent;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.GenericReagentDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.ReagentDesignDao;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventReagent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventReagent_;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent_;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.hibernate.SQLQuery;
import org.hibernate.type.LongType;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
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
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private ProductDao productDao;

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

    @Test(enabled = false)
    public void fixupGplim3787date() throws Exception {
        userBean.loginOSUser();
        // Previous fixup used wrong date format.
        Long reagentId = 975951L;
        String expiration = "09/2016";

        Reagent reagent = genericReagentDao.findById(GenericReagent.class, reagentId);
        Assert.assertNotNull(reagent);
        System.out.println("Changing expiration date on reagent id " + reagentId);
        reagent.setExpiration((new SimpleDateFormat("MM/yyyy")).parse(expiration));
        genericReagentDao.persist(new FixupCommentary("GPLIM-3787 fixup reagent expiration."));
        genericReagentDao.flush();
    }

    // Two fixups:
    // - Adds bait reagents to events from data pulled from the Bravo logs.
    // - Moves bait reagents from Hybridization events to their companion Bait Pick events.
    @Test(enabled = false)
    public void gplim3791backfill() throws Exception {
        userBean.loginOSUser();
        utx.begin();

        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        // All existing RapCap4 bait reagents verified that they have exactly this name.
        final String reagentName = "Rapid Capture Kit Box 4 (Bait)";

        addReagentsToEvents(new String[]{
                "2015-08-26T10:37:10", "Ice1stBaitPick", "03-17-2018", "15F24A0005",
                "2015-08-26T15:45:07", "Ice2ndBaitPick", "03-17-2018", "15F24A0005",
                "2015-09-02T08:15:19", "Ice1stBaitPick", "03-17-2018", "010066231",
                "2015-09-02T13:25:56", "Ice2ndBaitPick", "03-17-2018", "010066231",
                "2015-09-10T09:21:07", "Ice1stBaitPick", "03-17-2018", "15F24A0005",
                "2015-09-10T14:31:08", "Ice2ndBaitPick", "03-17-2018", "15F24A0005",
                "2015-09-16T08:50:01", "Ice1stBaitPick", "03-17-2018", "15F24A0005",
                "2015-09-16T13:56:01", "Ice2ndBaitPick", "03-17-2018", "15F24A0005",
                "2015-09-21T09:08:48", "Ice1stBaitPick", "03-17-2018", "15F24A0005",
                "2015-09-21T14:12:36", "Ice2ndBaitPick", "03-17-2018", "15F24A0005",
                "2015-09-29T09:04:04", "Ice1stBaitPick", "03-17-2018", "10066231",
                "2015-09-29T14:13:05", "Ice2ndBaitPick", "03-17-2018", "15E28A0003",
                //"2015-10-07T08:13:56", "Ice1stBaitPick", "03-17-2018", "010066231", // fixed up in GPLIM-3787
                //"2015-10-07T13:22:53", "Ice2ndBaitPick", "03-17-2018", "010066231", // fixed up in GPLIM-3787
                "2015-10-14T11:13:25", "Ice1stBaitPick", "03-17-2018", "15F24A0005",
                "2015-10-14T16:19:01", "Ice2ndBaitPick", "03-17-2018", "15F24A0005",
        }, reagentName);


        // Finds all the bait reagents that are on hybridization events
        Query query = labEventDao.getEntityManager().createNativeQuery(
                "SELECT lab_event_reagent_id from lab_event_reagents ler, reagent r, lab_event le " +
                "where ler.lab_event = le.lab_event_id and ler.reagents = r.reagent_id " +
                "and r.reagent_name = 'Rapid Capture Kit Box 4 (Bait)' " +
                "and le.lab_event_type like '%HYBRIDIZATION' ");
        query.unwrap(SQLQuery.class).addScalar("lab_event_reagent_id", LongType.INSTANCE);
        List<Long> labEventReagentIds = query.getResultList();
        Assert.assertTrue(labEventReagentIds.size() > 0);
        List<LabEventReagent> labEventReagents = new ArrayList<>(labEventDao.findListByList(LabEventReagent.class,
                LabEventReagent_.labEventReagentId, labEventReagentIds));
        Assert.assertEquals(labEventReagents.size(), labEventReagentIds.size());
        for (LabEventReagent labEventReagent : labEventReagents) {
            LabEvent hybEvent = labEventReagent.getLabEvent();
            LabEvent pickEvent = null;
            for (LabEvent event : labEventDao.findByDate(hybEvent.getEventDate(), hybEvent.getEventDate())) {
                if (event.getLabEventType().getName().contains("BaitPick")) {
                    Assert.assertNull(pickEvent, "Multiple bait picks " + dateFormat.format(hybEvent.getEventDate()));
                    pickEvent = event;
                }
            }
            // Sanity checks the pick event and its reagents.
            Assert.assertNotNull(pickEvent, "Missing bait pick event on " + dateFormat.format(hybEvent.getEventDate()));
            for (Reagent reagent : pickEvent.getReagents()) {
                Assert.assertFalse(reagentName.equals(reagent.getName()),
                        reagentName + " is already on pick event " + pickEvent.getLabEventId());
            }
            Reagent baitReagent = labEventReagent.getReagent();
            System.out.println("Moving " + baitReagent.getName() + " lot " + baitReagent.getLot() +
                               " exp " + dateFormat.format(baitReagent.getExpiration()) +
                               " from " + hybEvent.getLabEventType().getName() + " (" + hybEvent.getLabEventId() + ")" +
                               " on " + dateFormat.format(hybEvent.getEventDate()) +
                               " to " + pickEvent.getLabEventType().getName() + " (" + pickEvent.getLabEventId() + ")");
            pickEvent.addReagent(baitReagent);
            hybEvent.getLabEventReagents().remove(labEventReagent);
            labEventDao.remove(labEventReagent);
        }

        genericReagentDao.persist(new FixupCommentary("GPLIM-3791 backfill missing bait reagents."));
        genericReagentDao.flush();
        utx.commit();

    }

    @Test(enabled = false)
    public void fixupGplim3917date() throws Exception {
        userBean.loginOSUser();

        // Replaces wrong expiration for P5 Indexed Adapter Plate reagent on event 1121392
        LabEvent labEvent = genericReagentDao.findById(LabEvent.class, 1121392L);
        Assert.assertNotNull(labEvent);

        String kitType = "P5 Indexed Adapter Plate";
        String barcode = "000001818323";
        Date expiration = new GregorianCalendar(2016, Calendar.MAY, 21).getTime();

        Reagent reagent = null;
        for (LabEventReagent labEventReagent : labEvent.getLabEventReagents()) {
            if (labEventReagent.getReagent().getName().equals(kitType)) {
                Assert.assertNull(reagent);
                reagent = labEventReagent.getReagent();
            }
        }
        Assert.assertNotNull(reagent);
        Assert.assertEquals(barcode, reagent.getLot());
        Assert.assertNull(genericReagentDao.findByReagentNameLotExpiration(kitType, barcode, expiration));
        System.out.println("Changing expiration date on reagent id " + reagent.getReagentId() + " to " + expiration);
        reagent.setExpiration(expiration);
        genericReagentDao.persist(new FixupCommentary("GPLIM-3917 fixup incorrect P5 adapter expiration"));
        genericReagentDao.flush();
    }

    private void addReagentsToEvents(String[] bravoLogData, String reagentName) throws Exception {
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        final SimpleDateFormat expDateFormat = new SimpleDateFormat("MM-dd-yyyy");

        // A mapping of the newly created reagents so that duplicate creation is not attempted (causes unique key fail).
        Map<String, Reagent> mapExpLotToNewReagent = new HashMap<>();

        Assert.assertEquals(bravoLogData.length % 4, 0, "Bad input array length");

        // Reads in the Bravo data and adds reagents.
        int index = 0;
        while (index < bravoLogData.length) {
            String eventStart = bravoLogData[index++];
            String eventType = bravoLogData[index++];
            String expiration = bravoLogData[index++];
            String lot = bravoLogData[index++];

            Date eventDate = dateFormat.parse(eventStart);
            Date expirationDate = expDateFormat.parse(expiration);

            LabEvent labEvent = null;
            for (LabEvent event : labEventDao.findByDate(eventDate, eventDate)) {
                if (event.getLabEventType().getName().equals(eventType)) {
                    Assert.assertNull(labEvent, "Multiple " + eventType + " events on " + eventStart);
                    labEvent = event;
                }
            }
            if (labEvent == null) {
                // For some reason the bravo log has an event that did not make it to Mercury, or at least
                // not with the expected timestamp. If these occur they should be investigated.
                System.out.println("Mercury does not have " + eventType + " event on " + eventStart);
            } else {
                // Finds any existing reagent of the same type on the lab event.
                Reagent existingReagent = null;
                for (Reagent reagent : labEvent.getReagents()) {
                    if (reagentName.equals(reagent.getName())) {
                        Assert.assertNull(existingReagent, "Found multiple " + reagentName + " reagents on " +
                                                           labEvent.getLabEventId());
                        existingReagent = reagent;
                    }
                }

                // Accepts an existing reagent if it's correct. If existing reagent
                // has incorrect lot or expiration, deletes the reagent from the event.
                // Adds the missing reagent.
                if (existingReagent != null &&
                    lot.equals(existingReagent.getLot()) &&
                    existingReagent.getExpiration() != null &&
                    expirationDate.compareTo(existingReagent.getExpiration()) == 0) {

                    System.out.println("Correct reagent is already on event " + labEvent.getLabEventId());
                } else {

                    // Gets or makes the reagent to use.
                    Reagent reagent = mapExpLotToNewReagent.get(lot + expiration);
                    if (reagent == null) {
                        reagent = genericReagentDao.findByReagentNameLotExpiration(reagentName, lot, expirationDate);
                    }
                    if (reagent == null) {
                        System.out.println("Making new instance of " + reagentName + " lot " + lot +
                                           " exp " + expiration);
                        reagent = new GenericReagent(reagentName, lot, expirationDate);
                        mapExpLotToNewReagent.put(lot + expiration, reagent);
                    }

                    // Removes reagent with incorrect lot or date.
                    if (existingReagent != null) {
                        System.out.println("Removing " + existingReagent.getName() +
                                           " lot " + existingReagent.getLot() +
                                           " exp " + (existingReagent.getExpiration() == null ? "null" :
                                dateFormat.format(reagent.getExpiration())) +
                                           " from " + eventType + " on " + eventStart +
                                           " eventId " + labEvent.getLabEventId());
                        LabEventReagent labEventReagent = labEvent.removeLabEventReagent(existingReagent);
                        genericReagentDao.remove(labEventReagent);
                    }

                    // Adds reagent to the event.
                    System.out.println("Adding " + reagent.getName() + " lot " + reagent.getLot() +
                                       " exp " + dateFormat.format(reagent.getExpiration()) + " to " + eventType +
                                       " on " + eventStart + " eventId " + labEvent.getLabEventId());
                    labEvent.addReagent(reagent);
                }
            }
        }
    }

    @Test(enabled = false)
    public void gplim3791addMissingBaits() throws Exception {
        userBean.loginOSUser();
        utx.begin();

        addReagentsToEvents(new String[]{
                "2015-10-19T10:24:08", "Ice1stBaitPick", "03-17-2018", "15F24A0005",
                "2015-10-19T15:29:25", "Ice2ndBaitPick", "03-17-2018", "15F24A0005",
                "2015-10-22T08:42:39", "Ice1stBaitPick", "03-17-2018", "10066231",
                "2015-10-22T14:01:01", "Ice2ndBaitPick", "03-17-2018", "10066231",
                "2015-10-27T11:27:52", "Ice1stBaitPick", "03-17-2018", "15F24A0005",
                "2015-10-27T16:29:06", "Ice2ndBaitPick", "02-20-2016", "15F24A0005",
                "2015-11-05T07:49:34", "Ice1stBaitPick", "03-17-2018", "10066231",
                "2015-11-05T13:00:51", "Ice2ndBaitPick", "03-17-2017", "10066231",
                "2015-11-09T10:25:23", "Ice1stBaitPick", "03-17-2018", "15F24A0005",
                "2015-11-09T15:36:39", "Ice2ndBaitPick", "03-17-2018", "15F24A0005",
                //"2015-11-12T07:50:34", "Ice1stBaitPick", "03-17-2018", "10066231",  //missing; no 1stBaitPick done on source
                "2015-11-12T12:52:23", "Ice2ndBaitPick", "03-17-2018", "15F24A0005",
        }, "Rapid Capture Kit Box 4 (Bait)");

        genericReagentDao.persist(
                new FixupCommentary("GPLIM-3791 add missing bait reagents using the Bravo log records."));
        genericReagentDao.flush();
        utx.commit();
    }

    @Test(enabled = false)
    public void gplim3850addOrFixQpcrStandards() throws Exception {
        userBean.loginOSUser();
        utx.begin();

        addReagentsToEvents(new String[]{
                "2014-10-29T15:43:08", "EcoTransfer", "10-24-2016", "000006728401",
                "2014-11-26T09:27:29", "EcoTransfer", "10-24-2016", "000006728401",
                //"2014-12-01T10:26:44", "EcoTransfer", "12-01-2014", "1234567098", //missing; bogus plate barcodes
                "2014-12-03T10:56:02", "EcoTransfer", "12-26-2014", "14I03A0027",
                "2014-12-08T17:18:05", "EcoTransfer", "12-26-2014", "000006725901",
                "2014-12-11T15:07:16", "EcoTransfer", "10-24-2016", "000006728401",
                "2014-12-12T10:35:23", "EcoTransfer", "10-24-2016", "6728401",
                //"2014-12-19T18:29:10", "EcoTransfer", "10-24-2016", "000006728401", //missing; eco was done in source (next line)
                "2014-12-19T19:15:56", "EcoTransfer", "10-24-2016", "6728401",
                "2015-01-15T13:38:58", "EcoTransfer", "10-24-2016", "000006759501",
                //"2015-01-15T15:17:44", "EcoTransfer", "01-16-2015", "9865983265", //missing; bogus plate barcodes
                "2015-01-23T11:53:35", "EcoTransfer", "01-24-2015", "000006759501",
                "2015-01-23T14:26:51", "EcoTransfer", "01-24-2015", "000006759501",
                "2015-01-26T08:34:14", "EcoTransfer", "10-26-2016", "000006759501",
                "2015-01-30T11:10:39", "EcoTransfer", "07-20-2015", "14G31A0012",
                "2015-02-05T10:02:45", "EcoTransfer", "10-24-2016", "6728401",
                "2015-02-11T08:50:59", "EcoTransfer", "10-23-2015", "000006759501",
                "2015-02-13T12:01:12", "EcoTransfer", "10-23-2015", "000006759501",
                "2015-02-17T10:24:34", "EcoTransfer", "10-23-2015", "000006759501",
                "2015-02-17T12:18:33", "EcoTransfer", "10-23-2015", "6759501",
                "2015-02-18T07:52:52", "EcoTransfer", "10-23-2015", "000006759501",
                "2015-02-19T10:10:27", "EcoTransfer", "11-19-2016", "000006759501",
                "2015-02-23T11:42:39", "EcoTransfer", "10-23-2015", "000006759501",
                //"2015-02-26T14:11:58", "EcoTransfer", "02-27-2015", "12345",  //missing; bogus plate barcodes
                //"2015-02-26T17:28:00", "EcoTransfer", "02-27-2015", "12345",  //missing; bogus plate barcodes
                "2015-03-04T10:02:04", "EcoTransfer", "10-15-2015", "000006759501",
                "2015-03-04T13:14:49", "EcoTransfer", "08-13-2015", "000006759501",
                "2015-03-05T07:20:14", "EcoTransfer", "10-23-2015", "6759501",
                //"2015-03-09T10:16:55", "EcoTransfer", "10-23-2015", "000006759501", //missing; eco was done on source (next line)
                "2015-03-11T08:26:54", "EcoTransfer", "07-20-2015", "000006759501",
                //"2015-03-13T08:59:34", "EcoTransfer", "03-14-2015", "789789789",  //missing; bogus plate barcodes
                "2015-03-18T09:44:28", "EcoTransfer", "10-23-2015", "000006759501",
                "2015-03-23T08:43:54", "EcoTransfer", "10-23-2015", "6759501",
                "2015-03-25T11:49:51", "EcoTransfer", "11-26-2015", "000006759501",
                "2015-03-26T08:07:52", "EcoTransfer", "11-26-2015", "0154840394",
                "2015-03-30T11:50:47", "EcoTransfer", "06-30-2015", "0154839741",
                "2015-04-01T09:47:34", "EcoTransfer", "09-24-2016", "000006728401",
                "2015-04-06T08:02:17", "EcoTransfer", "10-23-2015", "000006759501",
                "2015-04-09T15:10:52", "EcoTransfer", "10-24-2016", "00BATCH003",
                "2015-04-13T11:01:29", "EcoTransfer", "11-14-2015", "000006759501",
                "2015-04-13T13:37:43", "EcoTransfer", "10-23-2015", "000006759501",
                "2015-04-14T09:32:55", "EcoTransfer", "10-24-2016", "000006728401",
                "2015-04-15T10:58:39", "EcoTransfer", "06-30-2015", "154840366",
                "2015-04-15T13:18:30", "EcoTransfer", "06-30-2015", "154840366",
                "2015-04-16T13:39:38", "EcoTransfer", "10-24-2016", "000006728401",
                "2015-04-21T09:05:28", "EcoTransfer", "04-21-2015", "0154839691",
                "2015-04-28T16:01:47", "EcoTransfer", "10-24-2016", "000006728401",
                "2015-05-01T09:50:02", "EcoTransfer", "05-01-2015", "0154839666",
                "2015-05-05T11:14:14", "EcoTransfer", "10-23-2015", "000006759501",
                "2015-05-06T09:36:47", "EcoTransfer", "10-24-2016", "000006728401",
                "2015-05-11T09:49:46", "EcoTransfer", "10-24-2016", "6728401",
                "2015-05-13T16:10:55", "EcoTransfer", "05-14-2015", "000006759501",
                "2015-05-18T12:16:48", "EcoTransfer", "10-23-2015", "000006759501",
                "2015-05-20T08:58:16", "EcoTransfer", "10-24-2016", "000006728401",
                "2015-05-22T07:47:49", "EcoTransfer", "10-23-2015", "000006759501",
                "2015-05-26T09:58:09", "EcoTransfer", "11-26-2015", "000006759501",
                "2015-05-27T15:00:19", "EcoTransfer", "10-24-2016", "000006728401",
                "2015-06-01T11:09:19", "EcoTransfer", "11-30-2015", "000006759501",
                "2015-06-04T07:55:23", "EcoTransfer", "10-23-2015", "000006759501",
                "2015-06-05T08:54:49", "EcoTransfer", "10-24-2016", "Batch3",
                "2015-06-09T10:56:13", "EcoTransfer", "12-31-2015", "000006759501",
                "2015-06-11T14:56:33", "EcoTransfer", "10-24-2016", "6728401",
                "2015-06-15T08:44:56", "EcoTransfer", "10-24-2016", "6728401",
                "2015-06-15T11:04:35", "EcoTransfer", "10-24-2016", "6728401",
                "2015-06-18T09:53:59", "EcoTransfer", "12-31-2015", "0154840370",
                "2015-06-19T14:45:46", "EcoTransfer", "10-24-2016", "000006728401",
                "2015-06-25T16:20:15", "EcoTransfer", "10-24-2016", "0006728401",
                //"2015-06-30T12:32:10", "EcoTransfer", "06-30-2016", "000006808101", //missing; eco done on source in next line
                "2015-06-30T14:47:35", "EcoTransfer", "06-30-2016", "6808101",
                "2015-07-01T08:21:24", "EcoTransfer", "10-23-2015", "6759501",
                "2015-07-07T15:45:06", "EcoTransfer", "07-07-2015", "6809201",
                "2015-07-15T11:02:26", "EcoTransfer", "11-30-2015", "000006759501",
                "2015-07-15T13:27:01", "EcoTransfer", "07-01-2017", "000006809201",
                "2015-07-21T10:09:43", "EcoTransfer", "07-01-2017", "000006809201",
                //"2015-07-22T11:09:38", "EcoTransfer", "07-01-2017", "000006809201", //missing; no eco done on source
                "2015-07-23T13:34:20", "EcoTransfer", "10-24-2016", "000006728401",
                "2015-07-30T14:02:47", "EcoTransfer", "10-24-2016", "000006728401",
                "2015-08-06T11:59:03", "EcoTransfer", "10-24-2016", "000006728401",
                "2015-08-06T13:45:18", "EcoTransfer", "10-24-2016", "6728401",
                "2015-08-11T10:38:36", "EcoTransfer", "07-01-2017", "000006809201",
                "2015-08-13T09:59:42", "EcoTransfer", "07-01-2017", "000006809201",
                "2015-08-13T15:32:13", "EcoTransfer", "10-24-2016", "000006728401",
                "2015-08-18T11:44:30", "EcoTransfer", "07-01-2017", "000006809201",
                "2015-08-18T14:29:47", "EcoTransfer", "07-01-2017", "6809201",
                "2015-08-19T07:34:27", "EcoTransfer", "07-01-2017", "000006809201",
                "2015-08-20T14:02:01", "EcoTransfer", "10-24-2016", "000006728401",
                "2015-08-25T10:29:17", "EcoTransfer", "07-01-2017", "6809201",
                "2015-08-25T12:59:30", "EcoTransfer", "07-01-2017", "6809201",
                "2015-08-27T15:22:06", "EcoTransfer", "10-24-2015", "000006728401",
                "2015-09-01T08:19:45", "EcoTransfer", "07-01-2017", "000006809201",
                "2015-09-03T12:37:39", "EcoTransfer", "10-23-2015", "6728401",
                "2015-09-09T10:53:49", "EcoTransfer", "07-01-2017", "000006809201",
                "2015-09-11T12:35:39", "EcoTransfer", "10-24-2016", "000006728401",
                "2015-09-17T14:23:33", "EcoTransfer", "07-01-2017", "000006809201",
                "2015-09-22T13:42:58", "EcoTransfer", "07-01-2017", "6809201",
                "2015-09-23T09:20:29", "EcoTransfer", "07-01-2017", "000006809201",
                "2015-09-30T15:20:07", "EcoTransfer", "10-24-2016", "6728401",
                "2015-10-08T12:46:55", "EcoTransfer", "10-24-2016", "000006728401",
                "2015-10-15T15:29:44", "EcoTransfer", "10-24-2016", "000006728401",
                "2015-10-20T13:13:31", "EcoTransfer", "10-20-2015", "000006809201",
                "2015-10-23T13:20:08", "EcoTransfer", "07-01-2017", "000006809201",
                "2015-10-28T14:50:53", "EcoTransfer", "10-24-2016", "000006728401",
                "2015-11-02T08:28:35", "EcoTransfer", "07-01-2017", "000006809201",
                "2015-11-06T12:09:44", "EcoTransfer", "07-01-2017", "000006809201",
                "2015-11-09T10:42:15", "EcoTransfer", "07-01-2017", "6809201",
                "2015-11-12T09:39:03", "EcoTransfer", "07-01-2017", "000006809201",
        }, "QpcrStandards");

        genericReagentDao.persist(new FixupCommentary(
                "GPLIM-3850 add missing qPCR Standards reagents or fix incorrect ones using the Bravo log records."));
        genericReagentDao.flush();
        utx.commit();
    }


    @Test(enabled = false)
    public void gplim3849addMissingEEW() throws Exception {
        userBean.loginOSUser();
        utx.begin();

        addReagentsToEvents(new String[]{
                "2015-08-26T13:03:32", "Ice1stCapture", "05-04-2016", "050415SS",
                "2015-08-27T08:45:32", "Ice2ndCapture", "05-04-2016", "050415SS",
                "2015-09-02T10:42:12", "Ice1stCapture", "05-04-2016", "050415SS",
                "2015-09-03T07:03:55", "Ice2ndCapture", "05-04-2016", "050415SS",
                "2015-09-10T11:48:42", "Ice1stCapture", "05-04-2016", "050415SS",
                "2015-09-11T07:04:32", "Ice2ndCapture", "05-04-2016", "050415SS",
                "2015-09-16T11:15:58", "Ice1stCapture", "05-04-2016", "050415SS",
                "2015-09-17T08:17:10", "Ice2ndCapture", "05-04-2016", "050415SS",
                "2015-09-21T11:34:32", "Ice1stCapture", "05-04-2016", "050415SS",
                "2015-09-22T07:48:47", "Ice2ndCapture", "05-04-2016", "050415SS",
                "2015-09-29T11:30:57", "Ice1stCapture", "05-04-2016", "0540415SS",
                "2015-09-30T07:02:48", "Ice2ndCapture", "05-04-2016", "0540415SS",
                "2015-10-07T10:42:37", "Ice1stCapture", "05-04-2016", "050415SS",
                "2015-10-08T07:00:51", "Ice2ndCapture", "05-04-2016", "050415SS",
                "2015-10-14T13:34:11", "Ice1stCapture", "05-04-2016", "050415SS",
                "2015-10-15T09:17:15", "Ice2ndCapture", "05-04-2016", "05041SS",
                "2015-10-19T12:49:58", "Ice1stCapture", "05-04-2016", "050415SS",
                "2015-10-20T07:58:54", "Ice2ndCapture", "05-04-2016", "050415SS",
                "2015-10-22T11:09:47", "Ice1stCapture", "05-04-2016", "050415SS",
                "2015-10-23T06:55:32", "Ice2ndCapture", "05-04-2016", "050415SS",
                "2015-10-27T13:51:34", "Ice1stCapture", "05-04-2016", "050415SS",
                "2015-10-28T09:38:05", "Ice2ndCapture", "05-04-2016", "050415SS",
                //"2015-11-05T09:53:50", "Ice1stCapture", "05-04-2016", "050415SS", //missing; bogus plate barcodes
                "2015-11-05T12:58:40", "Ice1stCapture", "05-04-2016", "050415SS",
                "2015-11-06T06:31:32", "Ice2ndCapture", "05-04-2016", "050415SS",
                "2015-11-09T12:51:28", "Ice1stCapture", "05-04-2016", "050415SS",
                "2015-11-10T08:47:50", "Ice2ndCapture", "05-04-2016", "050415SS",
                "2015-11-12T10:15:37", "Ice1stCapture", "05-04-2016", "050415SS",
        }, "EEW");

        genericReagentDao.persist(new FixupCommentary(
                "GPLIM-3849 add missing EEW reagents using the Bravo log records."));
        genericReagentDao.flush();
        utx.commit();
    }

@Test(enabled = false)
    public void gplim4063EmergeBaitSpecifyColumn() throws Exception {
        userBean.loginOSUser();
        long firstHybBaitEventId = 1220422L;
        long secondHybBaitEventId = 1220756L;

        LabEvent firstBaitPickEvent = genericReagentDao.findById(LabEvent.class, firstHybBaitEventId);
        Assert.assertNotNull(firstBaitPickEvent);

        LabEvent secondBaitPickEvent = genericReagentDao.findById(LabEvent.class, secondHybBaitEventId);
        Assert.assertNotNull(secondBaitPickEvent);

        List<LabEvent> labEvents = Arrays.asList(firstBaitPickEvent, secondBaitPickEvent);

        //Set well metadata for non-emerge bait reagent to every other well except A1
        for (LabEvent labEvent : labEvents) {
            Set<LabEventReagent> reagents = labEvent.getLabEventReagents();
            Assert.assertTrue(reagents.size() == 1);
            LabEventReagent labEventReagent = reagents.iterator().next();
            Assert.assertEquals(labEventReagent.getReagent().getLot(), "16A07A0006");
            Assert.assertTrue(labEventReagent.getMetadata().isEmpty());
            Set<Metadata> nonEmergeMetadata = new HashSet<>();
            for (String well : Arrays.asList("A3", "A5", "A7", "A9", "A11")) {
                nonEmergeMetadata.add(new Metadata(Metadata.Key.BAIT_WELL, well));
                System.out.println("Created Metadata key " + Metadata.Key.BAIT_WELL.getDisplayName() +
                                   " value " + well + " for event " + labEvent.getLabEventId());
            }
            labEventReagent.setMetadata(nonEmergeMetadata);
        }

        //Create new emerge bait reagent
        final SimpleDateFormat expDateFormat = new SimpleDateFormat("MM-dd-yyyy");
        Date expiration = expDateFormat.parse("11-01-2016");
        String lot = "20024869";
        String type = "Rapid Capture Kit Box 4 (Bait)";

        Assert.assertNull(genericReagentDao.findByReagentNameLotExpiration(type, lot, expiration));
        Reagent emergeReagent = new GenericReagent(type, lot, expiration);
        System.out.println("Created reagent " + type + " lot " + lot + " expiration " + expiration);

        for (LabEvent labEvent: labEvents) {
            Set<Metadata> metadataSet = new HashSet<>();
            Metadata metadata = new Metadata(Metadata.Key.BAIT_WELL, "A1");
            metadataSet.add(metadata);
            labEvent.addReagentMetadata(emergeReagent, metadataSet);
            System.out.println("Reagent " + emergeReagent.getReagentId() +
                               " added to event " + labEvent.getLabEventId());
        }

        genericReagentDao.persist(new FixupCommentary("GPLIM-4063 fixup create emerge bait reagent and specify columns"));
        genericReagentDao.flush();
    }

    @Test(enabled = false)
    public void gplim4120fixDate() throws Exception {
        userBean.loginOSUser();
        // Replaces wrong expiration for P5 Indexed Adapter Plate reagent.
        // Reagent was newly created for the event and not used elsewhere so the
        // code just needs to fix the Reagent entity.
        Reagent reagent = genericReagentDao.findById(Reagent.class, 1195956L);
        Assert.assertNotNull(reagent);
        String lot = "000001805823";
        Assert.assertEquals(lot, reagent.getLot());
        Date expiration = new GregorianCalendar(2016, Calendar.MAY, 21).getTime();
        System.out.println("Changing expiration date on reagent id " + reagent.getReagentId() + " to " + expiration);
        reagent.setExpiration(expiration);
        genericReagentDao.persist(new FixupCommentary("GPLIM-4120 fixup incorrect P5 adapter expiration"));
        genericReagentDao.flush();
    }

    @Test(enabled = false)
    public void fixupGplim4130() throws Exception {
        userBean.loginOSUser();
        utx.begin();
        // Replaces reagent with one with the correct expiration. Preserves the original being used on other lab events.
        LabEvent labEvent = labEventDao.findById(LabEvent.class, 1308892L);
        Reagent reagent = genericReagentDao.findById(Reagent.class, 1086959L);

        // Finds and removes the labEventReagent
        LabEventReagent foundLabEventReagent = null;
        for (LabEventReagent labEventReagent : labEvent.getLabEventReagents()) {
            if (labEventReagent.getReagent().equals(reagent)) {
                foundLabEventReagent = labEventReagent;
            }
        }
        Assert.assertNotNull(foundLabEventReagent);
        labEvent.getLabEventReagents().remove(foundLabEventReagent);
        genericReagentDao.remove(foundLabEventReagent);

        Reagent newReagent = new GenericReagent(reagent.getName(), reagent.getLot(),
                new GregorianCalendar(2018, Calendar.MAY, 13).getTime());

        System.out.println("Replacing " + reagent.getName() + " on event " + labEvent.getLabEventId() +
                           " with one expiring " + newReagent.getExpiration().toString());
        labEvent.addReagent(newReagent);

        genericReagentDao.persist(new FixupCommentary("GPLIM-4130 change reagent due to wrong expiration date."));
        genericReagentDao.flush();
        utx.commit();
    }

    @Test(enabled = false)
    public void fixupGplim4130a() throws Exception {
        userBean.loginOSUser();
        utx.begin();
        // Updates date from 5/13/18 to 4/13/18.
        Reagent reagent = genericReagentDao.findById(Reagent.class, 1199001L);
        Assert.assertNotNull(reagent);
        reagent.setExpiration(new GregorianCalendar(2018, Calendar.APRIL, 13).getTime());
        System.out.println("Updating reagent " + reagent.getReagentId() +
                           " expiration to " + reagent.getExpiration().toString());

        genericReagentDao.persist(new FixupCommentary("GPLIM-4130 fix expiration date."));
        genericReagentDao.flush();
        utx.commit();
    }

    @Test(enabled = false)
    public void gplim4252EmergeBaitLotFixup() throws Exception {
        userBean.loginOSUser();
        utx.begin();

        Reagent undesired = genericReagentDao.findByReagentNameLotExpiration("Rapid Capture Kit Box 4 (Bait)",
                "16D29A0013", new GregorianCalendar(2018, Calendar.DECEMBER, 3).getTime());
        Assert.assertNotNull(undesired);

        Reagent desired = genericReagentDao.findByReagentNameLotExpiration("Rapid Capture Kit Box 4 (Bait)",
                "4520044903", new GregorianCalendar(2017, Calendar.JUNE, 29).getTime());
        Assert.assertNotNull(desired);

        LabEvent labEvent = labEventDao.findById(LabEvent.class, 1434789L);
        Assert.assertEquals(labEvent.getLabEventType(), LabEventType.ICE_1S_TBAIT_PICK);
        Assert.assertNotNull(labEvent.removeLabEventReagent(undesired));
        labEvent.addReagent(desired);

        genericReagentDao.persist(new FixupCommentary("GPLIM-4252 change reagent used on Ice 1st Bait Pick."));
        genericReagentDao.flush();
        utx.commit();
    }

    @Test(enabled = false)
    public void support2453CrspSeqReagentFixup() throws Exception {
        userBean.loginOSUser();
        utx.begin();

        Reagent box1Incorrect = genericReagentDao.findByReagentNameLotExpiration("TruSeq Rapid SBS Kit",
                "16J13A0023", new GregorianCalendar(2017, Calendar.AUGUST, 11).getTime());
        Assert.assertNotNull(box1Incorrect);

        Reagent box2Incorrect = genericReagentDao.findByReagentNameLotExpiration("TruSeq Rapid SBS Kit Box 2 of 2",
                "16J13A0024", new GregorianCalendar(2017, Calendar.JULY, 31).getTime());
        Assert.assertNotNull(box2Incorrect);

        // Correct reagents already exist since used on other flowcells
        Reagent box1Actual = genericReagentDao.findByReagentNameLotExpiration("TruSeq Rapid SBS Kit",
                "16J13A0024", new GregorianCalendar(2017, Calendar.JULY, 31).getTime());
        Assert.assertNotNull(box1Actual);

        Reagent box2Actual = genericReagentDao.findByReagentNameLotExpiration("TruSeq Rapid SBS Kit Box 2 of 2",
                "16J13A0023", new GregorianCalendar(2017, Calendar.AUGUST, 11).getTime());
        Assert.assertNotNull(box2Actual);

        LabEvent labEvent = labEventDao.findById(LabEvent.class, 1811530L);
        Assert.assertEquals(labEvent.getLabEventType(), LabEventType.DILUTION_TO_FLOWCELL_TRANSFER);
        Assert.assertNotNull(labEvent.removeLabEventReagent(box1Incorrect));
        Assert.assertNotNull(labEvent.removeLabEventReagent(box2Incorrect));
        labEvent.addReagent(box1Actual);
        labEvent.addReagent(box2Actual);

        genericReagentDao.persist(new FixupCommentary("SUPPORT-2453 change to correct lots."));
        genericReagentDao.flush();
        utx.commit();
    }

    @Test(enabled = false)
    public void support2496FixupWrongSeqReagents() throws Exception {
        userBean.loginOSUser();
        utx.begin();

        Reagent peKitIncorrect = genericReagentDao.findByReagentNameLotExpiration("TruSeq Rapid PE Cluster Kit",
                "RGT8269110", new GregorianCalendar(2017, Calendar.JULY, 11).getTime());
        Assert.assertNotNull(peKitIncorrect);

        Reagent peKitActual = genericReagentDao.findByReagentNameLotExpiration("TruSeq Rapid PE Cluster Kit",
                "16J13A0022", new GregorianCalendar(2017, Calendar.JULY, 11).getTime());
        Assert.assertNotNull(peKitActual);

        Reagent sbsKit1Incorrect = genericReagentDao.findByReagentNameLotExpiration("TruSeq Rapid SBS Kit",
                "RGT8133446", new GregorianCalendar(2017, Calendar.JULY, 31).getTime());
        Assert.assertNotNull(sbsKit1Incorrect);

        Reagent sbsKit1Actual = genericReagentDao.findByReagentNameLotExpiration("TruSeq Rapid SBS Kit",
                "16J13A0024", new GregorianCalendar(2017, Calendar.JULY, 31).getTime());
        Assert.assertNotNull(sbsKit1Actual);

        Reagent sbsKit2Incorrect = genericReagentDao.findByReagentNameLotExpiration("TruSeq Rapid SBS Kit Box 2 of 2",
                "RGT8229077", new GregorianCalendar(2017, Calendar.AUGUST, 11).getTime());
        Assert.assertNotNull(sbsKit2Incorrect);

        Reagent sbsKit2Actual = genericReagentDao.findByReagentNameLotExpiration("TruSeq Rapid SBS Kit Box 2 of 2",
                "16J13A0023", new GregorianCalendar(2017, Calendar.AUGUST, 11).getTime());
        Assert.assertNotNull(sbsKit2Actual);


        List<Long> labEventIds = Arrays.asList(1831151L, 1831152L);
        for (Long labEventId: labEventIds) {
            LabEvent labEvent = labEventDao.findById(LabEvent.class, labEventId);
            Assert.assertEquals(labEvent.getLabEventType(), LabEventType.DILUTION_TO_FLOWCELL_TRANSFER);
            Assert.assertNotNull(labEvent.removeLabEventReagent(peKitIncorrect));
            Assert.assertNotNull(labEvent.removeLabEventReagent(sbsKit1Incorrect));
            Assert.assertNotNull(labEvent.removeLabEventReagent(sbsKit2Incorrect));
            labEvent.addReagent(peKitActual);
            labEvent.addReagent(sbsKit1Actual);
            labEvent.addReagent(sbsKit2Actual);
        }

        genericReagentDao.persist(new FixupCommentary("SUPPORT-2496 change to correct lots."));
        genericReagentDao.flush();
        utx.commit();
    }

    @Test(enabled = false)
    public void fixupSupport3067() throws Exception {
        userBean.loginOSUser();
        utx.begin();
        // requires setting updatable = true on ReagentDesign.designName (On Product, bait is referred to by name,
        // not PK).
        ReagentDesign reagentDesign = reagentDesignDao.findByBusinessKey("Broad_Liquid_Biopsy_Panel_v1.1");
        reagentDesign.setDesignName("Broad_Liquid_Biopsy_Panel_v1_1");
        Product product = productDao.findByPartNumber("P-VAL-0018");
        Assert.assertEquals(product.getProductName(), "Deep Coverage Exome for Cell-Free Liquid Biopsy_Custom Panel");
        product.setReagentDesignKey(reagentDesign.getDesignName());

        reagentDesignDao.persist(new FixupCommentary("SUPPORT-3067 change design to correct spelling"));
        reagentDesignDao.flush();
        utx.commit();
    }

    /**
     * Backfill to update reagent with date of first use for GPLIM-4886
     */
    @Test(enabled = false)
    public void fixupBackfillFirstUse() throws Exception {
        userBean.loginOSUser();
        utx.begin();

        EntityManager em = genericReagentDao.getEntityManager();
        Query qry = em.createNativeQuery("select r.reagent_id, min(e.event_date)\n" +
                "  from lab_event e\n" +
                "     , lab_event_reagents er\n" +
                "     , reagent r\n" +
                " where er.lab_event = e.lab_event_id\n" +
                "   and er.reagents   = r.reagent_id\n" +
                "group by r.reagent_id");
        List<Object[]> rslts = qry.getResultList();
        Long reagentId;
        Date firstUsed;
        Reagent reagent;
        for( Object[] vals : rslts) {
            reagentId = ((BigDecimal) vals[0]).longValue();
            firstUsed = (Date) vals[1];
            reagent = genericReagentDao.findById( Reagent.class, reagentId );
            reagent.setFirstUse(firstUsed);

        }

        genericReagentDao.persist(new FixupCommentary("GPLIM-4886 Backfill reagent first used dates"));
        genericReagentDao.flush();
        utx.commit();
    }

    @Test(enabled = false)
    public void fixupSupport3618() throws Exception {
        userBean.loginOSUser();
        utx.begin();

        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");

        // Fix Linearization Mix 2
        Reagent linearizationReagent = genericReagentDao.findByReagentNameLotExpiration("Patterned Linearization Mix 2",
                "20177318", new GregorianCalendar(2018, Calendar.JULY, 1).getTime());
        Assert.assertNotNull(linearizationReagent);
        linearizationReagent.setLot("20208351");
        linearizationReagent.setExpiration(sdf.parse("10/17/2018"));
        System.out.println("Changing reagent: " + linearizationReagent.getReagentId() +
                           " lot to: " + linearizationReagent.getLot() + " and expiration to 10/17/2018");

        //Indexing Primer Mix
        Reagent indexingPrimerMix = genericReagentDao.findByReagentNameLotExpiration("Indexing Primer Mix",
                "20154795", new GregorianCalendar(2018, Calendar.APRIL, 24).getTime());
        Assert.assertNotNull(indexingPrimerMix);
        indexingPrimerMix.setLot("20185773");
        indexingPrimerMix.setExpiration(sdf.parse("08/02/2018"));
        System.out.println("Changing reagent: " + indexingPrimerMix.getReagentId() +
                           " lot to: " + indexingPrimerMix.getLot() + " and expiration to 08/02/2018");
        genericReagentDao.persist(new FixupCommentary("SUPPORT-3618 fixup reagent lot and expiration."));
        genericReagentDao.flush();
        utx.commit();
    }
    @Test(enabled = false)
    public void fixupSupport3794() throws Exception {
        userBean.loginOSUser();
        utx.begin();

        LabEvent labEvent = labEventDao.findById(LabEvent.class, 2522890L);
        Assert.assertEquals(labEvent.getLabEventType(), LabEventType.DILUTION_TO_FLOWCELL_TRANSFER);

        Reagent cleavageMM = genericReagentDao.findByReagentNameLotExpiration("Cleavage Reagent Master Mix",
                "20192115", new GregorianCalendar(2019, Calendar.AUGUST, 25).getTime());
        Assert.assertNotNull(cleavageMM);
        labEvent.addReagent(cleavageMM);

        Reagent cleavageWashMix = genericReagentDao.findByReagentNameLotExpiration("Cleavage Reagent Wash Mix",
                "20191163", new GregorianCalendar(2018, Calendar.AUGUST, 26).getTime());
        Assert.assertNotNull(cleavageWashMix);
        labEvent.addReagent(cleavageWashMix);

        Reagent fastAmp = genericReagentDao.findByReagentNameLotExpiration("Fast Amplifixation Mix",
                "20204400", new GregorianCalendar(2018, Calendar.OCTOBER, 11).getTime());
        Assert.assertNotNull(fastAmp);
        labEvent.addReagent(fastAmp);

        Reagent fastAmpPre = genericReagentDao.findByReagentNameLotExpiration("Fast Amplification Premix",
                "20210536", new GregorianCalendar(2019, Calendar.OCTOBER, 30).getTime());
        Assert.assertNotNull(fastAmpPre);
        labEvent.addReagent(fastAmpPre);

        Reagent fastDenature = genericReagentDao.findByReagentNameLotExpiration("Fast Denaturation Reagent",
                "20204370", new GregorianCalendar(2018, Calendar.OCTOBER, 8).getTime());
        Assert.assertNotNull(fastDenature);
        labEvent.addReagent(fastDenature);

        Reagent fastLine1 = genericReagentDao.findByReagentNameLotExpiration("Fast Linearization Mix 1",
                "20199036", new GregorianCalendar(2018, Calendar.SEPTEMBER, 19).getTime());
        Assert.assertNotNull(fastLine1);
        labEvent.addReagent(fastLine1);

        Reagent fastLine2 = genericReagentDao.findByReagentNameLotExpiration("Fast Linearization Mix 2",
                "20204405", new GregorianCalendar(2019, Calendar.FEBRUARY, 4).getTime());
        Assert.assertNotNull(fastLine2);
        labEvent.addReagent(fastLine2);

        Reagent fastResyn = genericReagentDao.findByReagentNameLotExpiration("Fast Resynthesis Mix",
                "20205214", new GregorianCalendar(2018, Calendar.OCTOBER, 15).getTime());
        Assert.assertNotNull(fastResyn);
        labEvent.addReagent(fastResyn);

        Reagent incorporationMix = genericReagentDao.findByReagentNameLotExpiration("Incorporation Master Mix",
                "20187116", new GregorianCalendar(2019, Calendar.JANUARY, 31).getTime());
        Assert.assertNotNull(incorporationMix);
        labEvent.addReagent(incorporationMix);

        Reagent i7Primer = genericReagentDao.findByReagentNameLotExpiration("Primer Mix Index i7",
                "20199029", new GregorianCalendar(2018, Calendar.OCTOBER, 21).getTime());
        Assert.assertNotNull(i7Primer);
        labEvent.addReagent(i7Primer);

        Reagent read1Primer = genericReagentDao.findByReagentNameLotExpiration("Primer Mix Read 1",
                "20199029", new GregorianCalendar(2019, Calendar.SEPTEMBER, 19).getTime());
        Assert.assertNotNull(read1Primer);
        labEvent.addReagent(read1Primer);

        Reagent read2Primer = genericReagentDao.findByReagentNameLotExpiration("Primer Mix Read 2",
                "20207978", new GregorianCalendar(2018, Calendar.OCTOBER, 20).getTime());
        Assert.assertNotNull(read2Primer);
        labEvent.addReagent(read2Primer);

        Reagent scanReagent = genericReagentDao.findByReagentNameLotExpiration("Scan Reagent",
                "20190535", new GregorianCalendar(2018, Calendar.AUGUST, 14).getTime());
        Assert.assertNotNull(scanReagent);
        labEvent.addReagent(scanReagent);

        Reagent clusterKit = genericReagentDao.findByReagentNameLotExpiration("TruSeq Rapid PE Cluster Kit",
                "18A12A0015", new GregorianCalendar(2018, Calendar.SEPTEMBER, 19).getTime());
        Assert.assertNotNull(clusterKit);
        labEvent.addReagent(clusterKit);

        Reagent sbsKit = genericReagentDao.findByReagentNameLotExpiration("TruSeq Rapid SBS Kit",
                "17K15A0031", new GregorianCalendar(2018, Calendar.AUGUST, 7).getTime());
        Assert.assertNotNull(sbsKit);
        labEvent.addReagent(sbsKit);

        Reagent sbsKit2 = genericReagentDao.findByReagentNameLotExpiration("TruSeq Rapid SBS Kit Box 2 of 2",
                "17K15A0029", new GregorianCalendar(2018, Calendar.AUGUST, 14).getTime());
        Assert.assertNotNull(sbsKit2);
        labEvent.addReagent(sbsKit2);

        Reagent usb1 = genericReagentDao.findByReagentNameLotExpiration("Universal Sequencing Buffer 1",
                "20186729", new GregorianCalendar(2018, Calendar.AUGUST, 7).getTime());
        Assert.assertNotNull(usb1);
        labEvent.addReagent(usb1);

        Reagent usb2 = genericReagentDao.findByReagentNameLotExpiration("Universal Sequencing Buffer 2",
                "20186729", new GregorianCalendar(2018, Calendar.AUGUST, 7).getTime());
        Assert.assertNotNull(usb2);
        labEvent.addReagent(usb2);

        Reagent flowcell = genericReagentDao.findByReagentNameLotExpiration("Flowcell Lot",
                "20216849", new GregorianCalendar(2018, Calendar.MAY, 27).getTime());
        Assert.assertNotNull(flowcell);
        labEvent.addReagent(flowcell);

        utx.commit();
    }

}
