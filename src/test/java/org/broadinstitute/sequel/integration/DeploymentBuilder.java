package org.broadinstitute.sequel.integration;

import org.broadinstitute.sequel.test.BettaLimsMessageFactory;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.importer.ExplodedImporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.DependencyResolvers;
import org.jboss.shrinkwrap.resolver.api.maven.MavenDependency;
import org.jboss.shrinkwrap.resolver.api.maven.MavenDependencyResolver;
import org.jboss.shrinkwrap.resolver.api.maven.MavenImporter;
import org.jboss.shrinkwrap.resolver.api.maven.MavenResolutionFilter;

import java.util.Collection;

/**
 * @author breilly
 */
public class DeploymentBuilder {

    private static final String SEQUEL_WAR = "SequeL-Arquillian.war";

    public static WebArchive buildSequelWar() {
        WebArchive war = ShrinkWrap.create(ExplodedImporter.class, SEQUEL_WAR)
                .importDirectory("src/main/webapp")
                .as(WebArchive.class)
                .addPackages(true, "org.broadinstitute.sequel");
        war = addWarDependencies(war);
        return war;
    }

    public static WebArchive buildSequelWar(String beansXml) {
        WebArchive war = ShrinkWrap.create(WebArchive.class, SEQUEL_WAR)
                .addAsWebInfResource(new StringAsset(beansXml), "beans.xml")
                .merge(buildSequelWar());
        return war;
    }

    public static WebArchive buildSequelWarWithAlternatives(String... alternatives) {
        StringBuilder sb = new StringBuilder();
        sb.append("<beans>\n")
                .append("  <alternatives>\n");
        for (String alternative : alternatives) {
            sb.append("    <class>").append(alternative).append("</class>\n");
        }
        sb.append("  </alternatives>\n")
                .append("</beans>");
        return buildSequelWar(sb.toString());
    }

    public static WebArchive buildSequelWarWithAlternatives(Class... alternatives) {
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
        return buildSequelWar(sb.toString());
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
