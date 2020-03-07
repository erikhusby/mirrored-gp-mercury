package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.BarcodedTubeDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;

import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.inject.Inject;
import javax.transaction.TransactionManager;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Not an Arquillian test - method returns a value so won't be considered, move this class from test package to src package and deploy
 * Produces lab vessels with delayed commits to verify that ETL doesn't skip over long running transactions (GPLIM-6687)
 * Edit barcodes as requires and call from bash shell (not Windows git bash, i/o is inconsistent):
 * echo "Starting..." > ./random.log
 * sleep 1
 * curl -k -L -X GET https://localhost:8443/Mercury/rest/test/createAndDelay/JMSAAE001/300 >> ./random.log &
 * sleep 1
 * curl -k -L -X GET https://localhost:8443/Mercury/rest/test/createAndDelay/JMSAAE002/400 >> ./random.log &
 * sleep 1
 * curl -k -L -X GET https://localhost:8443/Mercury/rest/test/createAndDelay/JMSAAE003/500 >> ./random.log &
 * sleep 1
 * curl -k -L -X GET https://localhost:8443/Mercury/rest/test/createAndDelay/JMSAAE004/595 >> ./random.log &
 * sleep 1
 * curl -k -L -X GET https://localhost:8443/Mercury/rest/test/createAndDelay/JMSAAE005/300 >> ./random.log &
 * sleep 1
 * curl -k -L -X GET https://localhost:8443/Mercury/rest/test/createAndDelay/JMSAAE006/400 >> ./random.log &
 * sleep 1
 * curl -k -L -X GET https://localhost:8443/Mercury/rest/test/createAndDelay/JMSAAE007/500 >> ./random.log &
 * sleep 1
 * curl -k -L -X GET https://localhost:8443/Mercury/rest/test/createAndDelay/JMSAAE008/595 >> ./random.log &
 * sleep 1
 * curl -k -L -X GET https://localhost:8443/Mercury/rest/test/createAndDelay/JMSAAE009/300 >> ./random.log &
 * sleep 1
 * curl -k -L -X GET https://localhost:8443/Mercury/rest/test/createAndDelay/JMSAAE010/400 >> ./random.log &
 * sleep 1
 * curl -k -L -X GET https://localhost:8443/Mercury/rest/test/createAndDelay/JMSAAE011/500 >> ./random.log &
 * sleep 1
 * curl -k -L -X GET https://localhost:8443/Mercury/rest/test/createAndDelay/JMSAAE012/595 >> ./random.log &
 */
@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
@Path("test")
public class EtlDelayedTxTest {

    SimpleDateFormat format;

    public EtlDelayedTxTest() {
        format = new SimpleDateFormat("MM-dd HH:mm:ss.SSS");
    }

    @Inject
    private BarcodedTubeDao dao;

    @Inject
    private TransactionManager txnManager;

    /**
     * The workhorse - not an Arquillian test - method returns a value so won't be considered
     *
     * @param barcode  The tube barcode to create - provided by client, see class docs
     * @param sleepFor Seconds to delay transaction commit
     */
    @GET
    @Path("createAndDelay/{barcode}/{sleepFor}")
    public String createAndDelay(@PathParam("barcode") String barcode, @PathParam("sleepFor") int sleepFor) {
        Date start = new Date();
        try {
            txnManager.begin();
            BarcodedTube tube = new BarcodedTube(barcode, BarcodedTube.BarcodedTubeType.Petri);
            dao.persist(tube);
            dao.flush(); // Creates and timestamps audit entry to now
            // Transaction delay
            Thread.sleep(sleepFor * 1000);
            txnManager.commit();
            // LabVesselEtl will now see the entry
        } catch (Exception e) {
            e.printStackTrace();
            return "Exception creating barcode " + barcode + " and sleeping for "
                    + sleepFor + " seconds.  Error: " + e.getMessage() + "\n";
        }
        // This is logged in client
        return "Finished creating barcode " + barcode + " and sleeping for "
                + sleepFor + " seconds.  Start: " + format.format(start) + ", end: " + format.format(new Date()) + "\n";
    }

}
