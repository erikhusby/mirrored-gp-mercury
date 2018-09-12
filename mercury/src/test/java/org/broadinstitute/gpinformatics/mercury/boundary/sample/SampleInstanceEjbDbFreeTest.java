package org.broadinstitute.gpinformatics.mercury.boundary.sample;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUtil;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.infrastructure.parsers.TableProcessor;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.SystemRouter;
import org.broadinstitute.gpinformatics.mercury.boundary.run.SolexaRunBean;
import org.broadinstitute.gpinformatics.mercury.control.dao.analysis.AnalysisTypeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.analysis.ReferenceSequenceDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.MolecularIndexingSchemeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.ReagentDesignDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.SampleInstanceEntityDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.SampleKitRequestDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.run.IlluminaSequencingRunFactory;
import org.broadinstitute.gpinformatics.mercury.control.sample.ExternalLibraryBarcodeUpdate;
import org.broadinstitute.gpinformatics.mercury.control.sample.ExternalLibraryProcessor;
import org.broadinstitute.gpinformatics.mercury.control.sample.ExternalLibraryProcessorEzPass;
import org.broadinstitute.gpinformatics.mercury.control.sample.ExternalLibraryProcessorNewTech;
import org.broadinstitute.gpinformatics.mercury.control.sample.VesselPooledTubesProcessor;
import org.broadinstitute.gpinformatics.mercury.control.vessel.JiraCommentUtil;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VarioskanParserTest;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.AnalysisType;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.ReferenceSequence;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceEntity;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleKitRequest;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MaterialType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.presentation.sample.WalkUpSequencing;
import org.broadinstitute.gpinformatics.mercury.test.BaseEventTest;
import org.broadinstitute.gpinformatics.mercury.test.builders.ExomeExpressShearingEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.HiSeq2500FlowcellEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.IceEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.LibraryConstructionEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.PicoPlatingEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.ProductionFlowcellPath;
import org.broadinstitute.gpinformatics.mercury.test.builders.QtpEntityBuilder;
import org.easymock.EasyMock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.parsers.TableProcessor.REQUIRED_VALUE_IS_MISSING;
import static org.broadinstitute.gpinformatics.mercury.boundary.sample.SampleInstanceEjbDbFreeTest.TestType.EZPASS;
import static org.broadinstitute.gpinformatics.mercury.boundary.sample.SampleInstanceEjbDbFreeTest.TestType.MULTIORGANISM;
import static org.broadinstitute.gpinformatics.mercury.boundary.sample.SampleInstanceEjbDbFreeTest.TestType.NONPOOLED;
import static org.broadinstitute.gpinformatics.mercury.boundary.sample.SampleInstanceEjbDbFreeTest.TestType.POOLEDTUBE;
import static org.broadinstitute.gpinformatics.mercury.test.LabEventTest.FCT_TICKET;
import static org.mockito.Matchers.anyString;

@Test(groups = TestGroups.DATABASE_FREE)
public class SampleInstanceEjbDbFreeTest extends BaseEventTest {
    private final boolean OVERWRITE = true;

    private LabVesselDao labVesselDao = Mockito.mock(LabVesselDao.class);
    private SampleInstanceEntityDao sampleInstanceEntityDao = Mockito.mock(SampleInstanceEntityDao.class);
    private MolecularIndexingSchemeDao molecularIndexingSchemeDao = Mockito.mock(MolecularIndexingSchemeDao.class);
    private MercurySampleDao mercurySampleDao = Mockito.mock(MercurySampleDao.class);
    private ReagentDesignDao reagentDesignDao = Mockito.mock(ReagentDesignDao.class);
    private SampleDataFetcher sampleDataFetcher = Mockito.mock(SampleDataFetcher.class);
    private JiraService jiraService = Mockito.mock(JiraService.class);
    private ReferenceSequenceDao referenceSequenceDao = Mockito.mock(ReferenceSequenceDao.class);
    private SampleKitRequestDao sampleKitRequestDao = Mockito.mock(SampleKitRequestDao.class);
    private AnalysisTypeDao analysisTypeDao = Mockito.mock(AnalysisTypeDao.class);

    enum TestType {EZPASS, NONPOOLED, POOLED, MULTIORGANISM, POOLEDTUBE, WALKUP};

    @Test
    public void testWalkupSequencing() throws Exception {
        SampleInstanceEjb sampleInstanceEjb = setMocks(TestType.WALKUP);
        MessageCollection messages = new MessageCollection();
        sampleInstanceEjb.verifyAndPersistSubmission(new WalkUpSequencing() {{
            setLibraryName("TEST_LIBRARY");
            setTubeBarcode("TEST_BARCODE");
            setReadType("TEST_READ_TYPE");
            setAnalysisType("TEST");
            setEmailAddress("TEST@TEST.COM");
            setLabName("TEST lab");
            setBaitSetName("TEST_BAIT");
            setReadType("Paried End");
        }}, messages);

        Assert.assertFalse(messages.hasErrors(), StringUtils.join(messages.getErrors(), "; "));
    }

    @Test
    public void parseExternalLibarayEZFail() throws Exception {
        String file = "testdata/ExternalLibraryEZFailTest.xlsx";
        SampleInstanceEjb sampleInstanceEjb = setMocks(EZPASS);
        MessageCollection messageCollection = new MessageCollection();

        List<SampleInstanceEntity> entities = sampleInstanceEjb.doExternalUpload(
                VarioskanParserTest.getSpreadsheet(file), OVERWRITE,
                new ExternalLibraryProcessorEzPass(), messageCollection, null, false);

        Assert.assertTrue(CollectionUtils.isEmpty(entities));
        Assert.assertEquals(messageCollection.getErrors().size(), 2);
        Assert.assertEquals(messageCollection.getWarnings().size(), 0,
                StringUtils.join(messageCollection.getWarnings(), "; "));

        errorIfMissing(messageCollection.getErrors(), file, String.format(TableProcessor.DUPLICATE_HEADER,
                "Virtual GSSR ID"));
        errorIfMissing(messageCollection.getErrors(), file, String.format(TableProcessor.DUPLICATE_HEADER,
                "SQUID Project"));
    }

    @Test
    public void testExternalLibraryEZPass() throws Exception {
        String file = "testdata/ExternalLibraryEZPassTest.xlsx";
        SampleInstanceEjb sampleInstanceEjb = setMocks(EZPASS);
        MessageCollection messageCollection = new MessageCollection();

        List<SampleInstanceEntity> entities = sampleInstanceEjb.doExternalUpload(
                VarioskanParserTest.getSpreadsheet(file), OVERWRITE,
                new ExternalLibraryProcessorEzPass(), messageCollection, null, false);

        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertEquals(messageCollection.getInfos().iterator().next(),
                String.format(SampleInstanceEjb.IS_SUCCESS, entities.size()),
                StringUtils.join(messageCollection.getInfos(), "; "));
        Assert.assertFalse(entities.isEmpty());

        SampleKitRequest sampleKitRequest = entities.get(0).getSampleKitRequest();
        Assert.assertEquals(sampleKitRequest.getFirstName(), "David");
        Assert.assertEquals(sampleKitRequest.getLastName(), "W");
        Assert.assertEquals(sampleKitRequest.getOrganization(), "Broad");
        Assert.assertEquals(sampleKitRequest.getAddress(), "Charles St, Room 2137B");
        Assert.assertEquals(sampleKitRequest.getCity(), "Cambridge");
        Assert.assertEquals(sampleKitRequest.getState(), "Ma");
        Assert.assertEquals(sampleKitRequest.getPostalCode(), "2215");
        Assert.assertEquals(sampleKitRequest.getCountry(), "USA");
        Assert.assertEquals(sampleKitRequest.getPhone(), "718-234-5510");
        Assert.assertEquals(sampleKitRequest.getEmail(), "test@test.com");
        Assert.assertNull(sampleKitRequest.getCommonName());
        Assert.assertEquals(sampleKitRequest.getGenus(), "G");
        Assert.assertEquals(sampleKitRequest.getSpecies(), "S");
        Assert.assertNull(sampleKitRequest.getIrbApprovalRequired());

        for (int i = 0; i < entities.size(); ++i) {
            SampleInstanceEntity entity = entities.get(i);
            Assert.assertEquals(entity.getSampleKitRequest(), sampleKitRequest);

            String libraryName = select(i, "Lib-MOCK.FSK1.A", "Lib-MOCK.FSK1.B", "Lib-MOCK.FSK1.A2", "Lib-MOCK.FSK1.C");
            Assert.assertEquals(entity.getSampleLibraryName(), libraryName);

            Assert.assertEquals(entity.getSequencerModel().name(), select(i, "MiSeqFlowcell", "HiSeqX10Flowcell",
                    "NovaSeqS4Flowcell", "NextSeqFlowcell"));

            MercurySample mercurySample = entity.getMercurySample();
            Assert.assertEquals(mercurySample.getSampleKey(), select(i, "917994.0", "917994.1", "917994.0", "917994.2"));

            Assert.assertEquals(mercurySample.getMetadataSource(), MercurySample.MetadataSource.MERCURY);
            Map<Metadata.Key, String> metadataMap = new HashMap<>();
            for (Metadata metadata : mercurySample.getMetadata()) {
                metadataMap.put(metadata.getKey(), metadata.getStringValue());
            }
            Assert.assertEquals(metadataMap.get(Metadata.Key.SAMPLE_ID),
                    select(i, "MOCK.FSK1.A", "MOCK.FSK1.B", "MOCK.FSK1.A", "MOCK.FSK1.C")); // from Collaborator Sample Id
            Assert.assertEquals(metadataMap.get(Metadata.Key.PATIENT_ID), select(i, "MOCK1", "MOCK2", "MOCK1", "MOCK1"));
            Assert.assertEquals(metadataMap.get(Metadata.Key.SPECIES), "G S");
            Assert.assertEquals(metadataMap.get(Metadata.Key.GENDER), select(i, "M", null, "M", null));
            if (i == 0) {
                Assert.assertEquals(entity.getMolecularIndexingScheme().getName(), "Illumina_P5-Bipof_P7-Dihib");
            } else {
                Assert.assertNull(entity.getMolecularIndexingScheme());
            }
            Assert.assertEquals(metadataMap.get(Metadata.Key.MATERIAL_TYPE), "DNA");
            Assert.assertTrue(StringUtils.isBlank(metadataMap.get(Metadata.Key.BROAD_PARTICIPANT_ID)));
            Assert.assertNull(metadataMap.get(Metadata.Key.LSID));

            LabVessel tube = entity.getLabVessel();
            Assert.assertEquals(tube.getLabel(), select(i, "E0098972718", "E0098972719", "E0098972720", "E0098972720"));
            Assert.assertEquals(tube.getVolume(), new BigDecimal(select(i, "96.00", "97.00", "98.00", "98.00")));
            Assert.assertEquals(tube.getConcentration(), new BigDecimal(select(i, "7.40", "8.00", "9.00", "9.00")));
            Assert.assertTrue(tube.getMercurySamples().contains(mercurySample), "index " + i);
            Assert.assertEquals(entity.getLibraryType(), "WholeGenomeShotgun");
            Assert.assertEquals(entity.getAggregationParticle(), select(i, "G96213", "G96214", "G96227", "G96215"));
            Assert.assertEquals(entity.getNumberLanes(), 1);
            Assert.assertEquals(entity.getComments(), select(i, "", "sample info 1", "analysis info 1", "asi4; aaai4"));
            Assert.assertEquals(entity.getPooled(), new Boolean(select(i, "false", "false", "true", "true")));
            Assert.assertEquals(entity.getReferenceSequence().getName().split("\\|")[0], "Homo_sapiens_RP11-78M2");
        }
    }

    @Test
    public void testExternalLibraryMultiOrganism() throws Exception {
        String file = "testdata/ExternalLibraryMultiOrganismTest.xlsx";
        SampleInstanceEjb sampleInstanceEjb = setMocks(MULTIORGANISM);
        MessageCollection messageCollection = new MessageCollection();

        List<SampleInstanceEntity> entities = sampleInstanceEjb.doExternalUpload(
                VarioskanParserTest.getSpreadsheet(file), OVERWRITE,
                new ExternalLibraryProcessorNewTech(), messageCollection, null, false);

        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertTrue(messageCollection.getInfos().iterator().next()
                        .startsWith(String.format(SampleInstanceEjb.IS_SUCCESS, entities.size())),
                StringUtils.join(messageCollection.getInfos(), "; "));

        Assert.assertEquals(entities.size(), 3);

        SampleKitRequest sampleKitRequest = entities.get(0).getSampleKitRequest();
        Assert.assertEquals(sampleKitRequest.getFirstName(), "Charlie");
        Assert.assertEquals(sampleKitRequest.getLastName(), "Delta");
        Assert.assertEquals(sampleKitRequest.getOrganization(), "Epsilon");
        Assert.assertEquals(sampleKitRequest.getAddress(), "Fox Trot");
        Assert.assertEquals(sampleKitRequest.getCity(), "Cambridge");
        Assert.assertEquals(sampleKitRequest.getState(), "MA");
        Assert.assertEquals(sampleKitRequest.getPostalCode(), "02142");
        Assert.assertEquals(sampleKitRequest.getCountry(), "USA");
        Assert.assertEquals(sampleKitRequest.getPhone(), "617-423-1234");
        Assert.assertEquals(sampleKitRequest.getEmail(), "none@none.com");
        Assert.assertEquals(sampleKitRequest.getCommonName(), "(leave blank for multi-organism submission)");
        Assert.assertEquals(sampleKitRequest.getGenus(), "see Organism column for genus");
        Assert.assertEquals(sampleKitRequest.getSpecies(), "see Organism column for species");
        Assert.assertEquals(sampleKitRequest.getIrbApprovalRequired(), "Y");

        for (int i = 0; i < entities.size(); ++i) {
            SampleInstanceEntity entity = entities.get(i);

            Assert.assertEquals(entity.getSampleKitRequest(), sampleKitRequest);

            String libraryName = select(i, "4076255991TEST", "4076255992TEST", "4076255993TEST");
            Assert.assertEquals(entity.getSampleLibraryName(), libraryName);

            Assert.assertTrue(entity.getSequencerModel().getDisplayName().startsWith("HiSeq X 10"),
                    entity.getSequencerModel().getDisplayName());

            MercurySample mercurySample = entity.getMercurySample();
            Assert.assertNotNull(mercurySample, libraryName);
            Assert.assertEquals(mercurySample.getSampleKey(), libraryName);

            Assert.assertEquals(mercurySample.getMetadataSource(), MercurySample.MetadataSource.MERCURY);
            Map<Metadata.Key, String> metadataMap = new HashMap<>();
            for (Metadata metadata : mercurySample.getMetadata()) {
                metadataMap.put(metadata.getKey(), metadata.getStringValue());
            }
            Assert.assertEquals(metadataMap.get(Metadata.Key.SAMPLE_ID), select(i,
                    "SampleColab20", "SampleColab21", "SampleColab22"));
            Assert.assertEquals(metadataMap.get(Metadata.Key.PATIENT_ID), select(i, "hh", "ii", "jj"));
            Assert.assertEquals(metadataMap.get(Metadata.Key.SPECIES), select(i, "human", "humanoid", "sub-human"));
            Assert.assertEquals(metadataMap.get(Metadata.Key.GENDER), select(i, "M", "F", "M"));
            Assert.assertEquals(metadataMap.get(Metadata.Key.MATERIAL_TYPE), "DNA");
            Assert.assertTrue(StringUtils.isBlank(metadataMap.get(Metadata.Key.BROAD_PARTICIPANT_ID)));
            Assert.assertNull(metadataMap.get(Metadata.Key.LSID));

            LabVessel tube = entity.getLabVessel();
            Assert.assertEquals(tube.getLabel(), libraryName);
            Assert.assertEquals(tube.getVolume(), new BigDecimal(select(i, "443.00", "444.00", "445.00")));
            Assert.assertEquals(tube.getConcentration(), new BigDecimal(select(i, "67.00", "68.00", "69.00")));
            Assert.assertTrue(tube.getMercurySamples().contains(mercurySample), libraryName);

            Assert.assertEquals(entity.getReadLength().intValue(), 76);
            Assert.assertEquals(entity.getLibraryType(), "WholeGenomeShotgun");
            Assert.assertEquals(entity.getAggregationParticle(), "Microsporidia_RNASeq_Sanscrainte");
            Assert.assertEquals(entity.getAnalysisType().getBusinessKey(), "WholeGenomeShotgun.Resequencing");
            Assert.assertEquals(entity.getNumberLanes(), new int[]{2, 2, 4}[i]);
            Assert.assertEquals(entity.getComments(),
                    select(i, "more info; CCLF", "more inf 2; CCLF", "more inf3; CCLF"));
            Assert.assertTrue(entity.getPooled());
            Assert.assertEquals(entity.getReferenceSequence().getName(), "Homo_sapiens_assembly19");

            if (i < 2) {
                Assert.assertNull(entity.getMolecularIndexingScheme());
            } else {
                Assert.assertEquals(entity.getMolecularIndexingScheme().getName(), "Illumina_P5-Lanah_P7-Cehih");
            }
        }
    }

    @Test
    public void testExternalLibraryPooled() throws Exception {
        String file = "testdata/ExternalLibraryPooledTest.xlsx";
        SampleInstanceEjb sampleInstanceEjb = setMocks(TestType.POOLED);
        MessageCollection messageCollection = new MessageCollection();

        List<SampleInstanceEntity> entities = sampleInstanceEjb.doExternalUpload(
                VarioskanParserTest.getSpreadsheet(file), OVERWRITE,
                new ExternalLibraryProcessorNewTech(), messageCollection, null, false);

        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertTrue(messageCollection.getInfos().iterator().next()
                .startsWith(String.format(SampleInstanceEjb.IS_SUCCESS, 2)),
                StringUtils.join(messageCollection.getInfos(), "; "));

        Assert.assertEquals(entities.size(), 2);

        SampleKitRequest sampleKitRequest = entities.get(0).getSampleKitRequest();
        // The other header value row fields have already been tested since it's shared code
        // in HeaderValueRowTableProcessor and ExternalLibraryProcessor.
        Assert.assertEquals(sampleKitRequest.getIrbApprovalRequired(), "N");

        for (int i = 0; i < entities.size(); ++i) {
            SampleInstanceEntity entity = entities.get(i);

            Assert.assertEquals(entity.getSampleKitRequest(), sampleKitRequest);

            String libraryName = select(i, "4442SFP6", "4442SFP7");
            Assert.assertEquals(entity.getSampleLibraryName(), libraryName);

            Assert.assertTrue(entity.getSequencerModel().getDisplayName().startsWith("NovaSeq"),
                    entity.getSequencerModel().getDisplayName());

            MercurySample mercurySample = entity.getMercurySample();
            Assert.assertNotNull(mercurySample, libraryName);
            Assert.assertEquals(mercurySample.getSampleKey(), libraryName);

            Assert.assertEquals(mercurySample.getMetadataSource(), MercurySample.MetadataSource.MERCURY);
            Map<Metadata.Key, String> metadataMap = new HashMap<>();
            for (Metadata metadata : mercurySample.getMetadata()) {
                metadataMap.put(metadata.getKey(), metadata.getStringValue());
            }
            Assert.assertEquals(metadataMap.get(Metadata.Key.SAMPLE_ID), select(i, "DDDSS2244", "DDDSS2245"));
            Assert.assertEquals(metadataMap.get(Metadata.Key.PATIENT_ID), select(i, "Patient X", "Patient Y"));
            Assert.assertEquals(metadataMap.get(Metadata.Key.SPECIES), "Test Genus Test Species");
            Assert.assertEquals(metadataMap.get(Metadata.Key.GENDER), select(i, "F", ""));
            Assert.assertEquals(metadataMap.get(Metadata.Key.MATERIAL_TYPE), "DNA");
            Assert.assertTrue(StringUtils.isBlank(metadataMap.get(Metadata.Key.BROAD_PARTICIPANT_ID)));
            Assert.assertNull(metadataMap.get(Metadata.Key.LSID));

            LabVessel tube = entity.getLabVessel();
            Assert.assertEquals(tube.getLabel(), libraryName);
            Assert.assertEquals(tube.getVolume(), new BigDecimal("77.00"));
            Assert.assertEquals(tube.getConcentration(), new BigDecimal("100.00"));
            Assert.assertTrue(tube.getMercurySamples().contains(mercurySample), libraryName);

            Assert.assertEquals(entity.getReadLength().intValue(), 151);
            Assert.assertEquals(entity.getLibraryType(), "WholeGenomeShotgun");
            Assert.assertEquals(entity.getAggregationParticle(), "Microsporidia_RNASeq_Sanscrainte");
            Assert.assertEquals(entity.getAnalysisType().getBusinessKey(), "WholeGenomeShotgun.Resequencing");
            Assert.assertEquals(entity.getNumberLanes(), new int[]{3, 5}[i]);
            Assert.assertEquals(entity.getComments(),
                    select(i, "Some info; Sarah Youngs group", "Sarah Youngs group"));
            Assert.assertTrue(entity.getPooled());
            Assert.assertEquals(entity.getReferenceSequence().getName(), "Plasmodium_falciparum_3D7");
            Assert.assertEquals(entity.getInsertSize(), select(i, "225-300", "1000-1000"));

            if (i == 0) {
                Assert.assertEquals(entity.getMolecularIndexingScheme().getName(), "Illumina_P5-Lanah_P7-Caber");
            } else {
                Assert.assertNull(entity.getMolecularIndexingScheme());
            }
        }
    }

    @Test
    public void testExternalLibraryNonPooled() throws Exception {
        String file = "testdata/ExternalLibraryNONPooledTest.xlsx";
        SampleInstanceEjb sampleInstanceEjb = setMocks(NONPOOLED);
        MessageCollection messageCollection = new MessageCollection();

        List<SampleInstanceEntity> entities = sampleInstanceEjb.doExternalUpload(
                VarioskanParserTest.getSpreadsheet(file), OVERWRITE,
                new ExternalLibraryProcessorNewTech(), messageCollection, null, false);

        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertTrue(messageCollection.getInfos().iterator().next()
                .startsWith(String.format(SampleInstanceEjb.IS_SUCCESS, 2)),
                StringUtils.join(messageCollection.getInfos(), "; "));

        Assert.assertEquals(entities.size(), 2);

        SampleKitRequest sampleKitRequest = entities.get(0).getSampleKitRequest();
        // The other header value row fields have already been tested since it's shared code
        // in HeaderValueRowTableProcessor and ExternalLibraryProcessor.
        Assert.assertEquals(sampleKitRequest.getIrbApprovalRequired(), "Y");

        for (int i = 0; i < entities.size(); ++i) {
            SampleInstanceEntity entity = entities.get(i);

            Assert.assertEquals(entity.getSampleKitRequest(), sampleKitRequest);

            String libraryName = select(i, "4442SFF6", "4442SFF7");
            Assert.assertEquals(entity.getSampleLibraryName(), libraryName);

            Assert.assertTrue(entity.getSequencerModel().getDisplayName().startsWith("HiSeq 2500 Rapid Run"),
                    entity.getSequencerModel().getDisplayName());

            MercurySample mercurySample = entity.getMercurySample();
            Assert.assertNotNull(mercurySample, libraryName);
            Assert.assertEquals(mercurySample.getSampleKey(), libraryName);

            Assert.assertEquals(mercurySample.getMetadataSource(), MercurySample.MetadataSource.MERCURY);
            Map<Metadata.Key, String> metadataMap = new HashMap<>();
            for (Metadata metadata : mercurySample.getMetadata()) {
                metadataMap.put(metadata.getKey(), metadata.getStringValue());
            }
            Assert.assertEquals(metadataMap.get(Metadata.Key.SAMPLE_ID), select(i, "DDDSS2244", "DDDSS2245"));
            Assert.assertEquals(metadataMap.get(Metadata.Key.PATIENT_ID), select(i, "Patient1", "Patient2"));
            Assert.assertEquals(metadataMap.get(Metadata.Key.SPECIES), "GG SS");
            Assert.assertEquals(metadataMap.get(Metadata.Key.GENDER), select(i, "M", "M"));
            Assert.assertEquals(metadataMap.get(Metadata.Key.MATERIAL_TYPE), "DNA");
            Assert.assertTrue(StringUtils.isBlank(metadataMap.get(Metadata.Key.BROAD_PARTICIPANT_ID)));
            Assert.assertNull(metadataMap.get(Metadata.Key.LSID));

            LabVessel tube = entity.getLabVessel();
            Assert.assertEquals(tube.getLabel(), libraryName);
            Assert.assertEquals(tube.getVolume(), new BigDecimal("33.00"));
            Assert.assertEquals(tube.getConcentration(), new BigDecimal("4444.00"));
            Assert.assertTrue(tube.getMercurySamples().contains(mercurySample), libraryName);

            Assert.assertEquals(entity.getReadLength().intValue(), 151);
            Assert.assertEquals(entity.getLibraryType(), "WholeGenomeShotgun");
            Assert.assertEquals(entity.getAggregationParticle(), "Microsporidia_RNASeq_Sanscrainte");
            Assert.assertEquals(entity.getAnalysisType().getBusinessKey(), "WholeGenomeShotgun.Resequencing");
            Assert.assertEquals(entity.getNumberLanes(), 4);
            Assert.assertEquals(entity.getComments(),
                    select(i, "mediocre sample; you, me, somebody", "great sample; you, me, somebody"));
            Assert.assertFalse(entity.getPooled());
            Assert.assertEquals(entity.getReferenceSequence().getName(), "Homo_sapiens_assembly19");

            if (i == 0) {
                Assert.assertNull(entity.getMolecularIndexingScheme());
            } else {
                Assert.assertEquals(entity.getMolecularIndexingScheme().getName(), "Illumina_P5-Lanah_P7-Cehih");
            }
        }
    }

    @Test
    public void testPooledTubes() throws Exception {
        // Uploads the spreadsheet.
        String file = "testdata/PooledTubesTest.xlsx";
        SampleInstanceEjb sampleInstanceEjb = setMocks(POOLEDTUBE);
        MessageCollection messageCollection = new MessageCollection();
        VesselPooledTubesProcessor processor = new VesselPooledTubesProcessor();

        List<SampleInstanceEntity> entities = sampleInstanceEjb.doExternalUpload(
                VarioskanParserTest.getSpreadsheet(file), OVERWRITE, processor, messageCollection, null, false);

        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertTrue(messageCollection.getInfos().iterator().next()
                .startsWith(String.format(SampleInstanceEjb.IS_SUCCESS, 2)),
                StringUtils.join(messageCollection.getInfos(), "; "));

        // Checks SampleInstanceEntities that were created.
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), ". "));
        Assert.assertTrue(messageCollection.getInfos().get(0)
                .startsWith(String.format(SampleInstanceEjb.IS_SUCCESS, 2)),
                StringUtils.join(messageCollection.getInfos(), "; "));
        Assert.assertEquals(entities.size(), 2);

        Map<String, BarcodedTube> mapBarcodeToTube = new LinkedHashMap<>();
        for (int i = 0; i < entities.size(); ++i) {
            SampleInstanceEntity entity = entities.get(i);

            Assert.assertNull(entity.getSampleKitRequest());

            String libraryName = select(i, "Jon Test 3a", "Jon Test 4b");
            Assert.assertEquals(entity.getSampleLibraryName(), libraryName);

            Assert.assertNull(entity.getSequencerModel());

            MercurySample mercurySample = entity.getMercurySample();
            Assert.assertNotNull(mercurySample.getSampleKey(), select(i, "SM-JT12", "SM-JT23"));
            Assert.assertEquals(entity.getRootSample().getSampleKey(), select(i, "SM-46IRUT1", "SM-46IRUT2"));

            Assert.assertEquals(mercurySample.getMetadataSource(), MercurySample.MetadataSource.BSP);
            SampleData sampleData = mercurySample.getSampleData();

            // This metadata is present only on root sample
            Assert.assertEquals(sampleData.getCollaboratorsSampleName(), select(i, "COLLAB-JT04121", "COLLAB-JT04122"));
            Assert.assertEquals(sampleData.getPatientId(), select(i, "PT-JT1", "PT-JT2"));
            Assert.assertEquals(sampleData.getCollaboratorParticipantId(),
                    select(i, "COLLAB-P-JT04121", "COLLAB-P-JT04122"));

            // This metadata is present on Broad sample
            Assert.assertEquals(sampleData.getGender(), "");
            Assert.assertEquals(sampleData.getOrganism(), "Homo Sapiens");
            Assert.assertEquals(sampleData.getMaterialType(), "");
            Assert.assertEquals(sampleData.getSampleLsid(), select(i,
                    "broadinstitute.org:bsp.dev.sample:JT1", "broadinstitute.org:bsp.dev.sample:JT2"));

            BarcodedTube tube = (BarcodedTube)entity.getLabVessel();
            mapBarcodeToTube.put(tube.getLabel(), tube);
            Assert.assertEquals(tube.getLabel(), "JT041431");
            Assert.assertEquals(tube.getVolume(), new BigDecimal("0.60"));
            Assert.assertTrue(tube.getMercurySamples().contains(mercurySample), libraryName);

            Assert.assertEquals(entity.getExperiment(), "DEV-7501");
            Assert.assertEquals(entity.getSubTasks().get(0), select(i, "DEV-7538", "DEV-7539"));

            Assert.assertEquals(entity.getReadLength().intValue(), new int[]{4, 2}[i]);
            Assert.assertNull(entity.getLibraryType());
            Assert.assertNull(entity.getAggregationParticle());
            Assert.assertNull(entity.getAnalysisType());
            Assert.assertEquals(entity.getNumberLanes(), 1);
            Assert.assertEquals(entity.getComments(), "");
            Assert.assertFalse(entity.getPooled());
            Assert.assertNull(entity.getReferenceSequence());

            Assert.assertEquals(entity.getReagentDesign().getName(), "NewtonCheh_NatPepMDC_12genes3regions_Sep2011");
            Assert.assertEquals(entity.getMolecularIndexingScheme().getName(),
                    select(i, "Illumina_P5-Nijow_P7-Waren","Illumina_P5-Piwan_P7-Bidih"));
        }
        assertSampleInstanceEntitiesPresent(mapBarcodeToTube.values(), entities);

        // Runs the workflow: PDO creation, LCSET creation, bucketing, pico plating, shearing, LC,
        // HybSelect, QTP, sequencing.
        Workflow workflow = Workflow.ICE_EXOME_EXPRESS;
        ProductOrder productOrder = ProductOrderTestFactory.createDummyProductOrder(0, "PDO-TEST123");
        Assert.assertTrue(entities.size() <= NUM_POSITIONS_IN_RACK,
                entities.size() + " samples is more than a single rack " + NUM_POSITIONS_IN_RACK);
        for (SampleInstanceEntity entity : entities) {
            ProductOrderSample productOrderSample = new ProductOrderSample(entity.getMercurySample().getSampleKey());
            productOrder.addSample(productOrderSample);
        }
        productOrder.setJiraTicketKey(productOrder.getJiraTicketKey());
        productOrder.setOrderStatus(ProductOrder.OrderStatus.Submitted);
        productOrder.getProduct().setWorkflow(workflow);
        expectedRouting = SystemRouter.System.MERCURY;

        LabBatch workflowBatch = new LabBatch("a batch", new HashSet<LabVessel>(mapBarcodeToTube.values()),
                LabBatch.LabBatchType.WORKFLOW);
        workflowBatch.setWorkflow(workflow);

        String lcsetSuffix = "1";
        bucketBatchAndDrain(mapBarcodeToTube, productOrder, workflowBatch, lcsetSuffix);
        Assert.assertEquals(productOrder.getSamples().size(), entities.size());

        PicoPlatingEntityBuilder picoPlatingEntityBuilder = runPicoPlatingProcess(mapBarcodeToTube,
                "P", lcsetSuffix, true);

        ExomeExpressShearingEntityBuilder exomeExpressShearingEntityBuilder =
                runExomeExpressShearingProcess(picoPlatingEntityBuilder.getNormBarcodeToTubeMap(),
                        picoPlatingEntityBuilder.getNormTubeFormation(),
                        picoPlatingEntityBuilder.getNormalizationBarcode(), lcsetSuffix);
        Assert.assertEquals(exomeExpressShearingEntityBuilder.getShearingCleanupPlate().getSampleInstancesV2().size(),
                entities.size());
        Assert.assertEquals(exomeExpressShearingEntityBuilder.getShearingCleanupPlate().getContainerRole()
                .getSampleInstancesAtPositionV2(VesselPosition.A01).size(), entities.size());

        LibraryConstructionEntityBuilder libraryConstructionEntityBuilder = runLibraryConstructionProcess(
                exomeExpressShearingEntityBuilder.getShearingCleanupPlate(),
                exomeExpressShearingEntityBuilder.getShearCleanPlateBarcode(),
                exomeExpressShearingEntityBuilder.getShearingPlate(),
                lcsetSuffix, mapBarcodeToTube.size());
        Assert.assertFalse(libraryConstructionEntityBuilder.getPondRegTubeBarcodes().isEmpty());
        Assert.assertEquals(libraryConstructionEntityBuilder.getPondRegRack().getContainerRole()
                .getSampleInstancesAtPositionV2(VesselPosition.A01).size(), entities.size());
        IceEntityBuilder iceEntityBuilder = runIceProcess(
                Collections.singletonList(libraryConstructionEntityBuilder.getPondRegRack()), "1");

        QtpEntityBuilder qtpEntityBuilder = runQtpProcess(iceEntityBuilder.getCatchEnrichRack(),
                iceEntityBuilder.getCatchEnrichBarcodes(), iceEntityBuilder.getMapBarcodeToCatchEnrichTubes(), "1");

        LabVessel denatureSource = qtpEntityBuilder.getDenatureRack().getContainerRole()
                .getVesselAtPosition(VesselPosition.A01);
        new LabBatch(FCT_TICKET, Collections.singleton(denatureSource), LabBatch.LabBatchType.FCT);
        HiSeq2500FlowcellEntityBuilder hiSeq2500FlowcellEntityBuilder = runHiSeq2500FlowcellProcess(
                qtpEntityBuilder.getDenatureRack(), "1", FCT_TICKET, ProductionFlowcellPath.DILUTION_TO_FLOWCELL,
                "designation", workflow);

        Date runDate = new Date();
        String runDateString = (new SimpleDateFormat(IlluminaSequencingRun.RUN_FORMAT_PATTERN)).format(runDate);
        File runPath = File.createTempFile("tempRun" + runDateString, ".txt");
        String flowcellBarcode = hiSeq2500FlowcellEntityBuilder.getIlluminaFlowcell().getCartridgeBarcode();

        SolexaRunBean runBean = new SolexaRunBean(flowcellBarcode, flowcellBarcode + runDateString,
                runDate, "Superman", runPath.getAbsolutePath(), null);

        IlluminaSequencingRunFactory illuminaSequencingRunFactory = new IlluminaSequencingRunFactory(
                EasyMock.createNiceMock(JiraCommentUtil.class));

        IlluminaSequencingRun run = illuminaSequencingRunFactory.buildDbFree(runBean,
                hiSeq2500FlowcellEntityBuilder.getIlluminaFlowcell());

        List<String> flowcellSamples = new ArrayList<>();
        for (SampleInstanceV2 sampleInstanceV2 : run.getSampleCartridge().getSampleInstancesV2()) {
            flowcellSamples.add(sampleInstanceV2.getNearestMercurySampleName());
        }
        Assert.assertTrue(flowcellSamples.containsAll(processor.getSampleNames()));
    }

    @Test
    public void testExternalLibraryBarcodeUpdateHeaders() throws Exception {
        String file = "ExternalLibrarySampleBarcodeUpdate.xlsx";
        SampleInstanceEjb sampleInstanceEjb = setMocks(EZPASS);
        MessageCollection messageCollection = new MessageCollection();

        List<SampleInstanceEntity> entities = sampleInstanceEjb.doExternalUpload(
                VarioskanParserTest.getSpreadsheet(file), OVERWRITE,
                new ExternalLibraryBarcodeUpdate(), messageCollection, null, false);

        // Tests the expected errors.
        Assert.assertEquals(messageCollection.getErrors().size(), 3,
                StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertTrue(messageCollection.getErrors().get(0).contains("Row #2 the value for Library Name") &&
                messageCollection.getErrors().get(0).contains("does not exist in Mercury."),
                StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertTrue(messageCollection.getErrors().get(1).contains("Row #3 duplicate value for Library Name."),
                StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertTrue(messageCollection.getErrors().get(2).contains("Row #3 the value for Library Name") &&
                messageCollection.getErrors().get(2).contains("does not exist in Mercury."),
                StringUtils.join(messageCollection.getErrors(), "; "));

        Assert.assertTrue(CollectionUtils.isEmpty(entities));
    }

    @Test
    public void testNullSpreadsheet() throws Exception {
        SampleInstanceEjb sampleInstanceEjb = setMocks(EZPASS);
        MessageCollection messageCollection = new MessageCollection();

        List<SampleInstanceEntity> entities = sampleInstanceEjb.doExternalUpload(
                new ByteArrayInputStream(new byte[]{0}),
                OVERWRITE, new ExternalLibraryBarcodeUpdate(), messageCollection, null, false);

        Assert.assertEquals(messageCollection.getErrors().size(), 1,
                StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertTrue(messageCollection.getErrors().get(0).startsWith("Cannot process spreadsheet:"),
                StringUtils.join(messageCollection.getErrors(), "; "));

        Assert.assertTrue(CollectionUtils.isEmpty(entities));
    }


    @Test
    public void testPooledTubeUploadFail() throws Exception {
        final String filename = "PooledTube_Test-363_case3.xls";
        SampleInstanceEjb sampleInstanceEjb = setMocks(TestType.POOLED);
        MessageCollection messageCollection = new MessageCollection();
        VesselPooledTubesProcessor processor = new VesselPooledTubesProcessor();
        List<SampleInstanceEntity> entities = sampleInstanceEjb.doExternalUpload(
                new ByteArrayInputStream(IOUtils.toByteArray(VarioskanParserTest.getSpreadsheet(filename))),
                true, processor, messageCollection, null, false);

        // Should be no sampleInstanceEntities.
        Assert.assertEquals(entities.size(), 0);
        // Checks the error messages for expected problems.
        List<String> errors = new ArrayList<>(messageCollection.getErrors());
        List<String> warnings = new ArrayList<>(messageCollection.getWarnings());

        errorIfMissing(errors, filename, String.format(SampleInstanceEjb.INCONSISTENT_SAMPLE_DATA, 3,
                VesselPooledTubesProcessor.Headers.COLLABORATOR_SAMPLE_ID.getText(), 2, "SM-748OO"));
        errorIfMissing(errors, filename, String.format(SampleInstanceEjb.INCONSISTENT_SAMPLE_DATA, 3,
                "Patient Id (Collaborator Participant ID)", 2, "SM-748OO"));
        errorIfMissing(errors, filename, String.format(SampleInstanceEjb.INCONSISTENT_SAMPLE_DATA, 3,
                "Sex", 2, "SM-748OO"));
        errorIfMissing(errors, filename, String.format(SampleInstanceEjb.INCONSISTENT_SAMPLE_DATA, 3,
                "Root Sample", 2, "SM-748OO"));
        errorIfMissing(errors, filename, String.format(SampleInstanceEjb.NONEXISTENT, 3,
                "Root Sample", "SM-UNKNOWN", "Mercury"));
        errorIfMissing(errors, filename, String.format(SampleInstanceEjb.DUPLICATE_IN_TUBE, 3,
                VesselPooledTubesProcessor.Headers.MOLECULAR_INDEXING_SCHEME.getText(), "01509634244"));
        errorIfMissing(errors, filename, String.format(SampleInstanceEjb.INCONSISTENT_TUBE, 3,
                VesselPooledTubesProcessor.Headers.VOLUME.getText(), 2, "01509634244"));
        errorIfMissing(warnings, filename, String.format(SampleInstanceEjb.DUPLICATE_S_M, 3,
                "SM-748OO", "Illumina_P5-Nijow_P7-Waren"));

        errorIfMissing(warnings, filename, String.format(SampleInstanceEjb.DUPLICATE_S_M, 4,
                "SM-748OO", "Illumina_P5-Nijow_P7-Waren"));
        errorIfMissing(errors, filename, String.format(SampleInstanceEjb.NONNEGATIVE_INTEGER, 4,
                VesselPooledTubesProcessor.Headers.READ_LENGTH.getText()));

        errorIfMissing(errors, filename, "Row #5 " + String.format(REQUIRED_VALUE_IS_MISSING,
                VesselPooledTubesProcessor.Headers.TUBE_BARCODE.getText()));
        errorIfMissing(errors, filename, "Row #5 " + String.format(REQUIRED_VALUE_IS_MISSING,
                VesselPooledTubesProcessor.Headers.EXPERIMENT.getText()));
        errorIfMissing(errors, filename, "Row #5 " + String.format(REQUIRED_VALUE_IS_MISSING,
                VesselPooledTubesProcessor.Headers.CONDITIONS.getText()));

        errorIfMissing(errors, filename, "Row #6 " + String.format(REQUIRED_VALUE_IS_MISSING,
                VesselPooledTubesProcessor.Headers.LIBRARY_NAME.getText()));
        errorIfMissing(errors, filename, String.format(SampleInstanceEjb.MISSING, 6, "Volume"));

        errorIfMissing(errors, filename, "Row #7 " + String.format(REQUIRED_VALUE_IS_MISSING,
                VesselPooledTubesProcessor.Headers.BROAD_SAMPLE_ID.getText()));
        errorIfMissing(errors, filename, "Row #7 " + String.format(REQUIRED_VALUE_IS_MISSING,
                VesselPooledTubesProcessor.Headers.MOLECULAR_INDEXING_SCHEME.getText()));
        errorIfMissing(errors, filename, String.format(SampleInstanceEjb.MUST_NOT_HAVE_BOTH, 7,
                VesselPooledTubesProcessor.Headers.BAIT.getText(), VesselPooledTubesProcessor.Headers.CAT.getText()));
        errorIfMissing(errors, filename, String.format(SampleInstanceEjb.UNKNOWN_COND, 7, "DEV-6796"));
        errorIfMissing(errors, filename, String.format(SampleInstanceEjb.INCONSISTENT_TUBE, 7,
                VesselPooledTubesProcessor.Headers.VOLUME.getText(), 6, "01509634249"));

        errorIfMissing(errors, filename, String.format(SampleInstanceEjb.DUPLICATE, 8,
                VesselPooledTubesProcessor.Headers.LIBRARY_NAME.getText()));
        errorIfMissing(errors, filename, String.format(SampleInstanceEjb.DUPLICATE_IN_TUBE, 8,
                VesselPooledTubesProcessor.Headers.MOLECULAR_INDEXING_SCHEME.getText(), "01509634249"));
        errorIfMissing(errors, filename, String.format(SampleInstanceEjb.MUST_NOT_HAVE_BOTH, 8,
                VesselPooledTubesProcessor.Headers.BAIT.getText(), VesselPooledTubesProcessor.Headers.CAT.getText()));
        errorIfMissing(errors, filename, String.format(SampleInstanceEjb.UNKNOWN_COND, 8, "DEV-6796"));
        errorIfMissing(errors, filename, String.format(SampleInstanceEjb.INCONSISTENT_SAMPLE_DATA, 8,
                VesselPooledTubesProcessor.Headers.LSID.getText(), 2, "SM-748OO"));
        errorIfMissing(warnings, filename, String.format(SampleInstanceEjb.DUPLICATE_S_M, 8,
                "SM-748OO", "Illumina_P5-Nijow_P7-Waren"));
        errorIfMissing(errors, filename, String.format(SampleInstanceEjb.INCONSISTENT_TUBE, 8,
                VesselPooledTubesProcessor.Headers.VOLUME.getText(), 6, "01509634249"));

        errorIfMissing(errors, filename, String.format(SampleInstanceEjb.NONNEGATIVE_INTEGER, 9,
                VesselPooledTubesProcessor.Headers.READ_LENGTH.getText()));
        errorIfMissing(errors, filename, String.format(SampleInstanceEjb.NONNEGATIVE_DECIMAL, 9,
                VesselPooledTubesProcessor.Headers.VOLUME.getText()));
        errorIfMissing(errors, filename, String.format(SampleInstanceEjb.UNKNOWN_COND, 9, "DEV-6796"));
        errorIfMissing(errors, filename, String.format(SampleInstanceEjb.UNKNOWN, 9,
                VesselPooledTubesProcessor.Headers.MOLECULAR_INDEXING_SCHEME.getText(), "Mercury"));

        Assert.assertTrue(errors.isEmpty(), "Found unexpected errors: " + StringUtils.join(errors, "; "));
        Assert.assertTrue(errors.isEmpty(), "Found unexpected warnings: " + StringUtils.join(warnings, "; "));
    }

    @Test
    public void testPooledTubeUploadFail2() throws Exception {
        final String filename = "ExternalLibraryEZFailTest2.xlsx";
        SampleInstanceEjb sampleInstanceEjb = setMocks(TestType.EZPASS);
        MessageCollection messages = new MessageCollection();
        ExternalLibraryProcessor processor = new ExternalLibraryProcessorEzPass();
        List<SampleInstanceEntity> entities = sampleInstanceEjb.doExternalUpload(
                new ByteArrayInputStream(IOUtils.toByteArray(VarioskanParserTest.getSpreadsheet(filename))),
                true, processor, messages, null, false);

        // Should be no sampleInstanceEntities.
        Assert.assertEquals(entities.size(), 0);
        // Checks the error messages for expected problems.
        List<String> errors = new ArrayList<>(messages.getErrors());
        List<String> warnings = new ArrayList<>(messages.getWarnings());

        errorIfMissing(errors, filename, String.format(SampleInstanceEjb.DUPLICATE, 31,
                VesselPooledTubesProcessor.Headers.LIBRARY_NAME.getText()));
        errorIfMissing(errors, filename, String.format(SampleInstanceEjb.DUPLICATE, 32,
                VesselPooledTubesProcessor.Headers.LIBRARY_NAME.getText()));
        errorIfMissing(errors, filename, String.format(SampleInstanceEjb.DUPLICATE, 33,
                VesselPooledTubesProcessor.Headers.LIBRARY_NAME.getText()));
        errorIfMissing(errors, filename, String.format(SampleInstanceEjb.DUPLICATE, 34,
                VesselPooledTubesProcessor.Headers.LIBRARY_NAME.getText()));
        errorIfMissing(errors, filename, String.format(SampleInstanceEjb.DUPLICATE, 35,
                VesselPooledTubesProcessor.Headers.LIBRARY_NAME.getText()));

        errorIfMissing(errors, filename, String.format(SampleInstanceEjb.UNKNOWN, 30,
                ExternalLibraryProcessorNewTech.Headers.SEQUENCING_TECHNOLOGY.getText(), "Mercury"));
        errorIfMissing(errors, filename, String.format(SampleInstanceEjb.UNKNOWN, 31,
                ExternalLibraryProcessorNewTech.Headers.SEQUENCING_TECHNOLOGY.getText(), "Mercury"));
        errorIfMissing(errors, filename, String.format(SampleInstanceEjb.UNKNOWN, 32,
                ExternalLibraryProcessorNewTech.Headers.SEQUENCING_TECHNOLOGY.getText(), "Mercury"));
        errorIfMissing(errors, filename, String.format(SampleInstanceEjb.UNKNOWN, 33,
                ExternalLibraryProcessorNewTech.Headers.SEQUENCING_TECHNOLOGY.getText(), "Mercury"));
        errorIfMissing(errors, filename, String.format(SampleInstanceEjb.UNKNOWN, 34,
                ExternalLibraryProcessorNewTech.Headers.SEQUENCING_TECHNOLOGY.getText(), "Mercury"));

        errorIfMissing(errors, filename, String.format(SampleInstanceEjb.UNKNOWN, 31,
                ExternalLibraryProcessorNewTech.Headers.DATA_ANALYSIS_TYPE.getText(), "Mercury"));

        errorIfMissing(errors, filename, "Row #32 " + String.format(REQUIRED_VALUE_IS_MISSING,
                ExternalLibraryProcessorNewTech.Headers.DATA_ANALYSIS_TYPE.getText()));

        errorIfMissing(errors, filename, String.format(SampleInstanceEjb.BAD_RANGE, 33,
                ExternalLibraryProcessorNewTech.Headers.INSERT_SIZE_RANGE.getText()));
        errorIfMissing(errors, filename, String.format(SampleInstanceEjb.BAD_RANGE, 34,
                ExternalLibraryProcessorNewTech.Headers.INSERT_SIZE_RANGE.getText()));
        errorIfMissing(errors, filename, String.format(SampleInstanceEjb.BAD_RANGE, 35,
                ExternalLibraryProcessorNewTech.Headers.INSERT_SIZE_RANGE.getText()));

        Assert.assertTrue(errors.isEmpty(), "Found unexpected errors: " + StringUtils.join(errors, "; "));
        Assert.assertTrue(errors.isEmpty(), "Found unexpected warnings: " + StringUtils.join(warnings, "; "));
    }

    private boolean errorIfMissing(List<String> errors, String filename, String expected) {
        for (Iterator<String> iterator = errors.iterator(); iterator.hasNext(); ) {
            String error = iterator.next();
            if (error.startsWith(expected)) {
                iterator.remove();
                return true;
            }
        }
        Assert.fail(filename + " error message \"" + expected + "\" is missing from the remaining errors: " +
                StringUtils.join(errors, "; "));
        return false;
    }

    private <VESSEL extends LabVessel> void assertSampleInstanceEntitiesPresent(Collection<VESSEL> labVessels,
            Collection<SampleInstanceEntity> entities) {
        List<String> list = new ArrayList<>();
        for (VESSEL labVessel : labVessels) {
            for (SampleInstanceV2 sampleInstanceV2 : labVessel.getSampleInstancesV2()) {
                list.add(sampleInstanceV2.getEarliestMercurySampleName());
            }
        }
        for (SampleInstanceEntity entity : entities) {
            Assert.assertTrue(list.contains(entity.getMercurySample().getSampleKey()));
        }
    }

    private String select(int idx, String... params) {
        Assert.assertTrue(params.length > idx, idx + " cannot select " + StringUtils.join(params, ", "));
        return Arrays.asList(params).get(idx);
    }

    /** Sets up the mocks for all test cases. */
    private SampleInstanceEjb setMocks(TestType testType) throws Exception {

        // BarcodedTubes
        final boolean createTubes = (testType == TestType.POOLEDTUBE || testType == TestType.WALKUP);
        Mockito.when(labVesselDao.findByBarcodes(Mockito.anyList())).thenAnswer(new Answer<Map<String, LabVessel>>() {
            @Override
            public Map<String, LabVessel> answer(InvocationOnMock invocation) throws Throwable {
                Map<String, LabVessel> map = new HashMap<>();
                if (createTubes) {
                    for (String barcode : (List<String>) invocation.getArguments()[0]) {
                        map.put(barcode,
                                new BarcodedTube(barcode, BarcodedTube.BarcodedTubeType.MatrixTube075));
                    }
                }
                return map;
            }
        });

        // Mercury samples
        final Map<String, Set<Metadata>> metadataMap = new HashMap<>();
        final Map<String, BspSampleData> bspSampleData = new HashMap<>();
        int bspIdx = 0;
        int mercuryIdx = 0;
        for (String sampleName : Arrays.asList("SM-JT12", "SM-JT23", "SM-46IRUT1", "SM-46IRUT2",
                "Lib-MOCK.FSK1.A", "4442SFF6", "4442SFF7", "4442SFP6", "4442SFP7",
                "4076255991TEST", "4076255992TEST", "4076255993TEST")) {

            if (sampleName.startsWith("SM-")) {
                // BSP samples
                Map<BSPSampleSearchColumn, String> map = new HashMap<>();
                map.put(BSPSampleSearchColumn.ROOT_SAMPLE, select(bspIdx, "SM-46IRUT1", "SM-46IRUT2", null, null));
                map.put(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID, select(bspIdx, "COLLAB-JT04121",
                        "COLLAB-JT04122", "COLLAB-JT04121", "COLLAB-JT04122"));
                map.put(BSPSampleSearchColumn.COLLABORATOR_PARTICIPANT_ID, select(bspIdx, "COLLAB-P-JT04121",
                        "COLLAB-P-JT04122", "COLLAB-P-JT04121", "COLLAB-P-JT04122"));
                map.put(BSPSampleSearchColumn.PARTICIPANT_ID, select(bspIdx, "PT-JT1", "PT-JT2", "PT-JT1", "PT-JT2"));
                map.put(BSPSampleSearchColumn.SPECIES, "Homo Sapiens");
                map.put(BSPSampleSearchColumn.LSID, select(bspIdx, "broadinstitute.org:bsp.dev.sample:JT1",
                        "broadinstitute.org:bsp.dev.sample:JT2", "root1", "root2"));
                bspSampleData.put(sampleName, new BspSampleData(map));
                ++bspIdx;

            } else {
                // non-BSP samples
                Set<Metadata> metadata = new HashSet<>();
                metadata.add(new Metadata(Metadata.Key.SAMPLE_ID, sampleName));
                metadata.add(new Metadata(Metadata.Key.PATIENT_ID, select(mercuryIdx,
                        "MOCK1", "", "", "Patient X", "Patient Y", "hh", "ii", "jj")));
                metadata.add(new Metadata(Metadata.Key.SPECIES, "S"));
                metadata.add(new Metadata(Metadata.Key.GENDER, select(mercuryIdx,
                        "M", "F", "", "F", "", "M", "F", "M")));
                metadata.add(new Metadata(Metadata.Key.MATERIAL_TYPE, MaterialType.DNA.getDisplayName()));
                metadataMap.put(sampleName, metadata);
                ++mercuryIdx;
            }
        }

        Mockito.when(mercurySampleDao.findMapIdToMercurySample(Mockito.anySet())).thenAnswer(
                new Answer<Map<String, MercurySample>>() {
                    @Override
                    public Map<String, MercurySample> answer(InvocationOnMock invocation) throws Throwable {
                        Map<String, MercurySample> map = new HashMap<>();
                        for (String name : (Set<String>) invocation.getArguments()[0]) {
                            if (metadataMap.containsKey(name)) {
                                map.put(name, new MercurySample(name, metadataMap.get(name)));
                            } else if (bspSampleData.containsKey(name)) {
                                map.put(name, new MercurySample(name, bspSampleData.get(name)));
                            } else if (!name.contains("UNKNOWN")) {
                                // Root samples will have no metadata.
                                map.put(name, new MercurySample(name, BSPUtil.isInBspFormat(name) ?
                                        MercurySample.MetadataSource.BSP : MercurySample.MetadataSource.MERCURY));
                            }
                        }
                        return map;
                    }
                });

        Mockito.when(sampleDataFetcher.fetchSampleData(Mockito.anyCollection())).thenAnswer(
                new Answer<Map<String, SampleData>>() {
                    @Override
                    public Map<String, SampleData> answer(InvocationOnMock invocation) throws Throwable {
                        Map<String, SampleData> map = new HashMap<>();
                        for (String name : (Collection<String>) invocation.getArguments()[0]) {
                            if (bspSampleData.containsKey(name)) {
                                map.put(name, bspSampleData.get(name));
                            }
                        }
                        return map;
                    }
                });

        Mockito.when(analysisTypeDao.findByBusinessKey(Mockito.anyString())).thenAnswer(new Answer<AnalysisType>() {
            @Override
            public AnalysisType answer(InvocationOnMock invocation) throws Throwable {
                String name = (String)invocation.getArguments()[0];
                return (StringUtils.isBlank(name) || name.equalsIgnoreCase("unknown")) ?
                        null : new AnalysisType(name);
            }
        });

        // ReferenceSequences
        Mockito.when(referenceSequenceDao.findCurrent(Mockito.anyString())).thenAnswer(
                new Answer<ReferenceSequence>() {
                    @Override
                    public ReferenceSequence answer(InvocationOnMock invocation) throws Throwable {
                        String name = (String)invocation.getArguments()[0];
                        return (StringUtils.isBlank(name) || name.equalsIgnoreCase("unknown")) ?
                                null : new ReferenceSequence(name, "");
                    }
                });

        Mockito.when(referenceSequenceDao.findByBusinessKey(Mockito.anyString())).thenAnswer(
                new Answer<ReferenceSequence>() {
                    @Override
                    public ReferenceSequence answer(InvocationOnMock invocation) throws Throwable {
                        String businessKey = (String)invocation.getArguments()[0];
                        return StringUtils.isBlank(businessKey) ? null : new ReferenceSequence(
                                StringUtils.substringBeforeLast(businessKey, "|"),
                                StringUtils.substringAfterLast(businessKey, "|"));

                    }
                });

        // Jira ticket
        Mockito.when(jiraService.getIssueInfo(Mockito.anyString(), Mockito.anyString())).thenAnswer(
                new Answer<JiraIssue>() {
                    @Override
                    public JiraIssue answer(InvocationOnMock invocation) throws Throwable {
                        String name = (String)invocation.getArguments()[0];
                        JiraIssue jiraIssue = null;
                        if ("DEV-7501".equals(name)) {
                            jiraIssue = new JiraIssue(name, jiraService);
                            List<String> conditionKeys = Arrays.asList("DEV-7538", "DEV-7539");
                            jiraIssue.setConditions(conditionKeys, conditionKeys);
                        } else if ("DEV-6796".equals(name)) {
                            jiraIssue = new JiraIssue(name, jiraService);
                            List<String> conditionKeys = Arrays.asList("DEV-6815", "DEV-6816");
                            jiraIssue.setConditions(conditionKeys, conditionKeys);
                        }
                        return jiraIssue;
                    }
                });

        // MolecularIndexingScheme
        Mockito.when(molecularIndexingSchemeDao.findByName(Mockito.anyString())).thenAnswer(
                new Answer<MolecularIndexingScheme>() {
                    @Override
                    public MolecularIndexingScheme answer(InvocationOnMock invocation) throws Throwable {
                        String name = (String)invocation.getArguments()[0];
                        MolecularIndexingScheme molecularIndexingScheme = null;
                        if (StringUtils.isNotBlank(name) && name.startsWith("Illumina_")) {
                            molecularIndexingScheme = new MolecularIndexingScheme();
                            molecularIndexingScheme.setName(name);
                        }
                        return molecularIndexingScheme;
                    }
                });

        // ReagentDesign
        Mockito.when(reagentDesignDao.findByBusinessKey(Mockito.anyString())).thenAnswer(new Answer<ReagentDesign>() {
            @Override
            public ReagentDesign answer(InvocationOnMock invocation) throws Throwable {
                String name = (String)invocation.getArguments()[0];
                ReagentDesign reagentDesign = null;
                if (StringUtils.isBlank(name) || name.equalsIgnoreCase("unknown")) {
                    return null;
                }
                reagentDesign = new ReagentDesign();
                reagentDesign.setDesignName(name);
                return reagentDesign;
            }
        });

        // SampleInstanceEntity
        Mockito.when(sampleInstanceEntityDao.findByName(anyString())).thenReturn(null);

        return new SampleInstanceEjb(molecularIndexingSchemeDao, jiraService,
                reagentDesignDao, labVesselDao, mercurySampleDao, sampleInstanceEntityDao,
                analysisTypeDao, sampleKitRequestDao, sampleDataFetcher, referenceSequenceDao);
    }

}
