package org.broadinstitute.gpinformatics.mercury.entity.reagent;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.MolecularIndexDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.MolecularIndexingSchemeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.ReagentDesignDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.BarcodedTubeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.StaticPlateDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.PlateWell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A Test to import reagents from Squid.  This prepares an empty database to accept messages.  This must
 * be in the same package as MolecularIndexingScheme, because it uses package visible methods on that class.
 * <p/>
 * Wildfly rejects deploying with non-existing persistence unit - uncomment attribute if running <br />
 * Use the following VM options in the test: -Xmx1G -XX:MaxPermSize=128M
 * <p/>
 * As of January 2013, the test takes about 35 minutes to run.
 */
@Test(groups = TestGroups.STANDARD)
public class ImportFromSquidTest extends Arquillian {

    public static final String TEST_MERCURY_URL = "https://localhost:8443/Mercury";
    // @PersistenceContext(unitName = "squid_pu")
    private EntityManager entityManager;

    @Inject
    private MolecularIndexingSchemeDao molecularIndexingSchemeDao;

    @Inject
    private MolecularIndexDao molecularIndexDao;

    @Inject
    private StaticPlateDao staticPlateDao;

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private BarcodedTubeDao barcodedTubeDao;

    @Inject
    private ReagentDesignDao reagentDesignDao;

    @Inject
    private UserBean userBean;

    @Deployment
    public static WebArchive buildMercuryWar() {
        // change dataSourceEnvironment parameter to "prod" when importing from production.
        return DeploymentBuilder.buildMercuryWar(
                org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV, "dev");
    }

    /**
     * Import index schemes from Squid.
     */
    @Test(enabled = false, groups = TestGroups.STANDARD)
    public void testImportIndexingSchemes() {
        userBean.loginOSUser();
        // Get schemes and sequences from Squid.
        Query nativeQuery = entityManager.createNativeQuery(
                "SELECT " +
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

        // Get schemes from Mercury, to avoid creating duplicates.
        List<MolecularIndexingScheme> mercurySchemes =
                molecularIndexingSchemeDao.findAll(MolecularIndexingScheme.class);
        Map<String, MolecularIndexingScheme> mapNameToScheme = new HashMap<>();
        for (MolecularIndexingScheme mercuryScheme : mercurySchemes) {
            mapNameToScheme.put(mercuryScheme.getName(), mercuryScheme);
        }

        // Reuse sequences across schemes, because sequence has a unique constraint.
        List<MolecularIndex> molecularIndexes = molecularIndexDao.findAll(MolecularIndex.class);
        Map<String, MolecularIndex> mapSequenceToIndex = new HashMap<>();
        for (MolecularIndex molecularIndex : molecularIndexes) {
            mapSequenceToIndex.put(molecularIndex.getSequence(), molecularIndex);
        }

        String previousSchemeName = "";
        MolecularIndexingScheme molecularIndexingScheme = null;
        for (Object o : resultList) {
            Object[] columns = (Object[]) o;
            String schemeName = (String) columns[0];
            String positionHint = (String) columns[1];
            String sequence = (String) columns[2];

            if (!schemeName.equals(previousSchemeName)) {
                if (molecularIndexingScheme != null && !mapNameToScheme
                        .containsKey(molecularIndexingScheme.getName())) {
                    molecularIndexingSchemeDao.persist(molecularIndexingScheme);
                }
                previousSchemeName = schemeName;
                molecularIndexingScheme = new MolecularIndexingScheme();
                molecularIndexingScheme.setName(schemeName);
            }
            MolecularIndex molecularIndex = mapSequenceToIndex.get(sequence);
            if (molecularIndex == null) {
                molecularIndex = new MolecularIndex(sequence);
                mapSequenceToIndex.put(sequence, molecularIndex);
            }
            assert molecularIndexingScheme != null;
            molecularIndexingScheme
                    .addIndexPosition(MolecularIndexingScheme.IndexPosition.valueOf(positionHint), molecularIndex);
        }
        if (molecularIndexingScheme != null && !mapNameToScheme.containsKey(molecularIndexingScheme.getName())) {
            molecularIndexingSchemeDao.persist(molecularIndexingScheme);
        }
    }

    /**
     * Import index plates from Squid.  This takes about 15 minutes.
     */
    @Test(enabled = false, groups = TestGroups.STANDARD, dependsOnMethods = {"testImportIndexingSchemes"})
    public void testImportIndexPlates() {
        userBean.loginOSUser();
        // Get plates, wells and molecular indexes from Squid.
        Query nativeQuery = entityManager.createNativeQuery(
                "SELECT " +
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
                "ORDER BY " +
                "     p.barcode, " +
                "     wd.NAME");
        List<?> resultList = nativeQuery.getResultList();

        // Get plates from Mercury, to avoid creating duplicates.
        String previousPlateBarcode = "";
        List<String> plateBarcodes = new ArrayList<>();
        for (Object o : resultList) {
            Object[] columns = (Object[]) o;
            String plateBarcode = (String) columns[0];
            if (!plateBarcode.equals(previousPlateBarcode)) {
                previousPlateBarcode = plateBarcode;
                plateBarcodes.add(plateBarcode);
            }
        }
        Map<String, LabVessel> mapBarcodeToVessel = labVesselDao.findByBarcodes(plateBarcodes);

        previousPlateBarcode = "";
        StaticPlate staticPlate = null;
        List<StaticPlate> plates = new ArrayList<>();
        Map<String, MolecularIndexingScheme> mapNameToScheme = new HashMap<>();
        for (Object o : resultList) {
            Object[] columns = (Object[]) o;
            String plateBarcode = (String) columns[0];
            String wellName = (String) columns[1];
            String indexingSchemeName = (String) columns[2];
            if (!plateBarcode.equals(previousPlateBarcode)) {
                previousPlateBarcode = plateBarcode;
                if (staticPlate != null) {
                    // Persist periodically, rather than all at the end, to avoid out of memory errors
                    if (plates.size() >= 100) {
                        System.out.println("Persisting " + plates.size() + " plates");
                        staticPlateDao.persistAll(plates);
                        plates.clear();
                        mapNameToScheme.clear();
                        staticPlateDao.clear();
                    }
                }
                staticPlate = new StaticPlate(plateBarcode, StaticPlate.PlateType.IndexedAdapterPlate96);
                if (mapBarcodeToVessel.get(staticPlate.getLabel()) == null) {
                    plates.add(staticPlate);
                }
            }
            VesselPosition vesselPosition = VesselPosition.getByName(wellName);
            PlateWell plateWell = new PlateWell(staticPlate, vesselPosition);
            MolecularIndexingScheme molecularIndexingScheme = mapNameToScheme.get(indexingSchemeName);
            if (molecularIndexingScheme == null) {
                System.out.println("Fetching scheme " + indexingSchemeName);
                molecularIndexingScheme = molecularIndexingSchemeDao.findByName(indexingSchemeName);
                if (molecularIndexingScheme == null) {
                    throw new RuntimeException("Failed to find scheme " + indexingSchemeName);
                }
                mapNameToScheme.put(indexingSchemeName, molecularIndexingScheme);
            }
            plateWell.addReagent(new MolecularIndexReagent(molecularIndexingScheme));

            assert staticPlate != null;
            staticPlate.getContainerRole().addContainedVessel(plateWell, vesselPosition);
        }
        System.out.println("Persisting " + plates.size() + " plates");
        staticPlateDao.persistAll(plates);
        staticPlateDao.clear();
    }

    /**
     * Import bait tubes from Squid.
     */
    @Test(enabled = false, groups = TestGroups.STANDARD)
    public void testCreateBaits() {
        userBean.loginOSUser();
        // todo jmt for a bait to show up in this query, it must have been transferred to a plate at least once.
        // Without this restriction, the number of tubes goes from ~40 to ~70,000
        Query nativeQuery = entityManager.createNativeQuery(
                "SELECT DISTINCT " +
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

        List<String> tubeBarcodes = new ArrayList<>();
        for (Object o : resultList) {
            Object[] columns = (Object[]) o;
            tubeBarcodes.add((String) columns[0]);
        }
        Map<String, BarcodedTube> mapBarcodeToTube = barcodedTubeDao.findByBarcodes(tubeBarcodes);

        Map<String, ReagentDesign> mapNameToReagentDesign = getMapNameToMercuryReagentDesign();

        String previousDesignName = "";
        ReagentDesign reagentDesign = null;
        for (Object o : resultList) {
            Object[] columns = (Object[]) o;
            String barcode = (String) columns[0];
            String designName = (String) columns[1];
            if (!previousDesignName.equals(designName)) {
                reagentDesign = mapNameToReagentDesign.get(designName);
                if (reagentDesign == null) {
                    reagentDesign = new ReagentDesign(designName, ReagentDesign.ReagentType.BAIT);
                    mapNameToReagentDesign.put(designName, reagentDesign);
                }
                previousDesignName = designName;
            }
            BarcodedTube barcodedTube = new BarcodedTube(barcode);
            barcodedTube.addReagent(new DesignedReagent(reagentDesign));
            if (mapBarcodeToTube.get(barcode) == null) {
                barcodedTubeDao.persist(barcodedTube);
            }
        }
        barcodedTubeDao.clear();
    }

    /**
     * Get map of existing Mercury reagent designs
     *
     * @return map from name of reagent design to entity
     */
    private Map<String, ReagentDesign> getMapNameToMercuryReagentDesign() {
        List<ReagentDesign> mercuryReagentDesigns = reagentDesignDao.findAll(ReagentDesign.class);
        Map<String, ReagentDesign> mapNameToReagentDesign = new HashMap<>();
        for (ReagentDesign mercuryReagentDesign : mercuryReagentDesigns) {
            mapNameToReagentDesign.put(mercuryReagentDesign.getBusinessKey(), mercuryReagentDesign);
        }
        return mapNameToReagentDesign;
    }

    /**
     * Import Custom Amplicon Tubes from Squid.
     */
    @Test(enabled = false, groups = TestGroups.STANDARD)
    public void testCreateCats() {
        userBean.loginOSUser();
        // todo jmt for a CAT to show up in this query, it must have been transferred to a plate at least once.
        // Without this restriction, the number of tubes rises from 122 to ~14,000
        Query nativeQuery = entityManager.createNativeQuery(
                "SELECT " +
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

        List<String> tubeBarcodes = new ArrayList<>();
        for (Object o : resultList) {
            Object[] columns = (Object[]) o;
            tubeBarcodes.add((String) columns[2]);
        }
        Map<String, BarcodedTube> mapBarcodeToTube = barcodedTubeDao.findByBarcodes(tubeBarcodes);

        Map<String, ReagentDesign> mapNameToReagentDesign = getMapNameToMercuryReagentDesign();

        String previousDesignName = "";
        ReagentDesign reagentDesign = null;
        for (Object o : resultList) {
            Object[] columns = (Object[]) o;
            String designName = (String) columns[0];
            String vendorDesignName = (String) columns[1];
            String barcode = (String) columns[2];
            if (!previousDesignName.equals(designName)) {
                reagentDesign = mapNameToReagentDesign.get(designName);
                if (reagentDesign == null) {
                    reagentDesign = new ReagentDesign(designName, ReagentDesign.ReagentType.CAT);
                    reagentDesign.setManufacturersName(vendorDesignName);
                    mapNameToReagentDesign.put(designName, reagentDesign);
                }
                previousDesignName = designName;
            }
            BarcodedTube barcodedTube = new BarcodedTube(barcode);
            barcodedTube.addReagent(new DesignedReagent(reagentDesign));
            if (mapBarcodeToTube.get(barcode) == null) {
                barcodedTubeDao.persist(barcodedTube);
            }
        }
        barcodedTubeDao.clear();
    }

}
