package org.broadinstitute.gpinformatics.mercury.presentation.audit;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditedRevDto;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.ReflectionUtil;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;

@UrlBinding(AuditTrailActionBean.ACTIONBEAN_URL)
public class AuditTrailActionBean extends CoreActionBean {
    private static Logger logger = Logger.getLogger(AuditTrailActionBean.class.getName());
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
    private static final Date FIRST_AUDIT;
    static {
        try {
            // Set to the earliest date of a change that can be displayed in audit trail.
            // This is the rev_date of the earliest rev_info record that joins with REVCHANGES.
            FIRST_AUDIT = (new SimpleDateFormat("d-MMM-yyyy hh:mm:ss")).parse("6-AUG-2014 21:35:29");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

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

    /** These entity classnames are removed from the Entity Type dropdown and the list of modified entities. */
    private static final SortedSet<String> excludedDisplayClassnames = new TreeSet<String>(){{
        add("RevInfo");
        add("Preference");
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
                // Due to a bug in Hibernate we need to exclude abstract entity classes from
                // being exposed in the UI as if they were realizable entities (see GPLIM-3011).
                if (!ReflectionUtil.getAbstractEntityClassnames().contains(classname)) {
                    // Makes display names from the ending of canonical classname, but disambiguates
                    // it if necessary by including more of the canonical classname.
                    for (int idx = classname.lastIndexOf('.'); idx >= 0; idx = classname.lastIndexOf('.', idx)) {
                        String displayName = classname.substring(idx + 1);
                        if (excludedDisplayClassnames.contains(displayName)) {
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
        String usernameParam =  ANY_USER.equals(searchUsername) ? AuditReaderDao.IS_ANY_USER :
                (NO_USER.equals(searchUsername) ? AuditReaderDao.IS_NULL_USER : searchUsername);
        long from = (getDateRange().getStartTime() != null ?
                getDateRange().getStartTime() : FIRST_AUDIT).getTime()/1000;
        long to = (getDateRange().getEndTime() != null ? getDateRange().getEndTime() : new Date()).getTime()/1000;
        String canonicalClassname = displayToCanonicalClassnames.get(searchEntityDisplayName);
        auditTrailList = auditReaderDao.fetchAuditIds(from, to, usernameParam, canonicalClassname);
        // Convert canonical classnames to our display names.
        for (AuditedRevDto auditedRevDto : auditTrailList) {
            List<String> displayNames = new ArrayList<>();
            for (String canonicalName : auditedRevDto.getEntityTypeNames()) {
                // Doesn't show excluded classes or abstract classes (due to a Hibernate bug) and logs the latter.
                if (canonicalToDisplayClassnames.containsKey(canonicalName)) {
                    displayNames.add(canonicalToDisplayClassnames.get(canonicalName));
                } else {
                    for (String excludedDisplayClassname : excludedDisplayClassnames) {
                        if (!canonicalName.endsWith(excludedDisplayClassname)) {
                            logger.info("AuditReader found an unexpected class " + canonicalName +
                                        " at revision id " + auditedRevDto.getRevId());
                        }
                    }
                }
            }
            auditedRevDto.getEntityTypeNames().clear();
            auditedRevDto.getEntityTypeNames().addAll(displayNames);
        }
    }

    public static SortedSet<String> getExcludedDisplayClassnames() {
        return excludedDisplayClassnames;
    }
}
