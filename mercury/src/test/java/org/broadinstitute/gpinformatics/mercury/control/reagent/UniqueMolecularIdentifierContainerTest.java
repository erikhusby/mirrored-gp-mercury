package org.broadinstitute.gpinformatics.mercury.control.reagent;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.LimsQueryResource;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.SequencingTemplateFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.zims.IlluminaRunResource;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VarioskanParserTest;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.VesselToSectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.UMIReagent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaChamber;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaRun;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.SequencingTemplateLaneType;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.SequencingTemplateType;
import org.hibernate.metamodel.source.annotations.entity.IdType;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Test persistence of UMI reagents.
 */
@Test(groups = TestGroups.STANDARD)
public class UniqueMolecularIdentifierContainerTest extends Arquillian {

    private static final String WGS_LC_PLATE = "000009472573";
    private static final String WGS_SEQ_RUN = "170401_SL-HXF_0640_BFCHG52TALXX";
    private static final String DUAL_BARCODE = "87654";

    @Inject
    private UniqueMolecularIdentifierReagentFactory umiFactory;

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private IlluminaRunResource illuminaRunResource;

    @Inject
    private LimsQueryResource limsQueryResource;

    @Inject
    private UserTransaction utx;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @BeforeMethod
    public void setUp() throws Exception {
        if (utx == null) {
            return;
        }
        utx.begin();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        if (utx == null) {
            return;
        }

        utx.rollback();
    }

    public void testBasic() {
        InputStream testSpreadSheetInputStream = VarioskanParserTest.getTestResource("UMIReagents.xlsx");
        Assert.assertNotNull(testSpreadSheetInputStream);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        String timestamp = simpleDateFormat.format(new Date());

        try {
            // replace plate barcode with timestamps, to avoid unique constraint
            Workbook workbook = WorkbookFactory.create(testSpreadSheetInputStream);
            Sheet sheet = workbook.getSheet("Sheet1");
            String dualTubeBarcode = "UMI" + timestamp + "DUAL";
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    Cell plateCell = row.getCell(1);
                    plateCell.setCellType(Cell.CELL_TYPE_STRING);
                    String oldValue = plateCell.getStringCellValue();
                    if (oldValue.equals(DUAL_BARCODE)) {
                        plateCell.setCellValue(dualTubeBarcode);
                    } else {
                        plateCell.setCellValue("UMI" + timestamp + i);
                    }
                }
            }
            File tempFile = File.createTempFile("UMIReagents", ".xlsx");
            workbook.write(new FileOutputStream(tempFile));

            MessageCollection messageCollection = new MessageCollection();
            List<LabVessel> labVessels = umiFactory.buildUMIFromSpreadsheet(
                    new FileInputStream(tempFile), messageCollection);

            Assert.assertFalse(messageCollection.hasErrors(),
                    "messageCollection failed: " + messageCollection.getErrors());

            labVesselDao.flush();
            labVesselDao.clear();

            LabVessel labVessel = labVesselDao.findByIdentifier(labVessels.get(0).getLabel());
            Set<SampleInstanceV2> sampleInstances =
                    labVessel.getContainerRole().getSampleInstancesAtPositionV2(VesselPosition.A01);
            Assert.assertEquals(sampleInstances.size(), 1);
            SampleInstanceV2 sampleInstance = sampleInstances.iterator().next();
            Assert.assertEquals(sampleInstance.getReagents().size(), 1);
            UMIReagent umiReagent =
                    (UMIReagent) sampleInstance.getReagents().iterator().next();
            Assert.assertEquals(umiReagent.getUmiLength(), Long.valueOf(6));
            Assert.assertEquals(umiReagent.getSpacerLength(), Long.valueOf(3));
            Assert.assertEquals(umiReagent.getUmiLocation(), UMIReagent.UMILocation.INLINE_FIRST_READ);

            // Grab tube
            LabVessel barcodedTubeLv = labVesselDao.findByIdentifier(labVessels.get(labVessels.size() - 1).getLabel());
            Assert.assertTrue(OrmUtil.proxySafeIsInstance(barcodedTubeLv, BarcodedTube.class));

            BarcodedTube barcodedTube = OrmUtil.proxySafeCast(barcodedTubeLv, BarcodedTube.class);
            Assert.assertEquals(barcodedTube.getTubeType(), BarcodedTube.BarcodedTubeType.MatrixTube075);
            UMIReagent firstRead = new UMIReagent(UMIReagent.UMILocation.BEFORE_FIRST_READ, 3L, 2L);
            UMIReagent secondRead = new UMIReagent(UMIReagent.UMILocation.BEFORE_SECOND_READ, 3L, 2L);
            Assert.assertEquals(barcodedTube.getReagentContents().size(), 2);
            for (Reagent reagent: barcodedTube.getReagentContents()) {
                UMIReagent actualUMI = (UMIReagent) reagent;
                if (actualUMI.getUmiLocation() == UMIReagent.UMILocation.BEFORE_FIRST_READ) {
                    Assert.assertEquals(actualUMI, firstRead);
                } else if (actualUMI.getUmiLocation() == UMIReagent.UMILocation.BEFORE_SECOND_READ) {
                    Assert.assertEquals(actualUMI, secondRead);
                } else {
                    Assert.fail("Failed to find one of the expected UMIs");
                }
            }

            //Add a UMI to WGS Clean up plate
            LabVessel wgsLCPLateLV = labVesselDao.findByIdentifier(WGS_LC_PLATE);
            StaticPlate wgsLcPlate = OrmUtil.proxySafeCast(wgsLCPLateLV, StaticPlate.class);
            LabEvent labEvent = new LabEvent(LabEventType.UMI_ADDITION, new Date(), "Mercury", 1L, 1L, "MercuryTest");
            labEvent.getVesselToSectionTransfers().add(new VesselToSectionTransfer(barcodedTube, SBSSection.ALL96,
                    wgsLcPlate.getContainerRole(), wgsLcPlate, labEvent));
            labVesselDao.persist(labEvent);
            labVesselDao.flush();

            SequencingTemplateType sequencingTemplateType = limsQueryResource
                    .fetchIlluminaSeqTemplate("HG52TALXX", SequencingTemplateFactory.QueryVesselType.FLOWCELL, false);

            //Check against illumina run query
            for (SequencingTemplateLaneType lane: sequencingTemplateType.getLanes()) {
                String readStructure = lane.getReadStructure();
                Assert.assertEquals(readStructure, "3M2S151T8B8B3M2S151T");
            }

        } catch (IOException | InvalidFormatException e) {
            throw new RuntimeException(e);
        }
    }
}
