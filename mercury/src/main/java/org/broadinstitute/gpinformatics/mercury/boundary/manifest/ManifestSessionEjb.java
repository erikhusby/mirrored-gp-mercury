package org.broadinstitute.gpinformatics.mercury.boundary.manifest;

import org.broadinstitute.gpinformatics.mercury.control.dao.manifest.ManifestSessionDao;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestSession;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
@Stateful
public class ManifestSessionEjb {

    @Inject
    private ManifestSessionDao manifestSessionDao;

    public void save(ManifestSession manifestSession) {
        manifestSessionDao.persist(manifestSession);
    }
}
