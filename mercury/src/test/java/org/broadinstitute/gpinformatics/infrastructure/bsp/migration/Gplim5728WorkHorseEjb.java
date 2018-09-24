package org.broadinstitute.gpinformatics.infrastructure.bsp.migration;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.gpinformatics.mercury.control.dao.storage.StorageLocationDao;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.entity.storage.StorageLocation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel_;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.PlateWell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselGeometry;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;

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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static javax.ejb.TransactionManagementType.BEAN;

/**
 * Transaction timeout control in an arquillian test is an epic fail - danced around several trial and error solutions and every one failed
 * Put the functionality in a real EJB and it all works.
 */
@RequestScoped
@TransactionManagement(BEAN)
public class Gplim5728WorkHorseEjb {


    PrintWriter processLogWriter = null;

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
     * The freezer migration fixup entry point
     */
    public void justDoIt(String bspURL, String bspUser, String bspPassword ) throws Exception {

        this.bspURL = bspURL;
        this.bspUser = bspUser;
        this.bspPassword = bspPassword;

        DriverManager.registerDriver( new oracle.jdbc.OracleDriver() );

        // We want a detailed log of all the activity
        String logDirName = System.getProperty("jboss.server.log.dir");
        processLogWriter = new PrintWriter( new FileWriter(logDirName + File.separator + "gplim5728_fixup.log", true) );
        ToStringBuilder.setDefaultStyle(ToStringStyle.SHORT_PREFIX_STYLE);
        processLogWriter.write("======== STARTING GPLIM-5728 FIXUP =============\n");
        processLogWriter.write(SimpleDateFormat.getDateTimeInstance().format(new Date()));
        processLogWriter.write( "\n");

        userBean.loginOSUser();

        try {
            utx.begin();
            // Give it 120 minutes
            utx.setTransactionTimeout( 60 * 60);

            // Migrate storage locations
            BspMigrationMapping pkMap = new BspMigrationMapping();
            List<Long> rootBspPks = migrateRootStorageLocations( pkMap );
            processLogWriter.write("Processed " + rootBspPks.size() + " root storage locations.\n");
            processLogWriter.flush();

            // Recurse into children from parents
            migrateChildStorageLocations( pkMap, rootBspPks );

            // Flush the storage locations
            storageLocationDao.flush();
            processLogWriter.write("======== FINISHED MIGRATING STORAGE LOCATIONS =============\n\n");
            processLogWriter.flush();

            // Migrate racks, etc. with storage locations, build out mapping with attached entities
            migrateBspStoredReceptacles(pkMap);
            storageLocationDao.flush();
            processLogWriter.write( "======== FINISHED MIGRATING STORED CONTAINERS =============\n\n" );
            processLogWriter.flush();

            // Now build out the contents of the stored containers using entities in the mapping
            migrateContainerContents( pkMap );
            storageLocationDao.flush();
            processLogWriter.write( "======== FINISHED MIGRATING STORED CONTAINER CONTENTS =============\n\n" );
            processLogWriter.flush();


            // Samples need to be migrated


            storageLocationDao.persist( new FixupCommentary("GPLIM-5728 Migrate BSP Storage to Mercury") );
            storageLocationDao.flush();

            processLogWriter.write("======== FINISHED GPLIM-5728 FIXUP =============\n");
            processLogWriter.write(SimpleDateFormat.getDateTimeInstance().format(new Date()));

            utx.commit();

        } catch ( Exception ex ) {
            try {
                utx.rollback();
            } catch ( Exception trex ){
                processLogWriter.write( "Rollback failed \n" );
                processLogWriter.write( ex.getMessage() );
                processLogWriter.write( "\n" );
            }
            if ( this.processLogWriter != null ) {
                processLogWriter.write( "Process died due to exception \n" );
                processLogWriter.write( ex.getMessage() );
            }
            throw ex;
        } finally {
            if ( this.processLogWriter != null ) {
                try {  processLogWriter.flush(); processLogWriter.close();  } catch ( Exception ignored ){}
            }
        }
    }

    /**
     * Using the container entities in mapping. build out the contents (plate wells, tube formations, etc.
     */
    private void migrateContainerContents(BspMigrationMapping pkMap ) throws Exception {
        Connection conn = null;
        PreparedStatement stmt = null;
        int count = 0;

        try {
            conn = DriverManager.getConnection(bspURL, bspUser, bspPassword);
            // Result columns MUST match migrateRootStorageLocations!!
            stmt = conn.prepareStatement("select 'SM-' || samp.sample_id as sample_id \n"
                                         + "     , samp.receptacle_id as tube_id \n"
                                         + "     , tube.external_id as tube_label \n"
                                         + "     , tube_type.receptacle_name as tube_type \n"
                                         + "     , rack.receptacle_id as rack_id \n"
                                         + "     , rack.external_id as rack_label \n"
                                         + "     , rack_type.receptacle_name as rack_type \n"
                                         + "     , tube.receptacle_row \n"
                                         + "     , tube.receptacle_column \n"
                                         + "  from bsp_sample samp \n"
                                         + "     , bsp_receptacle tube \n"
                                         + "     , bsp_receptacle_type tube_type \n"
                                         + "     , bsp_receptacle rack \n"
                                         + "     , bsp_receptacle_type rack_type \n"
                                         + " where samp.receptacle_id = tube.receptacle_id \n"
                                         + "   and tube_type.receptacle_type_id = tube.receptacle_type_id \n"
                                         + "   and tube.receptacle_group_id = rack.receptacle_id \n"
                                         + "   and rack_type.receptacle_type_id = rack.receptacle_type_id \n"
                                         + "   and rack.receptacle_id = ? " );  // <-- ********* STORAGE RECEPTACLE **********
            for( Pair<LabVessel,Long> storedVesselData : pkMap.getStoredContainerLabelToVesselMap().values() ) {
                // TODO Register as sample source
                if( storedVesselData.getLeft() instanceof BarcodedTube) {
                    continue;
                }
                stmt.setLong(1, storedVesselData.getRight() );
                ResultSet rs = stmt.executeQuery();
                count += buildMercuryStoredVessels( rs, storedVesselData.getLeft(), pkMap );
            }

        } finally {
            closeResource(stmt);
            closeResource(conn);
        }

        this.processLogWriter.write( "Processed " + count + " sample vessels storage \n");
        this.processLogWriter.flush();

    }

    /**
     * Migrates the root storage locations to Mercury to start hierarchy drill down
     */
    private List<Long> migrateRootStorageLocations( BspMigrationMapping pkMap ) throws Exception {
        // BSP data via old school JDBC access
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
                                             + "   AND sc.parent_strg_container_id IS NULL" );  // <-- ********* ROOTS HAVE NO PARENT **********
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
        // BSP data via old school JDBC access
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
            this.processLogWriter.write( "Processed " + allChildPks.size() + " child storage locations for " + parentPks.size() + " parent locations.\n");
            this.processLogWriter.flush();
            migrateChildStorageLocations( pkMap, allChildPks );
        } else {
            this.processLogWriter.write( "Hit the bottom of the hierarchy and stopping recursion.\n");
            this.processLogWriter.flush();
        }

        return allChildPks;
    }

    /**
     * Create the Mercury StorageLocation objects
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

    private int buildMercuryStoredVessels( ResultSet rs, LabVessel storedContainer, BspMigrationMapping pkMap ) throws Exception {
        int childCount = 0;

        String smId;
        Long tubeId;
        String tubeLabel;
        String tubeType;
        Long rackId;
        String rackLabel;
        Integer row;
        Integer column;

        RackOfTubes rackOfTubes = null;
        StaticPlate staticPlate = null;
        Map<VesselPosition, BarcodedTube> mapPositionToTube = null;

        while ( true ) {
            // ***** Begin column data fetching
            if( !rs.next() ) {
                break;
            }
            smId = rs.getString(1);
            tubeId = rs.getLong(2);
            tubeLabel = rs.getString(3);
            if( rs.wasNull() ) {
                // Create plate well barcode or assign to sm id later
                tubeLabel = null;
            }
            tubeType = rs.getString(4);
            rackId = rs.getLong(5);
            rackLabel = rs.getString(6);
            if( rs.wasNull() ) {
                rackLabel = "CO-" + rackId;
            }
            //String rackType = rs.getString(7);
            row = rs.getInt(8);
            if( rs.wasNull() ) {
                row = null;
            }
            column = rs.getInt(9);
            if( rs.wasNull() ) {
                column = null;
            }
            // ***** End column data fetching

            if( row == null || column == null ) {
                // Log it and carry on, skipping contents
                processLogWriter.write( "No row and/or column in stored container " + rackLabel + ", Sample " + smId + ", ignoring.\n" );
                continue;
            }

            // Different logic for different container types, but a result set applies to only one container
            Pair<Class<? extends LabVessel>,Object> classAndType = pkMap.getMercuryVesselType( tubeType );
            if( storedContainer instanceof RackOfTubes ) {
                // Initialize variables representing rack of tubes
                if( rackOfTubes == null ) {
                    rackOfTubes = (RackOfTubes) storedContainer;
                    mapPositionToTube = new HashMap<>();
                }

                if( classAndType == null || ! classAndType.getLeft().equals(BarcodedTube.class ) ) {
                    // Log it and carry on, skipping contents
                    processLogWriter.write( "Rack of tubes cannot contain other than BarcodedTube, rack " + rackLabel + ", Sample " + smId + ", ignoring.\n" );
                    continue;
                }
                BarcodedTube.BarcodedTubeType barcodedTubeType = (BarcodedTube.BarcodedTubeType) classAndType.getRight();

                VesselGeometry geometry = rackOfTubes.getVesselGeometry();
                VesselPosition position;
                try {
                    position = geometry.getVesselPositions()[(row * geometry.getColumnCount()) + column];
                } catch( ArrayIndexOutOfBoundsException ae ) {
                    String error = "Geometry " + geometry + " for receptacle id " + rackId + ", sample id " + smId + " is inconsistent with row " + row + " and column " + column + ". (index = " + ae.getMessage() + ")\n";
                    processLogWriter.write( error );
                    continue;
                    //throw new RuntimeException( error );
                }

                if( tubeLabel == null ) {
                    tubeLabel = smId;
                }

                LabVessel containedVessel = storageLocationDao.findSingle( LabVessel.class, LabVessel_.label, tubeLabel );
                if( containedVessel == null ) {
                    containedVessel = new BarcodedTube(tubeLabel, barcodedTubeType );
                    storageLocationDao.persist( containedVessel );
                }
                if( ! (containedVessel instanceof BarcodedTube) ) {
                    throw new RuntimeException( "RackOfTubes content in mercury must be a BarcodedTube, label: " + tubeLabel);
                }
                mapPositionToTube.put(position, ( BarcodedTube) containedVessel );
                pkMap.addBspSampleToVesselMap( tubeId, containedVessel);


            } else if ( storedContainer instanceof StaticPlate ) {

                // Initialize variables representing static plate
                if( staticPlate == null ) {
                    staticPlate = (StaticPlate)storedContainer;
                }

                if( classAndType == null || ! classAndType.getLeft().equals(PlateWell.class ) ) {
                    // Log it and carry on, skipping contents
                    processLogWriter.write( "Static plate cannot contain other than plate wells, plate " + rackLabel + ", Sample " + smId + ", ignoring.\n" );
                    continue;
                }

                VesselGeometry geometry = storedContainer.getVesselGeometry();
                VesselPosition position = geometry.getVesselPositions()[ ( row * geometry.getColumnCount() ) + column ];

                PlateWell well = new PlateWell( (StaticPlate)storedContainer, position );
                storageLocationDao.persist( well );
                pkMap.addBspSampleToVesselMap( tubeId, well);
            }

            childCount++;

        } // End of container contents

        // Clean up DB resources
        closeResource(rs);

        // Persist the tube formation
        if( rackOfTubes != null ) {
            TubeFormation tubeFormation = new TubeFormation( mapPositionToTube, rackOfTubes.getRackType() );
            TubeFormation mercuryTubeFormation = storageLocationDao.findSingle( TubeFormation.class, LabVessel_.label, tubeFormation.getLabel() );
            if( mercuryTubeFormation != null ) {
                mercuryTubeFormation.addRackOfTubes(rackOfTubes);
            } else {
                tubeFormation.addRackOfTubes(rackOfTubes);
                storageLocationDao.persist(tubeFormation);
            }
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
                                     + "  from bsp_receptacle r \n"
                                     + "     , bsp_receptacle_type rt \n"
                                     + "     , bsp_location l \n"
                                     + " where rt.receptacle_type_id = r.receptacle_type_id \n"
                                     + "   and r.location_id = l.location_id \n"
                                     + "   and r.location_id is not null " );

            Long bspReceptacleId, bspContainerId;
            String bspReceptacleType, bspLabel;
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
                // Null external_id check (prepend CO- to bspReceptacleId for label)
                bspLabel = rs.getString(4);
                if( rs.wasNull() ) {
                    bspLabel = null;
                }

                // Mercury lab vessel type
                Pair<Class<? extends LabVessel>,Object> classAndType = pkMap.getMercuryVesselType( bspReceptacleType );
                if( classAndType == null ) {
                    processLogWriter.write( "No mercury vessel type found for BSP receptacle type " + bspReceptacleType );
                    processLogWriter.write( "  -->   (bspReceptacleId: " + bspReceptacleId + ")\n" );
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
                    } else {
                        label = "CO-" + bspReceptacleId;
                    }
                    storageVessel = new BarcodedTube( label,(BarcodedTube.BarcodedTubeType) classAndType.getRight() );
                } else if ( vesselSubClass.equals( PlateWell.class ) ) {
                    processLogWriter.write( "Not putting an individual plate well (" + bspReceptacleType + ") into storage "  );
                    processLogWriter.write( "  -->   (bspReceptacleId: " + bspReceptacleId + ")\n" );
                    continue;
                }

                if ( storageVessel == null ) {
                    processLogWriter.write( "Can't determine which type of mercury lab vessel corresponds to " + bspReceptacleType  );
                    processLogWriter.write( "  -->   (bspReceptacleId: " + bspReceptacleId + ")\n" );
                    continue;
                }

                StorageLocation mercuryStorage = pkMap.getLocationByBspId(bspContainerId);
                if( mercuryStorage == null ) {
                    // Damn! Some BSP storage containers are children of archived parents or there are no container_ids in location
                    //throw new RuntimeException("BSP storage location container ID " + bspContainerId + " was not migrated to Mercury.");
                    if( bspContainerId.equals(0L) ) {
                        processLogWriter.write("BSP storage location has no container ID in any of the 4 levels ");
                    } else {
                        processLogWriter.write("BSP storage location container ID " + bspContainerId + " was not migrated to Mercury, is a parent archived? ");
                    }
                    processLogWriter.write( "  -->   (skipping bspReceptacleId: " + bspReceptacleId + ")\n" );
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

                // Vessel attached to entity manager - storage will be persisted
                storageVessel.setStorageLocation(mercuryStorage);

                // We don't do anything with a Mercury static plate other than give it a storage location
                // Migration of wells will be ignored
                if( mercuryVessel != null && vesselSubClass.equals( StaticPlate.class ) ) {
                    processLogWriter.write( "Static plate already in Mercury, ignoring wells "  );
                    processLogWriter.write( "  -->   (bspReceptacleId: " + bspReceptacleId + ", label: " + storageVessel.getLabel() + ")\n" );
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

        processLogWriter.write( "****************\n" );
        processLogWriter.write( "Created " + newCount  + " stored receptacles (containers) in Mercury\n" );
        processLogWriter.write( "Updated location for " + updateCount  + " BSP stored receptacles (containers) already in Mercury\n" );
        processLogWriter.write( "****************\n" );

    }

    private void closeResource( Connection conn ){
        if ( conn != null ) {
            try {  conn.close();  } catch ( Exception e ) {
                processLogWriter.write( "**************** Fail connection close \n" );
                e.printStackTrace(processLogWriter);
            }
        }
    }

    private void closeResource( Statement stmt ){
        if( stmt != null ) {
            try { stmt.close(); } catch ( Exception e ) {
                processLogWriter.write( "**************** Fail statement close \n" );
                e.printStackTrace(processLogWriter);
            }
        }
    }

    private void closeResource( ResultSet rs ){
        if( rs != null ) {
            try { rs.close(); } catch ( Exception e ) {
                processLogWriter.write( "**************** Fail resultset close \n" );
                e.printStackTrace(processLogWriter);
            }
        }
    }
}
