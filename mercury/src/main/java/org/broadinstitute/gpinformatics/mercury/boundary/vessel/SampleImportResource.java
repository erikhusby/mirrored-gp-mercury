package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.Path;

/**
 * JAX-RS web service to import samples from other systems, e.g. BSP
 */
@Path("/sampleimport")
@Stateful
@RequestScoped
public class SampleImportResource {
}
