package org.broadinstitute.gpinformatics.infrastructure.mercury;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Impl;
import org.broadinstitute.gpinformatics.mercury.boundary.bucket.BucketBean;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import javax.annotation.Nonnull;
import javax.enterprise.inject.Default;
import javax.inject.Inject;

@Impl
@Default
public class MercuryClientServiceImpl implements MercuryClientService {

    @Inject
    private MercurySampleDao mercurySampleDao;
    @Inject
    private BucketBean bucketBean;
    @Inject
    private BucketDao bucketDao;


    @Override
    public boolean addSampleToPicoBucket(ProductOrderSample pdoSample) {
        String stockSampleName = pdoSample.getSampleName();
        MercurySample mercurySample = mercurySampleDao.findBySampleKey(stockSampleName);
        if (null == mercurySample) {
            return false;
        }
        // todo finds vessel for sample
        LabVessel labVessel = null;


        String bucketName = "Pico/Plating Bucket";
        Bucket picoBucket = bucketDao.findByName(bucketName);
        if (null == picoBucket) {
            throw new RuntimeException("Cannot find bucket named '" + bucketName + "'");
        }

        // Validates entry into bucket.
        // todo The only criterion is that the sample be genomic DNA.


        String operator = "bsp user " + pdoSample.getProductOrder().getCreatedBy();
        LabEventType eventType;
        String eventLocation;
        BucketEntry bucketEntry = bucketBean.add(labVessel, picoBucket, operator, eventType, eventLocation);


        return false;
    }
}
