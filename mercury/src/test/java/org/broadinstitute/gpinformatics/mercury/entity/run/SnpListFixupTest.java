package org.broadinstitute.gpinformatics.mercury.entity.run;

import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.SnpListDao;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Fixups for SNP Lists.
 */
@Test(groups = TestGroups.FIXUP)
public class SnpListFixupTest extends Arquillian {

    @Inject
    private SnpListDao snpListDao;

    @Inject
    private UserBean userBean;

    @Inject
    private UserTransaction utx;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @Test
    public void fixupGplim5122CreateSnpLists() {
        try {
            userBean.loginOSUser();
            String[] rsIds = {
                    "rs10037858",
                    "rs532905",
                    "rs2229857",
                    "rs6104310",
                    "rs9304229",
                    "rs2273827",
                    "rs2036902",
                    "rs6679393",
                    "rs2369754",
                    "rs1052053",
                    "rs6726639",
                    "rs2512276",
                    "rs6565604",
                    "rs6972020",
                    "rs13269287",
                    "rs10888734",
                    "rs6966770",
                    "rs2639",
                    "rs10186291",
                    "rs7598922",
                    "rs2709828",
                    "rs1131171",
                    "rs7664169",
                    "rs1437808",
                    "rs11917105",
                    "rs10876820",
                    "rs2910006",
                    "AMG_3b",
                    "rs8015958",
                    "rs3105047",
                    "rs5009801",
                    "rs9277471",
                    "rs3744877",
                    "rs1549314",
                    "rs9369842",
                    "rs390299",
                    "rs1734422",
                    "rs9466",
                    "rs4517902",
                    "rs6563098",
                    "rs965897",
                    "rs4580999",
                    "rs1998603",
                    "rs2840240",
                    "rs4146473",
                    "rs2070132",
                    "rs2587507",
                    "rs827113",
                    "rs213199",
                    "rs1028564",
                    "rs1634997",
                    "rs2241759",
                    "rs10435864",
                    "rs6140742",
                    "rs2302768",
                    "rs1051374",
                    "rs6714975",
                    "rs238148",
                    "rs1075622",
                    "rs6874609",
                    "rs2549797",
                    "rs4608",
                    "rs7017199",
                    "rs2590339",
                    "rs10943605",
                    "rs3824253",
                    "rs2640464",
                    "rs11204697",
                    "rs2649123",
                    "rs27141",
                    "rs1158448",
                    "rs7679911",
                    "rs1045738",
                    "rs12057639",
                    "rs7949313",
                    "rs3094698",
                    "rs13030",
                    "rs8500",
                    "rs3732083",
                    "rs1406957",
                    "rs935765",
                    "rs6484648",
                    "rs1584717",
                    "rs9374227",
                    "rs11652797",
                    "rs7152601",
                    "rs2452785",
                    "rs4572767",
                    "rs2737706",
                    "rs9809404",
                    "rs1595271",
                    "rs2049330",
                    "rs1077393",
                    "rs520806",
                    "rs2108978",
                    "rs753307"
            };
            SnpList snpList = new SnpList("FluidigmFPv5");
            for (String rsId : rsIds) {
                snpList.getSnps().add(new Snp(rsId));
            }
            utx.begin();
            snpListDao.persist(snpList);
            snpListDao.persist(new FixupCommentary("GPLIM-5122 setup SNP lists."));
            utx.commit();
        } catch (NotSupportedException | SystemException | RollbackException | HeuristicRollbackException |
                HeuristicMixedException e) {
            throw new RuntimeException(e);
        }
    }
}
