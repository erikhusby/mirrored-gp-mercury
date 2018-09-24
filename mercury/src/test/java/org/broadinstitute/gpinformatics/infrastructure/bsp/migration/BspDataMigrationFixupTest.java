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

import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.File;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Migrates BSP storage locations to Mercury <br />
 * Modify BSP connection parameters and path to Oracle JDBC driver file
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

    // Use (RC, "rc"), (PROD, "prod") to push the backfill to RC and production respectively.
    @Deployment
    public static WebArchive buildMercuryWar() {
        WebArchive webArchive = DeploymentBuilder.buildMercuryWar(DEV, "dev");
        webArchive.addAsLibrary( oracleDriverFile );

        return webArchive;
    }

    @Test(enabled = true)
    public void gplim5728MigrateStorage() throws Exception {
        workHorse.justDoIt( bspURL, bspUser, bspPassword );
    }

}
