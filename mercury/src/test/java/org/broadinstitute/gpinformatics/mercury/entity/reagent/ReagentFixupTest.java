package org.broadinstitute.gpinformatics.mercury.entity.reagent;

import org.apache.commons.collections4.CollectionUtils;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.ReagentDesignDao;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.Arrays;
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
}
