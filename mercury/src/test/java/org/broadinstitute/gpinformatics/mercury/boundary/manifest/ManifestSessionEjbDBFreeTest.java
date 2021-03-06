package org.broadinstitute.gpinformatics.mercury.boundary.manifest;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimaps;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.common.TestUtils;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.transition.NextTransition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.transition.Transition;
import org.broadinstitute.gpinformatics.infrastructure.parsers.TableProcessor;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.boundary.queue.QueueEjb;
import org.broadinstitute.gpinformatics.mercury.boundary.queue.enqueuerules.DnaQuantEnqueueOverride;
import org.broadinstitute.gpinformatics.mercury.boundary.sample.ClinicalSampleFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.sample.ClinicalSampleTestFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.ParentVesselBean;
import org.broadinstitute.gpinformatics.mercury.control.dao.manifest.ManifestSessionDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.vessel.LabVesselFactory;
import org.broadinstitute.gpinformatics.mercury.crsp.generated.Sample;
import org.broadinstitute.gpinformatics.mercury.crsp.generated.SampleData;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestEvent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestRecord;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestSession;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestStatus;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.TubeTransferException;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.broadinstitute.gpinformatics.FormatStringMatcher.matchesFormatString;
import static org.broadinstitute.gpinformatics.mercury.boundary.manifest.ManifestStatusErrorMatcher.hasError;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.collection.IsEmptyCollection.emptyCollectionOf;

@Test(groups = TestGroups.DATABASE_FREE)
public class ManifestSessionEjbDBFreeTest {

    private static final String MANIFEST_FILE_GOOD = "manifest-upload/good-manifest.xlsx";

    private static final String BAD_MANIFEST_UPLOAD_PROVIDER = "badManifestUploadProvider";

    private static final BSPUserList.QADudeUser TEST_USER = new BSPUserList.QADudeUser("BUICK USER", 42);

    private static final String TEST_RESEARCH_PROJECT_KEY = "RP-1";

    private static final int NUM_RECORDS_IN_GOOD_MANIFEST = 23;

    private static final String MANIFEST_FILE_DUPLICATES_SAME_SESSION = "manifest-upload/manifest-with-duplicates.xlsx";
    private static final String MANIFEST_FILE_COVID_DUPLICATES_SAME_SESSION = "manifest-import/PHS_manifest_03232020_duplicate_sample_ids.xlsx";

    private static final int NUM_DUPLICATES_IN_MANIFEST_WITH_DUPLICATES_IN_SAME_SESSION = 7;

    private static final String MANIFEST_FILE_MISMATCHED_GENDERS_SAME_SESSION =
            "manifest-upload/gender-mismatches-within-session/good-manifest.xlsx";

    private static final String MANIFEST_FILE_DUPLICATE_MATRIX_IDS_SAME_SESSION =
            "manifest-import/PHS_manifest_03232020_duplicate_ids.xlsx";

    private static final String GOOD_COVID_MANIFEST =
            "manifest-import/PHS_manifest_03232020.csv";

    private static final Set<String> PATIENT_IDS_FOR_SAME_MANIFEST_GENDER_MISMATCHES =
            ImmutableSet.of("004-002", "003-009", "005-012");

    private static final Set<String> SAMPE_IDS_FOR_SAME_DUPLICATE_MATRIX_IDS =
            ImmutableSet.of("G3270000944", "G3270000947", "G3270000951", "G3270000955");

    private static final Set<String> DUPLICATED_SAMPLE_IDS =
            ImmutableSet.of("03101231193", "03101411324", "03101254356");

    private static final long ARBITRARY_MANIFEST_SESSION_ID = 3L;

    private static final String GOOD_MANIFEST_ACCESSION_SCAN_PROVIDER = "goodManifestAccessionScanProvider";

    private static final String GOOD_TUBE_BARCODE = "03101231193";

    private static final String BAD_TUBE_BARCODE = "bad tube barcode";
    private static final String FEMALE = "Female";
    private static final String MALE = "Male";
    private static final ManifestRecord.ErrorStatus NO_ERROR = null;
    private static final String SM_1 = "SM-1";
    private static final String PATIENT_1 = "patient-1";
    private static final String SM_2 = "SM-2";
    private static final String PATIENT_2 = "patient-2";
    private static final String TEST_SESSION_NAME = "SomeSession";
    private ManifestSessionDao manifestSessionDao;
    private ResearchProjectDao researchProjectDao;
    private String sourceForTransfer = "9294923";

    private static final String TEST_SAMPLE_KEY = "SM-BUICK1";
    private static final String BSP_TEST_SAMPLE_KEY = TEST_SAMPLE_KEY + "BSP";
    private static final String TEST_SAMPLE_ALREADY_TRANSFERRED = TEST_SAMPLE_KEY + "ACCESSIONED";
    private static final String TEST_SAMPLE_KEY_UNASSOCIATED = "SM-BUICK2";

    private MercurySample testSampleForAccessioning;
    private MercurySample testUnassociatedSampleForAccessioning;
    private MercurySample testSampleForBsp;
    private MercurySample testSampleAlreadyTransferred;

    private static final String TEST_VESSEL_LABEL_ALREADY_TRANSFERRED = "A0993929292Trans";
    private static final String TEST_VESSEL_LABEL = "A0993929292";
    private LabVessel testVessel;
    private LabVessel testVesselAlreadyTransferred;

    private MercurySampleDao mercurySampleDao;
    private LabVesselDao labVesselDao;
    private final BSPUserList.QADudeUser testLabUser = new BSPUserList.QADudeUser("LU", 342L);
    private UserBean mockUserBean;
    private ManifestSessionEjb manifestSessionEjb;
    private LabVesselFactory labVesselFactory;
    private CovidManifestCopier covidManifestCopier;
    /**
     * How many instances of a particular collaborator barcode are seen in this manifest (anything more than 1 is
     * an error).
     */
    private static final Map<String, Integer> SAMPLE_ID_TO_NUMBER_OF_COPIES_FOR_DUPLICATE_TESTS =
            ImmutableMap.of("03101231193", 2, "03101254356", 3, "03101411324", 2);
    public JiraService jiraService;
    private BSPUserList bspUserList;
    private QueueEjb queueEjb;
    private DnaQuantEnqueueOverride dnaQuantEnqueueOverride;

    @BeforeMethod
    public void setUp() throws Exception {
        manifestSessionDao = Mockito.mock(ManifestSessionDao.class);
        researchProjectDao = Mockito.mock(ResearchProjectDao.class);
        mercurySampleDao = Mockito.mock(MercurySampleDao.class);
        labVesselDao = Mockito.mock(LabVesselDao.class);
        mockUserBean = Mockito.mock(UserBean.class);
        jiraService = Mockito.mock(JiraService.class);
        bspUserList = Mockito.mock(BSPUserList.class);
        queueEjb = Mockito.mock(QueueEjb.class);
        dnaQuantEnqueueOverride = Mockito.mock(DnaQuantEnqueueOverride.class);
        labVesselFactory = Mockito.mock(LabVesselFactory.class);
        covidManifestCopier = Mockito.mock(CovidManifestCopier.class);

        Mockito.when(mockUserBean.getBspUser()).thenReturn(testLabUser);
        Mockito.when(mockUserBean.getLoginUserName()).thenReturn(testLabUser.getUsername());
        Mockito.when(bspUserList.getByUsername(Mockito.anyString())).thenReturn(testLabUser);
        Mockito.when(jiraService.getIssueInfo(Mockito.anyString())).thenAnswer(new Answer<JiraIssue>() {
            @Override
            public JiraIssue answer(InvocationOnMock invocationOnMock) throws Throwable {
                String jiraKey = (String) invocationOnMock.getArguments()[0];

                JiraIssue mockedIssue = new JiraIssue(jiraKey, jiraService);
                if (jiraKey == null) {
                    return null;
                }
                mockedIssue.setCreated(new Date());
                mockedIssue.setSummary("");
                mockedIssue.setDescription("");
                mockedIssue.setReporter("QADudeLU");
                return mockedIssue;
            }
        });
        Mockito.when(jiraService.getIssueInfo(Mockito.anyString(),Mockito.anyString())).thenAnswer(new Answer<JiraIssue>() {
            @Override
            public JiraIssue answer(InvocationOnMock invocationOnMock) throws Throwable {
                String jiraKey = (String) invocationOnMock.getArguments()[0];

                JiraIssue mockedIssue = new JiraIssue(jiraKey, jiraService);
                if(jiraKey == null) {
                    return null;
                }
                mockedIssue.setCreated(new Date());
                mockedIssue.setSummary("");
                mockedIssue.setDescription("");
                mockedIssue.setReporter("QADudeLU");
                mockedIssue.setStatus("Status");
                mockedIssue.addFieldValue((String) invocationOnMock.getArguments()[1],
                        Collections.singletonList(new HashMap<>()));
                return mockedIssue;
            }
        });
        Mockito.when(jiraService.findAvailableTransitionByName(Mockito.anyString(), Mockito.anyString())).thenAnswer(
                new Answer<Transition>() {
                    @Override
                    public Transition answer(InvocationOnMock invocationOnMock) throws Throwable {

                        String transitionName = (String) invocationOnMock.getArguments()[1];


                        return new Transition(transitionName, transitionName,
                                new NextTransition("", transitionName+"next", "Next Transition", "",
                                        transitionName + "next"));
                    }
                });

        testSampleForAccessioning =
                new MercurySample(TEST_SAMPLE_KEY, MercurySample.MetadataSource.MERCURY);
        testUnassociatedSampleForAccessioning =
                new MercurySample(TEST_SAMPLE_KEY_UNASSOCIATED, MercurySample.MetadataSource.MERCURY);
        testSampleForBsp =
                new MercurySample(BSP_TEST_SAMPLE_KEY, MercurySample.MetadataSource.BSP);
        testSampleAlreadyTransferred = new MercurySample(TEST_SAMPLE_ALREADY_TRANSFERRED,
                MercurySample.MetadataSource.MERCURY);
        testVessel =
                new BarcodedTube(TEST_VESSEL_LABEL, BarcodedTube.BarcodedTubeType.MatrixTube2mL);
        testVesselAlreadyTransferred = new BarcodedTube(TEST_VESSEL_LABEL_ALREADY_TRANSFERRED,
                BarcodedTube.BarcodedTubeType.MatrixTube2mL);

        Mockito.when(labVesselFactory.buildLabVessels(Mockito.anyListOf(ParentVesselBean.class), Mockito.anyString(),
                Mockito.any(Date.class),Mockito.any(LabEventType.class),
                Mockito.any(MercurySample.MetadataSource.class))).thenReturn(Pair.of(Collections.singletonList(testVessel),
                Collections.singletonList(testVessel)));

        manifestSessionEjb =
                new ManifestSessionEjb(manifestSessionDao, researchProjectDao, mercurySampleDao, labVesselDao,
                        mockUserBean, bspUserList, jiraService, queueEjb, dnaQuantEnqueueOverride, labVesselFactory,
                        covidManifestCopier);
    }

    /**
     * Worker method for manifest upload testing.
     */
    private ManifestSession uploadManifest(String pathToManifestFile,
                                           ManifestSessionEjb.AccessioningProcessType processType,
                                           boolean fromSampleKit) throws Exception {
        return uploadManifest(pathToManifestFile, createTestResearchProject(),
                processType, fromSampleKit);
    }

    private ManifestSession uploadManifest(ManifestSessionEjb manifestSessionEjb, String pathToManifestFile,
                                           ResearchProject researchProject,
                                           ManifestSessionEjb.AccessioningProcessType processType,
                                           boolean fromSampleKit) throws Exception {
        String PATH_TO_SPREADSHEET = TestUtils.getTestData(pathToManifestFile);
        InputStream inputStream = new FileInputStream(PATH_TO_SPREADSHEET);
        return manifestSessionEjb.uploadManifest(researchProject.getBusinessKey(), inputStream, PATH_TO_SPREADSHEET,
                processType, fromSampleKit);
    }

    /**
     * Worker method for manifest upload testing.
     */
    private ManifestSession uploadManifest(String pathToManifestFile, ResearchProject researchProject,
                                           ManifestSessionEjb.AccessioningProcessType processType,
                                           boolean fromSampleKit)
            throws Exception {
        ManifestSessionEjb ejb = buildEjbForUpload(researchProject);
        return uploadManifest(ejb, pathToManifestFile, researchProject, processType, fromSampleKit);
    }

    private ManifestSessionEjb buildEjbForUpload(ResearchProject researchProject) {
        Mockito.when(researchProjectDao.findByBusinessKey(researchProject.getBusinessKey()))
                .thenReturn(researchProject);

        return new ManifestSessionEjb(manifestSessionDao, researchProjectDao, mercurySampleDao, labVesselDao,
                mockUserBean, bspUserList, jiraService, queueEjb, dnaQuantEnqueueOverride, labVesselFactory,
                covidManifestCopier);
    }

    /**
     * Utility class to encapsulate both objects returned by the #buildForAcceptSession method.
     */
    private class ManifestSessionAndEjbHolder {
        ManifestSession manifestSession;
        ManifestSessionEjb ejb;
    }

    private static void addRecord(ManifestSessionAndEjbHolder holder, ManifestRecord.ErrorStatus errorStatus,
                                  ManifestRecord.Status status, Map<Metadata.Key, String> initialData,
                                  EnumSet<Metadata.Key> excludeKeys) {
        ManifestTestFactory.addRecord(holder.manifestSession, errorStatus, status, initialData,
                excludeKeys);
    }

    private static void addRecord(ManifestSessionAndEjbHolder holder, ManifestRecord.ErrorStatus errorStatus,
                                  ManifestRecord.Status status, Metadata.Key key1, String value1,
                                  Metadata.Key key2, String value2, Metadata.Key key3, String value3) {
        ManifestTestFactory.addRecord(holder.manifestSession, errorStatus, status,
                ImmutableMap.of(key1, value1, key2, value2, key3, value3), EnumSet.of(Metadata.Key.BROAD_2D_BARCODE));
    }

    /**
     * Helper method to initialize the EJB and session used for testing.
     *
     * @param initialStatus   Initial status for all the manifest records created
     * @param numberOfRecords the number of initial manifest records to create
     *
     * @param withSampleKit
     * @param processType
     * @return A new instance of EJB and Holder class (which holds a ManifestSessionEjb and
     * ManifestSession)
     */
    private ManifestSessionAndEjbHolder buildHolderForSession(ManifestRecord.Status initialStatus, int numberOfRecords,
                                                              boolean withSampleKit,
                                                              ManifestSessionEjb.AccessioningProcessType processType)
            throws Exception {

        return buildHolderForSession(initialStatus, numberOfRecords, createTestResearchProject(), withSampleKit,
                processType);
    }

    /**
     * @param initialStatus   Initial status for all the manifest records created
     * @param numberOfRecords the number of initial manifest records to create
     * @param researchProject An instance of a research project with which to associate the new session
     *
     * @param withSampleKit
     * @param accessioningProcessType
     * @return A new instance of the ManifestSessionAndEjbHolder class (which holds a manifest session ejb and
     * session)
     */
    private ManifestSessionAndEjbHolder buildHolderForSession(ManifestRecord.Status initialStatus,
                                                              int numberOfRecords,
                                                              ResearchProject researchProject, boolean withSampleKit,
                                                              ManifestSessionEjb.AccessioningProcessType accessioningProcessType)
            throws Exception {

        final ManifestSessionAndEjbHolder holder = new ManifestSessionAndEjbHolder();
        holder.manifestSession = ManifestTestFactory.buildManifestSession(researchProject.getBusinessKey(),
                accessioningProcessType.name()+"_CloseGood", testLabUser, numberOfRecords, initialStatus, withSampleKit,
                accessioningProcessType);

        Mockito.when(manifestSessionDao.find(Mockito.anyLong())).thenAnswer(new Answer<ManifestSession>() {
            @Override
            public ManifestSession answer(InvocationOnMock invocation) throws Throwable {
                Long id = (Long) invocation.getArguments()[0];
                if (id == 0) {
                    return null;
                }
                return holder.manifestSession;
            }
        });
        Mockito.when(researchProjectDao.findByBusinessKey(researchProject.getBusinessKey()))
                .thenReturn(researchProject);

        Mockito.when(mercurySampleDao.findBySampleKey(Mockito.eq(TEST_SAMPLE_KEY))).thenReturn(
                testSampleForAccessioning);

        Mockito.when(mercurySampleDao.findBySampleKey(Mockito.eq(BSP_TEST_SAMPLE_KEY))).thenReturn(
                testSampleForBsp);

        Mockito.when(mercurySampleDao.findBySampleKey(Mockito.eq(TEST_SAMPLE_ALREADY_TRANSFERRED))).thenReturn(
                testSampleAlreadyTransferred);

        Mockito.when(mercurySampleDao.findBySampleKey(Mockito.eq(TEST_SAMPLE_KEY_UNASSOCIATED))).thenReturn(
                testUnassociatedSampleForAccessioning);

        testVessel.addSample(testSampleForAccessioning);

        Mockito.when(labVesselDao.findByIdentifier(Mockito.eq(TEST_VESSEL_LABEL))).thenReturn(testVessel);

        testVesselAlreadyTransferred.addSample(testSampleAlreadyTransferred);
        LabEvent collaboratorTransferEvent =
                new LabEvent(LabEventType.COLLABORATOR_TRANSFER, new Date(), " ", 1L, testLabUser.getUserId(), "");
        testVesselAlreadyTransferred.addInPlaceEvent(collaboratorTransferEvent);

        Mockito.when(labVesselDao.findByIdentifier(Mockito.eq(TEST_VESSEL_LABEL_ALREADY_TRANSFERRED)))
                .thenReturn(testVesselAlreadyTransferred);

        holder.ejb = new ManifestSessionEjb(manifestSessionDao, researchProjectDao, mercurySampleDao, labVesselDao,
                mockUserBean, bspUserList, jiraService, queueEjb, dnaQuantEnqueueOverride, labVesselFactory,
                covidManifestCopier);

        return holder;
    }

    /* ================================================================= */
    /* =====  Test Cases =============================================== */
    /* ================================================================= */

    public void researchProjectNotFound() {
        ManifestSessionEjb ejb = new ManifestSessionEjb(manifestSessionDao, researchProjectDao, mercurySampleDao,
                labVesselDao, mockUserBean, bspUserList, jiraService, queueEjb, dnaQuantEnqueueOverride,
                labVesselFactory, covidManifestCopier);
        try {
            ejb.uploadManifest(null, null, null, ManifestSessionEjb.AccessioningProcessType.CRSP, false);
            Assert.fail();
        } catch (InformaticsServiceException ignored) {
        }
    }

    public void covidResearchProjectNotFound() throws Exception {
        ManifestSessionEjb ejb = new ManifestSessionEjb(manifestSessionDao, researchProjectDao, mercurySampleDao,
                labVesselDao, mockUserBean, bspUserList, jiraService, queueEjb, dnaQuantEnqueueOverride,
                labVesselFactory, covidManifestCopier);
        try {
            String PATH_TO_SPREADSHEET = TestUtils.getTestData("manifest-import/PHS_manifest_03232020.csv");
            InputStream inputStream = new FileInputStream(PATH_TO_SPREADSHEET);

            ejb.uploadManifest(null, inputStream, "PHS_manifest_03232020.csv",
                    ManifestSessionEjb.AccessioningProcessType.COVID, false);
        } catch (InformaticsServiceException ignored) {
            Assert.fail();
        }
    }

    @DataProvider(name = "processTypeFileAndSampleKitInput")
    public Object[][] processTypeFileAndSampleKitInput() {
        // @formatter:off
        return new Object[][]{
                {MANIFEST_FILE_GOOD,ManifestSessionEjb.AccessioningProcessType.CRSP, false, NUM_RECORDS_IN_GOOD_MANIFEST},
                {MANIFEST_FILE_GOOD,ManifestSessionEjb.AccessioningProcessType.CRSP, true, NUM_RECORDS_IN_GOOD_MANIFEST},
                {GOOD_COVID_MANIFEST,ManifestSessionEjb.AccessioningProcessType.COVID, false, 8},
                {GOOD_COVID_MANIFEST,ManifestSessionEjb.AccessioningProcessType.COVID, true, 8},
        };
    }

    /********************************************************************/
    /**  =======  upload manifest tests ============================== **/
    /********************************************************************/

    @Test(dataProvider = "processTypeFileAndSampleKitInput")
    public void uploadGoodManifest(String manifestFilePath, ManifestSessionEjb.AccessioningProcessType processType,
                                   boolean sampleKit, int numRecordsInGoodManifest) throws Exception {
        ManifestSession manifestSession = uploadManifest(manifestFilePath,
                processType, sampleKit);
        assertThat(manifestSession, is(notNullValue()));
        assertThat(manifestSession.getRecords(), hasSize(numRecordsInGoodManifest));
        assertThat(manifestSession.hasErrors(), is(false));
        assertThat(manifestSession.getManifestEvents(), is(empty()));
    }

    @DataProvider(name = BAD_MANIFEST_UPLOAD_PROVIDER)
    public Object[][] badManifestUploadProvider() {
        // @formatter:off
        return new Object[][]{
                {"Not an Excel file", "Your InputStream was neither an OLE2 stream, nor an OOXML stream",
                        "manifest-upload/not-an-excel-file.txt", ManifestSessionEjb.AccessioningProcessType.CRSP, false},
                {"Missing required field", TableProcessor.getPrefixedMessage(String.format(
                        TableProcessor.REQUIRED_VALUE_IS_MISSING, "Specimen_Number"), null, 1),
                        "manifest-import/test-manifest-missing-specimen.xlsx", ManifestSessionEjb.AccessioningProcessType.CRSP, false},
                {"Missing required field", TableProcessor.getPrefixedMessage(String.format(
                        TableProcessor.REQUIRED_VALUE_IS_MISSING, "Specimen_Number"), null, 1),
                        "manifest-import/test-manifest-missing-specimen.csv", ManifestSessionEjb.AccessioningProcessType.CRSP, false},
                {"Missing column", String.format(TableProcessor.REQUIRED_HEADER_IS_MISSING, "Specimen_Number", ManifestSessionEjb.AccessioningProcessType.CRSP),
                        "manifest-upload/manifest-with-missing-column.xlsx", ManifestSessionEjb.AccessioningProcessType.CRSP, false},
                {"Missing column", String.format(TableProcessor.REQUIRED_HEADER_IS_MISSING, "Specimen_Number", ManifestSessionEjb.AccessioningProcessType.CRSP),
                        "manifest-upload/manifest-with-missing-column.csv", ManifestSessionEjb.AccessioningProcessType.CRSP, false},
                {"Missing column", String.format(TableProcessor.REQUIRED_HEADER_IS_MISSING, "time_collected", ManifestSessionEjb.AccessioningProcessType.COVID),
                        "manifest-import/COVID_TEST_NO_PATIENT_or_collected_time.xlsx", ManifestSessionEjb.AccessioningProcessType.COVID, false},
                {"Missing column", String.format(TableProcessor.REQUIRED_HEADER_IS_MISSING, "time_collected", ManifestSessionEjb.AccessioningProcessType.COVID),
                        "manifest-import/COVID_TEST_NO_PATIENT_or_collected_time.csv", ManifestSessionEjb.AccessioningProcessType.COVID, false},
                {"Missing column", String.format(TableProcessor.REQUIRED_HEADER_IS_MISSING, "time_collected", ManifestSessionEjb.AccessioningProcessType.COVID),
                        "manifest-import/COVID_TEST_NO_PATIENT_or_collected_time.xlsx", ManifestSessionEjb.AccessioningProcessType.COVID, true},
                {"Missing column", String.format(TableProcessor.REQUIRED_HEADER_IS_MISSING, "time_collected", ManifestSessionEjb.AccessioningProcessType.COVID),
                        "manifest-import/COVID_TEST_NO_PATIENT_or_collected_time.csv", ManifestSessionEjb.AccessioningProcessType.COVID, true},
                {"Empty manifest", "The uploaded Manifest has no data.",
                        "manifest-upload/empty-manifest.xlsx", ManifestSessionEjb.AccessioningProcessType.CRSP, false},
                {"Empty manifest", "The uploaded Manifest has no data.",
                        "manifest-upload/empty-manifest.csv", ManifestSessionEjb.AccessioningProcessType.CRSP, false},
                {"Empty manifest", "The uploaded Manifest has no data.",
                        "manifest-upload/empty-COVID-manifest.xlsx", ManifestSessionEjb.AccessioningProcessType.COVID, false},
                {"Empty manifest", "The uploaded Manifest has no data.",
                        "manifest-upload/empty-COVID-manifest.csv", ManifestSessionEjb.AccessioningProcessType.COVID, false},
                {"Empty manifest", "The uploaded Manifest has no data.",
                        "manifest-upload/empty-COVID-manifest.xlsx", ManifestSessionEjb.AccessioningProcessType.COVID, true},
                {"Empty manifest", "The uploaded Manifest has no data.",
                        "manifest-upload/empty-COVID-manifest.csv", ManifestSessionEjb.AccessioningProcessType.COVID, true},
                {"Multiple Bad Columns in Manifest",
                        String.format(TableProcessor.REQUIRED_HEADER_IS_MISSING, "Specimen_Number") + "\n" +
                                String.format(TableProcessor.REQUIRED_HEADER_IS_MISSING, "Sex") + "\n" +
                                String.format(TableProcessor.REQUIRED_HEADER_IS_MISSING, "Patient_ID") + "\n" +
                                String.format(TableProcessor.REQUIRED_HEADER_IS_MISSING, "Collection_Date") + "\n" +
                                String.format(TableProcessor.REQUIRED_HEADER_IS_MISSING, "Visit") + "\n" +
                                String.format(TableProcessor.REQUIRED_HEADER_IS_MISSING, "SAMPLE_TYPE") + "\n" +
                                "Row #2 Unknown header(s) \"Sample Type\", \"Specimen_Numberz\", \"Date\", \"Gender\", \"Patient_Idee\", \"Visit Type\".",
                        "manifest-upload/manifest-with-multiple-bad-columns.xlsx", ManifestSessionEjb.AccessioningProcessType.CRSP, false},
                {"Multiple Bad Columns in Manifest",
                        String.format(TableProcessor.REQUIRED_HEADER_IS_MISSING, "Specimen_Number") + "\n" +
                                String.format(TableProcessor.REQUIRED_HEADER_IS_MISSING, "Sex") + "\n" +
                                String.format(TableProcessor.REQUIRED_HEADER_IS_MISSING, "Patient_ID") + "\n" +
                                String.format(TableProcessor.REQUIRED_HEADER_IS_MISSING, "Collection_Date") + "\n" +
                                String.format(TableProcessor.REQUIRED_HEADER_IS_MISSING, "Visit") + "\n" +
                                String.format(TableProcessor.REQUIRED_HEADER_IS_MISSING, "SAMPLE_TYPE") + "\n" +
                                "Row #2 Unknown header(s) \"Sample Type\", \"Specimen_Numberz\", \"Date\", \"Gender\", \"Patient_Idee\", \"Visit Type\".",
                        "manifest-upload/manifest-with-multiple-bad-columns.csv", ManifestSessionEjb.AccessioningProcessType.CRSP, false},
                {"Unrecognized Material Types in file",
                        "Row #8 An unrecognized material type was entered: DNR:Heroic\n"
                        + "Row #9 An unrecognized material type was entered: Fresh DNA\n"
                        + "Row #10 An unrecognized material type was entered: Buffy Vampire Coat",
                        "manifest-upload/manifest-with-bad-material-type.xlsx", ManifestSessionEjb.AccessioningProcessType.CRSP, false},
                {"Unrecognized Material Types in file",
                        "Row #8 An unrecognized material type was entered: DNR:Heroic\n"
                        + "Row #9 An unrecognized material type was entered: Fresh DNA\n"
                        + "Row #10 An unrecognized material type was entered: Buffy Vampire Coat",
                        "manifest-upload/manifest-with-bad-material-type.csv", ManifestSessionEjb.AccessioningProcessType.CRSP, false},
        };
        // @formatter:on
    }

    @Test(dataProvider = BAD_MANIFEST_UPLOAD_PROVIDER)
    public void uploadBadManifest(String description, String errorMessage, String pathToManifestFile,
                                  ManifestSessionEjb.AccessioningProcessType accessioningProcessType,
                                  boolean fromSampleKit) {
        try {
            uploadManifest(pathToManifestFile, accessioningProcessType, fromSampleKit);
            Assert.fail(description);
        } catch (Exception e) {
            final String splitOnThis = "Unknown header(s) ";
            if (e.getMessage().contains(splitOnThis)) {
                // Checks the part before the header list
                Assert.assertEquals(StringUtils.substringBefore(e.getMessage(), splitOnThis),
                        StringUtils.substringBefore(errorMessage, splitOnThis));
                // Checks the header list, allowing for the headers to be returned in a different order.
                String[] rawActualHeaders = StringUtils.substringAfter(e.getMessage(), splitOnThis).split(",");
                String[] rawExpectedHeaders = StringUtils.substringAfter(errorMessage, splitOnThis).split(",");
                List<String> actualHeaders = Arrays.asList(StringUtils.stripAll(rawActualHeaders, "[\\. ]"));
                List<String> expectedHeaders = Arrays.asList(StringUtils.stripAll(rawExpectedHeaders, "[\\. ]"));
                Collections.sort(actualHeaders);
                Collections.sort(expectedHeaders);
                Assert.assertEquals(actualHeaders, expectedHeaders);
            } else {
                Assert.assertEquals(e.getMessage(), errorMessage);
            }
        }
    }

    public void uploadManifestThatDuplicatesSampleIdInSameManifest() throws Exception {
        ManifestSession manifestSession = uploadManifest(MANIFEST_FILE_DUPLICATES_SAME_SESSION,
                ManifestSessionEjb.AccessioningProcessType.CRSP, false);
        assertThat(manifestSession, is(notNullValue()));
        assertThat(manifestSession.getRecords(), hasSize(NUM_RECORDS_IN_GOOD_MANIFEST));
        assertThat(manifestSession.hasErrors(), is(true));

        ImmutableListMultimap<String, ManifestRecord> sampleIdToRecordMultimap =
                buildSampleIdToRecordMultimap(manifestSession);
        List<ManifestRecord> quarantinedRecords = new ArrayList<>();

        for (ManifestRecord manifestRecord : manifestSession.getRecords()) {
            if (manifestRecord.isQuarantined()) {
                quarantinedRecords.add(manifestRecord);
            }
        }
        // This file contains two instances of duplication and one triplication, so 2 x 2 + 3 = 7.
        assertThat(quarantinedRecords, hasSize(NUM_DUPLICATES_IN_MANIFEST_WITH_DUPLICATES_IN_SAME_SESSION));

        for (ManifestRecord record : quarantinedRecords) {
            assertThat(record.getManifestEvents(), hasSize(1));
            ManifestEvent manifestEvent = record.getManifestEvents().iterator().next();
            Collection<ManifestRecord> duplicates = filterThisRecord(record,
                    sampleIdToRecordMultimap.get(record.getValueByKey(Metadata.Key.SAMPLE_ID)));
            assertThat(manifestEvent.getMessage(),
                    containsString(record.buildMessageForConflictingRecords(duplicates)));
        }
    }

    @DataProvider(name = "covidProcessTypeAndSampleKitInput")
    public Object[][] covidProcessTypeAndSampleKitInput() {
        // @formatter:off
        return new Object[][]{
                {ManifestSessionEjb.AccessioningProcessType.COVID, false},
                {ManifestSessionEjb.AccessioningProcessType.COVID, true},
        };
    }

    @Test(dataProvider = "covidProcessTypeAndSampleKitInput")
    public void uploadCOVIDManifestThatDuplicatesSampleIdInSameManifest(
            ManifestSessionEjb.AccessioningProcessType accessioningProcessType, boolean fromSampleKit) throws Exception {
        ManifestSession manifestSession = uploadManifest(MANIFEST_FILE_COVID_DUPLICATES_SAME_SESSION,
                accessioningProcessType, fromSampleKit);
        assertThat(manifestSession, is(notNullValue()));
        assertThat(manifestSession.getRecords(), hasSize(8));
        assertThat(manifestSession.hasErrors(), is(true));

        ImmutableListMultimap<String, ManifestRecord> sampleIdToRecordMultimap =
                buildSampleIdToRecordMultimap(manifestSession);
        List<ManifestRecord> quarantinedRecords = new ArrayList<>();

        for (ManifestRecord manifestRecord : manifestSession.getRecords()) {
            if (manifestRecord.isQuarantined()) {
                quarantinedRecords.add(manifestRecord);
            }
        }
        // This file contains two instances of duplication and one triplication, so 2 x 2 + 3 = 7.
        assertThat(quarantinedRecords, hasSize(5));

        for (ManifestRecord record : quarantinedRecords) {
            assertThat(record.getManifestEvents(), hasSize(1));
            ManifestEvent manifestEvent = record.getManifestEvents().iterator().next();
            Collection<ManifestRecord> duplicates = filterThisRecord(record,
                    sampleIdToRecordMultimap.get(record.getValueByKey(Metadata.Key.SAMPLE_ID)));
            assertThat(manifestEvent.getMessage(),
                    containsString(record.buildMessageForConflictingRecords(duplicates)));
        }
    }

    private Collection<ManifestRecord> filterThisRecord(final ManifestRecord record,
                                                        ImmutableList<ManifestRecord> manifestRecords) {
        return Collections2.filter(manifestRecords, new Predicate<ManifestRecord>() {
            @Override
            public boolean apply(ManifestRecord localRecord) {
                return localRecord != record;
            }
        });
    }

    public void uploadManifestThatDuplicatesSampleIdInAnotherManifest() throws Exception {
        ResearchProject researchProject = createTestResearchProject();
        Set<String> duplicatedSampleIds = ImmutableSet.of("03101231193", "03101752020");

        ManifestSession manifestSession1 =
                uploadManifest("manifest-upload/duplicates/good-manifest-1.xlsx", researchProject,
                        ManifestSessionEjb.AccessioningProcessType.CRSP, false);
        assertThat(manifestSession1, is(notNullValue()));
        assertThat(manifestSession1.getManifestEvents(), is(empty()));
        assertThat(manifestSession1.getRecords(), hasSize(NUM_RECORDS_IN_GOOD_MANIFEST));

        ImmutableListMultimap<String, ManifestRecord> session1RecordsBySampleId = buildSampleIdToRecordMultimap(
                manifestSession1);

        ManifestSession manifestSession2 =
                uploadManifest("manifest-upload/duplicates/good-manifest-2.xlsx", researchProject,
                        ManifestSessionEjb.AccessioningProcessType.CRSP, false);
        assertThat(manifestSession2, is(notNullValue()));
        assertThat(manifestSession2.getManifestEvents(), hasSize(2));
        assertThat(manifestSession2.getRecords(), hasSize(NUM_RECORDS_IN_GOOD_MANIFEST));

        for (ManifestRecord record : manifestSession2.getRecords()) {
            if (duplicatedSampleIds.contains(record.getValueByKey(Metadata.Key.SAMPLE_ID))) {
                assertThat(record.getManifestEvents(), hasSize(1));
                ManifestEvent manifestEvent = record.getManifestEvents().iterator().next();
                // Only one duplicate in this Multimap for each of these.
                ManifestRecord duplicateRecord = session1RecordsBySampleId.get(record.getValueByKey(
                        Metadata.Key.SAMPLE_ID)).iterator().next();
                assertThat(manifestEvent.getMessage(),
                        containsString(
                                record.buildMessageForConflictingRecords(Collections.singleton(duplicateRecord))));
            } else {
                assertThat(record.getManifestEvents(), is(empty()));
            }
        }
    }

    private ImmutableListMultimap<String, ManifestRecord> buildSampleIdToRecordMultimap(
            ManifestSession manifestSession) {
        return Multimaps.index(manifestSession.getRecords(), new Function<ManifestRecord, String>() {
            @Override
            public String apply(ManifestRecord manifestRecord) {
                return manifestRecord.getValueByKey(Metadata.Key.SAMPLE_ID);
            }
        });
    }

    private ImmutableListMultimap<String, ManifestRecord> buildMatrixIdToRecordMultimap(
            ManifestSession manifestSession) {
        return Multimaps.index(manifestSession.getRecords(), new Function<ManifestRecord, String>() {
            @Override
            public String apply(ManifestRecord manifestRecord) {
                return manifestRecord.getValueByKey(Metadata.Key.BROAD_2D_BARCODE);
            }
        });
    }

    private ResearchProject createTestResearchProject() {
        ResearchProject researchProject =
                ResearchProjectTestFactory.createTestResearchProject(TEST_RESEARCH_PROJECT_KEY);
        researchProject.setRegulatoryDesignation(ResearchProject.RegulatoryDesignation.CLINICAL_DIAGNOSTICS);
        return researchProject;
    }

    public void uploadManifestThatMismatchesGenderInSameManifest() throws Exception {
        ManifestSession manifestSession = uploadManifest(MANIFEST_FILE_MISMATCHED_GENDERS_SAME_SESSION,
                ManifestSessionEjb.AccessioningProcessType.CRSP, false);
        assertThat(manifestSession, is(notNullValue()));
        assertThat(manifestSession.getRecords(), hasSize(NUM_RECORDS_IN_GOOD_MANIFEST));

        ImmutableListMultimap<String, ManifestRecord> patientIdToManifestRecords =
                buildPatientIdToManifestRecordsMultimap(manifestSession);

        // The assert below is for size * 2 since all manifest records in question are in this manifest session.
        assertThat(manifestSession.getManifestEvents(),
                hasSize(PATIENT_IDS_FOR_SAME_MANIFEST_GENDER_MISMATCHES.size() * 2));
        for (ManifestRecord manifestRecord : manifestSession.getRecords()) {
            assertThat(manifestRecord.isQuarantined(), is(false));
            Metadata patientIdMetadata = manifestRecord.getMetadataByKey(Metadata.Key.PATIENT_ID);
            if (PATIENT_IDS_FOR_SAME_MANIFEST_GENDER_MISMATCHES.contains(patientIdMetadata.getValue())) {
                Collection<ManifestRecord> allMatchingPatientIdRecordsExceptThisOne =
                        filterThisRecord(manifestRecord, patientIdToManifestRecords.get(manifestRecord.getValueByKey(
                                Metadata.Key.PATIENT_ID)));
                assertThat(manifestRecord.getManifestEvents(), hasSize(1));
                ManifestEvent manifestEvent = manifestRecord.getManifestEvents().get(0);
                assertThat(manifestEvent.getSeverity(), is(ManifestEvent.Severity.ERROR));
                assertThat(manifestEvent.getMessage(), containsString(manifestRecord
                        .buildMessageForConflictingRecords(allMatchingPatientIdRecordsExceptThisOne)));
            }
        }
    }

    @Test(dataProvider = "covidProcessTypeAndSampleKitInput")
     public void uploadManifestThatDuplicatesMatrixIDInSameManifest(
            ManifestSessionEjb.AccessioningProcessType accessioningProcessType, boolean fromSampleKit) throws Exception {
        ManifestSession manifestSession = uploadManifest(MANIFEST_FILE_DUPLICATE_MATRIX_IDS_SAME_SESSION,
                accessioningProcessType, fromSampleKit);
        assertThat(manifestSession, is(notNullValue()));
        assertThat(manifestSession.getRecords(), hasSize(8));

        ImmutableListMultimap<String, ManifestRecord> matrixIdToManifestRecords =
                buildMatrixIdToRecordMultimap(manifestSession);

        // The assert below is for size * 2 since all manifest records in question are in this manifest session.
        assertThat(manifestSession.getManifestEvents(),
                hasSize(SAMPE_IDS_FOR_SAME_DUPLICATE_MATRIX_IDS.size()));
        for (ManifestRecord manifestRecord : manifestSession.getRecords()) {
            Metadata patientIdMetadata = manifestRecord.getMetadataByKey(Metadata.Key.SAMPLE_ID);
            if (SAMPE_IDS_FOR_SAME_DUPLICATE_MATRIX_IDS.contains(patientIdMetadata.getValue())) {
                Collection<ManifestRecord> allMatchingPatientIdRecordsExceptThisOne =
                        filterThisRecord(manifestRecord, matrixIdToManifestRecords.get(manifestRecord.getValueByKey(
                                Metadata.Key.SAMPLE_ID)));
                assertThat(manifestRecord.getManifestEvents(), hasSize(1));
                ManifestEvent manifestEvent = manifestRecord.getManifestEvents().get(0);
                assertThat(manifestEvent.getSeverity(), is(ManifestEvent.Severity.ERROR));
                assertThat(manifestEvent.getMessage(), containsString(manifestRecord
                        .buildMessageForDuplicateMatrixIds(allMatchingPatientIdRecordsExceptThisOne)));
            }
        }
    }

    @DataProvider(name = "crspProcessTypeAndSampleKitInput")
    public Object[][] crspProcessTypeAndSampleKitInput() {
        // @formatter:off
        return new Object[][]{
                {ManifestSessionEjb.AccessioningProcessType.CRSP, false},
                {ManifestSessionEjb.AccessioningProcessType.CRSP, true},
        };
    }


    @Test(dataProvider = "crspProcessTypeAndSampleKitInput")
    public void uploadManifestThatMismatchesGenderInAnotherManifest(
            ManifestSessionEjb.AccessioningProcessType processType, boolean fromSampleKit) throws Exception {
        ResearchProject researchProject = createTestResearchProject();

        ManifestSession manifestSession1 =
                uploadManifest("manifest-upload/gender-mismatches-across-sessions/good-manifest-1.xlsx",
                        researchProject, processType, fromSampleKit);
        assertThat(manifestSession1, is(notNullValue()));
        assertThat(manifestSession1.getManifestEvents(), is(empty()));
        assertThat(manifestSession1.getRecords(), hasSize(NUM_RECORDS_IN_GOOD_MANIFEST));

        ImmutableListMultimap<String, ManifestRecord> session1RecordsByPatientId =
                buildPatientIdToManifestRecordsMultimap(manifestSession1);

        ManifestSession manifestSession2 =
                uploadManifest("manifest-upload/gender-mismatches-across-sessions/good-manifest-2.xlsx",
                        researchProject, ManifestSessionEjb.AccessioningProcessType.CRSP, false);
        assertThat(manifestSession2, is(notNullValue()));
        Set<String> expectedPatientIds = ImmutableSet.of("001-001", "005-005", "009-001");
        assertThat(manifestSession2.getManifestEvents(), hasSize(expectedPatientIds.size()));
        assertThat(manifestSession2.getRecords(), hasSize(NUM_RECORDS_IN_GOOD_MANIFEST));
        int observedMismatches = 0;
        for (ManifestRecord manifestRecord : manifestSession2.getRecords()) {
            assertThat(manifestRecord.isQuarantined(), is(false));
            String patientId = manifestRecord.getValueByKey(Metadata.Key.PATIENT_ID);
            if (expectedPatientIds.contains(patientId)) {
                assertThat(manifestRecord.getManifestEvents(), hasSize(1));
                ManifestEvent manifestEvent = manifestRecord.getManifestEvents().get(0);
                assertThat(manifestEvent.getSeverity(), is(ManifestEvent.Severity.ERROR));
                ImmutableList<ManifestRecord> mismatchedRecords =
                        session1RecordsByPatientId.get(manifestRecord.getValueByKey(Metadata.Key.PATIENT_ID));
                assertThat(manifestEvent.getMessage(),
                        containsString(manifestRecord.buildMessageForConflictingRecords(
                                mismatchedRecords)));
                observedMismatches++;
            }
        }
        assertThat(observedMismatches, is(equalTo(expectedPatientIds.size())));
    }

    private ImmutableListMultimap<String, ManifestRecord> buildPatientIdToManifestRecordsMultimap(
            ManifestSession manifestSession) {
        return Multimaps.index(manifestSession.getRecords(), new Function<ManifestRecord, String>() {
            @Override
            public String apply(ManifestRecord manifestRecord) {
                return manifestRecord.getValueByKey(Metadata.Key.PATIENT_ID);
            }
        });
    }

    /********************************************************************/
    /**  =======  Load session tests  ================================ **/
    /**
     * ****************************************************************
     */

    public void loadManifestSessionSuccess() {
        final long TEST_MANIFEST_SESSION_ID = ARBITRARY_MANIFEST_SESSION_ID;
        Mockito.when(manifestSessionDao.find(Mockito.anyLong())).then(new Answer<ManifestSession>() {
            @Override
            public ManifestSession answer(final InvocationOnMock invocation) throws Throwable {
                return new ManifestSession() {
                    @Override
                    public Long getManifestSessionId() {
                        return TEST_MANIFEST_SESSION_ID;
                    }
                };
            }
        });
        ManifestSession manifestSession = manifestSessionDao.find(TEST_MANIFEST_SESSION_ID);
        assertThat(manifestSession, is(notNullValue()));
        assertThat(manifestSession.getManifestSessionId(), is(TEST_MANIFEST_SESSION_ID));
    }

    public void loadManifestSessionFailure() {
        ManifestSession manifestSession = manifestSessionDao.find(ARBITRARY_MANIFEST_SESSION_ID);
        assertThat(manifestSession, is(nullValue()));
    }

    /********************************************************************/
    /**  =======  Accept upload tests ================================ **/
    /**
     * ****************************************************************
     */

    public void acceptUploadSessionNotFound() {
        ManifestSessionEjb ejb = new ManifestSessionEjb(manifestSessionDao, researchProjectDao, mercurySampleDao,
                labVesselDao, mockUserBean, bspUserList, jiraService, queueEjb, dnaQuantEnqueueOverride,
                labVesselFactory, covidManifestCopier);
        try {
            ejb.acceptManifestUpload(ARBITRARY_MANIFEST_SESSION_ID);
            Assert.fail();
        } catch (InformaticsServiceException e) {
            assertThat(e.getMessage(), matchesFormatString(ManifestSessionEjb.MANIFEST_SESSION_NOT_FOUND_FORMAT));
        }
    }

    @Test(dataProvider = "processTypeFileAndSampleKitInput")
    public void acceptUploadSuccessful(String filePath, ManifestSessionEjb.AccessioningProcessType processType,
                                       boolean withSampleKit, int numberOfRecords) throws Exception {
        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.UPLOADED, 20, withSampleKit,
                processType);
        holder.ejb.acceptManifestUpload(ARBITRARY_MANIFEST_SESSION_ID);
        for (ManifestRecord manifestRecord : holder.manifestSession.getRecords()) {
            assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.UPLOAD_ACCEPTED));
        }
    }

    private ManifestSessionAndEjbHolder buildSessionWithDuplicates(ManifestRecord.Status initialStatus,
                                                                   ManifestSessionEjb.AccessioningProcessType processType,
                                                                   boolean withSampleKit)
            throws Exception {
        ManifestSessionAndEjbHolder holder = buildHolderForSession(initialStatus, 20, withSampleKit,
                processType);

        for (Map.Entry<String, Integer> entry : SAMPLE_ID_TO_NUMBER_OF_COPIES_FOR_DUPLICATE_TESTS.entrySet()) {
            Integer numberOfCopies = entry.getValue();
            for (int i = 0; i < numberOfCopies; i++) {
                String collaboratorBarcode = entry.getKey();
                addRecord(holder, ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID, initialStatus,
                        ImmutableMap.of(Metadata.Key.SAMPLE_ID, collaboratorBarcode,Metadata.Key.BROAD_SAMPLE_ID, collaboratorBarcode),
                        EnumSet.of(Metadata.Key.BROAD_2D_BARCODE));
//                (!withSampleKit || processType == ManifestSessionEjb.AccessioningProcessType.COVID )?EnumSet.of(Metadata.Key.BROAD_2D_BARCODE):EnumSet.noneOf(Metadata.Key.class));
            }
        }

        return holder;
    }

    @Test(dataProvider = "processTypeFileAndSampleKitInput")
    public void acceptSessionWithDuplicatesInThisSession(String uploadFilePath,
                                                         ManifestSessionEjb.AccessioningProcessType processType,
                                                         boolean withSampleKit, int numberOfRecords) throws Exception {
        ManifestSessionAndEjbHolder holder = buildSessionWithDuplicates(ManifestRecord.Status.UPLOADED,
                processType, withSampleKit);

        ManifestSession manifestSession = holder.manifestSession;

        List<ManifestRecord> manifestRecordsMarkedAsDuplicates = new ArrayList<>();

        for (ManifestRecord manifestRecord : manifestSession.getRecords()) {
            assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.UPLOADED));
            if (manifestRecord.isQuarantined()) {
                manifestRecordsMarkedAsDuplicates.add(manifestRecord);
            } else {
                assertThat(manifestRecord.getManifestEvents(), is(empty()));
            }
        }
        assertThat(manifestRecordsMarkedAsDuplicates,
                hasSize(NUM_DUPLICATES_IN_MANIFEST_WITH_DUPLICATES_IN_SAME_SESSION));

        // Arbitrary ID, the DAO is programmed to return the same manifest session for any requested ID.
        holder.ejb.acceptManifestUpload(ARBITRARY_MANIFEST_SESSION_ID);

        for (ManifestRecord manifestRecord : manifestSession.getRecords()) {
            boolean shouldBeMarkedAsDuplicate = manifestRecordsMarkedAsDuplicates.contains(manifestRecord);
            assertThat(manifestRecord.isQuarantined(), is(shouldBeMarkedAsDuplicate));
            if (!shouldBeMarkedAsDuplicate) {
                assertThat(manifestRecord.getManifestEvents(), is(empty()));
                assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.UPLOAD_ACCEPTED));
            } else {
                // No new events should have been added.
                assertThat(manifestRecord.getManifestEvents(), hasSize(1));
            }
        }
    }

    private ManifestSessionAndEjbHolder buildMismatchedGenderSession(ManifestRecord.Status initialStatus,
                                                                     ManifestSessionEjb.AccessioningProcessType processType,
                                                                     boolean withSampleKit)
            throws Exception {

        ManifestSessionAndEjbHolder holder = buildHolderForSession(initialStatus, 20, withSampleKit,
                processType);

        addRecord(holder, ManifestRecord.ErrorStatus.MISMATCHED_GENDER, initialStatus, Metadata.Key.SAMPLE_ID,
                "03101947686", Metadata.Key.PATIENT_ID, "003-009", Metadata.Key.GENDER, FEMALE);
        addRecord(holder, ManifestRecord.ErrorStatus.MISMATCHED_GENDER, initialStatus, Metadata.Key.SAMPLE_ID,
                "03101989209", Metadata.Key.PATIENT_ID, "003-009", Metadata.Key.GENDER, MALE);
        addRecord(holder, ManifestRecord.ErrorStatus.MISMATCHED_GENDER, initialStatus, Metadata.Key.SAMPLE_ID,
                "03101231193", Metadata.Key.PATIENT_ID, "004-002", Metadata.Key.GENDER, MALE);
        addRecord(holder, ManifestRecord.ErrorStatus.MISMATCHED_GENDER, initialStatus, Metadata.Key.SAMPLE_ID,
                "03101067213", Metadata.Key.PATIENT_ID, "004-002", Metadata.Key.GENDER, FEMALE);
        addRecord(holder, ManifestRecord.ErrorStatus.MISMATCHED_GENDER, initialStatus, Metadata.Key.SAMPLE_ID,
                "03101752021", Metadata.Key.PATIENT_ID, "005-012", Metadata.Key.GENDER, FEMALE);
        addRecord(holder, ManifestRecord.ErrorStatus.MISMATCHED_GENDER, initialStatus, Metadata.Key.SAMPLE_ID,
                "03101752020", Metadata.Key.PATIENT_ID, "005-012", Metadata.Key.GENDER, MALE);

        return holder;
    }

    @Test(dataProvider = "crspProcessTypeAndSampleKitInput")
    public void acceptSessionWithMismatchedGendersThisSession(ManifestSessionEjb.AccessioningProcessType processType,
                                                              boolean withSampleKit) throws Exception {
        ManifestSessionAndEjbHolder holder = buildMismatchedGenderSession(ManifestRecord.Status.UPLOADED,
                processType, withSampleKit);
        ManifestSession manifestSession = holder.manifestSession;

        List<ManifestRecord> manifestRecordsWithMismatchedGenders = new ArrayList<>();

        for (ManifestRecord manifestRecord : manifestSession.getRecords()) {
            assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.UPLOADED));
            assertThat(manifestRecord.isQuarantined(), is(false));

            List<ManifestEvent> manifestEvents = manifestRecord.getManifestEvents();
            boolean hasManifestEvents = !manifestEvents.isEmpty();
            if (hasManifestEvents) {
                assertThat(manifestEvents, hasSize(1));
                assertThat(manifestEvents.get(0).getSeverity(), is(ManifestEvent.Severity.ERROR));
                manifestRecordsWithMismatchedGenders.add(manifestRecord);
            }
            String patientId = manifestRecord.getValueByKey(Metadata.Key.PATIENT_ID);
            assertThat(PATIENT_IDS_FOR_SAME_MANIFEST_GENDER_MISMATCHES.contains(patientId),
                    is(equalTo(hasManifestEvents)));
        }

        assertThat(manifestSession.getManifestEvents(), hasSize(6));

        // Arbitrary ID, the DAO is programmed to return the same manifest session for any requested ID.
        holder.ejb.acceptManifestUpload(ARBITRARY_MANIFEST_SESSION_ID);

        assertThat(manifestSession.getManifestEvents(), hasSize(6));

        for (ManifestRecord manifestRecord : manifestSession.getRecords()) {
            boolean shouldBeMarkedAsDuplicate = manifestRecordsWithMismatchedGenders.contains(manifestRecord);
            assertThat(manifestRecord.getManifestEvents().isEmpty(), is(!shouldBeMarkedAsDuplicate));
            assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.UPLOAD_ACCEPTED));

            if (shouldBeMarkedAsDuplicate) {
                // No new events should have been added.
                assertThat(manifestRecord.getManifestEvents(), hasSize(1));
            }
        }
    }

    @DataProvider(name = GOOD_MANIFEST_ACCESSION_SCAN_PROVIDER)
    public Object[][] goodManifestAccessionScanProvider() {
        return new Object[][]{
                // Good tube barcode should succeed.
                {GOOD_TUBE_BARCODE, true, ManifestSessionEjb.AccessioningProcessType.CRSP, false},
                // Good tube barcode should succeed.
                {GOOD_TUBE_BARCODE, true, ManifestSessionEjb.AccessioningProcessType.COVID, false},
                // Bad tube barcode should fail.
                {BAD_TUBE_BARCODE, false, ManifestSessionEjb.AccessioningProcessType.CRSP, false},
                // Bad tube barcode should fail.
                {BAD_TUBE_BARCODE, false, ManifestSessionEjb.AccessioningProcessType.COVID, false},
                // Good tube barcode should succeed.
//                {GOOD_TUBE_BARCODE, true, ManifestSessionEjb.AccessioningProcessType.CRSP, true},
                // Good tube barcode should succeed.
                {GOOD_TUBE_BARCODE, true, ManifestSessionEjb.AccessioningProcessType.COVID, true},
                // Bad tube barcode should fail.
                {BAD_TUBE_BARCODE, false, ManifestSessionEjb.AccessioningProcessType.CRSP, true},
                // Bad tube barcode should fail.
                {BAD_TUBE_BARCODE, false, ManifestSessionEjb.AccessioningProcessType.COVID, true},
        };
    }

    /********************************************************************/
    /**  =======  accession scan tests =============================== **/
    /**
     * ****************************************************************
     */

    @Test(dataProvider = GOOD_MANIFEST_ACCESSION_SCAN_PROVIDER)
    public void accessionScanGoodManifest(String tubeBarcode, boolean successExpected,
                                          ManifestSessionEjb.AccessioningProcessType processType, boolean withSampleKit) throws Exception {
        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.UPLOAD_ACCEPTED, 20,
                withSampleKit,
                processType);
        addRecord(holder, NO_ERROR, ManifestRecord.Status.UPLOAD_ACCEPTED,
                ImmutableMap.of(Metadata.Key.SAMPLE_ID, GOOD_TUBE_BARCODE),
                EnumSet.of(Metadata.Key.BROAD_2D_BARCODE,Metadata.Key.BROAD_SAMPLE_ID));

        Mockito.when(labVesselDao.findByIdentifier(Mockito.anyString())).thenReturn(testVessel);
        Mockito.when(mercurySampleDao.findBySampleKey(Mockito.anyString())).thenReturn(testVessel.getMercurySamples().iterator().next());

        try {
            holder.ejb.accessionScan(ARBITRARY_MANIFEST_SESSION_ID, tubeBarcode, tubeBarcode);
            if (!successExpected) {
                Assert.fail();
            }
        } catch (InformaticsServiceException e) {
            if (successExpected) {
                Assert.fail();
            }
            assertThat(e.getMessage(), containsString(ManifestRecord.ErrorStatus.NOT_IN_MANIFEST.getBaseMessage()));
        }

        ManifestSession manifestSession = holder.manifestSession;
        assertThat(manifestSession.getManifestEvents(), is(empty()));

        for (ManifestRecord manifestRecord : manifestSession.getRecords()) {
            Optional<String> collaboratorSampleId = Optional.ofNullable(manifestRecord.getValueByKey(Metadata.Key.SAMPLE_ID));
            if(!withSampleKit || processType == ManifestSessionEjb.AccessioningProcessType.COVID) {
                assertThat(collaboratorSampleId.isPresent(), is(true));
            }
            if (collaboratorSampleId.orElse("").equals(tubeBarcode)) {
                if(processType == ManifestSessionEjb.AccessioningProcessType.COVID) {

                    assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.SAMPLE_TRANSFERRED_TO_TUBE));
                } else {
                    assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.SCANNED));
                }
            } else {
                assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.UPLOAD_ACCEPTED));
            }
            assertThat(manifestRecord.getManifestEvents(), is(empty()));
        }
    }

    @Test(dataProvider = "processTypeFileAndSampleKitInput")
    public void accessionScanManifestWithDuplicates(String uploadFile,ManifestSessionEjb.AccessioningProcessType processType,
                                                    boolean withSampleKit, int numnberOfRecords) throws Exception {
        ManifestSessionAndEjbHolder holder = buildSessionWithDuplicates(ManifestRecord.Status.UPLOAD_ACCEPTED,
                processType, withSampleKit);

        try {
            // Take an arbitrary one of the duplicated sample IDs.
            String duplicatedSampleId = DUPLICATED_SAMPLE_IDS.iterator().next();

            holder.ejb.accessionScan(ARBITRARY_MANIFEST_SESSION_ID, duplicatedSampleId, duplicatedSampleId
            );
            Assert.fail();
        } catch (InformaticsServiceException e) {
            assertThat(e.getMessage(), containsString(ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID.getBaseMessage()));
        }
    }

    @Test(dataProvider = "crspProcessTypeAndSampleKitInput")
    public void accessionScanGenderMismatches(ManifestSessionEjb.AccessioningProcessType processType,
                                              boolean withSampleKit) throws Exception {
        ManifestSessionAndEjbHolder holder = buildMismatchedGenderSession(ManifestRecord.Status.UPLOAD_ACCEPTED,
                processType, withSampleKit);
        ManifestSession manifestSession = holder.manifestSession;

        // There should be twice as many manifest events as there are patient IDs with mismatched genders as all the
        // mismatches are in the same manifest.
        int EXPECTED_NUMBER_OF_EVENTS_ON_SESSION = PATIENT_IDS_FOR_SAME_MANIFEST_GENDER_MISMATCHES.size() * 2;
        assertThat(manifestSession.getManifestEvents(), hasSize(EXPECTED_NUMBER_OF_EVENTS_ON_SESSION));

        // Pick an arbitrary one of the mismatched gender patient IDs.
        String mismatchedGenderPatientId = PATIENT_IDS_FOR_SAME_MANIFEST_GENDER_MISMATCHES.iterator().next();
        // Find one of the records for this patient.
        ManifestRecord manifestRecord =
                manifestSession.getRecordWithMatchingValueForKey(Metadata.Key.PATIENT_ID, mismatchedGenderPatientId);

        assertThat(manifestRecord.getManifestEvents(), hasSize(1));
        assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.UPLOAD_ACCEPTED));

        ManifestEvent manifestEvent = manifestRecord.getManifestEvents().iterator().next();
        assertThat(manifestEvent.getMessage(), containsString(
                ManifestRecord.ErrorStatus.MISMATCHED_GENDER.getBaseMessage()));
        assertThat(manifestEvent.getSeverity(), is(ManifestEvent.Severity.ERROR));

        try {
            holder.ejb.accessionScan(ARBITRARY_MANIFEST_SESSION_ID,
                    manifestRecord.getValueByKey(Metadata.Key.SAMPLE_ID),
                    manifestRecord.getValueByKey(Metadata.Key.SAMPLE_ID));
            if(withSampleKit && processType != ManifestSessionEjb.AccessioningProcessType.COVID) {
                Assert.fail();
            }
        } catch (Exception e) {
            if(!withSampleKit || processType == ManifestSessionEjb.AccessioningProcessType.COVID) {
                Assert.fail();
            }
        }

        assertThat(manifestSession.getManifestEvents(), hasSize(EXPECTED_NUMBER_OF_EVENTS_ON_SESSION));
        if (withSampleKit && processType != ManifestSessionEjb.AccessioningProcessType.COVID) {
            assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.UPLOAD_ACCEPTED));
        } else {
            assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.SCANNED));
        }
    }

    @Test(dataProvider = "processTypeFileAndSampleKitInput")
    public void accessionScanDoubleScan(String fileUploadPath,ManifestSessionEjb.AccessioningProcessType processType,
                                        boolean withSampleKit, int numberOfRecords) throws Exception {
        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.UPLOAD_ACCEPTED, 20,
                withSampleKit, processType);
        ManifestSessionEjb ejb = holder.ejb;

        String goodTubeBarcode = "SAMPLE_ID_11";

        String targetTubeBarcode = goodTubeBarcode;
        String sourceBarcode = goodTubeBarcode;
        if(withSampleKit && processType != ManifestSessionEjb.AccessioningProcessType.COVID) {
            sourceBarcode = "BROAD_SAMPLE_ID_11";
            targetTubeBarcode = "BROAD_2D_BARCODE_11";
        } else if(processType == ManifestSessionEjb.AccessioningProcessType.COVID) {
            sourceBarcode = "SAMPLE_ID_11";
            targetTubeBarcode = "BROAD_2D_BARCODE_11";
        }
        Mockito.when(labVesselDao.findByIdentifier(Mockito.anyString())).thenReturn(testVessel);
        Mockito.when(mercurySampleDao.findBySampleKey(Mockito.anyString())).thenReturn(testVessel.getMercurySamples().iterator().next());
        ejb.accessionScan(ARBITRARY_MANIFEST_SESSION_ID, sourceBarcode, targetTubeBarcode);
        // The results of this are checked in accessionScanGoodManifest

        try {
            // Should fail on a second scan.
            ejb.accessionScan(ARBITRARY_MANIFEST_SESSION_ID, sourceBarcode, targetTubeBarcode);
            Assert.fail();
        } catch (InformaticsServiceException e) {
                assertThat(e.getMessage(),
                        containsString(ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_SCAN.getBaseMessage()));
        }
    }

    /********************************************************************/
    /**  =======  Prepare to close    ================================ **/
    /**
     * ****************************************************************
     * @param processType
     * @param withSampleKit
     */

    @Test(dataProvider = "crspProcessTypeAndSampleKitInput")
    public void prepareForSessionCloseGoodSession(ManifestSessionEjb.AccessioningProcessType processType,
                                                  boolean withSampleKit) throws Exception {

        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.SCANNED, 20,
                withSampleKit, processType);

        ManifestStatus sessionStatus = holder.ejb.getSessionStatus(ARBITRARY_MANIFEST_SESSION_ID);

        assertThat(sessionStatus.getErrorMessages(), is(empty()));
        assertThat(sessionStatus.getSamplesEligibleForAccessioningInManifest(), is(0));
        assertThat(sessionStatus.getSamplesSuccessfullyScanned(), is(20));
        assertThat(sessionStatus.getSamplesInManifest(), is(20));
    }

    @Test(dataProvider = "crspProcessTypeAndSampleKitInput")
    public void prepareForSessionCloseWithDuplicate(ManifestSessionEjb.AccessioningProcessType processType,
                                                    boolean withSampleKit) throws Exception {

        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.SCANNED, 20, withSampleKit,
                processType);

        addRecord(holder, ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID, ManifestRecord.Status.UPLOADED,
                ImmutableMap.of(Metadata.Key.SAMPLE_ID, GOOD_TUBE_BARCODE), EnumSet.of(Metadata.Key.BROAD_2D_BARCODE));

        ManifestStatus sessionStatus = holder.ejb.getSessionStatus(ARBITRARY_MANIFEST_SESSION_ID);

        assertThat(sessionStatus.getErrorMessages(), hasSize(0));
        assertThat(sessionStatus.getSamplesEligibleForAccessioningInManifest(), is(0));
        assertThat(sessionStatus.getSamplesSuccessfullyScanned(), is(20));
        assertThat(sessionStatus.getSamplesInManifest(), is(21));
    }

    @Test(dataProvider = "crspProcessTypeAndSampleKitInput")
    public void prepareForSessionCloseWithUnScanned(ManifestSessionEjb.AccessioningProcessType processType,
                                                    boolean withSampleKit) throws Exception {

        ManifestSessionAndEjbHolder holder = buildHolderForSession((withSampleKit|| processType == ManifestSessionEjb.AccessioningProcessType.COVID)?ManifestRecord.Status.SAMPLE_TRANSFERRED_TO_TUBE:ManifestRecord.Status.SCANNED,
                20,
                withSampleKit, processType);

        addRecord(holder, NO_ERROR, ManifestRecord.Status.UPLOAD_ACCEPTED,
                ImmutableMap.of(Metadata.Key.SAMPLE_ID, GOOD_TUBE_BARCODE), EnumSet.of(Metadata.Key.BROAD_2D_BARCODE));

        ManifestStatus sessionStatus = holder.ejb.getSessionStatus(ARBITRARY_MANIFEST_SESSION_ID);

        assertThat(sessionStatus.getErrorMessages(), hasSize(1));
        assertThat(sessionStatus, hasError(ManifestRecord.ErrorStatus.MISSING_SAMPLE));
        assertThat(sessionStatus.getSamplesEligibleForAccessioningInManifest(), is(1));
        assertThat(sessionStatus.getSamplesSuccessfullyScanned(), is(20));
        assertThat(sessionStatus.getSamplesInManifest(), is(21));
    }

    @Test(dataProvider = "processTypeFileAndSampleKitInput")
    public void prepareForSessionCloseWithUnScannedAndDuplicate(String filePath, ManifestSessionEjb.AccessioningProcessType processType,
                                                                boolean withSampleKit, Integer expectedManifestNumber) throws Exception {

        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.SCANNED, 20,
                withSampleKit, processType);

        addRecord(holder, NO_ERROR, ManifestRecord.Status.UPLOAD_ACCEPTED,
                ImmutableMap.of(Metadata.Key.SAMPLE_ID, GOOD_TUBE_BARCODE), EnumSet.of(Metadata.Key.BROAD_2D_BARCODE));

        addRecord(holder, ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID, ManifestRecord.Status.UPLOADED,
                ImmutableMap.of(Metadata.Key.SAMPLE_ID, GOOD_TUBE_BARCODE), EnumSet.of(Metadata.Key.BROAD_2D_BARCODE));

        ManifestStatus sessionStatus = holder.ejb.getSessionStatus(ARBITRARY_MANIFEST_SESSION_ID);

        assertThat(sessionStatus.getErrorMessages(), hasSize(1));
        assertThat(sessionStatus, hasError(ManifestRecord.ErrorStatus.MISSING_SAMPLE));
        assertThat(sessionStatus.getSamplesEligibleForAccessioningInManifest(), is(1));
        assertThat(sessionStatus.getSamplesSuccessfullyScanned(), is(20));
        assertThat(sessionStatus.getSamplesInManifest(), is(22));
    }

    @Test(dataProvider = "crspProcessTypeAndSampleKitInput")
    public void prepareForSessionCloseWithMismatchedGenders(ManifestSessionEjb.AccessioningProcessType processType,
                                                            boolean withSampleKit) throws Exception {

        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.SCANNED, 20,
                withSampleKit, processType);

        addRecord(holder, ManifestRecord.ErrorStatus.MISMATCHED_GENDER, ManifestRecord.Status.SCANNED,
                ImmutableMap.of(Metadata.Key.SAMPLE_ID, GOOD_TUBE_BARCODE), EnumSet.of(Metadata.Key.BROAD_2D_BARCODE));

        ManifestStatus sessionStatus = holder.ejb.getSessionStatus(ARBITRARY_MANIFEST_SESSION_ID);

        assertThat(sessionStatus.getErrorMessages(), hasSize(0));
        assertThat(sessionStatus.getSamplesEligibleForAccessioningInManifest(), is(0));
        assertThat(sessionStatus.getSamplesSuccessfullyScanned(), is(21));
        assertThat(sessionStatus.getSamplesInManifest(), is(21));
    }

    /********************************************************************/
    /**  =======  close    ================================ **/
    /**
     * ****************************************************************
     * @param processType
     * @param withSampleKit
     */

    @Test(dataProvider = "processTypeFileAndSampleKitInput")
    public void closeGoodManifest(String uploadFilePath, ManifestSessionEjb.AccessioningProcessType processType,
                                  boolean withSampleKit, int expectedRecordAmount) throws Exception {

        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.SCANNED, 20, withSampleKit,
                processType);

        String receiptKey = String.format("%s-%d", CreateFields.ProjectType.RECEIPT_PROJECT.getKeyPrefix(),
                ARBITRARY_MANIFEST_SESSION_ID);
        holder.ejb.updateReceiptInfo(ARBITRARY_MANIFEST_SESSION_ID, receiptKey);
        try {
            holder.ejb.closeSession(ARBITRARY_MANIFEST_SESSION_ID);
            if(withSampleKit && processType != ManifestSessionEjb.AccessioningProcessType.COVID) {
                Assert.fail("At this time, closing session for the CRSP flow without transfering tubes will result in an error that tubes have not been transferred");
            }
            assertThat(holder.manifestSession.getStatus(), is(ManifestSession.SessionStatus.COMPLETED));
            if (withSampleKit || processType == ManifestSessionEjb.AccessioningProcessType.COVID) {

                assertThat(holder.manifestSession.getManifestEvents(), is(not(empty())));
            } else {
                assertThat(holder.manifestSession.getManifestEvents(), is(empty()));
            }
            for (ManifestRecord manifestRecord : holder.manifestSession.getRecords()) {
                if (withSampleKit || processType == ManifestSessionEjb.AccessioningProcessType.COVID) {
                    assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.SCANNED));

                } else {
                    assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.ACCESSIONED));
                }
            }
        } catch (TubeTransferException e) {
            if(!withSampleKit || processType == ManifestSessionEjb.AccessioningProcessType.COVID) {
                Assert.fail("There should not be an exception at close if no sample kit or COVID");
            }
            e.printStackTrace();
        }


    }

    @Test(dataProvider = "processTypeFileAndSampleKitInput")
    public void closeManifestWithDuplicate(String uploadFilePath, ManifestSessionEjb.AccessioningProcessType processType,
                                           boolean withSampleKit, int expectedRecordAmount) throws Exception {

        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.SCANNED, expectedRecordAmount, withSampleKit,
                processType);
        String duplicateSampleId = GOOD_TUBE_BARCODE;
        addRecord(holder, ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID, ManifestRecord.Status.UPLOADED,
                ImmutableMap.of(Metadata.Key.SAMPLE_ID, duplicateSampleId), EnumSet.of(Metadata.Key.BROAD_2D_BARCODE));

        try {
            holder.ejb.closeSession(ARBITRARY_MANIFEST_SESSION_ID);
            if(withSampleKit && processType != ManifestSessionEjb.AccessioningProcessType.COVID) {
                Assert.fail("At this time, closing session for the CRSP flow without transfering tubes will result in an error that tubes have not been transferred");
            }

            assertThat(holder.manifestSession.getStatus(), is(ManifestSession.SessionStatus.COMPLETED));

            if(withSampleKit || processType == ManifestSessionEjb.AccessioningProcessType.COVID) {
                assertThat(holder.manifestSession.getManifestEvents(), hasSize(1+expectedRecordAmount));
            } else {
                assertThat(holder.manifestSession.getManifestEvents(), hasSize(1));
            }

            for (ManifestRecord manifestRecord : holder.manifestSession.getRecords()) {
                if (manifestRecord.getValueByKey(Metadata.Key.SAMPLE_ID).equals(duplicateSampleId)) {
                    assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.UPLOADED));
                } else {
                    if(processType == ManifestSessionEjb.AccessioningProcessType.COVID) {
                        assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.SCANNED));
                    } else {
                        assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.ACCESSIONED));
                    }
                }
            }
        } catch (Exception e) {
            if(!withSampleKit || processType == ManifestSessionEjb.AccessioningProcessType.COVID) {
                Assert.fail("At this time, closing session for the CRSP flow without transfering tubes will result in an error that tubes have not been transferred");
            }
        }
    }

    @Test(dataProvider = "processTypeFileAndSampleKitInput")
    public void closeManifestWithUnScannedRecord(String uploadFilePath, ManifestSessionEjb.AccessioningProcessType processType,
                                                 boolean withSampleKit, int expectedRecordAmount) throws Exception {
        ManifestSessionAndEjbHolder holder = buildHolderForSession(
                (processType == ManifestSessionEjb.AccessioningProcessType.COVID)?ManifestRecord.Status.SAMPLE_TRANSFERRED_TO_TUBE:ManifestRecord.Status.SCANNED,
                20,
                withSampleKit, processType);
        String unScannedBarcode = GOOD_TUBE_BARCODE;
        addRecord(holder, NO_ERROR, ManifestRecord.Status.UPLOAD_ACCEPTED,
                ImmutableMap.of(Metadata.Key.SAMPLE_ID, unScannedBarcode), EnumSet.of(Metadata.Key.BROAD_2D_BARCODE));

        try {
            holder.ejb.closeSession(ARBITRARY_MANIFEST_SESSION_ID);
            if(withSampleKit && processType != ManifestSessionEjb.AccessioningProcessType.COVID) {
                Assert.fail("At this time, closing session for the CRSP flow without transfering tubes will result in an error that tubes have not been transferred");
            }

            assertThat(holder.manifestSession.getStatus(), is(ManifestSession.SessionStatus.COMPLETED));

            assertThat(holder.manifestSession.getManifestEvents(), hasSize(1+((processType == ManifestSessionEjb.AccessioningProcessType.COVID)?20:0)));

            for (ManifestRecord manifestRecord : holder.manifestSession.getRecords()) {
                if (manifestRecord.getValueByKey(Metadata.Key.SAMPLE_ID).equals(unScannedBarcode)) {
                    assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.UPLOAD_ACCEPTED));
                    assertThat(manifestRecord.isQuarantined(), is(true));
                } else {
                    if(processType == ManifestSessionEjb.AccessioningProcessType.COVID) {
                        assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.SAMPLE_TRANSFERRED_TO_TUBE));
                    } else {
                        assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.ACCESSIONED));
                    }
                }
            }
        } catch (Exception e) {
            if(!withSampleKit || processType == ManifestSessionEjb.AccessioningProcessType.COVID) {
                Assert.fail("At this time, closing session for the CRSP flow without transfering tubes will result in an error that tubes have not been transferred");
            }

        }
    }

    @Test(dataProvider = "processTypeFileAndSampleKitInput")
    public void closeManifestWithDuplicateAndUnScannedRecords(String uploadFilePath, ManifestSessionEjb.AccessioningProcessType processType,
                                                              boolean withSampleKit, int expectedRecordAmount) throws Exception {

        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.SCANNED, 20, withSampleKit,
                processType);

        String unscannedBarcode = GOOD_TUBE_BARCODE;
        addRecord(holder, NO_ERROR, ManifestRecord.Status.UPLOAD_ACCEPTED,
                ImmutableMap.of(Metadata.Key.SAMPLE_ID, unscannedBarcode), EnumSet.of(Metadata.Key.BROAD_2D_BARCODE));

        String dupeSampleId = GOOD_TUBE_BARCODE + "dupe";
        addRecord(holder, ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID, ManifestRecord.Status.UPLOADED,
                ImmutableMap.of(Metadata.Key.SAMPLE_ID, dupeSampleId), EnumSet.of(Metadata.Key.BROAD_2D_BARCODE));

        try {
            holder.ejb.closeSession(ARBITRARY_MANIFEST_SESSION_ID);
            if(withSampleKit && processType != ManifestSessionEjb.AccessioningProcessType.COVID) {
                Assert.fail("At this time, closing session for the CRSP flow without transfering tubes will result in an error that tubes have not been transferred");
            }

            assertThat(holder.manifestSession.getStatus(), is(ManifestSession.SessionStatus.COMPLETED));

            if(withSampleKit || processType == ManifestSessionEjb.AccessioningProcessType.COVID) {
                assertThat(holder.manifestSession.getManifestEvents(), hasSize(holder.manifestSession.getRecords().size()));
            } else {
                assertThat(holder.manifestSession.getManifestEvents(), hasSize(2));
            }
            for (ManifestRecord manifestRecord : holder.manifestSession.getRecords()) {
                if (manifestRecord.getValueByKey(Metadata.Key.SAMPLE_ID).equals(unscannedBarcode)) {
                    assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.UPLOAD_ACCEPTED));
                    assertThat(manifestRecord.isQuarantined(), is(true));
                } else if (manifestRecord.getValueByKey(Metadata.Key.SAMPLE_ID).equals(dupeSampleId)) {
                    assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.UPLOADED));
                    assertThat(manifestRecord.isQuarantined(), is(true));
                } else {
                    if(processType == ManifestSessionEjb.AccessioningProcessType.COVID) {
                        assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.SCANNED));
                    } else {
                        assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.ACCESSIONED));
                    }
                }
            }
        } catch (Exception e) {
            if(!withSampleKit ||processType == ManifestSessionEjb.AccessioningProcessType.COVID) {
                Assert.fail("At this time, closing session for the CRSP flow without transfering tubes will result in an error that tubes have not been transferred");
            }
        }
    }

    @Test(dataProvider = "crspProcessTypeAndSampleKitInput")
    public void closeManifestWithMisMatchedGenderRecords(ManifestSessionEjb.AccessioningProcessType processType,
                                                         boolean withSampleKit) throws Exception {

        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.SCANNED, 20,
                withSampleKit, processType);

        String misMatch1Barcode = GOOD_TUBE_BARCODE + "BadGender1";
        addRecord(holder,
                ManifestRecord.ErrorStatus.MISMATCHED_GENDER, ManifestRecord.Status.SCANNED,
                ImmutableMap.of(Metadata.Key.SAMPLE_ID, misMatch1Barcode), EnumSet.of(Metadata.Key.BROAD_2D_BARCODE));

        String misMatch2Barcode = GOOD_TUBE_BARCODE + "BadGender2";
        addRecord(holder,
                ManifestRecord.ErrorStatus.MISMATCHED_GENDER, ManifestRecord.Status.SCANNED,
                ImmutableMap.of(Metadata.Key.SAMPLE_ID, misMatch2Barcode), EnumSet.of(Metadata.Key.BROAD_2D_BARCODE));

        try {
            holder.ejb.closeSession(ARBITRARY_MANIFEST_SESSION_ID);
            if(withSampleKit && processType != ManifestSessionEjb.AccessioningProcessType.COVID) {
                Assert.fail("At this time, closing session for the CRSP flow without transfering tubes will result in an error that tubes have not been transferred");
            }

            ManifestSession manifestSession = holder.manifestSession;
            assertThat(manifestSession.getStatus(), is(ManifestSession.SessionStatus.COMPLETED));

            assertThat(manifestSession.getManifestEvents(), hasSize(2));

            for (ManifestRecord manifestRecord : manifestSession.getRecords()) {
                assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.ACCESSIONED));
            }
        } catch (Exception e) {
            if(!withSampleKit || processType == ManifestSessionEjb.AccessioningProcessType.COVID) {
                Assert.fail("At this time, closing session for the CRSP flow without transfering tubes will result in an error that tubes have not been transferred");
            }

        }
    }

    /********************************************************************/
    /**  =======  Validate source test (Supports Ajax Call) ========== **/
    /**
     * ****************************************************************
     * @param processType
     * @param withSampleKit
     */

    @Test(dataProvider = "processTypeFileAndSampleKitInput")
    public void validateSourceOnCleanSession(String uploadFilePath, ManifestSessionEjb.AccessioningProcessType processType,
                                             boolean withSampleKit, int numberOfRecords) throws Exception {

        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.ACCESSIONED, 20, withSampleKit,
                processType);

        addRecord(holder, NO_ERROR, ManifestRecord.Status.ACCESSIONED,
                ImmutableMap.of(Metadata.Key.SAMPLE_ID, sourceForTransfer), EnumSet.of(Metadata.Key.BROAD_2D_BARCODE));

        ManifestRecord foundRecord =
                holder.ejb.validateSourceTubeForTransfer(ARBITRARY_MANIFEST_SESSION_ID, sourceForTransfer);

        assertThat(foundRecord.getValueByKey(Metadata.Key.SAMPLE_ID), is(equalTo(sourceForTransfer)));
        assertThat(foundRecord.getManifestEvents(), is(emptyCollectionOf(ManifestEvent.class)));
    }

    @Test(dataProvider = "crspProcessTypeAndSampleKitInput")
    public void validateSourceOnDuplicateRecord(ManifestSessionEjb.AccessioningProcessType processType,
                                                boolean withSampleKit) throws Exception {
        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.ACCESSIONED, 20,
                withSampleKit, processType);

        addRecord(holder, ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID, ManifestRecord.Status.UPLOADED,
                ImmutableMap.of(Metadata.Key.SAMPLE_ID, sourceForTransfer), EnumSet.of(Metadata.Key.BROAD_2D_BARCODE));

        try {
            holder.ejb.validateSourceTubeForTransfer(ARBITRARY_MANIFEST_SESSION_ID, sourceForTransfer);
            Assert.fail();
        } catch (Exception e) {
            assertThat(e.getMessage(), is(equalTo(ManifestRecord.ErrorStatus.PREVIOUS_ERRORS_UNABLE_TO_CONTINUE
                    .formatMessage(Metadata.Key.SAMPLE_ID, sourceForTransfer))));
        }
    }

    @Test(dataProvider = "processTypeFileAndSampleKitInput")
    public void validateSourceNotFoundInManifest(String uploadFilePath, ManifestSessionEjb.AccessioningProcessType processType,
                                                 boolean withSampleKit, int numberOfRecords) throws Exception {
        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.ACCESSIONED,
                20, withSampleKit, processType);

        try {
            holder.ejb.validateSourceTubeForTransfer(ARBITRARY_MANIFEST_SESSION_ID, sourceForTransfer);
            Assert.fail();
        } catch (Exception e) {
            assertThat(e.getMessage(),
                    is(equalTo(ManifestRecord.ErrorStatus.NOT_IN_MANIFEST.formatMessage(Metadata.Key.SAMPLE_ID,
                            sourceForTransfer))));
        }
    }

    @Test(dataProvider = "crspProcessTypeAndSampleKitInput")
    public void validateSourceOnMismatchedGenderRecord(ManifestSessionEjb.AccessioningProcessType processType,
                                                       boolean withSampleKit) throws Exception {

        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.ACCESSIONED, 20, withSampleKit,
                processType);

        addRecord(holder, ManifestRecord.ErrorStatus.MISMATCHED_GENDER, ManifestRecord.Status.ACCESSIONED,
                ImmutableMap.of(Metadata.Key.SAMPLE_ID, sourceForTransfer), EnumSet.of(Metadata.Key.BROAD_2D_BARCODE));

        ManifestRecord foundRecord =
                holder.ejb.validateSourceTubeForTransfer(ARBITRARY_MANIFEST_SESSION_ID, sourceForTransfer);

        assertThat(foundRecord.getValueByKey(Metadata.Key.SAMPLE_ID), is(equalTo(sourceForTransfer)));
        assertThat(foundRecord.getManifestEvents(), hasSize(1));
    }

    @Test(dataProvider = "processTypeFileAndSampleKitInput")
    public void validateSourceOnUnScannedRecord(String uploadFilePath, ManifestSessionEjb.AccessioningProcessType processType,
                                                boolean withSampleKit, int numberOfRecords) throws Exception {
        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.ACCESSIONED,
                20, withSampleKit, processType);

        addRecord(holder, ManifestRecord.ErrorStatus.MISSING_SAMPLE, ManifestRecord.Status.UPLOAD_ACCEPTED,
                ImmutableMap.of(Metadata.Key.SAMPLE_ID, sourceForTransfer), EnumSet.of(Metadata.Key.BROAD_2D_BARCODE));

        try {
            holder.ejb.validateSourceTubeForTransfer(ARBITRARY_MANIFEST_SESSION_ID, sourceForTransfer);
            Assert.fail();
        } catch (Exception e) {
            assertThat(e.getMessage(), is(equalTo(ManifestRecord.ErrorStatus.PREVIOUS_ERRORS_UNABLE_TO_CONTINUE
                    .formatMessage(Metadata.Key.SAMPLE_ID, sourceForTransfer))));
        }
    }

    /********************************************************************/
    /**  =======  Validate target sample tests ======================= **/
    /**
     * ****************************************************************
     */

    @Test(dataProvider = "crspProcessTypeAndSampleKitInput")
    public void validateValidTargetSample(ManifestSessionEjb.AccessioningProcessType processType,
                                          boolean withSampleKit) throws Exception {
        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.ACCESSIONED, 1,
                withSampleKit, processType);

        MercurySample foundSample = holder.ejb.findAndValidateTargetSample(TEST_SAMPLE_KEY);

        assertThat(foundSample.getSampleKey(), is(equalTo(TEST_SAMPLE_KEY)));

    }

    @Test(dataProvider = "crspProcessTypeAndSampleKitInput")
    public void validateTargetForBSPSample(ManifestSessionEjb.AccessioningProcessType processType,
                                           boolean withSampleKit) throws Exception {
        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.ACCESSIONED,
                1, withSampleKit, processType);

        try {
            holder.ejb.findAndValidateTargetSample(BSP_TEST_SAMPLE_KEY);
            Assert.fail();
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString(ManifestRecord.ErrorStatus.INVALID_TARGET
                    .formatMessage(ManifestSessionEjb.MERCURY_SAMPLE_KEY, BSP_TEST_SAMPLE_KEY)));
            assertThat(e.getMessage(), containsString(ManifestSessionEjb.SAMPLE_NOT_ELIGIBLE_FOR_CLINICAL_MESSAGE));
        }
    }

    @Test(dataProvider = "crspProcessTypeAndSampleKitInput")
    public void validateTargetForNotFoundSample(ManifestSessionEjb.AccessioningProcessType processType,
                                                boolean withSampleKit) throws Exception {
        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.ACCESSIONED,
                1, withSampleKit, processType);

        try {
            holder.ejb.findAndValidateTargetSample(TEST_SAMPLE_KEY + "BAD");
            Assert.fail();
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString(ManifestRecord.ErrorStatus.INVALID_TARGET
                    .formatMessage(ManifestSessionEjb.MERCURY_SAMPLE_KEY, TEST_SAMPLE_KEY + "BAD")));
            assertThat(e.getMessage(), containsString(ManifestSessionEjb.SAMPLE_NOT_FOUND_MESSAGE));
        }
    }

    /********************************************************************/
    /**  =======  Validate tube and Sample tests ===================== **/
    /**
     * ****************************************************************
     * @param processType
     * @param withSampleKit
     */

    @Test(dataProvider = "crspProcessTypeAndSampleKitInput")
    public void validateTargetTubeAndSampleOnValidRecord(ManifestSessionEjb.AccessioningProcessType processType,
                                                         boolean withSampleKit) throws Exception {
        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.ACCESSIONED,
                1, withSampleKit, processType);

        LabVessel foundVessel = holder.ejb.findAndValidateTargetSampleAndVessel(TEST_SAMPLE_KEY, TEST_VESSEL_LABEL);

        assertThat(foundVessel.getLabel(), is(equalTo(TEST_VESSEL_LABEL)));

        assertThat(foundVessel.getSampleNames(), hasItem(TEST_SAMPLE_KEY));
    }

    @Test(dataProvider = "crspProcessTypeAndSampleKitInput")
    public void validateTargetTubeAndSampleNotAssociated(ManifestSessionEjb.AccessioningProcessType processType,
                                                         boolean withSampleKit) throws Exception {

        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.ACCESSIONED,
                1, withSampleKit, processType);

        try {
            holder.ejb.findAndValidateTargetSampleAndVessel(TEST_SAMPLE_KEY_UNASSOCIATED, TEST_VESSEL_LABEL);
            Assert.fail();
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString(ManifestRecord.ErrorStatus.INVALID_TARGET
                    .formatMessage(ManifestSession.VESSEL_LABEL, TEST_VESSEL_LABEL)));
            assertThat(e.getMessage(), containsString(ManifestSessionEjb.UNASSOCIATED_TUBE_SAMPLE_MESSAGE));
        }
    }

    @Test(dataProvider = "crspProcessTypeAndSampleKitInput")
    public void validateTargetTubeWithNonRegisteredSample(ManifestSessionEjb.AccessioningProcessType processType,
                                                          boolean withSampleKit) throws Exception {

        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.ACCESSIONED, 1, withSampleKit,
                processType);

        try {
            holder.ejb.findAndValidateTargetSampleAndVessel(TEST_SAMPLE_KEY + "BAD", TEST_VESSEL_LABEL);
            Assert.fail();
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString(ManifestRecord.ErrorStatus.INVALID_TARGET
                    .formatMessage(ManifestSessionEjb.MERCURY_SAMPLE_KEY, TEST_SAMPLE_KEY + "BAD")));
            assertThat(e.getMessage(), containsString(ManifestSessionEjb.SAMPLE_NOT_FOUND_MESSAGE));
        }
    }

    @Test(dataProvider = "crspProcessTypeAndSampleKitInput")
    public void validateTargetTubeNotRegisteredWithRegisteredSample(
            ManifestSessionEjb.AccessioningProcessType processType, boolean withSampleKit) throws Exception {

        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.ACCESSIONED, 1, withSampleKit,
                processType);

        try {
            holder.ejb.findAndValidateTargetSampleAndVessel(TEST_SAMPLE_KEY, TEST_VESSEL_LABEL + "BAD");
            Assert.fail();
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString(ManifestRecord.ErrorStatus.INVALID_TARGET
                    .formatMessage(ManifestSession.VESSEL_LABEL, TEST_VESSEL_LABEL + "BAD")));
            assertThat(e.getMessage(), containsString(ManifestSessionEjb.VESSEL_NOT_FOUND_MESSAGE));
        }
    }

    @Test(dataProvider = "processTypeFileAndSampleKitInput")
    public void validateTargetTubeAlreadyAccessioned(String filePath,ManifestSessionEjb.AccessioningProcessType processType,
                                                     boolean withSampleKit, int numberOfRecords) throws Exception {

        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.ACCESSIONED, 1, withSampleKit,
                processType);

        try {
            holder.ejb.findAndValidateTargetSampleAndVessel(TEST_SAMPLE_ALREADY_TRANSFERRED,
                    TEST_VESSEL_LABEL_ALREADY_TRANSFERRED);
            Assert.fail();
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString(ManifestRecord.ErrorStatus.INVALID_TARGET.getBaseMessage()));
            assertThat(e.getMessage(), containsString(ManifestSessionEjb.VESSEL_USED_FOR_PREVIOUS_TRANSFER));
        }
    }

    /********************************************************************/
    /**  =======  Record Transfer Tests ============================== **/
    /**
     * ****************************************************************
     * @param processType
     * @param withSampleKit
     */

    @Test(dataProvider = "processTypeFileAndSampleKitInput")
    public void transferSourceValidRecord(String filePath,ManifestSessionEjb.AccessioningProcessType processType,
                                          boolean withSampleKit, int numberOfRecords) throws Exception {
        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.ACCESSIONED, 20, withSampleKit,
                processType);

        addRecord(holder, NO_ERROR, ManifestRecord.Status.ACCESSIONED,
                ImmutableMap.of(Metadata.Key.SAMPLE_ID, GOOD_TUBE_BARCODE), EnumSet.of(Metadata.Key.BROAD_2D_BARCODE));

        ManifestRecord usedRecord = holder.manifestSession.findRecordByKey(GOOD_TUBE_BARCODE,
                Metadata.Key.SAMPLE_ID);
        assertThat(usedRecord.getStatus(), is(equalTo(ManifestRecord.Status.ACCESSIONED)));

        MercurySample testSample = mercurySampleDao.findBySampleKey(TEST_SAMPLE_KEY);

        assertThat(testSample.getMetadata(), is(empty()));

        holder.ejb.updateReceiptInfo(ARBITRARY_MANIFEST_SESSION_ID, "RCT-1");

        holder.ejb.transferSample(ARBITRARY_MANIFEST_SESSION_ID, GOOD_TUBE_BARCODE, TEST_SAMPLE_KEY, TEST_VESSEL_LABEL);

        assertThat(usedRecord.getStatus(), is(equalTo(ManifestRecord.Status.SAMPLE_TRANSFERRED_TO_TUBE)));

        assertThat(testSample.getMetadata(), is(not(empty())));
    }

    @Test(dataProvider = "crspProcessTypeAndSampleKitInput")
    public void transferSourceRecordNotFound(ManifestSessionEjb.AccessioningProcessType processType,
                                             boolean withSampleKit) throws Exception {
        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.ACCESSIONED, 20, withSampleKit,
                processType);

        MercurySample testSample = mercurySampleDao.findBySampleKey(TEST_SAMPLE_KEY);

        assertThat(testSample.getMetadata(), is(empty()));

        holder.ejb.updateReceiptInfo(ARBITRARY_MANIFEST_SESSION_ID, "RCT-1");

        try {
            holder.ejb.transferSample(ARBITRARY_MANIFEST_SESSION_ID, GOOD_TUBE_BARCODE, TEST_SAMPLE_KEY,
                    TEST_VESSEL_LABEL);
            Assert.fail();
        } catch (Exception e) {
            assertThat(e.getMessage(),
                    containsString(ManifestRecord.ErrorStatus.NOT_IN_MANIFEST
                            .formatMessage(Metadata.Key.SAMPLE_ID, GOOD_TUBE_BARCODE)));
            assertThat(testSample.getMetadata(), is(empty()));
        }
    }

    @Test(dataProvider = "crspProcessTypeAndSampleKitInput")
    public void transferSourceToSampleNotFound(ManifestSessionEjb.AccessioningProcessType processType,
                                               boolean withSampleKit) throws Exception {
        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.ACCESSIONED, 20, withSampleKit,
                processType);

        addRecord(holder, NO_ERROR, ManifestRecord.Status.ACCESSIONED,
                ImmutableMap.of(Metadata.Key.SAMPLE_ID, GOOD_TUBE_BARCODE), EnumSet.of(Metadata.Key.BROAD_2D_BARCODE));

        ManifestRecord usedRecord = holder.manifestSession.findRecordByKey(GOOD_TUBE_BARCODE,
                Metadata.Key.SAMPLE_ID);
        assertThat(usedRecord.getStatus(), is(equalTo(ManifestRecord.Status.ACCESSIONED)));

        try {
            holder.ejb.transferSample(ARBITRARY_MANIFEST_SESSION_ID, GOOD_TUBE_BARCODE, TEST_SAMPLE_KEY + "BAD",
                    TEST_VESSEL_LABEL);
            Assert.fail();
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString(ManifestRecord.ErrorStatus.INVALID_TARGET
                    .formatMessage(ManifestSessionEjb.MERCURY_SAMPLE_KEY, TEST_SAMPLE_KEY + "BAD")));
            assertThat(e.getMessage(), containsString(ManifestSessionEjb.SAMPLE_NOT_FOUND_MESSAGE));
        }
    }

    @Test(dataProvider = "crspProcessTypeAndSampleKitInput")
    public void transferSourceToVesselNotFound(ManifestSessionEjb.AccessioningProcessType processType,
                                               boolean withSampleKit) throws Exception {
        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.ACCESSIONED, 20, withSampleKit,
                processType);

        addRecord(holder, NO_ERROR, ManifestRecord.Status.ACCESSIONED,
                ImmutableMap.of(Metadata.Key.SAMPLE_ID, GOOD_TUBE_BARCODE), EnumSet.of(Metadata.Key.BROAD_2D_BARCODE));

        ManifestRecord usedRecord = holder.manifestSession.findRecordByKey(GOOD_TUBE_BARCODE,
                Metadata.Key.SAMPLE_ID);
        assertThat(usedRecord.getStatus(), is(equalTo(ManifestRecord.Status.ACCESSIONED)));

        MercurySample testSample = mercurySampleDao.findBySampleKey(TEST_SAMPLE_KEY);

        assertThat(testSample.getMetadata(), is(empty()));

        try {
            holder.ejb.transferSample(ARBITRARY_MANIFEST_SESSION_ID, GOOD_TUBE_BARCODE, TEST_SAMPLE_KEY,
                    TEST_VESSEL_LABEL + "BAD");
            Assert.fail();
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString(ManifestRecord.ErrorStatus.INVALID_TARGET
                    .formatMessage(ManifestSession.VESSEL_LABEL, TEST_VESSEL_LABEL + "BAD")));
            assertThat(e.getMessage(), containsString(ManifestSessionEjb.VESSEL_NOT_FOUND_MESSAGE));
        }
    }

    @Test(dataProvider = "crspProcessTypeAndSampleKitInput")
    public void transferSourceToMisMatchedSampleAndVesselNotFound(
            ManifestSessionEjb.AccessioningProcessType processType, boolean withSampleKit) throws Exception {
        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.ACCESSIONED, 20, withSampleKit,
                processType);

        addRecord(holder, NO_ERROR, ManifestRecord.Status.ACCESSIONED,
                ImmutableMap.of(Metadata.Key.SAMPLE_ID, GOOD_TUBE_BARCODE), EnumSet.of(Metadata.Key.BROAD_2D_BARCODE));

        ManifestRecord usedRecord = holder.manifestSession.findRecordByKey(GOOD_TUBE_BARCODE,
                Metadata.Key.SAMPLE_ID);
        assertThat(usedRecord.getStatus(), is(equalTo(ManifestRecord.Status.ACCESSIONED)));

        MercurySample testSample = mercurySampleDao.findBySampleKey(TEST_SAMPLE_KEY_UNASSOCIATED);

        assertThat(testSample.getMetadata(), is(empty()));

        try {
            holder.ejb.transferSample(ARBITRARY_MANIFEST_SESSION_ID, GOOD_TUBE_BARCODE,
                    TEST_SAMPLE_KEY_UNASSOCIATED, TEST_VESSEL_LABEL);
            Assert.fail();
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString(ManifestRecord.ErrorStatus.INVALID_TARGET
                    .formatMessage(ManifestSession.VESSEL_LABEL, TEST_VESSEL_LABEL)));
            assertThat(e.getMessage(), containsString(ManifestSessionEjb.UNASSOCIATED_TUBE_SAMPLE_MESSAGE));
        }
    }

    @Test(dataProvider = "processTypeFileAndSampleKitInput")
    public void transferSourceQuarantinedRecord(String fileUpload,ManifestSessionEjb.AccessioningProcessType processType,
                                                boolean withSampleKit, int numberOfRecords) throws Exception {
        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.ACCESSIONED, 20, withSampleKit,
                processType);

        addRecord(holder, ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID, ManifestRecord.Status.UPLOADED,
                ImmutableMap.of(Metadata.Key.SAMPLE_ID, GOOD_TUBE_BARCODE), EnumSet.of(Metadata.Key.BROAD_2D_BARCODE));

        ManifestRecord usedRecord = holder.manifestSession.findRecordByKey(GOOD_TUBE_BARCODE,
                Metadata.Key.SAMPLE_ID);
        assertThat(usedRecord.getStatus(), is(equalTo(ManifestRecord.Status.UPLOADED)));

        MercurySample testSample = mercurySampleDao.findBySampleKey(TEST_SAMPLE_KEY);

        assertThat(testSample.getMetadata(), is(empty()));

        holder.ejb.updateReceiptInfo(ARBITRARY_MANIFEST_SESSION_ID, "RCT-1");

        try {
            holder.ejb.transferSample(ARBITRARY_MANIFEST_SESSION_ID, GOOD_TUBE_BARCODE, TEST_SAMPLE_KEY,
                    TEST_VESSEL_LABEL);
        } catch (Exception e) {
            assertThat(e.getMessage(),
                    containsString(ManifestRecord.ErrorStatus.PREVIOUS_ERRORS_UNABLE_TO_CONTINUE
                            .formatMessage(Metadata.Key.SAMPLE_ID, GOOD_TUBE_BARCODE)));
            assertThat(testSample.getMetadata(), is(empty()));
        }
    }

    @Test(dataProvider = "crspProcessTypeAndSampleKitInput")
    public void transferSourceMismatchedGenderRecord(ManifestSessionEjb.AccessioningProcessType processType,
                                                     boolean withSampleKit) throws Exception {
        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.ACCESSIONED, 20, withSampleKit,
                processType);

        addRecord(holder, ManifestRecord.ErrorStatus.MISMATCHED_GENDER, ManifestRecord.Status.ACCESSIONED,
                ImmutableMap.of(Metadata.Key.SAMPLE_ID, GOOD_TUBE_BARCODE), EnumSet.of(Metadata.Key.BROAD_2D_BARCODE));

        ManifestRecord usedRecord = holder.manifestSession.findRecordByKey(GOOD_TUBE_BARCODE,
                Metadata.Key.SAMPLE_ID);
        assertThat(usedRecord.getStatus(), is(equalTo(ManifestRecord.Status.ACCESSIONED)));

        MercurySample testSample = mercurySampleDao.findBySampleKey(TEST_SAMPLE_KEY);

        assertThat(testSample.getMetadata(), is(empty()));

        holder.ejb.updateReceiptInfo(ARBITRARY_MANIFEST_SESSION_ID, "RCT-1");

        holder.ejb.transferSample(ARBITRARY_MANIFEST_SESSION_ID, GOOD_TUBE_BARCODE, TEST_SAMPLE_KEY, TEST_VESSEL_LABEL);

        assertThat(usedRecord.getStatus(), is(equalTo(ManifestRecord.Status.SAMPLE_TRANSFERRED_TO_TUBE)));

        assertThat(testSample.getMetadata(), is(not(empty())));
    }

    @Test(dataProvider = "processTypeFileAndSampleKitInput")
    public void transferSourceGoodRecordUsedTarget(String fileUpload,ManifestSessionEjb.AccessioningProcessType processType,
                                                   boolean withSampleKit, int numberOfRecords) throws Exception {
        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.ACCESSIONED, 20, withSampleKit,
                processType);

        addRecord(holder, ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID, ManifestRecord.Status.UPLOADED,
                ImmutableMap.of(Metadata.Key.SAMPLE_ID, GOOD_TUBE_BARCODE), EnumSet.of(Metadata.Key.BROAD_2D_BARCODE));

        ManifestRecord usedRecord = holder.manifestSession.findRecordByKey(GOOD_TUBE_BARCODE,
                Metadata.Key.SAMPLE_ID);
        assertThat(usedRecord.getStatus(), is(equalTo(ManifestRecord.Status.UPLOADED)));

        MercurySample testSample = mercurySampleDao.findBySampleKey(TEST_SAMPLE_ALREADY_TRANSFERRED);

        assertThat(testSample.getMetadata(), is(empty()));

        try {
            holder.ejb.transferSample(ARBITRARY_MANIFEST_SESSION_ID, GOOD_TUBE_BARCODE, TEST_SAMPLE_ALREADY_TRANSFERRED,
                    TEST_VESSEL_LABEL_ALREADY_TRANSFERRED);
            Assert.fail();
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString(ManifestRecord.ErrorStatus.INVALID_TARGET.getBaseMessage()));
            assertThat(e.getMessage(), containsString(ManifestSessionEjb.VESSEL_USED_FOR_PREVIOUS_TRANSFER));
            assertThat(testSample.getMetadata(), is(empty()));
        }
    }

    public void testAccessionAndCloseWithSampleKit() throws Exception {

        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.UPLOAD_ACCEPTED, 5, true,
                ManifestSessionEjb.AccessioningProcessType.CRSP);

        assertThat(holder.manifestSession.getStatus(), is(ManifestSession.SessionStatus.ACCESSIONING));

        holder.ejb.updateReceiptInfo(ARBITRARY_MANIFEST_SESSION_ID, "RCT-1");

        Map<Metadata.Key, String> initialData = new HashMap<>();
        initialData.put(Metadata.Key.BROAD_SAMPLE_ID, TEST_SAMPLE_KEY);
        addRecord(holder, NO_ERROR, ManifestRecord.Status.UPLOAD_ACCEPTED,
                initialData, EnumSet.of(Metadata.Key.BROAD_2D_BARCODE).of(Metadata.Key.SAMPLE_ID));

        ManifestRecord record = holder.manifestSession.findRecordByKey(TEST_SAMPLE_KEY, Metadata.Key.BROAD_SAMPLE_ID);

        assertThat(record.getMetadataByKey(Metadata.Key.BROAD_2D_BARCODE), is(nullValue()));

        holder.ejb.accessionScan(ARBITRARY_MANIFEST_SESSION_ID, record.getMetadataByKey(
                Metadata.Key.BROAD_SAMPLE_ID).getValue(), TEST_VESSEL_LABEL);

        assertThat(record.getMetadataByKey(Metadata.Key.BROAD_2D_BARCODE).getValue(), is(equalTo(TEST_VESSEL_LABEL)));

        MercurySample testSample = mercurySampleDao.findBySampleKey(TEST_SAMPLE_KEY);
        assertThat(testSample.getMetadata(), is(empty()));

        holder.ejb.closeSession(ARBITRARY_MANIFEST_SESSION_ID);

        assertThat(testSample.getMetadata(), is(not(empty())));
    }

    public void testConvertToMercuryMetadata() throws Exception {
        Sample sample = ClinicalSampleTestFactory
                .createSample(ImmutableMap.of(Metadata.Key.SAMPLE_ID, SM_1));

        List<Metadata> metadata = ClinicalSampleFactory.toMercuryMetadata(sample);
        assertThat(metadata.size(), is(1));
        Metadata metadataItem = metadata.get(0);
        assertThat(metadataItem.getValue(), is(SM_1));
        assertThat(metadataItem.getKey(), is(Metadata.Key.SAMPLE_ID));
    }

    public void testConvertToManifestRecords() {
        Sample sample = ClinicalSampleTestFactory
                .createSample(ImmutableMap.of(Metadata.Key.SAMPLE_ID, SM_1));
        SampleData sampleData = sample.getSampleData().iterator().next();

        Collection<ManifestRecord> manifestRecords =
                ClinicalSampleFactory.toManifestRecords(Collections.singleton(sample));
        assertThat(manifestRecords.size(), is(1));
        ManifestRecord manifestRecord = manifestRecords.iterator().next();

        assertThat(manifestRecord.getValueByKey(Metadata.Key.SAMPLE_ID), is(sampleData.getValue()));
    }


    private void stubResearchProjectDaoFindReturnsNewObject() {
        Mockito.when(researchProjectDao.findByBusinessKey(Mockito.anyString()))
                .thenAnswer(new Answer<ResearchProject>() {
                    @Override
                    public ResearchProject answer(InvocationOnMock invocation) throws Throwable {
                        String id = (String) invocation.getArguments()[0];
                        ResearchProject researchProject = ResearchProjectTestFactory.createTestResearchProject(id);
                        researchProject
                                .setRegulatoryDesignation(ResearchProject.RegulatoryDesignation.CLINICAL_DIAGNOSTICS);
                        return researchProject;
                    }
                });
    }

    public void testAddDuplicateSamplesToManifestSession() throws Exception {
        stubResearchProjectDaoFindReturnsNewObject();
        List<Sample> samples = new ArrayList<>();
        Sample sample = ClinicalSampleTestFactory.createSample(ImmutableMap.of(Metadata.Key.SAMPLE_ID, SM_1,
                Metadata.Key.PATIENT_ID, PATIENT_1, Metadata.Key.BROAD_SAMPLE_ID, SM_1));
        samples.add(sample);
        sample = ClinicalSampleTestFactory.createSample(ImmutableMap.of(Metadata.Key.SAMPLE_ID, SM_1,
                Metadata.Key.PATIENT_ID, PATIENT_1, Metadata.Key.BROAD_SAMPLE_ID, SM_1));
        samples.add(sample);

        Mockito.when(mercurySampleDao.findMapIdToMercurySample(Mockito.anyCollectionOf(String.class)))
                .thenReturn(ImmutableMap.of(SM_1,new MercurySample(SM_1, MercurySample.MetadataSource.MERCURY)));


        ManifestSession manifestSession = manifestSessionEjb
                .createManifestSession(TEST_RESEARCH_PROJECT_KEY, TEST_SESSION_NAME, true, samples);
        assertThat(manifestSession.getRecords().size(), is(2));
    }

    public void testAddAccessionedSampleToManifestSession() throws Exception {
        stubResearchProjectDaoFindReturnsNewObject();
        List<Sample> samples = Arrays.asList(ClinicalSampleTestFactory
                .createSample(ImmutableMap.of(Metadata.Key.SAMPLE_ID, SM_1, Metadata.Key.PATIENT_ID, PATIENT_1,
                        Metadata.Key.BROAD_SAMPLE_ID, SM_1)));

        MercurySample sample = new MercurySample(SM_1, MercurySample.MetadataSource.MERCURY);
        BarcodedTube barcodedTube = new BarcodedTube("VesselFor" + SM_1, BarcodedTube.BarcodedTubeType.MatrixTube);
        LabEvent collaboratorTransferEvent =
                new LabEvent(LabEventType.COLLABORATOR_TRANSFER, new Date(), "thisLocation", 0l, 0l, "testprogram");
        barcodedTube.addInPlaceEvent(collaboratorTransferEvent);
        sample.addLabVessel(barcodedTube);

        Mockito.when(mercurySampleDao.findMapIdToMercurySample(Mockito.anyCollectionOf(String.class)))
                .thenReturn(Collections.singletonMap(SM_1, sample));

        try {
            manifestSessionEjb
                    .createManifestSession(TEST_RESEARCH_PROJECT_KEY, TEST_SESSION_NAME + "_NEW", true, samples);
            Assert.fail();
        } catch (InformaticsServiceException | TubeTransferException e) {
            assertThat(e.getCause(), instanceOf(TubeTransferException.class));
            assertThat(e.getLocalizedMessage(), containsString(ManifestSessionEjb.VESSEL_USED_FOR_PREVIOUS_TRANSFER));
        }
    }

    public void testAddBSPSampleToManifestSession() throws Exception {
        stubResearchProjectDaoFindReturnsNewObject();
        List<Sample> samples = Arrays.asList(ClinicalSampleTestFactory
                .createSample(ImmutableMap.of(Metadata.Key.SAMPLE_ID, SM_1, Metadata.Key.PATIENT_ID, PATIENT_1, Metadata.Key.BROAD_SAMPLE_ID, SM_1)));

        MercurySample sample = new MercurySample(SM_1, MercurySample.MetadataSource.BSP);
        BarcodedTube barcodedTube = new BarcodedTube("VesselFor" + SM_1, BarcodedTube.BarcodedTubeType.MatrixTube);

        sample.addLabVessel(barcodedTube);

        Mockito.when(mercurySampleDao.findMapIdToMercurySample(Mockito.anyCollectionOf(String.class)))
                .thenReturn(Collections.singletonMap(SM_1, sample));

        try {
            manifestSessionEjb
                    .createManifestSession(TEST_RESEARCH_PROJECT_KEY, TEST_SESSION_NAME + "_NEW", true, samples);
            Assert.fail();
        } catch (InformaticsServiceException e) {
            assertThat(e.getCause(), instanceOf(TubeTransferException.class));
            assertThat(e.getLocalizedMessage(), containsString(ManifestSessionEjb.SAMPLE_NOT_ELIGIBLE_FOR_CLINICAL_MESSAGE));
        }
    }


    @DataProvider(name = "metaDataProvider")
    public static Object[][] metaDataProvider() {
        return new Object[][]{
                {new HashMap<Metadata.Key, String>() {{ put(Metadata.Key.PERCENT_TUMOR, ""); put(Metadata.Key.BROAD_SAMPLE_ID, SM_1);}}},
                {new HashMap<Metadata.Key, String>() {{ put(Metadata.Key.PERCENT_TUMOR, null); put(Metadata.Key.BROAD_SAMPLE_ID, SM_1);}}}
        };
    }

    @Test(dataProvider = "metaDataProvider")
    public void testAddSampleWithProvidedMetadataToManifestSession(final Map<Metadata.Key, String> metadata)
            throws Exception {
        stubResearchProjectDaoFindReturnsNewObject();
        Sample sample = ClinicalSampleTestFactory.createSample(metadata);

        Mockito.when(mercurySampleDao.findMapIdToMercurySample(Mockito.anyCollectionOf(String.class)))
                .thenReturn(Collections.singletonMap(SM_1, new MercurySample(SM_1, MercurySample.MetadataSource.MERCURY)));

        ManifestSession manifestSession = manifestSessionEjb.createManifestSession(
                TEST_RESEARCH_PROJECT_KEY, TEST_SESSION_NAME, true, Collections.singletonList(sample));
        assertThat(manifestSession.getRecords().size(), is(1));
        for (ManifestRecord manifestRecord : manifestSession.getRecords()) {
            assertThat(manifestRecord.getMetadata().size(), is(1));
        }
    }

    /* ************************************************** *
     * createManifestSession tests
     * ************************************************** */

    /**
     * Test creation of a valid manifest session.
     */
    public void testCreateValidManifestWithSamples() {
        stubResearchProjectDaoFindReturnsNewObject();
        Collection<Sample> samples = Collections.singleton(
                ClinicalSampleTestFactory.createSample(Collections.singletonMap(Metadata.Key.BROAD_SAMPLE_ID, SM_1)));

        Mockito.when(mercurySampleDao.findMapIdToMercurySample(Mockito.anyCollectionOf(String.class)))
                .thenReturn(Collections.singletonMap(SM_1, new MercurySample(SM_1, MercurySample.MetadataSource.MERCURY)));

        ManifestSession manifestSession = manifestSessionEjb
                .createManifestSession(TEST_RESEARCH_PROJECT_KEY, TEST_SESSION_NAME, true, samples);

        assertThat(manifestSession.getResearchProject().getBusinessKey(), equalTo(TEST_RESEARCH_PROJECT_KEY));
        assertThat(manifestSession.getSessionName(), startsWith(TEST_SESSION_NAME));
        assertThat(manifestSession.isFromSampleKit(), is(true));
        assertThat(manifestSession.getUpdateData().getCreatedBy(), equalTo(testLabUser.getUserId()));
        assertThat(manifestSession.getRecords().size(), equalTo(samples.size()));
        ManifestRecord manifestRecord = manifestSession.getRecords().iterator().next();
        assertThat(manifestRecord.getValueByKey(Metadata.Key.BROAD_SAMPLE_ID), equalTo(SM_1));
    }

    /* ************************************************** *
     * createManifestSession tests
     * ************************************************** */

    /**
     * Test creation of a valid manifest session.
     */
    public void testCreateManifestWithBadSamples() {
        stubResearchProjectDaoFindReturnsNewObject();
        Collection<Sample> samples = Collections.singleton(
                ClinicalSampleTestFactory.createSample(Collections.singletonMap(Metadata.Key.BROAD_SAMPLE_ID, SM_1)));

        ManifestSession manifestSession = null;
        try {
            manifestSession = manifestSessionEjb
                    .createManifestSession(TEST_RESEARCH_PROJECT_KEY, TEST_SESSION_NAME, true, samples);
            Assert.fail();
        } catch (Exception e) {
            assertThat(e.getLocalizedMessage(),
                    containsString(ManifestSessionEjb.SAMPLE_IDS_ARE_NOT_FOUND_MESSAGE));
        }
    }

    /**
     * Test creation of a manifest session when the research project doesn't exist.
     */
    public void testCreateManifestManifestBadResearchProject() {
        String researchProjectName = "BadRP";
        try {
            manifestSessionEjb.createManifestSession(
                    researchProjectName, TEST_SESSION_NAME, true, Collections.<Sample>emptySet());
            Assert.fail();
        } catch (Exception e) {
            assertThat(e.getLocalizedMessage(),
                    is(String.format(ManifestSessionEjb.RESEARCH_PROJECT_NOT_FOUND_FORMAT, researchProjectName)));
        }
    }
}
