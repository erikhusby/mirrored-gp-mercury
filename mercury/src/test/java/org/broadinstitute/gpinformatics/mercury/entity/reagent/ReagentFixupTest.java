package org.broadinstitute.gpinformatics.mercury.entity.reagent;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.GenericReagentDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.ReagentDesignDao;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
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

    @Test(enabled = true)
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
    public void fixupSupport565(){
        userBean.loginOSUser();
        GenericReagent genericReagent = genericReagentDao.findByReagentNameAndLot("HS buffer", "91Q33120101670146301");
        genericReagent.setLot("RG-8252");
        genericReagentDao.persist(new FixupCommentary("SUPPORT-565 reagent fixup"));
        genericReagentDao.flush();
    }
}
