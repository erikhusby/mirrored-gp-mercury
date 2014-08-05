package org.broadinstitute.gpinformatics.mercury.presentation.audit;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.ValidationMethod;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditedRevDto;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.ReflectionUtil;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@UrlBinding(AuditTrailActionBean.ACTIONBEAN_URL_BINDING)
public class AuditTrailActionBean extends CoreActionBean {
    public static final String ACTIONBEAN_URL_BINDING = "/audit/auditTrail.action";
    // Jsp pages
    private static final String AUDIT_TRAIL_LISTING_PAGE = "/audit/trail_list.jsp";
    private static final String AUDIT_TRAIL_ENTRY_PAGE = "/audit/trail_entry.jsp";
    // Events
    private static final String LIST_AUDIT_TRAILS = "listAuditTrails";
    private static final String VIEW_ENTITIES_AT_REV = "viewEntitiesAtRev";

    @Inject
    private AuditReaderDao auditReaderDao;

    private final String DEFAULT_TIME = "00:00:00";
    private String searchEntityDisplayName = "";
    private String searchUsername = "";
    private String searchStartTime = DEFAULT_TIME;
    private String searchEndTime = DEFAULT_TIME;
    private List<AuditedRevDto> auditTrailList = new ArrayList<>();
    private List<String> auditUsernames =  new ArrayList<>();
    // Maps display name to canonical classname.
    private Map<String, String> displayToCanonicalClassname = new HashMap<>();
    private Map<String, String> canonicalToDisplayClassname = new HashMap<>();
    private List<String> entityDisplayNames = new ArrayList<>();

    public AuditReaderDao getAuditReaderDao() {
        return auditReaderDao;
    }

    public void setAuditReaderDao(AuditReaderDao auditReaderDao) {
        this.auditReaderDao = auditReaderDao;
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

    public String getSearchStartTime() {
        return searchStartTime;
    }

    public void setSearchStartTime(String searchStartTime) {
        this.searchStartTime = searchStartTime;
    }

    public String getSearchEndTime() {
        return searchEndTime;
    }

    public void setSearchEndTime(String searchEndTime) {
        this.searchEndTime = searchEndTime;
    }

    public List<AuditedRevDto> getAuditTrailList() {
        return auditTrailList;
    }

    public List<String> getAuditUsernames() {
        return auditUsernames;
    }

    public List<String> getEntityDisplayNames() {
        return entityDisplayNames;
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////

    @DefaultHandler
    public Resolution selectAuditTrails() {
        setupOnce();
        return new ForwardResolution(AUDIT_TRAIL_LISTING_PAGE);
    }

    private void setupOnce() {
        if (auditUsernames.size() == 0) {
            auditUsernames.addAll(auditReaderDao.getAllAuditUsername());
            if (auditUsernames.contains(null)) {
                auditUsernames.remove(null);
                auditUsernames.add("no user");
            }
        }
        if (entityDisplayNames.size() == 0) {
            displayToCanonicalClassname.clear();
            canonicalToDisplayClassname.clear();
            for (String classname : ReflectionUtil.getEntityClassnames(ReflectionUtil.getMercuryAthenaClasses())) {
                // Makes display names from the ending of canonical classname, but disambiguates
                // it if necessary by including more of the canonical classname.
                for (int idx = classname.lastIndexOf('.') - 1; idx >= 0; idx = classname.lastIndexOf('.', idx) - 1) {
                    String displayName = classname.substring(idx);
                    if (!entityDisplayNames.contains(displayName)) {
                        entityDisplayNames.add(displayName);
                        displayToCanonicalClassname.put(displayName, classname);
                        canonicalToDisplayClassname.put(classname, displayName);
                        break;
                    }
                }
            }
            Collections.sort(entityDisplayNames);
        }
        getDateRange().setStart(new Date());
        getDateRange().setEnd(new Date());
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////

    @HandlesEvent(LIST_AUDIT_TRAILS)
    public Resolution listAuditTrails() {
        generateAuditTrailList();
        return new ForwardResolution(AUDIT_TRAIL_LISTING_PAGE);
    }

    private void generateAuditTrailList() {
        // Search Envers using the user's criteria.
        Collection<Long> revIds = auditReaderDao.fetchAuditIds(
                calculateSeconds(getDateRange().getStartTime(), searchStartTime),
                calculateSeconds(getDateRange().getEndTime(), searchEndTime),
                auditUsernames.contains(searchUsername) ? searchUsername : null).keySet();
        auditTrailList = auditReaderDao.fetchAuditedRevs(revIds);
        // Convert canonical classnames to our display names.
        for (AuditedRevDto auditedRevDto : auditTrailList) {
            List<String> displayNames = new ArrayList<>();
            for (String canonicalName : auditedRevDto.getEntityTypeNames()) {
                displayNames.add(canonicalToDisplayClassname.get(canonicalName));
            }
            auditedRevDto.getEntityTypeNames().clear();
            auditedRevDto.getEntityTypeNames().addAll(displayNames);
        }

        // Filters out the dtos that don't match the entity type criteria.  Ignore criteria if set to "any".
        if (entityDisplayNames.contains(searchEntityDisplayName)) {
            for (Iterator<AuditedRevDto> iter = auditTrailList.iterator(); iter.hasNext(); ) {
                AuditedRevDto dto = iter.next();
                if (!dto.getEntityTypeNames().contains(searchEntityDisplayName)) {
                    iter.remove();
                }
            }
        }
        Collections.sort(auditTrailList, AuditedRevDto.BY_REV_ID);
    }

    private long calculateSeconds(Date date, String time) {
        String[] parts = StringUtils.isNotBlank(time) ? time.split(":") : new String[]{""};
        int hour = parts.length > 0 ? Integer.parseInt(parts[0].trim()) : 0;
        int minute = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 0;
        int second = parts.length > 2 ? Integer.parseInt(parts[2].trim()) : 0;
        return date.getTime()/1000 + (((hour * 60) + minute) * 60) + second;
    }

    @ValidationMethod(on = LIST_AUDIT_TRAILS)
    public void validateSelection() {
        if (StringUtils.isBlank(searchStartTime)) {
            searchStartTime = DEFAULT_TIME;
        }
        if (StringUtils.isBlank(searchEndTime)) {
            searchEndTime = DEFAULT_TIME;
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////

    @HandlesEvent(VIEW_ENTITIES_AT_REV)
    public Resolution viewEntitiesAtRev() {
        generateAuditTrailEntry();
        return new ForwardResolution(AUDIT_TRAIL_ENTRY_PAGE);
    }

    private void generateAuditTrailEntry() {
    }


    /////////////////////////////////////////////////////////////////////////////////////////////////////
}
