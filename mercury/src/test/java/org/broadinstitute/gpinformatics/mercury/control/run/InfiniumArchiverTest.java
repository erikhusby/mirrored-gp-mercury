package org.broadinstitute.gpinformatics.mercury.control.run;

import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.gpinformatics.infrastructure.deployment.InfiniumStarterConfig;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.easymock.EasyMock;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.mockito.Mockito;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;


/**
 * Test archiving
 */
@Test(groups = TestGroups.STUBBY)
public class InfiniumArchiverTest extends Arquillian {

    @Inject
    private InfiniumArchiver infiniumArchiver;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    public void testX() {
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.add(Calendar.DAY_OF_YEAR, -20);
        List<Pair<String, Boolean>> chipsToArchive = infiniumArchiver.findChipsToArchive(50,
                gregorianCalendar.getTime());
        for (Pair<String, Boolean> stringBooleanPair : chipsToArchive) {
            if (stringBooleanPair.getRight()) {
                System.out.println(stringBooleanPair);
            }
        }
    }

    public void testY() {
        try {
            InfiniumStarterConfig mock = Mockito.mock(InfiniumStarterConfig.class);
            Path archive = Files.createTempDirectory("archive");
            Mockito.when(mock.getDataPath()).thenReturn(dataPath);
            String decodeDataPath;
            Mockito.when(mock.getDecodeDataPath()).thenReturn(decodeDataPath);
            String archivePath;
            Mockito.when(mock.getArchivePath()).thenReturn(archivePath);
            infiniumArchiver.archiveChip("x", mock);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}