package org.broadinstitute.gpinformatics.infrastructure.bsp.plating;

import org.broadinstitute.bsp.client.container.ContainerManager;
import org.broadinstitute.bsp.client.response.RecentSampleKitResponse;
import org.broadinstitute.bsp.client.response.SampleKitListResponse;
import org.broadinstitute.bsp.client.response.SampleKitResponse;
import org.broadinstitute.bsp.client.response.SampleResponse;
import org.broadinstitute.bsp.client.sample.MaterialType;
import org.broadinstitute.bsp.client.sample.Sample;
import org.broadinstitute.bsp.client.sample.SampleManager;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.bsp.client.users.UserManager;
import org.broadinstitute.bsp.client.workrequest.WorkRequestManager;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Stub;

import javax.enterprise.inject.Alternative;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Scott Matthews
 *         Date: 11/26/12
 *         Time: 3:03 PM
 */
@Stub
@Alternative
public class BSPManagerFactoryStub implements BSPManagerFactory {
    public static final long QA_DUDE_USER_ID = 9382L;

    @Override
    public WorkRequestManager createWorkRequestManager() {
        return null;
    }

    @Override
    public ContainerManager createContainerManager() {
        return null;
    }

    @Override
    public UserManager createUserManager() {

        return new UserManager() {
            @Override
            public BspUser get(String s) {
                return new BSPUserList.QADudeUser("Test", QA_DUDE_USER_ID);
            }

            @Override
            public BspUser getByDomainUserId(Long aLong) {
                return null;
            }

            @Override
            public List<BspUser> getPrimaryInvestigators() {
                List<BspUser> testList = new LinkedList<>();
                testList.add(new BSPUserList.QADudeUser("PM", QA_DUDE_USER_ID + 1));
                testList.add(new BSPUserList.QADudeUser("PDM", QA_DUDE_USER_ID + 2));
                testList.add(new BSPUserList.QADudeUser("LU", QA_DUDE_USER_ID + 3));
                testList.add(new BSPUserList.QADudeUser("LM", QA_DUDE_USER_ID + 4));
                return testList;
            }

            @Override
            public List<BspUser> getProjectManagers() {
                List<BspUser> testList = new LinkedList<>();
                testList.add(new BSPUserList.QADudeUser("PM", QA_DUDE_USER_ID + 1));
                return testList;
            }

            @Override
            public List<BspUser> getUsers() {
                List<BspUser> testList = new LinkedList<>();
                testList.add(new BSPUserList.QADudeUser("Test", QA_DUDE_USER_ID));
                testList.add(new BSPUserList.QADudeUser("PM", QA_DUDE_USER_ID + 1));
                testList.add(new BSPUserList.QADudeUser("PDM", QA_DUDE_USER_ID + 2));
                testList.add(new BSPUserList.QADudeUser("LU", QA_DUDE_USER_ID + 3));
                testList.add(new BSPUserList.QADudeUser("LM", QA_DUDE_USER_ID + 4));
                BspUser jwalshUsr = new BspUser();
                jwalshUsr.setUserId(QA_DUDE_USER_ID + 100);
                jwalshUsr.setUsername("jowalsh");
                jwalshUsr.setFirstName("John");
                jwalshUsr.setLastName("Walsh");
                jwalshUsr.setEmail("jowaslh@broadinstitute.com");
                testList.add(jwalshUsr);

                BspUser hrafalUsr = new BspUser();
                hrafalUsr.setUserId(QA_DUDE_USER_ID + 101);
                hrafalUsr.setUsername("hrafal");
                hrafalUsr.setFirstName("Howard");
                hrafalUsr.setLastName("Rafal");
                hrafalUsr.setEmail("hrafal@broadinstitute.com");
                testList.add(hrafalUsr);

                BspUser philDunleaUser = new BspUser();
                philDunleaUser.setUserId(7160L);
                philDunleaUser.setUsername("pdunlea");
                philDunleaUser.setFirstName("Phil");
                philDunleaUser.setLastName("Dunlea");
                philDunleaUser.setEmail("pdunlea@broadinstitute.org");
                testList.add(philDunleaUser);

                return testList;
            }
        };
    }

    @Override
    public SampleManager createSampleManager() {

        return new SampleManager() {

            @Override
            public SampleKitListResponse getSampleKitsBySampleIds(List<String> strings) {
                throw new IllegalStateException("Not Yet Implemented");
            }

            @Override
            public SampleKitResponse getSampleKit(String s) {
                throw new IllegalStateException("Not Yet Implemented");
            }

            @Override
            public SampleResponse updateSample(Sample sample) {
                throw new IllegalStateException("Not Yet Implemented");
            }

            @Override
            public SampleResponse getSample(String s) {
                throw new IllegalStateException("Not Yet Implemented");
            }

            @Override
            public RecentSampleKitResponse getRecentlyUpdatedKits(String s) {
                throw new IllegalStateException("Not Yet Implemented");
            }

            @Override
            public List<MaterialType> getMaterialTypes() {
                List<MaterialType> materialTypes = new ArrayList<>();

                materialTypes.add( new MaterialType("Cells:Pellet frozen") );
                materialTypes.add( new MaterialType("DNA:DNA Genomic") );
                materialTypes.add( new MaterialType("DNA:DNA Library External") );
                materialTypes.add( new MaterialType("DNA:Viral Hybrid") );
                materialTypes.add( new MaterialType("RNA:Total RNA") );
                return  materialTypes;

            }

            @Override
            public Boolean participantHasBeenSubmitted(String s, String s1) {
                throw new IllegalStateException("Not Yet Implemented");
            }
        };
    }

}
