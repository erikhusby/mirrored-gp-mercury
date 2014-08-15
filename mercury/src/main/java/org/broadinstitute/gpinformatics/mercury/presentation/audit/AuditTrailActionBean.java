package org.broadinstitute.gpinformatics.mercury.presentation.audit;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.ValidationMethod;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditedRevDto;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.ReflectionUtil;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

@UrlBinding(AuditTrailActionBean.ACTIONBEAN_URL)
public class AuditTrailActionBean extends CoreActionBean {
    static final String ACTIONBEAN_URL = "/audit/auditTrail.action";
    static final String AUDIT_TRAIL_ENTRY_ACTIONBEAN_URL = "/audit/auditTrailEntry.action";
    // Jsp pages
    static final String AUDIT_TRAIL_LISTING_PAGE = "/audit/trail_list.jsp";
    static final String AUDIT_TRAIL_ENTRY_PAGE = "/audit/view_trail_entry.jsp";
    static final String AUDITED_ENTITY_PAGE = "/audit/view_entity.jsp";
    // Events
    static final String LIST_AUDIT_TRAILS = "listAuditTrails";
    static final String VIEW_AUDIT_TRAIL_ENTRIES = "viewAuditTrailEntries";
    static final String VIEW_ENTITY = "viewEntity";

    private static final String ANY_USER = "Any user";
    private static final String NO_USER = "(no user)";
    private static final String ANY_ENTITY = "Any type";

    @Inject
    private AuditReaderDao auditReaderDao;

    private static final List<String> auditUsernames =  new ArrayList<>();
    private static final Map<String, String> displayToCanonicalClassnames = new HashMap<>();
    private static final Map<String, String> canonicalToDisplayClassnames = new HashMap<>();
    private static final List<String> entityDisplayNames = new ArrayList<>();

    private String searchEntityDisplayName;
    private String searchUsername;
    private List<AuditedRevDto> auditTrailList = new ArrayList<>();

    public String getAuditTrailEntryActionBean() {
        return AUDIT_TRAIL_ENTRY_ACTIONBEAN_URL;
    }

    public String getSearchEntityDisplayName() {
        return searchEntityDisplayName;
    }

    public void setSearchEntityDisplayName(String searchEntityDisplayName) {
        this.searchEntityDisplayName = searchEntityDisplayName;
    }

    public String getSearchUsername() {
        return searchUsername;
    }

    public void setSearchUsername(String searchUsername) {
        this.searchUsername = searchUsername;
    }

    public List<AuditedRevDto> getAuditTrailList() {
        return auditTrailList;
    }

    public List<String> getAuditUsernames() {
        setupUsernames();
        return auditUsernames;
    }

    public List<String> getEntityDisplayNames() {
        setupClassnames();
        return entityDisplayNames;
    }

    public String getAnyUser() {
        return ANY_USER;
    }

    public String getAnyEntity() {
        return ANY_ENTITY;
    }

    public static String getDisplayToCanonicalClassname(String displayClassname) {
        setupClassnames();
        return displayToCanonicalClassnames.get(displayClassname);
    }

    public static String getCanonicalToDisplayClassname(String canonicalClassname) {
        setupClassnames();
        return canonicalToDisplayClassnames.get(canonicalClassname);
    }

    /** These entity classnames are removed from the Entity Type dropdown. */
    private static final SortedSet<String> excludedEntityTypes = new TreeSet<String>(){{
        add("RevInfo");
    }};

    /////////////////////////////////////////////////////////////////////////////////////////////////////

    @DefaultHandler
    public Resolution selectAuditTrails() {
        // Reload this to pick up any additional users.
        auditUsernames.clear();
        setupUsernames();
        return new ForwardResolution(AUDIT_TRAIL_LISTING_PAGE);
    }

    private void setupUsernames() {
        if (auditUsernames.size() == 0) {
            auditUsernames.addAll(auditReaderDao.getAllAuditUsername());
            if (auditUsernames.remove(null)) {
                auditUsernames.add(NO_USER);
            }
        }
    }

    private static void setupClassnames() {
        if (entityDisplayNames.size() == 0) {
            displayToCanonicalClassnames.clear();
            canonicalToDisplayClassnames.clear();
            for (String classname : ReflectionUtil.getMercuryAthenaEntityClassnames()) {
                // Makes display names from the ending of canonical classname, but disambiguates
                // it if necessary by including more of the canonical classname.
                for (int idx = classname.lastIndexOf('.'); idx >= 0; idx = classname.lastIndexOf('.', idx)) {
                    String displayName = classname.substring(idx + 1);
                    if (excludedEntityTypes.contains(displayName)) {
                        break;
                    }
                    if (!entityDisplayNames.contains(displayName)) {
                        entityDisplayNames.add(displayName);
                        displayToCanonicalClassnames.put(displayName, classname);
                        canonicalToDisplayClassnames.put(classname, displayName);
                        break;
                    }
                }
            }
            Collections.sort(entityDisplayNames);
        }
    }


    @HandlesEvent(LIST_AUDIT_TRAILS)
    public Resolution listAuditTrails() {
        generateAuditTrailList();
        return new ForwardResolution(AUDIT_TRAIL_LISTING_PAGE);
    }

    private void generateAuditTrailList() {
        // Search Envers using the user's criteria.
        String usernameParam =  ANY_USER.equals(searchUsername) ? AuditReaderDao.IS_ANY_USER :
                (NO_USER.equals(searchUsername) ? AuditReaderDao.IS_NULL_USER : searchUsername);
        Set<Long> revIds = (SortedSet)auditReaderDao.fetchAuditIds(getDateRange().getStartTime().getTime()/1000,
                getDateRange().getEndTime().getTime()/1000, usernameParam).keySet();
        auditTrailList = auditReaderDao.fetchAuditedRevs(revIds);
        // Convert canonical classnames to our display names.
        for (AuditedRevDto auditedRevDto : auditTrailList) {
            List<String> displayNames = new ArrayList<>();
            for (String canonicalName : auditedRevDto.getEntityTypeNames()) {
                displayNames.add(canonicalToDisplayClassnames.get(canonicalName));
            }
            auditedRevDto.getEntityTypeNames().clear();
            auditedRevDto.getEntityTypeNames().addAll(displayNames);
        }

        // Filters out the dtos that don't match the entity type criteria.  Ignore criteria if set to "any".
        if (!ANY_ENTITY.equals(searchEntityDisplayName)) {
            for (Iterator<AuditedRevDto> iter = auditTrailList.iterator(); iter.hasNext(); ) {
                AuditedRevDto dto = iter.next();
                if (!dto.getEntityTypeNames().contains(searchEntityDisplayName)) {
                    iter.remove();
                }
            }
        }
        Collections.sort(auditTrailList, AuditedRevDto.BY_REV_ID);
    }

    @ValidationMethod(on = LIST_AUDIT_TRAILS)
    public void validateSelection() {
    }

}
