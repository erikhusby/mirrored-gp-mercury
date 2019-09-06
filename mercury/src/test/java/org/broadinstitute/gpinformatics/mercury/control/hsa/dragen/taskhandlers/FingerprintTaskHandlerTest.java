package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.taskhandlers;

import com.google.common.collect.ImmutableMap;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.run.FingerprintBean;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.FingerprintTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.FingerprintUploadTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.FingerprintState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.FiniteStateMachine;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Status;
import org.broadinstitute.gpinformatics.mercury.control.run.ConcordanceCalculator;
import org.broadinstitute.gpinformatics.mercury.entity.run.Snp;
import org.broadinstitute.gpinformatics.mercury.entity.run.SnpList;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.presentation.hsa.AlignmentActionBean;
import org.jboss.arquillian.testng.Arquillian;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Map;


import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

@Test(groups = TestGroups.DATABASE_FREE)
public class FingerprintTaskHandlerTest extends Arquillian {

    private FingerprintTaskHandler fingerprintTaskHandler = new FingerprintTaskHandler();

    @Test
    public void testHandleTask() {
        File bam = new File("src/test/resources/testdata/dragen/SM-IN8EV.bam");
        File vcf = new File("src/test/resources/testdata/dragen/SM-IN8EV.vcf.gz");
        File hapDb = new File(AlignmentActionBean.ReferenceGenome.HG38.getHaplotypeDatabase());

        FingerprintTask fp = new FingerprintTask(bam, vcf, hapDb, "SM-IN8EV", null);
        FingerprintUploadTask fpUploadTask = new FingerprintUploadTask();

        MercurySample mercurySample  = new MercurySample("SM-IN8EV", new BspSampleData(
                ImmutableMap.of(BSPSampleSearchColumn.GENDER, "Male")));

        FiniteStateMachine fsm = new FiniteStateMachine();
        FingerprintState fpState = new FingerprintState("FPMe", mercurySample, fsm);

        fpState.addTask(fp);
        fpState.addExitTask(fpUploadTask);

        SnpList snpList = mock(SnpList.class);
        Map rsidMap = mock(Map.class);

        when(snpList.getMapRsIdToSnp()).thenReturn(rsidMap);
        when(rsidMap.get(anyString())).thenReturn(null);

        FingerprintBean fingerprintBean = fingerprintTaskHandler.handleTaskDaoFree(fp, mercurySample, snpList);

        Assert.assertNotNull(fingerprintBean);

    }
}