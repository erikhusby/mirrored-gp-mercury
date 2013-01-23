package org.broadinstitute.gpinformatics.mercury.control.zims;

import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaRunConfiguration;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaRun;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Date;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.DATABASE_FREE;
import static org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell.FLOWCELL_TYPE.EIGHT_LANE;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author breilly
 */
public class ZimsIlluminaRunFactoryTest {

    private ZimsIlluminaRunFactory zimsIlluminaRunFactory = new ZimsIlluminaRunFactory();

    @BeforeMethod(groups = DATABASE_FREE)
    public void setUp() throws Exception {
    }

    @Test(groups = DATABASE_FREE)
    public void testMakeZimsIlluminaRun() throws Exception {
        IlluminaFlowcell flowcell = new IlluminaFlowcell(EIGHT_LANE, "TestFlowcell", new IlluminaRunConfiguration(76, true));
        Date runDate = new Date(1358889107084L);
        IlluminaSequencingRun sequencingRun = new IlluminaSequencingRun(flowcell, "TestRun", "Run-123", "IlluminaRunServiceImplTest", 101L, true, runDate);
        ZimsIlluminaRun zimsIlluminaRun = zimsIlluminaRunFactory.makeZimsIlluminaRun(sequencingRun);
//        LibraryBeanFactory libraryBeanFactory = new LibraryBeanFactory();
//        ZimsIlluminaRun zimsIlluminaRun = libraryBeanFactory.buildLibraries(sequencingRun);

        assertThat(zimsIlluminaRun.getError(), nullValue());
        assertThat(zimsIlluminaRun.getName(), equalTo("TestRun"));
        assertThat(zimsIlluminaRun.getBarcode(), equalTo("Run-123"));
        assertThat(zimsIlluminaRun.getSequencer(), equalTo("IlluminaRunServiceImplTest"));
        assertThat(zimsIlluminaRun.getFlowcellBarcode(), equalTo("TestFlowcell"));
        assertThat(zimsIlluminaRun.getRunDateString(), equalTo("01/22/2013 16:11"));
//        assertThat(zimsIlluminaRun.getPairedRun(), is(true)); // TODO
//        assertThat(zimsIlluminaRun.getSequencerModel(), equalTo("HiSeq")); // TODO
    }
}
