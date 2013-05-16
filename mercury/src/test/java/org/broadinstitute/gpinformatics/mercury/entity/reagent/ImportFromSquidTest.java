package org.broadinstitute.gpinformatics.mercury.entity.reagent;

import com.sun.jersey.api.client.Client;
import org.apache.commons.io.FileUtils;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.run.SolexaRunBean;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchBean;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.TubeBean;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.VesselMetricBean;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.VesselMetricRunBean;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.MolecularIndexingSchemeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.ReagentDesignDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.StaticPlateDAO;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.TwoDBarcodedTubeDAO;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.PlateWell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A Test to import molecular indexes and LCSETs from Squid.  This prepares an empty database to accept messages.  This must
 * be in the same package as MolecularIndexingScheme, because it uses package visible methods on that class.
 *
 * Use the following VM options in the test: -Xmx1G -XX:MaxPermSize=128M -Dorg.jboss.remoting-jmx.timeout=3000
 *
 * Use -DlastImportDate=2013-01-31 for incremental imports (note, this has to supplied on the command line for the
 * app server, not the test).
 *
 * As of January 2013, the test takes about 35 minutes to run.
 */
public class ImportFromSquidTest extends Arquillian {

    public static final String TEST_MERCURY_URL = "http://localhost:8080/Mercury";
    @PersistenceContext(unitName = "squid_pu")
    private EntityManager entityManager;

    @Inject
    private MolecularIndexingSchemeDao molecularIndexingSchemeDao;

    @Inject
    private StaticPlateDAO staticPlateDAO;

    @Inject
    private TwoDBarcodedTubeDAO twoDBarcodedTubeDAO;

    @Inject
    private ResearchProjectDao researchProjectDao;

    @Inject
    private ProductDao productDao;

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private ReagentDesignDao reagentDesignDao;

    public static final String XML_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    private SimpleDateFormat xmlDateFormat = new SimpleDateFormat(XML_DATE_FORMAT);

    public static final String IMPORT_DATE_FORMAT = "yyyy-MM-dd";
    private SimpleDateFormat importDateFormat = new SimpleDateFormat(IMPORT_DATE_FORMAT);

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(
                org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV, "dev");
    }

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

        List<MolecularIndexingScheme> mercurySchemes = molecularIndexingSchemeDao.findAll(MolecularIndexingScheme.class);
        Map<String, MolecularIndexingScheme> mapNameToScheme = new HashMap<String, MolecularIndexingScheme>();
        for (MolecularIndexingScheme mercuryScheme : mercurySchemes) {
            mapNameToScheme.put(mercuryScheme.getName(), mercuryScheme);
        }

        String previousSchemeName = "";
        MolecularIndexingScheme molecularIndexingScheme = null;
        Map<String, MolecularIndex> mapSequenceToIndex = new HashMap<String, MolecularIndex>();
        for (Object o : resultList) {
            Object[] columns = (Object[]) o;
            String schemeName = (String) columns[0];
            String positionHint = (String) columns[1];
            String sequence = (String) columns[2];

            if(!schemeName.equals(previousSchemeName)) {
                previousSchemeName = schemeName;
                if(molecularIndexingScheme != null && !mapNameToScheme.containsKey(schemeName)) {
                    molecularIndexingSchemeDao.persist(molecularIndexingScheme);
                }
                molecularIndexingScheme = new MolecularIndexingScheme();
                molecularIndexingScheme.setName(schemeName);
            }
            // reuse sequences across schemes, because sequence has a unique constraint
            MolecularIndex molecularIndex = mapSequenceToIndex.get(sequence);
            if(molecularIndex == null) {
                molecularIndex = new MolecularIndex(sequence);
                mapSequenceToIndex.put(sequence, molecularIndex);
            }
            assert molecularIndexingScheme != null;
            molecularIndexingScheme.addIndexPosition(MolecularIndexingScheme.IndexPosition.valueOf(positionHint), molecularIndex);
        }
        molecularIndexingSchemeDao.persist(molecularIndexingScheme);
    }

    /**
     * Import index plates from Squid.  This takes about 15 minutes.
     */
    @Test(enabled = false, groups = TestGroups.EXTERNAL_INTEGRATION, dependsOnMethods = {"testImportIndexingSchemes"})
    public void testImportIndexPlates() {
        Query nativeQuery = entityManager.createNativeQuery("SELECT " +
                "     p.barcode AS plate_barcode, " +
                "     wd.NAME AS well_name, " +
                "     mis.NAME AS scheme_name " +
                "FROM " +
                "     plate_seq_sample_identifier pssi " +
                "     INNER JOIN plate p " +
                "          ON   p.plate_id = pssi.plate_id " +
                "     INNER JOIN well_description wd " +
                "          ON   wd.well_description_id = pssi.well_description_id " +
                "     INNER JOIN molecular_indexing_scheme mis " +
                "          ON   mis.ID = pssi.molecular_indexing_scheme_id " +
                "WHERE " +
                "     p.created_on > :lastImportDate " +
                "ORDER BY " +
                "     p.barcode, " +
                "     wd.NAME");
        nativeQuery.setParameter("lastImportDate", getLastImportDate());
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
            PlateWell plateWell = new PlateWell(staticPlate, vesselPosition);
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

            assert staticPlate != null;
            staticPlate.getContainerRole().addContainedVessel(plateWell, vesselPosition);
        }
        staticPlateDAO.persistAll(plates);
        staticPlateDAO.clear();
    }

    private Date getLastImportDate() {
        Date lastImportDate;
        String lastImportDateString = System.getProperty("lastImportDate");
        if (lastImportDateString == null) {
            lastImportDate = new GregorianCalendar(2000, 5, 6).getTime();
        } else {
            try {
                lastImportDate = importDateFormat.parse(lastImportDateString);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
        return lastImportDate;
    }

    /**
     * To prepare for sending past production BettaLIMS messages into Mercury, this method creates LC Sets and associated
     * batches of tubes that are stored in Squid.
     */
    @Test(enabled = false, groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testCreateLcSets() {
//        Map<String, String> mapWorkflowToPartNum = new HashMap<String, String>();
//        mapWorkflowToPartNum.put("Custom Amplicon", "P-VAL-0002");
////        ("IGN WGS", "?")
////        ("Fluidigm Multi", "?")
//        mapWorkflowToPartNum.put("Whole Genome", "P-WG-0002");
//        mapWorkflowToPartNum.put("Exome Express", "P-EX-0002");
//        mapWorkflowToPartNum.put("Hybrid Selection", "P-EX-0001");
////        ("180SM", "?")

        // Positive and negative controls are not included in the Product Order, but are included in the LCSet
        Query nativeQuery = entityManager.createNativeQuery("SELECT " +
                "    DISTINCT  " +
                "    l.KEY AS lcsetKey, " +
                "    lwd.NAME AS workflowName, " +
                "    r.barcode AS tube_barcode, " +
                "    ls.barcode AS sample_barcode, " +
                "    ls.lsid AS sampleLsid, " +
                "    wd.name AS wellName, " +
                "    wrmd.product_order_name " +
                "FROM " +
                "    lcset l " +
                "    INNER JOIN lab_workflow lw " +
                "        ON   lw.lcset_id = l.lcset_id " +
                "    INNER JOIN lab_workflow_def lwd " +
                "        ON   lwd.lab_workflow_def_id = lw.lab_workflow_def_id " +
                "    INNER JOIN lab_workflow_receptacle lwr " +
                "        ON   lwr.lab_workflow_id = lw.lab_workflow_id " +
                "    INNER JOIN receptacle r " +
                "        ON   r.receptacle_id = lwr.receptacle_id " +
                "    INNER JOIN wr_material_recep_membership wmrm " +
                "        ON   wmrm.receptacle_id = r.receptacle_id " +
                "    INNER JOIN work_request_material wrm " +
                "        ON   wrm.work_request_material_id = wmrm.work_request_material_id " +
                "    INNER JOIN work_request_material_descr wrmd " +
                "        ON   wrmd.work_request_material_id = wrm.work_request_material_id " +
                "    INNER JOIN seq_content sc " +
                "        ON   sc.receptacle_id = r.receptacle_id " +
                "    INNER JOIN seq_content_descr_set scds " +
                "        ON   scds.seq_content_id = sc.seq_content_id " +
                "    INNER JOIN seq_content_descr scd " +
                "        ON   scd.seq_content_descr_id = scds.seq_content_descr_id " +
                "    INNER JOIN gssr_pool_descr gpd " +
                "        ON   gpd.seq_content_descr_id = scd.seq_content_descr_id " +
                "    INNER JOIN lc_sample ls " +
                "        ON   ls.lc_sample_id = gpd.lc_sample_id " +
                "    INNER JOIN well_map_entry wme " +
                "        ON   wme.receptacle_id = r.receptacle_id " +
                "    INNER JOIN well_description wd " +
                "        ON   wd.well_description_id = wme.well_description_id " +
                "    INNER JOIN well_map wm " +
                "        ON   wm.well_map_id = wme.well_map_id " +
                "    INNER JOIN plate p " +
                "        ON   p.plate_id = wm.rack_plate_id " +
                "    INNER JOIN plate_transfer_event pte " +
                "        ON   pte.source_well_map_id = wm.well_map_id " +
                "    INNER JOIN plate_event pe " +
                "        ON   pe.station_event_id = pte.station_event_id " +
                "    INNER JOIN station_event se " +
                "        ON   se.station_event_id = pe.station_event_id " +
                "    INNER JOIN station_event_type set1 " +
                "        ON   set1.station_event_type_id = se.station_event_type_id " +
//                "WHERE " +
//                "    wrmd.product_order_name IS NOT NULL " +
//                "    AND lwd.name = 'Exome Express' " +
                "ORDER BY " +
                "    l.KEY, " +
                "    wd.NAME ");
        List<?> resultList = nativeQuery.getResultList();
        String previousLcSet = "";
        LabBatchBean labBatch = null;
        List<TubeBean> tubeBeans = null;

//        ResearchProject researchProject = new ResearchProject(1701L, "Import from Squid", "Import from Squid", false);
//        String jiraTicketKey = "RP-ImportFromSquid";
//        researchProject.setJiraTicketKey(jiraTicketKey);
//        researchProjectDao.persist(researchProject);
//        ArrayList<ProductOrderSample> productOrderSamples = null;

        for (Object o : resultList) {
            Object[] columns = (Object[]) o;
            String lcSet = (String) columns[0];
            String workflowName = (String) columns[1];
            String tubeBarcode = (String) columns[2];
//            String sampleBarcode = (String) columns[3];
            String sampleLsid = (String) columns[4];
//            String wellName = (String) columns[5];
            String productOrder = (String) columns[6];
            if(!lcSet.equals(previousLcSet)) {
                previousLcSet = lcSet;
//                String partNumber = mapWorkflowToPartNum.get(workflowName);
                if(labBatch != null) {
                    System.out.println("About to persist batch " + labBatch.getBatchId());
                    String response = null;
                    try {
                        // Use a web service, rather than just calling persist on a DAO, because a constraint
                        // violation invalidates the EntityManager.  The web service gets a fresh EntityManager for
                        // each request.
                        response = Client.create().resource(TEST_MERCURY_URL + "/rest/labbatch")
                                .type(MediaType.APPLICATION_XML_TYPE)
                                .accept(MediaType.APPLICATION_XML)
                                .entity(labBatch)
                                .post(String.class);
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                    System.out.println(response);
//                    if (partNumber != null) {
//                        Product product = productDao.findByBusinessKey(partNumber);
//                        researchProject = researchProjectDao.findByBusinessKey(jiraTicketKey);
//                        ProductOrder productOrder = new ProductOrder(1701L, lcSet, productOrderSamples, "BSP-123", product, researchProject);
//                        productOrder.setJiraTicketKey(lcSet);
//                        productOrder.setOrderStatus(ProductOrder.OrderStatus.Submitted);
//                        productOrderDao.persist(productOrder);
//                        productOrderDao.clear();
//                    }
                }
                tubeBeans = new ArrayList<TubeBean>();
                labBatch = new LabBatchBean(lcSet, workflowName, tubeBeans);
//                productOrderSamples = new ArrayList<ProductOrderSample>();
            }
            assert tubeBeans != null;
            tubeBeans.add(new TubeBean(tubeBarcode, "SM-" + sampleLsid.substring(sampleLsid.lastIndexOf(':') + 1)));
//            productOrderSamples.add(new ProductOrderSample(sampleBarcode));
        }
    }

    /**
     * To prepare for sending past production BettaLIMS messages into Mercury, this method creates bait tubes.
     */
    @Test(enabled = false, groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testCreateBaits() {
        // todo jmt for a bait to show up in this query, it must have been transferred to a plate at least once.
        // Without this restriction, the number of tubes goes from ~40 to ~70,000
        Query nativeQuery = entityManager.createNativeQuery("SELECT DISTINCT " +
                "    r.barcode, " +
                "    hbd.design_name " +
                "FROM " +
                "    recep_plate_transfer_event rpte " +
                "    INNER JOIN receptacle r " +
                "        ON   r.receptacle_id = rpte.receptacle_id " +
                "    INNER JOIN seq_content sc " +
                "        ON   sc.receptacle_id = r.receptacle_id " +
                "    INNER JOIN seq_content_type sct " +
                "        ON   sct.seq_content_type_id = sc.seq_content_type_id " +
                "    INNER JOIN seq_content_descr_set scds " +
                "        ON   scds.seq_content_id = sc.seq_content_id " +
                "    INNER JOIN seq_content_descr scd " +
                "        ON   scd.seq_content_descr_id = scds.seq_content_descr_id " +
                "    INNER JOIN next_generation_library_descr ngld " +
                "        ON   ngld.library_descr_id = scd.seq_content_descr_id " +
                "    INNER JOIN lc_sample ls " +
                "        ON   ls.lc_sample_id = ngld.sample_id " +
                "    INNER JOIN lc_sample_data_set lsds " +
                "        ON   lsds.lc_sample_id = ls.lc_sample_id " +
                "    INNER JOIN lc_sample_data lsd " +
                "        ON   lsd.lc_sample_data_id = lsds.lc_sample_data_id " +
                "    INNER JOIN lc_sample_data_type lsdt " +
                "        ON   lsdt.lc_sample_data_type_id = lsd.lc_sample_data_type_id " +
                "    INNER JOIN lc_sample_pool_oligo_data lspod " +
                "        ON   lspod.lc_sample_data_id = lsd.lc_sample_data_id " +
                "    INNER JOIN hybsel_bait_design hbd " +
                "        ON   hbd.hybsel_bait_design_id = lspod.hybsel_bait_design_id " +
                "ORDER BY " +
                "        hbd.design_name ");
        List<?> resultList = nativeQuery.getResultList();

        Map<String, ReagentDesign> mapNameToReagentDesign = getMapNameToMercuryReagentDesign();

        String previousDesignName = "";
        ReagentDesign reagentDesign = null;
        for (Object o : resultList) {
            Object[] columns = (Object[]) o;
            String barcode = (String) columns[0];
            String designName = (String) columns[1];
            if(!previousDesignName.equals(designName)) {
                reagentDesign = new ReagentDesign(designName, ReagentDesign.ReagentType.BAIT);
                previousDesignName = designName;
            }
            TwoDBarcodedTube twoDBarcodedTube = new TwoDBarcodedTube(barcode);
            twoDBarcodedTube.addReagent(new DesignedReagent(reagentDesign));
            if (!mapNameToReagentDesign.containsKey(designName)) {
                twoDBarcodedTubeDAO.persist(twoDBarcodedTube);
            }
        }
        twoDBarcodedTubeDAO.clear();
    }

    /**
     * Get map of existing Mercury reagent designs
     * @return map from name of reagent design to entity
     */
    private Map<String, ReagentDesign> getMapNameToMercuryReagentDesign() {
        List<ReagentDesign> mercuryReagentDesigns = reagentDesignDao.findAll(ReagentDesign.class);
        Map<String, ReagentDesign> mapNameToReagentDesign = new HashMap<String, ReagentDesign>();
        for (ReagentDesign mercuryReagentDesign : mercuryReagentDesigns) {
            mapNameToReagentDesign.put(mercuryReagentDesign.getBusinessKey(), mercuryReagentDesign);
        }
        return mapNameToReagentDesign;
    }

    /**
     * To prepare for sending past production BettaLIMS messages into Mercury, this method creates Custom Amplicon Tubes.
     */
    @Test(enabled = false, groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testCreateCats() {
        // todo jmt for a CAT to show up in this query, it must have been transferred to a plate at least once.
        // Without this restriction, the number of tubes rises from 122 to ~14,000
        Query nativeQuery = entityManager.createNativeQuery("SELECT " +
                "     DISTINCT  " +
                "     cas.design_name, " +
                "     cas.vendor_design_name, " +
                "     r.barcode " +
                "FROM " +
                "     custom_amplicon_set cas " +
                "     INNER JOIN ngld_custom_amplicon_set ncas " +
                "          ON   ncas.custom_amplicon_set_id = cas.custom_amplicon_set_id " +
                "     INNER JOIN next_generation_library_descr ngld " +
                "          ON   ngld.library_descr_id = ncas.library_descr_id " +
                "     INNER JOIN seq_content_descr scd " +
                "          ON   scd.seq_content_descr_id = ngld.library_descr_id " +
                "     INNER JOIN seq_content_descr_set scds " +
                "          ON   scds.seq_content_descr_id = scd.seq_content_descr_id " +
                "     INNER JOIN seq_content sc " +
                "          ON   sc.seq_content_id = scds.seq_content_id " +
                "     INNER JOIN receptacle r " +
                "          ON   r.receptacle_id = sc.receptacle_id " +
                "     INNER JOIN recep_plate_transfer_event rpte " +
                "          ON   rpte.receptacle_id = r.receptacle_id " +
                "     ORDER BY " +
                "          cas.design_name ");
        List<?> resultList = nativeQuery.getResultList();

        Map<String, ReagentDesign> mapNameToReagentDesign = getMapNameToMercuryReagentDesign();

        String previousDesignName = "";
        ReagentDesign reagentDesign = null;
        for (Object o : resultList) {
            Object[] columns = (Object[]) o;
            String designName = (String) columns[0];
            String vendorDesignName = (String) columns[1];
            String barcode = (String) columns[2];
            if(!previousDesignName.equals(designName)) {
                reagentDesign = new ReagentDesign(designName, ReagentDesign.ReagentType.CAT);
                reagentDesign.setManufacturersName(vendorDesignName);
                previousDesignName = designName;
            }
            TwoDBarcodedTube twoDBarcodedTube = new TwoDBarcodedTube(barcode);
            twoDBarcodedTube.addReagent(new DesignedReagent(reagentDesign));
            if (!mapNameToReagentDesign.containsKey(designName)) {
                twoDBarcodedTubeDAO.persist(twoDBarcodedTube);
            }
        }
        twoDBarcodedTubeDAO.clear();
    }

    /**
     * Runs a native query against Squid to get the station events associated with a hard-coded list of LCSETs (derived
     * from a query that returns LCSETs that have Product Order associations).  For each station event, attempts to
     * find on the file system the corresponding message file.  This list of message files can be used in
     * BettalimsMessageResourceTest.testFileList to send the messages to Mercury.
     * Note: later discovered that this misses many events after PoolingTransfer, because of workflow validation errors.
     */
    @Test(enabled = false, groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testFindMessageFilesForLcSet() {
        try {
            Query nativeQuery = entityManager.createNativeQuery("SELECT " +
                    "    DISTINCT  " +
                    "    l.key, " +
                    "    min(se.start_time), " +
                    "    set1.name " +
                    "FROM " +
                    "    lcset l " +
                    "    INNER JOIN lab_workflow lw " +
                    "        ON   lw.lcset_id = l.lcset_id " +
                    "    INNER JOIN lab_workflow_workflow_reg lwwr " +
                    "        ON   lwwr.lab_workflow_id = lw.lab_workflow_id " +
                    "    INNER JOIN lab_workflow_registration lwr " +
                    "        ON   lwr.lab_workflow_registration_id = lwwr.lab_workflow_registration_id " +
                    "    INNER JOIN station_event se " +
                    "        ON   se.station_event_id = lwr.station_event_id " +
                    "    INNER JOIN station_event_type set1 " +
                    "        ON   set1.station_event_type_id = se.station_event_type_id " +
                    "WHERE " +
                    "    l.\"KEY\" IN ( " +
                    "'LCSET-2339', " +
                    "'LCSET-2403', " +
                    "'LCSET-2420', " +
                    "'LCSET-2422', " +
                    "'LCSET-2423', " +
                    "'LCSET-2424', " +
                    "'LCSET-2425', " +
                    "'LCSET-2427', " +
                    "'LCSET-2428', " +
                    "'LCSET-2432', " +
                    "'LCSET-2433', " +
                    "'LCSET-2436', " +
                    "'LCSET-2438', " +
                    "'LCSET-2439', " +
                    "'LCSET-2442', " +
                    "'LCSET-2445', " +
                    "'LCSET-2456', " +
                    "'LCSET-2470', " +
                    "'LCSET-2471', " +
                    "'LCSET-2472', " +
                    "'LCSET-2473', " +
                    "'LCSET-2477', " +
                    "'LCSET-2479', " +
                    "'LCSET-2486', " +
                    "'LCSET-2489', " +
                    "'LCSET-2490', " +
                    "'LCSET-2491', " +
                    "'LCSET-2492', " +
                    "'LCSET-2495', " +
                    "'LCSET-2497', " +
                    "'LCSET-2498', " +
                    "'LCSET-2501', " +
                    "'LCSET-2502', " +
                    "'LCSET-2504', " +
                    "'LCSET-2507', " +
                    "'LCSET-2508', " +
                    "'LCSET-2509', " +
                    "'LCSET-2511', " +
                    "'LCSET-2512', " +
                    "'LCSET-2515', " +
                    "'LCSET-2516', " +
                    "'LCSET-2519', " +
                    "'LCSET-2521', " +
                    "'LCSET-2522', " +
                    "'LCSET-2523', " +
                    "'LCSET-2527', " +
                    "'LCSET-2528', " +
                    "'LCSET-2531', " +
                    "'LCSET-2542', " +
                    "'LCSET-2547', " +
                    "'LCSET-2563', " +
                    "'LCSET-2573', " +
                    "'LCSET-2578', " +
                    "'LCSET-2582', " +
                    "'LCSET-2599', " +
                    "'LCSET-2603', " +
                    "'LCSET-2606', " +
                    "'LCSET-2607', " +
                    "'LCSET-2613', " +
                    "'LCSET-2615', " +
                    "'LCSET-2616', " +
                    "'LCSET-2622', " +
                    "'LCSET-2623', " +
                    "'LCSET-2700'" +
                    ") " +
                    "GROUP BY " +
                    "    l.key, " +
                    "    set1.name " +
                    "ORDER BY " +
                    "    1, " +
                    "    2 ");
            List<?> resultList = nativeQuery.getResultList();
            File inboxDirectory = new File("C:/Temp/seq/lims/bettalims/production/inbox");
            String previousLcSset = "";

            for (Object o : resultList) {
                Object[] columns = (Object[]) o;
                String lcSet = (String) columns[0];
                Date startTime = (Date) columns[1];
                String eventName = (String) columns[2];

                if(!lcSet.equals(previousLcSset)) {
                    System.out.println("# "+ lcSet);
                    previousLcSset = lcSet;
                }
                GregorianCalendar gregorianCalendar = new GregorianCalendar();
                gregorianCalendar.setTime(startTime);
                int year = gregorianCalendar.get(Calendar.YEAR);
                int month = gregorianCalendar.get(Calendar.MONTH) + 1;
                int day = gregorianCalendar.get(Calendar.DAY_OF_MONTH);
                int hour = gregorianCalendar.get(Calendar.HOUR_OF_DAY);
                int minute = gregorianCalendar.get(Calendar.MINUTE);
                int second = gregorianCalendar.get(Calendar.SECOND);
                String yearMonthDay = String.format("%d%02d%02d", year, month, day);
                String dateString = xmlDateFormat.format(startTime);

                File dayDirectory = new File(inboxDirectory, yearMonthDay);
                String[] messageFileList = dayDirectory.list();
                if(messageFileList == null) {
                    throw new RuntimeException("Failed to find directory " + dayDirectory.getName());
                }
                Collections.sort(Arrays.asList(messageFileList));
                boolean found = false;
                for (String fileName : messageFileList) {
                    File messageFile = new File(dayDirectory, fileName);
                    String message = FileUtils.readFileToString(messageFile);
                    // Strip tube and flowcell transfer dates are assigned on the server, so they may not match
                    // what's in the file, hence we have to do a lenient check against the file name
                    if(eventName.equals("StripTubeBTransfer") || eventName.equals("FlowcellTransfer")) {
                        if(message.contains(eventName) &&
                                (fileName.startsWith(yearMonthDay + "_" + String.format("%02d%02d", hour, minute)) ||
                                fileName.startsWith(yearMonthDay + "_" + String.format("%02d%02d", hour, minute + 1)) ||
                                fileName.startsWith(yearMonthDay + "_" + String.format("%02d", hour)) ||
                                fileName.startsWith(yearMonthDay + "_" + String.format("%02d", hour + 1)))) {
                            System.out.println("#" + eventName);
                            System.out.println(messageFile.getCanonicalPath());
                            found = true;
                            break;
                        }
                    } else if(message.contains(eventName) && message.contains(dateString)) {
                        System.out.println("#" + eventName);
                        System.out.println(messageFile.getCanonicalPath());
                        found = true;
                        break;
                    }
                }
                if(!found) {
                    System.out.println("#Failed to find file for " + lcSet + " " + eventName + " " + startTime);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test(enabled = false, groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testImportQuants() {
        Query nativeQuery = entityManager.createNativeQuery("SELECT " +
                "     lqr.run_name, " +
                "     lqr.run_date, " +
                "     lqt.quant_type_name, " +
                "     lq.quant_value, " +
                "     r.barcode " +
                "FROM " +
                "     library_quant_run lqr " +
                "     INNER JOIN library_quant_type lqt " +
                "          ON   lqt.quant_type_id = lqr.quant_type_id " +
                "     INNER JOIN library_quant lq " +
                "          ON   lq.quant_run_id = lqr.run_id " +
                "     INNER JOIN receptacle r " +
                "          ON   r.receptacle_id = lq.receptacle_id " +
                "WHERE " +
                "     lq.is_archived = 'N' AND lqr.run_name = :lcSetNumber " +
                "ORDER BY " +
                "     1, 3");
        nativeQuery.setParameter("lcSetNumber", "2588");
        List<?> resultList = nativeQuery.getResultList();

        ArrayList<VesselMetricBean> vesselMetricBeans = new ArrayList<VesselMetricBean>();
        String previousQuantType = "";
        VesselMetricRunBean vesselMetricRunBean = null;
        for (Object o : resultList) {
            Object[] columns = (Object[]) o;
            String runName = (String) columns[0];
            Date runDate = (Date) columns[1];
            String quantTypeName = (String) columns[2];
            BigDecimal quantValue = (BigDecimal) columns[3];
            String barcode = (String) columns[4];

            if(!quantTypeName.equals(previousQuantType)) {
                if(!previousQuantType.isEmpty()) {
                    ImportFromBspTest.recordMetrics(vesselMetricRunBean);
                }
                vesselMetricBeans.clear();
                vesselMetricRunBean = new VesselMetricRunBean(runName, runDate, quantTypeName,
                        vesselMetricBeans);
                previousQuantType = quantTypeName;
            }
            vesselMetricBeans.add(new VesselMetricBean(barcode, quantValue.toString(), "ng/uL"));
        }

        ImportFromBspTest.recordMetrics(vesselMetricRunBean);
    }

    @Test(enabled = false, groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testImportRuns() {
        Query nativeQuery = entityManager.createNativeQuery("SELECT " +
                "    r.barcode AS flowcell_barcode, " +
                "    ngr.barcode AS run_barcode, " +
                "    ngr.run_date, " +
                "    lm.machine_name, " +
                "    odd.directory_path " +
                "FROM " +
                "    solexa_run sr " +
                "    INNER JOIN next_generation_run ngr " +
                "        ON   ngr.run_id = sr.run_id " +
                "    INNER JOIN object_data_directory odd " +
                "        ON   odd.directory_id = ngr.directory_id " +
                "    INNER JOIN lab_machine lm " +
                "        ON   lm.lab_machine_id = ngr.lab_machine_id " +
                "    INNER JOIN flowcell f " +
                "        ON   f.flowcell_id = sr.new_flowcell_id " +
                "    INNER JOIN receptacle r " +
                "        ON   r.receptacle_id = f.flowcell_id " +
                "WHERE " +
                "    r.barcode IN ( " +
                "'C19F5ACXX', " +
                "'C1E2VACXX', " +
                "'C1E35ACXX', " +
                "'C1E3NACXX', " +
                "'C1EAAACXX', " +
                "'C1EAHACXX', " +
                "'C1EE5ACXX', " +
                "'C1EK8ACXX', " +
                "'C1ETHACXX', " +
                "'C1EU9ACXX', " +
                "'D1DUTACXX', " +
                "'D1J6MACXX', " +
                "'D1JN1ACXX', " +
                "'D1JNDACXX', " +
                "'D1JP2ACXX', " +
                "'D1JPLACXX', " +
                "'D1JRRACXX', " +
                "'D1JYWACXX', " +
                "'D1K4LACXX', " +
                "'D1K54ACXX', " +
                "'D1K5DACXX', " +
                "'D1K7DACXD') ");
        List<?> resultList = nativeQuery.getResultList();
        for (Object o : resultList) {
            Object[] columns = (Object[]) o;
            String flowcellBarcode = (String) columns[0];
            String runBarcode = (String) columns[1];
            Date runDate = (Date) columns[2];
            String machineName = (String) columns[3];
            String directory = (String) columns[4];

            SolexaRunBean solexaRunBean = new SolexaRunBean(flowcellBarcode, runBarcode, runDate, machineName, directory, null);
            String response = Client.create().resource(TEST_MERCURY_URL + "/rest/solexarun")
                    .type(MediaType.APPLICATION_XML_TYPE)
                    .accept(MediaType.APPLICATION_XML)
                    .entity(solexaRunBean)
                    .post(String.class);
            System.out.println(response);
        }
    }

    /*
SELECT
	lqr.run_name,
	lqr.run_date,
	lqt.quant_type_name,
	lq.quant_value,
	NULL,
	r.barcode
FROM
	library_quant_run lqr
	INNER JOIN library_quant_type lqt
		ON   lqt.quant_type_id = lqr.quant_type_id
	INNER JOIN library_quant lq
		ON   lq.quant_run_id = lqr.run_id
	INNER JOIN receptacle r
		ON   r.receptacle_id = lq.receptacle_id
WHERE
	lq.is_archived = 'N'
UNION ALL
SELECT
	qr.run_name,
	qr.run_date,
	'QPCR',
	qrl.concentration,
	qrl.status,
	r.barcode
FROM
	qpcr_run qr
	INNER JOIN qpcr_run_library qrl
		ON   qrl.qpcr_run_id = qr.qpcr_run_id
	INNER JOIN seq_content sc
		ON   sc.seq_content_id = qrl.seq_content_id
	INNER JOIN seq_content_descr_set scds
		ON   scds.seq_content_id = sc.seq_content_id
	INNER JOIN seq_content sc2
		ON   sc2.seq_content_id = scds.seq_content_id
	INNER JOIN receptacle r
		ON   r.receptacle_id = sc2.receptacle_id

		493,677 rows
     */
/*
SELECT
	DISTINCT product_order_name,
--	set1."NAME",
	l."KEY"--,
--	l2."KEY"
FROM
	work_request_material_descr
	INNER JOIN work_request_material wrm
		ON   wrm.work_request_material_id = work_request_material_descr.work_request_material_id
	INNER JOIN work_request wr
		ON   wr.work_request_id = wrm.work_request_id
	INNER JOIN lcset l
		ON   l.lcset_id = wr.lcset_id
	INNER JOIN wr_material_recep_membership wmrm
		ON   wmrm.work_request_material_id = wrm.work_request_material_id
	INNER JOIN receptacle r
		ON   r.receptacle_id = wmrm.receptacle_id
--	INNER JOIN lab_workflow_receptacle lwr
--		ON   lwr.receptacle_id = r.receptacle_id
--	INNER JOIN lab_workflow lw
--		ON   lw.lab_workflow_id = lwr.lab_workflow_id
--	INNER JOIN lcset l2
--		ON   l2.lcset_id = lw.lcset_id
	INNER JOIN receptacle_transfer_event rte
		ON   rte.src_receptacle_id = r.receptacle_id
	INNER JOIN receptacle_event re
		ON   re.station_event_id = rte.station_event_id
	INNER JOIN station_event se
		ON   se.station_event_id = re.station_event_id
	INNER JOIN station_event_type set1
		ON   set1.station_event_type_id = se.station_event_type_id
WHERE
	product_order_name IS NOT NULL
	AND set1.name = 'StripTubeBTransfer';
*/
}
