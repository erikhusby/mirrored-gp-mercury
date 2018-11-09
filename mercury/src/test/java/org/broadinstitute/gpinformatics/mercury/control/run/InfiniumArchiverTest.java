package org.broadinstitute.gpinformatics.mercury.control.run;

import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.gpinformatics.infrastructure.deployment.InfiniumStarterConfig;
import org.broadinstitute.gpinformatics.infrastructure.test.StubbyContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


/**
 * Test archiving.
 */
@Test(groups = TestGroups.STUBBY)
@Dependent
public class InfiniumArchiverTest extends StubbyContainerTest {

    public InfiniumArchiverTest(){}

    @Inject
    private InfiniumArchiver infiniumArchiver;

    public void testQuery() {
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        // Archive interval is 10 days, so we should find something at 5 days.
        gregorianCalendar.add(Calendar.DAY_OF_YEAR, -5);
        List<Pair<String, Boolean>> chipsToArchive = infiniumArchiver.findChipsToArchive(50,
                gregorianCalendar.getTime());
        Assert.assertTrue(!chipsToArchive.isEmpty());
    }

    public void testZip() {
        try {
            Path infiniumTest = Files.createTempDirectory("infiniumTest");

            File data = new File(infiniumTest.toFile(), "data");
            Assert.assertTrue(data.mkdir());
            String barcode = "x";
            File chipDataDir = new File(data, barcode);
            Assert.assertTrue(chipDataDir.mkdir());
            String position = "R01C01";
            File jpg = new File(chipDataDir, barcode + "_" + position + "_Red.jpg");
            Assert.assertTrue(jpg.createNewFile());
            String idatName = barcode + "_" + position + "_Red.idat";
            File idat = new File(chipDataDir, idatName);
            Assert.assertTrue(idat.createNewFile());
            List<String> expectedNames = new ArrayList<>();
            expectedNames.add(idatName);

            File decodeData = new File(infiniumTest.toFile(), "decodeData");
            Assert.assertTrue(decodeData.mkdir());
            File chipDecodeData = new File(decodeData, barcode);
            Assert.assertTrue(chipDecodeData.mkdir());
            String dmapName = barcode + "_" + position + "_1.dmap.gz";
            File dmap = new File(chipDecodeData, dmapName);
            Assert.assertTrue(dmap.createNewFile());
            expectedNames.add(dmapName);

            File archive = new File(infiniumTest.toFile(), "archive");
            Assert.assertTrue(archive.mkdir());

            InfiniumStarterConfig mock = Mockito.mock(InfiniumStarterConfig.class);
            Mockito.when(mock.getDataPath()).thenReturn(data.getAbsolutePath());
            Mockito.when(mock.getDecodeDataPath()).thenReturn(decodeData.getAbsolutePath());
            Mockito.when(mock.getArchivePath()).thenReturn(archive.getAbsolutePath());

            InfiniumArchiver.archiveChip(barcode, mock);

            File zipFile = new File(archive, barcode + ".zip");
            ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFile));
            ZipEntry nextEntry;
            int found = 0;
            while((nextEntry = zipInputStream.getNextEntry()) != null) {
                for (String expectedName : expectedNames) {
                    if (nextEntry.getName().endsWith(expectedName)) {
                        found++;
                    }
                }
            }
           Assert.assertEquals(found, expectedNames.size());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}