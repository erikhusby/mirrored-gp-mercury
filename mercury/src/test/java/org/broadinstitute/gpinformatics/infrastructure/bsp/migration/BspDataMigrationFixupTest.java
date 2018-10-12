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

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Migrates BSP storage locations to Mercury <br />
 * Modify BSP connection parameters and path to Oracle JDBC driver file
 *
 * ******************************************************************************************
 * ************************** TRANSACTION TIMEOUT ADJUSTMENT REQUIRED ***********************
 * Wildfly standalone-full.xml:  <coordinator-environment default-timeout="7200"/>
 * ******************************************************************************************
 */
@Test(groups = TestGroups.FIXUP, singleThreaded = true)
public class BspDataMigrationFixupTest extends Arquillian {

    @Inject
    Gplim5728WorkHorseEjb workHorse;

    // Oracle JDBC driver - Modify to suit runtime environment
    private static File oracleDriverFile = new File("C:\\opt\\wildfly-10.1.0.Final\\modules\\system\\layers\\base\\com\\oracle\\main", "ojdbc7-12.1.0.2.jar");

    /* ############# Modify BSP database connection settings (READ ONLY!) as required!!! ####################### */
    private String bspURL = "jdbc:oracle:thin:@curium:1521:gap_dev";
    private String bspUser = "bsp";
    private String bspPassword = "b1nnacl3";

    String logDirName = System.getProperty("jboss.server.log.dir");
    // These files hold the persisted references between BSP and Mercury for each phase to feed into subsequent phases
    // Required for a subsequent phase to start
    // A re-run of a phase will fail if the file already exists
    private String phaseOneMapName = "gplim5728_phase1.dat";
    private PrintWriter processPhaseTwoMap = null;
    private String phaseTwoMapName = "gplim5728_phase2.dat";
    private PrintWriter processPhaseThreeMap = null;
    private String phaseThreeMapName = "gplim5728_phase3.dat";


    // Use (RC, "rc"), (PROD, "prod") to push the backfill to RC and production respectively.
    @Deployment
    public static WebArchive buildMercuryWar() {
        WebArchive webArchive = DeploymentBuilder.buildMercuryWar(DEV, "dev");
        webArchive.addAsLibrary( oracleDriverFile );

        return webArchive;
    }

    @BeforeClass
    public void initialize() throws Exception {
        DriverManager.registerDriver( new oracle.jdbc.OracleDriver() );
    }

    /**
     * Phase 1 of the migration process - freezer locations and vessels/containers directly stored within <br />
     * Builds a tab delimited data file:  stored vessel label (containers mostly), corresponding BSP receptacle id, mercury storage location ID
     * @throws Exception on any error - entire process fails and is rolled back - delete the log file and phase 1 .dat status file (probably empty) before correcting and rerunning
     */
    @Test(enabled = false)
    public void gplim5728PhaseOneMigrateStorage() throws Exception {

        File phaseOneMapFile = new File(logDirName, phaseOneMapName);
        if( phaseOneMapFile.exists() ) {
            throw new IllegalStateException("File from phase 1 exists - will not re-run this phase without user intervention");
        }
        PrintWriter processPhaseOneMap = new PrintWriter( new FileWriter(phaseOneMapFile, false) );

        try {
            Collection<Pair<LabVessel,Long>> pkMap = workHorse.migrateLocsAndStoredVessels(bspURL, bspUser, bspPassword);
            for( Pair<LabVessel,Long> storedVesselData : pkMap ) {
                processPhaseOneMap.write(storedVesselData.getLeft().getLabel());
                processPhaseOneMap.write("\t" );
                processPhaseOneMap.write(storedVesselData.getRight().toString());
                processPhaseOneMap.write("\t" );
                processPhaseOneMap.write(storedVesselData.getLeft().getStorageLocation().getStorageLocationId().toString());
                processPhaseOneMap.write("\n" );
            }
        } finally {
            processPhaseOneMap.close();
        }
    }

    /**
     * Phase 2 of the migration process - contents of vessels/containers stored in phase 1 <br />
     * Reads data from phase 1 output .dat file and breaks it into sequential vessel batches <br/>
     * Creates and attaches contents of stored containers to mercury locations and StorageCheckIn lab events
     * Builds a tab delimited data file with list of three values: rack barcode, tube formation barcode (null if stored container is not a rack), and BSP receptacle ID
     * @throws Exception on any error - only the current batch fails and is rolled back
     *   - Examine the phase 2 <strong>LOG</strong> file and remove the successful rows from the phase 1 <strong>.dat</strong> status file before correcting and rerunning
     */
    @Test(enabled = true)
    public void gplim5728PhaseTwoMigrateTubes() throws Exception {

        System.gc();

        // Need phase 1 map file to exist
        File phaseOneMapFile = new File(logDirName, phaseOneMapName);
        if( !phaseOneMapFile.exists() ) {
            throw new IllegalStateException("Mapping file from phase 1 does not exist - cannot run this phase without mercury vessel label to BSP receptacle ID map");
        }

        // If phase 2 map file exists, the phase is attempting to be re-run and must fail
        File phaseTwoMapFile = new File(logDirName, phaseTwoMapName);
        if( phaseTwoMapFile.exists() ) {
            throw new IllegalStateException("File from phase 2 exists - will not re-run this phase without user intervention");
        }
        PrintWriter processPhaseTwoMap = new PrintWriter( new FileWriter(phaseTwoMapFile, false) );

        // Get mapping values from previous phase
        List<Triple<String,Long,Long>> labelToReceptacleIds = new ArrayList<>();
        LineNumberReader lineReader = new LineNumberReader(new FileReader(phaseOneMapFile));
        while( true ) {
            String line = lineReader.readLine();
            if( line == null || line.length() == 0 ) {
                break;
            }
            String[] values = line.split("\t");
            labelToReceptacleIds.add(Triple.of( values[0], new Long(values[1]), new Long(values[2])));
        }
        lineReader.close();

        try {
            // Roll through list (~40,000), 2500 at a time, hits about 4.5G max heap.
            int fromIndex = 0;
            int toIndex;
            do {
                toIndex = fromIndex + 2500;
                if (toIndex > labelToReceptacleIds.size()) {
                    toIndex = labelToReceptacleIds.size();
                }
                // List of six values: type (sample/rack/plate), barcode, tube formation barcode (null if stored container is not a rack, BSP receptacle ID, sample id, true if sample created in mercury)
                List<String[]> pkMap = workHorse
                        .migrateStoredVesselContents(bspURL, bspUser, bspPassword,
                                labelToReceptacleIds.subList(fromIndex, toIndex));

                // rack barcode, tube formation barcode (null if stored container is not a rack), and BSP receptacle ID
                for (String[] storedVesselData : pkMap) {
                    processPhaseTwoMap.write(storedVesselData[0]);
                    processPhaseTwoMap.write("\t");
                    processPhaseTwoMap.write(storedVesselData[1]);
                    processPhaseTwoMap.write("\t");
                    processPhaseTwoMap.write(storedVesselData[2]);
                    processPhaseTwoMap.write("\t");
                    processPhaseTwoMap.write(storedVesselData[3]);
                    processPhaseTwoMap.write("\t");
                    processPhaseTwoMap.write(storedVesselData[4]);
                    processPhaseTwoMap.write("\t");
                    processPhaseTwoMap.write(storedVesselData[5]);
                    processPhaseTwoMap.write("\n");
                }

                fromIndex = toIndex;

            } while( fromIndex < labelToReceptacleIds.size());

        } finally {
            processPhaseTwoMap.close();
        }
    }
}
