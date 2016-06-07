/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2016 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure.submission;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.samples.MercurySampleData;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;

@Test(groups = TestGroups.DATABASE_FREE)
public class SubmissionDtoFetcherDbFreeTest {
    public void testGetSamplesForProjectHasNoNullKeys() throws Exception {
        SubmissionDtoFetcher submissionDtoFetcher = new SubmissionDtoFetcher(null, null, null, null, null, null);

        Set<Metadata> nullMetadata = Collections.singleton(new Metadata(Metadata.Key.SAMPLE_ID, (String)null));
        SampleData sampleData = new MercurySampleData("SM-234", nullMetadata);
        ProductOrderSample productOrderSample = new ProductOrderSample("SM-234", sampleData);

        Set<Metadata> okMetadata = Collections.singleton(new Metadata(Metadata.Key.SAMPLE_ID, "collabSample"));
        SampleData moreSampleData = new MercurySampleData("SM-OK", okMetadata);
        ProductOrderSample moreProductOrderSample = new ProductOrderSample("SM-OK", moreSampleData);

        List<ProductOrderSample> productOrderSamples = Arrays.asList(productOrderSample, moreProductOrderSample);
        Map<String, Collection<ProductOrder>> collaboratorSampleNameToPdoMap =
                submissionDtoFetcher.getCollaboratorSampleNameToPdoMap(productOrderSamples);

        assertThat(collaboratorSampleNameToPdoMap.keySet(), hasSize(1));
        assertThat(collaboratorSampleNameToPdoMap.keySet(), everyItem(not(isEmptyOrNullString())));
    }
}
