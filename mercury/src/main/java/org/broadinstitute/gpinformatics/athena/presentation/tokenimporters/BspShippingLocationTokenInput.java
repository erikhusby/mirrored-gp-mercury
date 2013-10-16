package org.broadinstitute.gpinformatics.athena.presentation.tokenimporters;

import org.broadinstitute.bsp.client.site.Site;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSiteList;
import org.broadinstitute.gpinformatics.infrastructure.common.TokenInput;
import org.json.JSONException;

import javax.inject.Inject;
import java.text.MessageFormat;

/**
 * Token Input support for Shipping Locations (aka Sites).
 */
public class BspShippingLocationTokenInput extends TokenInput<Site> {

    @Inject
    BSPSiteList bspSiteList;

    public BspShippingLocationTokenInput() {
        super(DOUBLE_LINE_FORMAT);
    }

    public String getJsonString(String query) throws JSONException {
        return createItemListString(bspSiteList.find(query));
    }

    @Override
    protected String getTokenId(Site site) {
        return String.valueOf(site.getId());
    }

    @Override
    protected String getTokenName(Site site) {
        return site.getName();
    }

    @Override
    protected String formatMessage(String messageString, Site tokenObject) {
        return MessageFormat
                .format(messageString, tokenObject.getName(),
                        tokenObject.getPrimaryShipper() + " " + tokenObject.getAddress());
    }

    @Override
    protected Site getById(String key) {
        return bspSiteList.getById(Long.valueOf(key));
    }
}
