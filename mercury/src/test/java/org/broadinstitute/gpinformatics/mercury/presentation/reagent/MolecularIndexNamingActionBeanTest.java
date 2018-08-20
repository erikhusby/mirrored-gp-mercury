package org.broadinstitute.gpinformatics.mercury.presentation.reagent;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.MolecularIndexDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.MolecularIndexingSchemeDao;
import org.broadinstitute.gpinformatics.mercury.control.reagent.MolecularIndexingSchemeFactory;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndex;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.presentation.TestCoreActionBeanContext;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Tests MolecularIndexNamingActionBean.
 */
@Test(groups = TestGroups.STANDARD)
public class MolecularIndexNamingActionBeanTest extends Arquillian {
    private static final Log log = LogFactory.getLog(MolecularIndexNamingActionBeanTest.class);

    // Uses "impossible" sequences for testing to ensure there won't be a collision with real data.
    private String[] impossibleSequences = {"AAAAAAAAA", "AAAAAAAAT", "AAAAAAAAC", "AAAAAAAAG"};

    // Invalid upload data expected to fail due to duplicate headers.
    private List<List<String>> duplicateHeader = Arrays.asList(
            Arrays.asList("P5", "P5"),
            Arrays.asList(impossibleSequences[0], impossibleSequences[0])
    );

    // Invalid upload data expected to fail due to blank header row.
    private List<List<String>> blankHeaderRow = Arrays.asList(
            new ArrayList<String>(),
            Arrays.asList(impossibleSequences[0], impossibleSequences[0])
    );

    // Invalid upload data expected to fail due to one blank header.
    private List<List<String>> oneBlankHeader = Arrays.asList(
            Arrays.asList("P5", "", "P7"),
            Arrays.asList(impossibleSequences[0], "", impossibleSequences[0])
    );

    // Valid upload data made from the "impossible" test sequences.
    private List<List<String>> p7is1Upload = Arrays.asList(
            Arrays.asList("P7", "IS1"),
            Arrays.asList(impossibleSequences[0], impossibleSequences[1]),
            Arrays.asList(impossibleSequences[1], impossibleSequences[2]),
            Arrays.asList(impossibleSequences[2], impossibleSequences[3]),
            Arrays.asList(impossibleSequences[3], impossibleSequences[0])
    );

    // The corresponding scheme names.
    private List<String> p7is1Schemes = Arrays.asList(
            "Illumina_P7-Bababa_IS1-Bababo",
            "Illumina_P7-Bababo_IS1-Bababe",
            "Illumina_P7-Bababe_IS1-Bababi",
            "Illumina_P7-Bababi_IS1-Bababa"
    );

    private List<List<String>> quotedP7is1 = Arrays.asList(
            Arrays.asList(" \"P7\" ", "\"IS1\""),
            Arrays.asList("\"AGCAATTC\" ", " \"AGTTGCTT\"")
    );

    private List<String> quotedP7is1Schemes = Arrays.asList(
            "Illumina_P7-Debox_IS1-Doyez"
    );

    private List<List<String>> ionUpload = Arrays.asList(
            Arrays.asList("A+B"),
            Arrays.asList(impossibleSequences[0] + "+" + impossibleSequences[3]),
            Arrays.asList(impossibleSequences[1] + "+" + impossibleSequences[0]),
            Arrays.asList(impossibleSequences[2] + "+" + impossibleSequences[1])
    );

    // The corresponding scheme names.
    private List<String> ionSchemes = Arrays.asList(
            "Ion_A-Bababa_B-Bababi",
            "Ion_A-Bababo_B-Bababa",
            "Ion_A-Bababe_B-Bababo"
    );

    private List<List<String>> existingIndexUpload = Arrays.asList(
            Arrays.asList("P5-P7"),
            Arrays.asList("ATCGACTG-AAGTAGAG"),
            Arrays.asList("GCTAGCAG-GGTCCAGA")
    );

    private List<String> existingSchemes = Arrays.asList(
            "Illumina_P5-Feney_P7-Biwid",
            "Illumina_P5-Poded_P7-Rojan");

    private MolecularIndexNamingActionBean actionBean;

    @Inject
    private MolecularIndexingSchemeDao molecularIndexingSchemeDao;

    @Inject
    private MolecularIndexDao molecularIndexDao;

    @Inject
    private MolecularIndexingSchemeFactory molecularIndexingSchemeFactory;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private UserTransaction utx;

    @org.jboss.arquillian.container.test.api.Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(Deployment.DEV, "dev");
    }

    @BeforeMethod
    public void setup() throws Exception {
        if (molecularIndexDao == null) {
            actionBean = null;
            return;
        }
        actionBean = new MolecularIndexNamingActionBean();
        actionBean.setContext(new TestCoreActionBeanContext());
        actionBean.setMolecularIndexingSchemeFactory(molecularIndexingSchemeFactory);

        // Removes any test sequence if it already exists.
        utx.begin();
        for (String sequence : impossibleSequences) {
            MolecularIndex molecularIndex = molecularIndexDao.findBySequence(sequence);
            if (molecularIndex != null) {
                // Removes any test schemes using the test sequence, and removes the molecular_index_position join.
                for (MolecularIndexingScheme molecularIndexingScheme : molecularIndex.getMolecularIndexingSchemes()) {
                    log.info("Removing scheme " + molecularIndexingScheme.getName());
                    molecularIndexDao.remove(molecularIndexingScheme);
                    // Native Query is used because molecular_index_position join table is not an entity.
                    String sql = "delete from molecular_index_position where scheme_id = " +
                            "(select molecular_indexing_scheme_id from molecular_indexing_scheme " +
                            " where name = '" + molecularIndexingScheme.getName() + "')";
                    molecularIndexDao.getEntityManager().createNativeQuery(sql).executeUpdate();
                }
                log.info("Removing index " + molecularIndex.getSequence());
                molecularIndexDao.remove(molecularIndex);
            }
        }
        molecularIndexDao.flush();
        utx.commit();
    }

    @Test
    public void uploadEmptySpreadsheet() {
        actionBean.setInputStream(new ByteArrayInputStream(new byte[0]));
        actionBean.setFilename("test.tsv");
        actionBean.setTechnology(MolecularIndexingScheme.TECHNOLOGY_ILLUMINA);
        actionBean.setCreateMissingNames(false);
        actionBean.upload();
        Assert.assertEquals(actionBean.getFormattedMessages().get(0), "Found 0 indexes in the upload.");
    }

    @Test
    public void testDuplicateHeader() {
        // Does the upload.
        actionBean.setInputStream(makeInputStream(duplicateHeader, ","));
        actionBean.setFilename("test.csv");
        actionBean.setTechnology(MolecularIndexingScheme.TECHNOLOGY_ILLUMINA);
        actionBean.setCreateMissingNames(false);
        actionBean.upload();

        // Should have an error about duplicate headers and no name spreadsheet.
        Assert.assertEquals(actionBean.getFormattedErrors().get(0),
                String.format(MolecularIndexNamingActionBean.DUPLICATE_HEADER, 2, "P5"));
        Assert.assertEquals(actionBean.getMolecularIndexNames().size(), 0);
    }

    @Test
    public void testBlankHeaderRow() {
        // Does the upload.
        actionBean.setInputStream(makeInputStream(blankHeaderRow, ","));
        actionBean.setFilename("test.csv");
        actionBean.setTechnology(MolecularIndexingScheme.TECHNOLOGY_ILLUMINA);
        actionBean.setCreateMissingNames(false);
        actionBean.upload();

        // Should have an error.
        Assert.assertEquals(actionBean.getFormattedErrors().get(0),
                String.format(MolecularIndexNamingActionBean.BLANK_ROW, 1));
        Assert.assertEquals(actionBean.getMolecularIndexNames().size(), 0);
    }

    @Test
    public void testBlankHeaderColumn() {
        // Does the upload.
        actionBean.setInputStream(makeInputStream(oneBlankHeader, ","));
        actionBean.setFilename("test.csv");
        actionBean.setTechnology(MolecularIndexingScheme.TECHNOLOGY_ILLUMINA);
        actionBean.setCreateMissingNames(false);
        actionBean.upload();

        // Should have an error.
        Assert.assertEquals(actionBean.getFormattedErrors().get(0),
                String.format(MolecularIndexNamingActionBean.BLANK_HEADER, 2));
        Assert.assertEquals(actionBean.getMolecularIndexNames().size(), 0);
    }

    @Test
    public void testWrongTechnology() {
        // Does the upload.
        actionBean.setInputStream(makeInputStream(p7is1Upload, ","));
        actionBean.setFilename("test.csv");
        actionBean.setTechnology(MolecularIndexingScheme.TECHNOLOGY_ION);
        actionBean.setCreateMissingNames(false);
        actionBean.upload();

        // Should have 2 errors and no returned spreadsheet.
        for (int i = 0; i < 2; ++i) {
            Assert.assertEquals(actionBean.getFormattedErrors().get(i),
                    String.format(MolecularIndexNamingActionBean.UNKNOWN_POSITION, i + 1,
                            p7is1Upload.get(0).get(i), "A, B"),
                    StringUtils.join(actionBean.getFormattedErrors(), "; "));
        }
        Assert.assertEquals(actionBean.getMolecularIndexNames().size(), 0);
    }

    @Test
    public void testIon() throws Exception {
        // Does the upload.
        actionBean.setInputStream(makeInputStream(ionUpload, ","));
        actionBean.setFilename("test.csv");
        actionBean.setTechnology(MolecularIndexingScheme.TECHNOLOGY_ION);
        actionBean.setCreateMissingNames(true);
        actionBean.upload();

        // Should not have any errors.
        Assert.assertFalse(actionBean.hasErrors());
        // Returned spreadsheet should be present and the names should match expected ones. The input data
        // won't, because the returned spreadsheet splits a combination header into individual columns.
        byte[] bytes = actionBean.makeSpreadsheet(actionBean.getIndexPositions(), actionBean.getDataRows(),
                actionBean.getMolecularIndexNames());
        List<String> names = validateReturnedSpreadsheet(bytes, ionUpload, true);
        Assert.assertTrue(CollectionUtils.isEqualCollection(names, ionSchemes), StringUtils.join(names, "; "));
    }

    @Test
    public void testCsv() throws Exception {
        // Does the upload.
        actionBean.setInputStream(makeInputStream(p7is1Upload, ","));
        actionBean.setFilename("test.csv");
        actionBean.setTechnology(MolecularIndexingScheme.TECHNOLOGY_ILLUMINA);
        actionBean.setCreateMissingNames(false);
        actionBean.upload();

        // Should not have any errors.
        Assert.assertFalse(actionBean.hasErrors());
        // Returned spreadsheet should be present but names are flagged as missing.
        byte[] bytes = actionBean.makeSpreadsheet(actionBean.getIndexPositions(), actionBean.getDataRows(),
                actionBean.getMolecularIndexNames());
        List<String> names = validateReturnedSpreadsheet(bytes, p7is1Upload, false);
        for (String name : names) {
            Assert.assertEquals(name, MolecularIndexNamingActionBean.UNKNOWN_NAME);
        }
    }

    @Test
    public void testQuotedCsv() {
        actionBean.setInputStream(makeInputStream(quotedP7is1, ","));
        actionBean.setFilename("test.csv");
        actionBean.setTechnology(MolecularIndexingScheme.TECHNOLOGY_ILLUMINA);
        actionBean.setCreateMissingNames(false);
        actionBean.upload();

        // Should not have any errors.
        Assert.assertFalse(actionBean.hasErrors());
        // Returned spreadsheet should have the name that already exists in Mercury for this sequence.
        Assert.assertEquals(actionBean.getMolecularIndexNames(), quotedP7is1Schemes);
    }

    @Test
    public void testTsv() throws Exception {
        // Does the upload.
        actionBean.setInputStream(makeInputStream(existingIndexUpload, "\\t"));
        actionBean.setFilename("test.tsv");
        actionBean.setTechnology(MolecularIndexingScheme.TECHNOLOGY_ILLUMINA);
        actionBean.setCreateMissingNames(false);
        actionBean.upload();

        // Should not have any errors.
        Assert.assertFalse(actionBean.hasErrors());
        // Returned spreadsheet should be present and the names should match existing ones. The input data
        // won't, because the returned spreadsheet splits a combination header into individual columns.
        byte[] bytes = actionBean.makeSpreadsheet(actionBean.getIndexPositions(), actionBean.getDataRows(),
                actionBean.getMolecularIndexNames());
        List<String> names = validateReturnedSpreadsheet(bytes, existingIndexUpload, true);
        Assert.assertTrue(CollectionUtils.isEqualCollection(names, existingSchemes), StringUtils.join(names, "; "));
    }

    @Test
    public void testExcelSpreadsheet() throws Exception {
        actionBean.setTechnology(MolecularIndexingScheme.TECHNOLOGY_ILLUMINA);

        // Makes an .xls spreadsheet from a predefined list of strings.
        List<MolecularIndexingScheme.IndexPosition> indexPositions = new ArrayList<>();
        for (String header : p7is1Upload.get(0)) {
            Assert.assertEquals(MolecularIndexNamingActionBean.DELIMITER_REGEX.split(header.trim()).length, 1,
                    "This test code only supports one position per header");
            for (MolecularIndexingScheme.IndexPosition position : MolecularIndexingScheme.IndexPosition.values()) {
                if (position.getTechnology().equals(actionBean.getTechnology()) &&
                        position.getPosition().equals(header)) {
                    indexPositions.add(position);
                }
            }
        }
        List<String> dataRows = new ArrayList<>();
        for (List<String> sequences : p7is1Upload.subList(1, p7is1Upload.size())) {
            dataRows.add(StringUtils.join(sequences, MolecularIndexNamingActionBean.CONCATENATOR));
        }
        byte[] inputBytes = actionBean.makeSpreadsheet(indexPositions, dataRows, null);
        // Does the upload.
        actionBean.setInputStream(new ByteArrayInputStream(inputBytes));
        actionBean.setFilename("test.xls");
        actionBean.setCreateMissingNames(true);
        actionBean.upload();

        // Should have no errors.
        Assert.assertTrue(actionBean.getFormattedErrors().isEmpty(),
                StringUtils.join(actionBean.getFormattedErrors(), "; "));
        // Checks the spreadsheet data and names.
        byte[] bytes = actionBean.makeSpreadsheet(actionBean.getIndexPositions(), actionBean.getDataRows(),
                actionBean.getMolecularIndexNames());
        List<String> names = validateReturnedSpreadsheet(bytes, p7is1Upload, false);
        Assert.assertEquals(names, p7is1Schemes);
    }

    // Parses the returned spreadsheet, validates the header and data, and returns the index names.
    private List<String> validateReturnedSpreadsheet(byte[] bytes, List<List<String>> expectedHeaderAndData,
            boolean skipColumnChecks) throws Exception {
        Assert.assertNotNull(bytes);
        List<List<String>> headerAndData = parseSpreadsheet(new ByteArrayInputStream(bytes));
        Assert.assertEquals(headerAndData.size(), expectedHeaderAndData.size());
        List<String> names = new ArrayList<>();
        for (int i = 0; i < expectedHeaderAndData.size(); ++i) {
            List<String> expectedRow = expectedHeaderAndData.get(i);
            List<String> row = headerAndData.get(i);
            if (!skipColumnChecks) {
                List<String> rowWithoutLast = row.subList(0, row.size() - 1);
                // Row without the last column (the added index name) should match unless there were
                // combined position headers.
                Assert.assertEquals(rowWithoutLast, expectedRow, "At row " + i + " found " + StringUtils.join(row, "; "));
            }
            // Checks the last column.
            String lastColumn = row.get(row.size() - 1);
            if (i == 0) {
                Assert.assertEquals(lastColumn, MolecularIndexNamingActionBean.NAME_HEADER);
            } else {
                Assert.assertTrue(StringUtils.isNotBlank(lastColumn));
                names.add(lastColumn);
            }
        }
        return names;
    }

    private InputStream makeInputStream(List<List<String>> fileContent, String separator) {
        StringBuilder sb = new StringBuilder();
        for (List<String> line : fileContent) {
            sb.append(StringUtils.join(line, separator)).append(System.lineSeparator());
        }
        return IOUtils.toInputStream(sb.toString());
    }

    /** Parses the spreadsheet into list of header and data rows. */
    public static List<List<String>> parseSpreadsheet(InputStream inputStream) throws Exception {
        GenericTableProcessor processor = new GenericTableProcessor();
        PoiSpreadsheetParser parser = new PoiSpreadsheetParser(Collections.singletonMap("Sheet1", processor));
        parser.processUploadFile(inputStream);
        return processor.getHeaderAndDataRows();
    }

}