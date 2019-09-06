package org.broadinstitute.gpinformatics.mercury.control.hsa;

import org.apache.commons.io.IOUtils;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.IlluminaSequencingRunDao;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VarioskanParserTest;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;

import java.io.IOException;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.testng.Assert.*;

@Test(groups = TestGroups.STANDARD)
public class SampleSheetBuilderTest extends Arquillian {

    @Inject
    private SampleSheetBuilder sampleSheetBuilder;

    @Inject
    private IlluminaSequencingRunDao illuminaSequencingRunDao;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    /**
     * This test reads its parameters from a file, mercury/src/test/resources/testdata/CreateSampleSheet.txt, so it can
     * be used for other similar runs, without writing a new test.  Example contents of the file are:
     * 190813_SL-NVK_0203_AHKLWVDSXX
     */
    @Test(enabled = true)
    public void testMakeSampleSheet() throws IOException {
        List<String> lines = IOUtils.readLines(VarioskanParserTest.getTestResource("CreateSampleSheet.txt"));
        for (String runName: lines) {
            IlluminaSequencingRun illuminaSequencingRun = illuminaSequencingRunDao.findByRunName(runName);
            if (illuminaSequencingRun != null) {
                SampleSheetBuilder.SampleSheet sampleSheet = sampleSheetBuilder.makeSampleSheet(illuminaSequencingRun);
                String csv = sampleSheet.toCsv();
                System.out.println(csv);
            }
        }
    }
}