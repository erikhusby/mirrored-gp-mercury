package org.broadinstitute.gpinformatics.infrastructure.test;

import org.apache.commons.io.FileUtils;
import org.broadinstitute.gpinformatics.infrastructure.common.TestUtils;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.deployment.DeploymentProducer;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.importer.ExplodedImporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.MavenResolvedArtifact;
import org.jboss.shrinkwrap.resolver.api.maven.ScopeType;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * @author breilly
 */
public class DeploymentBuilder {

    public static final String MERCURY_APP_NAME = "Mercury-Arquillian";

    private static final String MERCURY_WAR = MERCURY_APP_NAME + ".war";

    /**
     * Called by default {@link #buildMercuryWar()}, and also useful explicitly in the rare case where you want an
     * in-container test to run as if it's really in another environment (for instance, to isolate a production bug).
     */
    public static WebArchive buildMercuryWar(Deployment deployment) {
        return buildMercuryWar(deployment, "dev");
    }

    /**
     * Allows caller to specify environments for remote systems, and for the database
     *
     * @param deployment            maps to settings in mercury-config.yaml
     * @param dataSourceEnvironment which datasources to use: dev, qa or prod
     *
     * @return war
     */
    public static WebArchive buildMercuryWar(Deployment deployment, String dataSourceEnvironment) {

        // Look for the mercury data source in two places, prefering the target/test-classes over src/test/resources
        // A feature branch build needs to filter the mercury datasource from src/test/resources-fb to target/test-classes
        // And if the build is being done with clover, it will be in target/clover/test-classes. If not there then
        // use the one in src/test/resources
        File mercuryDS = new File("target/test-classes/" + "mercury-"
                + dataSourceEnvironment + "-ds.xml");
        if (!mercuryDS.exists()) {
            mercuryDS = new File("target/clover/test-classes/" + "mercury-" + dataSourceEnvironment + "-ds.xml");
            if (!mercuryDS.exists()) {
                mercuryDS = new File("src/test/resources/" + "mercury-" + dataSourceEnvironment + "-ds.xml");
            }
        }
        WebArchive war = ShrinkWrap.create(ExplodedImporter.class, MERCURY_WAR)
                .importDirectory("src/main/webapp")
                .as(WebArchive.class)
                .addAsWebInfResource(mercuryDS)
                .addAsWebInfResource(new File("src/test/resources/squid-" + dataSourceEnvironment + "-ds.xml"))
                .addAsWebInfResource(new File("src/test/resources/metrics-" + dataSourceEnvironment + "-ds.xml"))
                .addAsWebInfResource(new File("src/test/resources/analytics-" + dataSourceEnvironment + "-ds.xml"))
                .addAsResource(new File("src/main/resources/META-INF/persistence.xml"), "META-INF/persistence.xml")
                .addAsResource(new File("src/main/resources/META-INF/beans.xml"), "META-INF/beans.xml")
                .addAsWebInfResource(new File("src/main/webapp/WEB-INF/ejb-jar.xml"))
                .addAsWebInfResource(new File("src/main/webapp/WEB-INF/jboss-deployment-structure.xml"))
                //TODO  Cherry Picking resources is not Ideal.  When we have more auto front end tests, we will need everything in resources.
                .addAsResource(new File("src/main/resources/WorkflowConfig.xml"), "WorkflowConfig.xml")
                .addAsResource(new File("src/main/resources/templates/WorkflowValidation.ftl"),
                        "templates/WorkflowValidation.ftl")
                .addPackages(true, "org.broadinstitute.gpinformatics")
                .addPackages(true, "edu.mit.broad.prodinfo.bean.generated")
                .addAsWebInfResource(new StringAsset(DeploymentProducer.MERCURY_DEPLOYMENT + "=" + deployment.name()),
                        "classes/jndi.properties");
        addWebResourcesTo(war, TestUtils.TEST_DATA_LOCATION);
        war = addWarDependencies(war);
        /*
         *** Capture contents of war to troubleshoot dependencies
        try {
            war.writeTo(new FileOutputStream("target/Mercury-Arquillian.war.files.txt"), Formatters.VERBOSE);
        } catch (Exception e) {
            e.printStackTrace();
        }*/
        return war;
    }


    private static WebArchive addWebResourcesTo(WebArchive archive, String directoryName) {
        final File webAppDirectory = new File(directoryName);
        for (File file : FileUtils.listFiles(webAppDirectory, null, true)) {
            if (!file.isDirectory()) {
                // Replace backslashes with forward slashes to avoid "file not found" error when deploying on Windows
                archive.addAsResource(file, file.getPath().replace('\\', '/').substring(directoryName.length()));
            }
        }
        return archive;
    }

    public static WebArchive buildMercuryWar() {
        return buildMercuryWar(Deployment.STUBBY);
    }

    /**
     * Replace the default beans.xml file as deployed with alternative beans for testing
     * @param beansXml Contents of alternative beans.xml file
     * @return WebArchive with alternative beans.xml file substituted
     */
    public static WebArchive buildMercuryWar(String beansXml) {
        WebArchive war = buildMercuryWar();
        war.addAsResource(new StringAsset(beansXml), "META-INF/beans.xml");
        return war;
    }

    /**
     * @see DeploymentBuilder#buildMercuryWar(String)
     */
    private static WebArchive buildMercuryWar(String beansXml, Deployment deployment) {
        WebArchive war = buildMercuryWar(deployment);
        war.addAsResource(new StringAsset(beansXml), "META-INF/beans.xml");
        return war;
    }

    /**
     * @see DeploymentBuilder#buildMercuryWar(String)
     */
    private static WebArchive buildMercuryWar(String beansXml, String dataSourceEnvironment, Deployment deployment) {
        WebArchive war = buildMercuryWar(deployment, dataSourceEnvironment);
        war.addAsResource(new StringAsset(beansXml), "META-INF/beans.xml");
        return war;
    }

    public static WebArchive buildMercuryWarWithAlternatives(String... alternatives) {
        StringBuilder sbAlts = new StringBuilder();
        sbAlts.append("  <alternatives>\n");
        for (String alternative : alternatives) {
           sbAlts.append("    <class>").append(alternative).append("</class>\n");
        }
        sbAlts.append("  </alternatives>\n");

        // Small enough, dump it in a String
        StringBuilder sb = new StringBuilder();
        try {
            sb.append(new String(Files.readAllBytes(FileSystems.getDefault().getPath("./src/main/resources/META-INF/beans.xml"))));
            sb.insert(sb.indexOf("</beans>"), sbAlts.toString());
        } catch ( Exception ex ) {
            throw new RuntimeException("Fail to read beans.xml template file: " + ex.getMessage() );
        }

        return buildMercuryWar(sb.toString());
    }

    public static WebArchive buildMercuryWarWithAlternatives(Class... alternatives) {
        return buildMercuryWarWithAlternatives(null, null, alternatives);
    }

    public static WebArchive buildMercuryWarWithAlternatives(Deployment deployment, Class... alternatives) {
        return buildMercuryWarWithAlternatives(deployment, null, alternatives);
    }

    /**
     * Uses the alternative data source e.g. "prod" or "dev", and adds the @Alternative classes.
     */
    public static WebArchive buildMercuryWarWithAlternatives(Deployment deployment,
                                                             String dataSourceEnvironment,
                                                             Class... alternatives) {
        if (deployment == null && dataSourceEnvironment != null) {
            throw new UnsupportedOperationException(
                    "Must specify a deployment when specifying a dataSourceEnvironment");
        }

        String beansXml = buildBeansXml(alternatives);

        if (deployment == null) {
            return buildMercuryWar(beansXml);
        } else if (dataSourceEnvironment == null) {
            return buildMercuryWar(beansXml, deployment);
        } else {
            return buildMercuryWar(beansXml, dataSourceEnvironment, deployment);
        }
    }

    /**
     * Generates the contents for beans.xml with the given alternatives. An alternative that is an annotation will be
     * included as a &lt;stereotype&gt; while any other type will be a &lt;class&gt;.
     *
     * @param alternatives the alternatives and stereotypes to include in the beans.xml content
     * @return the string contents for a beans.xml file
     */
    public static String buildBeansXml(Class... alternatives) {
        StringBuilder sbAlts = new StringBuilder();
        sbAlts.append("  <alternatives>\n");
        for (Class alternative : alternatives) {
            if (alternative.isAnnotation()) {
                sbAlts.append("    <stereotype>").append(alternative.getName()).append("</stereotype>\n");
            } else {
                sbAlts.append("    <class>").append(alternative.getName()).append("</class>\n");
            }
        }
        sbAlts.append("  </alternatives>\n");

        // Small enough, dump it in a String
        StringBuilder sb = new StringBuilder();
        try {
            sb.append(new String(Files.readAllBytes(Paths.get("./src/main/resources/META-INF/beans.xml"))));
            sb.insert(sb.indexOf("</beans>"), sbAlts.toString());
        } catch ( Exception ex ) {
            throw new RuntimeException("Fail to read beans.xml template file: " + ex.getMessage() );
        }

        return sb.toString();
    }

    @SuppressWarnings("UnusedDeclaration")
    private static JavaArchive addTestHelpers(JavaArchive archive) {
        // TODO: put all test helpers into a single package or two to import all at once
        return archive.addClass(BettaLimsMessageTestFactory.class);
    }

    /**
     * Import Maven dependencies to WEB-INF/lib folder
     * @param archive The archive to build out with dependencies
     * @return Same archive instance with added dependencies (why not use by reference and return void?)
     */
    private static WebArchive addWarDependencies(WebArchive archive) {

        // Import Maven runtime dependencies
        List<File> artifacts = new ArrayList<>();

        for (MavenResolvedArtifact artifact : Maven.resolver().loadPomFromFile("pom.xml")
                .importDependencies(ScopeType.IMPORT, ScopeType.RUNTIME, ScopeType.TEST, ScopeType.COMPILE )
                .resolve().withTransitivity().asResolvedArtifact()) {
            // This is some old stuff I had to pull up to use new API and be consistent
            // TODO: remove all test-scoped dependencies; optionally explicitly add certain test dependencies that we commit to supporting
            // TODO: remove exclusion of xerces, which is a workaround until all test-scoped dependencies are removed
            // TODO: remove exclusion of dom4j, WildFly problem with an older release in it's runtime classpath
            if( artifact.getExtension().equals("jar")
                    && !artifact.getCoordinate().getArtifactId().contains("xerces")
                    // Pulled in with another dependency
                    && !artifact.getCoordinate().getArtifactId().contains("dom4j") ) {
                artifacts.add(artifact.asFile());
            }
        }
        return archive.addAsLibraries(artifacts.toArray(new File[0] ));
    }
}
