package org.broadinstitute.sequel.entity.vessel;

import org.broadinstitute.sequel.control.dao.vessel.TwoDBarcodedTubeDAO;
import org.broadinstitute.sequel.integration.ContainerTest;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.List;

import static org.broadinstitute.sequel.TestGroups.EXTERNAL_INTEGRATION;

/**
 * To prepare for sending past production BettaLIMS messages into SequeL, this class creates bait tubes.
 */
public class CreateBaitsTest extends ContainerTest {

    @PersistenceContext(unitName = "squid_pu")
    private EntityManager entityManager;

    @Inject
    private TwoDBarcodedTubeDAO twoDBarcodedTubeDAO;

    @Test(enabled = false, groups = EXTERNAL_INTEGRATION)
    public void createBaits() {
        Query nativeQuery = entityManager.createNativeQuery("SELECT " +
                "    DISTINCT r.barcode " +
                "FROM " +
                "    recep_plate_transfer_event rpte " +
                "    INNER JOIN receptacle r " +
                "        ON   r.receptacle_id = rpte.receptacle_id ");
        List resultList = nativeQuery.getResultList();
        for (Object o : resultList) {
            String barcode = (String) o;
            twoDBarcodedTubeDAO.persist(new TwoDBarcodedTube(barcode));
        }
    }
}
