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

    private static final String ADDITIONAL_LINE_FORMAT = "<div class=\"ac-dropdown-subtext\">{0}</div>";

    @Inject
    private BSPSiteList bspSiteList;

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

    /**
     * Reformat an address so its lines split into different menu lines, when shown in the HTML drop down menu.
     *
     * @param address the address, with embedded CRs
     * @return the HTML formatted address text
     */
    private static String formatAddress(String address) {
        StringBuilder formattedAddress = new StringBuilder();
        for (String line : address.split("\n")) {
           formattedAddress.append(MessageFormat.format(ADDITIONAL_LINE_FORMAT, line));
        }
        return formattedAddress.toString();
    }

    @Override
    protected String formatMessage(String messageString, Site tokenObject) {
        return MessageFormat.format(messageString, tokenObject.getName(), tokenObject.getPrimaryShipper()) +
               formatAddress(tokenObject.getAddress());
    }

    @Override
    protected Site getById(String key) {
        return bspSiteList.getById(Long.valueOf(key));
    }
}
