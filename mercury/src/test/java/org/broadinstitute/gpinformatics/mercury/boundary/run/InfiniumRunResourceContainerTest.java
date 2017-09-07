package org.broadinstitute.gpinformatics.mercury.boundary.run;

import org.apache.commons.io.FileUtils;
import org.apache.poi.util.IOUtils;
import org.broadinstitute.gpinformatics.athena.entity.products.GenotypingProductOrderMapping;
import org.broadinstitute.gpinformatics.infrastructure.deployment.InfiniumStarterConfig;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.AttributeArchetypeDao;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VarioskanParserTest;
import org.broadinstitute.gpinformatics.mercury.entity.run.ArchetypeAttribute;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/*
 * Database test for the Infinium Run Resource
 */
@Test(groups = TestGroups.STANDARD)
public class InfiniumRunResourceContainerTest extends Arquillian {

    public static final String XML_FILE = "3999582166_R01C01_1_Red.xml";

    @Inject
    private AttributeArchetypeDao attributeArchetypeDao;

    @Inject
    private InfiniumRunResource infiniumRunResource;
    private File runDir;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        if (runDir != null && runDir.exists()) {
            FileUtils.deleteDirectory(runDir);
        }
    }

    @Test
    public void testScannerName() throws Exception {
        String tmpDirPath = System.getProperty("java.io.tmpdir");
        File tmpDir = new File(tmpDirPath);
        runDir = new File(tmpDir, "3999582166");
        runDir.mkdir();
        File tempFile = new File(runDir, "3999582166_R01C01_1_Red.xml");
        InputStream xmlFile = VarioskanParserTest.getSpreadsheet(XML_FILE);
        OutputStream outputStream = new FileOutputStream(tempFile);
        IOUtils.copy(xmlFile, outputStream);
        outputStream.close();
        InfiniumStarterConfig config = mock(InfiniumStarterConfig.class);
        when(config.getDataPath()).thenReturn(tmpDir.getPath());
        infiniumRunResource.setInfiniumStarterConfig(config);
        String chipBarcode = "3999582166_R01C01";
        InfiniumRunBean run = infiniumRunResource.getRun(chipBarcode);
        Assert.assertEquals(run.getScannerName(), "Big Bad Wolf");
    }

    @Test
    public void testPdoOverrideDefaultChipValue() throws Exception {
        GenotypingProductOrderMapping mockGenotypingProductOrderMapping1 = mock(GenotypingProductOrderMapping.class);
        final ArchetypeAttribute archetypeAttribute1 = mock(ArchetypeAttribute.class);
        final ArchetypeAttribute archetypeAttribute2 = mock(ArchetypeAttribute.class);

        Set<ArchetypeAttribute> archetypeAttributes = new HashSet<ArchetypeAttribute>() {{
            add(archetypeAttribute1); add(archetypeAttribute2);
        }};

        when(mockGenotypingProductOrderMapping1.getAttributes()).thenReturn(archetypeAttributes);

        when(archetypeAttribute1.getAttributeName()).thenReturn("zcall_threshold_unix");
        when(archetypeAttribute1.getAttributeValue()).thenReturn("zcalloverride");

        when(archetypeAttribute2.getAttributeName()).thenReturn("cluster_location_unix");
        when(archetypeAttribute2.getAttributeValue()).thenReturn("clusteroverride");

        AttributeArchetypeDao spyAttributeArchetypeDao = spy(attributeArchetypeDao);
        doReturn(mockGenotypingProductOrderMapping1)
                .when(spyAttributeArchetypeDao).findGenotypingProductOrderMapping(anyLong());
        infiniumRunResource.setAttributeArchetypeDao(spyAttributeArchetypeDao);
        InfiniumRunBean run = infiniumRunResource.getRun("200724570010_R01C01");
        Assert.assertEquals(run.getClusterFilePath(), "clusteroverride");
        Assert.assertEquals(run.getzCallThresholdsPath(), "zcalloverride");
    }
}
