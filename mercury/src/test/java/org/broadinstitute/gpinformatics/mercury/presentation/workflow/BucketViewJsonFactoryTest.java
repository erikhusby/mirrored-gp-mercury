/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2015 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.mercury.presentation.workflow;


import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketTestFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@Test(groups = TestGroups.DATABASE_FREE)
public class BucketViewJsonFactoryTest {

    private static final String testUser = "Alfred E. Neumann";
    private BSPUserList bspUserList = Mockito.mock(BSPUserList.class);

    @BeforeMethod
    public void setUp() throws Exception {
        Mockito.when(bspUserList.getUserFullName(Mockito.anyLong())).thenReturn(testUser);

    }

    public void testBucketEntry() throws JSONException {
        final ProductOrder productOrder = ProductOrderTestFactory.buildIceProductOrder(1);
        final String barcode = productOrder.getSamples().iterator().next().getSampleKey();
        final String bucketName = "Pre-flight";
        Bucket bucket = BucketTestFactory.getBucket(productOrder, barcode, bucketName,
                BucketEntry.BucketEntryType.PDO_ENTRY);
        JSONArray jsonArray = BucketViewJsonFactory.toJson(bucket, bspUserList);
        JSONObject jsonObject = jsonArray.optJSONObject(0);
        assertThat(jsonObject.getString(BucketViewJsonFactory.BATCH_NAME), is("batchName"));
        assertThat(jsonObject.getString(BucketViewJsonFactory.ADD_ONS), is("addOnProduct"));
        assertThat(jsonObject.getString(BucketViewJsonFactory.BUCKET_ENTRY_TYPE),
                is(BucketEntry.BucketEntryType.PDO_ENTRY.getName()));
        assertThat(jsonObject.getString(BucketViewJsonFactory.PRODUCT), is("productName"));
        assertThat(jsonObject.getString(BucketViewJsonFactory.BATCH_NAME), is("batchName"));
    }

    public void testReworkEntry() throws JSONException {
        final ProductOrder productOrder = ProductOrderTestFactory.buildIceProductOrder(1);
        final String barcode = productOrder.getSamples().iterator().next().getSampleKey();
        final String bucketName = "Pre-flight";
        Bucket bucket = BucketTestFactory.getBucket(productOrder, barcode, bucketName,
                BucketEntry.BucketEntryType.REWORK_ENTRY);

        JSONArray jsonArray = BucketViewJsonFactory.toJson(bucket, bspUserList);
        JSONObject jsonObject = jsonArray.optJSONObject(0);
        assertThat(jsonObject.getString(BucketViewJsonFactory.BUCKET_ENTRY_TYPE),
                is(BucketEntry.BucketEntryType.REWORK_ENTRY.getName()));
        assertThat(jsonObject.getString(BucketViewJsonFactory.REWORK_COMMENT), is("hola"));
        assertThat(jsonObject.getString(BucketViewJsonFactory.REWORK_DATE), notNullValue());
        assertThat(jsonObject.getString(BucketViewJsonFactory.REWORK_REASON), is("foo"));
        assertThat(jsonObject.getString(BucketViewJsonFactory.REWORK_USER), is(testUser));
    }

}
