package org.broadinstitute.gpinformatics.mercury.boundary.run;

import org.apache.commons.io.FileUtils;
import org.apache.poi.util.IOUtils;
import org.broadinstitute.gpinformatics.infrastructure.deployment.InfiniumStarterConfig;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VarioskanParserTest;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselGeometry;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test Infinium Run Processor
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class InfiniumRunProcesserTest {

    private String testChipBarcode = "TestInfiniumChipBarcode";
    private File tmpDir;
    private File runDir;
    private InfiniumRunProcessor infiniumRunProcessor;
    private StaticPlate testChip;

    @BeforeMethod
    public void setUp() throws Exception {
        String tmpDirPath = System.getProperty("java.io.tmpdir");
        tmpDir = new File(tmpDirPath);
        runDir = new File(tmpDir, testChipBarcode);
        runDir.mkdir();
        InfiniumStarterConfig starterConfig = mock(InfiniumStarterConfig.class);
        when(starterConfig.getDataPath()).thenReturn(tmpDirPath);
        infiniumRunProcessor = new InfiniumRunProcessor(starterConfig);
        testChip = mock(StaticPlate.class);
        when(testChip.getLabel()).thenReturn(testChipBarcode);
        when(testChip.getVesselGeometry()).thenReturn(VesselGeometry.INFINIUM_24_CHIP);
        VesselContainer vesselContainer = mock(VesselContainer.class);
        when(testChip.getContainerRole()).thenReturn(vesselContainer);
        SampleInstanceV2 sampleInstanceV2 = mock(SampleInstanceV2.class);
        Set<SampleInstanceV2> sampleInstanceV2s = new HashSet<>();
        sampleInstanceV2s.add(sampleInstanceV2);
        when(vesselContainer.getSampleInstancesAtPositionV2(any(VesselPosition.class))).thenReturn(sampleInstanceV2s);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        if (runDir != null && runDir.exists()) {
            FileUtils.deleteDirectory(runDir);
        }
    }

    @Test
    public void testXmlFilesNotCreated() throws Exception {
        createTestFolderStructure(testChipBarcode, VesselGeometry.INFINIUM_24_CHIP, false, true);
        InfiniumRunProcessor.ChipWellResults process = infiniumRunProcessor.process(testChip);
        Assert.assertEquals(process.isHasRunStarted(), false);

        createTestFolderStructure(testChipBarcode, VesselGeometry.INFINIUM_24_CHIP, true, false);
        process = infiniumRunProcessor.process(testChip);
        Assert.assertEquals(process.isHasRunStarted(), true);
    }

    private void createTestFolderStructure(String barcode, VesselGeometry vesselGeometry, boolean generateXml,
                                          boolean generateIdats) throws Exception {
        for (VesselPosition vesselPosition: vesselGeometry.getVesselPositions()) {
            if (generateXml) {
                String xml = String.format("%s_%s_1_Red.xml", barcode, vesselPosition.name());
                File fXml = new File(runDir, xml);
                fXml.createNewFile();

                InputStream xmlFile = VarioskanParserTest.getSpreadsheet(InfiniumRunResourceContainerTest.XML_FILE);
                OutputStream outputStream = new FileOutputStream(fXml);
                IOUtils.copy(xmlFile, outputStream);
            }
            if (generateIdats) {
                String red = String.format("%s_%s_Red.idat", barcode, vesselPosition.name());
                String green = String.format("%s_%s_Grn.idat", barcode, vesselPosition.name());
                File fRed = new File(runDir, red);
                fRed.createNewFile();

                File fGreen = new File(runDir, green);
                fGreen.createNewFile();
            }
        }
    }
}
