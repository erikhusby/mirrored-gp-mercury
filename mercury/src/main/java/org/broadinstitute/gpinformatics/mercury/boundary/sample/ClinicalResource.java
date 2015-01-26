package org.broadinstitute.gpinformatics.mercury.boundary.sample;

import com.sun.jersey.api.client.Client;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfig;
import org.broadinstitute.gpinformatics.infrastructure.portal.PortalConfig;
import org.broadinstitute.gpinformatics.mercury.boundary.ResourceException;
import org.broadinstitute.gpinformatics.mercury.control.AbstractJerseyClientService;
import org.broadinstitute.gpinformatics.mercury.control.LoginAndPassword;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.sax.SAXSource;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This resource supports the Rest call to retrieve PHI for the CRSP platform.  Calls to this service are protected
 * and accessible only by users in a select group
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

}
