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
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.presentation.hsa.AggregationActionBean;
import org.jboss.arquillian.testng.Arquillian;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;


import static org.mockito.Mockito.mock;

@Test(groups = TestGroups.DATABASE_FREE)
public class FingerprintTaskHandlerTest extends Arquillian {

    private FingerprintTaskHandler fingerprintTaskHandler = new FingerprintTaskHandler();

    @Test
    public void testHandleTask() {
        File bam = new File("src/test/resources/testdata/dragen/SM-IN8EV.bam");
        File vcf = new File("src/test/resources/testdata/dragen/SM-IN8EV.vcf.gz");
        File hapDb = new File(AggregationActionBean.ReferenceGenome.HG38.getHaplotypeDatabase());

        FingerprintTask fp = new FingerprintTask(bam, vcf, hapDb, "SM-IN8EV", null);
        FingerprintUploadTask fpUploadTask = new FingerprintUploadTask();

        MercurySample mercurySample  = new MercurySample("SM-IN8EV", new BspSampleData(
                ImmutableMap.of(BSPSampleSearchColumn.GENDER, "Male")));

        FiniteStateMachine fsm = new FiniteStateMachine();
        FingerprintState fpState = new FingerprintState("FPMe", mercurySample, fsm, null);

        fpState.addTask(fp);
        fpState.addExitTask(fpUploadTask);

        FingerprintBean fingerprintBean = fingerprintTaskHandler.handleTaskDaoFree(fp, mercurySample);

        Assert.assertNotNull(fingerprintBean);

    }
}