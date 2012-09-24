package org.broadinstitute.gpinformatics.mercury.control.dao.authentication;

import org.broadinstitute.gpinformatics.mercury.entity.DB;
import org.broadinstitute.gpinformatics.mercury.entity.authentication.PageAuthorization;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.util.Collection;
import java.util.Map;

/**
 *
 * PageAuthorizationDao provides boundary objects with a mechanism to access, insert and manipulate
 * {@link PageAuthorization}s defined within the system
 *
 * @author Scott Matthews
 *         Date: 5/1/12
 *         Time: 4:14 PM
 */

//@Stateful
@RequestScoped
public class PageAuthorizationDao {

//    @PersistenceContext(type = PersistenceContextType.EXTENDED)
//    private EntityManager entityManager;

    /**
     * Temporary mock database for the initial phases of Mercury development
     */
    @Inject
    private DB db;

    /**
     * persist Saves a newly created {@link PageAuthorization} instance to the system
     * @param pageAuthorizationIn New instance of a {@link PageAuthorization} to be saved
     */
    public void persist(PageAuthorization pageAuthorizationIn)  {
        db.addPageAuthorization(pageAuthorizationIn);
    }

    /**
     * findPageAuthorizationByPage searches the system for an instance of a {@link PageAuthorization} that matches
     * the presentation resource path given
     * @param pageNameIn A path to a presentation resource that may be protected by the system
     * @return The matching instance of a pre-defined {@link PageAuthorization} if one exists.  Null if it does not
     */
    public PageAuthorization findPageAuthorizationByPage(String pageNameIn) {

        PageAuthorization authorization = db.getPageAuthorizationMap().get(pageNameIn);
        if(null == authorization) {
            for(Map.Entry<String, PageAuthorization> currAuthorization:db.getPageAuthorizationMap().entrySet()) {
                if(pageNameIn.startsWith(currAuthorization.getKey())) {
                    authorization = currAuthorization.getValue();
                }
            }
        }

        return authorization;
    }

    /**
     * removePageAuthorization provides boundary objects with the ability to remove a previously defined
     * {@link PageAuthorization} from the system
     * @param pageIn Instance of a previously defined  {@link PageAuthorization} entity to be removed
     */
    public void removePageAuthorization(PageAuthorization pageIn) {
        db.removePageAuthorization(pageIn);
    }

    /**
     *
     * getAllPageAuthorizations provides boundary objects with the ability to find all previously defined
     * {@link PageAuthorization}s registered in the system
     *
     * @return A {@link Collection} of all defined {@link PageAuthorization} registrations contained in the system
     */
    public Collection<PageAuthorization> getAllPageAuthorizations() {
        return db.getPageAuthorizationMap().values();
    }



}
