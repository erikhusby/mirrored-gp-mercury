package org.broadinstitute.gpinformatics.infrastructure.bsp.migration;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.mercury.control.dao.storage.StorageLocationDao;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample_;
import org.broadinstitute.gpinformatics.mercury.entity.storage.StorageLocation;
import org.broadinstitute.gpinformatics.mercury.entity.storage.StorageLocation_;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel_;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.PlateWell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.PlateWell_;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselGeometry;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.broadinstitute.gpinformatics.mercury.presentation.storage.StorageLocationActionBean;

import javax.ejb.TransactionManagement;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static javax.ejb.TransactionManagementType.BEAN;

/**
 * Does all the heavy lifting for freezer migration logic
 * Put it in an EJB so JavaEE framework bean managed transactions (vs. whatever it is that Arquillian does) can be used in each method call.
 */
@RequestScoped
@TransactionManagement(BEAN)
public class Gplim5728WorkHorseEjb {

    private static final Log logger = LogFactory.getLog(Gplim5728WorkHorseEjb.class);

    final String logDirName = System.getProperty("jboss.server.log.dir");

    // Detailed output log files
    private PrintWriter processPhaseOneLog = null;
    private final String phaseOneLogName = "gplim5728_phase1.log";

    private PrintWriter processPhaseTwoLog = null;
    private final String phaseTwoLogName = "gplim5728_phase2.log";

    // Phase 2 troubleshooting output values
    static final String EMPTY = "";
    static final String SAMPLE = "sample";
    static final String PLATE = "plate";
    static final String RACK = "rack";

    @Inject
    private UserBean userBean;

    @Inject
    StorageLocationDao storageLocationDao;

    @Inject
    UserTransaction utx;

    private String bspURL;
    private String bspUser;
    private String bspPassword;

    /**
     * The freezer migration fixup entry point - migrates all the non-archived freezers locations and
     * the containers/vessels stored in any of the locations
     * @return List of mercury vessel label to BSP receptacle ID for every receptacle in BSP Freezer storage
     */
    public Collection<Pair<LabVessel,Long>> migrateLocsAndStoredVessels(String bspURL, String bspUser, String bspPassword ) throws Exception {

        this.bspURL = bspURL;
        this.bspUser = bspUser;
        this.bspPassword = bspPassword;

        // We want a detailed log of all the activity
        processPhaseOneLog = new PrintWriter( new FileWriter(logDirName + File.separator + phaseOneLogName, true) );
        processPhaseOneLog.write("======== STARTING GPLIM-5728 PHASE 1 FIXUP =============\n");
        processPhaseOneLog.write(SimpleDateFormat.getDateTimeInstance().format(new Date()));
        processPhaseOneLog.write( "\n");

        // Hold mappings to build out location hierarchies and stored vessels
        BspMigrationMapping pkMap = new BspMigrationMapping();
        try {
            utx.begin();

            userBean.loginOSUser();

            List<Long> rootBspPks = migrateRootStorageLocations( pkMap );
            processPhaseOneLog.write("Processed " + rootBspPks.size() + " root storage locations.\n");
            processPhaseOneLog.flush();

            // Recurse into children from parents
            migrateChildStorageLocations( pkMap, rootBspPks );

            // Storage locations complete
            processPhaseOneLog.write("======== FINISHED MIGRATING STORAGE LOCATIONS =============\n\n");
            processPhaseOneLog.write(SimpleDateFormat.getDateTimeInstance().format(new Date()));
            processPhaseOneLog.write( "\n");
            processPhaseOneLog.flush();

            // Migrate racks, etc. with storage locations, build out mapping with attached entities
            migrateBspStoredReceptacles(pkMap);
            processPhaseOneLog.write( "======== FINISHED MIGRATING STORED CONTAINERS =============\n\n" );
            processPhaseOneLog.write(SimpleDateFormat.getDateTimeInstance().format(new Date()));
            processPhaseOneLog.write( "\n");
            processPhaseOneLog.flush();

            storageLocationDao.persist( new FixupCommentary("GPLIM-5728 phase 1 - Migrate BSP Storage locations and stored vessels to Mercury") );
            // storageLocationDao.flush();

            utx.commit();

            // Explicitly release all entities from entity manager to reduce heap memory usage
            storageLocationDao.clear();

            processPhaseOneLog.write("======== FINISHED GPLIM-5728 PHASE 1 FIXUP =============\n");
            processPhaseOneLog.write(SimpleDateFormat.getDateTimeInstance().format(new Date()));

        } catch ( Exception ex ) {
            try {
                utx.rollback();
            } catch ( Exception trex ){
                processPhaseOneLog.write( "Rollback failed \n" );
                processPhaseOneLog.write( ex.getMessage() );
                processPhaseOneLog.write( "\n" );
            }
            if (this.processPhaseOneLog != null ) {
                processPhaseOneLog.write( "Process died due to exception - see server log \n" );
                processPhaseOneLog.write( ex.getMessage() );
            }
            logger.error("Fixup test failed", ex);
            throw ex;
        } finally {
            if (this.processPhaseOneLog != null ) {
                try {  processPhaseOneLog.flush(); processPhaseOneLog.close();  } catch ( Exception ignored ){}
            }
        }

        return pkMap.getStoredContainerLabelToVesselMap().values();
    }


    /**
     * The second phase of the freezer migration builds out the contents of all containers migrated from BSP locations
     * Using the container entities in mapping. build out the contents (plate wells, tube formations, etc.
     * @param labelToReceptacleIds stored vessel label (containers mostly), corresponding BSP receptacle id, mercury storage location ID
     * @return List of six values: type (sample/rack/plate), barcode, tube formation barcode (null if stored container is not a rack, BSP receptacle ID, sample id, true if sample created in mercury)
     */
    public List<String[]> migrateStoredVesselContents(String bspURL, String bspUser, String bspPassword, List<Triple<String,Long,Long>> labelToReceptacleIds ) throws Exception {

        this.bspURL = bspURL;
        this.bspUser = bspUser;
        this.bspPassword = bspPassword;

        String status = "Batch of " + labelToReceptacleIds.size() + " phase 1 vessels from label [" + labelToReceptacleIds.get(0).getLeft() + "], to [" + labelToReceptacleIds.get( labelToReceptacleIds.size() - 1 ).getLeft() + "]";

        // We want a detailed log of all the activity, appending all new writes
        processPhaseTwoLog = new PrintWriter( new FileWriter(logDirName + File.separator + phaseTwoLogName, true) );
        processPhaseTwoLog.write("======== STARTING GPLIM-5728 PHASE 2 FIXUP BATCH =============\n");
        processPhaseTwoLog.write(SimpleDateFormat.getDateTimeInstance().format(new Date()));
        processPhaseTwoLog.write( "\n");
        processPhaseTwoLog.write("======== " + status + " =============\n");
        processPhaseTwoLog.write("Thread: " + Thread.currentThread().getName() + "\n");
        processPhaseTwoLog.flush();

        // Hold mercury vessel to BSP sample receptacle ID mappings to build out stored vessel samples
        List<String[]> labelToBspReceptaclIdMap = new ArrayList<>();
        Connection conn = null;
        PreparedStatement containerStmt = null;
        PreparedStatement sampleStmt = null;
        int count = 0;
        int tubeCount = 0;

        try {
            utx.begin();
            userBean.loginOSUser();

            // Now build out the contents of the stored containers using entities in the mapping
            try {
                conn = DriverManager.getConnection(bspURL, bspUser, bspPassword);
                containerStmt = conn.prepareStatement("select samp.sample_id \n"
                                             + "     , samp.receptacle_id as tube_id \n"
                                             + "     , nvl( tube.external_id, 'SM-' || samp.sample_id ) as tube_label \n"
                                             + "     , tube_type.receptacle_name as tube_type \n"
                                             + "     , rack.receptacle_id as rack_id \n"
                                             + "     , rack.external_id as rack_label \n"
                                             + "     , tube.receptacle_row \n"
                                             + "     , tube.receptacle_column \n"
                                             + "  from bsp_receptacle tube \n"
                                             + "     , bsp_receptacle_type tube_type \n"
                                             + "     , bsp_receptacle rack \n"
                                             + "     , bsp_sample samp \n"
                                             + " where samp.receptacle_id = tube.receptacle_id \n"
                                             + "   and tube_type.receptacle_type_id = tube.receptacle_type_id \n"
                                             + "   and tube.receptacle_group_id = rack.receptacle_id \n"
                                             + "   and rack.receptacle_id = ? " );  // <-- ********* STORAGE CONTAINER RECEPTACLE **********
                sampleStmt = conn.prepareStatement( "select s.sample_id \n"
                                             + "  from bsp_receptacle r \n"
                                             + "     , bsp_sample s \n"
                                             + " where s.receptacle_id = r.receptacle_id \n"
                                             + "   and r.receptacle_id = ? ");  // <-- ********* STORAGE SAMPLE RECEPTACLE **********

                for( Triple<String,Long,Long> storedVesselData : labelToReceptacleIds ) {
                    String label = storedVesselData.getLeft();
                    Long receptacleId = storedVesselData.getMiddle();
                    Long storageId = storedVesselData.getRight();
                    LabVessel storedVessel = storageLocationDao.findSingle( LabVessel.class, LabVessel_.label, label );
                    // Sanity check - should probably let it die on its own
                    if( storedVessel == null ) {
                        String error = "Stored vessel label (" + label + ") not found in Mercury" ;
                        this.processPhaseTwoLog.write(error);
                        this.processPhaseTwoLog.write("\n");
                        throw new Exception(error);
                    }

                    // Is slide or cryo straw, no contained vessels.  Register as sample source and continue, storage location set in phase 1
                    if( storedVessel instanceof BarcodedTube) {
                        sampleStmt.setLong(1, receptacleId);
                        ResultSet rs = sampleStmt.executeQuery();
                        if( rs.next() ) {
                            String sampleId = rs.getString(1);
                            addSampleToVessel( storedVessel, receptacleId, sampleId, labelToBspReceptaclIdMap);
                            createCheckInEvent(storedVessel, null, new Date(), labelToBspReceptaclIdMap.size());
                            tubeCount++;
                        } else {
                            // Sanity test
                            throw new Exception("No receptacle in BSP for receptacle_id = " + receptacleId + "?!");
                        }
                        rs.close();
                    } else {
                        containerStmt.setLong(1, receptacleId);
                        ResultSet rs = containerStmt.executeQuery();
                        count += buildMercuryStoredVesselContents(rs, storedVessel, labelToBspReceptaclIdMap,
                                processPhaseTwoLog);
                    }
                }

                this.processPhaseTwoLog.write("Processed " + count + " sample vessel container storage \n");
                this.processPhaseTwoLog.write("Processed " + tubeCount + " sample vessel tube only storage \n");

            } finally {
                closeResource(containerStmt);
                closeResource(sampleStmt);
                closeResource(conn);
                this.processPhaseTwoLog.flush();
            }

            storageLocationDao.persist( new FixupCommentary("GPLIM-5728 phase 2 - Migrate BSP Storage location container contents " + status ) );
            //storageLocationDao.flush();

            processPhaseTwoLog.write( "======== FINISHED MIGRATING STORED CONTAINER CONTENTS BATCH =============\n" );
            processPhaseTwoLog.write(SimpleDateFormat.getDateTimeInstance().format(new Date()));
            processPhaseTwoLog.write( "\n\n");
            processPhaseTwoLog.flush();

            storageLocationDao.flush();
            utx.commit();

            storageLocationDao.clear();

            processPhaseTwoLog.write( "======== FINISHED MIGRATING STORED CONTAINER CONTENTS BATCH =============\n" );
            processPhaseTwoLog.write(SimpleDateFormat.getDateTimeInstance().format(new Date()));
            processPhaseTwoLog.write( "\n\n");
            processPhaseTwoLog.flush();
        } catch ( Exception ex ) {
            try {
                utx.rollback();
            } catch ( Exception trex ){
                processPhaseTwoLog.write( "Rollback failed \n" );
                processPhaseTwoLog.write( trex.getMessage() );
                processPhaseTwoLog.write( "\n" );
            }
            if (this.processPhaseTwoLog != null ) {
                processPhaseTwoLog.write( "Process died due to exception \n" );
                processPhaseTwoLog.write( ex.getMessage()==null?ex.getClass().getName():ex.getMessage() );
            }
            logger.error("Phase 2 freezer migration failure", ex);

            processPhaseTwoLog.write( "\n!!!!!!!!!! Recovery requires all processed container records be removed from phase 1 data file !!!!!!!!!!\n" );

            throw ex;
        } finally {
            if (this.processPhaseTwoLog != null ) {
                try {  processPhaseTwoLog.flush(); processPhaseTwoLog.close();  } catch ( Exception ignored ){}
            }
        }

        // Request a garbage collection
        System.gc();

        return labelToBspReceptaclIdMap;
    }

    private void addSampleToVessel(LabVessel storedVessel, Long receptacleId, String sampleId, List<String[]> labelToBspReceptaclIdMap) {
        if( storedVessel.getMercurySamples().size() > 0 ) {
            // At least one sample already attached, do nothing
            labelToBspReceptaclIdMap.add( new String[] {SAMPLE, storedVessel.getLabel(), EMPTY, receptacleId.toString(), sampleId, Boolean.FALSE.toString() } );
        } else {
            String mercurySampleKey = "SM-" + sampleId;
            MercurySample sample = storageLocationDao.findSingle(MercurySample.class, MercurySample_.sampleKey, mercurySampleKey);
            if( sample == null ) {
                sample = new MercurySample( "SM-" + sampleId, MercurySample.MetadataSource.BSP );
                storageLocationDao.persist(sample);
            }
            storedVessel.addSample( sample );
            labelToBspReceptaclIdMap.add( new String[] {SAMPLE, storedVessel.getLabel(), EMPTY, receptacleId.toString(), sampleId, Boolean.TRUE.toString() } );
        }
    }

    /**
     * Migrates the root storage locations to Mercury to start hierarchy drill down
     */
    private List<Long> migrateRootStorageLocations( BspMigrationMapping pkMap ) throws Exception {
        Connection conn = null;
        Statement stmt = null;
        List<Long> rootPks = null;
        try {
            conn = DriverManager.getConnection(bspURL, bspUser, bspPassword);
            stmt = conn.createStatement();
            // Result columns MUST match migrateChildStorageLocations!!
            ResultSet rs = stmt.executeQuery("SELECT sc.storage_container_id, \n"
                                             + "       sc.parent_strg_container_id, \n"
                                             + "       sc.storage_container_name, \n"
                                             + "       NVL( st.type_name, 'Unspecified' ) as storage_type \n"
                                             + "  FROM bsp_storage_container sc, \n"
                                             + "       bsp_storage_container_type st \n"
                                             + " WHERE sc.archived = 0 \n"
                                             + "   AND st.storage_container_type_id = sc.storage_container_type_id \n"
                                             + "   AND sc.parent_strg_container_id IS NULL \n"  // <-- ********* ROOTS HAVE NO PARENT **********
                                             + "   AND sc.storage_container_id NOT IN ( 1, 2 )" );  // <-- ********* EXCLUDE RTS1 and RTS2 **********
            // Do not re-use ResultSet! Closed when migration complete
            rootPks = buildMercuryLocations(rs, pkMap);
        } finally {
            // De-allocate
            closeResource(stmt);
            closeResource(conn);
        }

        return rootPks;
    }

    /**
     * Drill down each hierarchy level and migrates the root storage locations to Mercury
     */
    private List<Long> migrateChildStorageLocations( BspMigrationMapping pkMap, List<Long> parentPks ) throws Exception {
        Connection conn = null;
        PreparedStatement stmt = null;
        List<Long> allChildPks = new ArrayList<>();

        try {
            conn = DriverManager.getConnection(bspURL, bspUser, bspPassword);
            // Result columns MUST match migrateRootStorageLocations!!
            stmt = conn.prepareStatement("SELECT sc.storage_container_id, \n"
                                         + "       sc.parent_strg_container_id, \n"
                                         + "       sc.storage_container_name, \n"
                                         + "       NVL( st.type_name, 'Unspecified' ) as storage_type \n"
                                         + "  FROM bsp_storage_container sc, \n"
                                         + "       bsp_storage_container_type st \n"
                                         + " WHERE sc.archived = 0 \n"
                                         + "   AND st.storage_container_type_id = sc.storage_container_type_id \n"
                                         + "   AND sc.parent_strg_container_id = ? " );  // <-- ********* CHILDREN OF PARENT **********
            for( Long parentPk : parentPks ) {
                stmt.setLong(1, parentPk );
                ResultSet rs = stmt.executeQuery();
                List<Long> childPks = buildMercuryLocations( rs, pkMap );
                // A child can't have 2 parents so no need to worry about non-unique PKs
                allChildPks.addAll( childPks );
            }

        } finally {
            // Clean up DB resources
            closeResource(stmt);
            closeResource(conn);
        }

        if( allChildPks.size() > 0 ) {
            this.processPhaseOneLog
                    .write("Processed " + allChildPks.size() + " child storage locations for " + parentPks.size() + " parent locations.\n");
            this.processPhaseOneLog.flush();
            migrateChildStorageLocations( pkMap, allChildPks );
        } else {
            this.processPhaseOneLog.write( "Hit the bottom of the hierarchy and stopping recursion.\n");
            this.processPhaseOneLog.flush();
        }

        return allChildPks;
    }

    /**
     * Create the Mercury StorageLocation objects for root and child nodes
     */
    private List<Long> buildMercuryLocations(ResultSet rs, BspMigrationMapping pkMap ) throws Exception {
        Long bspPk;
        Long bspParentPk;
        String bspName;
        String bspStorageType;
        StorageLocation.LocationType mercuryLocationType;
        StorageLocation mercuryStorageLocation;
        StorageLocation mercuryParentStorageLocation;

        List<Long> bspPks = new ArrayList<>();

        while ( true ) {
            if( !rs.next() ) {
                break;
            }
            bspPk = rs.getLong(1 );
            bspPks.add (bspPk );

            bspParentPk = rs.getLong( 2 );
            if( rs.wasNull() ) {
                bspParentPk = null;
            }

            bspName = rs.getString( 3 );
            bspStorageType = rs.getString( 4 );

            mercuryLocationType = pkMap.getMercuryLocationTypeFromBspType( bspStorageType );
            if( mercuryLocationType == null ) {
                // Should get a match, bad logic?  Die!
                throw new RuntimeException("No mercury storage location to match BSP name " + bspStorageType );
            }

            if( bspParentPk == null ) {
                mercuryStorageLocation = new StorageLocation( bspName, mercuryLocationType, null  );
            } else {
                mercuryParentStorageLocation = pkMap.getMercuryLocationPk( bspParentPk );

                if( mercuryParentStorageLocation == null ) {
                    // Why hasn't the parent been persisted?  Bad logic? Die!
                    throw new RuntimeException("No mercury parent storage location found to match BSP parent PK " + bspParentPk );
                }

                mercuryStorageLocation = new StorageLocation( bspName, mercuryLocationType, mercuryParentStorageLocation  );
            }

            storageLocationDao.persist(mercuryStorageLocation);
            pkMap.addBspToMercuryLocationPk(bspPk, mercuryStorageLocation);
        }

        // Clean up DB resources
        closeResource(rs);

        return bspPks;
    }

    /**
     * Builds out contents of containers
     * @param rs  BSP container (group) contents
     * @param storedContainer container in Mercury (attached to entity manager)
     * @param labelToBspReceptaclIdMap Reference to add data to (container label, tube formation label, BSP receptacle ID)
     * @param out Log writer (using instance variable throws NPE)
     * @return Count of children added
     */
    private int buildMercuryStoredVesselContents(ResultSet rs, LabVessel storedContainer, List<String[]> labelToBspReceptaclIdMap, PrintWriter out ) throws Exception {
        int childCount = 0;

        // Result set fields in this order
        String smId;
        Long tubeId;
        String tubeLabel;
        String tubeType;
        Long containerId = null; // Same value for entire result set
        String rackLabel; // Same value for entire result set
        Integer row;
        Integer column;

        Date eventdate = new Date();

        RackOfTubes rackOfTubes = null;
        StaticPlate staticPlate = null;
        Map<VesselPosition, BarcodedTube> mapPositionToTube = null;

        while (true) {
            // ***** Begin column data fetching
            if (!rs.next()) {
                break;
            }
            smId = rs.getString(1);
            tubeId = rs.getLong(2);
            tubeLabel = rs.getString(3);
            if (rs.wasNull()) {
                // Create plate well barcode or assign to sm id later
                tubeLabel = null;
            }
            tubeType = rs.getString(4);
            containerId = rs.getLong(5);
            rackLabel = rs.getString(6);
            if (rs.wasNull()) {
                rackLabel = "CO-" + containerId;
            }
            //String rackType = rs.getString(7);
            row = rs.getInt(7);
            if (rs.wasNull()) {
                row = null;
            }
            column = rs.getInt(8);
            if (rs.wasNull()) {
                column = null;
            }
            // ***** End column data fetching

            if (row == null || column == null) {
                // Log it and carry on, skipping contents
                out.write(
                        "No row and/or column in stored container " + rackLabel + ", Sample " + smId + ", ignoring.\n");
                continue;
            }

            // Different logic for different container types, but a result set applies to only one container
            Pair<Class<? extends LabVessel>, Object> classAndType = BspMigrationMapping.getMercuryVesselType(tubeType);
            if (storedContainer instanceof RackOfTubes) {
                // Initialize variables representing rack of tubes
                if (rackOfTubes == null) {
                    rackOfTubes = (RackOfTubes) storedContainer;
                    mapPositionToTube = new HashMap<>();
                }

                if (classAndType == null || !classAndType.getLeft().equals(BarcodedTube.class)) {
                    // Log it and carry on, skipping contents
                    out.write("Rack of tubes cannot contain other than BarcodedTube, rack " + rackLabel + ", Sample "
                              + smId + ", ignoring.\n");
                    continue;
                }
                BarcodedTube.BarcodedTubeType barcodedTubeType =
                        (BarcodedTube.BarcodedTubeType) classAndType.getRight();

                VesselGeometry geometry = rackOfTubes.getVesselGeometry();
                VesselPosition position;
                try {
                    position = geometry.getVesselPositions()[(row * geometry.getColumnCount()) + column];
                } catch (ArrayIndexOutOfBoundsException ae) {
                    String error = "Geometry " + geometry + " for receptacle id " + containerId + ", sample id " + smId
                                   + " is inconsistent with row " + row + " and column " + column + ". (index = " + ae
                                           .getMessage() + ")\n";
                    out.write(error);
                    continue;
                    //throw new RuntimeException( error );
                }

                if (tubeLabel == null) {
                    tubeLabel = smId;
                }

                LabVessel containedVessel = storageLocationDao.findSingle(LabVessel.class, LabVessel_.label, tubeLabel);
                if (containedVessel == null) {
                    containedVessel = new BarcodedTube(tubeLabel, barcodedTubeType);
                    storageLocationDao.persist(containedVessel);
                }
                if (!(containedVessel instanceof BarcodedTube)) {
                    throw new RuntimeException(
                            "RackOfTubes content in mercury must be a BarcodedTube, label: " + tubeLabel);
                }
                mapPositionToTube.put(position, (BarcodedTube) containedVessel);

                // Store the tube in same location as rack
                containedVessel.setStorageLocation(rackOfTubes.getStorageLocation());

                // Add sample to tube
                addSampleToVessel( containedVessel, tubeId, smId, labelToBspReceptaclIdMap);


            } else if (storedContainer instanceof StaticPlate) {

                // Initialize variables representing static plate
                if (staticPlate == null) {
                    staticPlate = (StaticPlate) storedContainer;
                }

                if (classAndType == null || !classAndType.getLeft().equals(PlateWell.class)) {
                    // Log it and carry on, skipping contents
                    out.write("Static plate cannot contain other than plate wells, plate " + rackLabel + ", Sample "
                              + smId + ", ignoring.\n");
                    continue;
                }

                VesselGeometry geometry = storedContainer.getVesselGeometry();
                VesselPosition position = geometry.getVesselPositions()[(row * geometry.getColumnCount()) + column];

                PlateWell well = storageLocationDao
                        .findSingle(PlateWell.class, PlateWell_.label, staticPlate.getLabel() + position);
                if (well == null) {
                    well = new PlateWell(staticPlate, position);
                    storageLocationDao.persist(well);
                    staticPlate.getContainerRole().addContainedVessel(well, position);
                }
                // Wells don't have storage location attached

                // Add sample to well
                addSampleToVessel( well, tubeId, smId, labelToBspReceptaclIdMap);

            }

            childCount++;

        } // End of container contents

        // Clean up DB resources
        closeResource(rs);

        // Don't do anything if no contents
        if (childCount == 0) {
            return 0;
        }

        if ( rackOfTubes != null ) {
            // Persist the tube formation
            TubeFormation tubeFormation = new TubeFormation(mapPositionToTube, rackOfTubes.getRackType());
            TubeFormation mercuryTubeFormation =
                    storageLocationDao.findSingle(TubeFormation.class, LabVessel_.label, tubeFormation.getLabel());
            if (mercuryTubeFormation != null) {
                tubeFormation = mercuryTubeFormation;
            } else {
                storageLocationDao.persist(tubeFormation);
            }
            tubeFormation.addRackOfTubes(rackOfTubes);

            // Add entry for rack
            labelToBspReceptaclIdMap.add( new String[]{RACK, rackOfTubes.getLabel(), tubeFormation.getLabel(), containerId.toString(), EMPTY, EMPTY});

            createCheckInEvent(tubeFormation, rackOfTubes, eventdate, labelToBspReceptaclIdMap.size());
        } else if (staticPlate != null) {
            labelToBspReceptaclIdMap.add( new String[]{PLATE, staticPlate.getLabel(), EMPTY, containerId.toString(), EMPTY, EMPTY});
            createCheckInEvent(staticPlate, null, eventdate, labelToBspReceptaclIdMap.size());
        }

        return childCount;
    }

    /**
     * Imports the receptacles in BSP with storage locations to Mercury
     */
    private void migrateBspStoredReceptacles(BspMigrationMapping pkMap ) throws Exception {
        // BSP data via old school JDBC access
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        int newCount = 0;
        int updateCount = 0;

        try {
            conn = DriverManager.getConnection(bspURL, bspUser, bspPassword);
            stmt = conn.createStatement();
            rs   = stmt.executeQuery("select r.receptacle_id \n"
                                     //+ "     , r.location_id \n"
                                     + "     , NVL( container_id_level_4, NVL( container_id_level_3, NVL( container_id_level_2, container_id_level_1 ) ) ) as container_id \n"
                                     + "     , rt.receptacle_name as receptacle_type \n"
                                     + "     , r.external_id \n"
                                     + "     , CASE rt.container WHEN 1 THEN CAST( NULL AS VARCHAR2(12) ) \n"
                                     + "       ELSE ( SELECT 'SM-' || s.sample_id from bsp_sample s where s.receptacle_id = r.receptacle_id ) END as sample_id \n"
                                     + "  from bsp_receptacle r \n"
                                     + "     , bsp_receptacle_type rt \n"
                                     + "     , bsp_location l \n"
                                     + " where rt.receptacle_type_id = r.receptacle_type_id \n"
                                     + "   and r.location_id = l.location_id \n"
                                     + "   and r.location_id is not null \n"
                                     + "   and r.location_id NOT IN ( 334, 4992 ) "); // Exclude anything stored in RTS1 and RTS2 (matrix tubes)

            Long bspReceptacleId, bspContainerId;
            String bspReceptacleType, bspLabel, bspSampleId;
            while ( true ) {
                // BSP data fields
                if( !rs.next() ) {
                    break;
                }
                bspReceptacleId = rs.getLong(1);
                //bspLocationId = rs.getLong(2);
                bspContainerId = rs.getLong(2);
                // All 4 container ids are null in certain cases
                if( rs.wasNull() ) {
                    bspContainerId = 0L;
                }
                bspReceptacleType = rs.getString(3);
                // external_id is manufacturers label
                bspLabel = rs.getString(4);
                if( rs.wasNull() ) {
                    bspLabel = null;
                }

                bspSampleId = rs.getString(5);
                // Null external_id uses sample id as label
                // If no sample id, prepend CO- to bspReceptacleId for label
                if( rs.wasNull() ) {
                    bspSampleId = null;
                }

                // Mercury lab vessel type
                Pair<Class<? extends LabVessel>,Object> classAndType = pkMap.getMercuryVesselType( bspReceptacleType );
                if( classAndType == null ) {
                    processPhaseOneLog.write("No mercury vessel type found for BSP receptacle type " + bspReceptacleType );
                    processPhaseOneLog.write("  -->   (bspReceptacleId: " + bspReceptacleId + ")\n" );
                    continue;
                }

                LabVessel storageVessel = null;
                Class<? extends LabVessel> vesselSubClass = classAndType.getLeft();
                if( vesselSubClass.equals( RackOfTubes.class ) ) {
                    String label;
                    if( bspLabel != null ) {
                        label = bspLabel;
                    } else {
                        label = "CO-" + bspReceptacleId;
                    }
                    storageVessel = new RackOfTubes( label, (RackOfTubes.RackType) classAndType.getRight() );
                } else if ( vesselSubClass.equals( StaticPlate.class ) ) {
                    String label;
                    if (bspLabel != null) {
                        label = bspLabel;
                    } else {
                        label = "CO-" + bspReceptacleId;
                    }
                    storageVessel = new StaticPlate(label, (StaticPlate.PlateType) classAndType.getRight());
                } else if ( vesselSubClass.equals( BarcodedTube.class ) ) {
                    // Cryo straws and slides are stored without vessel containers
                    String label;
                    if (bspLabel != null) {
                        label = bspLabel;
                    } else if ( bspSampleId != null ) {
                        label = bspSampleId;
                    } else {
                        processPhaseOneLog
                                .write("BSP receptacle type (" + bspReceptacleType + ") is not a container "  );
                        processPhaseOneLog.write("  and has no external_id or sample_id, skipping -->   (bspReceptacleId: " + bspReceptacleId + ")\n" );
                        continue;
                    }
                    storageVessel = new BarcodedTube( label,(BarcodedTube.BarcodedTubeType) classAndType.getRight() );
                } else if ( vesselSubClass.equals( PlateWell.class ) ) {
                    processPhaseOneLog
                            .write("Not putting an individual plate well (" + bspReceptacleType + ") into storage "  );
                    processPhaseOneLog.write("  -->   (bspReceptacleId: " + bspReceptacleId + ")\n" );
                    continue;
                }

                if ( storageVessel == null ) {
                    processPhaseOneLog.write("Can't determine which type of mercury lab vessel corresponds to " + bspReceptacleType  );
                    processPhaseOneLog.write("  -->   (bspReceptacleId: " + bspReceptacleId + ")\n" );
                    continue;
                }

                StorageLocation mercuryStorage = pkMap.getLocationByBspId(bspContainerId);
                if( mercuryStorage == null ) {
                    // Damn! Some BSP storage containers are children of archived parents or there are no container_ids in location
                    //throw new RuntimeException("BSP storage location container ID " + bspContainerId + " was not migrated to Mercury.");
                    if( bspContainerId.equals(0L) ) {
                        processPhaseOneLog.write("BSP storage location has no container ID in any of the 4 levels ");
                    } else {
                        processPhaseOneLog.write("BSP storage location container ID " + bspContainerId + " was not migrated to Mercury, is a parent archived? ");
                    }
                    processPhaseOneLog.write("  -->   (skipping bspReceptacleId: " + bspReceptacleId + ")\n" );
                    continue;
                }

                // Vessel may already be in Mercury
                LabVessel mercuryVessel = storageLocationDao.findSingle( LabVessel.class, LabVessel_.label, storageVessel.getLabel() );
                if( mercuryVessel != null ) {
                    storageVessel = mercuryVessel;
                    updateCount++;
                } else {
                    storageLocationDao.persist(storageVessel);
                    newCount++;
                }

                // Store loose vessels in 'Loose' pseudo location
                if ( vesselSubClass.equals( BarcodedTube.class ) ) {
                    // Need to create loose storage for barcoded tube
                    StorageLocation looseStorage = null;
                    for( StorageLocation loc : mercuryStorage.getChildrenStorageLocation() ) {
                        if( loc.getLocationType() == StorageLocation.LocationType.LOOSE ) {
                            looseStorage = loc;
                            break;
                        }
                    }
                    if( looseStorage == null ) {
                        looseStorage = new StorageLocation( StorageLocation.LocationType.LOOSE.getDisplayName(), StorageLocation.LocationType.LOOSE, mercuryStorage );
                        mercuryStorage.getChildrenStorageLocation().add(looseStorage);
                        storageLocationDao.persist( looseStorage );
                    }
                    mercuryStorage = looseStorage;
                }

                // Vessel attached to entity manager - storage will be persisted
                storageVessel.setStorageLocation(mercuryStorage);

                // We don't do anything with a Mercury static plate other than give it a storage location
                //   , assumption is that wells are already in place
                if( mercuryVessel != null && vesselSubClass.equals( StaticPlate.class ) ) {
                    processPhaseOneLog.write( "Static plate already in Mercury, ignoring wells "  );
                    processPhaseOneLog
                            .write("  -->   (bspReceptacleId: " + bspReceptacleId + ", label: " + storageVessel.getLabel() + ")\n" );
                } else {
                    // Contained vessels in these receptacles will be migrated
                    pkMap.addVesselToBspLocation( storageVessel, bspContainerId, bspReceptacleId );
                }

            }

        } finally {
            // Clean up DB resources
            closeResource( rs );
            closeResource( stmt );
            closeResource( conn );
        }

        processPhaseOneLog.write( "****************\n" );
        processPhaseOneLog.write("Created " + newCount + " stored receptacles (containers) in Mercury\n" );
        processPhaseOneLog.write("Updated location for " + updateCount + " BSP stored receptacles (containers) already in Mercury\n" );
        processPhaseOneLog.write( "****************\n" );

    }

    private void createCheckInEvent( LabVessel inPlaceVessel, RackOfTubes rack, Date eventdate, int disambiguator) {
        LabEvent checkInEvent = new LabEvent(LabEventType.STORAGE_CHECK_IN, eventdate, "Mercury", Long.valueOf(disambiguator), userBean.getBspUser().getUserId(),"Mercury");
        checkInEvent.setInPlaceLabVessel(inPlaceVessel);
        checkInEvent.setAncillaryInPlaceVessel(rack);
        storageLocationDao.persist(checkInEvent);
    }

    private void closeResource( Connection conn ){
        if ( conn != null ) {
            try {  conn.close();  } catch ( Exception e ) {
                processPhaseOneLog.write( "**************** Fail connection close \n" );
                e.printStackTrace(processPhaseOneLog);
            }
        }
    }

    private void closeResource( Statement stmt ){
        if( stmt != null ) {
            try { stmt.close(); } catch ( Exception e ) {
                processPhaseOneLog.write( "**************** Fail statement close \n" );
                e.printStackTrace(processPhaseOneLog);
            }
        }
    }

    private void closeResource( ResultSet rs ){
        if( rs != null ) {
            try { rs.close(); } catch ( Exception e ) {
                processPhaseOneLog.write( "**************** Fail resultset close \n" );
                e.printStackTrace(processPhaseOneLog);
            }
        }
    }
}
