package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import org.broadinstitute.gpinformatics.athena.boundary.products.InvalidProductException;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.BarcodedTubeDao;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MaterialType;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowBucketDef;
import org.testng.Assert;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * TODO scottmat fill in javadoc!!!
 */
@Dependent
public class LabBatchTestUtils {

    BucketDao bucketDao;

    ProductDao productDao;
    ResearchProjectDao researchProjectDao;
    BarcodedTubeDao tubeDao;

    @Inject
    public LabBatchTestUtils(BucketDao bucketDao,
                             ProductDao productDao,
                             ResearchProjectDao researchProjectDao,
                             BarcodedTubeDao barcodedTubeDao) {
        this.bucketDao = bucketDao;
        this.productDao = productDao;
        this.researchProjectDao = researchProjectDao;
        this.tubeDao = barcodedTubeDao;
    }

    protected Bucket putTubesInSpecificBucket(String bucketName, BucketEntry.BucketEntryType bucketEntryType,
                                                     LinkedHashMap<String, BarcodedTube> mapBarcodeToTube) {
        Bucket bucket = initializeBucket(bucketName);

        ProductOrder stubTestPDO = ProductOrderTestFactory.createDummyProductOrder(LabBatchEJBTest.STUB_TEST_PDO_KEY);
        stubTestPDO.setTitle(stubTestPDO.getTitle() + ((new Date()).getTime()));
        stubTestPDO.updateAddOnProducts(Collections.<Product>emptyList());
        try {
            stubTestPDO.setProduct(productDao.findByBusinessKey(Product.EXOME_EXPRESS_V2_PART_NUMBER));
        } catch (InvalidProductException e) {
            Assert.fail(e.getMessage());
        }
        stubTestPDO.setResearchProject(researchProjectDao.findByTitle("ADHD"));
        for (LabVessel vessel : mapBarcodeToTube.values()) {
            bucket.addEntry(stubTestPDO, vessel, bucketEntryType);
        }

        bucketDao.persist(bucket);
        bucketDao.flush();

        for (LabVessel vessel : mapBarcodeToTube.values()) {
            Assert.assertTrue(bucket.findEntry(vessel) != null);
        }

        return bucket;
    }

    public Bucket initializeBucket(String bucketName) {
        Bucket bucket = bucketDao.findByName(bucketName);

        if (bucket == null) {
            bucket = new Bucket(new WorkflowBucketDef(bucketName));
            bucketDao.persist(bucket);
            bucketDao.flush();
        }
        return bucket;
    }

    public LinkedHashMap<String, BarcodedTube> initializeTubes(List<String> vesselSampleList) {

        LinkedHashMap<String, BarcodedTube> barcodedTubes = new LinkedHashMap<>();
        // starting rack
        for (int sampleIndex = 1; sampleIndex <= vesselSampleList.size(); sampleIndex++) {
            String barcode = "R" + sampleIndex + sampleIndex + sampleIndex + sampleIndex + sampleIndex + sampleIndex + vesselSampleList.get(sampleIndex-1);
            String bspStock = vesselSampleList.get(sampleIndex - 1);
            BarcodedTube bspAliquot = new BarcodedTube(barcode);
            BspSampleData bspSampleData = new BspSampleData(new HashMap<BSPSampleSearchColumn, String>() {{
                put(BSPSampleSearchColumn.MATERIAL_TYPE, MaterialType.CELLS_PELLET_FROZEN.getDisplayName());
            }});
            bspAliquot.addSample(new MercurySample(bspStock, bspSampleData));
            tubeDao.persist(bspAliquot);
            barcodedTubes.put(barcode, bspAliquot);
        }
        tubeDao.flush();
        return barcodedTubes;
    }
}
