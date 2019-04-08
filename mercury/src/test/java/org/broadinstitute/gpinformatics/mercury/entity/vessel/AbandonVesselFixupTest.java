package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.transaction.UserTransaction;
import java.math.BigDecimal;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Fixup production AbandonVessel entities
 */
@Test(groups = TestGroups.FIXUP)
public class AbandonVesselFixupTest extends Arquillian {

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private BSPUserList bspUserList;

    @Inject
    private UserTransaction utx;

    /**
     * Assign creation users to abandon vessels
     */
    @Test( enabled = false )
    public void fixupGplim5218() throws Exception {

        int count = 0;

        try {
            utx.begin();

            EntityManager em = labVesselDao.getEntityManager();

            // 124116 as of 07/03/2018
            Query qry = em.createNativeQuery(
                    "SELECT R.USERNAME \n"
                    + "     , AUD.ABANDON_VESSEL_ID\n"
                    + "  FROM ABANDON_VESSEL_AUD AUD\n"
                    + "     , ABANDON_VESSEL AV\n"
                    + "     , REV_INFO R\n"
                    + " WHERE AUD.REVTYPE = 0\n"
                    + "   AND AUD.REV = R.REV_INFO_ID\n"
                    + "   AND AUD.ABANDON_VESSEL_ID = AV.ABANDON_VESSEL_ID "
                    + "   AND AV.ABANDONED_BY IS NULL ");

            List<Object[]> rslts = qry.getResultList();

            System.out.println( "Rows to update: " + rslts.size() );

            for (Object[] cols : rslts) {
                String userName = (String) cols[0];
                BspUser bspUser = bspUserList.getByUsername(userName);
                if (bspUser == null) {
                    continue;
                }
                Long userId = bspUser.getUserId();
                BigDecimal abandonVesselId = (BigDecimal) cols[1];
                AbandonVessel abandonVessel = labVesselDao
                        .findSingleSafely(AbandonVessel.class, AbandonVessel_.abandonVesselId,
                                abandonVesselId.longValue(), LockModeType.NONE);
                if (abandonVessel != null) {
                    abandonVessel.setAbandonedBy(userId);
                }

                count++;
                if( count % 5000 == 0 ) {
                    FixupCommentary fixupCommentary =
                            new FixupCommentary("GPLIM-5218 - Assign users from audit trail to AbandonVessel entities - batch " + ( count / 5000 ) );
                    em.persist(fixupCommentary);
                    em.flush();
                    utx.commit();
                    em.clear();
                    utx.begin();
                }

            }

            FixupCommentary fixupCommentary =
                    new FixupCommentary("GPLIM-5218 - Assign users from audit trail to AbandonVessel entities - batch " + ( count / 5000 + 1 ) );
            em.persist(fixupCommentary);
            em.flush();
            utx.commit();
        } catch ( Exception e ) {
            try {
                utx.rollback();
            } catch ( Exception ignored ) {

            }
            throw e;
        }

    }

}
