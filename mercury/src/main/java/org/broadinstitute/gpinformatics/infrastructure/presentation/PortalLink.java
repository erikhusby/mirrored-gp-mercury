package org.broadinstitute.gpinformatics.infrastructure.presentation;

import org.broadinstitute.gpinformatics.infrastructure.portal.PortalConfig;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

/**
 * This class is used to generate Portal links for the UI.
 */
@Dependent
public class PortalLink {
    private static final String LOAD_REQ =
            "/CRSP/public/Requisition.action?get=&key=";

    @Inject
    private PortalConfig portalConfig;

    /**
     * Create a link using the requisition key.
     *
     * @param requisitionKey the key to the requisition on the portal
     *
     * @return a string representing the URL
     */

    public String browseRequisitionUrl(String requisitionKey) {
        return portalConfig.getUrlBase() + LOAD_REQ + requisitionKey;
    }
}
