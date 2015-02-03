package org.broadinstitute.gpinformatics.mercury.boundary.sample;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfig;
import org.broadinstitute.gpinformatics.infrastructure.portal.PortalConfig;
import org.broadinstitute.gpinformatics.mercury.control.LoginAndPassword;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;

/**
 * ClinicalResource provides web services related to accessioning samples into Mercury along with sample data used for
 * lab processing, diagnostics, and analysis of the data.
 */
@Path("clinical")
@Stateful
@RequestScoped
public class ClinicalResource {
    private static final Log log = LogFactory.getLog(ClinicalResource.class);

    @Inject
    private BSPConfig bspConfig;

    @Inject
    private PortalConfig portalConfig;

    @Inject
    private ProductOrderDao productOrderDao;

    @Context
    SecurityContext sc;

    private LoginAndPassword currentConfig;

    public void createAccessioningSession(String username, String manifestName, String researchProjectKey,
                                          Boolean isFromSampleKit) {
        throw new UnsupportedOperationException(
                "Samples in containers other than from Broad sample kits are currently not supported.");
    }
}
