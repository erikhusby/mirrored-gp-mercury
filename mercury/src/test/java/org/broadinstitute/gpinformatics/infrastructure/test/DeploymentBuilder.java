package org.broadinstitute.gpinformatics.infrastructure.test;

import org.apache.commons.io.FileUtils;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.mercury.test.BettaLimsMessageFactory;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.importer.ExplodedImporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.MavenDependency;
import org.jboss.shrinkwrap.resolver.api.maven.MavenImporter;
import org.jboss.shrinkwrap.resolver.api.maven.MavenResolutionFilter;

//import java.io.File;
import java.io.File;
import java.util.Collection;

/**
 * @author breilly
 */
public class DeploymentBuilder {

    private static final String MERCURY_WAR = "Mercury-Arquillian.war";



    /**
     * Called by default {@link #buildMercuryWar()}, and also useful explicitly in the rare case where you want an
     * in-container test to run as if it's really in another environment (for instance, to isolate a production bug).
     *
     * @param deployment
     * @return
     */
    public static WebArchive buildMercuryWar(Deployment deployment) {
        WebArchive war = ShrinkWrap.create(ExplodedImporter.class, MERCURY_WAR)
                .importDirectory("src/main/webapp")
                .as(WebArchive.class)
                .addAsWebInfResource(new File("src/test/resources/mercury-dev-ds.xml"))
                // todo jmt switch this back to dev
                .addAsWebInfResource(new File("src/test/resources/squid-prod-ds.xml"))
                .addAsResource(new File("src/main/resources/META-INF/persistence.xml"), "META-INF/persistence.xml")
                // TODO PMB
                // TODO MLC this misses infrastucture and athena
                // .addPackages(true, "org.broadinstitute.gpinformatics.mercury")
                // this is yielding weird duplicate definition errors probably due to all our weird duplicate definitions
                .addPackages(true, "org.broadinstitute.gpinformatics")
                .addAsWebInfResource(new StringAsset("MERCURY_DEPLOYMENT=" + deployment.name()), "classes/jndi.properties");
        addWebResourcesTo(war, "src/test/resources/testdata");
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
        WebArchive war = ShrinkWrap.create(WebArchive.class, MERCURY_WAR)
                .addAsWebInfResource(new StringAsset(beansXml), "beans.xml")
                .merge(buildMercuryWar());
        return war;
    }

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
        return buildMercuryWar(sb.toString());
    }

    private static JavaArchive addTestHelpers(JavaArchive archive) {
        // TODO: put all test helpers into a single package or two to import all at once
        return archive
                .addClass(ContainerTest.class)
                .addClass(BettaLimsMessageFactory.class);
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
