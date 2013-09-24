package org.broadinstitute.gpinformatics.infrastructure.presentation;

import org.broadinstitute.gpinformatics.infrastructure.portal.PortalConfig;

import javax.inject.Inject;

/**
 * This class is used to generate Portal links for the UI using the requisition key.
 */
public class PortalLink {
    private static final String LOAD_REQ =
            "public/Requisition.action?get=&key=";

    @Inject
    private PortalConfig portalConfig;

    public String browseRequisitionUrl(String requisitionKey) {
        return portalConfig.getUrlBase() + LOAD_REQ + requisitionKey;
    }
}
