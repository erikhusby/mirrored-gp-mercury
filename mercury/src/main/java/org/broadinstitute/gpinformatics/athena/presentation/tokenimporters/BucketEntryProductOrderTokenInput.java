/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2018 by the 
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support 
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its 
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.athena.presentation.tokenimporters;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.common.TokenInput;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketEntryDao;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.json.JSONException;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Dependent
public class BucketEntryProductOrderTokenInput extends TokenInput<ProductOrder> {

    @Inject
    private BucketEntryDao bucketEntryDao;
    @Inject
    private ProductOrderDao productOrderDao;

    public BucketEntryProductOrderTokenInput() {
        super(SINGLE_LINE_FORMAT);
    }

    @Override
    protected ProductOrder getById(String key) {
        return productOrderDao.findByBusinessKey(key);
    }

    public String getJsonString(Bucket bucket, String query) throws JSONException {
        List<String> searchTerms = new ArrayList<>();
        if (query != null) {
            searchTerms = Stream.of(query.split("[\\s|,]+")).map(String::new).collect(Collectors.toList());
        }

        List<BucketEntry> bucketEntries = bucketEntryDao.findByBucketAndProductOrder(bucket, searchTerms);
        List<ProductOrder> orderList =
            bucketEntries.stream().map(BucketEntry::getProductOrder)
                .sorted(Comparator.comparing(ProductOrder::getBusinessKey)).distinct().collect(Collectors.toList());
        return createItemListString(orderList);
    }

    @Override
    protected String getTokenId(ProductOrder productOrder) {
        return productOrder.getBusinessKey();
    }

    @Override
    protected String getTokenName(ProductOrder productOrder) {
        return productOrder.getJiraTicketKey();
    }

    @Override
    protected String formatMessage(String messageString, ProductOrder productOrder) {
        return MessageFormat.format(messageString, String.format("%s %s", productOrder.getJiraTicketKey(), productOrder.getTitle()));
    }
}
