/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2013 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure.bsp.migration;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.storage.StorageLocationDao;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.entity.storage.StorageLocation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.PlateWell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Migrates BSP storage locations to Mercury <br />
 * Modify BSP connection parameters and path to Oracle JDBC driver file
 */
@Test(groups = TestGroups.FIXUP)
public class BspDataMigrationFixupTest extends Arquillian {

    // For BSP data query (READ ONLY!)
    private String bspURL = "jdbc:oracle:thin:@curium:1521:gap_dev";
    private String bspUser = "bsp";
    private String bspPassword = "b1nnacl3";
    // Oracle JDBC driver
    private static File oracleDriverFile = new File("C:\\opt\\wildfly-10.1.0.Final\\modules\\system\\layers\\base\\com\\oracle\\main", "ojdbc7-12.1.0.2.jar")

    Writer processLogWriter = null;

    @Inject
    private UserBean userBean;

    @Inject
    StorageLocationDao storageLocationDao;

    @Inject
    private UserTransaction utx;

    // Use (RC, "rc"), (PROD, "prod") to push the backfill to RC and production respectively.
    @Deployment
    public static WebArchive buildMercuryWar() {
        WebArchive webArchive = DeploymentBuilder.buildMercuryWar(DEV, "dev");

        // Modify path to Oracle JDBC driver as required
        webArchive.addAsLibrary( oracleDriverFile );

        return webArchive;
    }

    /**
     * Used to (initially) load workflow configuration from a file to a global preference
     */
    @Test(enabled = true)
    public void gplim5728MigrateStorage() throws Exception {

        DriverManager.registerDriver( new oracle.jdbc.OracleDriver() );

        // We want a detailed log of all the activity
        String logDirName = System.getProperty("jboss.server.log.dir");
        processLogWriter = new FileWriter(logDirName + File.separator + "gplim5728_fixup.log", true);
        ToStringBuilder.setDefaultStyle(ToStringStyle.SHORT_PREFIX_STYLE);
        processLogWriter.write("======== STARTING GPLIM-5728 FIXUP =============\n");
        processLogWriter.write(SimpleDateFormat.getDateTimeInstance().format(new Date()));
        processLogWriter.write( "\n");

        userBean.loginOSUser();

        try {

            utx.begin();
            // Give it 60 minutes
            utx.setTransactionTimeout( 60 * 60);

            // Migrate storage locations
            BspMigrationMapping pkMap = new BspMigrationMapping();
            List<Long> rootBspPks = migrateRootStorageLocations( pkMap );
            processLogWriter.write("Processed " + rootBspPks.size() + " root storage locations.\n");
            // Recurse into children from parent
            migrateChildStorageLocations( pkMap, rootBspPks );
            processLogWriter.write("======== FINISHED MIGRATING STORAGE LOCATIONS =============\n");

            // Flush the storage locations (clear persistence context too?)
            storageLocationDao.flush();

            // Migrate racks, etc.
            migrateBspReceptacles(pkMap);


            storageLocationDao.persist( new FixupCommentary("GPLIM-5728 Migrate BSP Storage to Mercury") );
            storageLocationDao.flush();

            processLogWriter.write("======== FINISHED GPLIM-5728 FIXUP =============\n");
            processLogWriter.write(SimpleDateFormat.getDateTimeInstance().format(new Date()));
            processLogWriter.close();

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
                try {    } catch ( Exception ignored ){}
            }
        }
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
            rootPks = migrateBspLocations(rs, pkMap);
        } finally {
            // De-allocate
            if( stmt != null ) {
                try { stmt.close(); } catch (Exception ignored) {}
            }
            if ( conn != null ) {
                try {  conn.close();  } catch ( Exception ignored ){}
            }
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
                List<Long> childPks = migrateBspLocations( rs, pkMap );
                // A child can't have 2 parents so no need to worry about non-unique PKs
                allChildPks.addAll( childPks );
            }

        } finally {
            // Clean up DB resources
            if( stmt != null ) {
                try { stmt.close(); } catch ( Exception ignored ) {}
            }
            if ( conn != null ) {
                try {  conn.close();  } catch ( Exception ignored ){}
            }
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
    private List<Long> migrateBspLocations( ResultSet rs, BspMigrationMapping pkMap ) throws Exception {
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

            mercuryLocationType = getMercuryTypeFromBspType( bspStorageType );
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
        try { rs.close(); } catch ( Exception ignored ) {}

        return bspPks;
    }

    /**
     * Get Mercury enum corresponding to BSP type, null if no match and let caller commit suicide on it
     */
    private StorageLocation.LocationType getMercuryTypeFromBspType( String bspStorageType ) {
        if( bspStorageType.startsWith("Freezer") ) {
            return StorageLocation.LocationType.FREEZER;
        }
        if( bspStorageType.startsWith("Refrigerator") ) {
            return StorageLocation.LocationType.REFRIGERATOR;
        }
        if( bspStorageType.startsWith("Side") ) {
            return StorageLocation.LocationType.SECTION;
        }
        return  StorageLocation.LocationType.getByDisplayName(bspStorageType);
    }

    private void migrateBspReceptacles( BspMigrationMapping pkMap ) throws Exception {
        // BSP data via old school JDBC access
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            conn = DriverManager.getConnection(bspURL, bspUser, bspPassword);
            stmt = conn.createStatement();
            rs   = stmt.executeQuery("select r.receptacle_id \n"
                                         + "     , r.location_id \n"
                                         + "     , rt.receptacle_name as receptacle_type \n"
                                         + "     , r.external_id \n"
                                         + "  from bsp_receptacle r \n"
                                         + "     , bsp_receptacle_type rt \n"
                                         + " where rt.receptacle_type_id = r.receptacle_type_id \n"
                                         + "   and r.location_id is not null " );

            Long bspReceptacleId, bspLocationId;
            String bspReceptacleType, bspLabel;
            while ( true ) {
                // BSP data fields
                if( !rs.next() ) {
                    break;
                }
                bspReceptacleId = rs.getLong(1);
                bspLocationId = rs.getLong(2);
                bspReceptacleType = rs.getString(3);
                bspLabel = rs.getString(4);
                if( rs.wasNull() ) {
                    bspLabel = null;
                }

                // Mercury lab vessel type
                Pair<Class<? extends LabVessel>,Object> mercuryVesselDef = getMercuryVesselType( bspReceptacleType );
                if( mercuryVesselDef == null ) {
                    processLogWriter.write( "No mercury vessel type found for BSP receptacle type " + bspReceptacleType );
                    processLogWriter.write( "  -->   (bspReceptacleId: " + bspReceptacleId + ")\n" );
                    continue;
                }

                LabVessel storageVessel = null;
                Class<? extends LabVessel> vesselSubClass = mercuryVesselDef.getLeft();
                if( vesselSubClass.equals( RackOfTubes.class ) ) {
                    String label;
                    if( bspLabel != null ) {
                        label = bspLabel;
                    } else {
                        label = "CO-" + bspReceptacleId;
                    }
                    storageVessel = new RackOfTubes( label, (RackOfTubes.RackType) mercuryVesselDef.getRight() );
                } else if ( vesselSubClass.equals( StaticPlate.class ) ) {
                    String label;
                    if (bspLabel != null) {
                        label = bspLabel;
                    } else {
                        label = "CO-" + bspReceptacleId;
                    }
                    storageVessel = new StaticPlate(label, (StaticPlate.PlateType) mercuryVesselDef.getRight());
                } else if ( vesselSubClass.equals( BarcodedTube.class ) ) {
                    String label;
                    if (bspLabel != null) {
                        label = bspLabel;
                    } else {
                        label = "CO-" + bspReceptacleId;
                    }
                    storageVessel = new BarcodedTube(label, (BarcodedTube.BarcodedTubeType) mercuryVesselDef.getRight());
                } else if ( vesselSubClass.equals( BarcodedTube.class ) ) {
                    String label;
                    if (bspLabel != null) {
                        label = bspLabel;
                    } else {
                        label = "CO-" + bspReceptacleId;
                    }
                    storageVessel = new BarcodedTube(label, (BarcodedTube.BarcodedTubeType) mercuryVesselDef.getRight());
                } else if ( vesselSubClass.equals( PlateWell.class ) ) {
                    processLogWriter.write( "Not putting an individual plate well (" + bspReceptacleType + " into storage "  );
                    processLogWriter.write( "  -->   (bspReceptacleId: " + bspReceptacleId + ")\n" );
                    continue;
                }

                if ( storageVessel == null ) {
                    processLogWriter.write( "Can't determine which type of mercury lab vessel corresponds to " + bspReceptacleType  );
                    processLogWriter.write( "  -->   (bspReceptacleId: " + bspReceptacleId + ")\n" );
                    continue;
                }

            }

        } finally {
            // Clean up DB resources
            if( rs != null ) {
                try { rs.close(); } catch ( Exception ignored ) {}
            }
            if( stmt != null ) {
                try { stmt.close(); } catch ( Exception ignored ) {}
            }
            if ( conn != null ) {
                try {  conn.close();  } catch ( Exception ignored ){}
            }
        }

//        if( allChildPks.size() > 0 ) {
//            this.processLogWriter.write( "Processed " + allChildPks.size() + " child storage locations for " + parentPks.size() + " parent locations.\n");
//            this.processLogWriter.flush();
//            migrateChildStorageLocations( pkMap, allChildPks );
//        } else {
//            this.processLogWriter.write( "Hit the bottom of the hierarchy and stopping recursion.\n");
//            this.processLogWriter.flush();
//        }

    }

    /**
     * Try to find the mercury lab vessel type associated with BSP_RECEPTACLE_TYPE.RECEPTACLE_NAME
     * @return LabVessel subclass and type, null if no match found
     */
    private Pair<Class<? extends LabVessel>,Object> getMercuryVesselType( String bspReceptacleType ) {
        RackOfTubes.RackType rackType = RackOfTubes.RackType.getByDisplayName(bspReceptacleType);
        if( rackType != null ) {
            return Pair.of( RackOfTubes.class, rackType );
        }
        StaticPlate.PlateType plateType = StaticPlate.PlateType.getByDisplayName(bspReceptacleType);
        if( plateType != null ) {
            return Pair.of( StaticPlate.class, plateType );
        }
        BarcodedTube.BarcodedTubeType tubeType = BarcodedTube.BarcodedTubeType.getByDisplayName(bspReceptacleType);
        if( tubeType != null ) {
            return Pair.of( BarcodedTube.class, tubeType );
        }
        PlateWell.WellType wellType = PlateWell.WellType.getByDisplayName(bspReceptacleType);
        if( wellType != null ) {
            return Pair.of( PlateWell.class, wellType );
        }
        // Caller has to deal with nothing found
        return null;
    }

}
