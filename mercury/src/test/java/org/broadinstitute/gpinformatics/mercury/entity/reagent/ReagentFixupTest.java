package org.broadinstitute.gpinformatics.mercury.entity.reagent;

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

}