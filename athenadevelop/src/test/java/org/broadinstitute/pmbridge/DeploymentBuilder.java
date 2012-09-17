package org.broadinstitute.pmbridge;

import org.apache.commons.lang.StringUtils;
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
 * modified hmccrory
 *
 */
public class DeploymentBuilder {

    public static final String DEFAULT_WAR_NAME = "PMBridge-Test.war";

    public static WebArchive createWebArchive() {
        return DeploymentBuilder.createWebArchive(DEFAULT_WAR_NAME);
    }

    public static WebArchive createWebArchive(String name) {
        String warName = ((StringUtils.isBlank(name)) ? DEFAULT_WAR_NAME : name);
        return  ShrinkWrap.create(ExplodedImporter.class, warName)
                .importDirectory("src/main/webapp")
                .as(WebArchive.class);
    }



    public static WebArchive buildBridgePlainWar() {
        WebArchive war = ShrinkWrap.create(ExplodedImporter.class, "PMBridgePlain.war")
                .importDirectory("src/main/webapp")
                .as(WebArchive.class)
                .addPackages(true, "org.broadinstitute.pmbridge", "org.broad.squid");
        war = addWarDependencies(war);
        return war;
    }

    public static WebArchive buildBridgeWar() {
        WebArchive war = ShrinkWrap.create(ExplodedImporter.class, "PMBridge.war")
                .importDirectory("src/main/webapp")
                .as(WebArchive.class)
                .addPackages(true, "org.broadinstitute.pmbridge", "org.broad.squid");
        war = addWarDependencies(war);
        return war;
    }

    public static WebArchive buildBridgeWar(String beansXml) {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "PMBridge.war")
                .addAsWebInfResource(new StringAsset(beansXml), "beans.xml")
                .merge(buildBridgeWar());
        return war;
    }

    public static WebArchive buildBridgeWarWithAlternatives(String... alternatives) {
        StringBuilder sb = new StringBuilder();
        sb.append("<beans>\n")
                .append("  <alternatives>\n");
        for (String alternative : alternatives) {
            sb.append("    <class>").append(alternative).append("</class>\n");
        }
        sb.append("  </alternatives>\n")
                .append("</beans>");
        return buildBridgeWar(sb.toString());
    }

    public static WebArchive buildBridgeWarWithAlternatives(Class... alternatives) {
        StringBuilder sb = new StringBuilder();
        sb.append("<beans>\n")
                .append("  <alternatives>\n");
        for (Class alternative : alternatives) {
            sb.append("    <class>").append(alternative.getName()).append("</class>\n");
        }
        sb.append("  </alternatives>\n")
                .append("</beans>");
        return buildBridgeWar(sb.toString());
    }

    private static JavaArchive importMain() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "PMBridge.jar")
                .addAsDirectory("src/main/resources")
                .addPackages(true, "org.broadinstitute.pmbridge", "org.broad.squid");
        return archive;
    }

    public static WebArchive addWarDependencies(WebArchive archive) {
        return archive
                .as(MavenImporter.class)
                .loadEffectivePom("pom.xml", "JavaEE")
                .importAnyDependencies(new MavenResolutionFilter() {
                    @Override
                    public boolean accept(MavenDependency dependency) {
                        if (dependency == null) {
                            return false;
                        }
                        return !dependency.getScope().equals("provided");
                    }

                    @Override
                    public MavenResolutionFilter configure(Collection<MavenDependency> dependencies) {
                        return this;
                    }
                })
                .as(WebArchive.class);
    }
}
