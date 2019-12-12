package org.broadinstitute.gpinformatics.mercury.boundary.run;

import org.broadinstitute.gpinformatics.infrastructure.security.Role;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RunAs;
import javax.ejb.Stateless;
import java.util.concurrent.Callable;

/**
 * Test-only EJB that grants FingerprintWebService role, to allow testing FingerprintResource without knowing a
 * concrete user password.
 */
@Stateless
@RunAs(Role.Constants.FINGERPRINT_WEB_SERVICE)
@PermitAll
public class FpWsRoleEjb {
    public <V> V call(Callable<V> callable) throws Exception {
        return callable.call();
    }
}
