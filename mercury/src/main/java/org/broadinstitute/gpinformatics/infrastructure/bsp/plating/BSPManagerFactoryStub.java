package org.broadinstitute.gpinformatics.infrastructure.bsp.plating;

import org.broadinstitute.bsp.client.collection.BspGroupCollectionManager;
import org.broadinstitute.bsp.client.collection.Group;
import org.broadinstitute.bsp.client.container.ContainerManager;
import org.broadinstitute.bsp.client.response.AllGroupsResponse;
import org.broadinstitute.bsp.client.response.RecentSampleKitResponse;
import org.broadinstitute.bsp.client.response.SampleKitListResponse;
import org.broadinstitute.bsp.client.response.SampleKitResponse;
import org.broadinstitute.bsp.client.response.SampleResponse;
import org.broadinstitute.bsp.client.sample.MaterialType;
import org.broadinstitute.bsp.client.sample.Sample;
import org.broadinstitute.bsp.client.sample.SampleKit;
import org.broadinstitute.bsp.client.sample.SampleManager;
import org.broadinstitute.bsp.client.site.AllSitesResponse;
import org.broadinstitute.bsp.client.site.BspSiteManager;
import org.broadinstitute.bsp.client.site.Site;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.bsp.client.users.UserManager;
import org.broadinstitute.bsp.client.workrequest.WorkRequestManager;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * A CDI bean created by a producer
 * @see org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryProducer
 */
public class BSPManagerFactoryStub implements BSPManagerFactory {
    public static final long QA_DUDE_USER_ID = 9382L;

    public static final String TEST_SAMPLE_KIT_ID = "SK-TST1";

    public static final String TEST_SK_SAMPLE_1 = "SM-SK11";
    public static final String TEST_SK_SAMPLE_2 = "SM-SK12";
    public static final String TEST_SK_SAMPLE_3 = "SM-SK13";
    public static final String TEST_SK_SAMPLE_4 = "SM-SK14";

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

                BspUser scottmatUser = new BspUser();
                scottmatUser.setUserId(17794L);
                scottmatUser.setUsername("scottmat");
                scottmatUser.setFirstName("Scott");
                scottmatUser.setLastName("Matthews");
                scottmatUser.setEmail("scottmat@broadinstitute.org");
                testList.add(scottmatUser);

                return testList;
            }
        };
    }

    @Override
    public SampleManager createSampleManager() {
        return new SampleManager() {
            @Override
            public SampleKitListResponse getSampleKitsBySampleIds(Collection<String> strings) {
                SampleKitListResponse response = new SampleKitListResponse();

                SampleKit kitForResponse = new SampleKit();
                kitForResponse.setSampleKitId(TEST_SAMPLE_KIT_ID);

                List<Sample> kitSamples = new ArrayList<>();
                Collections.addAll(kitSamples, new Sample(TEST_SK_SAMPLE_1), new Sample(TEST_SK_SAMPLE_2), new Sample(TEST_SK_SAMPLE_3), new Sample(TEST_SK_SAMPLE_4));

                kitForResponse.setSamples(kitSamples);
                response.setResult(Collections.singletonList(kitForResponse));
                response.setSuccess(true);

                return response;
            }

            @Override
            public SampleKitResponse getSampleKit(String s) {
                SampleKitResponse response = new SampleKitResponse();
                SampleKit kitForResponse = new SampleKit();
                kitForResponse.setSampleKitId(TEST_SAMPLE_KIT_ID);
                kitForResponse.setSamples(Collections.singletonList(new Sample(s)));
                response.setResult(kitForResponse);

                return response;
            }

            @Override
            public SampleResponse updateSample(String username, Sample sample) {
                SampleResponse response = new SampleResponse();
                response.setResult(sample);
                response.setSuccess(true);
                return response;
            }

            @Override
            public SampleResponse updateSample(Sample sample) {
                return updateSample(null, sample);
            }

            @Override
            public SampleResponse getSample(String s) {
                SampleResponse response = new SampleResponse();
                response.setResult(new Sample(s));
                response.setSuccess(true);
                return response;
            }

            @Override
            public RecentSampleKitResponse getRecentlyUpdatedKits(String s) {
                throw new IllegalStateException("Not Yet Implemented");
            }

            @Override
            public List<MaterialType> getMaterialTypes() {
                List<MaterialType> materialTypes = new ArrayList<>();
                materialTypes.add( new MaterialType("Cells:Pellet frozen") );
                materialTypes.add( new MaterialType("DNA:DNA Library External") );
                materialTypes.add( new MaterialType("DNA:Viral Hybrid") );
                materialTypes.add( new MaterialType("RNA:Total RNA") );
                materialTypes.add( new MaterialType("DNA:DNA WGA Qiagen") );
                materialTypes.add( new MaterialType("DNA:DNA Somatic") );
                materialTypes.add( new MaterialType("DNA:DNA Genomic") );
                return materialTypes;
            }

            @Override
            public Boolean participantHasBeenSubmitted(String s, String s1) {
                throw new IllegalStateException("Not Yet Implemented");
            }
        };
    }

    @Override
    public BspSiteManager createSiteManager() {
        return new BspSiteManager(null, null, null, null) {
            @Override
            public AllSitesResponse getAllSites() {
                return new AllSitesResponse(
                        Arrays.asList(new Site(1, "Name", "Description", "Address 1\nAddress 2", "Primary Shipper",
                                false, false)),
                        true);
            }
        };
    }

    @Override
    public BspGroupCollectionManager createGroupCollectionManager() {
        return new BspGroupCollectionManager(null, null, null, null) {
            @Override
            public AllGroupsResponse getAllGroups() {
                return new AllGroupsResponse(
                        Arrays.asList(new Group(1L, "Name", "Description", false)),
                        true);
            }
        };
    }
}
