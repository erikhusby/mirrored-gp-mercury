package org.broadinstitute.gpinformatics.athena.presentation.tokenimporters;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.collection.SampleCollection;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPGroupCollectionList;
import org.broadinstitute.gpinformatics.infrastructure.common.TokenInput;
import org.json.JSONException;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.text.MessageFormat;

/**
 * Token Input support for BSP Groups and Collections.
 */
@Dependent
public class BspGroupCollectionTokenInput extends TokenInput<SampleCollection> {

    private BSPGroupCollectionList bspCollectionList;

    public BspGroupCollectionTokenInput() {
        super(SINGLE_LINE_FORMAT);
    }

    public String getJsonString(String query) throws JSONException {
        return createItemListString(bspCollectionList.find(query));
    }

    @Override
    protected String getTokenId(SampleCollection collection) {
        return String.valueOf(collection.getCollectionId());
    }

    @Override
    protected String getTokenName(SampleCollection collection) {
        return collection.getCollectionName();
    }

    @Override
    protected String formatMessage(String messageString, SampleCollection collection) {
        return MessageFormat.format(messageString,
                collection.getGroup().getGroupName() + " - " + collection.getCollectionName());
    }

    @Override
    protected SampleCollection getById(String key) {
        return StringUtils.isNumeric(key) ? bspCollectionList.getById(Long.valueOf(key)) : null;
    }

    @Inject
    public void setBspCollectionList(BSPGroupCollectionList bspCollectionList) {
        this.bspCollectionList = bspCollectionList;
    }
}
