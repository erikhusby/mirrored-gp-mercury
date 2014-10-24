package org.broadinstitute.gpinformatics.mercury.control.dao.sample;

import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@Test(groups = TestGroups.STANDARD)
public class MercurySampleDaoTest extends ContainerTest {

    @Inject
    private MercurySampleDao mercurySampleDao;


    @BeforeMethod
    public void setUp() throws Exception {
        if(mercurySampleDao == null) {
            return;
        }
    }

    @Test(enabled = false, groups = TestGroups.STANDARD)
    public void testFindDupeSamples() throws Exception {

        List<MercurySample> duplicateSamples = mercurySampleDao.findDuplicateSamples();

        assertThat(duplicateSamples.size(), is(equalTo(2904)));
    }
}
