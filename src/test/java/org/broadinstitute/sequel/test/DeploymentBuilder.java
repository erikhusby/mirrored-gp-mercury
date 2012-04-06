package org.broadinstitute.sequel.test;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.importer.ExplodedImporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 * @author breilly
 */
public class DeploymentBuilder {

    public static WebArchive buildSequelWar() {
        WebArchive war = ShrinkWrap.create(ExplodedImporter.class, "SequeL.war")
                .importDirectory("src/main/webapp")
                .as(WebArchive.class)
                .merge(importMain(), "WEB-INF/classes");
        return war;
    }

    public static WebArchive buildSequelWar(String beansXml) {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "SequeL.war")
                .addAsWebInfResource(new StringAsset(beansXml), "beans.xml")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "lentils.xml")
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

    private static JavaArchive importMain() {
        return ShrinkWrap.create(ExplodedImporter.class, "SequeL.jar")
                .importDirectory("target/classes")
                .importDirectory("src/main/resources")
                .as(JavaArchive.class);
    }
}
