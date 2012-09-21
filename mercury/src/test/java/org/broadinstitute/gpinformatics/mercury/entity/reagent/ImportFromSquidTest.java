package org.broadinstitute.gpinformatics.mercury.entity.reagent;

import com.sun.jersey.api.client.Client;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchBean;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.TubeBean;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.MolecularIndexingSchemeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.StaticPlateDAO;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.TwoDBarcodedTubeDAO;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.PlateWell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * A Test to import molecular indexes and LCSETs from Squid.  This prepares an empty database to accept messages.  This must
 * be in the same package as MolecularIndexingScheme, because it uses package visible methods on that class.
 * Use the following VM options: -Xmx1G -XX:MaxPermSize=128M.
 * As of August 2012, the test takes about 20 minutes to run.
 */
public class ImportFromSquidTest extends ContainerTest {

    @PersistenceContext(unitName = "squid_pu")
    private EntityManager entityManager;

    @Inject
    private MolecularIndexingSchemeDao molecularIndexingSchemeDao;

    @Inject
    private StaticPlateDAO staticPlateDAO;

    @Inject
    private TwoDBarcodedTubeDAO twoDBarcodedTubeDAO;

    /**
     * Import index schemes from Squid.
     */
    @Test(enabled = false, groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testImportIndexingSchemes() {
        Query nativeQuery = entityManager.createNativeQuery("SELECT " +
                "     mis.NAME, " +
                "     mip.position_hint, " +
                "     mi.SEQUENCE " +
                "FROM " +
                "     molecular_indexing_scheme mis " +
                "     INNER JOIN molecular_index_position mip " +
                "          ON   mip.scheme_id = mis.ID " +
                "     INNER JOIN molecular_index mi " +
                "          ON   mi.ID = mip.index_id " +
                "ORDER BY " +
                "     mis.NAME ");
        List<?> resultList = nativeQuery.getResultList();

        String previousSchemeName = "";
        MolecularIndexingScheme molecularIndexingScheme = null;
        SortedMap<MolecularIndexingScheme.IndexPosition, MolecularIndex> indexesAndPositions = null;
        for (Object o : resultList) {
            Object[] columns = (Object[]) o;
            String schemeName = (String) columns[0];
            String positionHint = (String) columns[1];
            String sequence = (String) columns[2];

            if(!schemeName.equals(previousSchemeName)) {
                previousSchemeName = schemeName;
                if(molecularIndexingScheme != null) {
                    molecularIndexingScheme.setIndexPositions(indexesAndPositions);
                    molecularIndexingSchemeDao.persist(molecularIndexingScheme);
                    // Until fix the mapping problem that causes repeated deletes and inserts on molecular_index_position,
                    // clear the session to avoid dirty checks (each call to persist is in its own transaction, so it
                    // flushes immediately)
                    molecularIndexingSchemeDao.clear();
                }
                molecularIndexingScheme = new MolecularIndexingScheme();
                molecularIndexingScheme.setName(schemeName);
                indexesAndPositions = new TreeMap<MolecularIndexingScheme.IndexPosition, MolecularIndex>();
            }
            indexesAndPositions.put(MolecularIndexingScheme.IndexPosition.valueOf(positionHint), new MolecularIndex(sequence));
        }
        molecularIndexingSchemeDao.persist(molecularIndexingScheme);
        molecularIndexingSchemeDao.clear();
    }

    /**
     * Import index plates from Squid.  This takes about 15 minutes.
     */
    @Test(enabled = false, groups = TestGroups.EXTERNAL_INTEGRATION, dependsOnMethods = {"testImportIndexingSchemes"})
    public void testImportIndexPlates() {
        Query nativeQuery = entityManager.createNativeQuery("SELECT " +
                "     p.barcode as plate_barcode, " +
                "     wd.NAME as well_name, " +
                "     mis.NAME as scheme_name " +
                "FROM " +
                "     plate_seq_sample_identifier pssi " +
                "     INNER JOIN plate p " +
                "          ON   p.plate_id = pssi.plate_id " +
                "     INNER JOIN well_description wd " +
                "          ON   wd.well_description_id = pssi.well_description_id " +
                "     INNER JOIN molecular_indexing_scheme mis " +
                "          ON   mis.ID = pssi.molecular_indexing_scheme_id " +
                "ORDER BY " +
                "     p.barcode, " +
                "     wd.NAME");
        List<?> resultList = nativeQuery.getResultList();
        String previousPlateBarcode = "";
        StaticPlate staticPlate = null;
        List<StaticPlate> plates = new ArrayList<StaticPlate>();
        Map<String, MolecularIndexingScheme> mapNameToScheme = new HashMap<String, MolecularIndexingScheme>();
        for (Object o : resultList) {
            Object[] columns = (Object[]) o;
            String plateBarcode = (String) columns[0];
            String wellName = (String) columns[1];
            String indexingSchemeName = (String) columns[2];
            if(!plateBarcode.equals(previousPlateBarcode)) {
                previousPlateBarcode = plateBarcode;
                if(staticPlate != null) {
                    plates.add(staticPlate);
                    // Persist periodically, rather than all at the end, to avoid out of memory errors
                    if(plates.size() >= 100) {
                        System.out.println("Persisting plates");
                        staticPlateDAO.persistAll(plates);
                        plates.clear();
                        mapNameToScheme.clear();
                        staticPlateDAO.clear();
                    }
                }
                staticPlate = new StaticPlate(plateBarcode, StaticPlate.PlateType.IndexedAdapterPlate96);
            }
            VesselPosition vesselPosition = VesselPosition.getByName(wellName);
            final PlateWell plateWell = new PlateWell(staticPlate, vesselPosition);
            MolecularIndexingScheme molecularIndexingScheme = mapNameToScheme.get(indexingSchemeName);
            if (molecularIndexingScheme == null) {
                System.out.println("Fetching scheme " + indexingSchemeName);
                molecularIndexingScheme = molecularIndexingSchemeDao.findByName(indexingSchemeName);
                if(molecularIndexingScheme == null) {
                    throw new RuntimeException("Failed to find scheme " + indexingSchemeName);
                }
                mapNameToScheme.put(indexingSchemeName, molecularIndexingScheme);
            }
            plateWell.addReagent(new MolecularIndexReagent(molecularIndexingScheme));

            staticPlate.getVesselContainer().addContainedVessel(plateWell, vesselPosition);
        }
        staticPlateDAO.persistAll(plates);
        staticPlateDAO.clear();
    }

    /**
     * To prepare for sending past production BettaLIMS messages into SequeL, this method creates LC Sets and associated
     * batches of tubes that are stored in Squid.
     */
    @Test(enabled = false, groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testCreateLcSets() {
        Query nativeQuery = entityManager.createNativeQuery("SELECT " +
                "     l.KEY, " +
                "     lwd.NAME, " +
                "     r.barcode AS tube_barcode, " +
                "     ls.barcode AS sample_barcode " +
                "FROM " +
                "     lcset l " +
                "     INNER JOIN lab_workflow lw " +
                "          ON   lw.lcset_id = l.lcset_id " +
                "     INNER JOIN lab_workflow_def lwd " +
                "          ON   lwd.lab_workflow_def_id = lw.lab_workflow_def_id " +
                "     INNER JOIN lab_workflow_receptacle lwr " +
                "          ON   lwr.lab_workflow_id = lw.lab_workflow_id " +
                "     INNER JOIN receptacle r " +
                "          ON   r.receptacle_id = lwr.receptacle_id " +
                "     INNER JOIN seq_content sc " +
                "          ON   sc.receptacle_id = r.receptacle_id " +
                "     INNER JOIN seq_content_descr_set scds " +
                "          ON   scds.seq_content_id = sc.seq_content_id " +
                "     INNER JOIN seq_content_descr scd " +
                "          ON   scd.seq_content_descr_id = scds.seq_content_descr_id " +
                "     INNER JOIN gssr_pool_descr gpd " +
                "          ON   gpd.seq_content_descr_id = scd.seq_content_descr_id " +
                "     INNER JOIN lc_sample ls " +
                "          ON   ls.lc_sample_id = gpd.lc_sample_id " +
                "     INNER JOIN well_map_entry wme " +
                "          ON   wme.receptacle_id = r.receptacle_id " +
                "     INNER JOIN well_description wd " +
                "          ON   wd.well_description_id = wme.well_description_id " +
                "     INNER JOIN well_map wm " +
                "          ON   wm.well_map_id = wme.well_map_id " +
                "     INNER JOIN plate p " +
                "          ON   p.plate_id = wm.rack_plate_id " +
                "     INNER JOIN plate_transfer_event pte " +
                "          ON   pte.source_well_map_id = wm.well_map_id " +
                "     INNER JOIN plate_event pe " +
                "          ON   pe.station_event_id = pte.station_event_id " +
                "     INNER JOIN station_event se " +
                "          ON   se.station_event_id = pe.station_event_id " +
                "     INNER JOIN station_event_type set1 " +
                "          ON   set1.station_event_type_id = se.station_event_type_id " +
                "ORDER BY " +
                "     l.KEY, " +
                "     wd.NAME ");
        List<?> resultList = nativeQuery.getResultList();
        String previousLcSet = "";
        LabBatchBean labBatch = null;
        List<TubeBean> tubeBeans = null;
        for (Object o : resultList) {
            Object[] columns = (Object[]) o;
            String lcSet = (String) columns[0];
            String workflowName = (String) columns[1];
            String tubeBarcode = (String) columns[2];
            String sampleBarcode = (String) columns[3];
            if(!lcSet.equals(previousLcSet)) {
                previousLcSet = lcSet;
                if(labBatch != null) {
                    System.out.println("About to persist batch " + labBatch.getBatchId());
                    String response = null;
                    try {
                        // Use a web service, rather than just calling persist on a DAO, because a constraint
                        // violation invalidates the EntityManager.  The web service gets a fresh EntityManager for
                        // each request.
                        response = Client.create().resource("http://localhost:8181/SequeL/rest/labbatch")
                                .type(MediaType.APPLICATION_XML_TYPE)
                                .accept(MediaType.APPLICATION_XML)
                                .entity(labBatch)
                                .post(String.class);
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                    System.out.println(response);
                }
                tubeBeans = new ArrayList<TubeBean>();
                labBatch = new LabBatchBean(lcSet, workflowName, tubeBeans);
            }
            tubeBeans.add(new TubeBean(tubeBarcode, sampleBarcode));
        }
    }

    /**
     * To prepare for sending past production BettaLIMS messages into SequeL, this method creates bait tubes.
     */
    @Test(enabled = false, groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testCreateBaits() {
        Query nativeQuery = entityManager.createNativeQuery("SELECT " +
                "    DISTINCT r.barcode " +
                "FROM " +
                "    recep_plate_transfer_event rpte " +
                "    INNER JOIN receptacle r " +
                "        ON   r.receptacle_id = rpte.receptacle_id ");
        List<?> resultList = nativeQuery.getResultList();
        for (Object o : resultList) {
            String barcode = (String) o;
            twoDBarcodedTubeDAO.persist(new TwoDBarcodedTube(barcode));
        }
        twoDBarcodedTubeDAO.clear();
    }
}
