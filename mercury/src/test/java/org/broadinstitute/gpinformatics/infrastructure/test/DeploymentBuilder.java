package org.broadinstitute.gpinformatics.infrastructure.test;

import org.apache.commons.io.FileUtils;
import org.broadinstitute.gpinformatics.infrastructure.common.TestUtils;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.deployment.DeploymentProducer;
import org.broadinstitute.gpinformatics.infrastructure.security.ApplicationInstance;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.importer.ExplodedImporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.MavenDependency;
import org.jboss.shrinkwrap.resolver.api.maven.MavenImporter;
import org.jboss.shrinkwrap.resolver.api.maven.MavenResolutionFilter;

import java.io.File;
import java.util.Collection;

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
        WebArchive war = ShrinkWrap.create(ExplodedImporter.class, MERCURY_WAR)
                .importDirectory("src/main/webapp")
                .as(WebArchive.class)
                .addAsWebInfResource(new File("src/test/resources/" + "mercury-"
                                              + dataSourceEnvironment + "-ds.xml"))
                .addAsWebInfResource(new File("src/test/resources/squid-" + dataSourceEnvironment + "-ds.xml"))
                .addAsWebInfResource(new File("src/test/resources/metrics-prod-ds.xml"))
                .addAsResource(new File("src/main/resources/META-INF/persistence.xml"), "META-INF/persistence.xml")
                .addAsWebInfResource(new File("src/main/webapp/WEB-INF/ejb-jar.xml"))
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
        return war;
    }

    private static WebArchive addWebResourcesTo(WebArchive archive, String directoryName) {
        final File webAppDirectory = new File(directoryName);
        for (File file : FileUtils.listFiles(webAppDirectory, null, true)) {
            if (!file.isDirectory()) {
                archive.addAsResource(file, file.getPath().substring(directoryName.length()));
            }
        }
        return archive;
    }

    public static WebArchive buildMercuryWar() {

        return buildMercuryWar(Deployment.STUBBY);
    }

    public static WebArchive buildMercuryWar(String beansXml) {
        return ShrinkWrap.create(WebArchive.class, MERCURY_WAR)
                .addAsWebInfResource(new StringAsset(beansXml), "beans.xml")
                .merge(buildMercuryWar());
    }

    private static WebArchive buildMercuryWar(String beansXml, Deployment deployment) {
        return ShrinkWrap.create(WebArchive.class, MERCURY_WAR)
                .addAsWebInfResource(new StringAsset(beansXml), "beans.xml")
                .merge(buildMercuryWar(deployment));
    }

    private static WebArchive buildMercuryWar(String beansXml, String dataSourceEnvironment, Deployment deployment) {
        return ShrinkWrap.create(WebArchive.class, MERCURY_WAR)
                .addAsWebInfResource(new StringAsset(beansXml), "beans.xml")
                .merge(buildMercuryWar(deployment, dataSourceEnvironment));
    }

    @SuppressWarnings("UnusedDeclaration")
    public static WebArchive buildMercuryWarWithAlternatives(String... alternatives) {
        StringBuilder sb = new StringBuilder();
        sb.append("<beans>\n")
                .append("  <alternatives>\n");
        for (String alternative : alternatives) {
            sb.append("    <class>").append(alternative).append("</class>\n");
        }
        sb.append("  </alternatives>\n")
                .append("</beans>");
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
        StringBuilder sb = new StringBuilder();
        sb.append("<beans>\n")
                .append("  <alternatives>\n");
        for (Class alternative : alternatives) {
            if (alternative.isAnnotation()) {
                sb.append("    <stereotype>").append(alternative.getName()).append("</stereotype>\n");
            } else {
                sb.append("    <class>").append(alternative.getName()).append("</class>\n");
            }
        }
        sb.append("  </alternatives>\n")
                .append("</beans>");
        return sb.toString();
    }

    @SuppressWarnings("UnusedDeclaration")
    private static JavaArchive addTestHelpers(JavaArchive archive) {
        // TODO: put all test helpers into a single package or two to import all at once
        return archive
                .addClass(ContainerTest.class)
                .addClass(BettaLimsMessageTestFactory.class);
    }

    private static WebArchive addWarDependencies(WebArchive archive) {
        MavenResolutionFilter resolutionFilter = new MavenResolutionFilter() {
            @Override
            public boolean accept(MavenDependency dependency) {
                if (dependency == null) {
                    return false;
                }
                // TODO: remove all test-scoped dependencies; optionally explicitly add certain test dependencies that we commit to supporting
                // TODO: remove exclusion of xerces, which is a workaround until all test-scoped dependencies are removed
                return
//                        !dependency.getScope().equals("test") &&
                        !dependency.getScope().equals("provided") &&
                        !dependency.getCoordinates().contains("xerces");
            }

            @Override
            public MavenResolutionFilter configure(Collection<MavenDependency> dependencies) {
                return this;
            }
        };

//        MavenDependencyResolver resolver = DependencyResolvers.use(MavenDependencyResolver.class).includeDependenciesFromPom("pom.xml");
//        return archive.addAsLibraries(resolver.resolveAsFiles(resolutionFilter);

        return archive
                .as(MavenImporter.class)
                .loadEffectivePom("pom.xml")
                .importAnyDependencies(resolutionFilter)
                .as(WebArchive.class);
    }
}
