package org.broadinstitute.sequel.test;

import org.broadinstitute.sequel.BettaLimsMessageFactory;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.importer.ExplodedImporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.MavenDependency;
import org.jboss.shrinkwrap.resolver.api.maven.MavenImporter;
import org.jboss.shrinkwrap.resolver.api.maven.MavenResolutionFilter;

import java.util.Collection;

/**
 * @author breilly
 */
public class DeploymentBuilder {

    public static WebArchive buildSequelWar() {
        WebArchive war = ShrinkWrap.create(ExplodedImporter.class, "SequeL.war")
                .importDirectory("src/main/webapp")
                .as(WebArchive.class)
                .merge(importMain(), "WEB-INF/classes");
        war = addWarDependencies(war);
        return war;
    }

    public static WebArchive buildSequelWar(String beansXml) {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "SequeL.war")
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
            sb.append("    <class>").append(alternative.getName()).append("</class>\n");
        }
        sb.append("  </alternatives>\n")
                .append("</beans>");
        return buildSequelWar(sb.toString());
    }

    private static JavaArchive importMain() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "SequeL.jar")
                .addAsDirectory("src/main/resources")
                .addPackages(true, "org.broadinstitute.sequel");
        return archive;
    }

    private static JavaArchive addTestHelpers(JavaArchive archive) {
        // TODO: put all test helpers into a single package or two to import all at once
        return archive
                .addClass(ContainerTest.class)
                .addClass(BettaLimsMessageFactory.class);
    }

    private static WebArchive addWarDependencies(WebArchive archive) {
        return archive
                .as(MavenImporter.class)
                .loadEffectivePom("pom.xml", "JavaEE")
                .importAnyDependencies(new MavenResolutionFilter() {
                    @Override
                    public boolean accept(MavenDependency dependency) {
                        if (dependency == null) {
                            return false;
                        }
                        // TODO: remove all test-scoped dependencies; optionally explicitly add certain test dependencies that we commit to supporting
                        // TODO: remove exclusion of xerces, which is a workaround until all test-scoped dependencies are removed
                        return
//                                !dependency.getScope().equals("test") &&
                                !dependency.getScope().equals("provided")
                                && !dependency.getCoordinates().contains("xerces");
                    }

                    @Override
                    public MavenResolutionFilter configure(Collection<MavenDependency> dependencies) {
                        return this;
                    }
                })
                .as(WebArchive.class);
    }
}
